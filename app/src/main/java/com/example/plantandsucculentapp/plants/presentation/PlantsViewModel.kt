package com.example.plantandsucculentapp.plants.presentation

//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.plantandsucculentapp.plants.domain.Repository
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import plant.PlantOuterClass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.KoinApplication.Companion.init
import plant.PlantOuterClass

class PlantsViewModel(
private val repository: Repository
) : ViewModel() {

    // Holds the fetched data string
    private val _data = MutableStateFlow("")
    val data: StateFlow<String> = _data

    // Holds the list of plants
    private val _plants = MutableStateFlow<List<PlantOuterClass.Plant>>(emptyList())
    val plants: StateFlow<List<PlantOuterClass.Plant>> = _plants

    init {
        fetchPlantData()
        fetchPlantList()
    }

    private fun fetchPlantData() {
        viewModelScope.launch {
            val result = repository.fetchData()
            _data.value = result
        }
    }

    private fun fetchPlantList() {
        viewModelScope.launch {
            // Fetch plants from the repository or MockGrpcClient
            val mockPlants = repository.getWateredPlants()
            _plants.value = mockPlants
        }
    }
}