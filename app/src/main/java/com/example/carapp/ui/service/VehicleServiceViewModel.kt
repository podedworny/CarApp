package com.example.carapp.ui.service

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.model.ServiceEntry
import com.example.carapp.domain.model.ServiceType
import com.example.carapp.domain.repository.ServiceRepository
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.domain.validation.parseCostMinor
import com.example.carapp.domain.validation.validateService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class VehicleServiceViewModel(
    private val car: Car,
    private val repository: ServiceRepository,
    private val savedState: SavedStateHandle,
    private val today: () -> Long = ServiceDates::todayMillis
) : ViewModel() {
    private val _state = MutableStateFlow(VehicleServiceState(form = ServiceFormState(
        visible = savedState["visible"] ?: false,
        type = ServiceType.valueOf(savedState["type"] ?: ServiceType.MAINTENANCE.name),
        title = savedState["title"] ?: "",
        dateMillis = savedState["date"],
        mileage = savedState["mileage"] ?: "",
        cost = savedState["cost"] ?: "",
        note = savedState["note"] ?: ""
    )))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load() }

    fun load() {
        if (_state.value.form.saving) return
        _state.value = _state.value.copy(loading = true, loadError = false)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val entries = repository.getEntries(car.id)
                _state.value = _state.value.copy(entries = entries.sortedByDescending { it.dateMillis }, loading = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(loading = false, loadError = true) }
        }
    }

    fun openForm(currentMileage: Int = car.mileage) {
        if (_state.value.loading || _state.value.loadError || _state.value.form.saving) return
        val previous = _state.value.form
        savedState["entryId"] = savedState.get<String>("entryId") ?: UUID.randomUUID().toString()
        updateForm(previous.copy(visible = true,
            dateMillis = previous.dateMillis ?: today(),
            mileage = previous.mileage.ifBlank {
                maxOf(currentMileage, _state.value.entries.maxOfOrNull { it.mileage } ?: 0).toString()
            }, saveError = null))
    }

    /** Closing keeps an unfinished draft, so accidental navigation does not discard entered data. */
    fun closeForm() {
        if (!_state.value.form.saving) updateForm(_state.value.form.copy(visible = false))
    }

    fun changeField(field: ServiceField, value: String) {
        val form = _state.value.form
        if (form.saving) return
        val updated = when (field) {
            ServiceField.TITLE -> form.copy(title = value.take(100))
            ServiceField.MILEAGE -> form.copy(mileage = value.filter { it in '0'..'9' }.take(10))
            ServiceField.COST -> form.copy(cost = value.take(12))
            ServiceField.NOTE -> form.copy(note = value.take(1000))
        }
        updateForm(updated.copy(saveError = null))
    }

    fun changeType(type: ServiceType) {
        if (!_state.value.form.saving) updateForm(_state.value.form.copy(type = type, saveError = null))
    }

    fun changeDate(dateMillis: Long) {
        if (!_state.value.form.saving) updateForm(_state.value.form.copy(dateMillis = dateMillis, saveError = null))
    }

    fun save() {
        val input = _state.value.form
        if (!input.visible || input.saving || _state.value.loading || _state.value.loadError) return
        updateForm(input.copy(submitted = true, saveError = null))
        val errors = validateService(input.title, input.dateMillis, input.mileage, input.cost, car.year, today())
        if (!errors.isValid) return
        val id = savedState.get<String>("entryId") ?: UUID.randomUUID().toString().also { savedState["entryId"] = it }
        val entry = ServiceEntry(id = id, carId = car.id, type = input.type, title = input.title.trim(),
            dateMillis = input.dateMillis!!, mileage = input.mileage.toInt(),
            costMinor = parseCostMinor(input.cost), note = input.note.trim())
        updateForm(_state.value.form.copy(saving = true))
        viewModelScope.launch {
            try {
                repository.addEntry(entry)
                val entries = (_state.value.entries.filterNot { it.id == entry.id } + entry)
                    .sortedWith(compareByDescending<ServiceEntry> { it.dateMillis }.thenByDescending { it.mileage })
                _state.value = _state.value.copy(entries = entries)
                savedState.remove<String>("entryId")
                updateForm(ServiceFormState())
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                updateForm(_state.value.form.copy(saving = false,
                    saveError = "Nie udało się zapisać wpisu. Dane pozostają w formularzu — spróbuj ponownie."))
            }
        }
    }

    private fun updateForm(form: ServiceFormState) {
        _state.value = _state.value.copy(form = form)
        savedState["visible"] = form.visible
        savedState["type"] = form.type.name
        savedState["title"] = form.title
        savedState["date"] = form.dateMillis
        savedState["mileage"] = form.mileage
        savedState["cost"] = form.cost
        savedState["note"] = form.note
    }

    class Factory(private val car: Car, private val repository: ServiceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(VehicleServiceViewModel::class.java))
            return VehicleServiceViewModel(car, repository, extras.createSavedStateHandle()) as T
        }
    }
}
