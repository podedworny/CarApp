package com.example.carapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RegistrationPlate(registration: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(23.dp)) {
                Text("PL", Modifier.padding(vertical = 7.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Text(registration.ifBlank { "Brak numeru" }, Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
