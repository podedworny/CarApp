package com.example.carapp.domain.model

import java.util.UUID

enum class ExpenseType { FUEL, INSURANCE, CAR_WASH, OTHER }

data class ExpenseEntry(
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val type: ExpenseType,
    val title: String,
    val dateMillis: Long,
    /** Integer grosze to stay consistent with service costs. */
    val costMinor: Long,
    val note: String = ""
)
