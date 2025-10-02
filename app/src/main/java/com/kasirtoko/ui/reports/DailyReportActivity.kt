package com.kasirtoko.ui.reports

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.kasirtoko.R
import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.entities.Transaction
import com.kasirtoko.databinding.ActivityDailyReportBinding
import com.kasirtoko.ui.adapters.DailyReportAdapter
import com.kasirtoko.ui.adapters.HourlyReportAdapter
import com.kasirtoko.utils.CurrencyUtils
import com.kasirtoko.utils.DateUtils
import com.kasirtoko.utils.ReportGenerator
import kotlinx.coroutines.launch
import java.util.*

class DailyReportActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDailyReportBinding
    private lateinit var database: AppDatabase
    private lateinit var reportGenerator: ReportGenerator
    private lateinit var dailyReportAdapter: DailyReportAdapter
    private lateinit var hourlyReportAdapter: HourlyReportAdapter
    
    private var selectedDate: Long = System.currentTimeMillis()
    private var currentTransactions: List<Transaction> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getDatabase(this)
        reportGenerator = ReportGenerator(this)
        
        setupUI()
        setupRecyclerViews()
        setupCharts()
        generateDailyReport()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Laporan Harian"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        updateDateDisplay()
        
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
        
        binding.btnPreviousDay.setOnClickListener {
            selectedDate -= 24 * 60 * 60 * 1000 // Go back 1 day
            updateDateDisplay()
            generateDailyReport()
        }
        
        binding.btnNextDay.setOnClickListener {
            selectedDate += 24 * 60 * 60 * 1000 // Go forward 1 day
            updateDateDisplay()
            generateDailyReport()
        }
        
        binding.btnToday.setOnClickListener {
            selectedDate = System.currentTimeMillis()
            updateDateDisplay()
            generateDailyReport()
        }
        
        binding.btnRefresh.setOnClickListener {
            generateDailyReport()
        }
        
        binding.btnExportPdf.setOnClickListener {
            exportToPDF()
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            generateDailyReport()
        }
        
        setupTabLayout()
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Ringkasan"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Grafik"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Per Jam"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Detail"))
        
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showSummaryView()
                    1 -> showChartsView()
                    2 -> showHourlyView()
                    3 -> showDetailView()
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
        
        // Show summary by default
        showSummaryView()
    }
    
    private fun showSummaryView() {
        binding.layoutSummary.visibility = View.VISIBLE
        binding.layoutCharts.visibility = View.GONE
        binding.layoutHourly.visibility = View.GONE
        binding.layoutDetail.visibility = View.GONE
    }
    
    private fun showChartsView() {
        binding.layoutSummary.visibility = View.GONE
        binding.layoutCharts.visibility = View.VISIBLE
        binding.layoutHourly.visibility = View.GONE
        binding.layoutDetail.visibility = View.GONE
        
        setupChartsData()
    }
    
    private fun showHourlyView() {
        binding.layoutSummary.visibility = View.GONE
        binding.layoutCharts.visibility = View.GONE
        binding.layoutHourly.visibility = View.VISIBLE
        binding.layoutDetail.visibility = View.GONE
        
        generateHourlyReport()
    }
    
    private fun showDetailView() {
        binding.layoutSummary.visibility = View.GONE
        binding.layoutCharts.visibility = View.GONE
        binding.layoutHourly.visibility = View.GONE
        binding.layoutDetail.visibility = View.VISIBLE
    }
    
    private fun setupRecyclerViews() {
        dailyReportAdapter = DailyReportAdapter { transaction ->
            showTransactionDetail(transaction)
        }
        
        binding.rvDetailTransactions.apply {
            adapter = dailyReportAdapter
            layoutManager = LinearLayoutManager(this@DailyReportActivity)
        }
        
        hourlyReportAdapter = HourlyReportAdapter { hourlyData ->
            showHourlyDetail(hourlyData)
        }
        
        binding.rvHourlyReport.apply {
            adapter = hourlyReportAdapter
            layoutManager = LinearLayoutManager(this@DailyReportActivity)
        }
    }
    
    private fun setupCharts() {
        setupPieChart()
        setupBarChart()
        setupLineChart()
    }
    
    private fun setupPieChart() {
        binding.pieChartPayments.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(android.R.color.white)
            setTransparentCircleColor(android.R.color.white)
            holeRadius = 45f
            transparentCircleRadius = 50f
            setDrawCenterText(true)
            centerText = "Metode\nPembayaran"
            isRotationEnabled = true
            legend.isEnabled = true
        }
    }
    
    private fun setupBarChart() {
        binding.barChartHourly.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }
    
    private fun setupLineChart() {
        binding.lineChartTrend.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }
    
    private fun updateDateDisplay() {
        binding.btnSelectDate.text = DateUtils.formatForDisplay(selectedDate)
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate
        val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
        
        binding.tvSelectedDate.text = "$dayOfWeek, ${DateUtils.formatForDisplay(selectedDate)}"
        
        // Check if it's today
        val isToday = DateUtils.isToday(selectedDate)
        binding.btnToday.visibility = if (isToday) View.GONE else View.VISIBLE
        
        // Disable next button if selected date is today or in the future
        binding.btnNextDay.isEnabled = selectedDate < DateUtils.getStartOfDay(System.currentTimeMillis())
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                selectedDate = newCalendar.timeInMillis
                
                updateDateDisplay()
                generateDailyReport()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun generateDailyReport() {
        binding.swipeRefresh.isRefreshing = true
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val startOfDay = DateUtils.getStartOfDay(selectedDate)
                val endOfDay = DateUtils.getEndOfDay(selectedDate)
                
                currentTransactions = database.transactionDao().getTransactionsByDateRangeSync(startOfDay, endOfDay)
                
                binding.swipeRefresh.isRefreshing = false
                binding.progressBar.visibility = View.GONE
                
                updateSummaryData()
                updateDetailData()
                
                if (currentTransactions.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                }
                
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@DailyReportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private suspend fun updateSummaryData() {
        val paidTransactions = currentTransactions.filter { it.paymentStatus == "PAID" }
        val unpaidTransactions = currentTransactions.filter { it.paymentStatus == "UNPAID" }
        
        // Basic stats
        val totalTransactions = currentTransactions.size
        val totalAmount = currentTransactions.sumOf { it.totalAmount }
        val paidAmount = paidTransactions.sumOf { it.totalAmount }
        val unpaidAmount = unpaidTransactions.sumOf { it.totalAmount }
        
        // Payment method breakdown
        val paymentMethodBreakdown = paidTransactions.groupBy { it.paymentMethod }
            .mapValues { (_, transactions) ->
                PaymentMethodData(
                    count = transactions.size,
                    amount = transactions.sumOf { it.totalAmount }
                )
            }
        
        // Items sold calculation
        val totalItemsSold = getTotalItemsSold()
        val averageTransaction = if (totalTransactions > 0) totalAmount / totalTransactions else 0.0
        
        // Kasir performance
        val kasirPerformance = currentTransactions.groupBy { it.kasirId }
            .map { (kasirId, transactions) ->
                KasirDailyPerformance(
                    kasirId = kasirId,
                    kasirName = transactions.first().kasirName,
                    transactionCount = transactions.size,
                    totalAmount = transactions.sumOf { it.totalAmount },
                    paidTransactions = transactions.count { it.paymentStatus == "PAID" }
                )
            }
            .sortedByDescending { it.totalAmount }
        
        // Peak hour calculation
        val peakHour = findPeakHour()
        
        // Update UI
        binding.apply {
            tvTotalTransactions.text = totalTransactions.toString()
            tvTotalAmount.text = CurrencyUtils.formatToRupiah(totalAmount)
            tvPaidTransactions.text = paidTransactions.size.toString()
            tvUnpaidTransactions.text = unpaidTransactions.size.toString()
            tvPaidAmount.text = CurrencyUtils.formatToRupiah(paidAmount)
            tvUnpaidAmount.text = CurrencyUtils.formatToRupiah(unpaidAmount)
            tvTotalItemsSold.text = totalItemsSold.toString()
            tvAverageTransaction.text = CurrencyUtils.formatToRupiah(averageTransaction)
            tvPeakHour.text = "${peakHour}:00 - ${peakHour + 1}:00"
            
            // Payment method details
            val cashData = paymentMethodBreakdown["CASH"]
            val qrisData = paymentMethodBreakdown["QRIS"]
            val debtData = paymentMethodBreakdown["DEBT"]
            
            tvCashCount.text = "${cashData?.count ?: 0} transaksi"
            tvCashAmount.text = CurrencyUtils.formatToRupiah(cashData?.amount ?: 0.0)
            
            tvQrisCount.text = "${qrisData?.count ?: 0} transaksi"
            tvQrisAmount.text = CurrencyUtils.formatToRupiah(qrisData?.amount ?: 0.0)
            
            tvDebtCount.text = "${debtData?.count ?: 0} transaksi"
            tvDebtAmount.text = CurrencyUtils.formatToRupiah(debtData?.amount ?: 0.0)
            
            // Best performing kasir
            if (kasirPerformance.isNotEmpty()) {
                val bestKasir = kasirPerformance.first()
                tvBestKasir.text = "${bestKasir.kasirName} (${bestKasir.transactionCount} transaksi)"
                tvBestKasirAmount.text = CurrencyUtils.formatToRupiah(bestKasir.totalAmount)
                layoutBestKasir.visibility = View.VISIBLE
            } else {
                layoutBestKasir.visibility = View.GONE
            }
            
            // Growth comparison (compare with previous day)
            updateGrowthComparison()
        }
    }
    
    private suspend fun updateGrowthComparison() {
        val previousDay = selectedDate - (24 * 60 * 60 * 1000)
        val previousDayStart = DateUtils.getStartOfDay(previousDay)
        val previousDayEnd = DateUtils.getEndOfDay(previousDay)
        
        val previousTransactions = database.transactionDao().getTransactionsByDateRangeSync(previousDayStart, previousDayEnd)
        val previousAmount = previousTransactions.sumOf { it.totalAmount }
        val currentAmount = currentTransactions.sumOf { it.totalAmount }
        
        val growth = if (previousAmount > 0) {
            ((currentAmount - previousAmount) / previousAmount * 100)
        } else if (currentAmount > 0) {
            100.0
        } else {
            0.0
        }
        
        binding.apply {
            if (growth >= 0) {
                tvGrowthPercentage.text = "+${String.format("%.1f", growth)}%"
                tvGrowthPercentage.setTextColor(getColor(android.R.color.holo_green_dark))
                ivGrowthIcon.setImageResource(R.drawable.ic_trending_up)
                ivGrowthIcon.setColorFilter(getColor(android.R.color.holo_green_dark))
            } else {
                tvGrowthPercentage.text = "${String.format("%.1f", growth)}%"
                tvGrowthPercentage.setTextColor(getColor(R.color.error))
                ivGrowthIcon.setImageResource(R.drawable.ic_trending_down)
                ivGrowthIcon.setColorFilter(getColor(R.color.error))
            }
            
            tvGrowthComparison.text = "vs kemarin (${CurrencyUtils.formatToShortRupiah(previousAmount)})"
        }
    }
    
    private suspend fun getTotalItemsSold(): Int {
        var totalItems = 0
        currentTransactions.forEach { transaction ->
            val items = database.transactionDao().getTransactionItems(transaction.id)
            totalItems += items.sumOf { it.quantity }
        }
        return totalItems
    }
    
    private fun findPeakHour(): Int {
        val hourlyCount = IntArray(24)
        
        currentTransactions.forEach { transaction ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = transaction.createdAt
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyCount[hour]++
        }
        
        return hourlyCount.indices.maxByOrNull { hourlyCount[it] } ?: 0
    }
    
    private fun updateDetailData() {
        dailyReportAdapter.submitList(currentTransactions.sortedByDescending { it.createdAt })
    }
    
    private fun setupChartsData() {
        setupPieChartData()
        setupBarChartData()
        setupLineChartData()
    }
    
    private fun setupPieChartData() {
        val paidTransactions = currentTransactions.filter { it.paymentStatus == "PAID" }
        val paymentMethods = paidTransactions.groupBy { it.paymentMethod }
        
        if (paymentMethods.isEmpty()) {
            binding.pieChartPayments.clear()
            binding.tvNoPieData.visibility = View.VISIBLE
            return
        }
        
        binding.tvNoPieData.visibility = View.GONE
        
        val entries = paymentMethods.map { (method, transactions) ->
            PieEntry(
                transactions.sumOf { it.totalAmount }.toFloat(),
                getPaymentMethodName(method)
            )
        }
        
        val dataSet = PieDataSet(entries, "Metode Pembayaran")
        dataSet.colors = listOf(
            getColor(android.R.color.holo_green_light),
            getColor(android.R.color.holo_blue_light),
            getColor(android.R.color.holo_orange_light)
        )
        
        val data = PieData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return CurrencyUtils.formatToShortRupiah(value.toDouble())
            }
        })
        
        binding.pieChartPayments.data = data
        binding.pieChartPayments.invalidate()
    }
    
    private fun setupBarChartData() {
        val hourlyData = IntArray(24)
        val hourlyAmounts = DoubleArray(24)
        
        currentTransactions.forEach { transaction ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = transaction.createdAt
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyData[hour]++
            hourlyAmounts[hour] += transaction.totalAmount
        }
        
        val entries = hourlyData.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }
        
        val dataSet = BarDataSet(entries, "Transaksi per Jam")
        dataSet.color = getColor(R.color.primary)
        dataSet.valueTextColor = getColor(R.color.on_surface)
        
        val data = BarData(dataSet)
        data.barWidth = 0.8f
        
        binding.barChartHourly.data = data
        binding.barChartHourly.xAxis.valueFormatter = IndexAxisValueFormatter(
            (0..23).map { "${it}:00" }
        )
        binding.barChartHourly.invalidate()
    }
    
    private fun setupLineChartData() {
        // Get last 7 days data for trend
        val entries = mutableListOf<Entry>()
        
        lifecycleScope.launch {
            for (i in 6 downTo 0) {
                val date = selectedDate - (i * 24 * 60 * 60 * 1000)
                val startOfDay = DateUtils.getStartOfDay(date)
                val endOfDay = DateUtils.getEndOfDay(date)
                
                val dayTransactions = database.transactionDao().getTransactionsByDateRangeSync(startOfDay, endOfDay)
                val dayAmount = dayTransactions.sumOf { it.totalAmount }
                
                entries.add(Entry((6 - i).toFloat(), dayAmount.toFloat()))
            }
            
            val dataSet = LineDataSet(entries, "Tren 7 Hari Terakhir")
            dataSet.apply {
                color = getColor(R.color.primary)
                setCircleColor(getColor(R.color.primary))
                lineWidth = 2f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 10f
                setDrawFilled(true)
                fillColor = getColor(R.color.primary_light)
            }
            
            val lineData = LineData(dataSet)
            lineData.setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return CurrencyUtils.formatToShortRupiah(value.toDouble())
                }
            })
            
            binding.lineChartTrend.data = lineData
            binding.lineChartTrend.xAxis.valueFormatter = IndexAxisValueFormatter(
                listOf("6 hari", "5 hari", "4 hari", "3 hari", "2 hari", "Kemarin", "Hari ini")
            )
            binding.lineChartTrend.invalidate()
        }
    }
    
    private fun generateHourlyReport() {
        val hourlyReports = mutableListOf<HourlyReportData>()
        
        for (hour in 0..23) {
            val hourTransactions = currentTransactions.filter { transaction ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = transaction.createdAt
                calendar.get(Calendar.HOUR_OF_DAY) == hour
            }
            
            if (hourTransactions.isNotEmpty()) {
                val hourlyData = HourlyReportData(
                    hour = hour,
                    transactionCount = hourTransactions.size,
                    totalAmount = hourTransactions.sumOf { it.totalAmount },
                    paidCount = hourTransactions.count { it.paymentStatus == "PAID" },
                    unpaidCount = hourTransactions.count { it.paymentStatus == "UNPAID" },
                    averageTransaction = hourTransactions.sumOf { it.totalAmount } / hourTransactions.size,
                    transactions = hourTransactions
                )
                hourlyReports.add(hourlyData)
            }
        }
        
        hourlyReportAdapter.submitList(hourlyReports)
        
        if (hourlyReports.isEmpty()) {
            binding.tvNoHourlyData.visibility = View.VISIBLE
            binding.rvHourlyReport.visibility = View.GONE
        } else {
            binding.tvNoHourlyData.visibility = View.GONE
            binding.rvHourlyReport.visibility = View.VISIBLE
        }
    }
    
    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.tabLayout.visibility = View.GONE
        binding.viewPager.visibility = View.GONE
    }
    
    private fun hideEmptyState() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.tabLayout.visibility = View.VISIBLE
        binding.viewPager.visibility = View.VISIBLE
    }
    
    private fun showTransactionDetail(transaction: Transaction) {
        lifecycleScope.launch {
            val items = database.transactionDao().getTransactionItems(transaction.id)
            val dialog = com.kasirtoko.ui.dialogs.TransactionDetailDialogFragment.newInstance(transaction, items)
            dialog.show(supportFragmentManager, "transaction_detail")
        }
    }
    
    private fun showHourlyDetail(hourlyData: HourlyReportData) {
        val dialog = HourlyDetailDialogFragment.newInstance(hourlyData)
        dialog.show(supportFragmentManager, "hourly_detail")
    }
    
    private fun exportToPDF() {
        if (currentTransactions.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val file = reportGenerator.generateDailyReportPDF(selectedDate)
                shareFile(file)
                
            } catch (e: Exception) {
                Toast.makeText(this@DailyReportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun shareFile(file: java.io.File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Laporan Harian"))
    }
    
    private fun getPaymentMethodName(method: String): String {
        return when (method) {
            "CASH" -> "Tunai"
            "QRIS" -> "QRIS"
            "DEBT" -> "Utang"
            else -> method
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

// Data classes
data class PaymentMethodData(
    val count: Int,
    val amount: Double
)

data class KasirDailyPerformance(
    val kasirId: String,
    val kasirName: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val paidTransactions: Int
)

data class HourlyReportData(
    val hour: Int,
    val transactionCount: Int,
    val totalAmount: Double,
    val paidCount: Int,
    val unpaidCount: Int,
    val averageTransaction: Double,
    val transactions: List<Transaction>
)
