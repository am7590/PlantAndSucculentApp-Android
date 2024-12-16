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

    // Add a state to hold the current photo being processed
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
                // Handle error if needed
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
                
                // Store the current photo URL before processing
                _currentHealthCheckPhoto = latestPhoto.url
                
                // Perform health check
                val result = repository.performHealthCheck(
                    userId = "user123", 
                    identifier = plant.identifier,
                    latestPhotoUrl = latestPhoto.url
                )
                _lastHealthCheckResult.value = result
                _shouldNavigateToHealthCheck = true
                
                // Update plant's health check status in database
                updatePlantHealthStatus(plant.identifier.sku, result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform health check", e)
                _lastHealthCheckResult.value = """{"error": "Failed to perform health check: ${e.message}"}"""
            }
        }
    }

    private suspend fun updatePlantHealthStatus(sku: String, healthCheckResult: String) {
        try {
            // Get current plant
            val currentPlant = (plantsState.value as? UiState.Success)?.data
                ?.find { it.identifier.sku == sku }
                ?: return

            // Parse health data
            val healthData = Gson().fromJson(healthCheckResult, JsonObject::class.java)
            val probability = healthData
                .getAsJsonObject("health_assessment")
                ?.get("is_healthy_probability")
                ?.asDouble
                ?: return

            // Update plant information with new health check data
            val updatedInformation = currentPlant.information.toBuilder()
                .setLastHealthCheck(System.currentTimeMillis())
                .setLastHealthResult(healthCheckResult)
                .build()

            // Update plant in database
            updatePlant(
                userId = "user123",
                identifier = currentPlant.identifier,
                information = updatedInformation
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update plant health status", e)
        }
    }

    // Add method to get current health check photo
    fun getCurrentHealthCheckPhoto(): String? = _currentHealthCheckPhoto

    // Clear the current photo after health check is complete
    fun clearCurrentHealthCheckPhoto() {
        _currentHealthCheckPhoto = null
    }

    private fun shouldAllowHealthCheck(plant: PlantOuterClass.Plant): Boolean {
        val lastHealthCheck = if (true) {//if (plant.information.hasLastHealthCheck()) {
            plant.information.lastHealthCheck
        } else {
            0L
        }
        
        // Allow health check if:
        // 1. Never checked before (lastHealthCheck = 0)
        // 2. Latest photo is newer than last health check
        val latestPhotoTimestamp = plant.information.photosList
            .maxOfOrNull { it.timestamp } ?: 0L
            
        return lastHealthCheck == 0L || latestPhotoTimestamp > lastHealthCheck
    }

    fun identifyPlant(plant: PlantOuterClass.Plant) {
        viewModelScope.launch {
            try {
                val latestPhoto = plant.information.photosList.maxByOrNull { it.timestamp }
                if (latestPhoto != null) {
                    _identificationResult.value = UiState.Loading
                    val result = repository.identifyPlant(latestPhoto.url)
                    _identificationResult.value = UiState.Success(result)
                }
            } catch (e: Exception) {
                Log.e("PlantsViewModel", "Failed to identify plant", e)
                _identificationResult.value = UiState.Error(e.message ?: "Failed to identify plant")
            }
        }
        // Reset state to trigger recomposition
        _identificationResult.value = UiState.Loading
    }

    fun retryIdentification() {
        // Reset state to trigger recomposition
        _identificationResult.value = UiState.Loading
    }

    fun addPhotoToPlant(sku: String, uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                // Generate a unique filename
                val fileName = "plant_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, fileName)
                
                // Copy the image data
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Create internal URI
                val internalUri = "file://${file.absolutePath}"
                
                // Create photo entry
                val newPhoto = PlantOuterClass.PhotoEntry.newBuilder()
                    .setUrl(internalUri)
                    .setTimestamp(System.currentTimeMillis())
                    .setNote("Added photo")
                    .build()

                // Add photo using repository
                val success = repository.addPhotoToPlant("user123", sku, newPhoto)
                
                if (success) {
                    // Only refresh if the update was successful
                    fetchPlantList()
                } else {
                    Log.e(TAG, "Failed to add photo to plant")
                    // Handle error - maybe show a toast or error state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add photo", e)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Clear database
                database.clearAllTables()
                
                // Clear identification cache and files
                (repository as? PlantsRepositoryImpl)?.let { repo ->
                    repo.healthService.clearCache()
                }
                
                // Reset states
                _identificationResult.value = UiState.Loading
                _lastHealthCheckResult.value = null
                
                // Refresh data
                fetchPlantList()
                
                Log.d("PlantsViewModel", "Successfully cleared all data")
            } catch (e: Exception) {
                Log.e("PlantsViewModel", "Failed to clear data", e)
            }
        }
    }

    fun startPlantIdentification(photoUrl: String, plantSku: String) {
        if (isIdentificationInProgress) {
            Log.d("PlantsViewModel", "Plant identification already in progress, skipping request")
            return
        }
        
        viewModelScope.launch {
            try {
                isIdentificationInProgress = true
                currentIdentificationSku = plantSku
                _identificationResults.value = emptyList() // Clear previous results
                
                val result = repository.identifyPlant(photoUrl)
                _identificationResults.value = result.suggestions
            } catch (e: Exception) {
                Log.e("PlantsViewModel", "Failed to identify plant", e)
                _identificationResults.value = emptyList()
            } finally {
                isIdentificationInProgress = false
            }
        }
    }

    fun selectIdentifiedPlant(suggestion: PlantSuggestion) {
        viewModelScope.launch {
            try {
                val currentPlant = (plantsState.value as? UiState.Success)?.data
                    ?.find { it.identifier.sku == currentIdentificationSku } ?: return@launch

                val updatedInformation = currentPlant.information.toBuilder()
                    .setIdentifiedSpeciesName(suggestion.plantName)
                    .setLastIdentification(System.currentTimeMillis())
                    .build()

                updatePlant("user123", currentPlant.identifier, updatedInformation)
                
                // Clear results and SKU after successful selection
                _identificationResults.value = emptyList()
                currentIdentificationSku = null
            } catch (e: Exception) {
                Log.e("PlantsViewModel", "Failed to update plant with identification", e)
            }
        }
    }

    fun clearNavigateToHealthCheck() {
        _shouldNavigateToHealthCheck = false
    }
}
