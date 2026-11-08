package com.example.carapp.domain

import com.example.carapp.domain.validation.validateCar
import org.junit.Assert.*
import org.junit.Test

class CarValidatorTest {
    @Test fun `accepts zero mileage and next model year`() {
        assertTrue(validateCar("Toyota", "Corolla", "2027", "0", 2026).isValid)
    }

    @Test fun `rejects blank names and impossible numbers`() {
        val errors = validateCar("  ", "", "1885", "2147483648", 2026)
        assertTrue(errors.brand)
        assertTrue(errors.model)
        assertTrue(errors.year)
        assertTrue(errors.mileage)
    }

    @Test fun `rejects distant future year and negative mileage`() {
        val errors = validateCar("Toyota", "Corolla", "2028", "-1", 2026)
        assertTrue(errors.year)
        assertTrue(errors.mileage)
    }
}
