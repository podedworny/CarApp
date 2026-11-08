package com.example.carapp.ui.garage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carapp.domain.model.Car
import com.example.carapp.ui.components.*
import com.example.carapp.ui.theme.CarAppTheme

@Composable
fun GarageScreen(cars: List<Car>, loaded: Boolean, loadError: Boolean,
    modifier: Modifier = Modifier, onRetry: () -> Unit = {}, onDelete: (String) -> Unit = {},
    onOpen: (String) -> Unit = {}, onAdd: () -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CARAPP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text("Mój garaż", style = MaterialTheme.typography.headlineLarge)
                }
                FilledTonalButton(onClick = onAdd, enabled = loaded && !loadError,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    AppIcon(CarAppIcon.ADD, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(6.dp))
                    Text("Dodaj auto")
                }
            }
        }
        when {
            !loaded -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            loadError -> item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Nie udało się odczytać garażu.")
                        TextButton(onClick = onRetry) { Text("Spróbuj ponownie") }
                    }
                }
            }
            cars.isEmpty() -> item {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CarIllustration(Modifier.fillMaxWidth().height(120.dp))
                        Text("Miejsce na Twoje auto", style = MaterialTheme.typography.titleLarge)
                        Text("Dodaj samochód. Jego serwis, koszty i terminy znajdziesz w jednym miejscu.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Button(onClick = onAdd) { Text("Dodaj pierwszy samochód") }
                    }
                }
            }
            else -> {
                item {
                    Text("Twoje samochody · ${cars.size}", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(cars, key = { it.id }) { car ->
                    CarCard(car, onDelete = { onDelete(car.id) }, onOpen = { onOpen(car.id) })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GaragePreview() {
    CarAppTheme { Surface { GarageScreen(emptyList(), true, false, onAdd = {}) } }
}

@Preview(showBackground = true)
@Composable
private fun GarageWithCarPreview() {
    CarAppTheme { Surface { GarageScreen(listOf(Car(brand = "Toyota", model = "Corolla", year = 2020,
        mileage = 85000, registration = "WA 12345")), true, false, onAdd = {}) } }
}
