package com.example.plantandsucculentapp.PlantsFeature.domain

interface Repository {
    suspend fun fetchData(): String
}