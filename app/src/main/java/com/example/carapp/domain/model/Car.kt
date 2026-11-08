package com.example.carapp.domain.model

import java.util.UUID

data class Car(
    val id: String = UUID.randomUUID().toString(),
    val brand: String,
    val model: String,
    val year: Int,
    val mileage: Int,
    val registration: String,
    /** Calendar date of the last confirmed odometer reading; null for legacy cars. */
    val mileageUpdatedDateMillis: Long? = null
)

/** Mileage-based predictions become uncertain when the reading is older than 30 days or undated. */
fun Car.hasStaleMileage(todayMillis: Long): Boolean =
    mileageUpdatedDateMillis?.let { todayMillis - it >= 30L * 86400000 } ?: true
