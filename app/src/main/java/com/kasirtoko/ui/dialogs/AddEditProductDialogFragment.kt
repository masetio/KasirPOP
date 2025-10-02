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
