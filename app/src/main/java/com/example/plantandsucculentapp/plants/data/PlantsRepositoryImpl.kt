package com.example.plantandsucculentapp.plants.data

//import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.core.util.NetworkException
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import plant.PlantOuterClass
import java.net.UnknownHostException

class PlantsRepositoryImpl(
    private val grpcClient: GrpcClientInterface
) : Repository {
    override suspend fun fetchData(): String {
        TODO("Not yet implemented")
    }

    override suspend fun getWateredPlants(): List<PlantOuterClass.Plant> = withContext(Dispatchers.IO) {
        try {
            val result = grpcClient.getWatered("user123")
            result.getOrThrow().plantsList
        } catch (e: Exception) {
            when (e) {
                is UnknownHostException -> throw NetworkException.NoConnection
                else -> throw NetworkException.ServerError
            }
        }
    }

    override suspend fun addPlant(userId: String, plant: PlantOuterClass.Plant): String = 
        withContext(Dispatchers.IO) {
            try {
                val response = grpcClient.addPlant(userId, plant)
                if (response.isFailure) {
                    throw NetworkException.ApiError("Failed to add plant: ${response}")
                }
                response.isSuccess.toString()
            } catch (e: UnknownHostException) {
                throw NetworkException.NoConnection
            } catch (e: Exception) {
                throw NetworkException.ServerError
            }
        }

    override suspend fun updatePlant(
        userId: String, 
        identifier: PlantOuterClass.PlantIdentifier, 
        information: PlantOuterClass.PlantInformation
    ): String {
        val response = grpcClient.updatePlant(userId, identifier, information)
        return response.isSuccess.toString()
    }
}
