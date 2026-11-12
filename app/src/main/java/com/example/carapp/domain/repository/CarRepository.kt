package com.example.carapp.domain.repository
import com.example.carapp.domain.model.Car

interface CarRepository {
    suspend fun getCars(): List<Car>
    /** Repeating a write with the same ID replaces that car rather than creating a duplicate. */
    suspend fun addCar(car: Car)
    suspend fun updateMileage(id: String, mileage: Int): Car
    /** Replaces all editable fields of an existing car (brand, model, year, registration). */
    suspend fun updateCar(car: Car): Car
    suspend fun deleteCar(id: String)
}
