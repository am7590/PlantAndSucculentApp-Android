package com.example.plantandsucculentapp.plants.trends

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.plantandsucculentapp.core.presentation.components.ErrorScreen
import com.example.plantandsucculentapp.core.presentation.components.LoadingScreen
import com.example.plantandsucculentapp.core.presentation.util.UiState
import com.example.plantandsucculentapp.plants.presentation.PlantsContent
import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel
import plant.PlantOuterClass
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import com.example.plantandsucculentapp.plants.data.local.HealthCheckEntity
import com.example.plantandsucculentapp.plants.data.local.toEntity
import com.example.plantandsucculentapp.plants.trends.components.EmptyTrendsScreen
import com.example.plantandsucculentapp.plants.data.local.PlantHealthHistoryManager
import com.example.plantandsucculentapp.plants.domain.Repository
import org.koin.compose.koinInject

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(viewModel: PlantsViewModel) {
    val repository = koinInject<Repository>()
    val plantsState by viewModel.plantsState.collectAsState()
    val healthCheckResult by viewModel.lastHealthCheckResult.collectAsState()

    // Refresh when screen becomes visible or after health check
    LaunchedEffect(healthCheckResult) {
        viewModel.fetchPlantList()
    }

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
    ) {
        when (plantsState) {
            is UiState.Loading -> {
                LoadingScreen()
            }
            is UiState.Success -> {
                val plants = (plantsState as UiState.Success).data
                if (plants.isEmpty()) {
                    EmptyTrendsScreen()
                } else {
                    TrendsContent(plants, repository)
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
fun TrendsContent(plants: List<PlantOuterClass.Plant>, repository: Repository) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 90.dp, horizontal = 16.dp)
    ) {
        // Overall Health Summary
        item {
            OverallHealthCard(plants, repository)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Individual Plant Health Cards
        item {
            Text(
                "Plant Health History",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(plants) { plant ->
            PlantHealthCard(plant, repository)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OverallHealthCard(plants: List<PlantOuterClass.Plant>, repository: Repository) {
    val minValidTimestamp = 1672531200000 // Jan 1, 2023 in milliseconds
    var averageHealth by remember { mutableStateOf(0.0) }
    
    // Calculate average health when the card is first shown
    LaunchedEffect(plants) {
        val healthScores = plants
            .filter { it.information.lastHealthCheck > minValidTimestamp }
            .map { calculateHealthPercentage(it, repository) }
        
        averageHealth = if (healthScores.isNotEmpty()) {
            healthScores.average()
        } else {
            0.0
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Garden Overview",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total Plants",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "${plants.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        "Health Checked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "${plants.count { it.information.lastHealthCheck > minValidTimestamp }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        "Average Health",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "${averageHealth.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantHealthCard(
    plant: PlantOuterClass.Plant,
    repository: Repository
) {
    val entity = plant.toEntity()
    var currentHealth by remember { mutableStateOf(0.0) }

    // Calculate health when the card is first shown
    LaunchedEffect(plant.identifier.sku) {
        currentHealth = calculateHealthPercentage(plant, repository) / 100.0 // Convert from percentage to decimal
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plant Image
                AsyncImage(
                    model = plant.information.photosList.maxByOrNull { it.timestamp }?.url,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plant.information.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        "Health: ${(currentHealth * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            currentHealth >= 0.8 -> Color.Green
                            currentHealth >= 0.6 -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                    
                    Text(
                        "Last checked: ${formatDate(plant.information.lastHealthCheck)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (entity.healthCheckHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Health History",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entity.healthCheckHistory
                        .sortedBy { it.timestamp }
                        .takeLast(5)  // Show last 5 checks
                        .forEach { healthCheck ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = when {
                                            healthCheck.probability >= 0.8 -> Color.Green
                                            healthCheck.probability >= 0.6 -> Color.Yellow
                                            else -> Color.Red
                                        }.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                            )
                        }
                }
            }
        }
    }
}

private suspend fun calculateHealthPercentage(
    plant: PlantOuterClass.Plant,
    repository: Repository
): Double {
    try {
        // Get health history from server using repository
        val healthHistory = repository.getHealthHistory(plant.identifier)
            .historicalProbabilities
            .probabilitiesList
            .maxByOrNull { it.date }

        if (healthHistory != null) {
            return healthHistory.probability * 100
        }
    } catch (e: Exception) {
        Log.e("TrendsScreen", "Failed to get health history", e)
    }

    return calculateFallbackHealth(plant)
}

private fun calculateFallbackHealth(plant: PlantOuterClass.Plant): Double {
    Log.d("TrendsScreen", "Using fallback calculation for plant: ${plant.identifier.sku}")

    val daysSinceLastWatered = (System.currentTimeMillis() - plant.information.lastWatered) /
        (1000 * 60 * 60 * 24)
    
    val healthScore = if (daysSinceLastWatered < 7) {
        100.0 - (daysSinceLastWatered * 10.0).coerceAtLeast(0.0)
    } else {
        100.0 - (daysSinceLastWatered * 5.0).coerceAtMost(100.0)
    }

    Log.d("TrendsScreen", "Using fallback calculation: $healthScore")
    return healthScore
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd", Locale.getDefault())
    return format.format(date)
}

@Composable
fun EmptyTrendsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Trends Yet",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add some plants and track their health to see trends",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}