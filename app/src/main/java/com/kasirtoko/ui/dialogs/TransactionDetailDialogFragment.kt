package com.kasirtoko.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasirtoko.data.entities.Transaction
import com.kasirtoko.data.entities.TransactionItem
import com.kasirtoko.databinding.DialogTransactionDetailBinding
import com.kasirtoko.ui.adapters.TransactionItemAdapter
import com.kasirtoko.utils.formatCurrency
import com.kasirtoko.utils.formatDateTime

class TransactionDetailDialogFragment : DialogFragment() {
    
    private var _binding: DialogTransactionDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var transaction: Transaction
    private lateinit var items: List<TransactionItem>
    private lateinit var itemAdapter: TransactionItemAdapter
    
    companion object {
        fun newInstance(
            transaction: Transaction,
            items: List<TransactionItem>
        ): TransactionDetailDialogFragment {
            return TransactionDetailDialogFragment().apply {
                this.transaction = transaction
                this.items = items
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransactionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
    }
    
    private fun setupUI() {
        binding.apply {
            // Transaction header info
            tvTransactionId.text = transaction.id
            tvDateTime.text = transaction.createdAt.formatDateTime()
            tvKasirName.text = transaction.kasirName
            tvPaymentMethod.text = getPaymentMethodText(transaction.paymentMethod)
            tvPaymentStatus.text = if (transaction.paymentStatus == "PAID") "LUNAS" else "BELUM LUNAS"
            
            // Payment details
            when (transaction.paymentMethod) {
                "CASH" -> {
                    layoutCashDetails.visibility = View.VISIBLE
                    tvCashReceived.text = transaction.cashReceived?.formatCurrency() ?: "Rp 0"
                    tvCashChange.text = transaction.cashChange?.formatCurrency() ?: "Rp 0"
                }
                "DEBT" -> {
                    layoutDebtDetails.visibility = View.VISIBLE
                    tvCustomerName.text = transaction.customerName ?: "-"
                }
            }
            
            // Notes
            if (!transaction.notes.isNullOrEmpty()) {
                layoutNotes.visibility = View.VISIBLE
                tvNotes.text = transaction.notes
            }
            
            // Paid at
            if (transaction.paymentStatus == "PAID" && transaction.paidAt != null) {
                layoutPaidAt.visibility = View.VISIBLE
                tvPaidAt.text = transaction.paidAt!!.formatDateTime()
            }
            
            // Total
            val subtotal = items.sumOf { it.subtotal }
            tvSubtotal.text = subtotal.formatCurrency()
            tvTotal.text = transaction.totalAmount.formatCurrency()
            
            btnClose.setOnClickListener {
                dismiss()
            }
        }
    }
    
    private fun setupRecyclerView() {
        itemAdapter = TransactionItemAdapter()
        binding.rvItems.apply {
            adapter = itemAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        
        itemAdapter.submitList(items)
    }
    
    private fun getPaymentMethodText(method: String): String {
        return when (method) {
            "CASH" -> "Tunai"
            "QRIS" -> "QRIS"
            "DEBT" -> "Utang"
            else -> method
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
