package com.example.plantandsucculentapp.core.data

import com.example.plantandsucculentapp.plants.domain.Repository
import plant.PlantOuterClass

class CoreRepositoryImpl(get: Any) : Repository {
    override suspend fun fetchData(): String {
        TODO("Not yet implemented")
    }

    override suspend fun getWateredPlants(): List<PlantOuterClass.Plant> {
        TODO("Not yet implemented")
    }

    override suspend fun addPlant(userId: String, plant: PlantOuterClass.Plant): String {
        TODO("Not yet implemented")
    }

    override suspend fun updatePlant(
        userId: String,
        identifier: PlantOuterClass.PlantIdentifier,
        information: PlantOuterClass.PlantInformation
    ): String {
        TODO("Not yet implemented")
    }

}