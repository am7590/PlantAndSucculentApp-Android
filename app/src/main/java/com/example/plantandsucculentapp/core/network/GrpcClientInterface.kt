package com.example.plantandsucculentapp.core.network

import plant.PlantOuterClass

interface GrpcClientInterface {
    fun registerOrGetUser(uuid: String): Result<PlantOuterClass.UserResponse>
    fun getWatered(userId: String): Result<PlantOuterClass.ListOfPlants>
    fun addPlant(userId: String, plant: PlantOuterClass.Plant): Result<PlantOuterClass.PlantResponse>
    fun updatePlant(
        userId: String,
        identifier: PlantOuterClass.PlantIdentifier,
        information: PlantOuterClass.PlantInformation
    ): Result<PlantOuterClass.PlantUpdateResponse>
    fun shutdown()
    fun testConnection(): Boolean
    fun performHealthCheck(
        identifier: PlantOuterClass.PlantIdentifier,
        healthCheckData: String
    ): Result<PlantOuterClass.HealthCheckDataResponse>
    suspend fun saveHealthCheckData(request: PlantOuterClass.HealthCheckDataRequest): Result<PlantOuterClass.HealthCheckDataResponse>
    suspend fun getHealthCheckHistory(identifier: PlantOuterClass.PlantIdentifier): Result<PlantOuterClass.HealthCheckInformation>
}