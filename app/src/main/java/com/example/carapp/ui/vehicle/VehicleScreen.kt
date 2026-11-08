package com.example.carapp.ui.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.ui.components.*
import com.example.carapp.ui.reminders.ReminderState
import com.example.carapp.ui.reminders.reminderContent
import com.example.carapp.ui.service.*
import com.example.carapp.ui.theme.CarAppTheme

private enum class VehicleSection(val title: String, val icon: CarAppIcon) {
    OVERVIEW("Przegląd", CarAppIcon.CAR), SERVICE("Serwis", CarAppIcon.SERVICE),
    COSTS("Koszty", CarAppIcon.COSTS), REMINDERS("Terminy", CarAppIcon.CALENDAR)
}

@Composable
fun VehicleScreen(car: Car, onBack: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier,
    serviceState: VehicleServiceState = VehicleServiceState(loading = false),
    onAddService: () -> Unit = {}, onRetryService: () -> Unit = {},
    reminderState: ReminderState = ReminderState(loading = false), todayMillis: Long = ServiceDates.todayMillis(),
    onAddReminder: () -> Unit = {}, onRetryReminders: () -> Unit = {},
    onCompleteReminder: (String) -> Unit = {}, onMileage: () -> Unit = {}) {
    var sectionName by rememberSaveable(car.id) { mutableStateOf(VehicleSection.OVERVIEW.name) }
    var menuOpen by rememberSaveable(car.id) { mutableStateOf(false) }
    val section = VehicleSection.valueOf(sectionName)
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { AppIcon(CarAppIcon.BACK, "Powrót do garażu") }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(car.brand, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(car.model, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { AppIcon(CarAppIcon.MORE, "Akcje pojazdu") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Aktualizuj przebieg") }, onClick = { menuOpen = false; onMileage() })
                    DropdownMenuItem(text = { Text("Usuń samochód", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() })
                }
            }
        }
        key(sectionName) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (section) {
                    VehicleSection.OVERVIEW -> vehicleOverview(car, serviceState, reminderState, todayMillis, onMileage,
                        onService = { sectionName = VehicleSection.SERVICE.name },
                        onReminders = { sectionName = VehicleSection.REMINDERS.name },
                        onCosts = { sectionName = VehicleSection.COSTS.name },
                        onAddService = { sectionName = VehicleSection.SERVICE.name; onAddService() }, onRetryService = onRetryService)
                    VehicleSection.SERVICE -> serviceHistory(serviceState, onAddService, onRetryService)
                    VehicleSection.COSTS -> serviceCosts(serviceState, onAddService, onRetryService)
                    VehicleSection.REMINDERS -> reminderContent(reminderState, car, todayMillis, onAddReminder, onRetryReminders, onCompleteReminder)
                }
            }
        }
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            windowInsets = WindowInsets(0, 0, 0, 0), tonalElevation = 0.dp) {
            VehicleSection.entries.forEach { destination ->
                NavigationBarItem(selected = section == destination, onClick = { sectionName = destination.name },
                    icon = { AppIcon(destination.icon, tint = if (section == destination) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = { Text(destination.title, style = MaterialTheme.typography.labelSmall, maxLines = 2) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VehiclePreview() {
    CarAppTheme { Surface { VehicleScreen(Car(brand = "Toyota", model = "Corolla", year = 2020,
        mileage = 85000, registration = "WA 12345"), onBack = {}, onDelete = {}) } }
}
