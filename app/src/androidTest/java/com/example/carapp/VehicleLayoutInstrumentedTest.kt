package com.example.carapp

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.carapp.domain.model.Car
import com.example.carapp.domain.validation.ServiceDates
import com.example.carapp.ui.garage.GarageScreen
import com.example.carapp.ui.theme.CarAppTheme
import com.example.carapp.ui.vehicle.VehicleScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Navigation/accessibility checks plus repeatable visual review artifacts on the test device. */
@RunWith(AndroidJUnit4::class)
class VehicleLayoutInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val car = Car(brand = "Toyota", model = "Corolla", year = 2020, mileage = 85000,
        registration = "WA 12345", mileageUpdatedDateMillis = ServiceDates.todayMillis())

    @Test fun garageCardIsReadableAndOpensVehicle() {
        var opened = false
        compose.setContent {
            CarAppTheme(darkTheme = false) {
                Surface(Modifier.fillMaxSize()) {
                    GarageScreen(listOf(car, car.copy(id = "second", brand = "Audi", model = "A4", year = 2017,
                        registration = "KR 8492K", mileage = 164200)), true, false,
                        onOpen = { opened = true }, onAdd = {})
                }
            }
        }
        compose.onNodeWithText("Corolla").assertIsDisplayed()
        compose.onNodeWithText("${java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("pl-PL")).format(car.mileage)} km").assertIsDisplayed()
        screenshot("garage-light")
        compose.onNodeWithText("Corolla").performClick()
        assertTrue(opened)
    }

    @Test fun vehicleOverviewHasPersistentNavigationAndMileageAction() {
        var updating = false
        compose.setContent {
            CarAppTheme(darkTheme = false) { Surface(Modifier.fillMaxSize()) {
                VehicleScreen(car, onBack = {}, onDelete = {}, onMileage = { updating = true })
            } }
        }
        screenshot("vehicle-light")
        compose.onNodeWithText("Aktualizuj").performClick()
        assertTrue(updating)
        compose.onNodeWithText("Serwis").performClick()
        compose.onNodeWithText("Historia serwisowa").assertIsDisplayed()
        compose.onNodeWithContentDescription("Powrót do garażu").assertIsDisplayed()
        compose.onNodeWithText("Koszty").assertIsDisplayed()
        compose.onNodeWithText("Terminy").assertIsDisplayed()
    }

    @Test fun darkThemeKeepsMenuAndNavigationAccessible() {
        compose.setContent {
            CarAppTheme(darkTheme = true) { Surface(Modifier.fillMaxSize()) {
                VehicleScreen(car, onBack = {}, onDelete = {})
            } }
        }
        screenshot("vehicle-dark")
        compose.onNodeWithContentDescription("Akcje pojazdu").performClick()
        compose.onNodeWithText("Usuń samochód").assertIsDisplayed()
        compose.onNodeWithText("Aktualizuj przebieg").assertIsDisplayed()
    }

    @Test fun compactWidthAndLargeTextKeepAllDestinationsAccessible() {
        compose.setContent {
            CarAppTheme(darkTheme = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    Box(Modifier.fillMaxSize()) {
                        Surface(Modifier.width(320.dp).fillMaxHeight().testTag("compact")) {
                            VehicleScreen(car.copy(brand = "Mercedes-Benz", model = "Klasa C"), onBack = {}, onDelete = {})
                        }
                    }
                }
            }
        }
        screenshot("vehicle-compact", "compact")
        compose.onNodeWithText("Terminy").assertIsDisplayed().performClick()
        compose.onNodeWithText("Terminy serwisowe").assertIsDisplayed()
        compose.onNodeWithText("Przegląd").assertIsDisplayed()
        compose.onNodeWithContentDescription("Powrót do garażu").assertIsDisplayed()
    }

    private fun screenshot(name: String, tag: String? = null) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "ui-review/$name.png")
        file.parentFile!!.mkdirs()
        val node = if (tag == null) compose.onRoot() else compose.onNodeWithTag(tag)
        file.outputStream().use { node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Keep review artifacts outside the app directory, which the runner removes after tests.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("mkdir -p /data/local/tmp/carapp-ui-review")).use { it.readBytes() }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/carapp-ui-review/$name.png")).use { it.readBytes() }
    }
}
