@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE kasirId = :kasirId ORDER BY createdAt DESC")
    fun getTransactionsByKasir(kasirId: String): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE paymentStatus = 'UNPAID' ORDER BY createdAt DESC")
    fun getUnpaidTransactions(): LiveData<List<Transaction>>
    
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getTransactionItems(transactionId: String): List<TransactionItem>
    
    @Insert
    suspend fun insertTransactionItems(items: List<TransactionItem>)
    
    @Query("UPDATE transactions SET paymentStatus = 'PAID', paidAt = :paidAt WHERE id = :transactionId")
    suspend fun markAsPaid(transactionId: String, paidAt: Long)
} 
