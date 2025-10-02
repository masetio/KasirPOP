package com.kasirtoko.ui.kasir

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.entities.Transaction
import com.kasirtoko.databinding.ActivityTransactionHistoryBinding
import com.kasirtoko.ui.adapters.TransactionAdapter
import com.kasirtoko.utils.ThermalPrinterHelper
import com.kasirtoko.utils.formatCurrency
import kotlinx.coroutines.launch
import java.util.*

class TransactionHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var database: AppDatabase
    private lateinit var printerHelper: ThermalPrinterHelper
    
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var isAdmin: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getDatabase(this)
        printerHelper = ThermalPrinterHelper(this)
        
        // Get user session
        val prefs = getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        currentUserId = prefs.getString("user_id", "") ?: ""
        currentUserName = prefs.getString("full_name", "") ?: ""
        val userRole = prefs.getString("role", "") ?: ""
        isAdmin = userRole == "admin"
        
        setupUI()
        setupRecyclerView()
        setupFilters()
        loadTransactions()
    }
    
    private fun setupUI() {
        binding.toolbar.title = if (isAdmin) "Riwayat Transaksi" else "Riwayat Transaksi Saya"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.swipeRefresh.setOnRefreshListener {
            loadTransactions()
        }
        
        // Setup date filter buttons
        binding.chipToday.setOnClickListener {
            filterByToday()
        }
        
        binding.chipWeek.setOnClickListener {
            filterByWeek()
        }
        
        binding.chipMonth.setOnClickListener {
            filterByMonth()
        }
        
        binding.chipAll.setOnClickListener {
            filterByAll()
        }
        
        // Setup status filter
        binding.chipPaid.setOnClickListener {
            filterByStatus("PAID")
        }
        
        binding.chipUnpaid.setOnClickListener {
            filterByStatus("UNPAID")
        }
        
        binding.chipAllStatus.setOnClickListener {
            filterByStatus("ALL")
        }
        
        // Set default selections
        binding.chipToday.isChecked = true
        binding.chipAllStatus.isChecked = true
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                showTransactionDetail(transaction)
            },
            onPrintClick = { transaction ->
                printTransaction(transaction)
            }
        )
        
        binding.rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(this@TransactionHistoryActivity)
        }
    }
    
    private fun setupFilters() {
        // Setup chip group listeners for exclusive selection
        binding.chipGroupDateFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            // Handle date filter changes
        }
        
        binding.chipGroupStatusFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            // Handle status filter changes
        }
    }
    
    private fun loadTransactions() {
        binding.swipeRefresh.isRefreshing = true
        
        val observer = if (isAdmin) {
            database.transactionDao().getAllTransactions()
        } else {
            database.transactionDao().getTransactionsByKasir(currentUserId)
        }
        
        observer.observe(this) { transactions ->
            binding.swipeRefresh.isRefreshing = false
            
            // Apply current filters
            val filteredTransactions = applyFilters(transactions)
            
            transactionAdapter.submitList(filteredTransactions)
            updateSummary(filteredTransactions)
            
            if (filteredTransactions.isEmpty()) {
                binding.tvEmptyState.visibility = android.view.View.VISIBLE
                binding.rvTransactions.visibility = android.view.View.GONE
                binding.layoutSummary.visibility = android.view.View.GONE
            } else {
                binding.tvEmptyState.visibility = android.view.View.GONE
                binding.rvTransactions.visibility = android.view.View.VISIBLE
                binding.layoutSummary.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    private fun applyFilters(transactions: List<Transaction>): List<Transaction> {
        var filtered = transactions
        
        // Apply date filter
        filtered = when {
            binding.chipToday.isChecked -> filterTransactionsByToday(filtered)
            binding.chipWeek.isChecked -> filterTransactionsByWeek(filtered)
            binding.chipMonth.isChecked -> filterTransactionsByMonth(filtered)
            else -> filtered
        }
        
        // Apply status filter
        filtered = when {
            binding.chipPaid.isChecked -> filtered.filter { it.paymentStatus == "PAID" }
            binding.chipUnpaid.isChecked -> filtered.filter { it.paymentStatus == "UNPAID" }
            else -> filtered
        }
        
        return filtered.sortedByDescending { it.createdAt }
    }
    
    private fun filterTransactionsByToday(transactions: List<Transaction>): List<Transaction> {
        val calendar = Calendar.getInstance()
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        return transactions.filter { it.createdAt in startOfDay..endOfDay }
    }
    
    private fun filterTransactionsByWeek(transactions: List<Transaction>): List<Transaction> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfWeek = calendar.timeInMillis
        
        return transactions.filter { it.createdAt in startOfWeek..endOfWeek }
    }
    
    private fun filterTransactionsByMonth(transactions: List<Transaction>): List<Transaction> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis
        
        return transactions.filter { it.createdAt in startOfMonth..endOfMonth }
    }
    
    private fun filterByToday() {
        binding.chipToday.isChecked = true
        binding.chipWeek.isChecked = false
        binding.chipMonth.isChecked = false
        binding.chipAll.isChecked = false
        loadTransactions()
    }
    
    private fun filterByWeek() {
        binding.chipToday.isChecked = false
        binding.chipWeek.isChecked = true
        binding.chipMonth.isChecked = false
        binding.chipAll.isChecked = false
        loadTransactions()
    }
    
    private fun filterByMonth() {
        binding.chipToday.isChecked = false
        binding.chipWeek.isChecked = false
        binding.chipMonth.isChecked = true
        binding.chipAll.isChecked = false
        loadTransactions()
    }
    
    private fun filterByAll() {
        binding.chipToday.isChecked = false
        binding.chipWeek.isChecked = false
        binding.chipMonth.isChecked = false
        binding.chipAll.isChecked = true
        loadTransactions()
    }
    
    private fun filterByStatus(status: String) {
        when (status) {
            "PAID" -> {
                binding.chipPaid.isChecked = true
                binding.chipUnpaid.isChecked = false
                binding.chipAllStatus.isChecked = false
            }
            "UNPAID" -> {
                binding.chipPaid.isChecked = false
                binding.chipUnpaid.isChecked = true
                binding.chipAllStatus.isChecked = false
            }
            else -> {
                binding.chipPaid.isChecked = false
                binding.chipUnpaid.isChecked = false
                binding.chipAllStatus.isChecked = true
            }
        }
        loadTransactions()
    }
    
    private fun updateSummary(transactions: List<Transaction>) {
        val totalTransactions = transactions.size
        val paidTransactions = transactions.count { it.paymentStatus == "PAID" }
        val unpaidTransactions = transactions.count { it.paymentStatus == "UNPAID" }
        
        val totalAmount = transactions.sumOf { it.totalAmount }
        val paidAmount = transactions.filter { it.paymentStatus == "PAID" }.sumOf { it.totalAmount }
        val unpaidAmount = transactions.filter { it.paymentStatus == "UNPAID" }.sumOf { it.totalAmount }
        
        binding.tvTotalTransactions.text = "$totalTransactions transaksi"
        binding.tvPaidTransactions.text = "$paidTransactions lunas"
        binding.tvUnpaidTransactions.text = "$unpaidTransactions belum lunas"
        
        binding.tvTotalAmount.text = totalAmount.formatCurrency()
        binding.tvPaidAmount.text = paidAmount.formatCurrency()
        binding.tvUnpaidAmount.text = unpaidAmount.formatCurrency()
    }
    
    private fun showTransactionDetail(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                val items = database.transactionDao().getTransactionItems(transaction.id)
                
                val dialog = TransactionDetailDialogFragment.newInstance(transaction, items)
                dialog.show(supportFragmentManager, "transaction_detail")
                
            } catch (e: Exception) {
                Toast.makeText(this@TransactionHistoryActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun printTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                val items = database.transactionDao().getTransactionItems(transaction.id)
                printerHelper.printReceipt(transaction, items)
                Toast.makeText(this@TransactionHistoryActivity, "Nota sedang dicetak", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(this@TransactionHistoryActivity, "Error print: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(com.kasirtoko.R.menu.transaction_history_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            com.kasirtoko.R.id.action_search -> {
                showSearchDialog()
                true
            }
            com.kasirtoko.R.id.action_export -> {
                showExportOptions()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showSearchDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "Cari berdasarkan ID transaksi atau customer"
            setPadding(50, 30, 50, 30)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cari Transaksi")
            .setView(editText)
            .setPositiveButton("Cari") { _, _ ->
                val query = editText.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchTransactions(query)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun searchTransactions(query: String) {
        lifecycleScope.launch {
            try {
                val allTransactions = if (isAdmin) {
                    database.transactionDao().getAllTransactionsSync()
                } else {
                    database.transactionDao().getTransactionsByKasirSync(currentUserId)
                }
                
                val filteredTransactions = allTransactions.filter { transaction ->
                    transaction.id.contains(query, ignoreCase = true) ||
                    transaction.customerName?.contains(query, ignoreCase = true) == true ||
                    transaction.kasirName.contains(query, ignoreCase = true)
                }
                
                transactionAdapter.submitList(filteredTransactions)
                updateSummary(filteredTransactions)
                
                if (filteredTransactions.isEmpty()) {
                    Toast.makeText(this@TransactionHistoryActivity, "Tidak ditemukan transaksi dengan kata kunci '$query'", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@TransactionHistoryActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showExportOptions() {
        val options = arrayOf("Export ke CSV", "Export ke PDF")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Export Data")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportToCSV()
                    1 -> exportToPDF()
                }
            }
            .show()
    }
    
    private fun exportToCSV() {
        lifecycleScope.launch {
            try {
                val transactions = transactionAdapter.currentList
                if (transactions.isEmpty()) {
                    Toast.makeText(this@TransactionHistoryActivity, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val dateFormat = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                val timestamp = dateFormat.format(java.util.Date())
                val fileName = "transaksi_${if (isAdmin) "semua" else "kasir"}_$timestamp.csv"
                
                val file = java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                val writer = com.opencsv.CSVWriter(java.io.FileWriter(file))
                
                // Header
                writer.writeNext(arrayOf(
                    "ID Transaksi", "Kasir", "Tanggal", "Total", "Metode Bayar", 
                    "Status", "Customer", "Catatan"
                ))
                
                // Data
                transactions.forEach { transaction ->
                    writer.writeNext(arrayOf(
                        transaction.id,
                        transaction.kasirName,
                        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(transaction.createdAt)),
                        transaction.totalAmount.toString(),
                        transaction.paymentMethod,
                        transaction.paymentStatus,
                        transaction.customerName ?: "",
                        transaction.notes ?: ""
                    ))
                }
                
                writer.close()
                
                shareFile(file, "text/csv")
                
            } catch (e: Exception) {
                Toast.makeText(this@TransactionHistoryActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun exportToPDF() {
        lifecycleScope.launch {
            try {
                val transactions = transactionAdapter.currentList
                if (transactions.isEmpty()) {
                    Toast.makeText(this@TransactionHistoryActivity, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val pdfGenerator = com.kasirtoko.utils.PDFReportGenerator(this@TransactionHistoryActivity)
                val file = pdfGenerator.generateTransactionHistoryReport(
                    transactions = transactions,
                    title = if (isAdmin) "Riwayat Transaksi Semua Kasir" else "Riwayat Transaksi $currentUserName"
                )
                
                shareFile(file, "application/pdf")
                
            } catch (e: Exception) {
                Toast.makeText(this@TransactionHistoryActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Riwayat Transaksi"))
    }
}
