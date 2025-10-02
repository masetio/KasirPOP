package com.kasirtoko.ui.reports

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasirtoko.R
import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.entities.User
import com.kasirtoko.databinding.ActivityCashierReportBinding
import com.kasirtoko.ui.adapters.CashierReportAdapter
import com.kasirtoko.utils.CurrencyUtils
import com.kasirtoko.utils.DateUtils
import com.kasirtoko.utils.ReportGenerator
import kotlinx.coroutines.launch
import java.util.*

class CashierReportActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCashierReportBinding
    private lateinit var database: AppDatabase
    private lateinit var reportGenerator: ReportGenerator
    private lateinit var cashierReportAdapter: CashierReportAdapter
    
    private var startDate: Long = 0
    private var endDate: Long = 0
    private var selectedKasirId: String = "ALL"
    private var allUsers: List<User> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCashierReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getDatabase(this)
        reportGenerator = ReportGenerator(this)
        
        // Set default date range (this month)
        val today = System.currentTimeMillis()
        startDate = DateUtils.getStartOfMonth(today)
        endDate = DateUtils.getEndOfMonth(today)
        
        setupUI()
        setupRecyclerView()
        loadKasirList()
        generateReport()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Laporan Per Kasir"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        updateDateDisplay()
        
        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }
        
        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }
        
        binding.btnRefresh.setOnClickListener {
            generateReport()
        }
        
        binding.btnExportCsv.setOnClickListener {
            exportToCSV()
        }
        
        binding.btnExportPdf.setOnClickListener {
            exportToPDF()
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            generateReport()
        }
        
        setupDateRangeChips()
    }
    
    private fun setupDateRangeChips() {
        binding.chipToday.setOnClickListener {
            selectTodayRange()
        }
        
        binding.chipWeek.setOnClickListener {
            selectWeekRange()
        }
        
        binding.chipMonth.setOnClickListener {
            selectMonthRange()
        }
        
        binding.chipCustom.setOnClickListener {
            // Custom range already handled by date pickers
            updateChipSelection("custom")
        }
    }
    
    private fun selectTodayRange() {
        val today = System.currentTimeMillis()
        startDate = DateUtils.getStartOfDay(today)
        endDate = DateUtils.getEndOfDay(today)
        updateDateDisplay()
        updateChipSelection("today")
        generateReport()
    }
    
    private fun selectWeekRange() {
        val today = System.currentTimeMillis()
        startDate = DateUtils.getStartOfWeek(today)
        endDate = DateUtils.getEndOfWeek(today)
        updateDateDisplay()
        updateChipSelection("week")
        generateReport()
    }
    
    private fun selectMonthRange() {
        val today = System.currentTimeMillis()
        startDate = DateUtils.getStartOfMonth(today)
        endDate = DateUtils.getEndOfMonth(today)
        updateDateDisplay()
        updateChipSelection("month")
        generateReport()
    }
    
    private fun updateChipSelection(selected: String) {
        binding.chipToday.isChecked = selected == "today"
        binding.chipWeek.isChecked = selected == "week"
        binding.chipMonth.isChecked = selected == "month"
        binding.chipCustom.isChecked = selected == "custom"
    }
    
    private fun setupRecyclerView() {
        cashierReportAdapter = CashierReportAdapter { kasir ->
            showKasirDetailDialog(kasir)
        }
        
        binding.rvCashierReport.apply {
            adapter = cashierReportAdapter
            layoutManager = LinearLayoutManager(this@CashierReportActivity)
        }
    }
    
    private fun loadKasirList() {
        lifecycleScope.launch {
            try {
                allUsers = database.userDao().getAllActiveUsers()
                
                val kasirList = mutableListOf("Semua Kasir").apply {
                    addAll(allUsers.map { "${it.fullName} (${it.username})" })
                }
                
                val adapter = ArrayAdapter(
                    this@CashierReportActivity,
                    android.R.layout.simple_spinner_item,
                    kasirList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                
                binding.spinnerKasir.adapter = adapter
                binding.spinnerKasir.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedKasirId = if (position == 0) "ALL" else allUsers[position - 1].id
                        generateReport()
                    }
                    
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@CashierReportActivity, "Error loading kasir list: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateDateDisplay() {
        binding.btnStartDate.text = DateUtils.formatForDisplay(startDate)
        binding.btnEndDate.text = DateUtils.formatForDisplay(endDate)
        binding.tvDateRange.text = "Periode: ${DateUtils.getDateRangeText(startDate, endDate)}"
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val currentDate = if (isStartDate) startDate else endDate
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                
                if (isStartDate) {
                    startDate = DateUtils.getStartOfDay(selectedCalendar.timeInMillis)
                    if (startDate > endDate) {
                        endDate = DateUtils.getEndOfDay(selectedCalendar.timeInMillis)
                    }
                } else {
                    endDate = DateUtils.getEndOfDay(selectedCalendar.timeInMillis)
                    if (endDate < startDate) {
                        startDate = DateUtils.getStartOfDay(selectedCalendar.timeInMillis)
                    }
                }
                
                updateDateDisplay()
                updateChipSelection("custom")
                generateReport()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun generateReport() {
        binding.swipeRefresh.isRefreshing = true
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val transactions = if (selectedKasirId == "ALL") {
                    database.transactionDao().getTransactionsByDateRangeSync(startDate, endDate)
                } else {
                    database.transactionDao().getTransactionsByKasirAndDateRange(selectedKasirId, startDate, endDate)
                }
                
                // Group transactions by kasir
                val kasirReports = mutableListOf<CashierReportItem>()
                
                if (selectedKasirId == "ALL") {
                    // Generate report for all kasirs
                    val groupedTransactions = transactions.groupBy { it.kasirId }
                    
                    for ((kasirId, kasirTransactions) in groupedTransactions) {
                        val kasirUser = allUsers.find { it.id == kasirId }
                        val kasirName = kasirUser?.fullName ?: kasirTransactions.first().kasirName
                        val kasirUsername = kasirUser?.username ?: ""
                        
                        val report = generateKasirReport(kasirTransactions, kasirId, kasirName, kasirUsername)
                        kasirReports.add(report)
                    }
                } else {
                    // Generate report for selected kasir
                    val kasirUser = allUsers.find { it.id == selectedKasirId }
                    if (kasirUser != null) {
                        val report = generateKasirReport(transactions, kasirUser.id, kasirUser.fullName, kasirUser.username)
                        kasirReports.add(report)
                    }
                }
                
                // Sort by total amount descending
                kasirReports.sortByDescending { it.totalAmount }
                
                binding.swipeRefresh.isRefreshing = false
                binding.progressBar.visibility = View.GONE
                
                // Update UI
                cashierReportAdapter.submitList(kasirReports)
                updateSummary(kasirReports)
                
                if (kasirReports.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvCashierReport.visibility = View.GONE
                    binding.cardSummary.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvCashierReport.visibility = View.VISIBLE
                    binding.cardSummary.visibility = View.VISIBLE
                }
                
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@CashierReportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private suspend fun generateKasirReport(
        transactions: List<com.kasirtoko.data.entities.Transaction>,
        kasirId: String,
        kasirName: String,
        kasirUsername: String
    ): CashierReportItem {
        
        val totalTransactions = transactions.size
        val paidTransactions = transactions.filter { it.paymentStatus == "PAID" }
        val unpaidTransactions = transactions.filter { it.paymentStatus == "UNPAID" }
        
        val totalAmount = transactions.sumOf { it.totalAmount }
        val paidAmount = paidTransactions.sumOf { it.totalAmount }
        val unpaidAmount = unpaidTransactions.sumOf { it.totalAmount }
        
        // Payment method breakdown
        val paymentMethodBreakdown = paidTransactions.groupBy { it.paymentMethod }
            .mapValues { (_, txns) -> 
                PaymentMethodSummary(
                    count = txns.size,
                    amount = txns.sumOf { it.totalAmount }
                )
            }
        
        // Get all transaction items for this kasir
        val allTransactionItems = mutableListOf<com.kasirtoko.data.entities.TransactionItem>()
        transactions.forEach { transaction ->
            val items = database.transactionDao().getTransactionItems(transaction.id)
            allTransactionItems.addAll(items)
        }
        
        val totalItemsSold = allTransactionItems.sumOf { it.quantity }
        val averageTransactionValue = if (totalTransactions > 0) totalAmount / totalTransactions else 0.0
        
        // Find best selling hour (if transactions exist)
        val bestSellingHour = if (transactions.isNotEmpty()) {
            transactions.groupBy { 
                Calendar.getInstance().apply { timeInMillis = it.createdAt }.get(Calendar.HOUR_OF_DAY) 
            }.maxByOrNull { it.value.size }?.key ?: 0
        } else 0
        
        return CashierReportItem(
            kasirId = kasirId,
            kasirName = kasirName,
            kasirUsername = kasirUsername,
            totalTransactions = totalTransactions,
            paidTransactions = paidTransactions.size,
            unpaidTransactions = unpaidTransactions.size,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            unpaidAmount = unpaidAmount,
            totalItemsSold = totalItemsSold,
            averageTransactionValue = averageTransactionValue,
            paymentMethodBreakdown = paymentMethodBreakdown,
            bestSellingHour = bestSellingHour,
            firstTransactionTime = transactions.minByOrNull { it.createdAt }?.createdAt,
            lastTransactionTime = transactions.maxByOrNull { it.createdAt }?.createdAt
        )
    }
    
    private fun updateSummary(reports: List<CashierReportItem>) {
        val totalKasirs = reports.size
        val totalTransactions = reports.sumOf { it.totalTransactions }
        val totalAmount = reports.sumOf { it.totalAmount }
        val totalPaidAmount = reports.sumOf { it.paidAmount }
        val totalUnpaidAmount = reports.sumOf { it.unpaidAmount }
        val totalItemsSold = reports.sumOf { it.totalItemsSold }
        
        binding.tvTotalKasirs.text = "$totalKasirs kasir"
        binding.tvTotalTransactions.text = "$totalTransactions transaksi"
        binding.tvTotalAmount.text = CurrencyUtils.formatToRupiah(totalAmount)
        binding.tvTotalPaidAmount.text = CurrencyUtils.formatToRupiah(totalPaidAmount)
        binding.tvTotalUnpaidAmount.text = CurrencyUtils.formatToRupiah(totalUnpaidAmount)
        binding.tvTotalItemsSold.text = "$totalItemsSold item"
        
        if (totalTransactions > 0) {
            val averagePerTransaction = totalAmount / totalTransactions
            binding.tvAverageTransaction.text = "Rata-rata: ${CurrencyUtils.formatToRupiah(averagePerTransaction)}"
        } else {
            binding.tvAverageTransaction.text = "Rata-rata: Rp 0"
        }
        
        // Find best performing kasir
        val bestKasir = reports.maxByOrNull { it.totalAmount }
        if (bestKasir != null) {
            binding.tvBestKasir.text = "Terbaik: ${bestKasir.kasirName}"
            binding.tvBestKasir.visibility = View.VISIBLE
        } else {
            binding.tvBestKasir.visibility = View.GONE
        }
    }
    
    private fun showKasirDetailDialog(kasir: CashierReportItem) {
        val dialog = CashierDetailDialogFragment.newInstance(kasir, startDate, endDate)
        dialog.show(supportFragmentManager, "kasir_detail")
    }
    
    private fun exportToCSV() {
        val reports = cashierReportAdapter.currentList
        if (reports.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val file = reportGenerator.generateCashierReportCSV(reports, startDate, endDate)
                shareFile(file, "text/csv")
                
            } catch (e: Exception) {
                Toast.makeText(this@CashierReportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun exportToPDF() {
        val reports = cashierReportAdapter.currentList
        if (reports.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val file = reportGenerator.generateCashierReportPDF(reports, startDate, endDate)
                shareFile(file, "application/pdf")
                
            } catch (e: Exception) {
                Toast.makeText(this@CashierReportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun shareFile(file: java.io.File, mimeType: String) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Laporan Kasir"))
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

// Data classes
data class CashierReportItem(
    val kasirId: String,
    val kasirName: String,
    val kasirUsername: String,
    val totalTransactions: Int,
    val paidTransactions: Int,
    val unpaidTransactions: Int,
    val totalAmount: Double,
    val paidAmount: Double,
    val unpaidAmount: Double,
    val totalItemsSold: Int,
    val averageTransactionValue: Double,
    val paymentMethodBreakdown: Map<String, PaymentMethodSummary>,
    val bestSellingHour: Int,
    val firstTransactionTime: Long?,
    val lastTransactionTime: Long?
)

data class PaymentMethodSummary(
    val count: Int,
    val amount: Double
)
