package com.example.carapp.ui.reminders

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.model.ServiceReminder
import com.example.carapp.domain.repository.ReminderRepository
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.domain.validation.validateReminder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ReminderViewModel(private val car: Car, private val repository: ReminderRepository,
                        private val saved: SavedStateHandle,
                        private val today: () -> Long = ServiceDates::todayMillis) : ViewModel() {
    private val _state = MutableStateFlow(ReminderState(form = ReminderFormState(
        visible = saved["visible"] ?: false, title = saved["title"] ?: "",
        dateEnabled = saved["dateEnabled"] ?: true, dateMillis = saved["date"],
        mileageEnabled = saved["mileageEnabled"] ?: false, mileage = saved["mileage"] ?: ""
    )))
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        if (_state.value.form.saving || _state.value.updatingId != null) return
        _state.value = _state.value.copy(loading = true, loadError = false)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try { _state.value = _state.value.copy(reminders = repository.getReminders(car.id), loading = false) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(loading = false, loadError = true) }
        }
    }

    fun openForm(currentMileage: Int) {
        if (_state.value.loading || _state.value.loadError || _state.value.updatingId != null) return
        saved["entryId"] = saved.get<String>("entryId") ?: UUID.randomUUID().toString()
        form(_state.value.form.copy(visible = true, error = null,
            dateMillis = _state.value.form.dateMillis ?: today() + 30L * 86400000,
            mileage = _state.value.form.mileage.ifBlank { (currentMileage.toLong() + 10000).coerceAtMost(Int.MAX_VALUE.toLong()).toString() }))
    }

    fun closeForm() { if (!_state.value.form.saving) form(_state.value.form.copy(visible = false)) }
    fun title(value: String) = edit { it.copy(title = value.take(100)) }
    fun mileage(value: String) = edit { it.copy(mileage = value.filter { digit -> digit in '0'..'9' }.take(10)) }
    fun date(value: Long) = edit { it.copy(dateMillis = value) }
    fun useDate(value: Boolean) = edit { it.copy(dateEnabled = value) }
    fun useMileage(value: Boolean) = edit { it.copy(mileageEnabled = value) }

    fun save() {
        val input = _state.value.form
        if (!input.visible || input.saving || _state.value.loading || _state.value.loadError) return
        form(input.copy(submitted = true, error = null))
        if (!validateReminder(input.title, input.dateEnabled, input.dateMillis, input.mileageEnabled, input.mileage, car.year).isValid) return
        val entry = ServiceReminder(id = saved.get<String>("entryId") ?: return, carId = car.id,
            title = input.title.trim(), dueDateMillis = input.dateMillis.takeIf { input.dateEnabled },
            dueMileage = if (input.mileageEnabled) input.mileage.toInt() else null)
        form(_state.value.form.copy(saving = true))
        viewModelScope.launch {
            try {
                repository.saveReminder(entry)
                _state.value = _state.value.copy(reminders = _state.value.reminders.filterNot { it.id == entry.id } + entry)
                saved.remove<String>("entryId")
                form(ReminderFormState())
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { form(_state.value.form.copy(saving = false, error = "Nie udało się zapisać przypomnienia. Spróbuj ponownie.")) }
        }
    }

    fun toggleCompleted(id: String) {
        if (_state.value.updatingId != null || _state.value.loading || _state.value.loadError || _state.value.form.visible) return
        val reminder = _state.value.reminders.firstOrNull { it.id == id } ?: return
        val completed = if (reminder.completedDateMillis == null) today() else null
        _state.value = _state.value.copy(updatingId = id, error = null)
        viewModelScope.launch {
            try {
                repository.setCompleted(car.id, id, completed)
                _state.value = _state.value.copy(updatingId = null, reminders = _state.value.reminders.map {
                    if (it.id == id) it.copy(completedDateMillis = completed) else it
                })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(updatingId = null, error = "Nie udało się zmienić statusu. Spróbuj ponownie.") }
        }
    }

    private fun edit(change: (ReminderFormState) -> ReminderFormState) {
        if (!_state.value.form.saving) form(change(_state.value.form).copy(error = null))
    }
    private fun form(value: ReminderFormState) {
        _state.value = _state.value.copy(form = value)
        saved["visible"] = value.visible
        saved["title"] = value.title
        saved["dateEnabled"] = value.dateEnabled
        saved["date"] = value.dateMillis
        saved["mileageEnabled"] = value.mileageEnabled
        saved["mileage"] = value.mileage
    }

    class Factory(private val car: Car, private val repository: ReminderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(ReminderViewModel::class.java))
            return ReminderViewModel(car, repository, extras.createSavedStateHandle()) as T
        }
    }
}
