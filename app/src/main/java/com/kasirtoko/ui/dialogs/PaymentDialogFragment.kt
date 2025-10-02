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
