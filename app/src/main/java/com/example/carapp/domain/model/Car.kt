package com.example.carapp.domain.model

import java.util.UUID

enum class FuelSource { PETROL, DIESEL, LPG, ELECTRIC }

fun FuelSource.displayName(): String = when (this) {
    FuelSource.PETROL -> "Benzyna"
    FuelSource.DIESEL -> "Diesel"
    FuelSource.LPG -> "LPG"
    FuelSource.ELECTRIC -> "Prąd (EV)"
}

fun Set<FuelSource>.displaySummary(): String =
    FuelSource.entries.filter { it in this }.joinToString(" + ") { it.displayName() }

data class Car(
    val id: String = UUID.randomUUID().toString(),
    val brand: String,
    val model: String,
    val year: Int,
    val mileage: Int,
    val registration: String,
    /** Calendar date of the last confirmed odometer reading; null for legacy cars. */
    val mileageUpdatedDateMillis: Long? = null,
    /** Fuel/energy sources used by this vehicle (e.g. PETROL + LPG, or PETROL + ELECTRIC for PHEV). */
    val fuelSources: Set<FuelSource> = emptySet()
)

/** Mileage-based predictions become uncertain when the reading is older than 30 days or undated. */
fun Car.hasStaleMileage(todayMillis: Long): Boolean =
    mileageUpdatedDateMillis?.let { todayMillis - it >= 30L * 86400000 } ?: true
