package com.example.plantandsucculentapp.plants.trends

import android.annotation.SuppressLint
import android.os.Build
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
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.plantandsucculentapp.plants.data.local.PlantDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrendsScreen(viewModel: PlantsViewModel) {
    val repository = koinInject<Repository>()
    val plantsState by viewModel.plantsState.collectAsState()
    val scope = rememberCoroutineScope()
    val database: PlantDatabase by inject(PlantDatabase::class.java)

    Box(
        modifier = Modifier
            .fillMaxSize()
//            .combinedClickable(
//                onClick = { },
//                onLongClick = {
//                    scope.launch(Dispatchers.IO) {
//                        database.clearAllTables()
//                        viewModel.fetchPlantList()
//                    }
//                }
//            )
    ) {
        when (plantsState) {
            is UiState.Success -> {
                val plants = (plantsState as UiState.Success).data
                if (plants.isEmpty()) {
                    EmptyTrendsScreen()
                } else {
                    TrendsContent(plants, repository)
                }
            }
            is UiState.Loading -> {
                LoadingScreen()
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TrendsContent(plants: List<PlantOuterClass.Plant>, repository: Repository) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = 90.dp,
                bottom = 0.dp,
                start = 16.dp,
                end = 16.dp
            )
    ) {
        // Overall Health Summary
        item {
            OverallHealthCard(plants, repository)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Individual Plant Health Cards
        item {
            Text(
                "Plant Health",
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PlantHealthCard(
    plant: PlantOuterClass.Plant,
    repository: Repository
) {
    var currentHealth by remember { mutableStateOf(0.0) }
    var healthHistory by remember { mutableStateOf<List<PlantOuterClass.Probability>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Refresh when plant data changes or after health check
    LaunchedEffect(plant.identifier.sku, plant.information.lastHealthCheck) {
        try {
            val history = repository.getHealthHistory(plant.identifier)
            Log.d("TrendsScreen", """
                Health history for plant ${plant.identifier.sku}:
                - Probability: ${history.probability}
                - History size: ${history.historicalProbabilities.probabilitiesList.size}
                - Raw history: ${history.historicalProbabilities.probabilitiesList}
                - Last health check: ${plant.information.lastHealthCheck}
            """.trimIndent())
            
            healthHistory = history.historicalProbabilities.probabilitiesList
            currentHealth = history.probability * 100
        } catch (e: Exception) {
            Log.e("TrendsScreen", "Failed to get health history", e)
            errorMessage = e.message
            currentHealth = calculateFallbackHealth(plant)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plant image and basic info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = plant.information.photosList.lastOrNull()?.url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = plant.information.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Health: ${currentHealth.roundToInt()}%",
                            color = when {
                                currentHealth >= 70 -> Color.Green
                                currentHealth >= 40 -> Color.Yellow
                                else -> Color.Red
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Last checked: ${
                                Date(plant.information.lastHealthCheck)
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Health History Graph
            if (healthHistory.size > 1) {
                Log.d("TrendsScreen", """
                    Showing graph section:
                    - Plant: ${plant.identifier.sku}
                    - History size: ${healthHistory.size}
                    - History: ${healthHistory.map { "${it.date}: ${it.probability}" }}
                """.trimIndent())
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Health History",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HealthHistoryGraph(
                    history = healthHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            } else {
                Log.d("TrendsScreen", """
                    Not showing graph:
                    - Plant: ${plant.identifier.sku}
                    - History size: ${healthHistory.size}
                """.trimIndent())
            }

            // Show error if any
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HealthHistoryGraph(
    history: List<PlantOuterClass.Probability>,
    modifier: Modifier = Modifier
) {
    val sortedHistory = remember(history) {
        history.sortedBy { it.date }
    }
    
    // Calculate time range and normalize points
    val points = remember(sortedHistory) {
        val firstTimestamp = sortedHistory.first().date
        val lastTimestamp = sortedHistory.last().date
        val timeRange = lastTimestamp - firstTimestamp
        
        // Use percentage of total width for x-coordinates (0 to 100)
        sortedHistory.map { probability ->
            val xPosition = if (timeRange > 0) {
                ((probability.date - firstTimestamp).toFloat() / timeRange.toFloat()) * 100f
            } else {
                50f // Center single point
            }
            Pair(xPosition, (probability.probability * 100).toFloat())
        }.also { points ->
            Log.d("TrendsScreen", "Calculated normalized points (0-100 scale): $points")
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 16.dp.toPx()
        
        // Fixed scale: x from 0-100, y from 0-100
        val xMin = 0f
        val xMax = 100f
        val yMin = 0f
        val yMax = 100f

        // Calculate scale factors
        val xScale = (width - 2 * padding) / (xMax - xMin)
        val yScale = (height - 2 * padding) / (yMax - yMin)

        Log.d("TrendsScreen", """
            Graph dimensions:
            - Width: $width, Height: $height
            - X range: $xMin to $xMax percent (scale: $xScale)
            - Y range: $yMin to $yMax (scale: $yScale)
        """.trimIndent())
        
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 1.dp.toPx()
        )
        
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 1.dp.toPx()
        )

        // Draw line graph
        if (points.size > 1) {
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = padding + (point.first - xMin) * xScale
                val y = height - padding - (point.second - yMin) * yScale
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                
                // Draw points
                drawCircle(
                    color = Color.Blue,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Draw line connecting points
            drawPath(
                path = path,
                color = Color.Blue,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.cornerPathEffect(8.dp.toPx())
                )
            )
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