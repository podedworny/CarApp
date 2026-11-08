package com.example.carapp.ui.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.*
import com.example.carapp.ui.service.formatServiceDate

fun LazyListScope.reminderContent(state: ReminderState, currentMileage: Int, today: Long,
                                  onAdd: () -> Unit, onRetry: () -> Unit, onComplete: (String) -> Unit) {
    item {
        Text("Przypomnienia", style = MaterialTheme.typography.titleLarge)
        Text("Według zapisanego przebiegu: $currentMileage km. Aktualizuj licznik, aby statusy były aktualne.")
        Button(onClick = onAdd, enabled = !state.loading && !state.loadError && state.updatingId == null) { Text("+ Dodaj przypomnienie") }
        Text("Widoczne w aplikacji · bez powiadomień systemowych", style = MaterialTheme.typography.bodySmall)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    when {
        state.loading -> item { CircularProgressIndicator() }
        state.loadError -> item {
            Text("Nie udało się odczytać przypomnień.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Spróbuj ponownie") }
        }
        state.reminders.isEmpty() -> item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Text("Nie masz jeszcze przypomnień. Dodaj termin przeglądu lub następnej wymiany oleju.", Modifier.padding(20.dp))
            }
        }
        else -> {
            val sorted = state.reminders.sortedWith(compareBy<ServiceReminder> { it.status(today, currentMileage).ordinal }
                .thenBy { it.dueDateMillis ?: Long.MAX_VALUE }.thenBy { it.dueMileage ?: Int.MAX_VALUE })
            items(sorted, key = { "reminder:${it.id}" }) { reminder ->
                ReminderCard(reminder, reminder.status(today, currentMileage), state.updatingId == reminder.id,
                    state.updatingId == null, onComplete)
            }
        }
    }
}

@Composable
private fun ReminderCard(reminder: ServiceReminder, status: ReminderStatus, updating: Boolean,
                         enabled: Boolean, onComplete: (String) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(when (status) {
                ReminderStatus.OVERDUE -> "Zaległe"
                ReminderStatus.DUE -> "Do wykonania"
                ReminderStatus.UPCOMING -> "Nadchodzące"
                ReminderStatus.PLANNED -> "Zaplanowane"
                ReminderStatus.COMPLETED -> "Wykonane"
            }, color = if (status == ReminderStatus.OVERDUE || status == ReminderStatus.DUE) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text(reminder.title, style = MaterialTheme.typography.titleMedium)
            reminder.dueDateMillis?.let { Text("Termin: ${formatServiceDate(it)}") }
            reminder.dueMileage?.let { Text("Przy przebiegu: $it km") }
            reminder.completedDateMillis?.let { Text("Wykonano: ${formatServiceDate(it)}") }
            if (reminder.dueDateMillis != null && reminder.dueMileage != null) Text("Obowiązuje wcześniejszy próg.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onComplete(reminder.id) }, enabled = enabled) {
                Text(if (updating) "Zapisywanie…" else if (status == ReminderStatus.COMPLETED) "Przywróć zadanie" else "Oznacz jako wykonane")
            }
        }
    }
}
