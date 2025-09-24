@Entity(tableName = "transaction_items")
data class TransactionItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val kodeBarang: String,
    val namaBarang: String,
    val quantity: Int,
    val hargaSatuan: Double,
    val subtotal: Double,
    val lastSyncAt: Long = 0
)
