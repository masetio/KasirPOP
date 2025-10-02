Baik, saya akan melanjutkan dengan bagian-bagian yang belum selesai:

## 11. Product Adapter

```kotlin
// ui/adapters/ProductAdapter.kt
class ProductAdapter(
    private val onProductClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                tvProductName.text = product.namaBarang
                tvProductCode.text = product.kodeBarang
                tvPrice.text = formatCurrency(product.hargaJual)
                tvStock.text = "Stok: ${product.stok} ${product.unit}"
                tvCategory.text = product.kategori
                
                // Set stock color based on quantity
                tvStock.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        when {
                            product.stok == 0 -> R.color.error
                            product.stok <= 5 -> android.R.color.holo_orange_dark
                            else -> android.R.color.holo_green_dark
                        }
                    )
                )
                
                root.setOnClickListener {
                    if (product.stok > 0) {
                        onProductClick(product)
                    }
                }
                
                root.alpha = if (product.stok > 0) 1.0f else 0.6f
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.kodeBarang == newItem.kodeBarang
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
```

## 12. Cart Adapter

```kotlin
// ui/adapters/CartAdapter.kt
class CartAdapter(
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onRemoveItem: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItem) {
            binding.apply {
                tvProductName.text = cartItem.product.namaBarang
                tvPrice.text = formatCurrency(cartItem.product.hargaJual)
                tvQuantity.text = cartItem.quantity.toString()
                tvSubtotal.text = formatCurrency(cartItem.product.hargaJual * cartItem.quantity)
                tvMaxStock.text = "Max: ${cartItem.product.stok}"
                
                btnDecrease.setOnClickListener {
                    if (cartItem.quantity > 1) {
                        onQuantityChanged(cartItem, cartItem.quantity - 1)
                    }
                }
                
                btnIncrease.setOnClickListener {
                    if (cartItem.quantity < cartItem.product.stok) {
                        onQuantityChanged(cartItem, cartItem.quantity + 1)
                    }
                }
                
                btnRemove.setOnClickListener {
                    onRemoveItem(cartItem)
                }
                
                // Disable increase button if max stock reached
                btnIncrease.isEnabled = cartItem.quantity < cartItem.product.stok
                btnDecrease.isEnabled = cartItem.quantity > 1
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.product.kodeBarang == newItem.product.kodeBarang
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
```

## 13. Payment Dialog Fragment

```kotlin
// ui/dialogs/PaymentDialogFragment.kt
class PaymentDialogFragment : DialogFragment() {
    
    private var _binding: DialogPaymentBinding? = null
    private val binding get() = _binding!!
    
    private var totalAmount: Double = 0.0
    private var onPaymentConfirmed: ((String, Double?) -> Unit)? = null
    private var selectedPaymentMethod = "CASH"
    
    companion object {
        fun newInstance(
            totalAmount: Double,
            onPaymentConfirmed: (String, Double?) -> Unit
        ): PaymentDialogFragment {
            return PaymentDialogFragment().apply {
                this.totalAmount = totalAmount
                this.onPaymentConfirmed = onPaymentConfirmed
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }
    
    private fun setupUI() {
        binding.tvTotalAmount.text = "Total: ${formatCurrency(totalAmount)}"
        
        // Setup payment method radio buttons
        binding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            selectedPaymentMethod = when (checkedId) {
                R.id.rb_cash -> "CASH"
                R.id.rb_qris -> "QRIS"
                R.id.rb_debt -> "DEBT"
                else -> "CASH"
            }
            
            updateUIBasedOnPaymentMethod()
        }
        
        // Set default selection
        binding.rbCash.isChecked = true
        updateUIBasedOnPaymentMethod()
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnConfirm.setOnClickListener {
            processPayment()
        }
        
        // Auto calculate change for cash payment
        binding.etCashReceived.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (selectedPaymentMethod == "CASH") {
                    calculateChange()
                }
            }
        })
    }
    
    private fun updateUIBasedOnPaymentMethod() {
        when (selectedPaymentMethod) {
            "CASH" -> {
                binding.layoutCashPayment.visibility = View.VISIBLE
                binding.layoutDebtPayment.visibility = View.GONE
                binding.etCashReceived.setText(totalAmount.toString())
                calculateChange()
            }
            "QRIS" -> {
                binding.layoutCashPayment.visibility = View.GONE
                binding.layoutDebtPayment.visibility = View.GONE
            }
            "DEBT" -> {
                binding.layoutCashPayment.visibility = View.GONE
                binding.layoutDebtPayment.visibility = View.VISIBLE
            }
        }
    }
    
    private fun calculateChange() {
        val cashReceivedText = binding.etCashReceived.text.toString()
        if (cashReceivedText.isNotEmpty()) {
            try {
                val cashReceived = cashReceivedText.toDouble()
                val change = cashReceived - totalAmount
                binding.tvChange.text = "Kembalian: ${formatCurrency(change)}"
                
                binding.btnConfirm.isEnabled = cashReceived >= totalAmount
                
                if (change < 0) {
                    binding.tvChange.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.error)
                    )
                } else {
                    binding.tvChange.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                    )
                }
            } catch (e: NumberFormatException) {
                binding.btnConfirm.isEnabled = false
                binding.tvChange.text = "Kembalian: -"
            }
        } else {
            binding.btnConfirm.isEnabled = false
            binding.tvChange.text = "Kembalian: -"
        }
    }
    
    private fun processPayment() {
        when (selectedPaymentMethod) {
            "CASH" -> {
                val cashReceivedText = binding.etCashReceived.text.toString()
                if (cashReceivedText.isNotEmpty()) {
                    try {
                        val cashReceived = cashReceivedText.toDouble()
                        if (cashReceived >= totalAmount) {
                            onPaymentConfirmed?.invoke("CASH", cashReceived)
                            dismiss()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Jumlah bayar kurang dari total",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(
                            requireContext(),
                            "Masukkan jumlah yang valid",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Masukkan jumlah bayar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            "QRIS" -> {
                // Show QRIS confirmation dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Konfirmasi Pembayaran QRIS")
                    .setMessage("Pastikan pembayaran QRIS telah berhasil")
                    .setPositiveButton("Sudah Bayar") { _, _ ->
                        onPaymentConfirmed?.invoke("QRIS", null)
                        dismiss()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
            
            "DEBT" -> {
                val customerName = binding.etCustomerName.text.toString().trim()
                if (customerName.isNotEmpty()) {
                    onPaymentConfirmed?.invoke("DEBT", null)
                    dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Masukkan nama customer",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
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
```

