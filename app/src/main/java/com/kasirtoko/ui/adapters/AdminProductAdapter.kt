class AdminProductAdapter(
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : ListAdapter<Product, AdminProductAdapter.AdminProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminProductViewHolder {
        val binding = ItemAdminProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AdminProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminProductViewHolder(
        private val binding: ItemAdminProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                tvProductName.text = product.namaBarang
                tvProductCode.text = product.kodeBarang
                tvCategory.text = product.kategori
                tvUnit.text = product.unit
                tvPrice.text = formatCurrency(product.hargaJual)
                tvStock.text = product.stok.toString()
                
                // Set stock indicator color
                when {
                    product.stok == 0 -> {
                        stockIndicator.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.error)
                        )
                    }
                    product.stok <= 5 -> {
                        stockIndicator.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
                        )
                    }
                    else -> {
                        stockIndicator.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                        )
                    }
                }
                
                btnEdit.setOnClickListener {
                    onEditClick(product)
                }
                
                btnDelete.setOnClickListener {
                    onDeleteClick(product)
                }
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.kodeBarang == newItem.kodeBarang
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
