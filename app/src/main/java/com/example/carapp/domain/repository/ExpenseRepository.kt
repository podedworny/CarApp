package com.example.carapp.domain.repository

import com.example.carapp.domain.model.ExpenseEntry

interface ExpenseRepository {
    suspend fun getEntries(carId: String): List<ExpenseEntry>
    suspend fun addEntry(entry: ExpenseEntry)
    suspend fun deleteEntry(carId: String, entryId: String)
}
