package com.example.carapp.domain.validation

data class ExpenseValidationErrors(val title: Boolean, val date: Boolean, val cost: Boolean) {
    val isValid get() = !title && !date && !cost
}

fun validateExpense(
    title: String,
    dateMillis: Long?,
    cost: String,
    productionYear: Int,
    todayMillis: Long
): ExpenseValidationErrors = ExpenseValidationErrors(
    title = title.isBlank() || title.length > 100,
    date = dateMillis == null || dateMillis < ServiceDates.dateMillis(productionYear, 1, 1) || dateMillis > todayMillis,
    cost = cost.isBlank() || parseCostMinor(cost) == null
)

