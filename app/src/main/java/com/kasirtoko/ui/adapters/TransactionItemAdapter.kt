package com.kasirtoko.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kasirtoko.data.entities.TransactionItem
import com.kasirtoko.databinding.ItemTransactionItemBinding
import com.kasirtoko.utils.formatCurrency

class TransactionItemAdapter : ListAdapter<TransactionItem, TransactionItemAdapter.TransactionItemViewHolder>(
    TransactionItemDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionItemViewHolder {
        val binding = ItemTransactionItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionItemViewHolder(
        private val binding: ItemTransactionItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionItem) {
            binding.apply {
                tvProductName.text = item.namaBarang
                tvProductCode.text = item.kodeBarang
                tvQuantity.text = "${item.quantity} x"
                tvUnitPrice.text = item.hargaSatuan.formatCurrency()
                tvSubtotal.text = item.subtotal.formatCurrency()
            }
        }
    }

    class TransactionItemDiffCallback : DiffUtil.ItemCallback<TransactionItem>() {
        override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
            return oldItem == newItem
        }
    }
}
