package com.example.plantandsucculentapp.plants.data.local

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject

class PlantHealthHistoryManager(private val plantDao: PlantDao) {
    suspend fun addHealthCheckToHistory(entity: PlantEntity, healthResult: String, probability: Double) {
        val newHealthCheck = HealthCheckEntity(
            timestamp = entity.lastHealthCheck,
            result = healthResult,
            probability = probability
        )
        
        plantDao.insertPlant(entity.copy(
            healthCheckHistory = entity.healthCheckHistory + newHealthCheck
        ))
        
        Log.d("HealthHistoryManager", "Added health check to history. New size: ${entity.healthCheckHistory.size + 1}")
    }

    suspend fun parseAndAddHealthCheck(entity: PlantEntity) {
        val healthResult = entity.lastHealthResult
        if (!healthResult.isNullOrEmpty()) {
            try {
                val healthData = Gson().fromJson(healthResult, JsonObject::class.java)
                val healthAssessment = healthData.getAsJsonObject("health_assessment")
                val probability = healthAssessment?.get("is_healthy_probability")?.asDouble
                if (probability != null) {
                    addHealthCheckToHistory(entity, healthResult, probability)
                }
            } catch (e: Exception) {
                Log.e("HealthHistoryManager", "Failed to parse health data", e)
            }
        }
    }
} 