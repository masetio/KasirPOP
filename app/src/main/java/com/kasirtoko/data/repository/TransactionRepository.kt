package com.kasirtoko.data.repository

import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.entities.Transaction
import com.kasirtoko.data.entities.TransactionItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val database: AppDatabase
) {
    
    fun getAllTransactions() = database.transactionDao().getAllTransactions()
    
    fun getTransactionsByKasir(kasirId: String) = database.transactionDao().getTransactionsByKasir(kasirId)
    
    fun getTransactionsByDateRange(startDate: Long, endDate: Long) = 
        database.transactionDao().getTransactionsByDateRange(startDate, endDate)
    
    fun getUnpaidTransactions() = database.transactionDao().getUnpaidTransactions()
    
    suspend fun getTransactionById(transactionId: String) = database.transactionDao().getTransactionById(transactionId)
    
    suspend fun getTransactionItems(transactionId: String) = database.transactionDao().getTransactionItems(transactionId)
    
    suspend fun insertTransaction(transaction: Transaction) = database.transactionDao().insertTransaction(transaction)
    
    suspend fun insertTransactionItems(items: List<TransactionItem>) = 
        database.transactionDao().insertTransactionItems(items)
    
    suspend fun markAsPaid(transactionId: String, paidAt: Long) = 
        database.transactionDao().markAsPaid(transactionId, paidAt)
    
    suspend fun getTodayTransactionCount() = database.transactionDao().getTodayTransactionCount()
    
    suspend fun getTodayRevenue() = database.transactionDao().getTodayRevenue()
    
    suspend fun getUnpaidCount() = database.transactionDao().getUnpaidCount()
    
    suspend fun getUnpaidAmount() = database.transactionDao().getUnpaidAmount()
    
    suspend fun getTransactionsByDateRangeSync(startDate: Long, endDate: Long) = 
        database.transactionDao().getTransactionsByDateRangeSync(startDate, endDate)
    
    suspend fun getTransactionsByKasirAndDateRange(kasirId: String, startDate: Long, endDate: Long) = 
        database.transactionDao().getTransactionsByKasirAndDateRange(kasirId, startDate, endDate)
}
