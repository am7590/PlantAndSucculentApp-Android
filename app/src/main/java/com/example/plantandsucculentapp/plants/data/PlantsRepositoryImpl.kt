package com.example.plantandsucculentapp.plants.data

//import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.core.util.NetworkException
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import plant.PlantOuterClass
import java.net.UnknownHostException

class PlantsRepositoryImpl(
    private val mockGrpcClient: MockGrpcClient
) : Repository {

    override suspend fun fetchData(): String {
        // no-op for now
        // in a non-mock setting, this would connect us to the gRPC server
        return ""
    }

    override suspend fun getWateredPlants(): List<PlantOuterClass.Plant> = withContext(Dispatchers.IO) {
        try {
            mockGrpcClient.getWatered("user123").plantsList
        } catch (e: UnknownHostException) {
            throw NetworkException.NoConnection
        } catch (e: Exception) {
            throw NetworkException.ServerError
        }
    }

    override suspend fun addPlant(userId: String, plant: PlantOuterClass.Plant): String = 
        withContext(Dispatchers.IO) {
            try {
                val response = mockGrpcClient.addPlant(userId, plant)
                if (response.status != "SUCCESS") {
                    throw NetworkException.ApiError("Failed to add plant: ${response.status}")
                }
                response.status
            } catch (e: UnknownHostException) {
                throw NetworkException.NoConnection
            } catch (e: Exception) {
                throw NetworkException.ServerError
            }
        }

    override suspend fun updatePlant(userId: String, identifier: PlantOuterClass.PlantIdentifier, information: PlantOuterClass.PlantInformation): String {
        val response = mockGrpcClient.updatePlant(userId, identifier, information)
        return response.status
    }
}
