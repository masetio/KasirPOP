class ThermalPrinterHelper(private val context: Context) {
    
    private val appSettingDao = AppDatabase.getDatabase(context).appSettingDao()
    private val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
    
    data class PrinterDevice(
        val name: String,
        val address: String,
        val isConnected: Boolean = false
    )
    
    suspend fun getAvailablePrinters(): List<PrinterDevice> {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val printers = mutableListOf<PrinterDevice>()
        
        if (bluetoothAdapter?.isEnabled == true) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                
                val pairedDevices = bluetoothAdapter.bondedDevices
                
                pairedDevices?.forEach { device ->
                    // Check if device name suggests it's a printer
                    val deviceName = device.name ?: "Unknown Device"
                    if (isPotentialPrinter(deviceName) || device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.IMAGING) {
                        printers.add(
                            PrinterDevice(
                                name = deviceName,
                                address = device.address,
                                isConnected = isDeviceConnected(device)
                            )
                        )
                    }
                }
                
                // If no obvious printers found, add all paired devices for user to choose
                if (printers.isEmpty()) {
                    pairedDevices.forEach { device ->
                        printers.add(
                            PrinterDevice(
                                name = device.name ?: "Unknown Device",
                                address = device.address,
                                isConnected = isDeviceConnected(device)
                            )
                        )
                    }
                }
            }
        }
        
        return printers.sortedWith(compareByDescending<PrinterDevice> { it.isConnected }.thenBy { it.name })
    }
    
    private fun isPotentialPrinter(deviceName: String): Boolean {
        val printerKeywords = listOf(
            "printer", "pos", "receipt", "thermal", "bluetooth printer",
            "rpp", "goojprt", "zjiang", "xprinter", "epson", "star",
            "citizen", "bixolon", "sewoo", "custom", "partner"
        )
        
        return printerKeywords.any { keyword ->
            deviceName.contains(keyword, ignoreCase = true)
        }
    }
    
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            val method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as Boolean
        } catch (e: Exception) {
            false
        }
    }
    
    fun setSelectedPrinter(printerAddress: String, printerName: String) {
        prefs.edit()
            .putString("selected_printer_address", printerAddress)
            .putString("selected_printer_name", printerName)
            .apply()
    }
    
    fun getSelectedPrinter(): PrinterDevice? {
        val address = prefs.getString("selected_printer_address", null)
        val name = prefs.getString("selected_printer_name", null)
        
        return if (address != null && name != null) {
            PrinterDevice(name, address)
        } else null
    }
    
    fun clearSelectedPrinter() {
        prefs.edit()
            .remove("selected_printer_address")
            .remove("selected_printer_name")
            .apply()
    }
    
    suspend fun testPrintConnection(printerAddress: String): Boolean {
        return try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(printerAddress)
            
            val printer = EscPosPrinter(
                BluetoothPrintersConnections.selectSpecificPrinter(printerAddress),
                203,
                58f,
                32
            )
            
            // Test print
            val testText = """
                [C]<b>TEST PRINT</b>
                [C]================================
                [L]Printer: ${device.name}
                [L]Address: $printerAddress
                [L]Time: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}
                [L]
                [C]Test berhasil!
                [L]
                [L]
            """.trimIndent()
            
            printer.printFormattedText(testText)
            true
            
        } catch (e: Exception) {
            Log.e("PrinterTest", "Test print failed", e)
            false
        }
    }
    
    suspend fun printReceipt(
        transaction: Transaction, 
        items: List<TransactionItem>,
        printerAddress: String? = null
    ) {
        try {
            val selectedAddress = printerAddress ?: getSelectedPrinter()?.address
            
            if (selectedAddress == null) {
                throw Exception("Tidak ada printer yang dipilih. Silakan pilih printer di pengaturan.")
            }
            
            // Get shop settings
            val shopName = appSettingDao.getSetting("shop_name")?.value ?: "TOKO SAYA"
            val footerText1 = appSettingDao.getSetting("footer_text_1")?.value ?: ""
            val footerText2 = appSettingDao.getSetting("footer_text_2")?.value ?: ""
            val logoPath = appSettingDao.getSetting("logo_path")?.value
            
            val printer = EscPosPrinter(
                BluetoothPrintersConnections.selectSpecificPrinter(selectedAddress),
                203,
                58f,
                32
            )
            
            val receiptText = buildReceiptText(transaction, items, shopName, footerText1, footerText2, logoPath)
            
            printer.printFormattedText(receiptText)
            
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            throw e
        }
    }
    
    // ... rest of existing methods remain the same
    
    private fun buildReceiptText(
        transaction: Transaction,
        items: List<TransactionItem>,
        shopName: String,
        footerText1: String,
        footerText2: String,
        logoPath: String?
    ): String {
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val date = dateFormat.format(Date(transaction.createdAt))
        
        return buildString {
            append("[C]<b>$shopName</b>\n")
            append("[C]================================\n")
            append("[L]\n")
            append("[L]Kasir: ${transaction.kasirName}\n")
            append("[L]Tanggal: $date\n")
            append("[L]ID: ${transaction.id.takeLast(8)}\n")
            
            if (transaction.paymentStatus == "UNPAID") {
                append("[C]<b>** UNPAID **</b>\n")
            }
            
            append("[L]\n")
            append("[L]--------------------------------\n")
            
            items.forEach { item ->
                append("[L]${item.namaBarang}\n")
                append("[L]${item.quantity} x ${formatCurrency(item.hargaSatuan)}")
                append("[R]${formatCurrency(item.subtotal)}\n")
            }
            
            append("[L]--------------------------------\n")
            append("[L]<b>TOTAL[R]${formatCurrency(transaction.totalAmount)}</b>\n")
            
            when (transaction.paymentMethod) {
                "CASH" -> {
                    append("[L]Bayar[R]${formatCurrency(transaction.cashReceived ?: 0.0)}\n")
                    append("[L]Kembali[R]${formatCurrency(transaction.cashChange ?: 0.0)}\n")
                }
                "QRIS" -> {
                    append("[L]Pembayaran: QRIS\n")
                }
                "DEBT" -> {
                    append("[L]Pembayaran: UTANG\n")
                    transaction.customerName?.let {
                        append("[L]Atas nama: $it\n")
                    }
                }
            }
            
            append("[L]\n")
            
            if (footerText1.isNotEmpty()) {
                append("[C]$footerText1\n")
            }
            
            if (footerText2.isNotEmpty()) {
                append("[C]$footerText2\n")
            }
            
            append("[L]\n")
            append("[L]\n")
            append("[L]\n")
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }
}
