package com.example.carapp

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.carapp.ui.addcar.YearSelectionField
import com.example.carapp.ui.theme.CarAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YearPickerInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun olderYearsCanBeSelectedAndChanged() {
        var selected by mutableStateOf("")
        compose.setContent { CarAppTheme { YearSelectionField(selected, 2027, true, false) { selected = it } } }
        compose.onNodeWithContentDescription("Rok produkcji, Nie wybrano").performClick()
        repeat(3) { compose.onNodeWithContentDescription("Starsza dekada").performClick() }
        compose.onNodeWithText("1995").performClick()
        assertEquals("1995", selected)
        compose.onNodeWithContentDescription("Rok produkcji, 1995").performClick()
        compose.onNodeWithContentDescription("Nowsza dekada").performClick()
        compose.onNodeWithText("2004").performClick()
        assertEquals("2004", selected)
    }
}
