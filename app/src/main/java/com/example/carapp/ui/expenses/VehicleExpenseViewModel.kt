package com.example.carapp.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.model.ExpenseEntry
import com.example.carapp.domain.model.ExpenseType
import com.example.carapp.domain.repository.ExpenseRepository
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.domain.validation.parseCostMinor
import com.example.carapp.domain.validation.validateExpense
import com.example.carapp.ui.service.formatCostInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class VehicleExpenseViewModel(
    private val car: Car,
    private val repository: ExpenseRepository,
    private val savedState: SavedStateHandle,
    private val today: () -> Long = ServiceDates::todayMillis
) : ViewModel() {
    private val _state = MutableStateFlow(VehicleExpenseState(form = ExpenseFormState(
        visible = savedState["visible"] ?: false,
        editingEntryId = savedState["editingEntryId"],
        type = ExpenseType.valueOf(savedState["type"] ?: ExpenseType.FUEL.name),
        title = savedState["title"] ?: "",
        dateMillis = savedState["date"],
        cost = savedState["cost"] ?: "",
        note = savedState["note"] ?: ""
    )))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load() }

    fun load() {
        if (_state.value.form.saving || _state.value.deleting) return
        _state.value = _state.value.copy(loading = true, loadError = false)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val entries = repository.getEntries(car.id)
                _state.value = _state.value.copy(entries = entries.sortedByDescending { it.dateMillis }, loading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false, loadError = true)
            }
        }
    }

    fun openForm(initialType: ExpenseType? = null) {
        if (_state.value.loading || _state.value.loadError || _state.value.form.saving) return
        val previous = if (_state.value.form.isEditing) ExpenseFormState() else _state.value.form
        savedState["entryId"] = if (_state.value.form.isEditing) {
            UUID.randomUUID().toString()
        } else {
            savedState.get<String>("entryId") ?: UUID.randomUUID().toString()
        }
        updateForm(previous.copy(
            visible = true,
            type = initialType ?: previous.type,
            dateMillis = previous.dateMillis ?: today(),
            saveError = null
        ))
    }

    fun openEditForm(entry: ExpenseEntry) {
        if (_state.value.loading || _state.value.loadError || _state.value.form.saving || _state.value.deleting) return
        savedState["entryId"] = entry.id
        updateForm(ExpenseFormState(
            visible = true,
            editingEntryId = entry.id,
            type = entry.type,
            title = entry.title,
            dateMillis = entry.dateMillis,
            cost = formatCostInput(entry.costMinor),
            note = entry.note
        ))
    }

    /** Closing keeps an unfinished new-entry draft, while cancelling an edit resets the form cleanly. */
    fun closeForm() {
        if (_state.value.form.saving) return
        if (_state.value.form.isEditing) {
            savedState.remove<String>("entryId")
            updateForm(ExpenseFormState())
        } else {
            updateForm(_state.value.form.copy(visible = false))
        }
    }

    fun changeField(field: ExpenseField, value: String) {
        val form = _state.value.form
        if (form.saving) return
        val updated = when (field) {
            ExpenseField.TITLE -> form.copy(title = value.take(100))
            ExpenseField.COST -> form.copy(cost = value.take(12))
            ExpenseField.NOTE -> form.copy(note = value.take(1000))
        }
        updateForm(updated.copy(saveError = null))
    }

    fun changeType(type: ExpenseType) {
        if (!_state.value.form.saving) updateForm(_state.value.form.copy(type = type, saveError = null))
    }

    fun changeDate(dateMillis: Long) {
        if (!_state.value.form.saving) updateForm(_state.value.form.copy(dateMillis = dateMillis, saveError = null))
    }

    fun save() {
        val input = _state.value.form
        if (!input.visible || input.saving || _state.value.loading || _state.value.loadError) return
        updateForm(input.copy(submitted = true, saveError = null))
        val errors = validateExpense(input.title, input.dateMillis, input.cost, car.year, today())
        val costMinor = parseCostMinor(input.cost)
        if (!errors.isValid || costMinor == null) return
        val id = input.editingEntryId
            ?: savedState.get<String>("entryId")
            ?: UUID.randomUUID().toString().also { savedState["entryId"] = it }
        val entry = ExpenseEntry(
            id = id,
            carId = car.id,
            type = input.type,
            title = input.title.trim(),
            dateMillis = input.dateMillis!!,
            costMinor = costMinor,
            note = input.note.trim()
        )
        updateForm(_state.value.form.copy(saving = true))
        viewModelScope.launch {
            try {
                repository.addEntry(entry)
                val entries = (_state.value.entries.filterNot { it.id == entry.id } + entry)
                    .sortedByDescending { it.dateMillis }
                _state.value = _state.value.copy(entries = entries)
                savedState.remove<String>("entryId")
                updateForm(ExpenseFormState())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateForm(_state.value.form.copy(
                    saving = false,
                    saveError = "Nie udało się zapisać kosztu. Dane pozostają w formularzu — spróbuj ponownie."
                ))
            }
        }
    }

    fun requestDelete(entry: ExpenseEntry) {
        if (_state.value.deleting || _state.value.form.saving) return
        _state.value = _state.value.copy(deletingEntry = entry, deleteError = null)
    }

    fun cancelDelete() {
        if (!_state.value.deleting) {
            _state.value = _state.value.copy(deletingEntry = null, deleteError = null)
        }
    }

    fun confirmDelete() {
        val entry = _state.value.deletingEntry ?: return
        if (_state.value.deleting) return
        _state.value = _state.value.copy(deleting = true, deleteError = null)
        viewModelScope.launch {
            try {
                repository.deleteEntry(car.id, entry.id)
                _state.value = _state.value.copy(
                    entries = _state.value.entries.filterNot { it.id == entry.id },
                    deletingEntry = null,
                    deleting = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    deleting = false,
                    deleteError = "Nie udało się usunąć wpisu. Spróbuj ponownie."
                )
            }
        }
    }

    private fun updateForm(form: ExpenseFormState) {
        _state.value = _state.value.copy(form = form)
        savedState["visible"] = form.visible
        savedState["editingEntryId"] = form.editingEntryId
        savedState["type"] = form.type.name
        savedState["title"] = form.title
        savedState["date"] = form.dateMillis
        savedState["cost"] = form.cost
        savedState["note"] = form.note
    }

    class Factory(private val car: Car, private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(VehicleExpenseViewModel::class.java))
            return VehicleExpenseViewModel(car, repository, extras.createSavedStateHandle()) as T
        }
    }
}
