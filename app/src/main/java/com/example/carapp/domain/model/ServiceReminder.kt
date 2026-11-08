package com.example.carapp.domain.model

import java.util.UUID

data class ServiceReminder(
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val title: String,
    val dueDateMillis: Long? = null,
    val dueMileage: Int? = null,
    val completedDateMillis: Long? = null
)

enum class ReminderStatus { OVERDUE, DUE, UPCOMING, PLANNED, COMPLETED }

/** Either threshold triggers a reminder. Dates are calendar dates at UTC midnight. */
fun ServiceReminder.status(todayMillis: Long, currentMileage: Int): ReminderStatus = when {
    completedDateMillis != null -> ReminderStatus.COMPLETED
    dueDateMillis?.let { it < todayMillis } == true || dueMileage?.let { it < currentMileage } == true -> ReminderStatus.OVERDUE
    dueDateMillis == todayMillis || dueMileage == currentMileage -> ReminderStatus.DUE
    dueDateMillis?.let { it - todayMillis <= 30L * 86400000 } == true ||
        dueMileage?.let { it.toLong() - currentMileage <= 1000 } == true -> ReminderStatus.UPCOMING
    else -> ReminderStatus.PLANNED
}
