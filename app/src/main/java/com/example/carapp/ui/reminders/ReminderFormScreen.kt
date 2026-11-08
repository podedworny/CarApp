package com.example.carapp.ui.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.domain.validation.validateReminder
import com.example.carapp.ui.addcar.MileageInput
import com.example.carapp.ui.service.formatServiceDate
import java.util.Calendar

@Composable
fun ReminderFormScreen(car: Car, state: ReminderFormState, onBack: () -> Unit,
    onTitle: (String) -> Unit, onDate: (Long) -> Unit, onMileage: (String) -> Unit,
    onUseDate: (Boolean) -> Unit, onUseMileage: (Boolean) -> Unit, onSave: () -> Unit,
    modifier: Modifier = Modifier) {
    val errors = validateReminder(state.title, state.dateEnabled, state.dateMillis, state.mileageEnabled, state.mileage, car.year)
    LazyColumn(modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack, enabled = !state.saving) { Text("← Wróć do pojazdu") } }
        item {
            Text("Nowe przypomnienie", style = MaterialTheme.typography.headlineMedium)
            Text("${car.brand} ${car.model}")
        }
        item {
            OutlinedTextField(state.title, onTitle, label = { Text("Czego dotyczy? *") }, singleLine = true,
                enabled = !state.saving, isError = state.submitted && errors.title,
                supportingText = { if (state.submitted && errors.title) Text("Podaj nazwę czynności") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Wymiana oleju", "Badanie techniczne", "Wymiana rozrządu", "Wymiana opon").forEach { title ->
                    SuggestionChip(onClick = { onTitle(title) }, enabled = !state.saving, label = { Text(title) })
                }
            }
        }
        item {
            Text("Kiedy przypomnieć?", style = MaterialTheme.typography.titleMedium)
            Text("Najlepiej ustaw datę — działa bez regularnego sprawdzania licznika. Opcjonalnie dodaj przebieg; wtedy obowiązuje wcześniejszy próg.")
            if (state.submitted && errors.target) Text("Włącz co najmniej jeden próg.", color = MaterialTheme.colorScheme.error)
        }
        item {
            TargetSwitch("Termin według daty (zalecany)", state.dateEnabled, !state.saving, onUseDate, "Według daty")
            if (state.dateEnabled) {
                ReminderDateField(state.dateMillis, car.year, !state.saving, state.submitted && errors.date, onDate)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90, 365).forEach { days ->
                        SuggestionChip(onClick = { onDate(ServiceDates.todayMillis() + days.toLong() * 86400000) },
                            enabled = !state.saving, label = { Text("Za $days dni") })
                    }
                }
            }
        }
        item {
            TargetSwitch("Próg przebiegu (opcjonalnie)", state.mileageEnabled, !state.saving, onUseMileage, "Według przebiegu")
            if (state.mileageEnabled) {
                MileageInput(state.mileage, onMileage, !state.saving, state.submitted && errors.mileage, label = "Przebieg docelowy *")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1000, 5000, 10000, 15000).forEach { delta ->
                        SuggestionChip(onClick = { onMileage((car.mileage.toLong() + delta).coerceAtMost(Int.MAX_VALUE.toLong()).toString()) },
                            enabled = !state.saving, label = { Text("Za ${delta / 1000} tys. km") })
                    }
                }
                Text("Próg będzie sprawdzany według ostatniego zapisanego licznika. Możesz potwierdzić jego aktualizację przy dodawaniu serwisu.", style = MaterialTheme.typography.bodySmall)
                if (!state.dateEnabled) Text("Bez daty przypomnienie zależy wyłącznie od aktualności licznika. Zalecamy także ustawienie terminu.", style = MaterialTheme.typography.bodySmall)
            }
        }
        state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        item {
            Text("Przypomnienia są widoczne w aplikacji. Powiadomienia systemowe nie są jeszcze włączone.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.saving) "Zapisywanie…" else "Zapisz przypomnienie")
            }
        }
    }
}

@Composable
private fun TargetSwitch(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit, accessibilityLabel: String = label) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange, enabled = enabled, modifier = Modifier.semanticsLabel(accessibilityLabel))
    }
}

private fun Modifier.semanticsLabel(label: String) = this.then(Modifier.semantics { contentDescription = label })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDateField(value: Long?, productionYear: Int, enabled: Boolean, error: Boolean, onDate: (Long) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedTextField(value?.let(::formatServiceDate).orEmpty(), {}, readOnly = true, enabled = enabled,
            label = { Text("Termin *") }, trailingIcon = { Text("▾") }, isError = error,
            supportingText = { if (error) Text("Wybierz datę od roku produkcji") }, modifier = Modifier.fillMaxWidth())
        Box(Modifier.matchParentSize().semantics { contentDescription = "Wybierz termin przypomnienia" }.clickable(enabled = enabled, role = Role.Button, onClickLabel = "Wybierz termin") { open = true })
    }
    if (open && enabled) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val picker = rememberDatePickerState(initialSelectedDateMillis = value,
            yearRange = minOf(productionYear, currentYear)..maxOf(currentYear + 20, productionYear),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= ServiceDates.dateMillis(productionYear, 1, 1)
            })
        DatePickerDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let(onDate); open = false },
                enabled = picker.selectedDateMillis != null) { Text("Wybierz") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Anuluj") } }) { DatePicker(picker) }
    }
}
