package com.example.carapp.domain.repository

import com.example.carapp.domain.model.ServiceEntry

interface ServiceRepository {
    suspend fun getEntries(carId: String): List<ServiceEntry>
    suspend fun addEntry(entry: ServiceEntry)
}
