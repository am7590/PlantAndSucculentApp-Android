package com.example.plantandsucculentapp.plants.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import androidx.room.TypeConverters
import androidx.room.ForeignKey
import androidx.room.Index
import plant.PlantOuterClass
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey
    val sku: String,
    val deviceIdentifier: String,
    val name: String,
    val lastWatered: Long,
    val lastHealthCheck: Long = 0,
    val lastHealthResult: String = "",
    @TypeConverters(Converters::class)
    val healthCheckHistory: List<HealthCheckEntity> = emptyList()
) {
    @Ignore
    var photos: List<PhotoEntity> = emptyList()
}

@Entity(
    tableName = "plant_photos",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["sku"],
            childColumns = ["plantSku"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantSku")]
)
data class PhotoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val plantSku: String,
    val url: String,
    val timestamp: Long,
    val note: String
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
                    lastHealthResult?.let { setLastHealthResult(it) }
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

fun PlantWithPhotos.toPlant(): PlantOuterClass.Plant {
    return PlantOuterClass.Plant.newBuilder()
        .setIdentifier(
            PlantOuterClass.PlantIdentifier.newBuilder()
                .setSku(plant.sku)
                .setDeviceIdentifier(plant.deviceIdentifier)
        )
        .setInformation(
            PlantOuterClass.PlantInformation.newBuilder()
                .setName(plant.name)
                .setLastWatered(plant.lastWatered)
                .setLastHealthCheck(plant.lastHealthCheck)
                .apply {
                    photos.forEach { photo ->
                        addPhotos(
                            PlantOuterClass.PhotoEntry.newBuilder()
                                .setUrl(photo.url)
                                .setTimestamp(photo.timestamp)
                                .setNote(photo.note)
                                .build()
                        )
                    }
                }
        )
        .build()
} 