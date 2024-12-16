import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import plant.PlantOuterClass
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Search
import java.io.File
import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel
import coil.compose.LocalImageLoader
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantsDetailScreen(
    plant: PlantOuterClass.Plant,
    sku: String,
    viewModel: PlantsViewModel,
    onIdentifyPlant: () -> Unit,
    onWaterPlant: () -> Unit,
    onHealthCheck: () -> Unit,
    onNavigateToHealthResult: () -> Unit,
    onBack: () -> Unit
) {
    val healthCheckResult by viewModel.lastHealthCheckResult.collectAsState()
    
    LaunchedEffect(healthCheckResult) {
        if (viewModel.shouldNavigateToHealthCheck) {
            healthCheckResult?.let {
                onNavigateToHealthResult()
                viewModel.clearNavigateToHealthCheck() // Reset flag after navigation
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plant.information.name) },
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
            // Plant Image Section
            item {
                PlantImageSection(
                    photos = plant.information.photosList,
                    viewModel = viewModel,
                    sku = sku
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Actions Section
            item {
                PlantActionsSection(
                    plant = plant,
                    onWaterPlant = onWaterPlant,
                    onHealthCheck = onHealthCheck,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Plant Info Section
            item {
                PlantInfoSection(plant)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Button(
                    onClick = onIdentifyPlant,
                    enabled = plant.information.photosList.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Identify Species"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Identify Species")
                }
            }

            // Photo History Section
            item {
                Text(
                    "Photo History",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(plant.information.photosList.sortedByDescending { it.timestamp }) { photo ->
                PhotoHistoryItem(photo)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlantImageSection(
    photos: List<PlantOuterClass.PhotoEntry>,
    viewModel: PlantsViewModel,
    sku: String
) {
    val context = LocalContext.current
    val imageLoader = LocalImageLoader.current
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { photos.size }
    )
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.addPhotoToPlant(sku, it, context) }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            if (photos.isNotEmpty()) {
                Column {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        val photoUrl = photos[page].url
                        AsyncImage(
                            model = when {
                                photoUrl.startsWith("file://") -> File(photoUrl.removePrefix("file://"))
                                else -> photoUrl
                            },
                            imageLoader = imageLoader,
                            contentDescription = "Plant photo ${page + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Row(
                        Modifier
                            .height(50.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(photos.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(8.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Add photo FAB
            FloatingActionButton(
                onClick = { 
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Add photo")
            }
        }
    }
}

private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        // Create a unique filename using timestamp
        val filename = "plant_photo_${System.currentTimeMillis()}.jpg"

        // Get the input stream from the URI
        context.contentResolver.openInputStream(uri)?.use { input ->
            // Save to internal storage
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
                input.copyTo(output)
            }
        }

        // Return the full path to the saved file
        context.getFileStreamPath(filename).absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
private fun PlantInfoSection(plant: PlantOuterClass.Plant) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            InfoRow(
                label = "Name",
                value = plant.information.name
            )
            InfoRow(
                label = "Species",
                value = plant.information.identifiedSpeciesName
            )
            InfoRow(
                label = "Last Watered",
                value = formatTimestamp(plant.information.lastWatered)
            )
            InfoRow(
                label = "Last Health Check",
                value = formatTimestamp(plant.information.lastHealthCheck)
            )
            InfoRow(
                label = "ID",
                value = plant.identifier.sku
            )
        }
    }
}

@Composable
private fun PlantActionsSection(
    plant: PlantOuterClass.Plant,
    onWaterPlant: () -> Unit,
    onHealthCheck: () -> Unit,
) {
    val canPerformHealthCheck = plant.information.photosList.isNotEmpty() && 
        (plant.information.lastHealthCheck == 0L || 
         plant.information.photosList.maxOf { it.timestamp } > plant.information.lastHealthCheck)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onWaterPlant,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Water Plant")
        }

        Button(
            onClick = onHealthCheck,
            enabled = canPerformHealthCheck,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        ) {
            Icon(Icons.Default.Favorite, null)
            Spacer(Modifier.width(8.dp))
            Text("Health Check")
        }
    }
}

@Composable
private fun PhotoHistoryItem(photo: PlantOuterClass.PhotoEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val photoUrl = photo.url
            AsyncImage(
                model = when {
                    photoUrl.startsWith("/") -> File(photoUrl)
                    photoUrl.startsWith("file://") -> File(photoUrl.removePrefix("file://"))
                    else -> photoUrl
                },
                contentDescription = "Historical plant photo",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = formatTimestamp(photo.timestamp),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (photo.hasNote()) {
                    Text(
                        text = photo.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}