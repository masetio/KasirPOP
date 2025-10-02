package com.kasirtoko.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasirtoko.databinding.DialogCashierDetailBinding
import com.kasirtoko.ui.adapters.DailyReportAdapter
import com.kasirtoko.ui.reports.CashierReportItem
import com.kasirtoko.utils.CurrencyUtils
import com.kasirtoko.utils.DateUtils

class CashierDetailDialogFragment : DialogFragment() {
    
    private var _binding: DialogCashierDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var cashierReport: CashierReportItem
    private var startDate: Long = 0
    private var endDate: Long = 0
    
    companion object {
        fun newInstance(
            cashierReport: CashierReportItem,
            startDate: Long,
            endDate: Long
        ): CashierDetailDialogFragment {
            return CashierDetailDialogFragment().apply {
                this.cashierReport = cashierReport
                this.startDate = startDate
                this.endDate = endDate
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCashierDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }
    
    private fun setupUI() {
        binding.apply {
            tvKasirName.text = cashierReport.kasirName
            tvKasirUsername.text = "@${cashierReport.kasirUsername}"
            tvPeriod.text = "Periode: ${DateUtils.getDateRangeText(startDate, endDate)}"
            
            // Basic stats
            tvTotalTransactions.text = "${cashierReport.totalTransactions} transaksi"
            tvTotalAmount.text = CurrencyUtils.formatToRupiah(cashierReport.totalAmount)
            tvPaidTransactions.text = "${cashierReport.paidTransactions} lunas"
            tvUnpaidTransactions.text = "${cashierReport.unpaidTransactions} belum lunas"
            tvTotalItems.text = "${cashierReport.totalItemsSold} item terjual"
            tvAverageTransaction.text = CurrencyUtils.formatToRupiah(cashierReport.averageTransactionValue)
            
            // Financial breakdown
            tvPaidAmount.text = CurrencyUtils.formatToRupiah(cashierReport.paidAmount)
            tvUnpaidAmount.text = CurrencyUtils.formatToRupiah(cashierReport.unpaidAmount)
            
            // Payment method breakdown
            val cashData = cashierReport.paymentMethodBreakdown["CASH"]
            val qrisData = cashierReport.paymentMethodBreakdown["QRIS"]
            val debtData = cashierReport.paymentMethodBreakdown["DEBT"]
            
            tvCashCount.text = "${cashData?.count ?: 0} transaksi"
            tvCashAmount.text = CurrencyUtils.formatToRupiah(cashData?.amount ?: 0.0)
            
            tvQrisCount.text = "${qrisData?.count ?: 0} transaksi"
            tvQrisAmount.text = CurrencyUtils.formatToRupiah(qrisData?.amount ?: 0.0)
            
            tvDebtCount.text = "${debtData?.count ?: 0} transaksi"
            tvDebtAmount.text = CurrencyUtils.formatToRupiah(debtData?.amount ?: 0.0)
            
            // Additional info
            tvBestHour.text = "${String.format("%02d", cashierReport.bestSellingHour)}:00 - ${String.format("%02d", cashierReport.bestSellingHour + 1)}:00"
            
            cashierReport.firstTransactionTime?.let {
                tvFirstTransaction.text = "Pertama: ${DateUtils.formatForDisplay(it)} ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))}"
            }
            
            cashierReport.lastTransactionTime?.let {
                tvLastTransaction.text = "Terakhir: ${DateUtils.formatForDisplay(it)} ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))}"
            }
            
            btnClose.setOnClickListener {
                dismiss()
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
