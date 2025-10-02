class SalesReportActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySalesReportBinding
    private lateinit var reportAdapter: SalesReportAdapter
    private lateinit var transactionDao: TransactionDao
    private lateinit var productDao: ProductDao
    
    private var startDate: Long = 0
    private var endDate: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        transactionDao = AppDatabase.getDatabase(this).transactionDao()
        productDao = AppDatabase.getDatabase(this).productDao()
        
        // Set default date range (today)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startDate = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        endDate = calendar.timeInMillis
        
        setupUI()
        setupRecyclerView()
        loadReport()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Laporan Penjualan"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        updateDateDisplay()
        
        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }
        
        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }
        
        binding.btnExportCsv.setOnClickListener {
            exportToCSV()
        }
        
        binding.btnExportPdf.setOnClickListener {
            exportToPDF()
        }
        
        binding.btnRefresh.setOnClickListener {
            loadReport()
        }
    }
    
    private fun setupRecyclerView() {
        reportAdapter = SalesReportAdapter()
        binding.rvSalesReport.adapter = reportAdapter
        binding.rvSalesReport.layoutManager = LinearLayoutManager(this)
    }
    
    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.btnStartDate.text = dateFormat.format(Date(startDate))
        binding.btnEndDate.text = dateFormat.format(Date(endDate))
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
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    selectedCalendar.set(Calendar.MINUTE, 0)
                    selectedCalendar.set(Calendar.SECOND, 0)
                    selectedCalendar.set(Calendar.MILLISECOND, 0)
                    startDate = selectedCalendar.timeInMillis
                } else {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 23)
                    selectedCalendar.set(Calendar.MINUTE, 59)
                    selectedCalendar.set(Calendar.SECOND, 59)
                    selectedCalendar.set(Calendar.MILLISECOND, 999)
                    endDate = selectedCalendar.timeInMillis
                }
                
                updateDateDisplay()
                loadReport()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun loadReport() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                
                val transactions = transactionDao.getTransactionsByDateRangeSync(startDate, endDate)
                val transactionItems = mutableListOf<TransactionItem>()
                
                transactions.forEach { transaction ->
                    val items = transactionDao.getTransactionItems(transaction.id)
                    transactionItems.addAll(items)
                }
                
                // Group by product and calculate totals
                val productSales = transactionItems
                    .groupBy { it.kodeBarang }
                    .map { (kodeBarang, items) ->
                        val totalQuantity = items.sumOf { it.quantity }
                        val totalAmount = items.sumOf { it.subtotal }
                        val productName = items.first().namaBarang
                        
                        SalesReportItem(
                            kodeBarang = kodeBarang,
                            namaBarang = productName,
                            totalQuantity = totalQuantity,
                            totalAmount = totalAmount,
                            transactionCount = items.size
                        )
                    }
                    .sortedByDescending { it.totalAmount }
                
                val totalTransactions = transactions.size
                val totalAmount = transactions.sumOf { it.totalAmount }
                val totalItems = transactionItems.sumOf { it.quantity }
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    
                    binding.tvTotalTransactions.text = totalTransactions.toString()
                    binding.tvTotalAmount.text = formatCurrency(totalAmount)
                    binding.tvTotalItems.text = totalItems.toString()
                    
                    reportAdapter.submitList(productSales)
                    
                    binding.btnExportCsv.isEnabled = productSales.isNotEmpty()
                    binding.btnExportPdf.isEnabled = productSales.isNotEmpty()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@SalesReportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun exportToCSV() {
        lifecycleScope.launch {
            try {
                val reportData = reportAdapter.currentList
                if (reportData.isEmpty()) {
                    Toast.makeText(this@SalesReportActivity, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val fileName = "laporan_penjualan_$timestamp.csv"
                
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                val writer = CSVWriter(FileWriter(file))
                
                // Header
                writer.writeNext(arrayOf(
                    "Kode Barang", "Nama Barang", "Jumlah Terjual", 
                    "Total Penjualan", "Jumlah Transaksi"
                ))
                
                // Data
                reportData.forEach { item ->
                    writer.writeNext(arrayOf(
                        item.kodeBarang,
                        item.namaBarang,
                        item.totalQuantity.toString(),
                        item.totalAmount.toString(),
                        item.transactionCount.toString()
                    ))
                }
                
                writer.close()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SalesReportActivity,
                        "CSV berhasil diekspor: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    shareFile(file, "text/csv")
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SalesReportActivity,
                        "Gagal mengekspor CSV: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun exportToPDF() {
        lifecycleScope.launch {
            try {
                val reportData = reportAdapter.currentList
                if (reportData.isEmpty()) {
                    Toast.makeText(this@SalesReportActivity, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val pdfGenerator = PDFReportGenerator(this@SalesReportActivity)
                val file = pdfGenerator.generateSalesReport(reportData, startDate, endDate)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SalesReportActivity,
                        "PDF berhasil dibuat: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    shareFile(file, "application/pdf")
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SalesReportActivity,
                        "Gagal membuat PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan"))
    }
    
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
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

// Data class for sales report
data class SalesReportItem(
    val kodeBarang: String,
    val namaBarang: String,
    val totalQuantity: Int,
    val totalAmount: Double,
    val transactionCount: Int
)
