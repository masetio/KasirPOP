package com.kasirtoko.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    
    private val displayDateFormat = SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.getDefault())
    private val displayDateTimeFormat = SimpleDateFormat(Constants.DATETIME_FORMAT_DISPLAY, Locale.getDefault())
    private val receiptDateTimeFormat = SimpleDateFormat(Constants.DATETIME_FORMAT_RECEIPT, Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat(Constants.DATE_FORMAT_FILE, Locale.getDefault())
    
    fun formatForDisplay(timestamp: Long): String {
        return displayDateFormat.format(Date(timestamp))
    }
    
    fun formatDateTimeForDisplay(timestamp: Long): String {
        return displayDateTimeFormat.format(Date(timestamp))
    }
    
    fun formatForReceipt(timestamp: Long): String {
        return receiptDateTimeFormat.format(Date(timestamp))
    }
    
    fun formatForFile(timestamp: Long): String {
        return fileDateFormat.format(Date(timestamp))
    }
    
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
    
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    fun getStartOfWeek(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return getStartOfDay(calendar.timeInMillis)
    }
    
    fun getEndOfWeek(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        return getEndOfDay(calendar.timeInMillis)
    }
    
    fun getStartOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return getStartOfDay(calendar.timeInMillis)
    }
    
    fun getEndOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return getEndOfDay(calendar.timeInMillis)
    }
    
    fun getDaysDifference(startTimestamp: Long, endTimestamp: Long): Int {
        val diff = endTimestamp - startTimestamp
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }
    
    fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Baru saja"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} menit yang lalu"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} jam yang lalu"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} hari yang lalu"
            else -> formatForDisplay(timestamp)
        }
    }
    
    fun isToday(timestamp: Long): Boolean {
        val today = getStartOfDay(System.currentTimeMillis())
        val tomorrow = today + TimeUnit.DAYS.toMillis(1)
        return timestamp >= today && timestamp < tomorrow
    }
    
    fun isThisWeek(timestamp: Long): Boolean {
        val startOfWeek = getStartOfWeek(System.currentTimeMillis())
        val endOfWeek = getEndOfWeek(System.currentTimeMillis())
        return timestamp >= startOfWeek && timestamp <= endOfWeek
    }
    
    fun isThisMonth(timestamp: Long): Boolean {
        val startOfMonth = getStartOfMonth(System.currentTimeMillis())
        val endOfMonth = getEndOfMonth(System.currentTimeMillis())
        return timestamp >= startOfMonth && timestamp <= endOfMonth
    }
    
    fun parseDate(dateString: String, format: String): Long? {
        return try {
            val formatter = SimpleDateFormat(format, Locale.getDefault())
            formatter.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    fun getDateRangeText(startTimestamp: Long, endTimestamp: Long): String {
        val startDate = formatForDisplay(startTimestamp)
        val endDate = formatForDisplay(endTimestamp)
        
        return if (startDate == endDate) {
            startDate
        } else {
            "$startDate - $endDate"
        }
    }
    
    fun getQuickDateRanges(): Map<String, Pair<Long, Long>> {
        val now = System.currentTimeMillis()
        
        return mapOf(
            "Hari Ini" to Pair(getStartOfDay(now), getEndOfDay(now)),
            "Kemarin" to Pair(
                getStartOfDay(now - TimeUnit.DAYS.toMillis(1)),
                getEndOfDay(now - TimeUnit.DAYS.toMillis(1))
            ),
            "7 Hari Terakhir" to Pair(
                getStartOfDay(now - TimeUnit.DAYS.toMillis(6)),
                getEndOfDay(now)
            ),
            "30 Hari Terakhir" to Pair(
                getStartOfDay(now - TimeUnit.DAYS.toMillis(29)),
                getEndOfDay(now)
            ),
            "Minggu Ini" to Pair(getStartOfWeek(now), getEndOfWeek(now)),
            "Bulan Ini" to Pair(getStartOfMonth(now), getEndOfMonth(now))
        )
    }
}
