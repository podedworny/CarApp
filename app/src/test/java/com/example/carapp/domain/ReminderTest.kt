package com.example.carapp.domain

import com.example.carapp.domain.model.*
import com.example.carapp.domain.validation.*
import org.junit.Assert.*
import org.junit.Test

class ReminderTest {
    private val today = ServiceDates.dateMillis(2026, 11, 8)
    private fun entry(date: Long? = null, mileage: Int? = null) = ServiceReminder(carId = "a", title = "Olej", dueDateMillis = date, dueMileage = mileage)

    @Test fun `either reached threshold triggers reminder`() {
        assertEquals(ReminderStatus.OVERDUE, entry(today + 90L * 86400000, 90000).status(today, 95000))
        assertEquals(ReminderStatus.OVERDUE, entry(today - 86400000, 200000).status(today, 95000))
        assertEquals(ReminderStatus.DUE, entry(today, 200000).status(today, 95000))
        assertEquals(ReminderStatus.DUE, entry(mileage = 95000).status(today, 95000))
    }
    @Test fun `upcoming and distant thresholds use correct boundaries`() {
        assertEquals(ReminderStatus.UPCOMING, entry(today + 30L * 86400000).status(today, 0))
        assertEquals(ReminderStatus.PLANNED, entry(today + 31L * 86400000).status(today, 0))
        assertEquals(ReminderStatus.UPCOMING, entry(mileage = 1000).status(today, 0))
        assertEquals(ReminderStatus.PLANNED, entry(mileage = Int.MAX_VALUE).status(today, 0))
        assertEquals(ReminderStatus.COMPLETED, entry(today - 86400000).copy(completedDateMillis = today).status(today, 0))
    }
    @Test fun `at least one valid target is required`() {
        assertFalse(validateReminder("Olej", false, null, false, "", 2010).isValid)
        assertTrue(validateReminder("Olej", true, today, false, "", 2010).isValid)
        assertTrue(validateReminder("Olej", false, null, true, "0", 2010).isValid)
        assertFalse(validateReminder("Olej", false, null, true, "2147483648", 2010).isValid)
        assertFalse(validateReminder("", true, today, false, "", 2010).isValid)
    }
}
