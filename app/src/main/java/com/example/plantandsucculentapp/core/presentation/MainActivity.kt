package com.example.plantandsucculentapp.core.presentation

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.plants.presentation.PlantsScreen
import com.example.plantandsucculentapp.core.presentation.ui.theme.PlantAndSucculentAppTheme
import com.example.plantandsucculentapp.plants.presentation.NewPlantScreen
import com.example.plantandsucculentapp.plants.presentation.PlantDetailScreen
import com.example.plantandsucculentapp.plants.trends.TrendsScreen
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
    val plants = mockClient.getWatered("user123").plantsList

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
                val plants = mockClient.getWatered("user123").plantsList

                PlantsScreen(
                    plants = plants,
                    onAddPlantClick = { navController.navigate("newPlant") },
                    onPlantClick = { plant ->
                        navController.navigate("plantDetail/${plant.identifier}")
                    }
                )
            }
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
            composable("trends") {
                TrendsScreen(plants = plants)
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

