package com.example.plantandsucculentapp.plants.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import plant.PlantOuterClass
import com.example.plantandsucculentapp.core.presentation.components.ErrorScreen
import com.example.plantandsucculentapp.core.presentation.components.LoadingScreen
import com.example.plantandsucculentapp.core.presentation.util.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlantsScreen(
    viewModel: PlantsViewModel,
    onAddPlantClick: () -> Unit,
    onPlantClick: (PlantOuterClass.Plant) -> Unit
) {
    val plantsState by viewModel.plantsState.collectAsState()

    when (plantsState) {
        is UiState.Loading -> {
            LoadingScreen()
        }
        is UiState.Success -> {
            val plants = (plantsState as UiState.Success).data
            PlantsContent(
                plants = plants,
                onAddPlantClick = {
                    onAddPlantClick()
                }) {
                onPlantClick(it)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantsContent(plants: List<PlantOuterClass.Plant>, onAddPlantClick: () -> Unit, onPlantClick: (PlantOuterClass.Plant) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Plants",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddPlantClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Plant")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(plants) { plant ->
                PlantListItem(
                    name = plant.information.name,
                    lastWatered = plant.information.lastWatered,
                    photoUrl = plant.information.photoUrl,
                    onItemClick = { onPlantClick(plant) }
                )
            }
        }
    }
}

@Composable
fun PlantListItem(
    name: String,
    lastWatered: Long,
    photoUrl: String,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onItemClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plant Image
            if (photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Plant photo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last watered: ${formatDate(lastWatered)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Update the call site in PlantsContent
@Composable
private fun PlantsContent(
    plants: List<PlantOuterClass.Plant>,
    onPlantClick: (PlantOuterClass.Plant) -> Unit
) {
    LazyColumn {
        items(plants) { plant ->
            PlantListItem(
                name = plant.information.name,
                lastWatered = plant.information.lastWatered,
                photoUrl = plant.information.photoUrl,
                onItemClick = { onPlantClick(plant) }
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}