## 14. Barcode Scanner Activity

```kotlin
// ui/scanner/BarcodeScannerActivity.kt
class BarcodeScannerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBarcodeScannerBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkCameraPermission()
    }
    
    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        
        binding.btnFlashlight.setOnClickListener {
            toggleFlashlight()
        }
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
        
        // Image Analysis for barcode scanning
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(this),
                    BarcodeAnalyzer { barcode ->
                        onBarcodeDetected(barcode)
                    }
                )
            }
        
        // Select back camera
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Toast.makeText(this, "Failed to bind camera: ${exc.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun onBarcodeDetected(barcode: String) {
        // Return the scanned barcode
        val intent = Intent().apply {
            putExtra("barcode", barcode)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
    
    private fun toggleFlashlight() {
        // Implementation for flashlight toggle
        // This requires camera2 API for more control
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
    }
    
    companion object {
        private const val CAMERA_PERMISSION_CODE = 1001
    }
}

// Barcode Analyzer
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_ITF
            )
            .build()
    )
    
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            onBarcodeDetected(value)
                            return@addOnSuccessListener
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
```

## 15. Admin Main Activity

```kotlin
// ui/admin/AdminMainActivity.kt
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
```

## 16. Manage Products Activity

```kotlin
// ui/admin/ManageProductsActivity.kt
class ManageProductsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityManageProductsBinding
    private lateinit var productAdapter: AdminProductAdapter
    private lateinit var productDao: ProductDao
    private lateinit var viewModel: ProductViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        productDao = AppDatabase.getDatabase(this).productDao()
        viewModel = ViewModelProvider(this)[ProductViewModel::class.java]
        
        setupUI()
        setupRecyclerView()
        observeData()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Kelola Produk"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.fabAddProduct.setOnClickListener {
            showAddProductDialog()
        }
        
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchProducts(s.toString())
            }
        })
        
        // Setup category filter
        setupCategoryFilter()
    }
    
    private fun setupRecyclerView() {
        productAdapter = AdminProductAdapter(
            onEditClick = { product ->
                showEditProductDialog(product)
            },
            onDeleteClick = { product ->
                showDeleteConfirmation(product)
            }
        )
        
        binding.rvProducts.adapter = productAdapter
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
    }
    
    private fun observeData() {
        viewModel.allProducts.observe(this) { products ->
            productAdapter.submitList(products)
            binding.tvProductCount.text = "${products.size} produk"
        }
    }
    
    private fun setupCategoryFilter() {
        lifecycleScope.launch {
            val categories = productDao.getAllCategories()
            val categoryList = mutableListOf("Semua Kategori").apply {
                addAll(categories)
            }
            
            val adapter = ArrayAdapter(
                this@ManageProductsActivity,
                android.R.layout.simple_spinner_item,
                categoryList
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            
            binding.spinnerCategory.adapter = adapter
            binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedCategory = categoryList[position]
                    if (selectedCategory == "Semua Kategori") {
                        viewModel.allProducts.observe(this@ManageProductsActivity) { products ->
                            productAdapter.submitList(products)
                        }
                    } else {
                        viewModel.getProductsByCategory(selectedCategory).observe(this@ManageProductsActivity) { products ->
                            productAdapter.submitList(products)
                        }
                    }
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }
    
    private fun searchProducts(query: String) {
        if (query.isNotEmpty()) {
            viewModel.searchProducts(query).observe(this) { products ->
                productAdapter.submitList(products)
            }
        } else {
            viewModel.allProducts.observe(this) { products ->
                productAdapter.submitList(products)
            }
        }
    }
    
    private fun showAddProductDialog() {
        val dialog = AddEditProductDialogFragment.newInstance(
            product = null,
            onProductSaved = { product ->
                saveProduct(product)
            }
        )
        dialog.show(supportFragmentManager, "add_product")
    }
    
    private fun showEditProductDialog(product: Product) {
        val dialog = AddEditProductDialogFragment.newInstance(
            product = product,
            onProductSaved = { updatedProduct ->
                updateProduct(updatedProduct)
            }
        )
        dialog.show(supportFragmentManager, "edit_product")
    }
    
    private fun showDeleteConfirmation(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Produk")
            .setMessage("Apakah Anda yakin ingin menghapus produk ${product.namaBarang}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteProduct(product)
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun saveProduct(product: Product) {
        lifecycleScope.launch {
            try {
                productDao.insertProduct(product)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageProductsActivity, "Produk berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageProductsActivity, "Gagal menambahkan produk: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateProduct(product: Product) {
        lifecycleScope.launch {
            try {
                productDao.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageProductsActivity, "Produk berhasil diupdate", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageProductsActivity, "Gagal mengupdate produk: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun deleteProduct(product: Product) {
        lifecycleScope.launch {
            try {
                productDao.deleteProduct(product)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageProductsActivity, "Produk berhasil dihapus", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageProductsActivity, "Gagal menghapus produk: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
```

