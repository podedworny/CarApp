package com.example.carapp.ui

import androidx.lifecycle.SavedStateHandle
import com.example.carapp.domain.model.*
import com.example.carapp.domain.repository.ServiceRepository
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.ui.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleServiceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val car = Car(id = "car-a", brand = "Toyota", model = "Corolla", year = 2010, mileage = 100000, registration = "")
    private val today = ServiceDates.dateMillis(2026, 11, 8)
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `entries are scoped to car and ordered newest first`() = runTest(dispatcher) {
        val repo = FakeServiceRepository()
        repo.entries += entry("old", car.id, today - 86400000)
        repo.entries += entry("foreign", "car-b", today)
        repo.entries += entry("new", car.id, today)
        val vm = model(repo)
        advanceUntilIdle()
        assertEquals(listOf("new", "old"), vm.state.value.entries.map { it.id })
        vm.openForm()
        assertEquals("100000", vm.state.value.form.mileage)
        assertEquals(today, vm.state.value.form.dateMillis)
    }

    @Test fun `invalid input never reaches storage`() = runTest(dispatcher) {
        val repo = FakeServiceRepository()
        val vm = model(repo)
        advanceUntilIdle()
        vm.openForm()
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.form.submitted)
        assertTrue(repo.entries.isEmpty())
    }

    @Test fun `save captures historical mileage and precise cost once`() = runTest(dispatcher) {
        val repo = FakeServiceRepository()
        val vm = model(repo)
        advanceUntilIdle()
        vm.openForm()
        vm.changeField(ServiceField.TITLE, " Wymiana oleju ")
        vm.changeField(ServiceField.MILEAGE, "50000")
        vm.changeField(ServiceField.COST, "450,35")
        vm.save()
        vm.save()
        assertTrue(vm.state.value.entries.isEmpty())
        advanceUntilIdle()
        val saved = repo.entries.single()
        assertEquals("Wymiana oleju", saved.title)
        assertEquals(45035L, saved.costMinor)
        assertEquals(50000, saved.mileage)
        assertEquals(car.id, saved.carId)
        assertFalse(vm.state.value.form.visible)
        assertEquals(listOf(saved), vm.state.value.entries)
    }

    @Test fun `failed write preserves draft and retry keeps stable entry id`() = runTest(dispatcher) {
        val repo = FakeServiceRepository().apply { failSave = true }
        val vm = model(repo)
        advanceUntilIdle()
        vm.openForm()
        vm.changeField(ServiceField.TITLE, "Przegląd")
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.state.value.form.saveError)
        assertEquals("Przegląd", vm.state.value.form.title)
        assertFalse(vm.state.value.form.saving)
        assertTrue(vm.state.value.entries.isEmpty())
        repo.failSave = false
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.entries.size)
        assertEquals(1, repo.attempts.map { it.id }.distinct().size)
    }

    @Test fun `draft survives close and saved state restoration`() = runTest(dispatcher) {
        val repo = FakeServiceRepository()
        val handle = SavedStateHandle()
        val vm = VehicleServiceViewModel(car, repo, handle) { today }
        advanceUntilIdle()
        vm.openForm()
        vm.changeField(ServiceField.TITLE, "Rozrząd")
        vm.changeField(ServiceField.NOTE, "Warsztat")
        vm.closeForm()
        vm.openForm()
        assertEquals("Rozrząd", vm.state.value.form.title)
        val restored = VehicleServiceViewModel(car, repo, handle) { today }
        advanceUntilIdle()
        assertTrue(restored.state.value.form.visible)
        assertEquals("Warsztat", restored.state.value.form.note)
    }

    @Test fun `load error blocks writing and supports retry`() = runTest(dispatcher) {
        val repo = FakeServiceRepository().apply { failLoad = true }
        val vm = model(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value.loadError)
        vm.openForm()
        assertFalse(vm.state.value.form.visible)
        repo.failLoad = false
        vm.load()
        advanceUntilIdle()
        vm.openForm()
        assertTrue(vm.state.value.form.visible)
    }

    @Test fun `only a successfully saved higher reading creates a suggestion`() = runTest(dispatcher) {
        val repo = FakeServiceRepository().apply { failSave = true }
        val vm = model(repo)
        advanceUntilIdle()
        vm.openForm()
        vm.changeField(ServiceField.TITLE, "Serwis")
        vm.changeField(ServiceField.MILEAGE, "110000")
        vm.save(currentMileage = 100000)
        advanceUntilIdle()
        assertNull(vm.state.value.suggestedMileage)
        repo.failSave = false
        vm.save(currentMileage = 100000)
        advanceUntilIdle()
        assertEquals(110000, vm.state.value.suggestedMileage)
        vm.dismissMileageSuggestion()
        assertNull(vm.state.value.suggestedMileage)
        assertEquals(1, vm.state.value.entries.size)
    }

    @Test fun `suggestion uses current car reading rather than initial snapshot`() = runTest(dispatcher) {
        val vm = model(FakeServiceRepository())
        advanceUntilIdle()
        vm.openForm(currentMileage = 150000)
        vm.changeField(ServiceField.TITLE, "Starszy wpis")
        vm.changeField(ServiceField.MILEAGE, "120000")
        vm.save(currentMileage = 150000)
        advanceUntilIdle()
        assertNull(vm.state.value.suggestedMileage)
    }

    @Test fun `pending mileage suggestion survives saved state restoration`() = runTest(dispatcher) {
        val repo = FakeServiceRepository()
        val handle = SavedStateHandle()
        val vm = VehicleServiceViewModel(car, repo, handle) { today }
        advanceUntilIdle()
        vm.openForm()
        vm.changeField(ServiceField.TITLE, "Serwis")
        vm.changeField(ServiceField.MILEAGE, "110000")
        vm.save()
        advanceUntilIdle()
        val restored = VehicleServiceViewModel(car, repo, handle) { today }
        advanceUntilIdle()
        assertEquals(110000, restored.state.value.suggestedMileage)
        restored.dismissMileageSuggestion()
        assertNull(handle.get<Int>("suggestedMileage"))
    }

    private fun model(repo: ServiceRepository) = VehicleServiceViewModel(car, repo, SavedStateHandle()) { today }
    private fun entry(id: String, carId: String, date: Long) = ServiceEntry(id, carId,
        ServiceType.MAINTENANCE, "Serwis", date, 50000, null)

    private class FakeServiceRepository : ServiceRepository {
        val entries = mutableListOf<ServiceEntry>()
        val attempts = mutableListOf<ServiceEntry>()
        var failSave = false
        var failLoad = false
        override suspend fun getEntries(carId: String): List<ServiceEntry> {
            check(!failLoad)
            return entries.filter { it.carId == carId }
        }
        override suspend fun addEntry(entry: ServiceEntry) {
            attempts += entry
            check(!failSave)
            entries.removeAll { it.id == entry.id }
            entries += entry
        }
    }
}
