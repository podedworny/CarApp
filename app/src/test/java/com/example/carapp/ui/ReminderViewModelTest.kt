package com.example.carapp.ui

import androidx.lifecycle.SavedStateHandle
import com.example.carapp.domain.model.*
import com.example.carapp.domain.repository.ReminderRepository
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.ui.reminders.ReminderViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val car = Car(id = "a", brand = "Toyota", model = "Corolla", year = 2010, mileage = 85000, registration = "")
    private val today = ServiceDates.dateMillis(2026, 11, 8)
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `date-only reminder saves exactly once and has no mileage target`() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = model(repo)
        advanceUntilIdle()
        vm.openForm(car.mileage)
        vm.title(" Olej ")
        vm.save(); vm.save()
        advanceUntilIdle()
        assertEquals("Olej", repo.entries.single().title)
        assertNull(repo.entries.single().dueMileage)
        assertNotNull(repo.entries.single().dueDateMillis)
        assertFalse(vm.state.value.form.visible)
    }
    @Test fun `no thresholds blocks persistence`() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = model(repo)
        advanceUntilIdle()
        vm.openForm(car.mileage); vm.title("Olej")
        vm.useDate(false); vm.useMileage(false)
        vm.save(); advanceUntilIdle()
        assertTrue(repo.entries.isEmpty())
        assertTrue(vm.state.value.form.submitted)
    }
    @Test fun `save failure retains draft and retry has stable id`() = runTest(dispatcher) {
        val repo = FakeRepository().apply { fail = true }
        val vm = model(repo)
        advanceUntilIdle()
        vm.openForm(car.mileage); vm.title("Olej")
        vm.save(); advanceUntilIdle()
        assertNotNull(vm.state.value.form.error)
        assertEquals("Olej", vm.state.value.form.title)
        repo.fail = false
        vm.save(); advanceUntilIdle()
        assertEquals(1, repo.attempts.distinct().size)
        assertEquals(1, repo.entries.size)
    }
    @Test fun `completion failure is not optimistic and completion can be reversed`() = runTest(dispatcher) {
        val repo = FakeRepository()
        val reminder = ServiceReminder(id = "r", carId = car.id, title = "Olej", dueMileage = 85000)
        repo.entries += reminder
        val vm = model(repo)
        advanceUntilIdle()
        repo.fail = true
        vm.toggleCompleted("r"); advanceUntilIdle()
        assertNull(vm.state.value.reminders.single().completedDateMillis)
        assertNotNull(vm.state.value.error)
        repo.fail = false
        vm.toggleCompleted("r"); vm.toggleCompleted("r"); advanceUntilIdle()
        assertEquals(today, vm.state.value.reminders.single().completedDateMillis)
        vm.toggleCompleted("r"); advanceUntilIdle()
        assertNull(vm.state.value.reminders.single().completedDateMillis)
    }
    @Test fun `draft restores both enabled targets`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val repo = FakeRepository()
        val vm = ReminderViewModel(car, repo, handle) { today }
        advanceUntilIdle()
        vm.openForm(car.mileage); vm.title("Rozrząd"); vm.useMileage(true); vm.mileage("120000")
        val restored = ReminderViewModel(car, repo, handle) { today }
        advanceUntilIdle()
        assertTrue(restored.state.value.form.visible)
        assertTrue(restored.state.value.form.dateEnabled)
        assertTrue(restored.state.value.form.mileageEnabled)
        assertEquals("120000", restored.state.value.form.mileage)
    }
    @Test fun `older load cannot replace the result of a newer retry`() = runTest(dispatcher) {
        val pending = mutableListOf<CompletableDeferred<List<ServiceReminder>>>()
        val repo = object : ReminderRepository {
            override suspend fun getReminders(carId: String): List<ServiceReminder> {
                val result = CompletableDeferred<List<ServiceReminder>>()
                pending += result
                return result.await()
            }
            override suspend fun saveReminder(reminder: ServiceReminder) = Unit
            override suspend fun setCompleted(carId: String, reminderId: String, dateMillis: Long?) = Unit
        }
        val vm = model(repo)
        runCurrent()
        vm.load()
        runCurrent()
        val latest = ServiceReminder(id = "latest", carId = car.id, title = "Nowy", dueMileage = 90000)
        pending[1].complete(listOf(latest))
        runCurrent()
        pending[0].complete(emptyList())
        advanceUntilIdle()
        assertEquals(listOf(latest), vm.state.value.reminders)
    }

    private fun model(repo: ReminderRepository) = ReminderViewModel(car, repo, SavedStateHandle()) { today }
    private class FakeRepository : ReminderRepository {
        val entries = mutableListOf<ServiceReminder>()
        val attempts = mutableListOf<String>()
        var fail = false
        override suspend fun getReminders(carId: String) = entries.filter { it.carId == carId }
        override suspend fun saveReminder(reminder: ServiceReminder) {
            attempts += reminder.id
            check(!fail)
            entries.removeAll { it.id == reminder.id }; entries += reminder
        }
        override suspend fun setCompleted(carId: String, reminderId: String, dateMillis: Long?) {
            check(!fail)
            entries.replaceAll { if (it.id == reminderId && it.carId == carId) it.copy(completedDateMillis = dateMillis) else it }
        }
    }
}
