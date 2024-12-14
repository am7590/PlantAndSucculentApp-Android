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
    val lastHealthCheck: Long?,
    val lastIdentification: Long?,
    val identifiedSpeciesName: String?,
    val photos: List<PhotoEntity>
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
                .apply {
                    lastHealthCheck?.let { setLastHealthCheck(it) }
                    lastIdentification?.let { setLastIdentification(it) }
                    identifiedSpeciesName?.let { setIdentifiedSpeciesName(it) }
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
        lastHealthCheck = if (information.hasLastHealthCheck()) information.lastHealthCheck else null,
        lastIdentification = if (information.hasLastIdentification()) information.lastIdentification else null,
        identifiedSpeciesName = if (information.hasIdentifiedSpeciesName()) information.identifiedSpeciesName else null,
        photos = information.photosList.map { photo ->
            PhotoEntity(
                url = photo.url,
                timestamp = photo.timestamp,
                note = if (photo.hasNote()) photo.note else null
            )
        }
    )
} 