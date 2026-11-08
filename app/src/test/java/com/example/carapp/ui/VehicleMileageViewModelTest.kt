package com.example.carapp.ui

import androidx.lifecycle.SavedStateHandle
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.repository.CarRepository
import com.example.carapp.ui.vehicle.VehicleMileageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleMileageViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val car = Car(id = "a", brand = "Toyota", model = "Corolla", year = 2010, mileage = 85000, registration = "")
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `decreasing mileage is rejected without storage call`() = runTest(dispatcher) {
        val repo = Repo(car)
        val vm = VehicleMileageViewModel(car, repo, SavedStateHandle())
        vm.open(car.mileage); vm.change("84000"); vm.save(); advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertEquals(0, repo.calls)
    }
    @Test fun `successful update publishes persisted car and raises next minimum`() = runTest(dispatcher) {
        val repo = Repo(car)
        val vm = VehicleMileageViewModel(car, repo, SavedStateHandle())
        vm.open(car.mileage); vm.change("90000"); vm.save(); vm.save(); advanceUntilIdle()
        assertEquals(90000, vm.state.value.updatedCar?.mileage)
        assertFalse(vm.state.value.visible)
        assertEquals(1, repo.calls)
        vm.open(90000); vm.change("89000"); vm.save(); advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertEquals(1, repo.calls)
    }
    @Test fun `failure keeps the entered value and retry succeeds`() = runTest(dispatcher) {
        val repo = Repo(car).apply { fail = true }
        val vm = VehicleMileageViewModel(car, repo, SavedStateHandle())
        vm.open(car.mileage); vm.change("90000"); vm.save(); advanceUntilIdle()
        assertEquals("90000", vm.state.value.value)
        assertNull(vm.state.value.updatedCar)
        assertNotNull(vm.state.value.error)
        repo.fail = false
        vm.save(); advanceUntilIdle()
        assertEquals(90000, repo.car.mileage)
    }
    private class Repo(var car: Car) : CarRepository {
        var calls = 0
        var fail = false
        override suspend fun getCars() = listOf(car)
        override suspend fun addCar(car: Car) = error("Unused")
        override suspend fun deleteCar(id: String) = error("Unused")
        override suspend fun updateMileage(id: String, mileage: Int): Car {
            calls++; check(!fail)
            car = car.copy(mileage = mileage)
            return car
        }
    }
}
