package com.example.carapp.ui.vehicle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.repository.CarRepository
import com.example.carapp.domain.repository.ExpenseRepository
import com.example.carapp.domain.repository.ReminderRepository
import com.example.carapp.domain.repository.ServiceRepository
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.ui.components.DeleteEntryDialog
import com.example.carapp.ui.expenses.CostCategoryScreen
import com.example.carapp.ui.expenses.CostCategorySelection
import com.example.carapp.ui.expenses.ExpenseFormScreen
import com.example.carapp.ui.expenses.VehicleExpenseViewModel
import com.example.carapp.ui.reminders.ReminderFormScreen
import com.example.carapp.ui.reminders.ReminderViewModel
import com.example.carapp.ui.service.ServiceFormScreen
import com.example.carapp.ui.service.VehicleServiceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class VehicleDestination(val depth: Int) {
    MAIN(0),
    COST_CATEGORY(1),
    FORM(2)
}

private enum class ActiveForm { NONE, SERVICE, REMINDER, EXPENSE }

private data class RouteScreenKey(
    val destination: VehicleDestination,
    val form: ActiveForm = ActiveForm.NONE,
    val categoryKey: String? = null
)

@Composable
fun VehicleRoute(car: Car, repository: ServiceRepository, carRepository: CarRepository,
                 reminderRepository: ReminderRepository, expenseRepository: ExpenseRepository,
                 onBack: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier,
                 onCarUpdated: (Car) -> Unit = {}, onEdit: () -> Unit = {}) {
    val serviceModel: VehicleServiceViewModel = viewModel(key = "vehicle-service:${car.id}",
        factory = VehicleServiceViewModel.Factory(car, repository))
    val reminderModel: ReminderViewModel = viewModel(key = "vehicle-reminders:${car.id}",
        factory = ReminderViewModel.Factory(car, reminderRepository))
    val expenseModel: VehicleExpenseViewModel = viewModel(key = "vehicle-expenses:${car.id}",
        factory = VehicleExpenseViewModel.Factory(car, expenseRepository))
    val mileageModel: VehicleMileageViewModel = viewModel(key = "vehicle-mileage:${car.id}",
        factory = VehicleMileageViewModel.Factory(car, carRepository))
    val service by serviceModel.state.collectAsStateWithLifecycle()
    val reminders by reminderModel.state.collectAsStateWithLifecycle()
    val expenses by expenseModel.state.collectAsStateWithLifecycle()
    val mileage by mileageModel.state.collectAsStateWithLifecycle()
    val currentCar = mileage.updatedCar ?: car
    val holder = rememberSaveableStateHolder()
    var selectedCategoryKey by rememberSaveable(car.id) { mutableStateOf<String?>(null) }
    val selectedCategory = CostCategorySelection.decode(selectedCategoryKey)
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
    service.deletingEntry?.let { entry ->
        DeleteEntryDialog(entry.title, service.deleting, service.deleteError,
            onConfirm = serviceModel::confirmDelete, onDismiss = serviceModel::cancelDelete)
    }
    expenses.deletingEntry?.let { entry ->
        DeleteEntryDialog(entry.title, expenses.deleting, expenses.deleteError,
            onConfirm = expenseModel::confirmDelete, onDismiss = expenseModel::cancelDelete)
    }
    BackHandler(service.form.visible) { serviceModel.closeForm() }
    BackHandler(reminders.form.visible) { reminderModel.closeForm() }
    BackHandler(expenses.form.visible) { expenseModel.closeForm() }
    BackHandler(selectedCategory != null && !service.form.visible && !reminders.form.visible && !expenses.form.visible) {
        selectedCategoryKey = null
    }
    if (mileage.visible) MileageDialog(mileage, mileageModel::change, mileageModel::save, mileageModel::close)

    val activeScreen = when {
        service.form.visible -> RouteScreenKey(VehicleDestination.FORM, ActiveForm.SERVICE, selectedCategoryKey)
        reminders.form.visible -> RouteScreenKey(VehicleDestination.FORM, ActiveForm.REMINDER, selectedCategoryKey)
        expenses.form.visible -> RouteScreenKey(VehicleDestination.FORM, ActiveForm.EXPENSE, selectedCategoryKey)
        selectedCategory != null -> RouteScreenKey(VehicleDestination.COST_CATEGORY, ActiveForm.NONE, selectedCategoryKey)
        else -> RouteScreenKey(VehicleDestination.MAIN)
    }

    AnimatedContent(
        targetState = activeScreen,
        transitionSpec = {
            val direction = if (targetState.destination.depth >= initialState.destination.depth) {
                AnimatedContentTransitionScope.SlideDirection.Start
            } else {
                AnimatedContentTransitionScope.SlideDirection.End
            }
            (slideIntoContainer(direction, tween(240)) + fadeIn(tween(180))) togetherWith
                (slideOutOfContainer(direction, tween(240)) + fadeOut(tween(180)))
        },
        label = "vehicle-route-transition"
    ) { screen ->
        when {
            screen.form == ActiveForm.SERVICE -> ServiceFormScreen(
                currentCar, service.form, onBack = serviceModel::closeForm,
                onField = serviceModel::changeField, onType = serviceModel::changeType,
                onDate = serviceModel::changeDate, onSave = { serviceModel.save(currentCar.mileage) }, modifier = modifier
            )
            screen.form == ActiveForm.REMINDER -> ReminderFormScreen(
                currentCar, reminders.form, onBack = reminderModel::closeForm,
                onTitle = reminderModel::title, onDate = reminderModel::date, onMileage = reminderModel::mileage,
                onUseDate = reminderModel::useDate, onUseMileage = reminderModel::useMileage,
                onSave = reminderModel::save, modifier = modifier
            )
            screen.form == ActiveForm.EXPENSE -> ExpenseFormScreen(
                currentCar, expenses.form, onBack = expenseModel::closeForm,
                onField = expenseModel::changeField, onType = expenseModel::changeType,
                onDate = expenseModel::changeDate, onSave = expenseModel::save, modifier = modifier
            )
            screen.destination == VehicleDestination.COST_CATEGORY -> {
                val category = CostCategorySelection.decode(screen.categoryKey)
                if (category != null) {
                    CostCategoryScreen(
                        car = currentCar,
                        selection = category,
                        serviceEntries = service.entries,
                        expenseEntries = expenses.entries,
                        onBack = { selectedCategoryKey = null },
                        onAddService = { type -> serviceModel.openForm(currentCar.mileage, initialType = type) },
                        onEditService = serviceModel::openEditForm,
                        onDeleteService = serviceModel::requestDelete,
                        onAddExpense = { type -> expenseModel.openForm(initialType = type) },
                        onEditExpense = expenseModel::openEditForm,
                        onDeleteExpense = expenseModel::requestDelete,
                        modifier = modifier
                    )
                }
            }
            else -> holder.SaveableStateProvider(car.id) {
                VehicleScreen(currentCar, onBack, onDelete, modifier, serviceState = service,
                    onAddService = { serviceModel.openForm(currentCar.mileage) }, onRetryService = serviceModel::load,
                    onEditService = serviceModel::openEditForm, onDeleteService = serviceModel::requestDelete,
                    onOpenServiceCategory = { selectedCategoryKey = CostCategorySelection.Service(it).encode() },
                    expenseState = expenses, onAddExpense = { expenseModel.openForm() }, onRetryExpense = expenseModel::load,
                    onOpenExpenseCategory = { selectedCategoryKey = CostCategorySelection.Expense(it).encode() },
                    reminderState = reminders, todayMillis = today,
                    onAddReminder = { reminderModel.openForm(currentCar.mileage) }, onRetryReminders = reminderModel::load,
                    onCompleteReminder = reminderModel::toggleCompleted, onMileage = { mileageModel.open(currentCar.mileage) },
                    onEdit = onEdit)
            }
        }
    }
}
