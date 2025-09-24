package com.kasirtoko.data.repository

import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.entities.*
import com.kasirtoko.data.models.CartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val database: AppDatabase
) {
    
    // User operations
    suspend fun login(username: String, password: String): User? {
        return database.userDao().login(username, password)
    }
    
    fun getAllUsers() = database.userDao().getAllActiveUsers()
    
    suspend fun insertUser(user: User) = database.userDao().insertUser(user)
    
    suspend fun updateUser(user: User) = database.userDao().updateUser(user)
    
    // Product operations
    fun getAllProducts() = database.productDao().getAllProducts()
    
    fun searchProducts(query: String) = database.productDao().searchProducts(query)
    
    fun getProductsByCategory(category: String) = database.productDao().getProductsByCategory(category)
    
    suspend fun getProductByBarcode(barcode: String) = database.productDao().getProductByBarcode(barcode)
    
    suspend fun insertProduct(product: Product) = database.productDao().insertProduct(product)
    
    suspend fun updateProduct(product: Product) = database.productDao().updateProduct(product)
    
    suspend fun deleteProduct(product: Product) = database.productDao().deleteProduct(product)
    
    // Transaction operations
    fun getAllTransactions() = database.transactionDao().getAllTransactions()
    
    fun getUnpaidTransactions() = database.transactionDao().getUnpaidTransactions()
    
    suspend fun insertTransaction(
        transaction: Transaction,
        items: List<TransactionItem>,
        cartItems: List<CartItem>
    ) {
        // Start transaction
        database.runInTransaction {
            // Insert transaction
            database.transactionDao().insertTransaction(transaction)
            
            // Insert transaction items
            database.transactionDao().insertTransactionItems(items)
            
            // Update product stock and create stock movements
            cartItems.forEach { cartItem ->
                database.productDao().reduceStock(cartItem.product.kodeBarang, cartItem.quantity)
                
                val stockMovement = StockMovement(
                    kodeBarang = cartItem.product.kodeBarang,
                    movementType = "OUT",
                    quantity = cartItem.quantity,
                    referenceId = transaction.id,
                    notes = "Penjualan",
                    createdBy = transaction.kasirId
                )
                database.stockMovementDao().insertStockMovement(stockMovement)
            }
        }
    }
    
    suspend fun markTransactionAsPaid(transactionId: String) {
        database.transactionDao().markAsPaid(transactionId, System.currentTimeMillis())
    }
    
    // Stock movement operations
    fun getStockMovements() = database.stockMovementDao().getAllStockMovements()
    
    fun getStockMovementsByProduct(kodeBarang: String) = 
        database.stockMovementDao().getStockMovementsByProduct(kodeBarang)
    
    suspend fun insertStockMovement(stockMovement: StockMovement) {
        database.runInTransaction {
            database.stockMovementDao().insertStockMovement(stockMovement)
            
            if (stockMovement.movementType == "IN") {
                database.productDao().addStock(stockMovement.kodeBarang, stockMovement.quantity)
                
                // Update purchase price if provided
                stockMovement.hargaBeli?.let { hargaBeli ->
                    database.productDao().updatePurchasePrice(stockMovement.kodeBarang, hargaBeli)
                }
            } else {
                database.productDao().reduceStock(stockMovement.kodeBarang, stockMovement.quantity)
            }
        }
    }
    
    // Settings operations
    suspend fun getSetting(key: String) = database.appSettingDao().getSetting(key)
    
    suspend fun updateSetting(key: String, value: String) {
        database.appSettingDao().updateSetting(key, value)
    }
    
    // Dashboard data
    suspend fun getDashboardData(): DashboardData {
        val totalProducts = database.productDao().getTotalProducts()
        val lowStockProducts = database.productDao().getLowStockCount(5)
        val todayTransactions = database.transactionDao().getTodayTransactionCount()
        val todayRevenue = database.transactionDao().getTodayRevenue()
        val unpaidCount = database.transactionDao().getUnpaidCount()
        val unpaidAmount = database.transactionDao().getUnpaidAmount()
        
        return DashboardData(
            totalProducts = totalProducts,
            lowStockProducts = lowStockProducts,
            todayTransactions = todayTransactions,
            todayRevenue = todayRevenue,
            unpaidCount = unpaidCount,
            unpaidAmount = unpaidAmount
        )
    }
}

data class DashboardData(
    val totalProducts: Int,
    val lowStockProducts: Int,
    val todayTransactions: Int,
    val todayRevenue: Double,
    val unpaidCount: Int,
    val unpaidAmount: Double
)
