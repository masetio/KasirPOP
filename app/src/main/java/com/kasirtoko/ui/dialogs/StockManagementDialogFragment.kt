class StockManagementDialogFragment : DialogFragment() {
    
    private var _binding: DialogStockManagementBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var product: Product
    private lateinit var stockMovementDao: StockMovementDao
    private lateinit var productDao: ProductDao
    private var onStockUpdated: (() -> Unit)? = null
    private var currentUser: String = ""
    
    companion object {
        fun newInstance(
            product: Product,
            onStockUpdated: () -> Unit
        ): StockManagementDialogFragment {
            return StockManagementDialogFragment().apply {
                this.product = product
                this.onStockUpdated = onStockUpdated
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogStockManagementBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        stockMovementDao = AppDatabase.getDatabase(requireContext()).stockMovementDao()
        productDao = AppDatabase.getDatabase(requireContext()).productDao()
        
        val prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        currentUser = prefs.getString("user_id", "") ?: ""
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.tvProductInfo.text = "${product.namaBarang} (${product.kodeBarang})"
        binding.tvCurrentStock.text = "Stok Saat Ini: ${product.stok} ${product.unit}"
        
        binding.rgMovementType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_stock_in -> {
                    binding.layoutPurchasePrice.visibility = View.VISIBLE
                    binding.etPurchasePrice.setText(product.hargaBeli.toString())
                }
                R.id.rb_stock_out -> {
                    binding.layoutPurchasePrice.visibility = View.GONE
                }
            }
        }
        
        binding.rbStockIn.isChecked = true
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveStockMovement()
        }
        
        binding.btnViewHistory.setOnClickListener {
            showStockHistory()
        }
    }
    
    private fun saveStockMovement() {
        val quantityStr = binding.etQuantity.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()
        val purchasePriceStr = binding.etPurchasePrice.text.toString().trim()
        
        if (quantityStr.isEmpty()) {
            binding.etQuantity.error = "Jumlah harus diisi"
            return
        }
        
        val quantity = try {
            quantityStr.toInt()
        } catch (e: NumberFormatException) {
            binding.etQuantity.error = "Jumlah tidak valid"
            return
        }
        
        if (quantity <= 0) {
            binding.etQuantity.error = "Jumlah harus lebih dari 0"
            return
        }
        
        val isStockIn = binding.rbStockIn.isChecked
        val movementType = if (isStockIn) "IN" else "OUT"
        
        // Validate stock out
        if (!isStockIn && quantity > product.stok) {
            binding.etQuantity.error = "Jumlah melebihi stok yang tersedia"
            return
        }
        
        val purchasePrice = if (isStockIn && purchasePriceStr.isNotEmpty()) {
            try {
                purchasePriceStr.toDouble()
            } catch (e: NumberFormatException) {
                binding.etPurchasePrice.error = "Harga beli tidak valid"
                return
            }
        } else null
        
        lifecycleScope.launch {
            try {
                // Create stock movement record
                val stockMovement = StockMovement(
                    kodeBarang = product.kodeBarang,
                    movementType = movementType,
                    quantity = quantity,
                    hargaBeli = purchasePrice,
                    notes = notes.ifEmpty { null },
                    createdBy = currentUser
                )
                
                stockMovementDao.insertStockMovement(stockMovement)
                
                // Update product stock
                if (isStockIn) {
                    productDao.addStock(product.kodeBarang, quantity)
                    // Update purchase price if provided
                    if (purchasePrice != null) {
                        productDao.updatePurchasePrice(product.kodeBarang, purchasePrice)
                    }
                } else {
                    productDao.reduceStock(product.kodeBarang, quantity)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Stok berhasil ${if (isStockIn) "ditambah" else "dikurangi"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    onStockUpdated?.invoke()
                    dismiss()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal menyimpan: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun showStockHistory() {
        val intent = Intent(requireContext(), StockHistoryActivity::class.java).apply {
            putExtra("product_code", product.kodeBarang)
            putExtra("product_name", product.namaBarang)
        }
        startActivity(intent)
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
