package com.kasirtoko.ui.admin

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.entities.Transaction
import com.kasirtoko.databinding.ActivityUnpaidTransactionsBinding
import com.kasirtoko.ui.adapters.TransactionAdapter
import com.kasirtoko.utils.ThermalPrinterHelper
import kotlinx.coroutines.launch

class UnpaidTransactionsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUnpaidTransactionsBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var database: AppDatabase
    private lateinit var printerHelper: ThermalPrinterHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnpaidTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getDatabase(this)
        printerHelper = ThermalPrinterHelper(this)
        
        setupUI()
        setupRecyclerView()
        loadUnpaidTransactions()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Transaksi Belum Lunas"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.swipeRefresh.setOnRefreshListener {
            loadUnpaidTransactions()
        }
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                showTransactionDetails(transaction)
            },
            onPayClick = { transaction ->
                showPaymentConfirmation(transaction)
            },
            onPrintClick = { transaction ->
                printTransaction(transaction)
            }
        )
        
        binding.rvUnpaidTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(this@UnpaidTransactionsActivity)
        }
    }
    
    private fun loadUnpaidTransactions() {
        binding.swipeRefresh.isRefreshing = true
        
        database.transactionDao().getUnpaidTransactions().observe(this) { transactions ->
            binding.swipeRefresh.isRefreshing = false
            transactionAdapter.submitList(transactions)
            
            if (transactions.isEmpty()) {
                binding.tvEmptyState.visibility = android.view.View.VISIBLE
                binding.rvUnpaidTransactions.visibility = android.view.View.GONE
            } else {
                binding.tvEmptyState.visibility = android.view.View.GONE
                binding.rvUnpaidTransactions.visibility = android.view.View.VISIBLE
                
                // Update summary
                val totalAmount = transactions.sumOf { it.totalAmount }
                binding.tvTotalUnpaid.text = "Total: ${totalAmount.formatCurrency()}"
                binding.tvTransactionCount.text = "${transactions.size} transaksi belum lunas"
            }
        }
    }
    
    private fun showTransactionDetails(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                val items = database.transactionDao().getTransactionItems(transaction.id)
                
                // Create detail dialog
                val dialogView = layoutInflater.inflate(R.layout.dialog_transaction_detail, null)
                
                // Populate dialog with transaction and items data
                // ... (implement dialog population)
                
                AlertDialog.Builder(this@UnpaidTransactionsActivity)
                    .setTitle("Detail Transaksi")
                    .setView(dialogView)
                    .setPositiveButton("Tutup", null)
                    .show()
                    
            } catch (e: Exception) {
                Toast.makeText(this@UnpaidTransactionsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showPaymentConfirmation(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Pembayaran")
            .setMessage("Tandai transaksi ${transaction.id.takeLast(8)} sebagai LUNAS?\n\nTotal: ${transaction.totalAmount.formatCurrency()}")
            .setPositiveButton("Ya, Lunas") { _, _ ->
                markAsPaid(transaction)
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun markAsPaid(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                database.transactionDao().markAsPaid(transaction.id, System.currentTimeMillis())
                Toast.makeText(this@UnpaidTransactionsActivity, "Transaksi telah ditandai lunas", Toast.LENGTH_SHORT).show()
                
                // Print receipt after payment
                printTransaction(transaction.copy(paymentStatus = "PAID", paidAt = System.currentTimeMillis()))
                
            } catch (e: Exception) {
                Toast.makeText(this@UnpaidTransactionsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun printTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                val items = database.transactionDao().getTransactionItems(transaction.id)
                printerHelper.printReceipt(transaction, items)
                Toast.makeText(this@UnpaidTransactionsActivity, "Nota sedang dicetak", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(this@UnpaidTransactionsActivity, "Error print: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
