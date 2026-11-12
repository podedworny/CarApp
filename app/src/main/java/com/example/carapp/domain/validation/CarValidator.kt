package com.example.carapp.domain.validation

/** Validation stays independent of Android and the presentation layer. */
data class CarValidationErrors(
    val brand: Boolean,
    val model: Boolean,
    val year: Boolean,
    val mileage: Boolean
) {
    val isValid: Boolean get() = !brand && !model && !year && !mileage
}

fun validateCar(brand: String, model: String, year: String, mileage: String, currentYear: Int) =
    CarValidationErrors(
        brand = brand.isBlank() || brand.length > 50,
        model = model.isBlank() || model.length > 70,
        year = year.toIntOrNull()?.let { it in 1886..currentYear + 1 } != true,
        mileage = mileage.toIntOrNull()?.let { it >= 0 } != true
    )
