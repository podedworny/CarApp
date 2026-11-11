package com.example.carapp.domain

import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.domain.validation.validateExpense
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseValidatorTest {
    private val today = ServiceDates.dateMillis(2026, 11, 11)

    @Test fun `valid expense with Polish decimal passes`() {
        assertTrue(validateExpense("Tankowanie", today, "285,50", 2015, today).isValid)
    }

    @Test fun `blank cost, invalid title and out-of-range dates are rejected`() {
        val errors = validateExpense(" ", today + 86400000, "", 2015, today)
        assertFalse(errors.isValid)
        assertTrue(errors.title)
        assertTrue(errors.date)
        assertTrue(errors.cost)
        assertTrue(validateExpense("OC", ServiceDates.dateMillis(2014, 12, 31), "900", 2015, today).date)
        assertTrue(validateExpense("OC", today, "12,345", 2015, today).cost)
    }
}

