package com.example.carapp.domain

import com.example.carapp.domain.validation.*
import org.junit.Assert.*
import org.junit.Test

class ServiceValidatorTest {
    private val today = ServiceDates.dateMillis(2026, 11, 8)

    @Test fun `Polish amounts are exact and missing differs from free`() {
        assertEquals(123456L, parseCostMinor("1234,56"))
        assertEquals(10L, parseCostMinor("0.10"))
        assertEquals(0L, parseCostMinor("0"))
        assertNull(parseCostMinor(""))
        listOf("-1", "1,234", "1.1.1", "abc", "100000000", "NaN").forEach { assertNull(parseCostMinor(it)) }
    }

    @Test fun `historical mileage and unknown cost are valid`() {
        assertTrue(validateService("Wymiana oleju", ServiceDates.dateMillis(2015, 5, 2),
            "50000", "", 2010, today).isValid)
    }

    @Test fun `invalid fields and future dates are rejected`() {
        val errors = validateService(" ", today + 86400000, "2147483648", "1,001", 2010, today)
        assertTrue(errors.title)
        assertTrue(errors.date)
        assertTrue(errors.mileage)
        assertTrue(errors.cost)
        assertTrue(validateService("Serwis", ServiceDates.dateMillis(2009, 12, 31), "0", "0", 2010, today).date)
    }
}
