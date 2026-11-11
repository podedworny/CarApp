package com.example.carapp.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.ExpenseType
import com.example.carapp.domain.model.FuelSource
import com.example.carapp.domain.model.ServiceType
import com.example.carapp.ui.components.AppIcon
import com.example.carapp.ui.components.CarAppIcon
import com.example.carapp.ui.service.VehicleServiceState
import com.example.carapp.ui.service.displayName
import com.example.carapp.ui.service.formatCost

fun LazyListScope.vehicleCosts(
    serviceState: VehicleServiceState,
    expenseState: VehicleExpenseState,
    fuelSources: Set<FuelSource>,
    onAddService: () -> Unit,
    onRetryService: () -> Unit,
    onOpenServiceCategory: (ServiceType) -> Unit,
    onAddExpense: () -> Unit,
    onRetryExpense: () -> Unit,
    onOpenExpenseCategory: (ExpenseType) -> Unit
) {
    val serviceReady = !serviceState.loading && !serviceState.loadError
    val expenseReady = !expenseState.loading && !expenseState.loadError
    val serviceKnown = serviceState.entries.filter { it.costMinor != null }
    val serviceUnknown = serviceState.entries.size - serviceKnown.size
    val serviceSum = if (serviceReady) serviceKnown.sumOf { it.costMinor!! } else 0L
    val expenseSum = if (expenseReady) expenseState.entries.sumOf { it.costMinor } else 0L

    item {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Łączne koszty pojazdu", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (serviceState.loading && expenseState.loading) "Wczytywanie…" else formatCost(serviceSum + expenseSum),
                    style = MaterialTheme.typography.headlineLarge
                )
                val servicePart = when {
                    serviceState.loading -> "…"
                    serviceState.loadError -> "błąd"
                    else -> formatCost(serviceSum)
                }
                val expensePart = when {
                    expenseState.loading -> "…"
                    expenseState.loadError -> "błąd"
                    else -> formatCost(expenseSum)
                }
                Text("Serwis: $servicePart · Eksploatacja: $expensePart", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    item {
        CostQuickActions(
            serviceEnabled = serviceReady,
            expenseEnabled = expenseReady,
            onAddService = onAddService,
            onAddExpense = onAddExpense
        )
    }

    item {
        Text("Koszty serwisowe", style = MaterialTheme.typography.headlineSmall)
    }
    when {
        serviceState.loading -> item { CircularProgressIndicator() }
        serviceState.loadError -> item { LoadErrorRetry("Nie udało się odczytać kosztów serwisowych.", onRetryService) }
        serviceKnown.isEmpty() -> item {
            Text(
                text = if (serviceUnknown > 0) "Brak wpisów z podanym kosztem (bez kosztu: $serviceUnknown)."
                else "Brak zapisanych kosztów serwisowych.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Łącznie serwis: ${formatCost(serviceSum)} (wpisy: ${serviceKnown.size})")
                    if (serviceUnknown > 0) {
                        Text(
                            "Bez podanego kosztu: $serviceUnknown — nie są uwzględnione w sumie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            ServiceType.entries.forEach { type ->
                val group = serviceKnown.filter { it.type == type }
                if (group.isNotEmpty()) {
                    item(key = "service-cat:${type.name}") {
                        CategoryCostCard(
                            title = type.displayName(),
                            sumMinor = group.sumOf { it.costMinor!! },
                            count = group.size,
                            onClick = { onOpenServiceCategory(type) }
                        )
                    }
                }
            }
        }
    }

    item {
        Spacer(Modifier.height(4.dp))
        Text("Koszty eksploatacyjne", style = MaterialTheme.typography.headlineSmall)
    }
    when {
        expenseState.loading -> item { CircularProgressIndicator() }
        expenseState.loadError -> item { LoadErrorRetry("Nie udało się odczytać kosztów eksploatacyjnych.", onRetryExpense) }
        expenseState.entries.isEmpty() -> item {
            Text(
                "Brak zapisanych kosztów eksploatacyjnych (paliwo, ubezpieczenie, myjnia itp.).",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            item {
                Text("Łącznie eksploatacja: ${formatCost(expenseSum)} (wpisy: ${expenseState.entries.size})")
            }
            ExpenseType.entries.forEach { type ->
                val group = expenseState.entries.filter { it.type == type }
                if (group.isNotEmpty()) {
                    item(key = "expense-cat:${type.name}") {
                        CategoryCostCard(
                            title = type.displayName(fuelSources),
                            sumMinor = group.sumOf { it.costMinor },
                            count = group.size,
                            onClick = { onOpenExpenseCategory(type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CostQuickActions(
    serviceEnabled: Boolean,
    expenseEnabled: Boolean,
    onAddService: () -> Unit,
    onAddExpense: () -> Unit
) {
    BoxWithConstraints {
        val compact = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.15f
        if (compact) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onAddService,
                    enabled = serviceEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Wpis serwisowy") }
                OutlinedButton(
                    onClick = onAddExpense,
                    enabled = expenseEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Koszt eksploatacji") }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onAddService,
                    enabled = serviceEnabled,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) { Text("+ Serwis", maxLines = 1) }
                OutlinedButton(
                    onClick = onAddExpense,
                    enabled = expenseEnabled,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) { Text("+ Eksploatacja", maxLines = 1) }
            }
        }
    }
}

@Composable
private fun CategoryCostCard(
    title: String,
    sumMinor: Long,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(formatCost(sumMinor), style = MaterialTheme.typography.titleLarge)
                Text("Liczba wpisów: $count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppIcon(CarAppIcon.NEXT, "Otwórz kategorię $title")
        }
    }
}

@Composable
private fun LoadErrorRetry(message: String, onRetry: () -> Unit) {
    Column {
        Text(message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Spróbuj ponownie") }
    }
}
