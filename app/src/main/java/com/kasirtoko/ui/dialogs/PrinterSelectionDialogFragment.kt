class PrinterSelectionDialogFragment : DialogFragment() {
    
    private var _binding: DialogPrinterSelectionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var printerAdapter: PrinterAdapter
    private lateinit var printerHelper: ThermalPrinterHelper
    private var onPrinterSelected: ((ThermalPrinterHelper.PrinterDevice) -> Unit)? = null
    
    companion object {
        fun newInstance(
            onPrinterSelected: (ThermalPrinterHelper.PrinterDevice) -> Unit
        ): PrinterSelectionDialogFragment {
            return PrinterSelectionDialogFragment().apply {
                this.onPrinterSelected = onPrinterSelected
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPrinterSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        printerHelper = ThermalPrinterHelper(requireContext())
        
        setupUI()
        setupRecyclerView()
        loadPrinters()
    }
    
    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        binding.btnRefresh.setOnClickListener {
            loadPrinters()
        }
        
        binding.btnScanPrinters.setOnClickListener {
            scanForPrinters()
        }
    }
    
    private fun setupRecyclerView() {
        printerAdapter = PrinterAdapter(
            onPrinterClick = { printer ->
                selectPrinter(printer)
            },
            onTestClick = { printer ->
                testPrinter(printer)
            }
        )
        
        binding.rvPrinters.apply {
            adapter = printerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun loadPrinters() {
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val printers = printerHelper.getAvailablePrinters()
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    
                    if (printers.isNotEmpty()) {
                        printerAdapter.submitList(printers)
                        binding.tvEmptyState.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyState.text = "Tidak ada printer ditemukan.\n\nPastikan:\n• Bluetooth aktif\n• Printer sudah dipair\n• Printer dalam jangkauan"
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "Error: ${e.message}"
                }
            }
        }
    }
    
    private fun scanForPrinters() {
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }
        
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        
        if (bluetoothAdapter?.isEnabled != true) {
            AlertDialog.Builder(requireContext())
                .setTitle("Bluetooth Tidak Aktif")
                .setMessage("Aktifkan Bluetooth untuk mencari printer")
                .setPositiveButton("Aktifkan") { _, _ ->
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivity(enableBtIntent)
                }
                .setNegativeButton("Batal", null)
                .show()
            return
        }
        
        // Start discovery
        binding.btnScanPrinters.isEnabled = false
        binding.btnScanPrinters.text = "Mencari..."
        
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothAdapter.startDiscovery()
        }
        
        // Stop discovery after 12 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothAdapter.cancelDiscovery()
            }
            
            binding.btnScanPrinters.isEnabled = true
            binding.btnScanPrinters.text = "Cari Printer"
            loadPrinters()
        }, 12000)
    }
    
    private fun selectPrinter(printer: ThermalPrinterHelper.PrinterDevice) {
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Printer")
            .setMessage("Gunakan printer '${printer.name}' sebagai printer default?")
            .setPositiveButton("Ya") { _, _ ->
                printerHelper.setSelectedPrinter(printer.address, printer.name)
                onPrinterSelected?.invoke(printer)
                
                Toast.makeText(
                    requireContext(),
                    "Printer '${printer.name}' telah dipilih",
                    Toast.LENGTH_SHORT
                ).show()
                
                dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun testPrinter(printer: ThermalPrinterHelper.PrinterDevice) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val success = printerHelper.testPrintConnection(printer.address)
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    
                    if (success) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Test Print Berhasil")
                            .setMessage("Printer '${printer.name}' berhasil mencetak test page.")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Test Print Gagal")
                            .setMessage("Tidak dapat mencetak ke printer '${printer.name}'.\n\nPeriksa:\n• Printer menyala\n• Kertas tersedia\n• Jarak printer")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    
                    AlertDialog.Builder(requireContext())
                        .setTitle("Test Print Error")
                        .setMessage("Error: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
    
    private fun checkBluetoothPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH)
            }
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
        
        return permissions.isEmpty()
    }
    
    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        
        requestPermissions(permissions.toTypedArray(), BLUETOOTH_PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadPrinters()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Izin Bluetooth diperlukan untuk mencari printer",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1001
    }
}
