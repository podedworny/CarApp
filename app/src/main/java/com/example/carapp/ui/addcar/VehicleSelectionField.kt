package com.example.carapp.ui.addcar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** A read-only selector that opens a searchable, scrollable list without showing a keyboard. */
@Composable
fun VehicleSelectionField(
    value: String,
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    allowCustom: Boolean = true,
    customLimit: Int = 70
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true,
            enabled = enabled, label = { Text(label) }, placeholder = { Text("Wybierz z listy") },
            trailingIcon = { Text("▾") }, isError = error != null,
            supportingText = error?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
        // Overlay makes the complete field a single accessible button, including its label.
        Box(Modifier.matchParentSize().semantics { contentDescription = "$label, ${value.ifBlank { "Nie wybrano" }}" }.clickable(enabled = enabled, role = Role.Button,
            onClickLabel = label) { open = true })
    }
    if (open && enabled) {
        SelectionDialog(label, value, options, allowCustom, customLimit,
            onDismiss = { open = false }, onSelect = { onSelect(it); open = false })
    }
}

@Composable
private fun SelectionDialog(
    label: String, selected: String, options: List<String>, allowCustom: Boolean,
    customLimit: Int, onDismiss: () -> Unit, onSelect: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var custom by rememberSaveable { mutableStateOf(false) }
    var customValue by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, options) { options.filter { VehicleSuggestions.searchKey(it).contains(VehicleSuggestions.searchKey(query.trim())) } }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp).fillMaxWidth().heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label.removeSuffix(" *"), style = MaterialTheme.typography.titleLarge)
                if (custom) {
                    OutlinedTextField(value = customValue, onValueChange = { customValue = it.take(customLimit) },
                        label = { Text("Wpisz własną wartość") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onSelect(customValue.trim()) }, enabled = customValue.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()) { Text("Użyj tej wartości") }
                    TextButton(onClick = { custom = false }) { Text("Wróć do listy") }
                } else {
                    OutlinedTextField(value = query, onValueChange = { query = it },
                        label = { Text("Szukaj") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Wyniki: ${filtered.size} · Przewiń listę, aby zobaczyć więcej", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        if (filtered.isEmpty()) item { Text("Brak pasujących wyników", Modifier.padding(12.dp)) }
                        items(filtered, key = { it }) { option ->
                            Row(Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 4.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                RadioButton(selected = option == selected, onClick = null)
                                Text(option, Modifier.padding(12.dp))
                            }
                        }
                    }
                    if (allowCustom) TextButton(onClick = { customValue = query; custom = true }) {
                        Text("Nie ma na liście? Wpisz ręcznie")
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Anuluj") }
            }
        }
    }
}
