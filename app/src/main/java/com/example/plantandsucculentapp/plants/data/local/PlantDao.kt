package com.example.plantandsucculentapp.plants.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Transaction
    @Query("SELECT * FROM plants")
    fun getAllPlants(): Flow<List<PlantWithPhotos>>

    @Transaction
    @Query("SELECT * FROM plants WHERE sku = :sku")
    suspend fun getPlantBySku(sku: String): PlantWithPhotos?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: PlantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Query("SELECT * FROM plant_photos WHERE plantSku = :sku ORDER BY timestamp ASC")
    suspend fun getPhotosForPlant(sku: String): List<PhotoEntity>
}

data class PlantWithPhotos(
    @Embedded val plant: PlantEntity,
    @Relation(
        parentColumn = "sku",
        entityColumn = "plantSku"
    )
    val photos: List<PhotoEntity> = emptyList()
) 