package com.example.plantandsucculentapp.plants.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantandsucculentapp.core.presentation.util.UiState
import com.example.plantandsucculentapp.plants.data.PlantsRepositoryImpl
import com.example.plantandsucculentapp.plants.data.local.PlantDatabase
import com.example.plantandsucculentapp.plants.data.model.PlantIdentificationResponse
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import plant.PlantOuterClass

class PlantsViewModel(
    private val repository: Repository,
    private val database: PlantDatabase
) : ViewModel() {

    private val _plantsState = MutableStateFlow<UiState<List<PlantOuterClass.Plant>>>(UiState.Loading)
    val plantsState = _plantsState.asStateFlow()

    private val _lastHealthCheckResult = MutableStateFlow<String?>(null)
    val lastHealthCheckResult: StateFlow<String?> = _lastHealthCheckResult.asStateFlow()

    private val _identificationResult = MutableStateFlow<UiState<PlantIdentificationResponse>>(UiState.Loading)
    val identificationResult = _identificationResult.asStateFlow()

    var selectedSpecies: String? = null
        internal set

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
                if (latestPhoto != null && shouldAllowHealthCheck(plant)) {
                    val result = repository.performHealthCheck(
                        "user123", 
                        plant.identifier,
                        latestPhoto.url
                    )
                    _lastHealthCheckResult.value = result
                    fetchPlantList() // This will trigger the LaunchedEffect in TrendsScreen
                }
            } catch (e: Exception) {
                _lastHealthCheckResult.value = """{"error": "Failed to perform health check: ${e.message}"}"""
            }
        }
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

    fun identifyPlant(plant: PlantOuterClass.Plant, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val latestPhoto = plant.information.photosList.maxByOrNull { it.timestamp }
                if (latestPhoto != null) {
                    _identificationResult.value = UiState.Loading
                    val result = if (forceRefresh) {
                        repository.identifyPlant(latestPhoto.url, skipCache = true)
                    } else {
                        repository.identifyPlant(latestPhoto.url)
                    }
                    Log.d("PlantsViewModel", "Got identification result: $result")

                    // First check if we have valid suggestions
                    val validSuggestions = result.suggestions.filter { 
                        !it.plantName.isNullOrBlank() && it.plantDetails != null 
                    }

                    when {
                        // If we have valid suggestions, use them regardless of isPlant flag
                        validSuggestions.isNotEmpty() -> {
                            _identificationResult.value = UiState.Success(result.copy(suggestions = validSuggestions))
                            val topSuggestion = validSuggestions[0]
                            Log.d("PlantsViewModel", "Updating plant with top suggestion: ${topSuggestion.plantName}")
                            val updatedInformation = plant.information.toBuilder()
                                .setIdentifiedSpeciesName(topSuggestion.plantName)
                                .setLastIdentification(System.currentTimeMillis())
                                .build()
                            updatePlant("user123", plant.identifier, updatedInformation)
                        }
                        // If API says it's not a plant with high confidence
                        !result.isPlant && result.isPlantProbability < 0.3 -> {
                            _identificationResult.value = UiState.Error("Image does not appear to be a plant (${(result.isPlantProbability * 100).toInt()}% confidence)")
                        }
                        // If we have suggestions but they're not valid
                        result.suggestions.isNotEmpty() -> {
                            _identificationResult.value = UiState.Error("Could not determine plant species. Please try with a clearer photo.")
                        }
                        // No suggestions at all
                        else -> {
                            _identificationResult.value = UiState.Error("No plant species identified. Please try again with a different photo.")
                        }
                    }
                } else {
                    _identificationResult.value = UiState.Error("No photos available for identification")
                }
            } catch (e: Exception) {
                Log.e("PlantsViewModel", "Failed to identify plant", e)
                _identificationResult.value = UiState.Error(e.message ?: "Failed to identify plant")
            }
        }
    }

    fun retryIdentification() {
        // Reset state to trigger recomposition
        _identificationResult.value = UiState.Loading
    }

    fun addPhotoToPlant(sku: String, photoUrl: String) {
        viewModelScope.launch {
            try {
                val currentPlant = (plantsState.value as? UiState.Success)?.data
                    ?.find { it.identifier.sku == sku } ?: return@launch

                val newPhoto = PlantOuterClass.PhotoEntry.newBuilder()
                    .setUrl(photoUrl)
                    .setTimestamp(System.currentTimeMillis())
                    .setNote("Added photo")
                    .build()

                val updatedInformation = currentPlant.information.toBuilder()
                    .addPhotos(newPhoto)
                    .build()

                updatePlant("user123", currentPlant.identifier, updatedInformation)
            } catch (e: Exception) {
                Log.e("PlantsViewModel", "Failed to add photo", e)
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
}
