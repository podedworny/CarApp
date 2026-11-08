package com.example.carapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

import com.example.carapp.domain.model.Car

@Composable
fun CarCard(car: Car, onDelete: () -> Unit = {}, onOpen: () -> Unit = {}) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("${car.brand} ${car.model}", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.semantics {
                        contentDescription = "Menu pojazdu ${car.brand} ${car.model}"
                    }) { Text("⋮", style = MaterialTheme.typography.headlineSmall) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Otwórz pojazd") },
                            onClick = { menuOpen = false; onOpen() })
                        DropdownMenuItem(text = { Text("Usuń samochód", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
            Text("Rocznik ${car.year}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Otwórz pojazd →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(car.registration.ifBlank { "Brak rejestracji" })
                Text("${NumberFormat.getIntegerInstance(Locale.forLanguageTag("pl-PL")).format(car.mileage)} km",
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
