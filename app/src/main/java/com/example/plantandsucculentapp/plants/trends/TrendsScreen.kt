package com.example.plantandsucculentapp.plants.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plantandsucculentapp.core.presentation.components.ErrorScreen
import com.example.plantandsucculentapp.core.presentation.components.LoadingScreen
import com.example.plantandsucculentapp.core.presentation.util.UiState
import com.example.plantandsucculentapp.plants.presentation.PlantsContent
import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel
import com.example.plantandsucculentapp.plants.trends.components.EmptyTrendsScreen
import plant.PlantOuterClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(viewModel: PlantsViewModel) {
    val plantsState by viewModel.plantsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trends",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { paddingValues ->
        when (plantsState) {
            is UiState.Loading -> {
                LoadingScreen()
            }
            is UiState.Success -> {
                val plants = (plantsState as UiState.Success).data
                if (plants.isEmpty()) {
                    EmptyTrendsScreen()
                } else {
                    // TODO: Show actual trends data when implemented
                    EmptyTrendsScreen()  // For now, always show empty state
                }
            }
            is UiState.Error -> {
                ErrorScreen(
                    message = (plantsState as UiState.Error).message,
                    onRetry = { viewModel.fetchPlantList() }
                )
            }
        }
    }
}

@Composable
fun TrendsContent(plants: List<PlantOuterClass.Plant>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Health Trends",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            plants.forEach { plant ->
                PlantTrendCard(
                    plantName = plant.information.name,
                    species = plant.information.identifiedSpeciesName,
                    healthPercentage = calculateHealthPercentage(plant)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

fun calculateHealthPercentage(plant: PlantOuterClass.Plant): Int {
    val daysSinceLastWatered = System.currentTimeMillis() / (1000 * 60 * 60 * 24) - plant.information.lastWatered / (1000 * 60 * 60 * 24)
    return if (daysSinceLastWatered < 7) {
        100 - (daysSinceLastWatered * 10).toInt().coerceAtLeast(0)
    } else {
        100 - (daysSinceLastWatered * 5).toInt().coerceAtMost(0)
    }
}

@Composable
fun PlantTrendCard(
    plantName: String,
    species: String,
    healthPercentage: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(50.dp)
            ) {
                drawCircle(
                    color = Color.Gray,
                    style = Stroke(width = 6.dp.toPx())
                )
                val sweepAngle = (healthPercentage / 100f) * 360
                drawArc(
                    color = if (healthPercentage > 50) Color.Green else Color.Red,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx())
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = plantName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = species,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${healthPercentage}%",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                color = if (healthPercentage > 50) Color.Green else Color.Red
            )
        }
    }
}
