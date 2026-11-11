package com.example.carapp.ui.service

import com.example.carapp.domain.model.ServiceEntry
import com.example.carapp.domain.model.ServiceType

enum class ServiceField { TITLE, MILEAGE, COST, NOTE }

data class ServiceFormState(
    val visible: Boolean = false,
    val editingEntryId: String? = null,
    val type: ServiceType = ServiceType.MAINTENANCE,
    val title: String = "",
    val dateMillis: Long? = null,
    val mileage: String = "",
    val cost: String = "",
    val note: String = "",
    val submitted: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null
) {
    val isEditing: Boolean get() = editingEntryId != null
}

data class VehicleServiceState(
    val entries: List<ServiceEntry> = emptyList(),
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val form: ServiceFormState = ServiceFormState(),
    val suggestedMileage: Int? = null,
    val deletingEntry: ServiceEntry? = null,
    val deleting: Boolean = false,
    val deleteError: String? = null
)

fun ServiceType.displayName(): String = when (this) {
    ServiceType.REPAIR -> "Naprawa"
    ServiceType.MAINTENANCE -> "Wymiana / serwis"
    ServiceType.INSPECTION -> "Przegląd"
    ServiceType.OTHER -> "Inne"
}
