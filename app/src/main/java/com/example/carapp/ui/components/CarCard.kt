package com.example.carapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.Car
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CarCard(car: Car, onDelete: () -> Unit = {}, onOpen: () -> Unit = {}) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        AppIcon(CarAppIcon.CAR, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(car.brand.uppercase(Locale.ROOT), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(car.model, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(car.year.toString(), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        AppIcon(CarAppIcon.MORE, "Menu pojazdu ${car.brand} ${car.model}")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Otwórz pojazd") }, onClick = { menuOpen = false; onOpen() })
                        DropdownMenuItem(text = { Text("Usuń samochód", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RegistrationPlate(car.registration, Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(12.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("${NumberFormat.getIntegerInstance(Locale.forLanguageTag("pl-PL")).format(car.mileage)} km",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    AppIcon(CarAppIcon.NEXT, modifier = Modifier.padding(start = 8.dp).size(18.dp))
                }
            }
        }
    }
}
