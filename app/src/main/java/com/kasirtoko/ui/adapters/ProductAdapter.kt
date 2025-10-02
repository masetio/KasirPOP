class ProductAdapter(
    private val onProductClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                tvProductName.text = product.namaBarang
                tvProductCode.text = product.kodeBarang
                tvPrice.text = formatCurrency(product.hargaJual)
                tvStock.text = "Stok: ${product.stok} ${product.unit}"
                tvCategory.text = product.kategori
                
                // Set stock color based on quantity
                tvStock.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        when {
                            product.stok == 0 -> R.color.error
                            product.stok <= 5 -> android.R.color.holo_orange_dark
                            else -> android.R.color.holo_green_dark
                        }
                    )
                )
                
                root.setOnClickListener {
                    if (product.stok > 0) {
                        onProductClick(product)
                    }
                }
                
                root.alpha = if (product.stok > 0) 1.0f else 0.6f
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
