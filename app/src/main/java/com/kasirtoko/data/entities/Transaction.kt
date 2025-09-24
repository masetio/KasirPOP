@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kasirId: String,
    val kasirName: String,
    val totalAmount: Double,
    val paymentMethod: String, // "CASH", "QRIS", "DEBT"
    val paymentStatus: String, // "PAID", "UNPAID"
    val cashReceived: Double? = null,
    val cashChange: Double? = null,
    val customerName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val lastSyncAt: Long = 0
)