## 17. Add/Edit Product Dialog

```kotlin
// ui/dialogs/AddEditProductDialogFragment.kt
class AddEditProductDialogFragment : DialogFragment() {
    
    private var _binding: DialogAddEditProductBinding? = null
    private val binding get() = _binding!!
    
    private var product: Product? = null
    private var onProductSaved: ((Product) -> Unit)? = null
    
    companion object {
        fun newInstance(
            product: Product? = null,
            onProductSaved: (Product) -> Unit
        ): AddEditProductDialogFragment {
            return AddEditProductDialogFragment().apply {
                this.product = product
                this.onProductSaved = onProductSaved
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }
    
    private fun setupUI() {
        val isEdit = product != null
        binding.tvTitle.text = if (isEdit) "Edit Produk" else "Tambah Produk"
        
        // Fill fields if editing
        product?.let { product ->
            binding.etKodeBarang.setText(product.kodeBarang)
            binding.etNamaBarang.setText(product.namaBarang)
            binding.etUnit.setText(product.unit)
            binding.etHargaJual.setText(product.hargaJual.toString())
            binding.etHargaBeli.setText(product.hargaBeli.toString())
            binding.etKodeBarcode.setText(product.kodeBarcode)
            binding.etKategori.setText(product.kategori)
            
            // Disable kode barang edit for existing products
            binding.etKodeBarang.isEnabled = false
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveProduct()
        }
        
        binding.btnScanBarcode.setOnClickListener {
            // Launch barcode scanner for barcode input
            startBarcodeScanner()
        }
    }
    
    private fun saveProduct() {
        val kodeBarang = binding.etKodeBarang.text.toString().trim()
        val namaBarang = binding.etNamaBarang.text.toString().trim()
        val unit = binding.etUnit.text.toString().trim()
        val hargaJualStr = binding.etHargaJual.text.toString().trim()
        val hargaBeliStr = binding.etHargaBeli.text.toString().trim()
        val kodeBarcode = binding.etKodeBarcode.text.toString().trim()
        val kategori = binding.etKategori.text.toString().trim()
        
        // Validation
        if (kodeBarang.isEmpty()) {
            binding.etKodeBarang.error = "Kode barang harus diisi"
            return
        }
        
        if (namaBarang.isEmpty()) {
            binding.etNamaBarang.error = "Nama barang harus diisi"
            return
        }
        
        if (unit.isEmpty()) {
            binding.etUnit.error = "Unit harus diisi"
            return
        }
        
        val hargaJual = try {
            hargaJualStr.toDouble()
        } catch (e: NumberFormatException) {
            binding.etHargaJual.error = "Harga jual tidak valid"
            return
        }
        
        val hargaBeli = try {
            if (hargaBeliStr.isNotEmpty()) hargaBeliStr.toDouble() else 0.0
        } catch (e: NumberFormatException) {
            binding.etHargaBeli.error = "Harga beli tidak valid"
            return
        }
        
        if (kategori.isEmpty()) {
            binding.etKategori.error = "Kategori harus diisi"
            return
        }
        
        val newProduct = if (product != null) {
            // Update existing product
            product!!.copy(
                namaBarang = namaBarang,
                unit = unit,
                hargaJual = hargaJual,
                hargaBeli = hargaBeli,
                kodeBarcode = kodeBarcode.ifEmpty { null },
                kategori = kategori,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            // Create new product
            Product(
                kodeBarang = kodeBarang,
                namaBarang = namaBarang,
                unit = unit,
                hargaJual = hargaJual,
                hargaBeli = hargaBeli,
                kodeBarcode = kodeBarcode.ifEmpty { null },
                kategori = kategori,
                stok = 0 // Initial stock is 0, will be added through stock management
            )
        }
        
        onProductSaved?.invoke(newProduct)
        dismiss()
    }
    
    private fun startBarcodeScanner() {
        val intent = Intent(requireActivity(), BarcodeScannerActivity::class.java)
        barcodeScannerLauncher.launch(intent)
    }
    
    private val barcodeScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val barcode = result.data?.getStringExtra("barcode")
            barcode?.let {
                binding.etKodeBarcode.setText(it)
            }
        }
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
```

