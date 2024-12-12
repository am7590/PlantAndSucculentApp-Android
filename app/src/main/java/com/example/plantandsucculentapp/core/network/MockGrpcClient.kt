package com.example.plantandsucculentapp.core.network

import plant.PlantOuterClass
import plant.PlantServiceGrpc

class MockGrpcClient : GrpcClientInterface {
    private val plantsList = mutableListOf<PlantOuterClass.Plant>()

    init {
        // mock dummy data
        // this can be used for unit tests if I decide they are necessary
        plantsList.addAll(
            listOf(
                PlantOuterClass.Plant.newBuilder()
                    .setIdentifier(
                        PlantOuterClass.PlantIdentifier.newBuilder().setSku("plant123")
                    )
                    .setInformation(
                        PlantOuterClass.PlantInformation.newBuilder()
                            .setName("Thirsty Plant 1")
                            .setLastWatered(System.currentTimeMillis() - 86400000)
                            .build()
                    )
                    .build(),
                PlantOuterClass.Plant.newBuilder()
                    .setIdentifier(
                        PlantOuterClass.PlantIdentifier.newBuilder().setSku("plant456")
                    )
                    .setInformation(
                        PlantOuterClass.PlantInformation.newBuilder()
                            .setName("Thirsty Plant 2")
                            .setLastWatered(System.currentTimeMillis() - 172800000)
                            .build()
                    )
                    .build()
            )
        )
    }

    override fun registerOrGetUser(uuid: String): Result<PlantOuterClass.UserResponse> {
        return Result.success(PlantOuterClass.UserResponse.getDefaultInstance())
    }

    override fun getWatered(userId: String): Result<PlantOuterClass.ListOfPlants> {
        return Result.success(PlantOuterClass.ListOfPlants.newBuilder()
            .addAllPlants(plantsList)
            .build())
    }

    override fun addPlant(userId: String, plant: PlantOuterClass.Plant): Result<PlantOuterClass.PlantResponse> {
        plantsList.add(plant)
        return Result.success(PlantOuterClass.PlantResponse.newBuilder()
            .setStatus("SUCCESS")
            .build())
    }

    override fun updatePlant(
        userId: String,
        identifier: PlantOuterClass.PlantIdentifier,
        information: PlantOuterClass.PlantInformation
    ): Result<PlantOuterClass.PlantUpdateResponse> {
        val index = plantsList.indexOfFirst { it.identifier.sku == identifier.sku }
        if (index != -1) {
            plantsList[index] = plantsList[index].toBuilder().setInformation(information).build()
        }
        return Result.success(PlantOuterClass.PlantUpdateResponse.newBuilder()
            .setStatus("SUCCESS")
            .build())
    }

    override fun shutdown() {
        // No-op for mock
    }

    override fun testConnection(): Boolean = true
}
