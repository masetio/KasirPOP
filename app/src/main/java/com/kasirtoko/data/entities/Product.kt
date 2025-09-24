@Entity(tableName = "products")
data class Product(
    @PrimaryKey val kodeBarang: String,
    val namaBarang: String,
    val unit: String,
    val hargaJual: Double,
    val hargaBeli: Double,
    val kodeBarcode: String?,
    val kategori: String,
    val stok: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = 0
)
