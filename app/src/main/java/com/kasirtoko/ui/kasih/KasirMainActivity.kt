class KasirMainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityKasirMainBinding
    private lateinit var productAdapter: ProductAdapter
    private lateinit var cartAdapter: CartAdapter
    private lateinit var productDao: ProductDao
    private lateinit var transactionDao: TransactionDao
    
    private val cartItems = mutableListOf<CartItem>()
    private var currentUser: String = ""
    private var currentUserName: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKasirMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get user session
        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        currentUser = prefs.getString("user_id", "") ?: ""
        currentUserName = prefs.getString("full_name", "") ?: ""
        
        productDao = AppDatabase.getDatabase(this).productDao()
        transactionDao = AppDatabase.getDatabase(this).transactionDao()
        
        setupUI()
        setupRecyclerViews()
        observeData()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Kasir - $currentUserName"
        
        binding.btnScanBarcode.setOnClickListener {
            startBarcodeScanner()
        }
        
        binding.btnCheckout.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                showPaymentDialog()
            } else {
                Toast.makeText(this, "Keranjang masih kosong", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnClearCart.setOnClickListener {
            clearCart()
        }
        
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchProducts(s.toString())
            }
        })
    }
    
    private fun setupRecyclerViews() {
        productAdapter = ProductAdapter { product ->
            addToCart(product)
        }
        
        cartAdapter = CartAdapter(
            onQuantityChanged = { item, newQuantity ->
                updateCartItemQuantity(item, newQuantity)
            },
            onRemoveItem = { item ->
                removeFromCart(item)
            }
        )
        
        binding.rvProducts.adapter = productAdapter
        binding.rvCart.adapter = cartAdapter
    }
    
    private fun observeData() {
        productDao.getAllProducts().observe(this) { products ->
            productAdapter.submitList(products.filter { it.stok > 0 })
        }
    }
    
    private fun searchProducts(query: String) {
        if (query.isNotEmpty()) {
            productDao.searchProducts(query).observe(this) { products ->
                productAdapter.submitList(products.filter { it.stok > 0 })
            }
        } else {
            productDao.getAllProducts().observe(this) { products ->
                productAdapter.submitList(products.filter { it.stok > 0 })
            }
        }
    }
    
    private fun addToCart(product: Product) {
        val existingItem = cartItems.find { it.product.kodeBarang == product.kodeBarang }
        
        if (existingItem != null) {
            if (existingItem.quantity < product.stok) {
                existingItem.quantity++
                updateCartDisplay()
            } else {
                Toast.makeText(this, "Stok tidak mencukupi", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (product.stok > 0) {
                cartItems.add(CartItem(product, 1))
                updateCartDisplay()
            } else {
                Toast.makeText(this, "Stok habis", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateCartItemQuantity(item: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            cartItems.remove(item)
        } else if (newQuantity <= item.product.stok) {
            item.quantity = newQuantity
        } else {
            Toast.makeText(this, "Stok tidak mencukupi", Toast.LENGTH_SHORT).show()
            return
        }
        updateCartDisplay()
    }
    
    private fun removeFromCart(item: CartItem) {
        cartItems.remove(item)
        updateCartDisplay()
    }
    
    private fun updateCartDisplay() {
        cartAdapter.submitList(cartItems.toList())
        
        val total = cartItems.sumOf { it.product.hargaJual * it.quantity }
        binding.tvTotal.text = "Total: ${formatCurrency(total)}"
        
        binding.btnCheckout.isEnabled = cartItems.isNotEmpty()
    }
    
    private fun clearCart() {
        cartItems.clear()
        updateCartDisplay()
    }
    
    private fun showPaymentDialog() {
        val total = cartItems.sumOf { it.product.hargaJual * it.quantity }
        
        val dialog = PaymentDialogFragment.newInstance(total) { paymentMethod, cashReceived ->
            processPayment(paymentMethod, cashReceived)
        }
        
        dialog.show(supportFragmentManager, "payment_dialog")
    }
    
    private fun processPayment(paymentMethod: String, cashReceived: Double? = null) {
        lifecycleScope.launch {
            try {
                val total = cartItems.sumOf { it.product.hargaJual * it.quantity }
                val cashChange = if (paymentMethod == "CASH" && cashReceived != null) {
                    cashReceived - total
                } else null
                
                // Create transaction
                val transaction = Transaction(
                    kasirId = currentUser,
                    kasirName = currentUserName,
                    totalAmount = total,
                    paymentMethod = paymentMethod,
                    paymentStatus = if (paymentMethod == "DEBT") "UNPAID" else "PAID",
                    cashReceived = cashReceived,
                    cashChange = cashChange,
                    paidAt = if (paymentMethod != "DEBT") System.currentTimeMillis() else null
                )
                
                val transactionId = transactionDao.insertTransaction(transaction)
                
                // Create transaction items
                val transactionItems = cartItems.map {
                    TransactionItem(
                        transactionId = transaction.id,
                        kodeBarang = it.product.kodeBarang,
                        namaBarang = it.product.namaBarang,
                        quantity = it.quantity,
                        hargaSatuan = it.product.hargaJual,
                        subtotal = it.product.hargaJual * it.quantity
                    )
                }
                
                transactionDao.insertTransactionItems(transactionItems)
                
                // Update stock
                cartItems.forEach { cartItem ->
                    productDao.reduceStock(cartItem.product.kodeBarang, cartItem.quantity)
                    
                    // Record stock movement
                    val stockMovement = StockMovement(
                        kodeBarang = cartItem.product.kodeBarang,
                        movementType = "OUT",
                        quantity = cartItem.quantity,
                        referenceId = transaction.id,
                        notes = "Penjualan",
                        createdBy = currentUser
                    )
                    // Insert stock movement (implement StockMovementDao)
                }
                
                // Print receipt
                printReceipt(transaction.copy(id = transactionId.toString()), transactionItems)
                
                // Clear cart
                clearCart()
                
                Toast.makeText(this@KasirMainActivity, "Transaksi berhasil", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(this@KasirMainActivity, "Gagal memproses transaksi: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun startBarcodeScanner() {
        // Implement barcode scanner using ML Kit
        val intent = Intent(this, BarcodeScannerActivity::class.java)
        barcodeScannerLauncher.launch(intent)
    }
    
    private val barcodeScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val barcode = result.data?.getStringExtra("barcode")
            barcode?.let { searchProductByBarcode(it) }
        }
    }
    
    private fun searchProductByBarcode(barcode: String) {
        lifecycleScope.launch {
            val product = productDao.getProductByBarcode(barcode)
            if (product != null) {
                addToCart(product)
            } else {
                Toast.makeText(this@KasirMainActivity, "Produk tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun printReceipt(transaction: Transaction, items: List<TransactionItem>) {
        // Implement thermal printer functionality
        val printerHelper = ThermalPrinterHelper(this)
        printerHelper.printReceipt(transaction, items)
    }
    
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }
}

// Data class for cart items
data class CartItem(
    val product: Product,
    var quantity: Int
)
