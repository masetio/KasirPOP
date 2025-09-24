@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC")
    fun getAllStockMovements(): LiveData<List<StockMovement>>
    
    @Query("SELECT * FROM stock_movements WHERE kodeBarang = :kodeBarang ORDER BY createdAt DESC")
    fun getStockMovementsByProduct(kodeBarang: String): LiveData<List<StockMovement>>
    
    @Query("SELECT * FROM stock_movements WHERE movementType = :type ORDER BY createdAt DESC")
    fun getStockMovementsByType(type: String): LiveData<List<StockMovement>>
    
    @Query("SELECT * FROM stock_movements WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getStockMovementsByDateRange(startDate: Long, endDate: Long): LiveData<List<StockMovement>>
    
    @Query("SELECT * FROM stock_movements WHERE lastSyncAt = 0")
    suspend fun getMovementsForSync(): List<StockMovement>
    
    @Query("SELECT * FROM stock_movements WHERE id = :id")
    suspend fun getMovementById(id: String): StockMovement?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(stockMovement: StockMovement)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovements(stockMovements: List<StockMovement>)
    
    @Update
    suspend fun updateStockMovement(stockMovement: StockMovement)
    
    @Query("UPDATE stock_movements SET lastSyncAt = :syncTime WHERE id = :id")
    suspend fun updateSyncTime(id: String, syncTime: Long)
}
