class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var appSettingDao: AppSettingDao
    private lateinit var printerHelper: ThermalPrinterHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        appSettingDao = AppDatabase.getDatabase(this).appSettingDao()
        printerHelper = ThermalPrinterHelper(this)
        
        setupUI()
        loadSettings()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Pengaturan"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Printer Settings
        binding.cardPrinterSettings.setOnClickListener {
            showPrinterSelectionDialog()
        }
        
        binding.btnTestPrint.setOnClickListener {
            testCurrentPrinter()
        }
        
        binding.btnClearPrinter.setOnClickListener {
            clearSelectedPrinter()
        }
        
        // Shop Settings
        binding.btnSaveShopSettings.setOnClickListener {
            saveShopSettings()
        }
        
        binding.btnSelectLogo.setOnClickListener {
            selectLogo()
        }
        
        // Data Settings
        binding.btnBackupData.setOnClickListener {
            backupData()
        }
        
        binding.btnRestoreData.setOnClickListener {
            restoreData()
        }
        
        binding.btnSyncData.setOnClickListener {
            syncData()
        }
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                // Load shop settings
                val shopName = appSettingDao.getSetting("shop_name")?.value ?: ""
                val footerText1 = appSettingDao.getSetting("footer_text_1")?.value ?: ""
                val footerText2 = appSettingDao.getSetting("footer_text_2")?.value ?: ""
                val logoPath = appSettingDao.getSetting("logo_path")?.value ?: ""
                
                // Load printer settings
                val selectedPrinter = printerHelper.getSelectedPrinter()
                
                withContext(Dispatchers.Main) {
                    binding.etShopName.setText(shopName)
                    binding.etFooterText1.setText(footerText1)
                    binding.etFooterText2.setText(footerText2)
                    
                    if (logoPath.isNotEmpty()) {
                        binding.tvLogoPath.text = logoPath
                        binding.btnClearLogo.visibility = View.VISIBLE
                    }
                    
                    updatePrinterDisplay(selectedPrinter)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Error loading settings: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun updatePrinterDisplay(printer: ThermalPrinterHelper.PrinterDevice?) {
        if (printer != null) {
            binding.tvSelectedPrinter.text = printer.name
            binding.tvPrinterAddress.text = printer.address
            binding.tvPrinterAddress.visibility = View.VISIBLE
            binding.btnTestPrint.isEnabled = true
            binding.btnClearPrinter.isEnabled = true
            binding.tvNoPrinter.visibility = View.GONE
        } else {
            binding.tvSelectedPrinter.text = "Belum ada printer dipilih"
            binding.tvPrinterAddress.visibility = View.GONE
            binding.btnTestPrint.isEnabled = false
            binding.btnClearPrinter.isEnabled = false
            binding.tvNoPrinter.visibility = View.VISIBLE
        }
    }
    
    private fun showPrinterSelectionDialog() {
        val dialog = PrinterSelectionDialogFragment.newInstance { printer ->
            updatePrinterDisplay(printer)
        }
        dialog.show(supportFragmentManager, "printer_selection")
    }
    
    private fun testCurrentPrinter() {
        val selectedPrinter = printerHelper.getSelectedPrinter()
        if (selectedPrinter != null) {
            binding.btnTestPrint.isEnabled = false
            binding.btnTestPrint.text = "Testing..."
            
            lifecycleScope.launch {
                try {
                    val success = printerHelper.testPrintConnection(selectedPrinter.address)
                    
                    withContext(Dispatchers.Main) {
                        binding.btnTestPrint.isEnabled = true
                        binding.btnTestPrint.text = "Test Print"
                        
                        if (success) {
                            Toast.makeText(this@SettingsActivity, "Test print berhasil!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@SettingsActivity, "Test print gagal!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.btnTestPrint.isEnabled = true
                        binding.btnTestPrint.text = "Test Print"
                        Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun clearSelectedPrinter() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Printer")
            .setMessage("Hapus printer yang dipilih?")
            .setPositiveButton("Ya") { _, _ ->
                printerHelper.clearSelectedPrinter()
                updatePrinterDisplay(null)
                Toast.makeText(this, "Printer berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun saveShopSettings() {
        val shopName = binding.etShopName.text.toString().trim()
        val footerText1 = binding.etFooterText1.text.toString().trim()
        val footerText2 = binding.etFooterText2.text.toString().trim()
        
        if (shopName.isEmpty()) {
            binding.etShopName.error = "Nama toko harus diisi"
            return
        }
        
        lifecycleScope.launch {
            try {
                appSettingDao.updateSetting("shop_name", shopName)
                appSettingDao.updateSetting("footer_text_1", footerText1)
                appSettingDao.updateSetting("footer_text_2", footerText2)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Pengaturan berhasil disimpan", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun selectLogo() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        logoPickerLauncher.launch(intent)
    }
    
    private val logoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Save logo to internal storage and update setting
                lifecycleScope.launch {
                    try {
                        val logoPath = saveLogoToInternalStorage(uri)
                        appSettingDao.updateSetting("logo_path", logoPath)
                        
                        withContext(Dispatchers.Main) {
                            binding.tvLogoPath.text = logoPath
                            binding.btnClearLogo.visibility = View.VISIBLE
                            Toast.makeText(this@SettingsActivity, "Logo berhasil disimpan", Toast.LENGTH_SHORT).show()
                        }
                        
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsActivity, "Gagal menyimpan logo: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun saveLogoToInternalStorage(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "shop_logo_${System.currentTimeMillis()}.png"
            val file = File(filesDir, fileName)
            
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            
            file.absolutePath
        }
    }
    
    private fun syncData() {
        // Implementation for sync data with cloud
        binding.btnSyncData.isEnabled = false
        binding.btnSyncData.text = "Syncing..."
        
        lifecycleScope.launch {
            try {
                val syncHelper = SupabaseSyncHelper(this@SettingsActivity)
                val result = syncHelper.syncAll()
                
                withContext(Dispatchers.Main) {
                    binding.btnSyncData.isEnabled = true
                    binding.btnSyncData.text = "Sinkronisasi Data"
                    
                    if (result.success) {
                        Toast.makeText(this@SettingsActivity, "Sinkronisasi berhasil", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, "Sinkronisasi gagal: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSyncData.isEnabled = true
                    binding.btnSyncData.text = "Sinkronisasi Data"
                    Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    // ... other methods for backup, restore data
    
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
