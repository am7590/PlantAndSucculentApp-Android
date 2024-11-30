package com.example.plantandsucculentapp.core.network

import plant.PlantOuterClass
import plant.PlantServiceGrpc

class MockGrpcClient {
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

    fun fetch(): String {
        // no-op
        return ""
    }

    fun addPlant(userId: String, plant: PlantOuterClass.Plant): PlantOuterClass.PlantResponse {
        plantsList.add(plant)
        return PlantOuterClass.PlantResponse.newBuilder()
            .setStatus("added")
            .build()
    }

    fun getWatered(uuid: String): PlantOuterClass.ListOfPlants {
        return PlantOuterClass.ListOfPlants.newBuilder()
            .addAllPlants(plantsList)
            .build()
    }

    fun getPlant(uuid: String, sku: String): PlantOuterClass.Plant? {
        return plantsList.find { it.identifier.sku == sku }
    }

    fun updatePlant(
        uuid: String,
        identifier: PlantOuterClass.PlantIdentifier,
        information: PlantOuterClass.PlantInformation
    ): PlantOuterClass.PlantUpdateResponse {
        val index = plantsList.indexOfFirst { it.identifier.sku == identifier.sku }
        if (index != -1) {
            plantsList[index] = plantsList[index].toBuilder().setInformation(information).build()
        }
        return PlantOuterClass.PlantUpdateResponse.newBuilder()
            .setStatus("updated")
            .build()
    }
}
