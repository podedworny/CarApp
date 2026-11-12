package com.example.carapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.carapp.data.*
import com.example.carapp.domain.model.*
import com.example.carapp.domain.validation.ServiceDates
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ServiceRepositoryInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-service-${UUID.randomUUID()}"
    private val storage = LocalGarageStorage(context, preferenceName = name)
    private val cars = LocalCarRepository(storage)
    private val services = LocalServiceRepository(storage)
    private val first = Car(id = "first", brand = "Toyota", model = "Corolla", year = 2010, mileage = 100, registration = "")
    private val second = first.copy(id = "second")

    @After fun cleanUp() { context.deleteSharedPreferences(name) }

    @Test fun roundTripKeepsUnknownAndFreeCostsDistinctAndScopesEntries() = runBlocking {
        cars.addCar(first)
        cars.addCar(second)
        val unknown = entry("unknown", first.id, null)
        val free = entry("free", first.id, 0)
        val paid = entry("paid", second.id, 12599)
        services.addEntry(unknown)
        services.addEntry(free)
        services.addEntry(paid)
        val reopened = LocalServiceRepository(LocalGarageStorage(context, preferenceName = name))
        assertEquals(setOf(unknown, free), reopened.getEntries(first.id).toSet())
        assertEquals(listOf(paid), reopened.getEntries(second.id))
    }

    @Test fun deletingCarCascadesOnlyItsHistoryAndRejectsOrphanWrites() = runBlocking {
        cars.addCar(first)
        cars.addCar(second)
        services.addEntry(entry("a", first.id, 1))
        val retained = entry("b", second.id, 2)
        services.addEntry(retained)
        cars.deleteCar(first.id)
        assertEquals(listOf(second), cars.getCars())
        assertTrue(services.getEntries(first.id).isEmpty())
        assertEquals(listOf(retained), services.getEntries(second.id))
        assertTrue(runCatching { services.addEntry(entry("orphan", first.id, 1)) }.isFailure)
    }

    @Test fun retryWithSameEntryIdDoesNotDuplicateHistory() = runBlocking {
        cars.addCar(first)
        val entry = entry("stable", first.id, 100)
        services.addEntry(entry)
        services.addEntry(entry)
        assertEquals(listOf(entry), services.getEntries(first.id))
    }

    @Test fun remindersPersistCompletionAndCascadeWithVehicle() = runBlocking {
        cars.addCar(first)
        cars.addCar(second)
        val reminders = LocalReminderRepository(storage)
        val item = ServiceReminder(id = "reminder", carId = first.id, title = "Olej", dueMileage = 1000)
        val other = item.copy(id = "other", carId = second.id)
        reminders.saveReminder(item)
        reminders.saveReminder(item)
        reminders.saveReminder(other)
        val completed = ServiceDates.dateMillis(2026, 11, 8)
        reminders.setCompleted(first.id, item.id, completed)
        val reopened = LocalReminderRepository(LocalGarageStorage(context, preferenceName = name))
        assertEquals(listOf(item.copy(completedDateMillis = completed)), reopened.getReminders(first.id))
        reminders.setCompleted(first.id, item.id, null)
        assertEquals(listOf(item), reopened.getReminders(first.id))
        cars.deleteCar(first.id)
        assertTrue(reminders.getReminders(first.id).isEmpty())
        assertEquals(listOf(other), reminders.getReminders(second.id))
        assertTrue(runCatching { reminders.saveReminder(item) }.isFailure)
    }

    @Test fun mileageUpdatesPersistAndCannotDecrease() = runBlocking {
        cars.addCar(first)
        val updated = cars.updateMileage(first.id, 1500)
        assertEquals(1500, updated.mileage)
        val reopened = LocalCarRepository(LocalGarageStorage(context, preferenceName = name))
        assertEquals(1500, reopened.getCars().single().mileage)
        assertTrue(runCatching { cars.updateMileage(first.id, 1000) }.isFailure)
        assertEquals(1500, reopened.getCars().single().mileage)
    }

    @Test fun repeatedCarWriteDoesNotDuplicateVehicleOrLoseHistory() = runBlocking {
        cars.addCar(first)
        services.addEntry(entry("existing", first.id, 100))
        cars.addCar(first)
        assertEquals(listOf(first), cars.getCars())
        assertEquals(1, services.getEntries(first.id).size)
    }

    @Test fun legacyCarsKeepUnknownDateUntilTheirMileageIsConfirmed() = runBlocking {
        val raw = """[{"id":"legacy","brand":"Toyota","model":"Corolla","year":2010,"mileage":85000,"registration":""}]"""
        context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE).edit().putString("cars", raw).commit()
        assertNull(cars.getCars().single().mileageUpdatedDateMillis)
        val confirmedDate = ServiceDates.dateMillis(2026, 11, 8)
        val datedCars = LocalCarRepository(storage) { confirmedDate }
        datedCars.updateMileage("legacy", 85000)
        val reopened = LocalCarRepository(LocalGarageStorage(context, preferenceName = name))
        assertEquals(confirmedDate, reopened.getCars().single().mileageUpdatedDateMillis)
        assertEquals(85000, reopened.getCars().single().mileage)
    }

    private fun entry(id: String, carId: String, cost: Long?) = ServiceEntry(id, carId,
        ServiceType.MAINTENANCE, "Wymiana oleju", ServiceDates.dateMillis(2025, 1, 10), 50, cost, "Notatka")
}
