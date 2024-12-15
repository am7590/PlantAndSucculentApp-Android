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

class PlantsRepositoryImpl(
    private val grpcClient: GrpcClientInterface,
    private val plantDao: PlantDao,
    private val isMockEnabled: Boolean,
    private val sharedPreferences: SharedPreferences,
    private val healthService: PlantHealthService,
    private val gson: Gson
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
                "${it.sku}: history size=${it.healthCheckHistory.size}" 
            }}")
            plants.map { it.toPlant() }
        }
    }

    override suspend fun addPlant(userId: String, plant: PlantOuterClass.Plant): String = 
        withContext(Dispatchers.IO) {
            try {
                val response = grpcClient.addPlant(userId, plant)
                if (!isMockEnabled) {
                    // For new plants, we start with empty history
                    plantDao.insertPlant(plant.toEntity())
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
                        healthCheckHistory = existingPlant?.healthCheckHistory ?: emptyList()
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
            val plant = plantDao.getPlantBySku(identifier.sku)
            Log.d(TAG, """
                Getting health history for ${identifier.sku}:
                - Plant found: ${plant != null}
                - History size: ${plant?.healthCheckHistory?.size}
                - Raw history: ${plant?.healthCheckHistory}
            """.trimIndent())

            if (plant?.healthCheckHistory?.isNotEmpty() == true) {
                val latestHealthCheck = plant.healthCheckHistory.maxByOrNull { it.timestamp }!!
                PlantOuterClass.HealthCheckInformation.newBuilder()
                    .setProbability(latestHealthCheck.probability)
                    .setHistoricalProbabilities(
                        PlantOuterClass.HistoricalProbabilities.newBuilder()
                            .addAllProbabilities(
                                plant.healthCheckHistory.map { healthCheck ->
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
            val existingPlant = plantDao.getPlantBySku(sku)
            
            // Parse the health result
            val healthData = Gson().fromJson(healthResult, JsonObject::class.java)
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
            existingPlant?.let { plant ->
                val updatedPlant = plant.copy(
                    lastHealthCheck = System.currentTimeMillis(),
                    lastHealthResult = healthResult,
                    healthCheckHistory = plant.healthCheckHistory + newHealthCheck
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
                // Pass a unique cache key to force a new request
                val uniqueUrl = "$photoUrl?t=${System.currentTimeMillis()}"
                healthService.identifyPlant(uniqueUrl)
            } else {
                healthService.identifyPlant(photoUrl)
            }
            gson.fromJson(jsonResponse, PlantIdentificationResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to identify plant", e)
            throw e
        }
    }
}
