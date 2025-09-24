package com.kasirtoko.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.entities.*
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class SupabaseSyncHelper(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val supabase = SupabaseClient.client
    private val syncPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    
    suspend fun syncAll(): SyncResult {
        val result = SyncResult()
        
        try {
            // Check internet connectivity
            if (!isNetworkAvailable()) {
                throw Exception("Tidak ada koneksi internet")
            }
            
            // Sync in order of dependency
            result.userSync = syncUsers()
            result.productSync = syncProducts()
            result.transactionSync = syncTransactions()
            result.stockMovementSync = syncStockMovements()
            result.settingsSync = syncAppSettings()
            
            // Update last sync time
            syncPrefs.edit()
                .putLong("last_sync_time", System.currentTimeMillis())
                .apply()
            
            result.success = true
            result.message = "Sinkronisasi berhasil"
            
        } catch (e: Exception) {
            result.success = false
            result.message = "Sinkronisasi gagal: ${e.message}"
            Log.e("SupabaseSync", "Sync failed", e)
        }
        
        return result
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    private suspend fun syncUsers(): EntitySyncResult = withContext(Dispatchers.IO) {
        val result = EntitySyncResult()
        
        try {
            val lastSyncTime = syncPrefs.getLong("users_last_sync", 0)
            
            // Upload local changes
            val localUsers = database.userDao().getUsersForSync()
            
            localUsers.forEach { user ->
                try {
                    val supabaseUser = user.toSupabaseMap()
                    
                    if (user.lastSyncAt == 0L) {
                        // New user - insert
                        supabase.database.from("users").insert(supabaseUser)
                        result.uploaded++
                    } else if (user.updatedAt > user.lastSyncAt) {
                        // Updated user - upsert
                        supabase.database.from("users").upsert(supabaseUser)
                        result.uploaded++
                    }
                    
                    // Update sync time
                    database.userDao().updateSyncTime(user.id, System.currentTimeMillis())
                    
                } catch (e: Exception) {
                    Log.e("UserSync", "Failed to upload user ${user.id}", e)
                    result.errors.add("Upload user ${user.username}: ${e.message}")
                }
            }
            
            // Download remote changes
            val remoteUsers = supabase.database.from("users")
                .select(columns = Columns.ALL)
                .gt("updated_at", lastSyncTime)
                .decodeList<SupabaseUser>()
            
            remoteUsers.forEach { remoteUser ->
                try {
                    val localUser = database.userDao().getUserById(remoteUser.id)
                    
                    if (localUser == null || remoteUser.updatedAt > localUser.updatedAt) {
                        database.userDao().insertUser(remoteUser.toLocalUser())
                        result.downloaded++
                    }
                } catch (e: Exception) {
                    Log.e("UserSync", "Failed to download user ${remoteUser.id}", e)
                    result.errors.add("Download user ${remoteUser.username}: ${e.message}")
                }
            }
            
            // Update users last sync time
            syncPrefs.edit()
                .putLong("users_last_sync", System.currentTimeMillis())
                .apply()
                
        } catch (e: RestException) {
            result.errors.add("User sync REST error: ${e.message}")
        } catch (e: Exception) {
            result.errors.add("User sync error: ${e.message}")
        }
        
        result
    }
    
    private suspend fun syncProducts(): EntitySyncResult = withContext(Dispatchers.IO) {
        val result = EntitySyncResult()
        
        try {
            val lastSyncTime = syncPrefs.getLong("products_last_sync", 0)
            
            // Upload local changes
            val localProducts = database.productDao().getProductsForSync()
            
            // Batch upload for better performance
            val batchSize = 50
            localProducts.chunked(batchSize).forEach { batch ->
                try {
                    val supabaseProducts = batch.map { it.toSupabaseMap() }
                    supabase.database.from("products").upsert(supabaseProducts)
                    
                    // Update sync times
                    batch.forEach { product ->
                        database.productDao().updateSyncTime(product.kodeBarang, System.currentTimeMillis())
                    }
                    
                    result.uploaded += batch.size
                    
                } catch (e: Exception) {
                    Log.e("ProductSync", "Failed to upload batch", e)
                    result.errors.add("Upload products batch: ${e.message}")
                }
            }
            
            // Download remote changes
            val remoteProducts = supabase.database.from("products")
                .select(columns = Columns.ALL)
                .gt("updated_at", lastSyncTime)
                .decodeList<SupabaseProduct>()
            
            remoteProducts.chunked(batchSize).forEach { batch ->
                try {
                    val localProducts = batch.map { it.toLocalProduct() }
                    database.productDao().insertProducts(localProducts)
                    result.downloaded += batch.size
                } catch (e: Exception) {
                    Log.e("ProductSync", "Failed to download batch", e)
                    result.errors.add("Download products batch: ${e.message}")
                }
            }
            
            syncPrefs.edit()
                .putLong("products_last_sync", System.currentTimeMillis())
                .apply()
                
        } catch (e: Exception) {
            result.errors.add("Product sync error: ${e.message}")
        }
        
        result
    }
    
    private suspend fun syncTransactions(): EntitySyncResult = withContext(Dispatchers.IO) {
        val result = EntitySyncResult()
        
        try {
            val lastSyncTime = syncPrefs.getLong("transactions_last_sync", 0)
            
            // Upload local transactions
            val localTransactions = database.transactionDao().getTransactionsForSync()
            
            localTransactions.forEach { transaction ->
                try {
                    val supabaseTransaction = transaction.toSupabaseMap()
                    
                    if (transaction.lastSyncAt == 0L) {
                        // Upload transaction
                        supabase.database.from("transactions").insert(supabaseTransaction)
                        
                        // Upload transaction items
                        val items = database.transactionDao().getTransactionItems(transaction.id)
                        if (items.isNotEmpty()) {
                            val supabaseItems = items.map { it.toSupabaseMap() }
                            supabase.database.from("transaction_items").insert(supabaseItems)
                        }
                        
                        result.uploaded++
                    }
                    
                    database.transactionDao().updateSyncTime(transaction.id, System.currentTimeMillis())
                    
                } catch (e: Exception) {
                    Log.e("TransactionSync", "Failed to upload transaction ${transaction.id}", e)
                    result.errors.add("Upload transaction: ${e.message}")
                }
            }
            
            // Download remote transactions
            val remoteTransactions = supabase.database.from("transactions")
                .select(columns = Columns.ALL)
                .gt("created_at", lastSyncTime)
                .decodeList<SupabaseTransaction>()
            
            remoteTransactions.forEach { remoteTransaction ->
                try {
                    val localTransaction = database.transactionDao().getTransactionById(remoteTransaction.id)
                    
                    if (localTransaction == null) {
                        database.transactionDao().insertTransaction(remoteTransaction.toLocalTransaction())
                        
                        // Download transaction items
                        val remoteItems = supabase.database.from("transaction_items")
                            .select(columns = Columns.ALL)
                            .eq("transaction_id", remoteTransaction.id)
                            .decodeList<SupabaseTransactionItem>()
                        
                        if (remoteItems.isNotEmpty()) {
                            val localItems = remoteItems.map { it.toLocalTransactionItem() }
                            database.transactionDao().insertTransactionItems(localItems)
                        }
                        
                        result.downloaded++
                    }
                } catch (e: Exception) {
                    Log.e("TransactionSync", "Failed to download transaction ${remoteTransaction.id}", e)
                    result.errors.add("Download transaction: ${e.message}")
                }
            }
            
            syncPrefs.edit()
                .putLong("transactions_last_sync", System.currentTimeMillis())
                .apply()
                
        } catch (e: Exception) {
            result.errors.add("Transaction sync error: ${e.message}")
        }
        
        result
    }
    
    private suspend fun syncStockMovements(): EntitySyncResult = withContext(Dispatchers.IO) {
        val result = EntitySyncResult()
        
        try {
            val lastSyncTime = syncPrefs.getLong("stock_movements_last_sync", 0)
            
            // Upload local stock movements
            val localMovements = database.stockMovementDao().getMovementsForSync()
            
            localMovements.chunked(100).forEach { batch ->
                try {
                    val supabaseMovements = batch.map { it.toSupabaseMap() }
                    supabase.database.from("stock_movements").insert(supabaseMovements)
                    
                    batch.forEach { movement ->
                        database.stockMovementDao().updateSyncTime(movement.id, System.currentTimeMillis())
                    }
                    
                    result.uploaded += batch.size
                    
                } catch (e: Exception) {
                    Log.e("StockMovementSync", "Failed to upload batch", e)
                    result.errors.add("Upload stock movements: ${e.message}")
                }
            }
            
            // Download remote stock movements
            val remoteMovements = supabase.database.from("stock_movements")
                .select(columns = Columns.ALL)
                .gt("created_at", lastSyncTime)
                .decodeList<SupabaseStockMovement>()
            
            remoteMovements.chunked(100).forEach { batch ->
                try {
                    val localMovements = batch.map { it.toLocalStockMovement() }
                    database.stockMovementDao().insertStockMovements(localMovements)
                    result.downloaded += batch.size
                } catch (e: Exception) {
                    Log.e("StockMovementSync", "Failed to download batch", e)
                    result.errors.add("Download stock movements: ${e.message}")
                }
            }
            
            syncPrefs.edit()
                .putLong("stock_movements_last_sync", System.currentTimeMillis())
                .apply()
                
        } catch (e: Exception) {
            result.errors.add("Stock movement sync error: ${e.message}")
        }
        
        result
    }
    
    private suspend fun syncAppSettings(): EntitySyncResult = withContext(Dispatchers.IO) {
        val result = EntitySyncResult()
        
        try {
            // Upload local settings
            val localSettings = database.appSettingDao().getAllSettingsForSync()
            
            localSettings.forEach { setting ->
                try {
                    val supabaseSetting = mapOf(
                        "key" to setting.key,
                        "value" to setting.value,
                        "updated_at" to setting.updatedAt
                    )
                    
                    supabase.database.from("app_settings").upsert(supabaseSetting)
                    result.uploaded++
                } catch (e: Exception) {
                    result.errors.add("Upload setting ${setting.key}: ${e.message}")
                }
            }
            
            // Download remote settings
            val remoteSettings = supabase.database.from("app_settings")
                .select(columns = Columns.ALL)
                .decodeList<SupabaseAppSetting>()
            
            remoteSettings.forEach { remoteSetting ->
                try {
                    val localSetting = database.appSettingDao().getSetting(remoteSetting.key)
                    
                    if (localSetting == null || remoteSetting.updatedAt > localSetting.updatedAt) {
                        database.appSettingDao().insertSetting(
                            AppSetting(
                                key = remoteSetting.key,
                                value = remoteSetting.value,
                                updatedAt = remoteSetting.updatedAt
                            )
                        )
                        result.downloaded++
                    }
                } catch (e: Exception) {
                    result.errors.add("Download setting ${remoteSetting.key}: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            result.errors.add("Settings sync error: ${e.message}")
        }
        
        result
    }
    
    fun getLastSyncTime(): Long {
        return syncPrefs.getLong("last_sync_time", 0)
    }
    
    fun getLastSyncTimeFormatted(): String {
        val lastSync = getLastSyncTime()
        return if (lastSync > 0) {
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(lastSync))
        } else {
            "Belum pernah sinkronisasi"
        }
    }
}
