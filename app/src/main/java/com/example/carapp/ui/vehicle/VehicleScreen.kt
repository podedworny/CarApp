package com.example.carapp.ui.vehicle

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carapp.ui.reminders.ReminderState
import com.example.carapp.ui.reminders.reminderContent
import com.example.carapp.domain.model.ReminderStatus
import com.example.carapp.domain.model.status
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.ui.service.VehicleServiceState
import com.example.carapp.ui.service.serviceHistory
import com.example.carapp.ui.service.serviceCosts
import com.example.carapp.domain.model.Car
import com.example.carapp.ui.theme.CarAppTheme
import java.text.NumberFormat
import java.util.Locale

private enum class VehicleSection(val title: String, val description: String) {
    OVERVIEW("Przegląd", ""),
    SERVICE("Serwis", "Tutaj znajdziesz historię napraw, przeglądów i wymian części tego samochodu."),
    COSTS("Koszty", "Tutaj zestawisz wydatki na serwis i eksploatację tego samochodu."),
    REMINDERS("Przypomnienia", "Tutaj zaplanujesz przeglądy i wymiany na podstawie daty lub przebiegu.")
}

/** Vehicle hub receives current data and actions; persistence stays outside the screen. */
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
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Mój garaż") }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.semantics {
                        contentDescription = "Akcje pojazdu"
                    }) { Text("⋮", style = MaterialTheme.typography.headlineSmall) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Aktualizuj przebieg") }, onClick = { menuOpen = false; onMileage() })
                        DropdownMenuItem(text = { Text("Usuń samochód", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
        }
        item {
            Text("TWÓJ SAMOCHÓD", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("${car.brand} ${car.model}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("${car.year} · ${car.registration.ifBlank { "Bez numeru rejestracyjnego" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleSection.entries.forEach { item ->
                    FilterChip(selected = section == item, onClick = { sectionName = item.name }, label = { Text(item.title) })
                }
            }
        }
        if (section == VehicleSection.OVERVIEW) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Zapisany przebieg", style = MaterialTheme.typography.labelLarge)
                        Text("${NumberFormat.getIntegerInstance(Locale.forLanguageTag("pl-PL")).format(car.mileage)} km",
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Ostatnio zapisany stan licznika.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onMileage) { Text("Aktualizuj przebieg") }
                    }
                }
            }
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Dane pojazdu", style = MaterialTheme.typography.titleLarge)
                        VehicleDetail("Marka", car.brand)
                        VehicleDetail("Model", car.model)
                        VehicleDetail("Rok produkcji", car.year.toString())
                        VehicleDetail("Rejestracja", car.registration.ifBlank { "Nie podano" })
                    }
                }
            }
            item {
                val urgent = reminderState.reminders.count { it.status(todayMillis, car.mileage) in listOf(ReminderStatus.OVERDUE, ReminderStatus.DUE) }
                if (!reminderState.loading && !reminderState.loadError) {
                    TextButton(onClick = { sectionName = VehicleSection.REMINDERS.name }) {
                        Text(if (urgent > 0) "Zadania do wykonania: $urgent →" else "Zobacz przypomnienia →")
                    }
                }
            }
            item {
                Text("Wszystko wokół Twojego auta", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Zapisuj naprawy i przeglądy, a w zakładce Koszty sprawdzaj wydatki na serwis.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Button(onClick = { sectionName = VehicleSection.SERVICE.name; onAddService() },
                    enabled = !serviceState.loading && !serviceState.loadError) { Text("+ Dodaj wpis serwisowy") }
                if (serviceState.loadError) TextButton(onClick = onRetryService) { Text("Ponów odczyt historii") }
            }
        } else if (section == VehicleSection.SERVICE) {
            serviceHistory(serviceState, onAddService, onRetryService)
        } else if (section == VehicleSection.COSTS) {
            serviceCosts(serviceState, onAddService, onRetryService)
        } else {
            reminderContent(reminderState, car.mileage, todayMillis, onAddReminder, onRetryReminders, onCompleteReminder)
        }
    }
}

@Composable
private fun VehicleDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun VehiclePreview() {
    CarAppTheme { Surface { VehicleScreen(Car(brand = "Toyota", model = "Corolla", year = 2020,
        mileage = 85000, registration = "WA 12345"), onBack = {}, onDelete = {}) } }
}
