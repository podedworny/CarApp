package com.example.carapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.ui.service.formatServiceDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDateField(
    value: Long?,
    productionYear: Int,
    enabled: Boolean,
    error: Boolean,
    onDate: (Long) -> Unit,
    label: String = "Data wykonania *",
    accessibilityLabel: String = "Wybierz datę"
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val today = ServiceDates.todayMillis()
    Box {
        OutlinedTextField(
            value = value?.let(::formatServiceDate).orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { AppIcon(CarAppIcon.DROPDOWN) },
            isError = error,
            supportingText = { if (error) Text("Wybierz datę od roku produkcji do dzisiaj") },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier.matchParentSize()
                .semantics { contentDescription = accessibilityLabel }
                .clickable(enabled = enabled, role = Role.Button) { open = true }
        )
    }
    if (open && enabled) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = value?.coerceAtMost(today) ?: today,
            yearRange = minOf(productionYear, currentYear)..currentYear,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis in ServiceDates.dateMillis(productionYear, 1, 1)..today
            }
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = { picker.selectedDateMillis?.let(onDate); open = false },
                    enabled = picker.selectedDateMillis != null
                ) { Text("Wybierz") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Anuluj") } }
        ) {
            DatePicker(state = picker)
        }
    }
}

