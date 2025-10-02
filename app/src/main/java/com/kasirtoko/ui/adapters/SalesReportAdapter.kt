class SalesReportAdapter : ListAdapter<SalesReportItem, SalesReportAdapter.SalesReportViewHolder>(SalesReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesReportViewHolder {
        val binding = ItemSalesReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SalesReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SalesReportViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class SalesReportViewHolder(
        private val binding: ItemSalesReportBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SalesReportItem, rank: Int) {
            binding.apply {
                tvRank.text = rank.toString()
                tvProductCode.text = item.kodeBarang
                tvProductName.text = item.namaBarang
                tvQuantity.text = item.totalQuantity.toString()
                tvAmount.text = formatCurrency(item.totalAmount)
                tvTransactionCount.text = item.transactionCount.toString()
                
                // Highlight top 3
                when (rank) {
                    1 -> cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.gold_light)
                    )
                    2 -> cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.silver_light)
                    )
                    3 -> cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.bronze_light)
                    )
                    else -> cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.surface)
                    )
                }
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    class SalesReportDiffCallback : DiffUtil.ItemCallback<SalesReportItem>() {
        override fun areItemsTheSame(oldItem: SalesReportItem, newItem: SalesReportItem): Boolean {
            return oldItem.kodeBarang == newItem.kodeBarang
        }

        override fun areContentsTheSame(oldItem: SalesReportItem, newItem: SalesReportItem): Boolean {
            return oldItem == newItem
        }
    }
}
