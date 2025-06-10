package com.example.banhangs.Helper

import java.text.DecimalFormat
import kotlin.math.ln
import kotlin.math.pow

fun formatNumberToShortForm(number: Double): String {
    if (number < 1000) {
        return DecimalFormat("#.##").format(number) // No abbreviation for numbers less than 1000
    }
    val exp = (ln(number) / ln(1000.0)).toInt()
    val suffix = charArrayOf('K', 'M', 'B', 'T', 'P', 'E') // Add more suffixes if needed

    // Format to one decimal place if it's not a whole number, otherwise no decimal places
    val formatPattern = if (number % 1000.0.pow(exp.toDouble()) == 0.0) "%.0f%c" else "%.1f%c"

    return String.format(java.util.Locale.US, formatPattern, number / 1000.0.pow(exp.toDouble()), suffix[exp - 1])
}

// Overload for Long if you deal with whole numbers primarily for counts
fun formatNumberToShortForm(number: Long): String {
    return formatNumberToShortForm(number.toDouble())
}