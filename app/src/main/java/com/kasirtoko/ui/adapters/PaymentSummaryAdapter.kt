package com.kasirtoko.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kasirtoko.data.models.PaymentSummary
import com.kasirtoko.databinding.ItemPaymentSummaryBinding
import com.kasirtoko.utils.formatCurrency

class PaymentSummaryAdapter : ListAdapter<PaymentSummary, PaymentSummaryAdapter.PaymentSummaryViewHolder>(
    PaymentSummaryDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentSummaryViewHolder {
        val binding = ItemPaymentSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PaymentSummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentSummaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaymentSummaryViewHolder(
        private val binding: ItemPaymentSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: PaymentSummary) {
            binding.apply {
                tvPaymentMethod.text = getPaymentMethodName(summary.method)
                tvTransactionCount.text = "${summary.count} transaksi"
                tvAmount.text = summary.amount.formatCurrency()
                
                // Set icon based on payment method
                val iconRes = when (summary.method) {
                    "CASH" -> com.kasirtoko.R.drawable.ic_cash
                    "QRIS" -> com.kasirtoko.R.drawable.ic_qris
                    "DEBT" -> com.kasirtoko.R.drawable.ic_debt
                    else -> com.kasirtoko.R.drawable.ic_cash
                }
                ivPaymentIcon.setImageResource(iconRes)
            }
        }
        
        private fun getPaymentMethodName(method: String): String {
            return when (method) {
                "CASH" -> "Tunai"
                "QRIS" -> "QRIS"
                "DEBT" -> "Utang"
                else -> method
            }
        }
    }

    class PaymentSummaryDiffCallback : DiffUtil.ItemCallback<PaymentSummary>() {
        override fun areItemsTheSame(oldItem: PaymentSummary, newItem: PaymentSummary): Boolean {
            return oldItem.method == newItem.method
        }

        override fun areContentsTheSame(oldItem: PaymentSummary, newItem: PaymentSummary): Boolean {
            return oldItem == newItem
        }
    }
}
