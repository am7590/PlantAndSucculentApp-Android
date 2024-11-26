package com.example.plantandsucculentapp.plants.domain

import plant.PlantOuterClass

interface Repository {
    suspend fun fetchData(): String
    suspend fun getWateredPlants(): List<PlantOuterClass.Plant>
}