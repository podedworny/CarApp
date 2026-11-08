package com.example.carapp.ui.addcar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp

/** Grouping is presentation only: state and persistence always contain plain digits. */
internal object MileageTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = raw.reversed().chunked(3).joinToString(" ").reversed()
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 0
                var digits = 0
                formatted.forEachIndexed { index, character ->
                    if (character != ' ') digits++
                    if (digits == offset) return index + 1
                }
                return formatted.length
            }
            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count { it != ' ' }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

@Composable
fun MileageInput(value: String, onChange: (String) -> Unit, enabled: Boolean, error: Boolean, label: String = "Przebieg *") {
    val mileage = value.toIntOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
            placeholder = { Text("np. 85 000") }, suffix = { Text("km") },
            enabled = enabled, singleLine = true, isError = error,
            visualTransformation = MileageTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = { Text(if (error) "Podaj poprawny przebieg" else "Wybierz wartość i dopasuj ją do licznika.") },
            modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 50000, 100000, 150000, 200000).forEach { preset ->
                SuggestionChip(onClick = { onChange(preset.toString()) }, enabled = enabled,
                    label = { Text(if (preset == 0) "Nowe auto · 0" else "${preset / 1000} tys.") })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onChange((mileage!! - 1000).coerceAtLeast(0).toString()) },
                enabled = enabled && mileage != null && mileage > 0, modifier = Modifier.weight(1f)) {
                Text("− 1 000 km")
            }
            OutlinedButton(onClick = { onChange((mileage!!.toLong() + 1000).coerceAtMost(Int.MAX_VALUE.toLong()).toString()) },
                enabled = enabled && mileage != null && mileage < Int.MAX_VALUE, modifier = Modifier.weight(1f)) {
                Text("+ 1 000 km")
            }
        }
    }
}
