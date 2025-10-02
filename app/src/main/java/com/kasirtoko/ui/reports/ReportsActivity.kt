class ReportsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReportsBinding
    private lateinit var transactionDao: TransactionDao
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        transactionDao = AppDatabase.getDatabase(this).transactionDao()
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Laporan"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.cardSalesReport.setOnClickListener {
            startActivity(Intent(this, SalesReportActivity::class.java))
        }
        
        binding.cardCashierReport.setOnClickListener {
            startActivity(Intent(this, CashierReportActivity::class.java))
        }
        
        binding.cardDailyReport.setOnClickListener {
            startActivity(Intent(this, DailyReportActivity::class.java))
        }
        
        binding.cardFinancialReport.setOnClickListener {
            startActivity(Intent(this, FinancialReportActivity::class.java))
        }
        
        binding.cardStockReport.setOnClickListener {
            startActivity(Intent(this, StockReportActivity::class.java))
        }
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
