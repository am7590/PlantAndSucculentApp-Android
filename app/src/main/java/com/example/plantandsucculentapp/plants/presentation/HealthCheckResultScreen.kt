import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthCheckResultScreen(
    viewModel: PlantsViewModel,
    onBack: () -> Unit
) {
    val healthCheckResult by viewModel.lastHealthCheckResult.collectAsState()
    val currentPhoto = viewModel.getCurrentHealthCheckPhoto()
    val gson = Gson()

    DisposableEffect(Unit) {
        // onDispose = onDisappear
        onDispose {
            viewModel.clearCurrentHealthCheckPhoto()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Check Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Photo Section
            item {
                currentPhoto?.let { photoUrl ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Plant photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Health Check Results
            healthCheckResult?.let { result ->
                try {
                    val healthData = gson.fromJson(result, JsonObject::class.java)
                    val healthAssessment = healthData.getAsJsonObject("health_assessment")
                    val isHealthy = healthAssessment?.get("is_healthy")?.asBoolean ?: false
                    val probability = healthAssessment?.get("is_healthy_probability")?.asDouble ?: 0.0
                    
                    // Health Status
                    item {
                        HealthStatusCard(isHealthy, probability)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Disease Section
                    healthAssessment?.getAsJsonArray("diseases")?.let { diseases ->
                        if (diseases.size() > 0) {
                            item {
                                Text(
                                    "Detected Issues",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            items(diseases.size()) { index ->
                                DiseaseCard(diseases[index].asJsonObject)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                } catch (e: Exception) {
                    item {
                        Text("Failed to parse health check result")
                    }
                }
            } ?: item {
                Text("No health check result available")
            }
        }
    }
}


@Composable
private fun HealthStatusCard(isHealthy: Boolean, probability: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp
                )
                
                // Progress circle
                CircularProgressIndicator(
                    progress = probability.toFloat(),
                    modifier = Modifier.fillMaxSize(),
                    color = if (isHealthy) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error,
                    strokeWidth = 12.dp
                )
                
                // Center content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isHealthy) 
                            Icons.Default.CheckCircle 
                        else 
                            Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = if (isHealthy) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = "${(probability * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isHealthy) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isHealthy) "Plant is Healthy" else "Issues Detected",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun DiseaseCard(disease: JsonObject) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = disease.get("name")?.asString ?: "Unknown Issue",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val probability = disease.get("probability")?.asDouble ?: 0.0
            LinearProgressIndicator(
                progress = probability.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            disease.getAsJsonObject("disease_details")?.let { details ->
                Text(
                    text = details.get("description")?.asString ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                details.get("treatment")?.asString?.let { treatment ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Treatment: $treatment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}