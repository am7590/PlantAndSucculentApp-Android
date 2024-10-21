package com.example.plantandsucculentapp.PlantsFeature.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlantsScreen(
    plantsViewModel: PlantsViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) {
        println("we got ${plantsViewModel.getData()}")
    }
}