package com.example.carapp.ui.addcar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddCarScreen(state: AddCarState, modifier: Modifier = Modifier,
    onBack: () -> Unit, onChange: (CarField, String) -> Unit, onSave: () -> Unit) {
    val brand = state.brand
    val model = state.model
    val year = state.year
    val mileage = state.mileage
    val registration = state.registration
    val saving = state.saving
    val saveError = state.saveError
    val submitted = state.submitted
    val errors = state.errors
    val yearValid = !errors.year
    val mileageValid = !errors.mileage
    LazyColumn(modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack, enabled = !saving) { Text(if (state.isEditing) "← Wróć do pojazdu" else "← Wróć do garażu") } }
        item {
            Text(if (state.isEditing) "Edytuj dane" else "Dodaj samochód", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Podaj podstawowe dane. Pola oznaczone * są wymagane.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { VehicleSelectionField(brand, "Marka *", VehicleSuggestions.brands, { onChange(CarField.BRAND, it) }, enabled = !saving, error = if (submitted && errors.brand) "Wybierz markę" else null, customLimit = 50) }
        item { VehicleSelectionField(model, "Model *", VehicleSuggestions.models(brand), { onChange(CarField.MODEL, it) }, enabled = !saving && brand.isNotBlank(), error = if (submitted && errors.model) "Wybierz model" else null) }
        item { YearSelectionField(year, state.maximumYear, enabled = !saving, error = submitted && !yearValid, onSelect = { onChange(CarField.YEAR, it) }) }
        if (!state.isEditing) {
            item { MileageInput(mileage, { onChange(CarField.MILEAGE, it) }, enabled = !saving, error = submitted && !mileageValid) }
        }
        item { CarInputField(registration, { onChange(CarField.REGISTRATION, it) }, "Numer rejestracyjny", "Opcjonalnie", false, "", saving) }
        if (saveError != null) item { Text(saveError, color = MaterialTheme.colorScheme.error) }
        item {
            Button(onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(if (saving) "Zapisywanie…" else if (state.isEditing) "Zapisz zmiany" else "Zapisz samochód")
            }
        }
    }
}

@Composable
private fun CarInputField(value: String, onChange: (String) -> Unit, label: String, placeholder: String,
                     error: Boolean, message: String, saving: Boolean, numeric: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, placeholder = { Text(placeholder) }, singleLine = true,
        enabled = !saving, isError = error,
        supportingText = if (error) ({ Text(message) }) else null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words,
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text))
}
