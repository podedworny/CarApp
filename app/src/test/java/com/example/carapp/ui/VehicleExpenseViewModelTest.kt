package com.example.carapp.ui

import androidx.lifecycle.SavedStateHandle
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.model.ExpenseEntry
import com.example.carapp.domain.model.ExpenseType
import com.example.carapp.domain.model.FuelSource
import com.example.carapp.domain.repository.ExpenseRepository
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.ui.expenses.ExpenseField
import com.example.carapp.ui.expenses.VehicleExpenseViewModel
import com.example.carapp.ui.expenses.fuelSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleExpenseViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val car = Car(id = "car-a", brand = "Toyota", model = "Corolla", year = 2015, mileage = 100000, registration = "")
    private val today = ServiceDates.dateMillis(2026, 11, 11)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `entries are scoped to car and ordered newest first`() = runTest(dispatcher) {
        val repo = FakeExpenseRepository()
        repo.entries += ExpenseEntry("old", car.id, ExpenseType.FUEL, "Tankowanie", today - 86400000, 25000)
        repo.entries += ExpenseEntry("foreign", "car-b", ExpenseType.FUEL, "Tankowanie", today, 30000)
        repo.entries += ExpenseEntry("new", car.id, ExpenseType.INSURANCE, "OC", today, 120000)
        val vm = VehicleExpenseViewModel(car, repo, SavedStateHandle()) { today }
        advanceUntilIdle()
        assertEquals(listOf("new", "old"), vm.state.value.entries.map { it.id })
        vm.openForm()
        assertEquals(today, vm.state.value.form.dateMillis)
    }

    @Test fun `invalid input never reaches storage`() = runTest(dispatcher) {
        val repo = FakeExpenseRepository()
        val vm = VehicleExpenseViewModel(car, repo, SavedStateHandle()) { today }
        advanceUntilIdle()
        vm.openForm()
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.form.submitted)
        assertTrue(repo.entries.isEmpty())
    }

    @Test fun `save stores trimmed entry with precise cost once`() = runTest(dispatcher) {
        val repo = FakeExpenseRepository()
        val vm = VehicleExpenseViewModel(car, repo, SavedStateHandle()) { today }
        advanceUntilIdle()
        vm.openForm()
        vm.changeType(ExpenseType.CAR_WASH)
        vm.changeField(ExpenseField.TITLE, " Myjnia bezdotykowa ")
        vm.changeField(ExpenseField.COST, "25,50")
        vm.changeField(ExpenseField.NOTE, " Program pełny ")
        vm.save()
        vm.save()
        assertTrue(vm.state.value.entries.isEmpty())
        advanceUntilIdle()
        val saved = repo.entries.single()
        assertEquals(ExpenseType.CAR_WASH, saved.type)
        assertEquals("Myjnia bezdotykowa", saved.title)
        assertEquals(2550L, saved.costMinor)
        assertEquals("Program pełny", saved.note)
        assertEquals(car.id, saved.carId)
        assertFalse(vm.state.value.form.visible)
    }

    @Test fun `failed write preserves draft and retry keeps stable entry id`() = runTest(dispatcher) {
        val repo = FakeExpenseRepository().apply { failSave = true }
        val vm = VehicleExpenseViewModel(car, repo, SavedStateHandle()) { today }
        advanceUntilIdle()
        vm.openForm()
        vm.changeField(ExpenseField.TITLE, "Tankowanie")
        vm.changeField(ExpenseField.COST, "300")
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.state.value.form.saveError)
        assertFalse(vm.state.value.form.saving)
        repo.failSave = false
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.entries.size)
        assertEquals(1, repo.attempts.map { it.id }.distinct().size)
    }

    @Test fun `fuel suggestions adapt to car fuel sources`() {
        assertEquals(listOf("Tankowanie benzyny", "Benzyna 95", "Benzyna 98"), fuelSuggestions(setOf(FuelSource.PETROL)))
        assertEquals(listOf("Tankowanie diesla", "AdBlue"), fuelSuggestions(setOf(FuelSource.DIESEL)))
        assertEquals(listOf("Tankowanie benzyny", "Tankowanie LPG"), fuelSuggestions(setOf(FuelSource.PETROL, FuelSource.LPG)))
        assertEquals(listOf("Tankowanie benzyny", "Ładowanie EV"), fuelSuggestions(setOf(FuelSource.PETROL, FuelSource.ELECTRIC)))
        assertEquals(listOf("Ładowanie EV", "Ładowanie domowe", "Szybka ładowarka"), fuelSuggestions(setOf(FuelSource.ELECTRIC)))
    }

    @Test fun `edit expense updates in place and delete handles errors safely`() = runTest(dispatcher) {
        val repo = FakeExpenseRepository()
        val original = ExpenseEntry("exp-1", car.id, ExpenseType.FUEL, "Tankowanie", today, 25005, "Orlen")
        repo.entries += original
        val vm = VehicleExpenseViewModel(car, repo, SavedStateHandle()) { today }
        advanceUntilIdle()

        vm.openEditForm(original)
        assertTrue(vm.state.value.form.isEditing)
        assertEquals("250,05", vm.state.value.form.cost)
        vm.changeType(ExpenseType.CAR_WASH)
        vm.changeField(ExpenseField.TITLE, "Myjnia")
        vm.changeField(ExpenseField.COST, "30")
        vm.save()
        advanceUntilIdle()

        val updated = repo.entries.single()
        assertEquals("exp-1", updated.id)
        assertEquals(ExpenseType.CAR_WASH, updated.type)
        assertEquals("Myjnia", updated.title)
        assertEquals(3000L, updated.costMinor)

        repo.failDelete = true
        vm.requestDelete(updated)
        vm.confirmDelete()
        advanceUntilIdle()
        assertNotNull(vm.state.value.deleteError)
        assertEquals(1, repo.entries.size)

        repo.failDelete = false
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue(repo.entries.isEmpty())
        assertTrue(vm.state.value.entries.isEmpty())
        assertNull(vm.state.value.deletingEntry)
    }

    private class FakeExpenseRepository : ExpenseRepository {
        val entries = mutableListOf<ExpenseEntry>()
        val attempts = mutableListOf<ExpenseEntry>()
        var failSave = false
        var failLoad = false
        var failDelete = false

        override suspend fun getEntries(carId: String): List<ExpenseEntry> {
            check(!failLoad)
            return entries.filter { it.carId == carId }
        }

        override suspend fun addEntry(entry: ExpenseEntry) {
            attempts += entry
            check(!failSave)
            entries.removeAll { it.id == entry.id }
            entries += entry
        }

        override suspend fun deleteEntry(carId: String, entryId: String) {
            check(!failDelete)
            entries.removeAll { it.carId == carId && it.id == entryId }
        }
    }
}

