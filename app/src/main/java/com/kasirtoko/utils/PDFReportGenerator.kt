class PDFReportGenerator(private val context: Context) {
    
    private val appSettingDao = AppDatabase.getDatabase(context).appSettingDao()
    
    suspend fun generateSalesReport(
        reportData: List<SalesReportItem>,
        startDate: Long,
        endDate: Long
    ): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val fileName = "laporan_penjualan_$timestamp.pdf"
        
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        val document = Document(PageSize.A4)
        PdfWriter.getInstance(document, FileOutputStream(file))
        
        document.open()
        
        // Shop info
        val shopName = appSettingDao.getSetting("shop_name")?.value ?: "TOKO SAYA"
        
        // Title
        val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
        val title = Paragraph("LAPORAN PENJUALAN", titleFont)
        title.alignment = Element.ALIGN_CENTER
        document.add(title)
        
        val shopTitle = Paragraph(shopName, Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD))
        shopTitle.alignment = Element.ALIGN_CENTER
        document.add(shopTitle)
        
        document.add(Paragraph(" ")) // Space
        
        // Date range
        val dateRangeFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateRange = "Periode: ${dateRangeFormat.format(Date(startDate))} - ${dateRangeFormat.format(Date(endDate))}"
        document.add(Paragraph(dateRange))
        
        document.add(Paragraph(" ")) // Space
        
        // Summary
        val totalAmount = reportData.sumOf { it.totalAmount }
        val totalQuantity = reportData.sumOf { it.totalQuantity }
        val totalTransactions = reportData.sumOf { it.transactionCount }
        
        val summaryTable = PdfPTable(2)
        summaryTable.widthPercentage = 100f
        summaryTable.setWidths(floatArrayOf(1f, 1f))
        
        summaryTable.addCell("Total Transaksi")
        summaryTable.addCell(totalTransactions.toString())
        summaryTable.addCell("Total Barang Terjual")
        summaryTable.addCell(totalQuantity.toString())
        summaryTable.addCell("Total Penjualan")
        summaryTable.addCell(formatCurrency(totalAmount))
        
        document.add(summaryTable)
        document.add(Paragraph(" ")) // Space
        
        // Detail table
        val detailTable = PdfPTable(5)
        detailTable.widthPercentage = 100f
        detailTable.setWidths(floatArrayOf(1f, 3f, 1f, 2f, 1f))
        
        // Header
        val headerFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)
        detailTable.addCell(PdfPCell(Phrase("Kode", headerFont)))
        detailTable.addCell(PdfPCell(Phrase("Nama Barang", headerFont)))
        detailTable.addCell(PdfPCell(Phrase("Qty", headerFont)))
        detailTable.addCell(PdfPCell(Phrase("Total", headerFont)))
        detailTable.addCell(PdfPCell(Phrase("Transaksi", headerFont)))
        
        // Data
        val dataFont = Font(Font.FontFamily.HELVETICA, 9f)
        reportData.forEach { item ->
            detailTable.addCell(PdfPCell(Phrase(item.kodeBarang, dataFont)))
            detailTable.addCell(PdfPCell(Phrase(item.namaBarang, dataFont)))
            detailTable.addCell(PdfPCell(Phrase(item.totalQuantity.toString(), dataFont)))
            detailTable.addCell(PdfPCell(Phrase(formatCurrency(item.totalAmount), dataFont)))
            detailTable.addCell(PdfPCell(Phrase(item.transactionCount.toString(), dataFont)))
        }
        
        document.add(detailTable)
        
        // Footer
        document.add(Paragraph(" "))
        val footer = Paragraph(
            "Laporan dibuat pada: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}",
            Font(Font.FontFamily.HELVETICA, 8f, Font.ITALIC)
        )
        footer.alignment = Element.ALIGN_RIGHT
        document.add(footer)
        
        document.close()
        
        return file
    }
    
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }
}
