package com.example.carapp.data

import com.example.carapp.domain.model.ServiceReminder
import com.example.carapp.domain.repository.ReminderRepository
import org.json.JSONArray
import org.json.JSONObject

class LocalReminderRepository(private val storage: LocalGarageStorage) : ReminderRepository {
    override suspend fun getReminders(carId: String): List<ServiceReminder> = storage.access {
        decode(it.getString(LocalGarageStorage.reminderKey(carId), "[]"), carId)
    }

    override suspend fun saveReminder(reminder: ServiceReminder): Unit = storage.access { preferences ->
        val cars = JSONArray(preferences.getString("cars", "[]"))
        check((0 until cars.length()).any { cars.getJSONObject(it).getString("id") == reminder.carId })
        val key = LocalGarageStorage.reminderKey(reminder.carId)
        val entries = decode(preferences.getString(key, "[]"), reminder.carId)
        check(preferences.edit().putString(key, encode(entries.filterNot { it.id == reminder.id } + reminder)).commit())
    }

    override suspend fun setCompleted(carId: String, reminderId: String, dateMillis: Long?): Unit = storage.access { preferences ->
        val key = LocalGarageStorage.reminderKey(carId)
        val entries = decode(preferences.getString(key, "[]"), carId)
        check(entries.any { it.id == reminderId })
        val updated = entries.map { if (it.id == reminderId) it.copy(completedDateMillis = dateMillis) else it }
        check(preferences.edit().putString(key, encode(updated)).commit())
    }

    private fun decode(json: String?, carId: String): List<ServiceReminder> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ServiceReminder(item.getString("id"), carId, item.getString("title"),
                if (item.isNull("date")) null else item.getLong("date"),
                if (item.isNull("mileage")) null else item.getInt("mileage"),
                if (item.isNull("completed")) null else item.getLong("completed"))
        }
    }

    private fun encode(entries: List<ServiceReminder>): String = JSONArray().apply {
        entries.forEach { entry ->
            put(JSONObject().put("id", entry.id).put("title", entry.title)
                .put("date", entry.dueDateMillis ?: JSONObject.NULL)
                .put("mileage", entry.dueMileage ?: JSONObject.NULL)
                .put("completed", entry.completedDateMillis ?: JSONObject.NULL))
        }
    }.toString()
}
