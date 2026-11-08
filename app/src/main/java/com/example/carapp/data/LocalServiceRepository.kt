package com.example.carapp.data

import com.example.carapp.domain.model.ServiceEntry
import com.example.carapp.domain.model.ServiceType
import com.example.carapp.domain.repository.ServiceRepository
import org.json.JSONArray
import org.json.JSONObject

class LocalServiceRepository(private val storage: LocalGarageStorage) : ServiceRepository {
    override suspend fun getEntries(carId: String): List<ServiceEntry> = storage.access { preferences ->
        decode(preferences.getString(LocalGarageStorage.serviceKey(carId), "[]"), carId)
    }

    override suspend fun addEntry(entry: ServiceEntry): Unit = storage.access { preferences ->
        val cars = JSONArray(preferences.getString("cars", "[]"))
        check((0 until cars.length()).any { cars.getJSONObject(it).getString("id") == entry.carId }) {
            "Vehicle no longer exists"
        }
        val key = LocalGarageStorage.serviceKey(entry.carId)
        val entries = decode(preferences.getString(key, "[]"), entry.carId)
        // Stable entry IDs make retrying a write idempotent.
        val updated = entries.filterNot { it.id == entry.id } + entry
        val json = JSONArray().apply {
            updated.forEach { item ->
                put(JSONObject().put("id", item.id).put("type", item.type.name)
                    .put("title", item.title).put("dateMillis", item.dateMillis)
                    .put("mileage", item.mileage).put("costMinor", item.costMinor ?: JSONObject.NULL)
                    .put("note", item.note))
            }
        }
        check(preferences.edit().putString(key, json.toString()).commit()) { "Cannot save service" }
    }

    private fun decode(json: String?, carId: String): List<ServiceEntry> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ServiceEntry(item.getString("id"), carId, ServiceType.valueOf(item.getString("type")),
                item.getString("title"), item.getLong("dateMillis"), item.getInt("mileage"),
                if (item.isNull("costMinor")) null else item.getLong("costMinor"), item.getString("note"))
        }.sortedWith(compareByDescending<ServiceEntry> { it.dateMillis }.thenByDescending { it.mileage })
    }
}
