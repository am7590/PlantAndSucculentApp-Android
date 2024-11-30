package com.example.plantandsucculentapp.plants.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import plant.PlantOuterClass

class PlantsViewModel(
    private val repository: Repository
) : ViewModel() {

    private val _plants = MutableStateFlow<List<PlantOuterClass.Plant>>(emptyList())
    val plants: StateFlow<List<PlantOuterClass.Plant>> = _plants

    init {
        fetchPlantList()
    }

    fun fetchPlantList() {
        viewModelScope.launch {
            val mockPlants = repository.getWateredPlants()
            _plants.value = mockPlants
        }
    }

    fun addPlant(userId: String, plant: PlantOuterClass.Plant) {
        viewModelScope.launch {
            repository.addPlant(userId, plant)
            fetchPlantList()
        }
    }

    fun updatePlant(identifier: PlantOuterClass.PlantIdentifier, information: PlantOuterClass.PlantInformation) {
        viewModelScope.launch {
            repository.updatePlant("user123", identifier, information)
            fetchPlantList()
        }
    }
}
