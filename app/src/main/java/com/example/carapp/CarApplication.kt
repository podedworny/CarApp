package com.example.carapp

import android.app.Application
import android.content.Context
import com.example.carapp.data.LocalReminderRepository
import com.example.carapp.domain.repository.ReminderRepository
import com.example.carapp.data.LocalGarageStorage
import com.example.carapp.data.LocalServiceRepository
import com.example.carapp.domain.repository.ServiceRepository
import com.example.carapp.data.LocalCarRepository
import com.example.carapp.data.LocalExpenseRepository
import com.example.carapp.domain.repository.CarRepository
import com.example.carapp.domain.repository.ExpenseRepository

/** Application-scoped dependencies; repositories can be replaced without changing screens. */
class AppContainer(context: Context) {
    private val storage = LocalGarageStorage(context)
    val carRepository: CarRepository = LocalCarRepository(storage)
    val reminderRepository: ReminderRepository = LocalReminderRepository(storage)
    val serviceRepository: ServiceRepository = LocalServiceRepository(storage)
    val expenseRepository: ExpenseRepository = LocalExpenseRepository(storage)
}

class CarApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
