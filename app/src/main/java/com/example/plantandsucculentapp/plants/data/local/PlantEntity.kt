package com.example.plantandsucculentapp.plants.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import plant.PlantOuterClass
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey
    val sku: String,
    val deviceIdentifier: String,
    val name: String,
    val lastWatered: Long,
    val lastHealthCheck: Long = 0,
    val lastIdentification: Long?,
    val identifiedSpeciesName: String?,
    val photos: List<PhotoEntity>,
    val lastHealthResult: String? = null,
    val healthCheckHistory: List<HealthCheckEntity> = emptyList()
)

data class PhotoEntity(
    val url: String,
    val timestamp: Long,
    val note: String?
)

data class HealthCheckEntity(
    val timestamp: Long,
    val result: String,
    val probability: Double
)

fun PlantEntity.toPlant(): PlantOuterClass.Plant {
    return PlantOuterClass.Plant.newBuilder()
        .setIdentifier(
            PlantOuterClass.PlantIdentifier.newBuilder()
                .setSku(sku)
                .setDeviceIdentifier(deviceIdentifier)
        )
        .setInformation(
            PlantOuterClass.PlantInformation.newBuilder()
                .setName(name)
                .setLastWatered(lastWatered)
                .setLastHealthCheck(lastHealthCheck)
                .apply {
                    lastIdentification?.let { setLastIdentification(it) }
                    identifiedSpeciesName?.let { setIdentifiedSpeciesName(it) }
                    if (!lastHealthResult.isNullOrEmpty()) {
                        setLastHealthResult(lastHealthResult)
                    }
                    photos.forEach { photo ->
                        addPhotos(
                            PlantOuterClass.PhotoEntry.newBuilder()
                                .setUrl(photo.url)
                                .setTimestamp(photo.timestamp)
                                .apply { photo.note?.let { setNote(it) } }
                                .build()
                        )
                    }
                    setHistoricalHealthChecks(
                        PlantOuterClass.HistoricalProbabilities.newBuilder()
                            .addAllProbabilities(
                                healthCheckHistory.map { healthCheck ->
                                    PlantOuterClass.Probability.newBuilder()
                                        .setId("health_check_${healthCheck.timestamp}")
                                        .setName("health_check")
                                        .setProbability(healthCheck.probability)
                                        .setDate(healthCheck.timestamp)
                                        .build()
                                }
                            )
                            .build()
                    )
                }
        )
        .build()
}

fun PlantOuterClass.Plant.toEntity(): PlantEntity {
    return PlantEntity(
        sku = identifier.sku,
        deviceIdentifier = identifier.deviceIdentifier,
        name = information.name,
        lastWatered = information.lastWatered,
        lastHealthCheck = information.lastHealthCheck,
        lastIdentification = information.lastIdentification,
        identifiedSpeciesName = information.identifiedSpeciesName,
        photos = information.photosList.map { photo ->
            PhotoEntity(
                url = photo.url,
                timestamp = photo.timestamp,
                note = if (photo.hasNote()) photo.note else null
            )
        },
        lastHealthResult = information.lastHealthResult,
        healthCheckHistory = if (information.hasHistoricalHealthChecks()) {
            information.historicalHealthChecks.probabilitiesList.map { prob ->
                HealthCheckEntity(
                    timestamp = prob.date,
                    result = "",
                    probability = prob.probability
                )
            }
        } else {
            emptyList()
        }
    )
} 