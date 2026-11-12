package com.example.carapp.ui.reminders

import com.example.carapp.domain.model.ServiceReminder

data class ReminderFormState(
    val visible: Boolean = false,
    val title: String = "",
    val dateEnabled: Boolean = true,
    val dateMillis: Long? = null,
    val mileageEnabled: Boolean = false,
    val mileage: String = "",
    val submitted: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null
)

data class ReminderState(
    val reminders: List<ServiceReminder> = emptyList(),
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val updatingId: String? = null,
    val error: String? = null,
    val form: ReminderFormState = ReminderFormState()
)
