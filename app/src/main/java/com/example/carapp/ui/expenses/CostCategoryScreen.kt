package com.example.carapp.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.model.ExpenseEntry
import com.example.carapp.domain.model.ExpenseType
import com.example.carapp.domain.model.ServiceEntry
import com.example.carapp.domain.model.ServiceType
import com.example.carapp.ui.components.AppIcon
import com.example.carapp.ui.components.CarAppIcon
import com.example.carapp.ui.components.EntryItemCard
import com.example.carapp.ui.service.displayName
import com.example.carapp.ui.service.formatCost
import com.example.carapp.ui.service.formatServiceDate
import java.text.NumberFormat
import java.util.Locale

sealed interface CostCategorySelection {
    data class Service(val type: ServiceType) : CostCategorySelection
    data class Expense(val type: ExpenseType) : CostCategorySelection

    fun encode(): String = when (this) {
        is Service -> "service:${type.name}"
        is Expense -> "expense:${type.name}"
    }

    companion object {
        fun decode(raw: String?): CostCategorySelection? {
            if (raw == null) return null
            val parts = raw.split(":", limit = 2)
            if (parts.size != 2) return null
            return when (parts[0]) {
                "service" -> runCatching { Service(ServiceType.valueOf(parts[1])) }.getOrNull()
                "expense" -> runCatching { Expense(ExpenseType.valueOf(parts[1])) }.getOrNull()
                else -> null
            }
        }
    }
}

private data class CategoryEntryUi(
    val id: String,
    val badgeLabel: String,
    val title: String,
    val footerLabel: String,
    val costMinor: Long,
    val note: String,
    val onEdit: () -> Unit,
    val onDelete: () -> Unit
)

@Composable
fun CostCategoryScreen(
    car: Car,
    selection: CostCategorySelection,
    serviceEntries: List<ServiceEntry>,
    expenseEntries: List<ExpenseEntry>,
    onBack: () -> Unit,
    onAddService: (ServiceType) -> Unit,
    onEditService: (ServiceEntry) -> Unit,
    onDeleteService: (ServiceEntry) -> Unit,
    onAddExpense: (ExpenseType) -> Unit,
    onEditExpense: (ExpenseEntry) -> Unit,
    onDeleteExpense: (ExpenseEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryTitle: String
    val sectionLabel: String
    val addButtonLabel: String
    val icon: CarAppIcon
    val onAdd: () -> Unit
    val itemsUi: List<CategoryEntryUi>

    when (selection) {
        is CostCategorySelection.Service -> {
            categoryTitle = selection.type.displayName()
            sectionLabel = "Koszty serwisowe"
            addButtonLabel = "+ Dodaj wpis serwisowy"
            icon = CarAppIcon.SERVICE
            onAdd = { onAddService(selection.type) }
            val numberFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pl-PL"))
            itemsUi = serviceEntries
                .filter { it.type == selection.type && it.costMinor != null }
                .map { entry ->
                    CategoryEntryUi(
                        id = "service:${entry.id}",
                        badgeLabel = formatServiceDate(entry.dateMillis),
                        title = entry.title,
                        footerLabel = "Przebieg: ${numberFormat.format(entry.mileage)} km",
                        costMinor = entry.costMinor!!,
                        note = entry.note,
                        onEdit = { onEditService(entry) },
                        onDelete = { onDeleteService(entry) }
                    )
                }
        }
        is CostCategorySelection.Expense -> {
            categoryTitle = selection.type.displayName(car.fuelSources)
            sectionLabel = "Koszty eksploatacyjne"
            addButtonLabel = "+ Dodaj koszt eksploatacji"
            icon = CarAppIcon.COSTS
            onAdd = { onAddExpense(selection.type) }
            itemsUi = expenseEntries
                .filter { it.type == selection.type }
                .map { entry ->
                    CategoryEntryUi(
                        id = "expense:${entry.id}",
                        badgeLabel = formatServiceDate(entry.dateMillis),
                        title = entry.title,
                        footerLabel = categoryTitle,
                        costMinor = entry.costMinor,
                        note = entry.note,
                        onEdit = { onEditExpense(entry) },
                        onDelete = { onDeleteExpense(entry) }
                    )
                }
        }
    }

    val totalMinor = itemsUi.sumOf { it.costMinor }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { AppIcon(CarAppIcon.BACK, "Wróć do kosztów") }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text("${car.brand} ${car.model}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(categoryTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(sectionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(formatCost(totalMinor), style = MaterialTheme.typography.headlineLarge)
                            Text("Liczba wpisów: ${itemsUi.size}", style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                AppIcon(icon, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Text(addButtonLabel)
                }
            }
            if (itemsUi.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Text(
                            "Brak wpisów w tej kategorii.",
                            Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(itemsUi, key = { it.id }) { entry ->
                    EntryItemCard(
                        badgeLabel = entry.badgeLabel,
                        title = entry.title,
                        footerLabel = entry.footerLabel,
                        costText = formatCost(entry.costMinor),
                        note = entry.note,
                        onEdit = entry.onEdit,
                        onDelete = entry.onDelete
                    )
                }
            }
        }
    }
}
