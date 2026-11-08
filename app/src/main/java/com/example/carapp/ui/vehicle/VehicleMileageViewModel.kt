package com.example.carapp.ui.vehicle

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.repository.CarRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MileageState(val visible: Boolean = false, val value: String = "", val saving: Boolean = false,
                        val error: String? = null, val updatedCar: Car? = null)

class VehicleMileageViewModel(private val car: Car, private val repository: CarRepository,
                              private val saved: SavedStateHandle) : ViewModel() {
    private val _state = MutableStateFlow(MileageState(visible = saved["visible"] ?: false, value = saved["value"] ?: ""))
    val state = _state.asStateFlow()
    fun open(currentMileage: Int) {
        if (!_state.value.saving) {
            saved["visible"] = true
            change(currentMileage.toString())
            _state.value = _state.value.copy(visible = true)
        }
    }
    fun close() {
        if (!_state.value.saving) { saved["visible"] = false; _state.value = _state.value.copy(visible = false, error = null) }
    }
    fun change(value: String) {
        if (_state.value.saving) return
        val digits = value.filter { it in '0'..'9' }.take(10)
        saved["value"] = digits
        _state.value = _state.value.copy(value = digits, error = null)
    }
    fun save() {
        val input = _state.value
        if (!input.visible || input.saving) return
        val mileage = input.value.toIntOrNull()
        val minimum = input.updatedCar?.mileage ?: car.mileage
        if (mileage == null || mileage < minimum) {
            _state.value = input.copy(error = "Podaj przebieg nie mniejszy niż $minimum km.")
            return
        }
        _state.value = input.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val updated = repository.updateMileage(car.id, mileage)
                saved["visible"] = false
                _state.value = MileageState(updatedCar = updated)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = input.copy(error = "Nie udało się zapisać przebiegu. Spróbuj ponownie.") }
        }
    }
    class Factory(private val car: Car, private val repository: CarRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(VehicleMileageViewModel::class.java))
            return VehicleMileageViewModel(car, repository, extras.createSavedStateHandle()) as T
        }
    }
}
