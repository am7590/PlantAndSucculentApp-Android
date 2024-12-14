package com.example.plantandsucculentapp.plants.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantandsucculentapp.core.presentation.util.UiState
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import plant.PlantOuterClass

class PlantsViewModel(
    private val repository: Repository
) : ViewModel() {

    private val _plantsState = MutableStateFlow<UiState<List<PlantOuterClass.Plant>>>(UiState.Loading)
    val plantsState = _plantsState.asStateFlow()

    private val _lastHealthCheckResult = MutableStateFlow<String?>(null)
    val lastHealthCheckResult: StateFlow<String?> = _lastHealthCheckResult.asStateFlow()

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
                // Handle error if needed
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
                // Only allow health check if there's a photo and it hasn't been checked recently
                val latestPhoto = plant.information.photosList.maxByOrNull { it.timestamp }
                if (latestPhoto != null && shouldAllowHealthCheck(plant)) {
                    val result = repository.performHealthCheck(
                        "user123", 
                        plant.identifier,
                        latestPhoto.url
                    )
                    _lastHealthCheckResult.value = result
                    fetchPlantList() // Refresh to show updated health check timestamp
                }
            } catch (e: Exception) {
                // Handle error
                _lastHealthCheckResult.value = """{"error": "Failed to perform health check: ${e.message}"}"""
            }
        }
    }

    private fun shouldAllowHealthCheck(plant: PlantOuterClass.Plant): Boolean {
        val lastHealthCheck = if (plant.information.hasLastHealthCheck()) {
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
}
