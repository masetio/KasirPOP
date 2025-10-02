package com.kasirtoko.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kasirtoko.R
import com.kasirtoko.data.entities.StockMovement
import com.kasirtoko.databinding.ItemStockMovementBinding
import com.kasirtoko.utils.formatCurrency
import com.kasirtoko.utils.formatDateTime

class StockMovementAdapter : ListAdapter<StockMovement, StockMovementAdapter.StockMovementViewHolder>(StockMovementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockMovementViewHolder {
        val binding = ItemStockMovementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StockMovementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockMovementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StockMovementViewHolder(
        private val binding: ItemStockMovementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movement: StockMovement) {
            binding.apply {
                tvDateTime.text = movement.createdAt.formatDateTime()
                tvQuantity.text = "${if (movement.movementType == "IN") "+" else "-"}${movement.quantity}"
                tvMovementType.text = if (movement.movementType == "IN") "MASUK" else "KELUAR"
                tvCreatedBy.text = "Oleh: ${movement.createdBy}"
                
                // Set movement type color and icon
                if (movement.movementType == "IN") {
                    tvQuantity.setTextColor(
                        ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                    )
                    ivMovementIcon.setImageResource(R.drawable.ic_arrow_upward)
                    ivMovementIcon.setColorFilter(
                        ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                    )
                    chipMovementType.setChipBackgroundColorResource(android.R.color.holo_green_light)
                } else {
                    tvQuantity.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.error)
                    )
                    ivMovementIcon.setImageResource(R.drawable.ic_arrow_downward)
                    ivMovementIcon.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.error)
                    )
                    chipMovementType.setChipBackgroundColorResource(android.R.color.holo_red_light)
                }
                
                chipMovementType.text = tvMovementType.text
                
                // Purchase price for stock in
                if (movement.movementType == "IN" && movement.hargaBeli != null && movement.hargaBeli > 0) {
                    tvPurchasePrice.text = "Harga beli: ${movement.hargaBeli.formatCurrency()}"
                    tvPurchasePrice.visibility = android.view.View.VISIBLE
                } else {
                    tvPurchasePrice.visibility = android.view.View.GONE
                }
                
                // Notes
                if (!movement.notes.isNullOrEmpty()) {
                    tvNotes.text = movement.notes
                    tvNotes.visibility = android.view.View.VISIBLE
                } else {
                    tvNotes.visibility = android.view.View.GONE
                }
                
                // Reference ID for sales
                if (movement.movementType == "OUT" && !movement.referenceId.isNullOrEmpty()) {
                    tvReference.text = "Ref: ${movement.referenceId.takeLast(8)}"
                    tvReference.visibility = android.view.View.VISIBLE
                } else {
                    tvReference.visibility = android.view.View.GONE
                }
            }
        }
    }

    class StockMovementDiffCallback : DiffUtil.ItemCallback<StockMovement>() {
        override fun areItemsTheSame(oldItem: StockMovement, newItem: StockMovement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StockMovement, newItem: StockMovement): Boolean {
            return oldItem == newItem
        }
    }
}
