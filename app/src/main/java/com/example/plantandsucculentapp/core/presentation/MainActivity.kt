package com.example.plantandsucculentapp.core.presentation

import PlantsDetailScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import plant.PlantOuterClass
import android.provider.Settings

class MainActivity : ComponentActivity() {
    private val deviceId: String by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantAndSucculentAppTheme {
                val plantsViewModel: PlantsViewModel = koinViewModel()
                PlantApp(plantsViewModel, deviceId)
            }
        }
    }
}

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

                when (val currentState = plantsViewModel.plantsState.collectAsState().value) {
                    is UiState.Success -> {
                        val plant = currentState.data.find { it.identifier.sku == sku }
                        if (plant != null) {
                            PlantsDetailScreen(
                                plant = plant,
                                onWaterPlant = {
                                    val updatedPlant = plant.toBuilder()
                                        .setInformation(
                                            plant.information.toBuilder()
                                                .setLastWatered(System.currentTimeMillis())
                                                .build()
                                        )
                                        .build()
                                    plantsViewModel.updatePlant("user123", updatedPlant.identifier, updatedPlant.information)
                                },
                                onHealthCheck = {
                                    // TODO: implement health check
                                },
                                onAddPhoto = { photoUrl ->
                                    val updatedPlant = plant.toBuilder()
                                        .setInformation(
                                            plant.information.toBuilder()
                                                .addPhotos(
                                                    PlantOuterClass.PhotoEntry.newBuilder()
                                                        .setUrl(photoUrl)
                                                        .setTimestamp(System.currentTimeMillis())
                                                        .setNote("Plant photo")
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build()
                                    plantsViewModel.updatePlant("user123", updatedPlant.identifier, updatedPlant.information)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    is UiState.Loading -> {
                        LoadingScreen()
                    }
                    is UiState.Error -> {
                        ErrorScreen(
                            message = currentState.message,
                            onRetry = { plantsViewModel.fetchPlantList() }
                        )
                    }
                }
            }
            composable("trends") {
                TrendsScreen(viewModel = plantsViewModel)
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

