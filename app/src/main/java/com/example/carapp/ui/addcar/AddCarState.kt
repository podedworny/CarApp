package com.example.carapp.ui.addcar

import com.example.carapp.domain.validation.validateCar

enum class CarField { BRAND, MODEL, YEAR, MILEAGE, REGISTRATION }

data class AddCarState(
    val brand: String = "",
    val model: String = "",
    val year: String = "",
    val mileage: String = "",
    val registration: String = "",
    val currentYear: Int,
    val submitted: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
    /** Non-null when editing an existing car. */
    val editingCarId: String? = null
) {
    val isEditing: Boolean get() = editingCarId != null
    val maximumYear: Int get() = currentYear + 1
    val errors get() = validateCar(brand, model, year, mileage, currentYear)
}
