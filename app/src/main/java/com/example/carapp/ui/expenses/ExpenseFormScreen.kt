package com.example.carapp.ui.expenses

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.model.ExpenseType
import com.example.carapp.domain.model.FuelSource
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.domain.validation.validateExpense
import com.example.carapp.ui.components.BackFormButton
import com.example.carapp.ui.components.PastDateField

@Composable
fun ExpenseFormScreen(
    car: Car,
    state: ExpenseFormState,
    onBack: () -> Unit,
    onField: (ExpenseField, String) -> Unit,
    onType: (ExpenseType) -> Unit,
    onDate: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errors = validateExpense(state.title, state.dateMillis, state.cost, car.year, ServiceDates.todayMillis())
    LazyColumn(
        modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BackFormButton("Wróć do pojazdu", onBack, enabled = !state.saving) }
        item {
            Text(if (state.isEditing) "Edytuj koszt eksploatacji" else "Dodaj koszt eksploatacji", style = MaterialTheme.typography.headlineMedium)
            Text("${car.brand} ${car.model}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Pola z * są wymagane. Wpisz tankowanie, ubezpieczenie lub inny wydatek poza serwisem.")
        }
        item {
            Text("Kategoria wydatku", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpenseType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        enabled = !state.saving,
                        onClick = { onType(type) },
                        label = { Text(type.displayName(car.fuelSources)) }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.title,
                onValueChange = { onField(ExpenseField.TITLE, it) },
                label = { Text("Czego dotyczy wydatek? *") },
                enabled = !state.saving,
                isError = state.submitted && errors.title,
                singleLine = true,
                supportingText = { if (state.submitted && errors.title) Text("Podaj nazwę wydatku") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                titleSuggestions(state.type, car.fuelSources).forEach { title ->
                    SuggestionChip(
                        onClick = { onField(ExpenseField.TITLE, title) },
                        enabled = !state.saving,
                        label = { Text(title) }
                    )
                }
            }
            if (state.type == ExpenseType.FUEL && car.fuelSources.isEmpty()) {
                Text(
                    "Ustaw rodzaj napędu w edycji auta, aby zawęzić podpowiedzi paliwa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            PastDateField(
                value = state.dateMillis,
                productionYear = car.year,
                enabled = !state.saving,
                error = state.submitted && errors.date,
                onDate = onDate,
                label = "Data wydatku *",
                accessibilityLabel = "Wybierz datę wydatku"
            )
        }
        item {
            OutlinedTextField(
                value = state.cost,
                onValueChange = { onField(ExpenseField.COST, it) },
                enabled = !state.saving,
                label = { Text("Kwota *") },
                placeholder = { Text("np. 250,00") },
                suffix = { Text("zł") },
                singleLine = true,
                isError = state.submitted && errors.cost,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    if (state.submitted && errors.cost) {
                        Text("Podaj kwotę od 0 do 99 999 999,99 zł (do 2 miejsc po przecinku)")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = state.note,
                onValueChange = { onField(ExpenseField.NOTE, it) },
                enabled = !state.saving,
                label = { Text("Notatka (opcjonalnie)") },
                placeholder = { Text("Stacja, ubezpieczyciel, dodatkowe informacje…") },
                minLines = 2,
                maxLines = 5,
                supportingText = { Text("${state.note.length}/1000") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        state.saveError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(if (state.saving) "Zapisywanie…" else if (state.isEditing) "Zapisz zmiany" else "Zapisz koszt")
            }
        }
    }
}

internal fun fuelSuggestions(sources: Set<FuelSource>): List<String> {
    if (sources.isEmpty()) {
        return listOf("Tankowanie benzyny", "Tankowanie diesla", "Tankowanie LPG", "Ładowanie EV")
    }
    if (sources.size == 1) {
        return when (sources.single()) {
            FuelSource.PETROL -> listOf("Tankowanie benzyny", "Benzyna 95", "Benzyna 98")
            FuelSource.DIESEL -> listOf("Tankowanie diesla", "AdBlue")
            FuelSource.LPG -> listOf("Tankowanie LPG")
            FuelSource.ELECTRIC -> listOf("Ładowanie EV", "Ładowanie domowe", "Szybka ładowarka")
        }
    }
    return buildList {
        if (FuelSource.PETROL in sources) add("Tankowanie benzyny")
        if (FuelSource.DIESEL in sources) {
            add("Tankowanie diesla")
            add("AdBlue")
        }
        if (FuelSource.LPG in sources) add("Tankowanie LPG")
        if (FuelSource.ELECTRIC in sources) add("Ładowanie EV")
    }
}

private fun titleSuggestions(type: ExpenseType, fuelSources: Set<FuelSource>): List<String> = when (type) {
    ExpenseType.FUEL -> fuelSuggestions(fuelSources)
    ExpenseType.INSURANCE -> listOf("Polisa OC", "Pakiet OC/AC", "Assistance")
    ExpenseType.CAR_WASH -> listOf("Myjnia")
    ExpenseType.OTHER -> listOf("Zakup opon", "Akcesoria", "Parking")
}
