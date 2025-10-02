package com.kasirtoko.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kasirtoko.R
import com.kasirtoko.data.entities.Transaction
import com.kasirtoko.databinding.ItemTransactionBinding
import com.kasirtoko.utils.formatCurrency
import com.kasirtoko.utils.formatDateTime

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onPayClick: ((Transaction) -> Unit)? = null,
    private val onPrintClick: ((Transaction) -> Unit)? = null
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                tvTransactionId.text = transaction.id.takeLast(8)
                tvKasirName.text = transaction.kasirName
                tvDateTime.text = transaction.createdAt.formatDateTime()
                tvTotalAmount.text = transaction.totalAmount.formatCurrency()
                
                // Payment method chip
                chipPaymentMethod.text = getPaymentMethodText(transaction.paymentMethod)
                val paymentMethodColor = getPaymentMethodColor(transaction.paymentMethod)
                chipPaymentMethod.setChipBackgroundColorResource(paymentMethodColor)
                
                // Payment status
                if (transaction.paymentStatus == "PAID") {
                    tvStatus.text = "LUNAS"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                    )
                    indicatorStatus.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                    )
                    
                    btnPay.visibility = android.view.View.GONE
                    btnPrint.visibility = if (onPrintClick != null) android.view.View.VISIBLE else android.view.View.GONE
                    
                    transaction.paidAt?.let {
                        tvPaidAt.text = "Dibayar: ${it.formatDateTime()}"
                        tvPaidAt.visibility = android.view.View.VISIBLE
                    }
                    
                } else {
                    tvStatus.text = "BELUM LUNAS"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.error)
                    )
                    indicatorStatus.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.error)
                    )
                    
                    btnPay.visibility = if (onPayClick != null) android.view.View.VISIBLE else android.view.View.GONE
                    btnPrint.visibility = android.view.View.GONE
                    tvPaidAt.visibility = android.view.View.GONE
                }
                
                // Customer name for debt transactions
                if (transaction.paymentMethod == "DEBT" && !transaction.customerName.isNullOrEmpty()) {
                    tvCustomerName.text = "Customer: ${transaction.customerName}"
                    tvCustomerName.visibility = android.view.View.VISIBLE
                } else {
                    tvCustomerName.visibility = android.view.View.GONE
                }
                
                // Cash details for cash transactions
                if (transaction.paymentMethod == "CASH" && transaction.paymentStatus == "PAID") {
                    val cashDetails = buildString {
                        transaction.cashReceived?.let { 
                            append("Bayar: ${it.formatCurrency()}")
                        }
                        transaction.cashChange?.let {
                            if (isNotEmpty()) append(" | ")
                            append("Kembali: ${it.formatCurrency()}")
                        }
                    }
                    if (cashDetails.isNotEmpty()) {
                        tvCashDetails.text = cashDetails
                        tvCashDetails.visibility = android.view.View.VISIBLE
                    } else {
                        tvCashDetails.visibility = android.view.View.GONE
                    }
                } else {
                    tvCashDetails.visibility = android.view.View.GONE
                }
                
                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
                
                btnPay.setOnClickListener {
                    onPayClick?.invoke(transaction)
                }
                
                btnPrint.setOnClickListener {
                    onPrintClick?.invoke(transaction)
                }
            }
        }
        
        private fun getPaymentMethodText(method: String): String {
            return when (method) {
                "CASH" -> "TUNAI"
                "QRIS" -> "QRIS"
                "DEBT" -> "UTANG"
                else -> method
            }
        }
        
        private fun getPaymentMethodColor(method: String): Int {
            return when (method) {
                "CASH" -> android.R.color.holo_green_light
                "QRIS" -> android.R.color.holo_blue_light
                "DEBT" -> android.R.color.holo_orange_light
                else -> R.color.gray_400
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
