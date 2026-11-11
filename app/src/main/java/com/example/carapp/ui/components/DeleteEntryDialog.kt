package com.example.carapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun DeleteEntryDialog(
    entryTitle: String,
    deleting: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("Usunąć wpis?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Czy na pewno chcesz usunąć wpis „$entryTitle”? Tej operacji nie można cofnąć.")
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !deleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(if (deleting) "Usuwanie…" else "Usuń") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !deleting) { Text("Anuluj") }
        }
    )
}

