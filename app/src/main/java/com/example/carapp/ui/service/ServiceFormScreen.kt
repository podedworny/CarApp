package com.example.carapp.ui.service

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.model.ServiceType
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.domain.validation.validateService
import com.example.carapp.ui.addcar.MileageInput
import com.example.carapp.ui.components.BackFormButton
import com.example.carapp.ui.components.PastDateField

@Composable
fun ServiceFormScreen(car: Car, state: ServiceFormState, onBack: () -> Unit,
                      onField: (ServiceField, String) -> Unit, onType: (ServiceType) -> Unit,
                      onDate: (Long) -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val errors = validateService(state.title, state.dateMillis, state.mileage, state.cost, car.year, ServiceDates.todayMillis())
    LazyColumn(modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { BackFormButton("Wróć do pojazdu", onBack, enabled = !state.saving) }
        item {
            Text(if (state.isEditing) "Edytuj wpis serwisowy" else "Dodaj wpis serwisowy", style = MaterialTheme.typography.headlineMedium)
            Text("${car.brand} ${car.model}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Pola z * są wymagane. Możesz też uzupełnić starsze naprawy i przeglądy.")
        }
        item {
            Text("Rodzaj czynności", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceType.entries.forEach { type ->
                    FilterChip(selected = state.type == type, enabled = !state.saving,
                        onClick = { onType(type) }, label = { Text(type.displayName()) })
                }
            }
        }
        item {
            OutlinedTextField(state.title, { onField(ServiceField.TITLE, it) }, label = { Text("Co zostało zrobione? *") },
                enabled = !state.saving, isError = state.submitted && errors.title, singleLine = true,
                supportingText = { if (state.submitted && errors.title) Text("Podaj nazwę czynności") },
                modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                titleSuggestions(state.type).forEach { title ->
                    SuggestionChip(onClick = { onField(ServiceField.TITLE, title) }, enabled = !state.saving,
                        label = { Text(title) })
                }
            }
        }
        item {
            PastDateField(
                value = state.dateMillis,
                productionYear = car.year,
                enabled = !state.saving,
                error = state.submitted && errors.date,
                onDate = onDate,
                accessibilityLabel = "Wybierz datę wykonania czynności"
            )
        }
        item {
            MileageInput(state.mileage, { onField(ServiceField.MILEAGE, it) }, !state.saving, state.submitted && errors.mileage)
            Text("Podaj stan licznika w dniu wykonania czynności.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(state.cost, { onField(ServiceField.COST, it) }, enabled = !state.saving,
                label = { Text("Koszt (opcjonalnie)") }, placeholder = { Text("np. 450,00") }, suffix = { Text("zł") },
                singleLine = true, isError = state.submitted && errors.cost,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text(if (state.submitted && errors.cost) "Podaj kwotę od 0 do 99 999 999,99 zł (do 2 miejsc po przecinku)"
                    else "Zostaw puste, jeśli nie znasz kosztu. Wpisz 0 dla bezpłatnej czynności.") },
                modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(state.note, { onField(ServiceField.NOTE, it) }, enabled = !state.saving,
                label = { Text("Notatka (opcjonalnie)") }, placeholder = { Text("Warsztat, użyte części, dodatkowe informacje…") },
                minLines = 3, maxLines = 6, supportingText = { Text("${state.note.length}/1000") },
                modifier = Modifier.fillMaxWidth())
        }
        state.saveError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(if (state.saving) "Zapisywanie…" else if (state.isEditing) "Zapisz zmiany" else "Zapisz wpis")
            }
        }
    }
}

private fun titleSuggestions(type: ServiceType): List<String> = when (type) {
    ServiceType.MAINTENANCE -> listOf("Wymiana oleju i filtrów", "Wymiana opon", "Wymiana rozrządu", "Serwis klimatyzacji")
    ServiceType.REPAIR -> listOf("Naprawa hamulców", "Naprawa zawieszenia", "Wymiana akumulatora")
    ServiceType.INSPECTION -> listOf("Badanie techniczne", "Przegląd okresowy", "Diagnostyka komputerowa")
    ServiceType.OTHER -> listOf("Konserwacja podwozia", "Detailing")
}
