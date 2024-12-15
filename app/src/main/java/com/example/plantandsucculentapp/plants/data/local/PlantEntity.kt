package com.example.plantandsucculentapp.plants.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import plant.PlantOuterClass

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
    val lastHealthResult: String? = null
)

data class PhotoEntity(
    val url: String,
    val timestamp: Long,
    val note: String?
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
        lastHealthResult = information.lastHealthResult
    )
} 