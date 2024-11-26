package com.example.plantandsucculentapp.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: Repository
): ViewModel() {

    init {
        viewModelScope.launch {
            print("got mainViewModel data source: ${repository.fetchData()}")
        }
    }
}