package com.example.carapp.data

import com.example.carapp.domain.model.ExpenseEntry
import com.example.carapp.domain.model.ExpenseType
import com.example.carapp.domain.repository.ExpenseRepository
import org.json.JSONArray
import org.json.JSONObject

class LocalExpenseRepository(private val storage: LocalGarageStorage) : ExpenseRepository {
    override suspend fun getEntries(carId: String): List<ExpenseEntry> = storage.access { preferences ->
        decode(preferences.getString(LocalGarageStorage.expenseKey(carId), "[]"), carId)
    }

    override suspend fun addEntry(entry: ExpenseEntry): Unit = storage.access { preferences ->
        val cars = JSONArray(preferences.getString("cars", "[]"))
        check((0 until cars.length()).any { cars.getJSONObject(it).getString("id") == entry.carId }) {
            "Vehicle no longer exists"
        }
        val key = LocalGarageStorage.expenseKey(entry.carId)
        val entries = decode(preferences.getString(key, "[]"), entry.carId)
        val updated = entries.filterNot { it.id == entry.id } + entry
        check(preferences.edit().putString(key, encode(updated)).commit()) { "Cannot save expense" }
    }

    override suspend fun deleteEntry(carId: String, entryId: String): Unit = storage.access { preferences ->
        val key = LocalGarageStorage.expenseKey(carId)
        val entries = decode(preferences.getString(key, "[]"), carId)
        val updated = entries.filterNot { it.id == entryId }
        check(preferences.edit().putString(key, encode(updated)).commit()) { "Cannot delete expense" }
    }

    private fun encode(entries: List<ExpenseEntry>): String = JSONArray().apply {
        entries.forEach { item ->
            put(JSONObject().put("id", item.id).put("type", item.type.name)
                .put("title", item.title).put("dateMillis", item.dateMillis)
                .put("costMinor", item.costMinor).put("note", item.note))
        }
    }.toString()

    private fun decode(json: String?, carId: String): List<ExpenseEntry> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val type = runCatching { ExpenseType.valueOf(item.getString("type")) }.getOrDefault(ExpenseType.OTHER)
            ExpenseEntry(item.getString("id"), carId, type,
                item.getString("title"), item.getLong("dateMillis"), item.getLong("costMinor"),
                item.getString("note"))
        }.sortedByDescending { it.dateMillis }
    }
}
