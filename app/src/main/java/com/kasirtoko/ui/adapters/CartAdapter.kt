class CartAdapter(
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onRemoveItem: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItem) {
            binding.apply {
                tvProductName.text = cartItem.product.namaBarang
                tvPrice.text = formatCurrency(cartItem.product.hargaJual)
                tvQuantity.text = cartItem.quantity.toString()
                tvSubtotal.text = formatCurrency(cartItem.product.hargaJual * cartItem.quantity)
                tvMaxStock.text = "Max: ${cartItem.product.stok}"
                
                btnDecrease.setOnClickListener {
                    if (cartItem.quantity > 1) {
                        onQuantityChanged(cartItem, cartItem.quantity - 1)
                    }
                }
                
                btnIncrease.setOnClickListener {
                    if (cartItem.quantity < cartItem.product.stok) {
                        onQuantityChanged(cartItem, cartItem.quantity + 1)
                    }
                }
                
                btnRemove.setOnClickListener {
                    onRemoveItem(cartItem)
                }
                
                // Disable increase button if max stock reached
                btnIncrease.isEnabled = cartItem.quantity < cartItem.product.stok
                btnDecrease.isEnabled = cartItem.quantity > 1
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.product.kodeBarang == newItem.product.kodeBarang
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
