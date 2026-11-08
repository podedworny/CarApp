package com.example.carapp.ui.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.*
import com.example.carapp.ui.components.*
import com.example.carapp.ui.reminders.ReminderState
import com.example.carapp.ui.service.VehicleServiceState
import com.example.carapp.ui.service.formatCost
import java.text.NumberFormat
import java.util.Locale

internal fun LazyListScope.vehicleOverview(car: Car, service: VehicleServiceState, reminders: ReminderState,
    today: Long, onMileage: () -> Unit, onService: () -> Unit, onReminders: () -> Unit,
    onCosts: () -> Unit, onAddService: () -> Unit, onRetryService: () -> Unit) {
    item {
        BoxWithConstraints {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.15f
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(car.brand, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(car.model, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("Rok produkcji ${car.year}", style = MaterialTheme.typography.bodySmall)
                        }
                        VehicleEmblem(Modifier.width(if (compact) 56.dp else 80.dp).height(80.dp))
                    }
                    RegistrationPlate(car.registration)
                }
            }
        }
    }
    item {
        BoxWithConstraints {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.15f
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(CarAppIcon.MILEAGE, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Przebieg", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${NumberFormat.getIntegerInstance(Locale.forLanguageTag("pl-PL")).format(car.mileage)} km",
                                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall)
                        }
                        if (compact) IconButton(onClick = onMileage) {
                            AppIcon(CarAppIcon.EDIT, "Aktualizuj przebieg", tint = MaterialTheme.colorScheme.primary)
                        } else TextButton(onClick = onMileage) { Text("Aktualizuj") }
                    }
                    Text(car.mileageDateLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (car.hasStaleMileage(today)) Text("Odczyt wymaga potwierdzenia", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    item {
        val urgent = reminders.reminders.count { it.status(today, car.mileage) in listOf(ReminderStatus.OVERDUE, ReminderStatus.DUE) }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewShortcut(CarAppIcon.SERVICE, "Historia serwisowa",
                when { service.loading -> "Wczytywanie…"; service.loadError -> "Błąd odczytu"; else -> "Wpisy: ${service.entries.size}" },
                onService, Modifier.weight(1f).fillMaxHeight())
            OverviewShortcut(CarAppIcon.CALENDAR, "Najbliższe terminy",
                when { reminders.loading -> "Wczytywanie…"; reminders.loadError -> "Błąd odczytu"; urgent > 0 -> "Do wykonania: $urgent"; else -> "Brak pilnych zadań" },
                onReminders, Modifier.weight(1f).fillMaxHeight(), urgent > 0)
        }
    }
    item {
        Card(onClick = onCosts, shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(CarAppIcon.COSTS, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Koszty serwisu", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (service.loading || service.loadError) "—" else formatCost(service.entries.sumOf { it.costMinor ?: 0 }),
                        style = MaterialTheme.typography.titleLarge)
                }
                AppIcon(CarAppIcon.NEXT)
            }
        }
    }
    item {
        Button(onClick = onAddService, enabled = !service.loading && !service.loadError,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("+ Dodaj wpis serwisowy") }
        if (service.loadError) TextButton(onClick = onRetryService) { Text("Ponów odczyt historii") }
    }
}

@Composable
private fun OverviewShortcut(icon: CarAppIcon, title: String, subtitle: String, onClick: () -> Unit,
                             modifier: Modifier = Modifier, urgent: Boolean = false) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIcon(icon, tint = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
