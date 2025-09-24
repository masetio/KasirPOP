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
    
