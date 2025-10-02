@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY namaBarang ASC")
    fun getAllProducts(): LiveData<List<Product>>
    
    @Query("SELECT * FROM products WHERE kategori = :kategori ORDER BY namaBarang ASC")
    fun getProductsByCategory(kategori: String): LiveData<List<Product>>
    
    @Query("SELECT * FROM products WHERE namaBarang LIKE '%' || :search || '%' OR kodeBarang LIKE '%' || :search || '%'")
    fun searchProducts(search: String): LiveData<List<Product>>
    
    @Query("SELECT * FROM products WHERE kodeBarcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): Product?
    
    @Query("SELECT * FROM products WHERE kodeBarang = :kodeBarang")
    suspend fun getProductByKode(kodeBarang: String): Product?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)
    
    @Query("SELECT * FROM products WHERE lastSyncAt = 0 OR updatedAt > lastSyncAt")
    suspend fun getProductsForSync(): List<Product>

    @Query("UPDATE products SET lastSyncAt = :syncTime WHERE kodeBarang = :kodeBarang")
    suspend fun updateSyncTime(kodeBarang: String, syncTime: Long)

    // Tambahan methods yang diperlukan
    @Query("SELECT COUNT(*) FROM products")
    suspend fun getTotalProducts(): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE stok <= :threshold")
    suspend fun getLowStockCount(threshold: Int): Int
    
    @Query("DELETE FROM products WHERE stok = 0")
    suspend fun deleteProductsWithZeroStock(): Int
    
    @Query("UPDATE products SET hargaBeli = :hargaBeli WHERE kodeBarang = :kodeBarang")
    suspend fun updatePurchasePrice(kodeBarang: String, hargaBeli: Double)
    
    @Query("SELECT * FROM products ORDER BY namaBarang ASC")
    suspend fun getAllProductsSync(): List<Product>
}
    
    
