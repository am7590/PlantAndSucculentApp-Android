package com.example.plantandsucculentapp.core.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.plants.presentation.PlantsScreen
import com.example.plantandsucculentapp.core.presentation.ui.theme.PlantAndSucculentAppTheme
import com.example.plantandsucculentapp.plants.presentation.NewPlantScreen
import com.example.plantandsucculentapp.plants.presentation.PlantDetailScreen
import org.koin.androidx.compose.koinViewModel
import plant.PlantOuterClass

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantAndSucculentAppTheme {
                val mainViewModel: MainViewModel = koinViewModel()
                PlantApp()
            }
        }
    }
}

@Composable
fun PlantApp() {
    val navController = rememberNavController()
    val mockClient = MockGrpcClient()

    NavHost(navController, startDestination = "plants") {
        composable("plants") {
            val plants = mockClient.getWatered("user123").plantsList

            PlantsScreen(
                plants = plants,
                onAddPlantClick = { navController.navigate("newPlant") },
                onPlantClick = { plant ->
                    navController.navigate("plantDetail/${plant.identifier}")
                }
            )
        }

        // New Plant Screen
        composable("newPlant") {
            NewPlantScreen(
                onCreatePlant = { userId, plantInfo ->
                    mockClient.addPlant(
                        userId,
                        PlantOuterClass.Plant.newBuilder().setInformation(plantInfo).build()
                    )
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        // Plant Detail Screen
        composable("plantDetail/{sku}") { backStackEntry ->
            val sku = backStackEntry.arguments?.getString("sku") ?: return@composable
            val plant = mockClient.getPlant("user123", sku)

            PlantDetailScreen(
                plant = plant,
                onWaterPlant = {
                    mockClient.updatePlant(
                        "user123",
                        plant.identifier,
                        plant.information.toBuilder()
                            .setLastWatered(System.currentTimeMillis())
                            .build()
                    )
                },
                onHealthCheck = {
                    mockClient.healthCheckRequest("user123", sku)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
