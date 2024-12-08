package com.example.plantandsucculentapp.plants.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantandsucculentapp.core.presentation.util.UiState
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import plant.PlantOuterClass

class PlantsViewModel(
    private val repository: Repository
) : ViewModel() {

    private val _plantsState = MutableStateFlow<UiState<List<PlantOuterClass.Plant>>>(UiState.Loading)
    val plantsState: StateFlow<UiState<List<PlantOuterClass.Plant>>> = _plantsState

    init {
        fetchPlantList()
    }

    fun fetchPlantList() {
        viewModelScope.launch {
            try {
                _plantsState.value = UiState.Loading
                val mockPlants = repository.getWateredPlants()
                _plantsState.value = UiState.Success(mockPlants)
            } catch (e: Exception) {
                _plantsState.value = UiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun addPlant(userId: String, plant: PlantOuterClass.Plant) {
        viewModelScope.launch {
            try {
                _plantsState.value = UiState.Loading
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
                _plantsState.value = UiState.Loading
                repository.updatePlant(userId, identifier, information)
                fetchPlantList()
            } catch (e: Exception) {
                _plantsState.value = UiState.Error(e.message ?: "Failed to update plant")
            }
        }
    }
}
