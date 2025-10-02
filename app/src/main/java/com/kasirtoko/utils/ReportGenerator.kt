class ReportGenerator(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    suspend fun generateSalesReportCSV(
        startDate: Long,
        endDate: Long,
        categoryFilter: String? = null,
        kasirFilter: String? = null
    ): File {
        val transactions = getFilteredTransactions(startDate, endDate, kasirFilter)
        val transactionItems = mutableListOf<TransactionItem>()
        
        transactions.forEach { transaction ->
            val items = database.transactionDao().getTransactionItems(transaction.id)
            transactionItems.addAll(items)
        }
        
        // Filter by category if specified
        val filteredItems = if (categoryFilter != null) {
            transactionItems.filter { item ->
                val product = database.productDao().getProductByKode(item.kodeBarang)
                product?.kategori == categoryFilter
            }
        } else {
            transactionItems
        }
        
        // Group by product and calculate totals
        val productSales = filteredItems
            .groupBy { it.kodeBarang }
            .map { (kodeBarang, items) ->
                val product = database.productDao().getProductByKode(kodeBarang)
                SalesReportItem(
                    kodeBarang = kodeBarang,
                    namaBarang = items.first().namaBarang,
                    kategori = product?.kategori ?: "",
                    totalQuantity = items.sumOf { it.quantity },
                    totalAmount = items.sumOf { it.subtotal },
                    transactionCount = items.size,
                    avgPrice = items.sumOf { it.hargaSatuan } / items.size
                )
            }
            .sortedByDescending { it.totalAmount }
        
        val fileName = "laporan_penjualan_${dateFormat.format(Date(startDate))}_${dateFormat.format(Date(endDate))}.csv"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        val writer = CSVWriter(FileWriter(file))
        
        // Header information
        writer.writeNext(arrayOf("LAPORAN PENJUALAN"))
        writer.writeNext(arrayOf("Periode", "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"))
        if (categoryFilter != null) {
            writer.writeNext(arrayOf("Kategori", categoryFilter))
        }
        if (kasirFilter != null) {
            val kasir = database.userDao().getUserById(kasirFilter)
            writer.writeNext(arrayOf("Kasir", kasir?.fullName ?: kasirFilter))
        }
        writer.writeNext(arrayOf("Dibuat", dateTimeFormat.format(Date())))
        writer.writeNext(arrayOf("")) // Empty row
        
        // Summary
        val totalTransactions = transactions.size
        val totalAmount = transactions.sumOf { it.totalAmount }
        val totalItems = filteredItems.sumOf { it.quantity }
        
        writer.writeNext(arrayOf("RINGKASAN"))
        writer.writeNext(arrayOf("Total Transaksi", totalTransactions.toString()))
        writer.writeNext(arrayOf("Total Barang Terjual", totalItems.toString()))
        writer.writeNext(arrayOf("Total Penjualan", totalAmount.toString()))
        writer.writeNext(arrayOf("")) // Empty row
        
        // Detail header
        writer.writeNext(arrayOf(
            "Ranking", "Kode Barang", "Nama Barang", "Kategori",
            "Jumlah Terjual", "Harga Rata-rata", "Total Penjualan", "Jumlah Transaksi"
        ))
        
        // Detail data
        productSales.forEachIndexed { index, item ->
            writer.writeNext(arrayOf(
                (index + 1).toString(),
                item.kodeBarang,
                item.namaBarang,
                item.kategori,
                item.totalQuantity.toString(),
                item.avgPrice.toString(),
                item.totalAmount.toString(),
                item.transactionCount.toString()
            ))
        }
        
        writer.close()
        return file
    }
    
    suspend fun generateSalesReportPDF(
        startDate: Long,
        endDate: Long,
        categoryFilter: String? = null,
        kasirFilter: String? = null
    ): File {
        val transactions = getFilteredTransactions(startDate, endDate, kasirFilter)
        val transactionItems = mutableListOf<TransactionItem>()
        
        transactions.forEach { transaction ->
            val items = database.transactionDao().getTransactionItems(transaction.id)
            transactionItems.addAll(items)
        }
        
        // Filter by category if specified
        val filteredItems = if (categoryFilter != null) {
            transactionItems.filter { item ->
                val product = database.productDao().getProductByKode(item.kodeBarang)
                product?.kategori == categoryFilter
            }
        } else {
            transactionItems
        }
        
        // Group by product and calculate totals
        val productSales = filteredItems
            .groupBy { it.kodeBarang }
            .map { (kodeBarang, items) ->
                val product = database.productDao().getProductByKode(kodeBarang)
                SalesReportItem(
                    kodeBarang = kodeBarang,
                    namaBarang = items.first().namaBarang,
                    kategori = product?.kategori ?: "",
                    totalQuantity = items.sumOf { it.quantity },
                    totalAmount = items.sumOf { it.subtotal },
                    transactionCount = items.size,
                    avgPrice = items.sumOf { it.hargaSatuan } / items.size
                )
            }
            .sortedByDescending { it.totalAmount }
        
        val fileName = "laporan_penjualan_${dateFormat.format(Date(startDate))}_${dateFormat.format(Date(endDate))}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        val document = Document(PageSize.A4)
        PdfWriter.getInstance(document, FileOutputStream(file))
        
        document.open()
        
        // Get shop info
        val shopName = database.appSettingDao().getSetting("shop_name")?.value ?: "TOKO SAYA"
        
        // Title and header
        val titleFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD)
        val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f)
        val smallFont = Font(Font.FontFamily.HELVETICA, 8f)
        
        // Shop name and title
        val title = Paragraph(shopName, titleFont)
        title.alignment = Element.ALIGN_CENTER
        document.add(title)
        
        val reportTitle = Paragraph("LAPORAN PENJUALAN", headerFont)
        reportTitle.alignment = Element.ALIGN_CENTER
        document.add(reportTitle)
        
        document.add(Paragraph(" ")) // Space
        
        // Report info
        val infoTable = PdfPTable(2)
        infoTable.widthPercentage = 100f
        infoTable.setWidths(floatArrayOf(1f, 2f))
        
        infoTable.addCell(PdfPCell(Phrase("Periode", normalFont)).apply { border = Rectangle.NO_BORDER })
        infoTable.addCell(PdfPCell(Phrase("${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}", normalFont)).apply { border = Rectangle.NO_BORDER })
        
        if (categoryFilter != null) {
            infoTable.addCell(PdfPCell(Phrase("Kategori", normalFont)).apply { border = Rectangle.NO_BORDER })
            infoTable.addCell(PdfPCell(Phrase(categoryFilter, normalFont)).apply { border = Rectangle.NO_BORDER })
        }
        
        if (kasirFilter != null) {
            val kasir = database.userDao().getUserById(kasirFilter)
            infoTable.addCell(PdfPCell(Phrase("Kasir", normalFont)).apply { border = Rectangle.NO_BORDER })
            infoTable.addCell(PdfPCell(Phrase(kasir?.fullName ?: kasirFilter, normalFont)).apply { border = Rectangle.NO_BORDER })
        }
        
        infoTable.addCell(PdfPCell(Phrase("Dibuat", normalFont)).apply { border = Rectangle.NO_BORDER })
        infoTable.addCell(PdfPCell(Phrase(dateTimeFormat.format(Date()), normalFont)).apply { border = Rectangle.NO_BORDER })
        
        document.add(infoTable)
        document.add(Paragraph(" ")) // Space
        
        // Summary
        val totalTransactions = transactions.size
        val totalAmount = transactions.sumOf { it.totalAmount }
        val totalItems = filteredItems.sumOf { it.quantity }
        
        val summaryTitle = Paragraph("RINGKASAN", headerFont)
        document.add(summaryTitle)
        
        val summaryTable = PdfPTable(2)
        summaryTable.widthPercentage = 70f
        summaryTable.setWidths(floatArrayOf(2f, 1f))
        
        addSummaryRow(summaryTable, "Total Transaksi", totalTransactions.toString(), normalFont)
        addSummaryRow(summaryTable, "Total Barang Terjual", totalItems.toString(), normalFont)
        addSummaryRow(summaryTable, "Total Penjualan", formatCurrency(totalAmount), normalFont)
        
        if (totalTransactions > 0) {
            addSummaryRow(summaryTable, "Rata-rata per Transaksi", formatCurrency(totalAmount / totalTransactions), normalFont)
        }
        
        document.add(summaryTable)
        document.add(Paragraph(" ")) // Space
        
        // Detail table
        if (productSales.isNotEmpty()) {
            val detailTitle = Paragraph("DETAIL PENJUALAN", headerFont)
            document.add(detailTitle)
            
            val detailTable = PdfPTable(7)
            detailTable.widthPercentage = 100f
            detailTable.setWidths(floatArrayOf(0.5f, 1f, 2.5f, 1f, 1f, 1.5f, 0.8f))
            
            // Header row
            addTableHeader(detailTable, arrayOf(
                "No", "Kode", "Nama Barang", "Kategori", "Qty", "Total", "Trx"
            ), smallFont)
            
            // Data rows
            productSales.forEachIndexed { index, item ->
                detailTable.addCell(PdfPCell(Phrase((index + 1).toString(), smallFont)))
                detailTable.addCell(PdfPCell(Phrase(item.kodeBarang, smallFont)))
                detailTable.addCell(PdfPCell(Phrase(item.namaBarang, smallFont)))
                detailTable.addCell(PdfPCell(Phrase(item.kategori, smallFont)))
                detailTable.addCell(PdfPCell(Phrase(item.totalQuantity.toString(), smallFont)))
                detailTable.addCell(PdfPCell(Phrase(formatCurrency(item.totalAmount), smallFont)))
                detailTable.addCell(PdfPCell(Phrase(item.transactionCount.toString(), smallFont)))
            }
            
            document.add(detailTable)
        }
        
        // Footer
        document.add(Paragraph(" "))
        val footer = Paragraph(
            "Laporan dibuat secara otomatis pada ${dateTimeFormat.format(Date())}",
            Font(Font.FontFamily.HELVETICA, 8f, Font.ITALIC)
        )
        footer.alignment = Element.ALIGN_RIGHT
        document.add(footer)
        
        document.close()
        return file
    }
    
    suspend fun generateDailyReportPDF(targetDate: Long): File {
        val startOfDay = getStartOfDay(targetDate)
        val endOfDay = getEndOfDay(targetDate)
        
        val transactions = database.transactionDao().getTransactionsByDateRangeSync(startOfDay, endOfDay)
        val paidTransactions = transactions.filter { it.paymentStatus == "PAID" }
        val unpaidTransactions = transactions.filter { it.paymentStatus == "UNPAID" }
        
        // Group by payment method
        val paymentSummary = paidTransactions.groupBy { it.paymentMethod }
            .map { (method, txns) ->
                PaymentSummary(
                    method = method,
                    count = txns.size,
                    amount = txns.sumOf { it.totalAmount }
                )
            }
        
        // Group by kasir
        val kasirSummary = transactions.groupBy { it.kasirId }
            .map { (kasirId, txns) ->
                KasirSummary(
                    kasirId = kasirId,
                    kasirName = txns.first().kasirName,
                    transactionCount = txns.size,
                    totalAmount = txns.sumOf { it.totalAmount },
                    paidAmount = txns.filter { it.paymentStatus == "PAID" }.sumOf { it.totalAmount }
                )
            }
            .sortedByDescending { it.totalAmount }
        
        val fileName = "laporan_harian_${dateFormat.format(Date(targetDate))}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        val document = Document(PageSize.A4)
        PdfWriter.getInstance(document, FileOutputStream(file))
        
        document.open()
        
        val shopName = database.appSettingDao().getSetting("shop_name")?.value ?: "TOKO SAYA"
        
        val titleFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD)
        val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
        val normalFont = Font(Font.FontFamily.HELVETICA, 10f)
        val smallFont = Font(Font.FontFamily.HELVETICA, 8f)
        
        // Title
        val title = Paragraph(shopName, titleFont)
        title.alignment = Element.ALIGN_CENTER
        document.add(title)
        
        val reportTitle = Paragraph("LAPORAN HARIAN", headerFont)
        reportTitle.alignment = Element.ALIGN_CENTER
        document.add(reportTitle)
        
        val dateTitle = Paragraph(dateFormat.format(Date(targetDate)), headerFont)
        dateTitle.alignment = Element.ALIGN_CENTER
        document.add(dateTitle)
        
        document.add(Paragraph(" "))
        
        // Overall summary
        val overallSummary = Paragraph("RINGKASAN UMUM", headerFont)
        document.add(overallSummary)
        
        val overallTable = PdfPTable(2)
        overallTable.widthPercentage = 70f
        overallTable.setWidths(floatArrayOf(2f, 1f))
        
        addSummaryRow(overallTable, "Total Transaksi", transactions.size.toString(), normalFont)
        addSummaryRow(overallTable, "Transaksi Lunas", paidTransactions.size.toString(), normalFont)
        addSummaryRow(overallTable, "Transaksi Belum Lunas", unpaidTransactions.size.toString(), normalFont)
        addSummaryRow(overallTable, "Total Penjualan", formatCurrency(transactions.sumOf { it.totalAmount }), normalFont)
        addSummaryRow(overallTable, "Total Terbayar", formatCurrency(paidTransactions.sumOf { it.totalAmount }), normalFont)
        addSummaryRow(overallTable, "Total Piutang", formatCurrency(unpaidTransactions.sumOf { it.totalAmount }), normalFont)
        
        document.add(overallTable)
        document.add(Paragraph(" "))
        
        // Payment method summary
        if (paymentSummary.isNotEmpty()) {
            val paymentTitle = Paragraph("RINGKASAN PEMBAYARAN", headerFont)
            document.add(paymentTitle)
            
            val paymentTable = PdfPTable(3)
            paymentTable.widthPercentage = 70f
            paymentTable.setWidths(floatArrayOf(2f, 1f, 1.5f))
            
            addTableHeader(paymentTable, arrayOf("Metode", "Jumlah", "Total"), normalFont)
            
            paymentSummary.forEach { payment ->
                paymentTable.addCell(PdfPCell(Phrase(getPaymentMethodName(payment.method), normalFont)))
                paymentTable.addCell(PdfPCell(Phrase(payment.count.toString(), normalFont)))
                paymentTable.addCell(PdfPCell(Phrase(formatCurrency(payment.amount), normalFont)))
            }
            
            document.add(paymentTable)
            document.add(Paragraph(" "))
        }
        
        // Kasir summary
        if (kasirSummary.isNotEmpty()) {
            val kasirTitle = Paragraph("RINGKASAN PER KASIR", headerFont)
            document.add(kasirTitle)
            
            val kasirTable = PdfPTable(4)
            kasirTable.widthPercentage = 100f
            kasirTable.setWidths(floatArrayOf(2f, 1f, 1.5f, 1.5f))
            
            addTableHeader(kasirTable, arrayOf("Nama Kasir", "Transaksi", "Total", "Terbayar"), normalFont)
            
            kasirSummary.forEach { kasir ->
                kasirTable.addCell(PdfPCell(Phrase(kasir.kasirName, normalFont)))
                kasirTable.addCell(PdfPCell(Phrase(kasir.transactionCount.toString(), normalFont)))
                kasirTable.addCell(PdfPCell(Phrase(formatCurrency(kasir.totalAmount), normalFont)))
                kasirTable.addCell(PdfPCell(Phrase(formatCurrency(kasir.paidAmount), normalFont)))
            }
            
            document.add(kasirTable)
        }
        
        // Footer
        document.add(Paragraph(" "))
        val footer = Paragraph(
            "Laporan dibuat pada ${dateTimeFormat.format(Date())}",
            Font(Font.FontFamily.HELVETICA, 8f, Font.ITALIC)
        )
        footer.alignment = Element.ALIGN_RIGHT
        document.add(footer)
        
        document.close()
        return file
    }
    
    private suspend fun getFilteredTransactions(
        startDate: Long, 
        endDate: Long, 
        kasirFilter: String?
    ): List<Transaction> {
        return if (kasirFilter != null) {
            database.transactionDao().getTransactionsByKasirAndDateRange(kasirFilter, startDate, endDate)
        } else {
            database.transactionDao().getTransactionsByDateRangeSync(startDate, endDate)
        }
    }
    
    private fun addSummaryRow(table: PdfPTable, label: String, value: String, font: Font) {
        table.addCell(PdfPCell(Phrase(label, font)).apply { border = Rectangle.NO_BORDER })
        table.addCell(PdfPCell(Phrase(value, font)).apply { 
            border = Rectangle.NO_BORDER
            horizontalAlignment = Element.ALIGN_RIGHT 
        })
    }
    
    private fun addTableHeader(table: PdfPTable, headers: Array<String>, font: Font) {
        headers.forEach { header ->
            val cell = PdfPCell(Phrase(header, font))
            cell.backgroundColor = BaseColor.LIGHT_GRAY
            cell.horizontalAlignment = Element.ALIGN_CENTER
            table.addCell(cell)
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
    
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }
}

// Data classes for reports
data class SalesReportItem(
    val kodeBarang: String,
    val namaBarang: String,
    val kategori: String,
    val totalQuantity: Int,
    val totalAmount: Double,
    val transactionCount: Int,
    val avgPrice: Double
)

data class PaymentSummary(
    val method: String,
    val count: Int,
    val amount: Double
)

data class KasirSummary(
    val kasirId: String,
    val kasirName: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val paidAmount: Double
)
