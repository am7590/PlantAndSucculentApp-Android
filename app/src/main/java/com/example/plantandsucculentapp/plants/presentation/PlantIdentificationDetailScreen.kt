package com.example.plantandsucculentapp.plants.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plantandsucculentapp.plants.data.model.PlantSuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantIdentificationDetailScreen(
    suggestion: PlantSuggestion,
    onBackClick: () -> Unit,
    onSelectPlant: (PlantSuggestion) -> Unit
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(suggestion.plantName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = { }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            InfoSection(
                title = "Scientific Name",
                content = suggestion.plantName
            )
            
            if (suggestion.plantDetails.commonNames.isNotEmpty()) {
                InfoSection(
                    title = "Common Names",
                    content = suggestion.plantDetails.commonNames.joinToString(", ")
                )
            }

            InfoSection(
                title = "Description",
                content = suggestion.plantDetails.wikiDescription.toString()
            )

            InfoSection(
                title = "Taxonomy",
                content = """
                    Family: ${suggestion.plantDetails.taxonomy.family}
                    Genus: ${suggestion.plantDetails.taxonomy.genus}
                    Order: ${suggestion.plantDetails.taxonomy.order}
                """.trimIndent()
            )

            suggestion.plantDetails.url?.let {
                Text(
                    text = "Learn more",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
} 