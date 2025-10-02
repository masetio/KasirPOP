package com.kasirtoko.ui.reports

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.kasirtoko.R
import com.kasirtoko.databinding.ActivityFinancialReportBinding
import com.kasirtoko.ui.adapters.PaymentSummaryAdapter
import com.kasirtoko.utils.DateUtils
import com.kasirtoko.utils.formatCurrency
import com.kasirtoko.viewmodel.ReportViewModel
import kotlinx.coroutines.launch
import java.util.*

class FinancialReportActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFinancialReportBinding
    private lateinit var viewModel: ReportViewModel
    private lateinit var paymentAdapter: PaymentSummaryAdapter
    
    private var startDate: Long = 0
    private var endDate: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinancialReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[ReportViewModel::class.java]
        
        // Set default date range (today)
        val today = System.currentTimeMillis()
        startDate = DateUtils.getStartOfDay(today)
        endDate = DateUtils.getEndOfDay(today)
        
        setupUI()
        setupRecyclerView()
        setupChart()
        observeViewModel()
        loadFinancialReport()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Laporan Keuangan"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        updateDateDisplay()
        
        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }
        
        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }
        
        binding.btnRefresh.setOnClickListener {
            loadFinancialReport()
        }
        
        binding.btnExportPdf.setOnClickListener {
            exportToPDF()
        }
    }
    
    private fun setupRecyclerView() {
        paymentAdapter = PaymentSummaryAdapter()
        binding.rvPaymentSummary.apply {
            adapter = paymentAdapter
            layoutManager = LinearLayoutManager(this@FinancialReportActivity)
        }
    }
    
    private fun setupChart() {
        binding.pieChartPayments.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(android.R.color.white)
            setTransparentCircleColor(android.R.color.white)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Metode\nPembayaran"
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
        }
    }
    
    private fun observeViewModel() {
        viewModel.paymentSummary.observe(this) { paymentSummary ->
            paymentAdapter.submitList(paymentSummary)
            updatePaymentChart(paymentSummary)
            updateSummary(paymentSummary)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                // Show error message
                binding.tvError.text = it
                binding.tvError.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvError.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun updateDateDisplay() {
        binding.btnStartDate.text = DateUtils.formatForDisplay(startDate)
        binding.btnEndDate.text = DateUtils.formatForDisplay(endDate)
        
        binding.tvDateRange.text = "Periode: ${DateUtils.getDateRangeText(startDate, endDate)}"
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val currentDate = if (isStartDate) startDate else endDate
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
        
        android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                
                if (isStartDate) {
                    startDate = DateUtils.getStartOfDay(selectedCalendar.timeInMillis)
                } else {
                    endDate = DateUtils.getEndOfDay(selectedCalendar.timeInMillis)
                }
                
                updateDateDisplay()
                loadFinancialReport()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun loadFinancialReport() {
        viewModel.generateDailyReport(startDate)
    }
    
    private fun updatePaymentChart(paymentSummary: List<com.kasirtoko.data.models.PaymentSummary>) {
        if (paymentSummary.isEmpty()) {
            binding.pieChartPayments.clear()
            return
        }
        
        val entries = paymentSummary.map { payment ->
            PieEntry(payment.amount.toFloat(), getPaymentMethodName(payment.method))
        }
        
        val dataSet = PieDataSet(entries, "Metode Pembayaran")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, android.R.color.holo_green_light),
            ContextCompat.getColor(this, android.R.color.holo_blue_light),
            ContextCompat.getColor(this, android.R.color.holo_orange_light)
        )
        
        val data = PieData(dataSet)
        data.setValueTextSize(11f)
        data.setValueTextColor(android.R.color.black)
        
        binding.pieChartPayments.data = data
        binding.pieChartPayments.invalidate()
    }
    
    private fun updateSummary(paymentSummary: List<com.kasirtoko.data.models.PaymentSummary>) {
        val totalAmount = paymentSummary.sumOf { it.amount }
        val totalTransactions = paymentSummary.sumOf { it.count }
        
        binding.tvTotalAmount.text = totalAmount.formatCurrency()
        binding.tvTotalTransactions.text = "$totalTransactions transaksi"
        
        if (totalTransactions > 0) {
            binding.tvAverageTransaction.text = "Rata-rata: ${(totalAmount / totalTransactions).formatCurrency()}"
        } else {
            binding.tvAverageTransaction.text = "Rata-rata: Rp 0"
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
    
    private fun exportToPDF() {
        viewModel.exportDailyReportPDF(startDate) { file ->
            shareFile(file)
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
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Laporan"))
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
