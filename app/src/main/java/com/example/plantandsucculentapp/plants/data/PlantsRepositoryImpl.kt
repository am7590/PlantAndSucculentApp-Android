package com.example.plantandsucculentapp.plants.data

//import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import android.content.ContentValues.TAG
import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import com.example.plantandsucculentapp.core.network.MockGrpcClient
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
import com.example.plantandsucculentapp.plants.data.PlantHealthService

class PlantsRepositoryImpl(
    private val grpcClient: GrpcClientInterface,
    private val plantDao: PlantDao,
    private val isMockEnabled: Boolean,
    private val sharedPreferences: SharedPreferences,
    private val healthService: PlantHealthService
) : Repository {
    private val healthCheckCache = mutableMapOf<String, HealthCheckResponse>()
    private val cacheKeyPrefix = "healthcheck_cache_"

    override suspend fun fetchData(): String {
        return grpcClient.testConnection().toString()
    }

    override suspend fun getWateredPlants(): List<PlantOuterClass.Plant> = withContext(Dispatchers.IO) {
        if (isMockEnabled) {
            // Use mock data directly
            val result = grpcClient.getWatered("user123")
            result.getOrThrow().plantsList
        } else {
            // For real implementation, just get plants from local cache
            plantDao.getAllPlants().first().map { it.toPlant() }
        }
    }

    override suspend fun addPlant(userId: String, plant: PlantOuterClass.Plant): String = 
        withContext(Dispatchers.IO) {
            try {
                val response = grpcClient.addPlant(userId, plant)
                if (!isMockEnabled) {
                    // Cache the plant locally after successful server add
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
                // Update the plant in local cache after successful server update
                val updatedPlant = PlantOuterClass.Plant.newBuilder()
                    .setIdentifier(identifier)
                    .setInformation(information)
                    .build()
                plantDao.insertPlant(updatedPlant.toEntity())
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

    private suspend fun updatePlantHealthStatus(sku: String, healthResult: String) {
        try {
            val plant = plantDao.getPlantBySku(sku)
            Log.d(TAG, "Current plant data before update: $plant")
            
            plant?.let {
                val updatedPlant = it.copy(
                    lastHealthCheck = System.currentTimeMillis(),
                    lastHealthResult = healthResult
                )
                Log.d(TAG, "Updating plant with health result: $updatedPlant")
                
                // Store in Room
                plantDao.insertPlant(updatedPlant)
                
                // Verify the update
                val verifyPlant = plantDao.getPlantBySku(sku)
                Log.d(TAG, """
                    Verified plant after update: 
                    SKU: ${verifyPlant?.sku}
                    LastHealthCheck: ${verifyPlant?.lastHealthCheck}
                    LastHealthResult: ${verifyPlant?.lastHealthResult}
                    LastHealthResult length: ${verifyPlant?.lastHealthResult?.length}
                """.trimIndent())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update plant health status", e)
        }
    }
}
