package com.kasirtoko.data.remote

import com.kasirtoko.data.entities.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseUser(
    val id: String,
    val username: String,
    val password: String,
    val role: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("updated_at") val updatedAt: Long
) {
    fun toLocalUser(): User {
        return User(
            id = id,
            username = username,
            password = password,
            role = role,
            fullName = fullName,
            createdAt = createdAt,
            isActive = isActive,
            lastSyncAt = System.currentTimeMillis()
        )
    }
}

@Serializable
data class SupabaseProduct(
    @SerialName("kode_barang") val kodeBarang: String,
    @SerialName("nama_barang") val namaBarang: String,
    val unit: String,
    @SerialName("harga_jual") val hargaJual: Double,
    @SerialName("harga_beli") val hargaBeli: Double,
    @SerialName("kode_barcode") val kodeBarcode: String?,
    val kategori: String,
    val stok: Int,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
) {
    fun toLocalProduct(): Product {
        return Product(
            kodeBarang = kodeBarang,
            namaBarang = namaBarang,
            unit = unit,
            hargaJual = hargaJual,
            hargaBeli = hargaBeli,
            kodeBarcode = kodeBarcode,
            kategori = kategori,
            stok = stok,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastSyncAt = System.currentTimeMillis()
        )
    }
}

@Serializable
data class SupabaseTransaction(
    val id: String,
    @SerialName("kasir_id") val kasirId: String,
    @SerialName("kasir_name") val kasirName: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("cash_received") val cashReceived: Double?,
    @SerialName("cash_change") val cashChange: Double?,
    @SerialName("customer_name") val customerName: String?,
    val notes: String?,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("paid_at") val paidAt: Long?
) {
    fun toLocalTransaction(): Transaction {
        return Transaction(
            id = id,
            kasirId = kasirId,
            kasirName = kasirName,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            cashReceived = cashReceived,
            cashChange = cashChange,
            customerName = customerName,
            notes = notes,
            createdAt = createdAt,
            paidAt = paidAt,
            lastSyncAt = System.currentTimeMillis()
        )
    }
}

@Serializable
data class SupabaseTransactionItem(
    val id: String,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("kode_barang") val kodeBarang: String,
    @SerialName("nama_barang") val namaBarang: String,
    val quantity: Int,
    @SerialName("harga_satuan") val hargaSatuan: Double,
    val subtotal: Double
) {
    fun toLocalTransactionItem(): TransactionItem {
        return TransactionItem(
            id = id,
            transactionId = transactionId,
            kodeBarang = kodeBarang,
            namaBarang = namaBarang,
            quantity = quantity,
            hargaSatuan = hargaSatuan,
            subtotal = subtotal,
            lastSyncAt = System.currentTimeMillis()
        )
    }
}

@Serializable
data class SupabaseStockMovement(
    val id: String,
    @SerialName("kode_barang") val kodeBarang: String,
    @SerialName("movement_type") val movementType: String,
    val quantity: Int,
    @SerialName("harga_beli") val hargaBeli: Double?,
    @SerialName("reference_id") val referenceId: String?,
    val notes: String?,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: Long
) {
    fun toLocalStockMovement(): StockMovement {
        return StockMovement(
            id = id,
            kodeBarang = kodeBarang,
            movementType = movementType,
            quantity = quantity,
            hargaBeli = hargaBeli,
            referenceId = referenceId,
            notes = notes,
            createdBy = createdBy,
            createdAt = createdAt,
            lastSyncAt = System.currentTimeMillis()
        )
    }
}

@Serializable
data class SupabaseAppSetting(
    val key: String,
    val value: String,
    @SerialName("updated_at") val updatedAt: Long
)

// Extension functions for converting to Supabase format
fun User.toSupabaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "username" to username,
    "password" to password,
    "role" to role,
    "full_name" to fullName,
    "created_at" to createdAt,
    "is_active" to isActive,
    "updated_at" to System.currentTimeMillis()
)

fun Product.toSupabaseMap(): Map<String, Any?> = mapOf(
    "kode_barang" to kodeBarang,
    "nama_barang" to namaBarang,
    "unit" to unit,
    "harga_jual" to hargaJual,
    "harga_beli" to hargaBeli,
    "kode_barcode" to kodeBarcode,
    "kategori" to kategori,
    "stok" to stok,
    "created_at" to createdAt,
    "updated_at" to System.currentTimeMillis()
)

fun Transaction.toSupabaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "kasir_id" to kasirId,
    "kasir_name" to kasirName,
    "total_amount" to totalAmount,
    "payment_method" to paymentMethod,
    "payment_status" to paymentStatus,
    "cash_received" to cashReceived,
    "cash_change" to cashChange,
    "customer_name" to customerName,
    "notes" to notes,
    "created_at" to createdAt,
    "paid_at" to paidAt
)

fun TransactionItem.toSupabaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "transaction_id" to transactionId,
    "kode_barang" to kodeBarang,
    "nama_barang" to namaBarang,
    "quantity" to quantity,
    "harga_satuan" to hargaSatuan,
    "subtotal" to subtotal
)

fun StockMovement.toSupabaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "kode_barang" to kodeBarang,
    "movement_type" to movementType,
    "quantity" to quantity,
    "harga_beli" to hargaBeli,
    "reference_id" to referenceId,
    "notes" to notes,
    "created_by" to createdBy,
    "created_at" to createdAt
)
