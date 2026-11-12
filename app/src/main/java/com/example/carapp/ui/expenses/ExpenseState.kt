package com.example.carapp.ui.expenses

import com.example.carapp.domain.model.ExpenseEntry
import com.example.carapp.domain.model.ExpenseType
import com.example.carapp.domain.model.FuelSource

enum class ExpenseField { TITLE, COST, NOTE }

data class ExpenseFormState(
    val visible: Boolean = false,
    val editingEntryId: String? = null,
    val type: ExpenseType = ExpenseType.FUEL,
    val title: String = "",
    val dateMillis: Long? = null,
    val cost: String = "",
    val note: String = "",
    val submitted: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null
) {
    val isEditing: Boolean get() = editingEntryId != null
}

data class VehicleExpenseState(
    val entries: List<ExpenseEntry> = emptyList(),
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val form: ExpenseFormState = ExpenseFormState(),
    val deletingEntry: ExpenseEntry? = null,
    val deleting: Boolean = false,
    val deleteError: String? = null
)

fun ExpenseType.displayName(fuelSources: Set<FuelSource> = emptySet()): String = when (this) {
    ExpenseType.FUEL -> when {
        fuelSources == setOf(FuelSource.ELECTRIC) -> "Ładowanie EV"
        FuelSource.ELECTRIC in fuelSources && fuelSources.size > 1 -> "Paliwo / ładowanie"
        else -> "Paliwo"
    }
    ExpenseType.INSURANCE -> "Ubezpieczenie"
    ExpenseType.CAR_WASH -> "Myjnia"
    ExpenseType.OTHER -> "Inne"
}
