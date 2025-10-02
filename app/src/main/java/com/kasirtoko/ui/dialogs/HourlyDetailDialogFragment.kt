package com.kasirtoko.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasirtoko.databinding.DialogHourlyDetailBinding
import com.kasirtoko.ui.adapters.DailyReportAdapter
import com.kasirtoko.ui.reports.HourlyReportData
import com.kasirtoko.utils.CurrencyUtils

class HourlyDetailDialogFragment : DialogFragment() {
    
    private var _binding: DialogHourlyDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var hourlyData: HourlyReportData
    private lateinit var transactionAdapter: DailyReportAdapter
    
    companion object {
        fun newInstance(hourlyData: HourlyReportData): HourlyDetailDialogFragment {
            return HourlyDetailDialogFragment().apply {
                this.hourlyData = hourlyData
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogHourlyDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
    }
    
    private fun setupUI() {
        binding.apply {
            tvHourRange.text = "${String.format("%02d", hourlyData.hour)}:00 - ${String.format("%02d", hourlyData.hour + 1)}:00"
            tvTotalTransactions.text = "${hourlyData.transactionCount} transaksi"
            tvTotalAmount.text = CurrencyUtils.formatToRupiah(hourlyData.totalAmount)
            tvPaidTransactions.text = "${hourlyData.paidCount} transaksi lunas"
            tvUnpaidTransactions.text = "${hourlyData.unpaidCount} transaksi belum lunas"
            tvAverageTransaction.text = "Rata-rata: ${CurrencyUtils.formatToRupiah(hourlyData.averageTransaction)}"
            
            btnClose.setOnClickListener {
                dismiss()
            }
        }
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = DailyReportAdapter { transaction ->
            // Handle transaction click if needed
        }
        
        binding.rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        
        transactionAdapter.submitList(hourlyData.transactions)
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
