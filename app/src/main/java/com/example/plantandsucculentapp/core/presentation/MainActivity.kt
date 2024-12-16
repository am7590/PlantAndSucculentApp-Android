package com.example.plantandsucculentapp.core.presentation

import HealthCheckResultScreen
import PlantsDetailScreen
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.identity.util.UUID
import com.example.plantandsucculentapp.core.presentation.components.ErrorScreen
import com.example.plantandsucculentapp.core.presentation.components.LoadingScreen
import com.example.plantandsucculentapp.plants.presentation.PlantsScreen
import com.example.plantandsucculentapp.core.presentation.ui.theme.PlantAndSucculentAppTheme
import com.example.plantandsucculentapp.core.presentation.util.UiState
import com.example.plantandsucculentapp.plants.presentation.NewPlantScreen
import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel
import com.example.plantandsucculentapp.plants.trends.TrendsScreen
import org.koin.androidx.compose.koinViewModel
import plant.PlantOuterClass
import android.provider.Settings
import androidx.annotation.RequiresApi
import android.Manifest
import android.annotation.SuppressLint
import androidx.navigation.navArgument
import android.widget.Toast
import android.net.Uri
import android.content.Intent
import android.util.Log
import com.example.plantandsucculentapp.plants.presentation.PlantIdentificationDetailScreen
import com.example.plantandsucculentapp.plants.presentation.PlantIdentificationHelper

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val deviceId: String by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Photo permissions are required for plant identification",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun takePersistablePermission(uri: Uri) {
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            Log.d(TAG, "Successfully took persistable permission for URI: $uri")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to take persistable permission for URI: $uri", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            )
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }

        setContent {
            PlantAndSucculentAppTheme {
                val plantsViewModel: PlantsViewModel = koinViewModel()
                PlantApp(plantsViewModel, deviceId)
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlantApp(plantsViewModel: PlantsViewModel, deviceId: String) {
    val navController = rememberNavController()

    val tabs = listOf(
        TabItem("Plants", Icons.Default.Favorite, "plants"),
        TabItem("Trends", Icons.Default.KeyboardArrowUp, "trends")
    )

    Scaffold(
        bottomBar = { BottomAppBar(navController, tabs) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "plants",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("plants") {
                PlantsScreen(
                    viewModel = plantsViewModel,
                    onAddPlantClick = { navController.navigate("newPlant") },
                    onPlantClick = { plant ->
                        navController.navigate("plantDetail/${plant.identifier.sku}")
                    }
                )
            }
            composable("newPlant") {
                NewPlantScreen(
                    onCreatePlant = { userId, plantInfo ->
                        val newPlant = PlantOuterClass.Plant.newBuilder()
                            .setIdentifier(
                                PlantOuterClass.PlantIdentifier.newBuilder()
                                    .setSku(UUID.randomUUID().toString())
                                    .setDeviceIdentifier(deviceId)
                                    .build()
                            )
                            .setInformation(plantInfo)
                            .build()

                        plantsViewModel.addPlant(userId, newPlant)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable("plantDetail/{sku}") { backStackEntry ->
                val sku = backStackEntry.arguments?.getString("sku") ?: return@composable
                val plant = (plantsViewModel.plantsState.value as? UiState.Success)?.data
                    ?.find { it.identifier.sku == sku } ?: return@composable

                PlantsDetailScreen(
                    plant = plant,
                    sku = sku,
                    viewModel = plantsViewModel,
                    onIdentifyPlant = {
                        navController.navigate("plantIdentification/${plant.identifier.sku}")
                    },
                    onWaterPlant = {
                        // Handle water plant
                    },
                    onHealthCheck = {
                        plantsViewModel.performHealthCheck(plant)
                    },
                    onNavigateToHealthResult = {
                        navController.navigate("healthCheckResult")
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("trends") {
                TrendsScreen(viewModel = plantsViewModel)
            }
            composable("healthCheckResult") {
                val healthCheckResult = plantsViewModel.lastHealthCheckResult.collectAsState()
                healthCheckResult.value?.let { it ->
                    HealthCheckResultScreen(viewModel = plantsViewModel) {
                        navController.popBackStack()
                    }
                }
            }
            composable(
                route = "plantIdentification/{sku}",
                arguments = listOf(navArgument("sku") { type = NavType.StringType })
            ) { backStackEntry ->
                val sku = backStackEntry.arguments?.getString("sku") ?: return@composable
                val currentPlant = (plantsViewModel.plantsState.value as? UiState.Success)?.data
                    ?.find { it.identifier.sku == sku } ?: return@composable

                when (val identificationResult = plantsViewModel.identificationResult.collectAsState().value) {
                    is UiState.Success -> {
                        val suggestion = PlantIdentificationHelper.parseIdentificationResult(identificationResult.data)
                        PlantIdentificationDetailScreen(
                            suggestion = suggestion,
                            onBackClick = {
                                navController.popBackStack()
                            }) {

                            val updatedInformation = currentPlant.information.toBuilder()
                                .setIdentifiedSpeciesName(suggestion.plantName)
                                .setLastIdentification(System.currentTimeMillis())
                                .build()

                            plantsViewModel.updatePlant(
                                deviceId,
                                currentPlant.identifier,
                                updatedInformation
                            )
                            navController.popBackStack("plantDetail/$sku", inclusive = false)
                        }
                    }
                    is UiState.Loading -> {
                        LoadingScreen()
                    }
                    is UiState.Error -> {
                        ErrorScreen(
                            message = identificationResult.message,
                            onRetry = { plantsViewModel.fetchPlantList() }
                        )
                    }
                }
            }
            composable(
                route = "identificationDetail/{sku}",
                arguments = listOf(navArgument("sku") { type = NavType.StringType })
            ) { backStackEntry ->
                val sku = backStackEntry.arguments?.getString("sku") ?: return@composable
                val identificationResult = plantsViewModel.identificationResult.value
                
                if (identificationResult is UiState.Success) {
                    val selectedSpecies = plantsViewModel.selectedSpecies
                    val suggestion = identificationResult.data.suggestions.find { 
                        it.plantName == selectedSpecies 
                    } ?: return@composable

                    PlantIdentificationDetailScreen(
                        suggestion = suggestion,
                        onBackClick = {
                            navController.popBackStack()
                        }) {
                        val currentPlant = (plantsViewModel.plantsState.value as? UiState.Success)?.data
                            ?.find { it.identifier.sku == sku } ?: return@PlantIdentificationDetailScreen

                        val updatedInformation = currentPlant.information.toBuilder()
                            .setIdentifiedSpeciesName(suggestion.plantName)
                            .setLastIdentification(System.currentTimeMillis())
                            .build()

                        plantsViewModel.updatePlant(
                            "user123",
                            currentPlant.identifier,
                            updatedInformation
                        )
                        navController.popBackStack("plantDetail/$sku", inclusive = false)

                    }
                }
            }
        }
    }
}

@Composable
fun BottomAppBar(navController: NavController, tabs: List<TabItem>) {
    NavigationBar {
        val currentDestination by navController.currentBackStackEntryAsState()
        tabs.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.title) },
                selected = currentDestination?.destination?.route == tab.route,
                onClick = { navController.navigate(tab.route) }
            )
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector, val route: String)

