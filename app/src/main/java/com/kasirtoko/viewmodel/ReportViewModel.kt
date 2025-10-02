package com.kasirtoko.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.models.SalesReportItem
import com.kasirtoko.data.models.PaymentSummary
import com.kasirtoko.data.models.KasirSummary
import com.kasirtoko.utils.ReportGenerator
import kotlinx.coroutines.launch
import java.io.File

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val reportGenerator = ReportGenerator(application)
    
    private val _salesReport = MutableLiveData<List<SalesReportItem>>()
    val salesReport: LiveData<List<SalesReportItem>> get() = _salesReport
    
    private val _paymentSummary = MutableLiveData<List<PaymentSummary>>()
    val paymentSummary: LiveData<List<PaymentSummary>> get() = _paymentSummary
    
    private val _kasirSummary = MutableLiveData<List<KasirSummary>>()
    val kasirSummary: LiveData<List<KasirSummary>> get() = _kasirSummary
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error
    
    fun generateSalesReport(
        startDate: Long,
        endDate: Long,
        categoryFilter: String? = null,
        kasirFilter: String? = null
    ) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val transactions = if (kasirFilter != null) {
                    database.transactionDao().getTransactionsByKasirAndDateRange(kasirFilter, startDate, endDate)
                } else {
                    database.transactionDao().getTransactionsByDateRangeSync(startDate, endDate)
                }
                
                val transactionItems = mutableListOf<com.kasirtoko.data.entities.TransactionItem>()
                transactions.forEach { transaction ->
                    val items = database.transactionDao().getTransactionItems(transaction.id)
                    transactionItems.addAll(items)
                }
                
                // Filter by category if specified
                val filteredItems = if (categoryFilter != null) {
                    transactionItems.filter { item ->
                        val product = database.productDao().getProductByKode(item.kodeBarang)
                        product?.kategori == categoryFilter
                    }
                } else {
                    transactionItems
                }
                
                // Group by product and calculate totals
                val productSales = filteredItems
                    .groupBy { it.kodeBarang }
                    .map { (kodeBarang, items) ->
                        val product = database.productDao().getProductByKode(kodeBarang)
                        SalesReportItem(
                            kodeBarang = kodeBarang,
                            namaBarang = items.first().namaBarang,
                            kategori = product?.kategori ?: "",
                            totalQuantity = items.sumOf { it.quantity },
                            totalAmount = items.sumOf { it.subtotal },
                            transactionCount = items.size,
                            avgPrice = items.sumOf { it.hargaSatuan } / items.size
                        )
                    }
                    .sortedByDescending { it.totalAmount }
                
                _salesReport.value = productSales
                
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun generateDailyReport(targetDate: Long) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val startOfDay = getStartOfDay(targetDate)
                val endOfDay = getEndOfDay(targetDate)
                
                val transactions = database.transactionDao().getTransactionsByDateRangeSync(startOfDay, endOfDay)
                val paidTransactions = transactions.filter { it.paymentStatus == "PAID" }
                
                // Group by payment method
                val paymentSummary = paidTransactions.groupBy { it.paymentMethod }
                    .map { (method, txns) ->
                        PaymentSummary(
                            method = method,
                            count = txns.size,
                            amount = txns.sumOf { it.totalAmount }
                        )
                    }
                
                // Group by kasir
                val kasirSummary = transactions.groupBy { it.kasirId }
                    .map { (kasirId, txns) ->
                        KasirSummary(
                            kasirId = kasirId,
                            kasirName = txns.first().kasirName,
                            transactionCount = txns.size,
                            totalAmount = txns.sumOf { it.totalAmount },
                            paidAmount = txns.filter { it.paymentStatus == "PAID" }.sumOf { it.totalAmount }
                        )
                    }
                    .sortedByDescending { it.totalAmount }
                
                _paymentSummary.value = paymentSummary
                _kasirSummary.value = kasirSummary
                
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun exportSalesReportCSV(
        startDate: Long,
        endDate: Long,
        categoryFilter: String? = null,
        kasirFilter: String? = null,
        onComplete: (File) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val file = reportGenerator.generateSalesReportCSV(startDate, endDate, categoryFilter, kasirFilter)
                onComplete(file)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun exportSalesReportPDF(
        startDate: Long,
        endDate: Long,
        categoryFilter: String? = null,
        kasirFilter: String? = null,
        onComplete: (File) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val file = reportGenerator.generateSalesReportPDF(startDate, endDate, categoryFilter, kasirFilter)
                onComplete(file)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun exportDailyReportPDF(
        targetDate: Long,
        onComplete: (File) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val file = reportGenerator.generateDailyReportPDF(targetDate)
                onComplete(file)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getEndOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