## 18. Import Data Activity

```kotlin
// ui/admin/ImportDataActivity.kt
class ImportDataActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityImportDataBinding
    private lateinit var productDao: ProductDao
    private lateinit var stockMovementDao: StockMovementDao
    private var currentUser: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        productDao = AppDatabase.getDatabase(this).productDao()
        stockMovementDao = AppDatabase.getDatabase(this).stockMovementDao()
        
        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        currentUser = prefs.getString("user_id", "") ?: ""
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Import Data"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.btnSelectFile.setOnClickListener {
            selectCSVFile()
        }
        
        binding.btnImport.setOnClickListener {
            if (binding.tvSelectedFile.text != "Belum ada file dipilih") {
                importCSVFile()
            } else {
                Toast.makeText(this, "Pilih file CSV terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnDownloadTemplate.setOnClickListener {
            downloadCSVTemplate()
        }
        
        // Show CSV format example
        showCSVFormat()
    }
    
    private fun showCSVFormat() {
        val formatText = """
            Format CSV yang dibutuhkan:
            
            kode_barang,nama_barang,unit,harga_jual,harga_beli,kode_barcode,kategori,stok_awal
            
            Contoh:
            BRG001,Indomie Goreng,pcs,3000,2500,8992388888888,Makanan,100
            BRG002,Aqua 600ml,pcs,3500,3000,899238999999,Minuman,50
            
            Keterangan:
            - Kode barang harus unik
            - Harga dalam rupiah (tanpa titik/koma)
            - Kode barcode boleh kosong
            - Stok awal akan dicatat sebagai stock in
        """.trimIndent()
        
        binding.tvFormatDescription.text = formatText
    }
    
    private fun selectCSVFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        try {
            filePickerLauncher.launch(intent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "Tidak ada aplikasi file manager yang tersedia", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileName = getFileName(uri)
                binding.tvSelectedFile.text = fileName
                binding.btnImport.isEnabled = true
                selectedFileUri = uri
            }
        }
    }
    
    private var selectedFileUri: Uri? = null
    
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = it.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "Unknown"
    }
    
    private fun importCSVFile() {
        val uri = selectedFileUri ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnImport.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val reader = CSVReader(InputStreamReader(inputStream))
                
                val allLines = reader.readAll()
                reader.close()
                
                if (allLines.isEmpty()) {
                    throw Exception("File CSV kosong")
                }
                
                // Skip header row
                val dataLines = allLines.drop(1)
                
                val products = mutableListOf<Product>()
                val stockMovements = mutableListOf<StockMovement>()
                var successCount = 0
                var errorCount = 0
                val errors = mutableListOf<String>()
                
                dataLines.forEachIndexed { index, line ->
                    try {
                        if (line.size >= 8) {
                            val kodeBarang = line[0].trim()
                            val namaBarang = line[1].trim()
                            val unit = line[2].trim()
                            val hargaJual = line[3].trim().toDouble()
                            val hargaBeli = line[4].trim().toDoubleOrNull() ?: 0.0
                            val kodeBarcode = line[5].trim().ifEmpty { null }
                            val kategori = line[6].trim()
                            val stokAwal = line[7].trim().toIntOrNull() ?: 0
                            
                            if (kodeBarang.isNotEmpty() && namaBarang.isNotEmpty()) {
                                val product = Product(
                                    kodeBarang = kodeBarang,
                                    namaBarang = namaBarang,
                                    unit = unit,
                                    hargaJual = hargaJual,
                                    hargaBeli = hargaBeli,
                                    kodeBarcode = kodeBarcode,
                                    kategori = kategori,
                                    stok = stokAwal
                                )
                                
                                products.add(product)
                                
                                // Create stock movement for initial stock
                                if (stokAwal > 0) {
                                    val stockMovement = StockMovement(
                                        kodeBarang = kodeBarang,
                                        movementType = "IN",
                                        quantity = stokAwal,
                                        hargaBeli = hargaBeli,
                                        notes = "Import data awal",
                                        createdBy = currentUser
                                    )
                                    stockMovements.add(stockMovement)
                                }
                                
                                successCount++
                            } else {
                                errors.add("Baris ${index + 2}: Kode barang dan nama barang harus diisi")
                                errorCount++
                            }
                        } else {
                            errors.add("Baris ${index + 2}: Format tidak lengkap")
                            errorCount++
                        }
                    } catch (e: Exception) {
                        errors.add("Baris ${index + 2}: ${e.message}")
                        errorCount++
                    }
                }
                
                // Insert products to database
                if (products.isNotEmpty()) {
                    productDao.insertProducts(products)
                }
                
                if (stockMovements.isNotEmpty()) {
                    stockMovements.forEach { movement ->
                        stockMovementDao.insertStockMovement(movement)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnImport.isEnabled = true
                    
                    val message = buildString {
                        append("Import selesai!\n")
                        append("Berhasil: $successCount produk\n")
                        append("Gagal: $errorCount produk")
                        
                        if (errors.isNotEmpty()) {
                            append("\n\nError:\n")
                            append(errors.take(5).joinToString("\n"))
                            if (errors.size > 5) {
                                append("\n... dan ${errors.size - 5} error lainnya")
                            }
                        }
                    }
                    
                    AlertDialog.Builder(this@ImportDataActivity)
                        .setTitle("Import Selesai")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                    
                    // Reset file selection
                    binding.tvSelectedFile.text = "Belum ada file dipilih"
                    selectedFileUri = null
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnImport.isEnabled = true
                    
                    Toast.makeText(
                        this@ImportDataActivity,
                        "Import gagal: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun downloadCSVTemplate() {
        lifecycleScope.launch {
            try {
                val templateData = listOf(
                    arrayOf("kode_barang", "nama_barang", "unit", "harga_jual", "harga_beli", "kode_barcode", "kategori", "stok_awal"),
                    arrayOf("BRG001", "Indomie Goreng", "pcs", "3000", "2500", "8992388888888", "Makanan", "100"),
                    arrayOf("BRG002", "Aqua 600ml", "pcs", "3500", "3000", "", "Minuman", "50"),
                    arrayOf("BRG003", "Buku Tulis", "pcs", "2000", "1500", "1234567890123", "ATK", "25")
                )
                
                val fileName = "template_import_${System.currentTimeMillis()}.csv"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                
                val writer = CSVWriter(FileWriter(file))
                templateData.forEach { row ->
                    writer.writeNext(row)
                }
                writer.close()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ImportDataActivity,
                        "Template berhasil disimpan: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Share file
                    val uri = FileProvider.getUriForFile(
                        this@ImportDataActivity,
                        "${packageName}.fileprovider",
                        file
                    )
                    
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    startActivity(Intent.createChooser(shareIntent, "Bagikan Template CSV"))
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ImportDataActivity,
                        "Gagal membuat template: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
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
