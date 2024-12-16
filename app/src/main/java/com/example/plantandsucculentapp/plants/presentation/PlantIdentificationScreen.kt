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
            val validSuggestions = response.suggestions.filter { suggestion ->
                !suggestion.plantName.isNullOrBlank() && suggestion.plantDetails != null 
            }
            
            when {
                validSuggestions.isNotEmpty() -> {
                    return validSuggestions[0]
                }
                !response.isPlant && response.isPlantProbability < 0.3 -> {
                    throw IllegalStateException("Image does not appear to be a plant (${(response.isPlantProbability * 100).toInt()}% confidence)")
                }
                response.suggestions.isNotEmpty() -> {
                    throw IllegalStateException("Could not determine plant species. Please try with a clearer photo.")
                }
                else -> {
                    throw IllegalStateException("No plant species identified. Please try again with a different photo.")
                }
            }
        }
    }
}
