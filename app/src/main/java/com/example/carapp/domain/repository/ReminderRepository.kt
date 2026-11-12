package com.example.carapp.domain.repository

import com.example.carapp.domain.model.ServiceReminder

interface ReminderRepository {
    suspend fun getReminders(carId: String): List<ServiceReminder>
    suspend fun saveReminder(reminder: ServiceReminder)
    suspend fun setCompleted(carId: String, reminderId: String, dateMillis: Long?)
}
