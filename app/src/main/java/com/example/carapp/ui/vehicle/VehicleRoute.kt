package com.example.carapp.ui.vehicle

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.repository.CarRepository
import com.example.carapp.domain.repository.ReminderRepository
import com.example.carapp.domain.repository.ServiceRepository
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.ui.reminders.ReminderFormScreen
import com.example.carapp.ui.reminders.ReminderViewModel
import com.example.carapp.ui.service.ServiceFormScreen
import com.example.carapp.ui.service.VehicleServiceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VehicleRoute(car: Car, repository: ServiceRepository, carRepository: CarRepository,
                 reminderRepository: ReminderRepository, onBack: () -> Unit,
                 onDelete: () -> Unit, modifier: Modifier = Modifier, onCarUpdated: (Car) -> Unit = {}) {
    val serviceModel: VehicleServiceViewModel = viewModel(key = "vehicle-service:${car.id}",
        factory = VehicleServiceViewModel.Factory(car, repository))
    val reminderModel: ReminderViewModel = viewModel(key = "vehicle-reminders:${car.id}",
        factory = ReminderViewModel.Factory(car, reminderRepository))
    val mileageModel: VehicleMileageViewModel = viewModel(key = "vehicle-mileage:${car.id}",
        factory = VehicleMileageViewModel.Factory(car, carRepository))
    val service by serviceModel.state.collectAsStateWithLifecycle()
    val reminders by reminderModel.state.collectAsStateWithLifecycle()
    val mileage by mileageModel.state.collectAsStateWithLifecycle()
    val currentCar = mileage.updatedCar ?: car
    val holder = rememberSaveableStateHolder()
    val today by produceState(ServiceDates.todayMillis()) {
        while (isActive) { value = ServiceDates.todayMillis(); delay(60000) }
    }
    LaunchedEffect(mileage.updatedCar) { mileage.updatedCar?.let(onCarUpdated) }
    LaunchedEffect(service.suggestedMileage, currentCar.mileage) {
        service.suggestedMileage?.let { if (it <= currentCar.mileage) serviceModel.dismissMileageSuggestion() }
    }
    service.suggestedMileage?.takeIf { it > currentCar.mileage }?.let { suggested ->
        ServiceMileageSuggestionDialog(suggested, mileage.saving, mileage.error,
            onConfirm = { mileageModel.acceptSuggestion(suggested) },
            onSkip = serviceModel::dismissMileageSuggestion)
    }
    BackHandler(service.form.visible) { serviceModel.closeForm() }
    BackHandler(reminders.form.visible) { reminderModel.closeForm() }
    if (mileage.visible) MileageDialog(mileage, mileageModel::change, mileageModel::save, mileageModel::close)
    if (service.form.visible) {
        ServiceFormScreen(currentCar, service.form, onBack = serviceModel::closeForm,
            onField = serviceModel::changeField, onType = serviceModel::changeType,
            onDate = serviceModel::changeDate, onSave = { serviceModel.save(currentCar.mileage) }, modifier = modifier)
    } else if (reminders.form.visible) {
        ReminderFormScreen(currentCar, reminders.form, onBack = reminderModel::closeForm,
            onTitle = reminderModel::title, onDate = reminderModel::date, onMileage = reminderModel::mileage,
            onUseDate = reminderModel::useDate, onUseMileage = reminderModel::useMileage,
            onSave = reminderModel::save, modifier = modifier)
    } else {
        holder.SaveableStateProvider(car.id) {
            VehicleScreen(currentCar, onBack, onDelete, modifier, serviceState = service,
                onAddService = { serviceModel.openForm(currentCar.mileage) }, onRetryService = serviceModel::load,
                reminderState = reminders, todayMillis = today,
                onAddReminder = { reminderModel.openForm(currentCar.mileage) }, onRetryReminders = reminderModel::load,
                onCompleteReminder = reminderModel::toggleCompleted, onMileage = { mileageModel.open(currentCar.mileage) })
        }
    }
}
