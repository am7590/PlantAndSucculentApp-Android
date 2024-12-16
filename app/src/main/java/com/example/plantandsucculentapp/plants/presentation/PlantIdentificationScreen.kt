package com.example.plantandsucculentapp.plants.presentation//import androidx.compose.foundation.clickable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plantandsucculentapp.core.presentation.components.ErrorScreen
import com.example.plantandsucculentapp.core.presentation.util.UiState
import com.example.plantandsucculentapp.plants.data.model.PlantIdentificationResponse
import com.example.plantandsucculentapp.plants.data.model.PlantSuggestion
import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel
import kotlin.math.roundToInt

class PlantIdentificationHelper {
    companion object {
        fun parseIdentificationResult(response: PlantIdentificationResponse): PlantSuggestion {
            // First check if we have valid suggestions
            val validSuggestions = response.suggestions.filter { suggestion -> 
                !suggestion.plantName.isNullOrBlank() && suggestion.plantDetails != null 
            }
            
            when {
                // If we have valid suggestions, use them regardless of isPlant flag
                validSuggestions.isNotEmpty() -> {
                    return validSuggestions[0]
                }
                // If API says it's not a plant with high confidence
                !response.isPlant && response.isPlantProbability < 0.3 -> {
                    throw IllegalStateException("Image does not appear to be a plant (${(response.isPlantProbability * 100).toInt()}% confidence)")
                }
                // If we have suggestions but they're not valid
                response.suggestions.isNotEmpty() -> {
                    throw IllegalStateException("Could not determine plant species. Please try with a clearer photo.")
                }
                // No suggestions at all
                else -> {
                    throw IllegalStateException("No plant species identified. Please try again with a different photo.")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantIdentificationScreen(
    viewModel: PlantsViewModel,
    onSpeciesSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    when (val result = viewModel.identificationResult.collectAsState().value) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> SuccessContent(result.data.suggestions, onSpeciesSelected)
        is UiState.Error -> ErrorContent(result.message) { viewModel.retryIdentification() }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Identifying plant...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
private fun SuccessContent(
    suggestions: List<PlantSuggestion>,
    onSpeciesSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        items(suggestions) { suggestion ->
            PlantSuggestionCard(
                suggestion = suggestion,
                onClick = {
                    onSpeciesSelected(suggestion.plantName)
                }
            )
        }
    }
}

@Composable
private fun PlantSuggestionCard(
    suggestion: PlantSuggestion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = suggestion.plantName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Probability: ${(suggestion.probability * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (suggestion.plantDetails.commonNames.isNotEmpty()) {
                Text(
                    text = "Also known as: ${suggestion.plantDetails.commonNames.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            suggestion.plantDetails.wikiDescription?.value?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}