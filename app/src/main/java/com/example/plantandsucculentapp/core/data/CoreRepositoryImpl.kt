package com.example.plantandsucculentapp.core.data

import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.plants.domain.Repository
import plant.PlantOuterClass

class CoreRepositoryImpl(
    private val mockGrpcClient: MockGrpcClient
) : Repository {
    override suspend fun fetchData(): String {
        return mockGrpcClient.fetch()
    }

    override suspend fun getWateredPlants(): List<PlantOuterClass.Plant> {
        return mockGrpcClient.getWatered("user123").plantsList
    }
}