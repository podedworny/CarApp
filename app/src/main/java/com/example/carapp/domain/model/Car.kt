package com.example.carapp.domain.model

import java.util.UUID

data class Car(
    val id: String = UUID.randomUUID().toString(),
    val brand: String,
    val model: String,
    val year: Int,
    val mileage: Int,
    val registration: String
)
