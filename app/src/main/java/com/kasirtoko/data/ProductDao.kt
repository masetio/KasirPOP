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
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)
    
    @Update
    suspend fun updateProduct(product: Product)
    
    @Delete
    suspend fun deleteProduct(product: Product)
    
    @Query("UPDATE products SET stok = stok + :quantity WHERE kodeBarang = :kodeBarang")
    suspend fun addStock(kodeBarang: String, quantity: Int)
    
    @Query("UPDATE products SET stok = stok - :quantity WHERE kodeBarang = :kodeBarang")
    suspend fun reduceStock(kodeBarang: String, quantity: Int)
    
    @Query("SELECT DISTINCT kategori FROM products ORDER BY kategori ASC")
    suspend fun getAllCategories(): List<String>
}
