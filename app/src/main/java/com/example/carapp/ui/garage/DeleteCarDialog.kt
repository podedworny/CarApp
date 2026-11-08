package com.example.carapp.ui.garage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeleteCarDialog(state: DeleteCarState, onContinue: () -> Unit,
                    onDelete: () -> Unit, onCancel: () -> Unit) {
    val finalStep = state.step == DeleteCarStep.FINAL_CONFIRMATION
    AlertDialog(
        onDismissRequest = { if (!state.deleting) onCancel() },
        title = { Text(if (finalStep) "Potwierdź usunięcie (2/2)" else "Usunąć samochód? (1/2)") },
        text = {
            Column {
                Text("${state.car.brand} ${state.car.model} · ${state.car.year}")
                if (state.car.registration.isNotBlank()) Text(state.car.registration)
                Spacer(Modifier.height(12.dp))
                Text(if (finalStep) "Samochód, jego historia serwisowa i przypomnienia zostaną trwale usunięte. Tej operacji nie można cofnąć."
                     else "Chcesz usunąć ten samochód z garażu? W następnym kroku poprosimy o ostateczne potwierdzenie.")
                state.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (finalStep) {
                Button(onClick = onDelete, enabled = !state.deleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError)) {
                    Text(if (state.deleting) "Usuwanie…" else "Usuń trwale")
                }
            } else {
                TextButton(onClick = onContinue) { Text("Dalej") }
            }
        },
        dismissButton = { TextButton(onClick = onCancel, enabled = !state.deleting) { Text("Anuluj") } }
    )
}
