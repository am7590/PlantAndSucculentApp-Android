package com.example.plantandsucculentapp.core.network

import plant.PlantOuterClass
import plant.PlantServiceGrpc
class MockGrpcClient {

    fun fetch(): String {
        return "Fetched mock data"
    }

    fun registerOrGetUser(uuid: String): PlantOuterClass.UserResponse {
        return PlantOuterClass.UserResponse.newBuilder()
            .setStatus("existing")  // can toggle existing or new
            .build()
    }

    fun addPlant(userId: String, plant: PlantOuterClass.Plant): PlantOuterClass.PlantResponse {
        return PlantOuterClass.PlantResponse.newBuilder()
            .setStatus("added")
            .build()
    }

    fun removePlant(uuid: String, sku: String): PlantOuterClass.PlantResponse {
        return PlantOuterClass.PlantResponse.newBuilder()
            .setStatus("removed")
            .build()
    }

    fun getPlant(uuid: String, sku: String): PlantOuterClass.Plant {
        return PlantOuterClass.Plant.newBuilder()
            .setIdentifier(
                PlantOuterClass.PlantIdentifier.newBuilder()
                    .setSku(sku)
                    .setDeviceIdentifier("device123")
                    .build()
            )
            .setInformation(
                PlantOuterClass.PlantInformation.newBuilder()
                    .setName("Mock Plant")
                    .setLastWatered(System.currentTimeMillis() - 86400000)
                    .setLastHealthCheck(System.currentTimeMillis() - 172800000)
                    .setLastIdentification(System.currentTimeMillis() - 259200000)
                    .setIdentifiedSpeciesName("Mock Species")
                    .build()
            )
            .build()
    }

    fun getWatered(uuid: String): PlantOuterClass.ListOfPlants {
        val plant1 = PlantOuterClass.Plant.newBuilder()
            .setIdentifier(PlantOuterClass.PlantIdentifier.newBuilder().setSku("plant123"))
            .setInformation(PlantOuterClass.PlantInformation.newBuilder().setName("Thirsty Plant 1"))
            .build()

        val plant2 = PlantOuterClass.Plant.newBuilder()
            .setIdentifier(PlantOuterClass.PlantIdentifier.newBuilder().setSku("plant456"))
            .setInformation(PlantOuterClass.PlantInformation.newBuilder().setName("Thirsty Plant 2"))
            .build()

        return PlantOuterClass.ListOfPlants.newBuilder()
            .addPlants(plant1)
            .addPlants(plant2)
            .build()
    }

    fun updatePlant(uuid: String, identifier: PlantOuterClass.PlantIdentifier, information: PlantOuterClass.PlantInformation): PlantOuterClass.PlantUpdateResponse {
        return PlantOuterClass.PlantUpdateResponse.newBuilder()
            .setStatus("updated")
            .build()
    }

    fun saveHealthCheckData(identifier: PlantOuterClass.PlantIdentifier, healthCheckInformation: String): PlantOuterClass.HealthCheckDataResponse {
        return PlantOuterClass.HealthCheckDataResponse.newBuilder()
            .setStatus("saved")
            .build()
    }

    fun identificationRequest(uuid: String, sku: String): PlantOuterClass.PlantInformation {
        return PlantOuterClass.PlantInformation.newBuilder()
            .setName("Identified Plant")
            .setLastIdentification(System.currentTimeMillis() - 3600000)
            .setIdentifiedSpeciesName("Mock Species Name")
            .build()
    }

    fun healthCheckRequest(uuid: String, sku: String): PlantOuterClass.HealthCheckInformation {
        val probability = PlantOuterClass.Probabilities.newBuilder()
            .setId("123")
            .setName("Mock Health Status")
            .setProbability(0.85)
            .setDate(System.currentTimeMillis() - 86400000)
            .build()

        val historicalProbabilities = PlantOuterClass.HistoricalProbabilities.newBuilder()
            .addProbabilities(probability)
            .build()

        return PlantOuterClass.HealthCheckInformation.newBuilder()
            .setProbability(0.90)
            .setHistoricalProbabilities(historicalProbabilities)
            .build()
    }
}