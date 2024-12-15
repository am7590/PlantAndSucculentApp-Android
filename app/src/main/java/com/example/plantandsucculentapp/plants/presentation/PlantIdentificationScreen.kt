package com.example.plantandsucculentapp.plants.presentation//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import com.example.plantandsucculentapp.core.presentation.components.ErrorScreen
//import com.example.plantandsucculentapp.core.presentation.util.UiState
//import com.example.plantandsucculentapp.plants.data.model.PlantSuggestion
//import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel
//import kotlin.math.roundToInt
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PlantIdentificationScreen(
//    viewModel: PlantsViewModel,
//    onSpeciesSelected: (String) -> Unit,
//    onBack: () -> Unit
//) {
//    val identificationResult by viewModel.identificationResult.collectAsState()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Identify Plant") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, "Back")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        when (val result = identificationResult) {
//            is UiState.Loading -> {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CircularProgressIndicator()
//                }
//            }
//            is UiState.Success -> {
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(padding)
//                ) {
//                    items(
//                        result.data.suggestions
//                    ) { suggestion ->
//                        PlantSuggestionCard(
//                            suggestion = suggestion,
//                            onClick = {
//                                onSpeciesSelected(suggestion.plantName)
//                            }
//                        )
//                    }
//                }
//            }
//            is UiState.Error -> {
//                ErrorScreen(
//                    message = result.message,
//                    onRetry = { viewModel.retryIdentification() }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun PlantSuggestionCard(
//    suggestion: PlantSuggestion,
//    onClick: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp)
//            .clickable(onClick = onClick),
//        elevation = CardDefaults.cardElevation(4.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text(
//                text = suggestion.plantName,
//                style = MaterialTheme.typography.titleMedium
//            )
//            Text(
//                text = "Probability: ${(suggestion.probability * 100).roundToInt()}%",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            if (suggestion.plantDetails.commonNames.isNotEmpty()) {
//                Text(
//                    text = "Also known as: ${suggestion.plantDetails.commonNames.joinToString(", ")}",
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//            suggestion.plantDetails.wikiDescription?.value?.let { description ->
//                Text(
//                    text = description,
//                    style = MaterialTheme.typography.bodySmall,
//                    maxLines = 3,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//        }
//    }
//}