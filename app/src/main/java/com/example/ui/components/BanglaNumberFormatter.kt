package com.example.ui.components

import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BanglaNumberFormatter {

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    fun toBanglaDigits(numberStr: String): String {
        val sb = StringBuilder()
        for (ch in numberStr) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(numberStr: String): String {
        val sb = StringBuilder()
        for (ch in numberStr) {
            val idx = banglaDigits.indexOf(ch)
            if (idx != -1) {
                sb.append(englishDigits[idx])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumber(number: Number, useBanglaDigits: Boolean = true): String {
        val formatter = DecimalFormat("#,##,##0")
        val formatted = formatter.format(number)
        return if (useBanglaDigits) toBanglaDigits(formatted) else formatted
    }

    fun formatDecimal(number: Double, useBanglaDigits: Boolean = true): String {
        val formatter = DecimalFormat("#,##,##0.00")
        val formatted = formatter.format(number)
        return if (useBanglaDigits) toBanglaDigits(formatted) else formatted
    }

    fun formatCurrency(amount: Double, useBanglaDigits: Boolean = true): String {
        val formatted = if (amount % 1.0 == 0.0) {
            formatNumber(amount.toLong(), useBanglaDigits)
        } else {
            formatDecimal(amount, useBanglaDigits)
        }
        return "৳$formatted"
    }

    fun formatBanglaDate(dateStr: String): String {
        // Input: YYYY-MM-DD
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toInt()
                val day = parts[2].toInt()

                val monthNames = arrayOf(
                    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
                    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
                )
                val monthName = if (month in 1..12) monthNames[month - 1] else ""
                val dayStr = toBanglaDigits(day.toString())
                val yearStr = toBanglaDigits(year)
                "$dayStr $monthName, $yearStr"
            } else {
                toBanglaDigits(dateStr)
            }
        } catch (e: Exception) {
            toBanglaDigits(dateStr)
        }
    }

    fun formatShortDate(dateStr: String): String {
        // Output: DD/MM/YY in Bangla digits
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val yy = parts[0].takeLast(2)
                val mm = parts[1]
                val dd = parts[2]
                toBanglaDigits("$dd/$mm/$yy")
            } else {
                toBanglaDigits(dateStr)
            }
        } catch (e: Exception) {
            toBanglaDigits(dateStr)
        }
    }

    fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private val monthNames = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    // Input: "YYYY-MM" -> Output: "আগস্ট ২০২৬"
    fun formatYearMonth(yearMonth: String): String {
        return try {
            val parts = yearMonth.split("-")
            if (parts.size == 2) {
                val year = parts[0]
                val month = parts[1].toInt()
                val monthName = if (month in 1..12) monthNames[month - 1] else yearMonth
                "$monthName ${toBanglaDigits(year)}"
            } else {
                toBanglaDigits(yearMonth)
            }
        } catch (e: Exception) {
            toBanglaDigits(yearMonth)
        }
    }

    fun getCurrentDateBangla(): String {
        return formatBanglaDate(getCurrentDateFormatted())
    }

    fun convertBanglaToEnglishDigits(numberStr: String): String {
        return toEnglishDigits(numberStr)
    }
}
