package com.example.carapp.ui.garage

import com.example.carapp.domain.model.Car

enum class DeleteCarStep { REVIEW, FINAL_CONFIRMATION }

data class DeleteCarState(
    val car: Car,
    val step: DeleteCarStep = DeleteCarStep.REVIEW,
    val deleting: Boolean = false,
    val error: String? = null
)
