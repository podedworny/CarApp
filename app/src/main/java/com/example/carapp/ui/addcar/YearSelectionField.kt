package com.example.carapp.ui.addcar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

internal fun yearsInDecade(decade: Int, maximumYear: Int): List<Int> =
    (decade..decade + 9).filter { it in 1886..maximumYear }

@Composable
fun YearSelectionField(value: String, maximumYear: Int, enabled: Boolean,
                       error: Boolean, onSelect: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedTextField(value, {}, readOnly = true, enabled = enabled,
            label = { Text("Rok produkcji *") }, placeholder = { Text("Wybierz rocznik") },
            trailingIcon = { Text("▾") }, isError = error, modifier = Modifier.fillMaxWidth(),
            supportingText = { Text(if (error) "Wybierz rok produkcji" else "Dostępne roczniki: 1886–$maximumYear") })
        Box(Modifier.matchParentSize().semantics {
            contentDescription = "Rok produkcji, ${value.ifBlank { "Nie wybrano" }}"
        }.clickable(enabled = enabled, role = Role.Button) { open = true })
    }
    if (open && enabled) {
        YearSelectionDialog(value.toIntOrNull() ?: maximumYear, maximumYear,
            onDismiss = { open = false }, onSelect = { onSelect(it.toString()); open = false })
    }
}

@Composable
private fun YearSelectionDialog(initialYear: Int, maximumYear: Int,
                                onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    var decade by rememberSaveable { mutableIntStateOf(initialYear / 10 * 10) }
    var choosingDecade by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Rok produkcji", style = MaterialTheme.typography.titleLarge)
                Text("Wybierz dekadę z listy lub zmień ją strzałkami. Zakres: 1886–$maximumYear.",
                    style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { decade -= 10 }, enabled = decade > 1880, modifier = Modifier.semantics { contentDescription = "Starsza dekada" }) { Text("←") }
                    Box {
                        TextButton(onClick = { choosingDecade = true }) { Text("${decade}–${minOf(decade + 9, maximumYear)} ▾") }
                        DropdownMenu(expanded = choosingDecade, onDismissRequest = { choosingDecade = false },
                            modifier = Modifier.heightIn(max = 300.dp)) {
                            (maximumYear / 10 * 10 downTo 1880 step 10).forEach { start ->
                                DropdownMenuItem(text = { Text("${maxOf(start, 1886)}–${minOf(start + 9, maximumYear)}") },
                                    onClick = { decade = start; choosingDecade = false })
                            }
                        }
                    }
                    TextButton(onClick = { decade += 10 }, enabled = decade + 10 <= maximumYear, modifier = Modifier.semantics { contentDescription = "Nowsza dekada" }) { Text("→") }
                }
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.heightIn(max = 240.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(yearsInDecade(decade, maximumYear), key = { it }) { year ->
                        OutlinedButton(onClick = { onSelect(year) }, contentPadding = PaddingValues(8.dp)) { Text(year.toString()) }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Anuluj") }
            }
        }
    }
}
