package com.example.carapp.ui.service

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.ServiceEntry
import com.example.carapp.ui.components.EntryItemCard
import java.text.NumberFormat
import java.util.Locale

fun LazyListScope.serviceHistory(
    state: VehicleServiceState,
    onAdd: () -> Unit,
    onRetry: () -> Unit,
    onEdit: (ServiceEntry) -> Unit = {},
    onDelete: (ServiceEntry) -> Unit = {}
) {
    item {
        Text("Historia serwisowa", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, enabled = !state.loading && !state.loadError) { Text("+ Dodaj wpis serwisowy") }
    }
    when {
        state.loading -> item { CircularProgressIndicator() }
        state.loadError -> item { ServiceLoadError(onRetry) }
        state.entries.isEmpty() -> item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pierwszy krok do pełnej historii auta", style = MaterialTheme.typography.titleMedium)
                    Text("Dodaj ostatni przegląd, naprawę lub wymianę części. Wszystkie wpisy znajdziesz tutaj, od najnowszych.")
                }
            }
        }
        else -> {
            val numberFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pl-PL"))
            items(state.entries, key = { "service:${it.id}" }) { entry ->
                EntryItemCard(
                    badgeLabel = "${entry.type.displayName()} · ${formatServiceDate(entry.dateMillis)}",
                    title = entry.title,
                    footerLabel = "Przebieg: ${numberFormat.format(entry.mileage)} km",
                    costText = entry.costMinor?.let(::formatCost) ?: "Koszt niepodany",
                    note = entry.note,
                    onEdit = { onEdit(entry) },
                    onDelete = { onDelete(entry) }
                )
            }
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
