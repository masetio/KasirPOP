class PrinterAdapter(
    private val onPrinterClick: (ThermalPrinterHelper.PrinterDevice) -> Unit,
    private val onTestClick: (ThermalPrinterHelper.PrinterDevice) -> Unit
) : ListAdapter<ThermalPrinterHelper.PrinterDevice, PrinterAdapter.PrinterViewHolder>(PrinterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrinterViewHolder {
        val binding = ItemPrinterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PrinterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrinterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PrinterViewHolder(
        private val binding: ItemPrinterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(printer: ThermalPrinterHelper.PrinterDevice) {
            binding.apply {
                tvPrinterName.text = printer.name
                tvPrinterAddress.text = printer.address
                
                // Show connection status
                if (printer.isConnected) {
                    tvConnectionStatus.text = "Terhubung"
                    tvConnectionStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                    )
                    indicatorConnection.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                    )
                } else {
                    tvConnectionStatus.text = "Tidak terhubung"
                    tvConnectionStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.gray_600)
                    )
                    indicatorConnection.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.gray_400)
                    )
                }
                
                // Set printer icon based on name
                if (isPotentialPrinter(printer.name)) {
                    ivPrinterIcon.setImageResource(R.drawable.ic_printer)
                    ivPrinterIcon.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.primary)
                    )
                } else {
                    ivPrinterIcon.setImageResource(R.drawable.ic_bluetooth)
                    ivPrinterIcon.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.gray_600)
                    )
                }
                
                root.setOnClickListener {
                    onPrinterClick(printer)
                }
                
                btnTestPrint.setOnClickListener {
                    onTestClick(printer)
                }
            }
        }
        
        private fun isPotentialPrinter(deviceName: String): Boolean {
            val printerKeywords = listOf(
                "printer", "pos", "receipt", "thermal"
            )
            return printerKeywords.any { keyword ->
                deviceName.contains(keyword, ignoreCase = true)
            }
        }
    }

    class PrinterDiffCallback : DiffUtil.ItemCallback<ThermalPrinterHelper.PrinterDevice>() {
        override fun areItemsTheSame(oldItem: ThermalPrinterHelper.PrinterDevice, newItem: ThermalPrinterHelper.PrinterDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: ThermalPrinterHelper.PrinterDevice, newItem: ThermalPrinterHelper.PrinterDevice): Boolean {
            return oldItem == newItem
        }
    }
}
