package com.example.carapp.domain.validation

data class ReminderValidationErrors(val title: Boolean, val target: Boolean, val date: Boolean, val mileage: Boolean) {
    val isValid get() = !title && !target && !date && !mileage
}

fun validateReminder(title: String, dateEnabled: Boolean, dateMillis: Long?, mileageEnabled: Boolean,
                     mileage: String, productionYear: Int) = ReminderValidationErrors(
    title = title.isBlank() || title.length > 100,
    target = !dateEnabled && !mileageEnabled,
    date = dateEnabled && (dateMillis == null || dateMillis < ServiceDates.dateMillis(productionYear, 1, 1)),
    mileage = mileageEnabled && mileage.toIntOrNull()?.let { it >= 0 } != true
)
