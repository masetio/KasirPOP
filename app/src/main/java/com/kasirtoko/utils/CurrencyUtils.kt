package com.kasirtoko.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*

object CurrencyUtils {
    
    private val indonesianLocale = Locale("id", "ID")
    
    fun formatToRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(indonesianLocale).format(amount)
    }
    
    fun formatToRupiahWithoutSymbol(amount: Double): String {
        val format = DecimalFormat("#,##0", DecimalFormatSymbols(indonesianLocale))
        return format.format(amount)
    }
    
    fun parseFromRupiah(rupiahString: String): Double {
        return try {
            val cleanString = rupiahString
                .replace("Rp", "")
                .replace(".", "")
                .replace(",", ".")
                .trim()
            cleanString.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }
    
    fun formatToShortRupiah(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "${formatToRupiahWithoutSymbol(amount / 1_000_000_000)} M"
            amount >= 1_000_000 -> "${formatToRupiahWithoutSymbol(amount / 1_000_000)} Jt"
            amount >= 1_000 -> "${formatToRupiahWithoutSymbol(amount / 1_000)} Rb"
            else -> formatToRupiahWithoutSymbol(amount)
        }
    }
    
    fun isValidAmount(amountString: String): Boolean {
        return try {
            val amount = parseFromRupiah(amountString)
            amount >= 0
        } catch (e: Exception) {
            false
        }
    }
    
    fun roundUpToNearest(amount: Double, roundTo: Int): Double {
        return kotlin.math.ceil(amount / roundTo) * roundTo
    }
    
    fun generateQuickAmounts(baseAmount: Double): List<Double> {
        return listOf(
            baseAmount,
            roundUpToNearest(baseAmount, 1000),
            roundUpToNearest(baseAmount, 5000),
            roundUpToNearest(baseAmount, 10000),
            roundUpToNearest(baseAmount, 20000),
            roundUpToNearest(baseAmount, 50000),
            roundUpToNearest(baseAmount, 100000)
        ).distinct().sorted().filter { it >= baseAmount }
    }
}
