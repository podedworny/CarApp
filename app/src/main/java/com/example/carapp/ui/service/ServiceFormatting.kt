package com.example.carapp.ui.service

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatServiceDate(dateMillis: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("pl-PL")).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}.format(Date(dateMillis))

fun formatCost(costMinor: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pl-PL"))
    .format(BigDecimal.valueOf(costMinor, 2))

fun formatCostInput(costMinor: Long): String {
    val major = costMinor / 100
    val minor = costMinor % 100
    return if (minor == 0L) major.toString() else "$major,${minor.toString().padStart(2, '0')}"
}
