package com.example.plantandsucculentapp.plants.data

//import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import android.content.ContentValues.TAG
import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import com.example.plantandsucculentapp.core.util.NetworkException
import com.example.plantandsucculentapp.plants.data.local.PlantDao
import com.example.plantandsucculentapp.plants.data.local.toEntity
import com.example.plantandsucculentapp.plants.data.local.toPlant
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import plant.PlantOuterClass
import java.net.UnknownHostException
import android.content.SharedPreferences
import com.example.plantandsucculentapp.plants.data.model.HealthCheckResponse
import android.util.Log
import com.example.plantandsucculentapp.plants.data.local.HealthCheckEntity
import com.example.plantandsucculentapp.plants.data.model.PlantIdentificationResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.example.plantandsucculentapp.plants.data.local.PhotoEntity

class PlantsRepositoryImpl(
    private val grpcClient: GrpcClientInterface,
    private val plantDao: PlantDao,
    private val isMockEnabled: Boolean,
    private val sharedPreferences: SharedPreferences,
    val healthService: PlantHealthService,
    private val gson: Gson,
) : Repository {
    private val healthCheckCache = mutableMapOf<String, HealthCheckResponse>()
    private val cacheKeyPrefix = "healthcheck_cache_"

    override suspend fun fetchData(): String {
        return grpcClient.testConnection().toString()
    }

    override suspend fun getWateredPlants(): List<PlantOuterClass.Plant> = withContext(Dispatchers.IO) {
        if (isMockEnabled) {
            val result = grpcClient.getWatered("user123")
            result.getOrThrow().plantsList
        } else {
            val plants = plantDao.getAllPlants().first()
            Log.d(TAG, "Retrieved plants from Room: ${plants.map { 
                "${it.plant.sku}: photos size=${it.photos.size}" 
            }}")
            plants.map { it.toPlant() }
        }
    }

    override suspend fun addPlant(userId: String, plant: PlantOuterClass.Plant): String = 
        withContext(Dispatchers.IO) {
            try {
                val response = grpcClient.addPlant(userId, plant)
                if (!isMockEnabled) {
                    // Store base plant info
                    plantDao.insertPlant(plant.toEntity())
                    
                    // Store photos separately
                    plant.information.photosList.forEach { photo ->
                        plantDao.insertPhoto(PhotoEntity(
                            plantSku = plant.identifier.sku,
                            url = photo.url,
                            timestamp = photo.timestamp,
                            note = photo.note
                        ))
                    }
                }
                response.getOrThrow().status
            } catch (e: UnknownHostException) {
                throw NetworkException.NoConnection
            } catch (e: Exception) {
                throw NetworkException.ServerError
            }
        }

    override suspend fun updatePlant(
        userId: String, 
        identifier: PlantOuterClass.PlantIdentifier, 
        information: PlantOuterClass.PlantInformation
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = grpcClient.updatePlant(userId, identifier, information)
            if (!isMockEnabled) {
                // Get existing plant to preserve health check history
                val existingPlant = plantDao.getPlantBySku(identifier.sku)
                
                // Create new plant entity with preserved history
                val updatedPlant = PlantOuterClass.Plant.newBuilder()
                    .setIdentifier(identifier)
                    .setInformation(information)
                    .build()
                    .toEntity()
                    .copy(
                        healthCheckHistory = existingPlant?.plant?.healthCheckHistory ?: emptyList()
                    )
                
                // Store in Room
                plantDao.insertPlant(updatedPlant)
            }
            response.getOrThrow().status
        } catch (e: UnknownHostException) {
            throw NetworkException.NoConnection
        } catch (e: Exception) {
            throw NetworkException.ServerError
        }
    }

    override suspend fun performHealthCheck(
        userId: String,
        identifier: PlantOuterClass.PlantIdentifier,
        latestPhotoUrl: String
    ): String = withContext(Dispatchers.IO) {
        val cacheKey = "$cacheKeyPrefix${identifier.sku}_${latestPhotoUrl.hashCode()}"
        
        try {
            // Check cache first before making API call
            sharedPreferences.getString(cacheKey, null)?.let { cached ->
                Log.d(TAG, "Returning cached health check result: $cached")
                updatePlantHealthStatus(identifier.sku, cached)
                return@withContext cached
            }

            Log.d(TAG, "No cache found, performing health check API call")
            val result = healthService.checkPlantHealth(latestPhotoUrl)
            Log.d(TAG, "Got health check result: $result")
            
            // Cache the result
            sharedPreferences.edit()
                .putString(cacheKey, result)
                .apply()
            Log.d(TAG, "Cached new health check result")
            
            // Update plant information
            updatePlantHealthStatus(identifier.sku, result)
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            throw NetworkException.ServerError
        }
    }

    override suspend fun getHealthHistory(identifier: PlantOuterClass.PlantIdentifier): PlantOuterClass.HealthCheckInformation {
        return try {
            val plantWithPhotos = plantDao.getPlantBySku(identifier.sku)
            Log.d(TAG, """
                Getting health history for ${identifier.sku}:
                - Plant found: ${plantWithPhotos != null}
                - History size: ${plantWithPhotos?.plant?.healthCheckHistory?.size}
                - Raw history: ${plantWithPhotos?.plant?.healthCheckHistory}
            """.trimIndent())

            if (plantWithPhotos?.plant?.healthCheckHistory?.isNotEmpty() == true) {
                val latestHealthCheck = plantWithPhotos.plant.healthCheckHistory.maxByOrNull { it.timestamp }!!
                PlantOuterClass.HealthCheckInformation.newBuilder()
                    .setProbability(latestHealthCheck.probability)
                    .setHistoricalProbabilities(
                        PlantOuterClass.HistoricalProbabilities.newBuilder()
                            .addAllProbabilities(
                                plantWithPhotos.plant.healthCheckHistory.map { healthCheck ->
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
                    .build()
            } else {
                PlantOuterClass.HealthCheckInformation.getDefaultInstance()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get health history", e)
            PlantOuterClass.HealthCheckInformation.getDefaultInstance()
        }
    }

    private suspend fun updatePlantHealthStatus(sku: String, healthResult: String) {
        try {
            // Get existing plant
            val plantWithPhotos = plantDao.getPlantBySku(sku)
            
            // Parse the health result
            val healthData = gson.fromJson(healthResult, JsonObject::class.java)
            val probability = healthData
                .getAsJsonObject("health_assessment")
                ?.get("is_healthy_probability")
                ?.asDouble ?: 0.0

            // Create new health check entry
            val newHealthCheck = HealthCheckEntity(
                timestamp = System.currentTimeMillis(),
                result = healthResult,
                probability = probability
            )

            // Update plant with new health check
            plantWithPhotos?.let { plant ->
                val updatedPlant = plant.plant.copy(
                    lastHealthCheck = System.currentTimeMillis(),
                    lastHealthResult = healthResult,
                    healthCheckHistory = plant.plant.healthCheckHistory + newHealthCheck
                )
                plantDao.insertPlant(updatedPlant)
                
                Log.d(TAG, """
                    Updated plant health status:
                    - SKU: $sku
                    - New probability: $probability
                    - History size: ${updatedPlant.healthCheckHistory.size}
                    - Latest history entry: ${updatedPlant.healthCheckHistory.lastOrNull()}
                """.trimIndent())
            }

            // Create health check data for server
            val healthCheckData = PlantOuterClass.HealthCheckInformation.newBuilder()
                .setProbability(probability)
                .setHistoricalProbabilities(
                    PlantOuterClass.HistoricalProbabilities.newBuilder()
                        .addProbabilities(
                            PlantOuterClass.Probability.newBuilder()
                                .setId("health_check_${System.currentTimeMillis()}")
                                .setName("health_check")
                                .setProbability(probability)
                                .setDate(System.currentTimeMillis())
                                .build()
                        )
                        .build()
                )
                .build()

            // Save to server
            val response = grpcClient.saveHealthCheckData(
                PlantOuterClass.HealthCheckDataRequest.newBuilder()
                    .setIdentifier(PlantOuterClass.PlantIdentifier.newBuilder().setSku(sku).build())
                    .setHealthCheckInformation(Gson().toJson(healthCheckData))
                    .build()
            )

            Log.d(TAG, "Saved health check to server: ${response.getOrThrow().status}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update plant health status", e)
            e.printStackTrace()
        }
    }

    override suspend fun identifyPlant(photoUrl: String, skipCache: Boolean): PlantIdentificationResponse = withContext(Dispatchers.IO) {
        try {
            val jsonResponse = if (skipCache) {
                val uniqueUrl = "$photoUrl?t=${System.currentTimeMillis()}"
                healthService.identifyPlant(uniqueUrl)
            } else {
                healthService.identifyPlant(photoUrl)
            }
            
            Log.d(TAG, "Parsing JSON response: $jsonResponse")
            val response = gson.fromJson(jsonResponse, PlantIdentificationResponse::class.java)
            Log.d(TAG, "Parsed response: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Failed to identify plant", e)
            throw e
        }
    }

    override suspend fun addPhotoToPlant(userId: String, sku: String, photo: PlantOuterClass.PhotoEntry): Boolean {
        return try {
            // Add photo to local database first
            plantDao.insertPhoto(PhotoEntity(
                plantSku = sku,
                url = photo.url,
                timestamp = photo.timestamp,
                note = photo.note
            ))
            
            Log.d(TAG, "Added photo to local database: ${photo.url}")
            
            try {
                // Try to sync with server (can fail without affecting local state)
                val plantWithPhotos = plantDao.getPlantBySku(sku) ?: return false
                val updatedPlant = plantWithPhotos.toPlant().toBuilder()
                    .setInformation(
                        plantWithPhotos.plant.toPlant().information.toBuilder().apply {
                            clearPhotos() // Clear existing photos
                            plantDao.getPhotosForPlant(sku).forEach { photoEntity ->
                                addPhotos(PlantOuterClass.PhotoEntry.newBuilder()
                                    .setUrl(photoEntity.url)
                                    .setTimestamp(photoEntity.timestamp)
                                    .setNote(photoEntity.note)
                                    .build())
                            }
                        }.build()
                    )
                    .build()
                
                grpcClient.updatePlant(userId, updatedPlant.identifier, updatedPlant.information)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync with server, but photo was saved locally", e)
            }

            true // Return true since local save succeeded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add photo to plant", e)
            false
        }
    }
}
