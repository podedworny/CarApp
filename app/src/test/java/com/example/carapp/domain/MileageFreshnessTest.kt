package com.example.carapp.domain

import com.example.carapp.domain.model.Car
import com.example.carapp.domain.model.hasStaleMileage
import com.example.carapp.domain.validation.ServiceDates
import org.junit.Assert.*
import org.junit.Test

class MileageFreshnessTest {
    private val today = ServiceDates.dateMillis(2026, 11, 8)
    private val car = Car(brand = "Toyota", model = "Corolla", year = 2010, mileage = 85000, registration = "")

    @Test fun `unknown dates and readings at least thirty days old are stale`() {
        assertTrue(car.hasStaleMileage(today))
        assertTrue(car.copy(mileageUpdatedDateMillis = today - 30L * 86400000).hasStaleMileage(today))
        assertFalse(car.copy(mileageUpdatedDateMillis = today - 29L * 86400000).hasStaleMileage(today))
        assertFalse(car.copy(mileageUpdatedDateMillis = today).hasStaleMileage(today))
    }
}
