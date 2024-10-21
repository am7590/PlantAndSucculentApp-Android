package com.example.plantandsucculentapp.PlantsFeature.presentation

import androidx.lifecycle.ViewModel
import com.example.plantandsucculentapp.PlantsFeature.domain.Repository

class PlantsViewModel(
    private val repository: Repository
) : ViewModel() {

    suspend fun getData(): String {
        return repository.fetchData()
    }
}


