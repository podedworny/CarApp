package com.example.carapp.ui.components

import com.example.carapp.domain.model.Car
import com.example.carapp.ui.service.formatServiceDate

fun Car.mileageDateLabel(): String = mileageUpdatedDateMillis?.let {
    "Licznik potwierdzony: ${formatServiceDate(it)}"
} ?: "Data potwierdzenia licznika nieznana"
