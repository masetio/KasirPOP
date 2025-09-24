class AdminMainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdminMainBinding
    private var currentUser: String = ""
    private var currentUserName: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get user session
        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        currentUser = prefs.getString("user_id", "") ?: ""
        currentUserName = prefs.getString("full_name", "") ?: ""
        
        setupUI()
        setupBottomNavigation()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Admin - $currentUserName"
        setSupportActionBar(binding.toolbar)
        
        // Setup menu items click listeners
        binding.cardManageProducts.setOnClickListener {
            startActivity(Intent(this, ManageProductsActivity::class.java))
        }
        
        binding.cardManageStock.setOnClickListener {
            startActivity(Intent(this, ManageStockActivity::class.java))
        }
        
        binding.cardManageUsers.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }
        
        binding.cardReports.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }
        
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        binding.cardImportData.setOnClickListener {
            startActivity(Intent(this, ImportDataActivity::class.java))
        }
        
        binding.cardUnpaidTransactions.setOnClickListener {
            startActivity(Intent(this, UnpaidTransactionsActivity::class.java))
        }
        
        binding.cardSwitchToCashier.setOnClickListener {
            startActivity(Intent(this, KasirMainActivity::class.java))
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Already on dashboard
                    true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ManageProductsActivity::class.java))
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    true
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                syncWithCloud()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun syncWithCloud() {
        // Implement cloud sync with Supabase
        Toast.makeText(this, "Syncing with cloud...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val syncHelper = SupabaseSyncHelper(this@AdminMainActivity)
                syncHelper.syncAll()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminMainActivity, "Sync completed successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminMainActivity, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                // Clear user session
                val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                
                // Navigate to login
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
