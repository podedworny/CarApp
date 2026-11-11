package com.example.carapp.ui

import androidx.lifecycle.SavedStateHandle
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.repository.CarRepository
import com.example.carapp.ui.addcar.CarField
import com.example.carapp.ui.garage.GarageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GarageViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `invalid input does not reach repository`() = runTest(dispatcher) {
        val repository = FakeCarRepository()
        val vm = GarageViewModel(repository, SavedStateHandle(), 2026)
        advanceUntilIdle()
        vm.openAddCar()
        vm.saveCar()
        advanceUntilIdle()
        assertTrue(repository.cars.isEmpty())
        assertTrue(vm.form.value.submitted)
        assertTrue(vm.adding.value)
    }

    @Test fun `successful save publishes car and clears form`() = runTest(dispatcher) {
        val repository = FakeCarRepository()
        val vm = GarageViewModel(repository, SavedStateHandle(), 2026)
        advanceUntilIdle()
        vm.openAddCar()
        fillForm(vm)
        vm.saveCar()
        vm.saveCar() // Rapid double tap must not create a second car.
        advanceUntilIdle()
        assertEquals(1, repository.cars.size)
        assertEquals("Toyota", vm.garage.value.cars.single().brand)
        assertFalse(vm.adding.value)
        assertEquals("", vm.form.value.brand)
    }

    @Test fun `failed save retains input and allows retry`() = runTest(dispatcher) {
        val repository = FakeCarRepository().apply { failSave = true }
        val vm = GarageViewModel(repository, SavedStateHandle(), 2026)
        advanceUntilIdle()
        vm.openAddCar()
        fillForm(vm)
        vm.saveCar()
        advanceUntilIdle()
        assertNotNull(vm.form.value.saveError)
        assertEquals(" Toyota ", vm.form.value.brand)
        assertTrue(vm.garage.value.cars.isEmpty())
        assertFalse(vm.form.value.saving)
        repository.failSave = false
        vm.saveCar()
        advanceUntilIdle()
        assertEquals(1, vm.garage.value.cars.size)
    }

    @Test fun `load failure can be retried`() = runTest(dispatcher) {
        val repository = FakeCarRepository().apply { failLoad = true }
        val vm = GarageViewModel(repository, SavedStateHandle(), 2026)
        advanceUntilIdle()
        vm.openAddCar()
        assertTrue(vm.garage.value.loadError)
        assertFalse(vm.adding.value)
        repository.failLoad = false
        vm.loadCars()
        advanceUntilIdle()
        assertFalse(vm.garage.value.loadError)
        assertFalse(vm.garage.value.loading)
    }

    @Test fun `draft is restored from saved state`() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("brand" to "Honda", "adding" to true))
        val vm = GarageViewModel(FakeCarRepository(), handle, 2026)
        advanceUntilIdle()
        assertEquals("Honda", vm.form.value.brand)
        assertTrue(vm.adding.value)
        vm.changeField(CarField.MODEL, "Civic")
        assertEquals("Civic", handle.get<String>("model"))
    }

    @Test fun `changing brand clears previous model including saved draft`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = GarageViewModel(FakeCarRepository(), handle, 2026)
        advanceUntilIdle()
        fillForm(vm)
        vm.changeField(CarField.BRAND, "Honda")
        assertEquals("", vm.form.value.model)
        assertEquals("", handle.get<String>("model"))
        vm.saveCar()
        advanceUntilIdle()
        assertTrue(vm.garage.value.cars.isEmpty())
    }

    @Test fun `deletion requires both confirmations and removes only selected id`() = runTest(dispatcher) {
        val repository = FakeCarRepository()
        val first = Car(brand = "Toyota", model = "Corolla", year = 2020, mileage = 10, registration = "")
        val second = first.copy(id = "other-car")
        repository.cars.addAll(listOf(first, second))
        val vm = GarageViewModel(repository, SavedStateHandle(), 2026)
        advanceUntilIdle()
        vm.requestDeleteCar(first.id)
        vm.confirmDeleteCar() // The final action cannot bypass step one.
        advanceUntilIdle()
        assertEquals(2, repository.cars.size)
        vm.continueDeleteCar()
        vm.confirmDeleteCar()
        vm.confirmDeleteCar() // Double tap is ignored.
        assertEquals(2, vm.garage.value.cars.size)
        advanceUntilIdle()
        assertEquals(listOf(second), repository.cars)
        assertEquals(listOf(second), vm.garage.value.cars)
        assertEquals(1, repository.deleteCalls)
        assertNull(vm.deletion.value)
    }

    @Test fun `cancelling either confirmation keeps the vehicle`() = runTest(dispatcher) {
        val repository = FakeCarRepository()
        val car = Car(brand = "Honda", model = "Civic", year = 2010, mileage = 100, registration = "")
        repository.cars += car
        val vm = GarageViewModel(repository, SavedStateHandle(), 2026)
        advanceUntilIdle()
        repeat(2) { step ->
            vm.requestDeleteCar(car.id)
            if (step == 1) vm.continueDeleteCar()
            vm.cancelDeleteCar()
            vm.confirmDeleteCar()
            advanceUntilIdle()
            assertNull(vm.deletion.value)
            assertEquals(listOf(car), repository.cars)
        }
        assertEquals(0, repository.deleteCalls)
    }

    @Test fun `failed deletion keeps vehicle and can be retried`() = runTest(dispatcher) {
        val repository = FakeCarRepository().apply { failDelete = true }
        val car = Car(brand = "Honda", model = "Civic", year = 2010, mileage = 100, registration = "")
        repository.cars += car
        val vm = GarageViewModel(repository, SavedStateHandle(), 2026)
        advanceUntilIdle()
        vm.requestDeleteCar(car.id)
        vm.continueDeleteCar()
        vm.confirmDeleteCar()
        vm.cancelDeleteCar() // Cannot dismiss an operation in progress.
        advanceUntilIdle()
        assertEquals(listOf(car), vm.garage.value.cars)
        assertNotNull(vm.deletion.value?.error)
        assertFalse(vm.deletion.value!!.deleting)
        repository.failDelete = false
        vm.confirmDeleteCar()
        advanceUntilIdle()
        assertTrue(vm.garage.value.cars.isEmpty())
        assertTrue(repository.cars.isEmpty())
        assertNull(vm.deletion.value)
    }

    @Test fun `vehicle navigation validates id and saves selection`() = runTest(dispatcher) {
        val repository = FakeCarRepository()
        val car = Car(brand = "Honda", model = "Civic", year = 2010, mileage = 100, registration = "")
        repository.cars += car
        val handle = SavedStateHandle()
        val vm = GarageViewModel(repository, handle, 2026)
        advanceUntilIdle()
        vm.openVehicle("missing")
        assertNull(vm.selectedCarId.value)
        vm.openVehicle(car.id)
        assertEquals(car.id, vm.selectedCarId.value)
        assertEquals(car.id, handle.get<String>("selectedCarId"))
        val restored = GarageViewModel(repository, SavedStateHandle(mapOf("selectedCarId" to car.id)), 2026)
        advanceUntilIdle()
        assertEquals(car.id, restored.selectedCarId.value)
        vm.closeVehicle()
        assertNull(vm.selectedCarId.value)
        assertNull(handle.get<String>("selectedCarId"))
    }

    @Test fun `deleting open vehicle returns to garage while cancelling stays on vehicle`() = runTest(dispatcher) {
        val repository = FakeCarRepository()
        val car = Car(brand = "Honda", model = "Civic", year = 2010, mileage = 100, registration = "")
        repository.cars += car
        val vm = GarageViewModel(repository, SavedStateHandle(), 2026)
        advanceUntilIdle()
        vm.openVehicle(car.id)
        vm.requestDeleteCar(car.id)
        vm.cancelDeleteCar()
        assertEquals(car.id, vm.selectedCarId.value)
        vm.requestDeleteCar(car.id)
        vm.continueDeleteCar()
        vm.confirmDeleteCar()
        advanceUntilIdle()
        assertNull(vm.selectedCarId.value)
        assertTrue(vm.garage.value.cars.isEmpty())
    }

    @Test fun `retry after uncertain save uses the same car identity`() = runTest(dispatcher) {
        val repository = FakeCarRepository().apply { failAfterWrite = true }
        val vm = GarageViewModel(repository, SavedStateHandle(), 2026)
        advanceUntilIdle()
        vm.openAddCar()
        fillForm(vm)
        vm.saveCar()
        advanceUntilIdle()
        assertNotNull(vm.form.value.saveError)
        assertEquals(1, repository.cars.size)
        val firstId = repository.cars.single().id
        repository.failAfterWrite = false
        vm.saveCar()
        advanceUntilIdle()
        assertEquals(1, repository.cars.size)
        assertEquals(firstId, repository.cars.single().id)
    }

    private fun fillForm(vm: GarageViewModel) {
        vm.changeField(CarField.BRAND, " Toyota ")
        vm.changeField(CarField.MODEL, "Corolla")
        vm.changeField(CarField.YEAR, "2020")
        vm.changeField(CarField.MILEAGE, "85000")
    }

    private class FakeCarRepository : CarRepository {
        val cars = mutableListOf<Car>()
        var failSave = false
        var failAfterWrite = false
        var failLoad = false
        var failDelete = false
        var deleteCalls = 0
        override suspend fun getCars(): List<Car> {
            check(!failLoad)
            return cars.toList()
        }
        override suspend fun updateMileage(id: String, mileage: Int): Car {
            val updated = cars.first { it.id == id }.copy(mileage = mileage)
            cars.replaceAll { if (it.id == id) updated else it }
            return updated
        }
        override suspend fun updateCar(car: Car): Car {
            val existing = cars.first { it.id == car.id }
            val updated = existing.copy(brand = car.brand, model = car.model, year = car.year, registration = car.registration)
            cars.replaceAll { if (it.id == car.id) updated else it }
            return updated
        }
        override suspend fun deleteCar(id: String) {
            deleteCalls++
            check(!failDelete)
            cars.removeAll { it.id == id }
        }
        override suspend fun addCar(car: Car) {
            check(!failSave)
            cars.removeAll { it.id == car.id }
            cars += car
            check(!failAfterWrite)
        }
    }
}
