package com.example.carapp.ui.service

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.ServiceEntry
import com.example.carapp.domain.model.ServiceType

fun LazyListScope.serviceHistory(state: VehicleServiceState, onAdd: () -> Unit, onRetry: () -> Unit) {
    item {
        Text("Historia serwisowa", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, enabled = !state.loading && !state.loadError) { Text("+ Dodaj wpis serwisowy") }
    }
    when {
        state.loading -> item { CircularProgressIndicator() }
        state.loadError -> item { ServiceLoadError(onRetry) }
        state.entries.isEmpty() -> item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pierwszy krok do pełnej historii auta", style = MaterialTheme.typography.titleMedium)
                    Text("Dodaj ostatni przegląd, naprawę lub wymianę części. Wszystkie wpisy znajdziesz tutaj, od najnowszych.")
                }
            }
        }
        else -> items(state.entries, key = { "service:${it.id}" }) { ServiceEntryCard(it) }
    }
}

@Composable
private fun ServiceEntryCard(entry: ServiceEntry) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${entry.type.displayName()} · ${formatServiceDate(entry.dateMillis)}",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("pl-PL")).format(entry.mileage)} km")
            Text(entry.costMinor?.let(::formatCost) ?: "Koszt niepodany", fontWeight = FontWeight.SemiBold)
            if (entry.note.isNotBlank()) Text(entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun LazyListScope.serviceCosts(state: VehicleServiceState, onAdd: () -> Unit, onRetry: () -> Unit) {
    when {
        state.loading -> item { CircularProgressIndicator() }
        state.loadError -> item { ServiceLoadError(onRetry) }
        else -> {
            val known = state.entries.filter { it.costMinor != null }
            val unknown = state.entries.size - known.size
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Łączne koszty serwisu", style = MaterialTheme.typography.titleMedium)
                        Text(formatCost(known.sumOf { it.costMinor!! }), style = MaterialTheme.typography.headlineLarge)
                        Text("Wpisy z podanym kosztem: ${known.size}")
                        if (unknown > 0) Text("Bez podanego kosztu: $unknown — nie są uwzględnione w sumie.")
                        Text("Podsumowanie obejmuje wpisy serwisowe. Paliwo, ubezpieczenie i inne wydatki dodamy w kolejnych etapach.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ServiceType.entries.forEach { type ->
                val group = known.filter { it.type == type }
                if (group.isNotEmpty()) item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(type.displayName(), style = MaterialTheme.typography.titleMedium)
                            Text(formatCost(group.sumOf { it.costMinor!! }), style = MaterialTheme.typography.titleLarge)
                            Text("Liczba wpisów: ${group.size}")
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = onAdd) { Text("+ Dodaj wpis serwisowy") } }
        }
    }
}

@Composable
private fun ServiceLoadError(onRetry: () -> Unit) {
    Column {
        Text("Nie udało się odczytać historii serwisowej.", color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Spróbuj ponownie") }
    }
}
