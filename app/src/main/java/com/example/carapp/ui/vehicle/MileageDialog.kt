package com.example.carapp.ui.vehicle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.carapp.ui.addcar.MileageInput

@Composable
fun MileageDialog(state: MileageState, onChange: (String) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!state.saving) onCancel() }, title = { Text("Aktualizuj przebieg") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MileageInput(state.value, onChange, !state.saving, state.error != null)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Przebieg zaktualizuje statusy przypomnień. Wpisy historyczne pozostaną bez zmian.")
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = !state.saving) { Text(if (state.saving) "Zapisywanie…" else "Zapisz przebieg") } },
        dismissButton = { TextButton(onClick = onCancel, enabled = !state.saving) { Text("Anuluj") } })
}
