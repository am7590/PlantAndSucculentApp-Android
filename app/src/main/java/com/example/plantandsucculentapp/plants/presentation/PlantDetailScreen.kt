package com.example.plantandsucculentapp.plants.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import plant.PlantOuterClass
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plant: PlantOuterClass.Plant,
    onWater: () -> Unit,
    onHealthCheck: () -> Unit,
    onAddPhoto: (String) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: PlantsViewModel
) {
    // Add effect to refresh plants list when screen is shown
    LaunchedEffect(Unit) {
        viewModel.fetchPlantList()
    }

    // Rest of your PlantDetailScreen implementation...
} 