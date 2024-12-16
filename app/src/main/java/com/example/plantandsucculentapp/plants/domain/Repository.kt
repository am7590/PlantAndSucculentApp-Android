package com.example.plantandsucculentapp.plants.domain

import com.example.plantandsucculentapp.plants.data.model.PlantIdentificationResponse
import plant.PlantOuterClass

interface Repository {
    suspend fun fetchData(): String
    suspend fun getWateredPlants(): List<PlantOuterClass.Plant>
    suspend fun addPlant(userId: String, plant: PlantOuterClass.Plant): String
    suspend fun updatePlant(
        userId: String,
        identifier: PlantOuterClass.PlantIdentifier,
        information: PlantOuterClass.PlantInformation
    ): String
    suspend fun performHealthCheck(
        userId: String,
        identifier: PlantOuterClass.PlantIdentifier,
        latestPhotoUrl: String
    ): String
    suspend fun getHealthHistory(identifier: PlantOuterClass.PlantIdentifier): PlantOuterClass.HealthCheckInformation
    suspend fun identifyPlant(photoUrl: String, skipCache: Boolean = false): PlantIdentificationResponse
    suspend fun addPhotoToPlant(userId: String, sku: String, photo: PlantOuterClass.PhotoEntry): Boolean
}
