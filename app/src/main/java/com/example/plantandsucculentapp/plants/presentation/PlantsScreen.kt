package com.example.plantandsucculentapp.plants.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.plantandsucculentapp.R
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.koinViewModel
import plant.PlantOuterClass

@Composable
fun PlantsScreen(
    viewModel: PlantsViewModel = koinViewModel(),
    onAddPlant: () -> Unit,
    onPlantSelected: (String) -> Unit
) {
    val plants by viewModel.plants.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlant) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Plant")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(plants) { plant ->
                PlantListItem(plant = plant, onPlantClick = { onPlantSelected(plant.identifier.sku) })
            }
        }
    }
}

@Composable
fun PlantListItem(plant: PlantOuterClass.Plant, onPlantClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onPlantClick() }
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Mock image
            Image(
                painter = painterResource(id = R.drawable.ic_plant_placeholder),
                contentDescription = plant.information.name,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = plant.information.name, style = MaterialTheme.typography.titleLarge)
                Text(text = "Last Watered: ${plant.information.lastWatered}")
            }
        }
    }
}