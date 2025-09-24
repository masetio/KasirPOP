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
