package com.example.plantandsucculentapp.plants.data

import com.example.plantandsucculentapp.plants.domain.Repository
import plant.PlantOuterClass
import com.example.plantandsucculentapp.core.network.MockGrpcClient

class PlantsRepositoryImpl(
    private val mockGrpcClient: MockGrpcClient
) : Repository {

    override suspend fun fetchData(): String {
        // no-op for now
        // in a non-mock setting, this would connect us to the gRPC server
        return ""
    }

    override suspend fun getWateredPlants(): List<PlantOuterClass.Plant> {
        return mockGrpcClient.getWatered("user123").plantsList
    }

    override suspend fun addPlant(userId: String, plant: PlantOuterClass.Plant): String {
        val response = mockGrpcClient.addPlant(userId, plant)
        return response.status
    }

    override suspend fun updatePlant(userId: String, identifier: PlantOuterClass.PlantIdentifier, information: PlantOuterClass.PlantInformation): String {
        val response = mockGrpcClient.updatePlant(userId, identifier, information)
        return response.status
    }
}
