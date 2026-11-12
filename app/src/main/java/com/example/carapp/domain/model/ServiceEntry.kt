package com.example.carapp.domain.model

import java.util.UUID

enum class ServiceType { REPAIR, MAINTENANCE, INSPECTION, OTHER }

data class ServiceEntry(
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val type: ServiceType,
    val title: String,
    /** Calendar date encoded at midnight UTC, matching the Material date picker. */
    val dateMillis: Long,
    val mileage: Int,
    /** Integer grosze prevent floating-point rounding in totals. Null means unknown cost. */
    val costMinor: Long?,
    val note: String = ""
)
