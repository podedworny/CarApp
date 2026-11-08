package com.example.carapp.domain.validation

import java.math.BigDecimal
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

object ServiceDates {
    fun dateMillis(year: Int, month: Int, day: Int): Long =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis

    fun todayMillis(): Long {
        val local = Calendar.getInstance()
        return dateMillis(local.get(Calendar.YEAR), local.get(Calendar.MONTH) + 1, local.get(Calendar.DAY_OF_MONTH))
    }
}

/** A missing amount differs from a service that was free. Reject fractions smaller than one grosz. */
fun parseCostMinor(value: String): Long? {
    val normalized = value.trim().replace(',', '.')
    if (!Regex("[0-9]{1,8}(\\.[0-9]{1,2})?").matches(normalized)) return null
    return runCatching { BigDecimal(normalized).movePointRight(2).longValueExact() }.getOrNull()
}

data class ServiceValidationErrors(val title: Boolean, val date: Boolean, val mileage: Boolean, val cost: Boolean) {
    val isValid get() = !title && !date && !mileage && !cost
}

fun validateService(title: String, dateMillis: Long?, mileage: String, cost: String,
                    productionYear: Int, todayMillis: Long): ServiceValidationErrors = ServiceValidationErrors(
    title = title.isBlank() || title.length > 100,
    date = dateMillis == null || dateMillis < ServiceDates.dateMillis(productionYear, 1, 1) || dateMillis > todayMillis,
    mileage = mileage.toIntOrNull()?.let { it >= 0 } != true,
    cost = cost.isNotBlank() && parseCostMinor(cost) == null
)
