package com.example.carapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carapp.ui.vehicle.VehicleRoute
import com.example.carapp.AppContainer
import com.example.carapp.ui.addcar.AddCarScreen
import com.example.carapp.ui.garage.DeleteCarDialog
import com.example.carapp.ui.garage.GarageScreen
import com.example.carapp.ui.garage.GarageViewModel

@Composable
fun GarageApp(viewModel: GarageViewModel, container: AppContainer) {
    val garage by viewModel.garage.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val adding by viewModel.adding.collectAsStateWithLifecycle()
    val editingCarId by viewModel.editing.collectAsStateWithLifecycle()
    val selectedCarId by viewModel.selectedCarId.collectAsStateWithLifecycle()
    val selectedCar = garage.cars.firstOrNull { it.id == selectedCarId }
    val deletion by viewModel.deletion.collectAsStateWithLifecycle()
    deletion?.let { state ->
        DeleteCarDialog(state, onContinue = viewModel::continueDeleteCar,
            onDelete = viewModel::confirmDeleteCar, onCancel = viewModel::cancelDeleteCar)
    }
    BackHandler(selectedCar != null && !adding && editingCarId == null && deletion == null) { viewModel.closeVehicle() }
    BackHandler(adding) { viewModel.closeAddCar() }
    BackHandler(editingCarId != null) { viewModel.closeEditCar() }
    Scaffold { padding ->
        if (adding) {
            AddCarScreen(state = form, modifier = Modifier.padding(padding),
                onBack = viewModel::closeAddCar, onChange = viewModel::changeField,
                onSave = viewModel::saveCar)
        } else if (editingCarId != null) {
            AddCarScreen(state = form, modifier = Modifier.padding(padding),
                onBack = viewModel::closeEditCar, onChange = viewModel::changeField,
                onSave = viewModel::saveEditCar)
        } else if (selectedCar != null) {
            VehicleRoute(selectedCar, container.serviceRepository, container.carRepository, container.reminderRepository, onBack = viewModel::closeVehicle,
                onDelete = { viewModel.requestDeleteCar(selectedCar.id) }, modifier = Modifier.padding(padding), onCarUpdated = viewModel::updateCarSnapshot,
                onEdit = { viewModel.openEditCar(selectedCar.id) })
        } else {
            GarageScreen(cars = garage.cars, loaded = !garage.loading,
                loadError = garage.loadError, modifier = Modifier.padding(padding),
                onRetry = viewModel::loadCars, onDelete = viewModel::requestDeleteCar, onOpen = viewModel::openVehicle, onAdd = viewModel::openAddCar)
        }
    }
}
