package com.example.carapp.ui.vehicle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ServiceMileageSuggestionDialog(mileage: Int, saving: Boolean, error: String?,
                                    onConfirm: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!saving) onSkip() },
        title = { Text("Zaktualizować przebieg auta?") },
        text = {
            Column {
                Text("Wpis serwisowy został zapisany. Podany w nim przebieg to ${NumberFormat.getIntegerInstance(Locale.forLanguageTag("pl-PL")).format(mileage)} km — więcej niż zapisany licznik auta.")
                Spacer(Modifier.height(12.dp))
                Text("Jeśli odpowiada aktualnemu stanowi licznika, możesz użyć go do aktualizacji auta i przypomnień. Dla starszego wpisu możesz pominąć ten krok.")
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text("Nie udało się zaktualizować licznika. Wpis serwisowy jest zapisany. Spróbuj ponownie lub wybierz „Nie teraz”.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !saving) { Text(if (saving) "Zapisywanie…" else "Aktualizuj licznik") } },
        dismissButton = { TextButton(onClick = onSkip, enabled = !saving) { Text("Nie teraz") } })
}
