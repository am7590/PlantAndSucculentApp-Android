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
}
