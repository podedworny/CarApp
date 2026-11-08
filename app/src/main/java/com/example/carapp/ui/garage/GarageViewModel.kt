package com.example.carapp.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.repository.CarRepository
import com.example.carapp.ui.addcar.AddCarState
import com.example.carapp.ui.addcar.CarField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class GarageState(
    val cars: List<Car> = emptyList(),
    val loading: Boolean = true,
    val loadError: Boolean = false
)

class GarageViewModel(
    private val repository: CarRepository,
    private val savedState: SavedStateHandle,
    currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) : ViewModel() {
    private val _garage = MutableStateFlow(GarageState())
    val garage = _garage.asStateFlow()
    val adding = savedState.getStateFlow("adding", false)
    val selectedCarId = savedState.getStateFlow<String?>("selectedCarId", null)
    private val _deletion = MutableStateFlow<DeleteCarState?>(null)
    val deletion = _deletion.asStateFlow()
    private val _form = MutableStateFlow(AddCarState(
        brand = savedState["brand"] ?: "",
        model = savedState["model"] ?: "",
        year = savedState["year"] ?: "",
        mileage = savedState["mileage"] ?: "",
        registration = savedState["registration"] ?: "",
        currentYear = currentYear
    ))
    val form = _form.asStateFlow()

    private var loadJob: Job? = null

    init { loadCars() }

    fun loadCars() {
        if (_deletion.value != null) return
        _garage.value = _garage.value.copy(loading = true, loadError = false)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _garage.value = GarageState(cars = repository.getCars(), loading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _garage.value = _garage.value.copy(loading = false, loadError = true)
            }
        }
    }

    /** Accepts a successfully persisted snapshot from a vehicle feature. */
    fun updateCarSnapshot(car: Car) {
        _garage.value = _garage.value.copy(cars = _garage.value.cars.map { if (it.id == car.id) car else it })
    }

    fun openVehicle(id: String) {
        if (adding.value || _deletion.value != null || _garage.value.loading || _garage.value.loadError) return
        if (_garage.value.cars.any { it.id == id }) savedState["selectedCarId"] = id
    }

    fun closeVehicle() {
        if (_deletion.value == null) savedState["selectedCarId"] = null
    }

    fun openAddCar() {
        if (!_garage.value.loading && !_garage.value.loadError && _deletion.value == null) savedState["adding"] = true
    }

    fun closeAddCar() {
        if (!_form.value.saving) {
            savedState["adding"] = false
            resetForm()
        }
    }

    fun changeField(field: CarField, value: String) {
        if (_form.value.saving) return
        val text = when (field) {
            CarField.BRAND -> value.take(50)
            CarField.MODEL -> value.take(70)
            CarField.YEAR -> value.filter { it in '0'..'9' }.take(4)
            CarField.MILEAGE -> value.filter { it in '0'..'9' }.take(10)
            CarField.REGISTRATION -> value.uppercase(Locale.ROOT).take(15)
        }
        if (field == CarField.BRAND && text != _form.value.brand) {
            savedState["model"] = ""
            _form.value = _form.value.copy(model = "")
        }
        savedState[field.name.lowercase(Locale.ROOT)] = text
        _form.value = when (field) {
            CarField.BRAND -> _form.value.copy(brand = text)
            CarField.MODEL -> _form.value.copy(model = text)
            CarField.YEAR -> _form.value.copy(year = text)
            CarField.MILEAGE -> _form.value.copy(mileage = text)
            CarField.REGISTRATION -> _form.value.copy(registration = text)
        }.copy(saveError = null)
    }

    fun saveCar() {
        val input = _form.value
        if (!adding.value || input.saving || _garage.value.loading || _garage.value.loadError) return
        _form.value = input.copy(submitted = true, saveError = null)
        if (!input.errors.isValid) return
        val draftId = savedState.get<String>("draftCarId") ?: UUID.randomUUID().toString().also { savedState["draftCarId"] = it }
        val car = Car(id = draftId, brand = input.brand.trim(), model = input.model.trim(),
            year = input.year.toInt(), mileage = input.mileage.toInt(),
            registration = input.registration.trim(), mileageUpdatedDateMillis = ServiceDates.todayMillis())
        _form.value = _form.value.copy(saving = true)
        viewModelScope.launch {
            try {
                repository.addCar(car)
                _garage.value = _garage.value.copy(cars = _garage.value.cars.filterNot { it.id == car.id } + car)
                savedState["adding"] = false
                resetForm()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _form.value = _form.value.copy(saving = false,
                    saveError = "Nie udało się zapisać auta. Spróbuj ponownie.")
            }
        }
    }

    fun requestDeleteCar(id: String) {
        if (adding.value || _garage.value.loading || _garage.value.loadError || _deletion.value != null) return
        val car = _garage.value.cars.firstOrNull { it.id == id } ?: return
        _deletion.value = DeleteCarState(car)
    }

    fun continueDeleteCar() {
        val state = _deletion.value ?: return
        if (!state.deleting && state.step == DeleteCarStep.REVIEW) {
            _deletion.value = state.copy(step = DeleteCarStep.FINAL_CONFIRMATION)
        }
    }

    fun cancelDeleteCar() {
        if (_deletion.value?.deleting != true) _deletion.value = null
    }

    fun confirmDeleteCar() {
        val state = _deletion.value ?: return
        if (state.deleting || state.step != DeleteCarStep.FINAL_CONFIRMATION) return
        _deletion.value = state.copy(deleting = true, error = null)
        viewModelScope.launch {
            try {
                repository.deleteCar(state.car.id)
                _garage.value = _garage.value.copy(cars = _garage.value.cars.filterNot { it.id == state.car.id })
                if (selectedCarId.value == state.car.id) savedState["selectedCarId"] = null
                _deletion.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _deletion.value = state.copy(error = "Nie udało się usunąć auta. Spróbuj ponownie.")
            }
        }
    }

    private fun resetForm() {
        savedState.remove<String>("draftCarId")
        CarField.entries.forEach { savedState[it.name.lowercase(Locale.ROOT)] = "" }
        _form.value = AddCarState(currentYear = _form.value.currentYear)
    }

    class Factory(private val repository: CarRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass.isAssignableFrom(GarageViewModel::class.java))
            return GarageViewModel(repository, extras.createSavedStateHandle()) as T
        }
    }
}
