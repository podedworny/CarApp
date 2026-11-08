package com.example.carapp.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Both repositories share this lock, so deleting a car cannot race with adding its service. */
class LocalGarageStorage(context: Context, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO, preferenceName: String = "garage") {
    private val preferences = context.applicationContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    suspend fun <T> access(block: (SharedPreferences) -> T): T = mutex.withLock {
        withContext(ioDispatcher) { block(preferences) }
    }

    companion object {
        fun reminderKey(carId: String) = "reminders:$carId"
        fun serviceKey(carId: String) = "service:$carId"
    }
}
