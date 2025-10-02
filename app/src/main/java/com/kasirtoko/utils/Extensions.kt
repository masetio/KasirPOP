package com.kasirtoko.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Context Extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.shareFile(file: File, mimeType: String, title: String = "Bagikan File") {
    try {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(shareIntent, title))
    } catch (e: Exception) {
        showToast("Gagal berbagi file: ${e.message}")
    }
}

// View Extensions
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

// String Extensions
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

// Double Extensions
fun Double.formatCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(this)
}

fun Double.formatNumber(): String {
    return NumberFormat.getNumberInstance(Locale("id", "ID")).format(this)
}

// Long Extensions (for timestamps)
fun Long.formatDate(pattern: String = Constants.DATE_FORMAT_DISPLAY): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date(this))
}

fun Long.formatDateTime(pattern: String = Constants.DATETIME_FORMAT_DISPLAY): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date(this))
}

// Date Extensions
fun Date.startOfDay(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}

fun Date.endOfDay(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.time
}

// List Extensions
fun <T> List<T>.chunkedBy(maxSize: Int): List<List<T>> {
    return if (size <= maxSize) listOf(this) else chunked(maxSize)
}

// Uri Extensions
fun Uri.getFileName(context: Context): String {
    var result: String? = null
    if (scheme == "content") {
        val cursor = context.contentResolver.query(this, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (columnIndex != -1) {
                    result = it.getString(columnIndex)
                }
            }
        }
    }
    if (result == null) {
        result = path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            result = result?.substring(cut!! + 1)
        }
    }
    return result ?: "Unknown"
}

// Number Extensions
fun Int.formatWithThousandSeparator(): String {
    return NumberFormat.getNumberInstance(Locale("id", "ID")).format(this)
}

// Boolean Extensions
fun Boolean.toInt(): Int = if (this) 1 else 0

fun Int.toBoolean(): Boolean = this != 0

// Collection Extensions
fun <T> Collection<T>.isNotEmpty(): Boolean = !isEmpty()

inline fun <T> Iterable<T>.sumByDouble(selector: (T) -> Double): Double {
    var sum = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
