package com.example.carapp.ui.garage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carapp.ui.theme.CarAppTheme

import com.example.carapp.domain.model.Car
import com.example.carapp.ui.components.CarCard

@Composable
fun GarageScreen(cars: List<Car>, loaded: Boolean, loadError: Boolean,
                         modifier: Modifier = Modifier, onRetry: () -> Unit = {}, onDelete: (String) -> Unit = {}, onOpen: (String) -> Unit = {}, onAdd: () -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text("CARAPP / TWÓJ GARAŻ", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Dobra droga zaczyna\nsię od zadbanego auta.", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Twoje samochody i ich historia w jednym miejscu.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(24.dp)) {
                    Text("Mój garaż", style = MaterialTheme.typography.titleMedium)
                    Text(if (loaded && !loadError) "${cars.size}" else "—",
                        style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text("Zadbaj o swoje auto. Zacznij od dodania go do garażu.")
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onAdd, enabled = loaded && !loadError) { Text("+  Dodaj samochód") }
                }
            }
        }
        item { Text("Twoje samochody", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        when {
            !loaded -> item { CircularProgressIndicator() }
            loadError -> item {
                Column {
                    Text("Nie udało się odczytać garażu.", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Spróbuj ponownie") }
                }
            }
            cars.isEmpty() -> item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tu zaczyna się historia Twojego auta", style = MaterialTheme.typography.titleMedium)
                        Text("Dodaj pierwszy samochód, aby mieć jego podstawowe dane zawsze pod ręką.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> items(cars, key = { it.id }) { car -> CarCard(car, onDelete = { onDelete(car.id) }, onOpen = { onOpen(car.id) }) }
        }
        item {
            Text("W kolejnych etapach", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Historia serwisowa · Koszty eksploatacji · Przypomnienia",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
