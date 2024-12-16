package com.example.plantandsucculentapp.plants.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantandsucculentapp.core.presentation.util.UiState
import com.example.plantandsucculentapp.plants.data.PlantsRepositoryImpl
import com.example.plantandsucculentapp.plants.data.local.PlantDatabase
import com.example.plantandsucculentapp.plants.data.model.PlantIdentificationResponse
import com.example.plantandsucculentapp.plants.data.model.PlantSuggestion
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import plant.PlantOuterClass
import java.io.File
import java.util.*
import com.google.gson.Gson
import com.google.gson.JsonObject

class PlantsViewModel(
    private val repository: Repository,
    private val database: PlantDatabase
) : ViewModel() {

    companion object {
        private const val TAG = "PlantsViewModel"
    }

    private val _plantsState = MutableStateFlow<UiState<List<PlantOuterClass.Plant>>>(UiState.Loading)
    val plantsState = _plantsState.asStateFlow()

    private val _lastHealthCheckResult = MutableStateFlow<String?>(null)
    val lastHealthCheckResult: StateFlow<String?> = _lastHealthCheckResult.asStateFlow()

    private val _identificationResult = MutableStateFlow<UiState<PlantIdentificationResponse>>(UiState.Loading)
    val identificationResult = _identificationResult.asStateFlow()

    private val _identificationResults = mutableStateOf<List<PlantSuggestion>>(emptyList())
    val identificationResults: List<PlantSuggestion> get() = _identificationResults.value

    var selectedSpecies: String? = null
        internal set

    private var isIdentificationInProgress = false
    private var currentIdentificationSku: String? = null

    private var _shouldNavigateToHealthCheck = false
    val shouldNavigateToHealthCheck get() = _shouldNavigateToHealthCheck

    private var _currentHealthCheckPhoto: String? = null

    init {
        fetchPlantList()
    }

    fun fetchPlantList() {
        viewModelScope.launch {
            _plantsState.value = UiState.Loading
            try {
                val plants = repository.getWateredPlants()
                _plantsState.value = UiState.Success(plants)
            } catch (e: Exception) {
                _plantsState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addPlant(userId: String, plant: PlantOuterClass.Plant) {
        viewModelScope.launch {
            try {
                repository.addPlant(userId, plant)
                fetchPlantList()
            } catch (e: Exception) {
                _plantsState.value = UiState.Error(e.message ?: "Failed to add plant")
            }
        }
    }

    fun updatePlant(userId: String, identifier: PlantOuterClass.PlantIdentifier, information: PlantOuterClass.PlantInformation) {
        viewModelScope.launch {
            try {
                repository.updatePlant(userId, identifier, information)
                fetchPlantList()
            } catch (e: Exception) {
                // TODO
            }
        }
    }

    fun performHealthCheck(plant: PlantOuterClass.Plant) {
        viewModelScope.launch {
            try {
                val latestPhoto = plant.information.photosList.maxByOrNull { it.timestamp }
                if (latestPhoto == null) {
                    _lastHealthCheckResult.value = """{"error": "No photo available"}"""
                    return@launch
                }
                
                _currentHealthCheckPhoto = latestPhoto.url
                
                val result = repository.performHealthCheck(
                    userId = "user123", 
                    identifier = plant.identifier,
                    latestPhotoUrl = latestPhoto.url
                )
                _lastHealthCheckResult.value = result
                _shouldNavigateToHealthCheck = true
                
                updatePlantHealthStatus(plant.identifier.sku, result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform health check", e)
                _lastHealthCheckResult.value = """{"error": "Failed to perform health check: ${e.message}"}"""
            }
        }
    }

    private suspend fun updatePlantHealthStatus(sku: String, healthCheckResult: String) {
        try {
            val currentPlant = (plantsState.value as? UiState.Success)?.data
                ?.find { it.identifier.sku == sku }
                ?: return

            val healthData = Gson().fromJson(healthCheckResult, JsonObject::class.java)
            val probability = healthData
                .getAsJsonObject("health_assessment")
                ?.get("is_healthy_probability")
                ?.asDouble
                ?: return

            val updatedInformation = currentPlant.information.toBuilder()
                .setLastHealthCheck(System.currentTimeMillis())
                .setLastHealthResult(healthCheckResult)
                .build()

            updatePlant(
                userId = "user123",
                identifier = currentPlant.identifier,
                information = updatedInformation
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update plant health status", e)
        }
    }

    fun getCurrentHealthCheckPhoto(): String? = _currentHealthCheckPhoto

    fun clearCurrentHealthCheckPhoto() {
        _currentHealthCheckPhoto = null
    }

    fun identifyPlant(plant: PlantOuterClass.Plant) {
        viewModelScope.launch {
            try {
                val latestPhoto = plant.information.photosList.maxByOrNull { it.timestamp }
                if (latestPhoto != null) {
                    startPlantIdentification(latestPhoto.url, plant.identifier.sku)
                }
            } catch (e: Exception) {
                Log.e("PlantsViewModel", "Failed to identify plant", e)
            }
        }
    }

    fun addPhotoToPlant(sku: String, uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val fileName = "plant_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, fileName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val internalUri = "file://${file.absolutePath}"
                
                val newPhoto = PlantOuterClass.PhotoEntry.newBuilder()
                    .setUrl(internalUri)
                    .setTimestamp(System.currentTimeMillis())
                    .setNote("Added photo")
                    .build()

                try {
                    repository.addPhotoToPlant("user123", sku, newPhoto)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync photo with server, continuing with local update", e)
                }

                fetchPlantList()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add photo", e)
            }
        }
    }

    fun startPlantIdentification(photoUrl: String, plantSku: String) {
        if (isIdentificationInProgress) return
        
        viewModelScope.launch {
            try {
                isIdentificationInProgress = true
                _identificationResult.value = UiState.Loading
                
                val result = repository.identifyPlant(photoUrl)
                _identificationResult.value = UiState.Success(result)
            } catch (e: Exception) {
                _identificationResult.value = UiState.Error(e.message ?: "Failed to identify plant")
            } finally {
                isIdentificationInProgress = false
            }
        }
    }

    fun clearNavigateToHealthCheck() {
        _shouldNavigateToHealthCheck = false
    }
}
