package com.example.carapp.ui

import com.example.carapp.ui.addcar.VehicleSuggestions
import com.example.carapp.ui.addcar.yearsInDecade
import org.junit.Assert.*
import org.junit.Test

class VehicleSelectionTest {
    @Test fun `older production years remain selectable across every decade`() {
        val years = (1880..2020 step 10).flatMap { yearsInDecade(it, 2027) }
        assertEquals((1886..2027).toList(), years)
        assertTrue(years.containsAll(listOf(1995, 2004, 2010, 2020)))
    }

    @Test fun `catalog covers older models and accentless brand names`() {
        assertTrue(VehicleSuggestions.brands.size >= 50)
        assertTrue(VehicleSuggestions.models("skoda").containsAll(listOf("Felicia", "Octavia")))
        assertTrue(VehicleSuggestions.models(" CITROEN ").contains("Xantia"))
        assertTrue(VehicleSuggestions.models("Audi").contains("80"))
        VehicleSuggestions.brands.forEach { brand ->
            val models = VehicleSuggestions.models(brand)
            assertTrue(models.isNotEmpty())
            assertEquals(models.distinct(), models)
        }
    }
}
