@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kodeBarang: String,
    val movementType: String, // "IN" or "OUT"
    val quantity: Int,
    val hargaBeli: Double? = null,
    val referenceId: String? = null, // ID transaksi jika OUT
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = 0
)
