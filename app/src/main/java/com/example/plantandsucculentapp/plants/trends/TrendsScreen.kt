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
import com.example.plantandsucculentapp.plants.data.local.toEntity
import com.example.plantandsucculentapp.plants.trends.components.EmptyTrendsScreen

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(viewModel: PlantsViewModel) {
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
                    TrendsContent(plants)
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 90.dp, horizontal = 16.dp)
    ) {
        // Overall Health Summary
        item {
            OverallHealthCard(plants)
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
            PlantHealthCard(plant)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OverallHealthCard(plants: List<PlantOuterClass.Plant>) {
    // Consider a plant "health checked" if it has a timestamp after 2023
    val minValidTimestamp = 1672531200000 // Jan 1, 2023 in milliseconds
    
    val healthyPlants = plants.count { 
        it.information.lastHealthCheck > minValidTimestamp
    }
    
    // Add detailed logging for each plant's health calculation
    plants.forEach { plant ->
        Log.d("TrendsScreen", """
            Plant: ${plant.identifier.sku}
            LastHealthCheck: ${plant.information.lastHealthCheck}
            LastHealthResult: ${plant.information.lastHealthResult}
            Raw calculation result: ${calculateHealthPercentage(plant)}
        """.trimIndent())
    }
    
    val averageHealth = plants
        .filter { 
            it.information.lastHealthCheck > minValidTimestamp
        }
        .map { calculateHealthPercentage(it) }
        .also { scores ->
            Log.d("TrendsScreen", "All health scores before averaging: $scores")
        }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?: 0.0

    Log.d("TrendsScreen", "Final average health: $averageHealth")

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
                        "$healthyPlants",
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
private fun PlantHealthCard(plant: PlantOuterClass.Plant) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    "Last checked: ${formatDate(plant.information.lastHealthCheck)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val healthScore = calculateHealthPercentage(plant)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp)
            ) {
                CircularProgressIndicator(
                    progress = (healthScore / 100f).toFloat(),
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 4.dp,
                    color = if (healthScore >= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                Text(
                    text = "${healthScore.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (healthScore >= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun calculateHealthPercentage(plant: PlantOuterClass.Plant): Double {
    val entity = plant.toEntity()
    Log.d("TrendsScreen", """
        Calculating health for plant: ${plant.identifier.sku}
        Entity data:
        - LastHealthCheck: ${entity.lastHealthCheck}
        - LastHealthResult length: ${entity.lastHealthResult?.length}
        - LastHealthResult: ${entity.lastHealthResult}
        Proto data:
        - LastHealthCheck: ${plant.information.lastHealthCheck}
        - LastHealthResult: ${plant.information.lastHealthResult}
    """.trimIndent())

    val healthResult = entity.lastHealthResult
    if (!healthResult.isNullOrEmpty()) {
        try {
            val healthData = Gson().fromJson(healthResult, JsonObject::class.java)
            Log.d("TrendsScreen", "Parsed health data: $healthData")
            
            val healthAssessment = healthData.getAsJsonObject("health_assessment")
            if (healthAssessment != null) {
                val probability = healthAssessment.get("is_healthy_probability")?.asDouble
                Log.d("TrendsScreen", "Health probability: $probability")
                
                if (probability != null) {
                    return probability * 100
                }
            }
        } catch (e: Exception) {
            Log.e("TrendsScreen", "Failed to parse health data", e)
            Log.e("TrendsScreen", "Raw health result: $healthResult")
        }
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