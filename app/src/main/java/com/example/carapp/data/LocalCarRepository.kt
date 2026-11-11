package com.example.carapp.data

import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.model.FuelSource
import com.example.carapp.domain.repository.CarRepository
import org.json.JSONArray
import org.json.JSONObject

class LocalCarRepository(private val storage: LocalGarageStorage, private val today: () -> Long = ServiceDates::todayMillis) : CarRepository {
    override suspend fun getCars(): List<Car> = storage.access { preferences ->
        decodeCars(preferences.getString("cars", "[]"))
    }

    override suspend fun addCar(car: Car): Unit = storage.access { preferences ->
        val cars = decodeCars(preferences.getString("cars", "[]"))
        check(preferences.edit().putString("cars", encodeCars(cars.filterNot { it.id == car.id } + car)).commit()) { "Cannot save garage" }
    }

    override suspend fun updateMileage(id: String, mileage: Int): Car = storage.access { preferences ->
        val cars = decodeCars(preferences.getString("cars", "[]"))
        val car = cars.firstOrNull { it.id == id } ?: error("Vehicle no longer exists")
        require(mileage >= car.mileage) { "Mileage cannot decrease" }
        val updated = car.copy(mileage = mileage, mileageUpdatedDateMillis = today())
        check(preferences.edit().putString("cars", encodeCars(cars.map { if (it.id == id) updated else it })).commit())
        updated
    }

    override suspend fun updateCar(car: Car): Car = storage.access { preferences ->
        val cars = decodeCars(preferences.getString("cars", "[]"))
        val existing = cars.firstOrNull { it.id == car.id } ?: error("Vehicle no longer exists")
        // Preserve the current mileage and mileageUpdatedDateMillis – those are updated via updateMileage only.
        val updated = existing.copy(
            brand = car.brand,
            model = car.model,
            year = car.year,
            registration = car.registration,
            fuelSources = car.fuelSources
        )
        check(preferences.edit().putString("cars", encodeCars(cars.map { if (it.id == car.id) updated else it })).commit())
        updated
    }

    override suspend fun deleteCar(id: String): Unit = storage.access { preferences ->
        val cars = decodeCars(preferences.getString("cars", "[]"))
        // Remove the car and its service history and expenses in the same persisted transaction.
        check(preferences.edit().putString("cars", encodeCars(cars.filterNot { it.id == id }))
            .remove(LocalGarageStorage.serviceKey(id)).remove(LocalGarageStorage.reminderKey(id))
            .remove(LocalGarageStorage.expenseKey(id)).commit()) { "Cannot delete car" }
    }

    private fun decodeCars(json: String?): List<Car> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val sourcesArray = item.optJSONArray("fuelSources")
            val fuelSources = if (sourcesArray == null) {
                emptySet()
            } else {
                buildSet {
                    for (i in 0 until sourcesArray.length()) {
                        runCatching { FuelSource.valueOf(sourcesArray.getString(i)) }.getOrNull()?.let(::add)
                    }
                }
            }
            Car(item.getString("id"), item.getString("brand"), item.getString("model"),
                item.getInt("year"), item.getInt("mileage"), item.getString("registration"),
                if (item.isNull("mileageUpdatedDateMillis")) null else item.getLong("mileageUpdatedDateMillis"),
                fuelSources)
        }
    }

    private fun encodeCars(cars: List<Car>): String = JSONArray().apply {
        cars.forEach { car ->
            put(JSONObject().put("id", car.id).put("brand", car.brand).put("model", car.model)
                .put("year", car.year).put("mileage", car.mileage).put("registration", car.registration)
                .put("mileageUpdatedDateMillis", car.mileageUpdatedDateMillis ?: JSONObject.NULL)
                .put("fuelSources", JSONArray(car.fuelSources.map { it.name })))
        }
    }.toString()
}
