package com.example.carapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.carapp.data.*
import com.example.carapp.domain.model.Car
import com.example.carapp.ui.service.formatCost
import com.example.carapp.ui.theme.CarAppTheme
import com.example.carapp.ui.vehicle.VehicleRoute
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ServiceFlowInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-flow-${UUID.randomUUID()}"
    private val storage = LocalGarageStorage(context, preferenceName = name)
    private val services = LocalServiceRepository(storage)
    private val car = Car(id = "ui-test", brand = "Toyota", model = "Corolla", year = 2010, mileage = 85000, registration = "TEST")

    @After fun cleanUp() { context.deleteSharedPreferences(name) }

    private fun scrollTo(text: String): SemanticsNodeInteraction {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        return compose.onNodeWithText(text)
    }

    @Test fun mileageReminderBecomesDueAndCanBeCompleted() {
        val cars = LocalCarRepository(storage)
        val reminders = LocalReminderRepository(storage)
        runBlocking { cars.addCar(car) }
        compose.setContent { CarAppTheme { VehicleRoute(car, services, cars, reminders, onBack = {}, onDelete = {}) } }
        compose.onNodeWithText("Przypomnienia").performScrollTo().performClick()
        compose.waitUntil(5000) {
            compose.onAllNodes(hasText("+ Dodaj przypomnienie") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("+ Dodaj przypomnienie").performClick()
        compose.onNodeWithText("Czego dotyczy? *").performTextInput("Olej testowy")
        scrollTo("Według daty")
        compose.onNodeWithContentDescription("Według daty").performClick()
        scrollTo("Według przebiegu")
        compose.onNodeWithContentDescription("Według przebiegu").performClick()
        scrollTo("Przebieg docelowy *").performTextReplacement("86000")
        scrollTo("Zapisz przypomnienie").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Serwis").fetchSemanticsNodes().isNotEmpty() }
        scrollTo("Olej testowy").assertIsDisplayed()
        compose.onNodeWithText("Nadchodzące").assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("Akcje pojazdu"))
        compose.onNodeWithContentDescription("Akcje pojazdu").performClick()
        compose.onNodeWithText("Aktualizuj przebieg").performClick()
        compose.onNodeWithText("Przebieg *").performTextReplacement("86000")
        compose.onNodeWithText("Zapisz przebieg").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Zapisz przebieg").fetchSemanticsNodes().isEmpty() }
        scrollTo("Do wykonania").assertIsDisplayed()
        scrollTo("Oznacz jako wykonane").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Wykonane").fetchSemanticsNodes().isNotEmpty() }
        assertNotNull(runBlocking { reminders.getReminders(car.id).single().completedDateMillis })
        assertEquals(86000, runBlocking { cars.getCars().single().mileage })
    }
    @Test fun dateOnlyReminderUsesCalendarAndReopeningKeepsTheDraft() {
        val cars = LocalCarRepository(storage)
        val reminders = LocalReminderRepository(storage)
        runBlocking { cars.addCar(car) }
        compose.setContent { CarAppTheme { VehicleRoute(car, services, cars, reminders, onBack = {}, onDelete = {}) } }
        compose.onNodeWithText("Przypomnienia").performScrollTo().performClick()
        compose.waitUntil(5000) {
            compose.onAllNodes(hasText("+ Dodaj przypomnienie") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("+ Dodaj przypomnienie").performClick()
        compose.onNodeWithText("Czego dotyczy? *").performTextInput("Badanie testowe")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithContentDescription("Wybierz termin przypomnienia").performScrollTo().performClick()
        compose.onNodeWithText("Wybierz").performClick()
        scrollTo("← Wróć do pojazdu").performClick()
        compose.onNodeWithText("+ Dodaj przypomnienie").performClick()
        compose.onNodeWithText("Badanie testowe").assertExists()
        scrollTo("Zapisz przypomnienie").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Serwis").fetchSemanticsNodes().isNotEmpty() }
        val saved = runBlocking { reminders.getReminders(car.id).single() }
        assertNotNull(saved.dueDateMillis)
        assertNull(saved.dueMileage)
        assertEquals("Badanie testowe", saved.title)
    }

    @Test fun addServiceAndSeeItInHistoryAndCostSummary() {
        runBlocking { LocalCarRepository(storage).addCar(car) }
        compose.setContent { CarAppTheme { VehicleRoute(car, services, LocalCarRepository(storage), LocalReminderRepository(storage), onBack = {}, onDelete = {}) } }
        compose.onNodeWithText("Serwis").performClick()
        compose.waitUntil(5000) {
            compose.onAllNodes(hasText("+ Dodaj wpis serwisowy") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("+ Dodaj wpis serwisowy").performClick()
        compose.onNodeWithText("Co zostało zrobione? *").performTextInput("Serwis testowy")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        scrollTo("Koszt (opcjonalnie)").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Koszt (opcjonalnie)").performTextInput("450,35")
        scrollTo("Zapisz wpis").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Serwis").fetchSemanticsNodes().isNotEmpty() }
        scrollTo("Serwis testowy").assertIsDisplayed()
        scrollTo("Koszty").performClick()
        scrollTo("Łączne koszty serwisu").assertIsDisplayed()
        compose.onAllNodesWithText(formatCost(45035)).onFirst().assertIsDisplayed()
        val persisted = runBlocking { services.getEntries(car.id).single() }
        assertEquals(45035L, persisted.costMinor)
        assertEquals(car.id, persisted.carId)
    }
}
