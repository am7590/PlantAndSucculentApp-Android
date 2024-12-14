package com.example.plantandsucculentapp.plants.data

//import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.core.util.NetworkException
import com.example.plantandsucculentapp.plants.data.local.PlantDao
import com.example.plantandsucculentapp.plants.data.local.toEntity
import com.example.plantandsucculentapp.plants.data.local.toPlant
import com.example.plantandsucculentapp.plants.domain.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import plant.PlantOuterClass
import java.net.UnknownHostException

class PlantsRepositoryImpl(
    private val grpcClient: GrpcClientInterface,
    private val plantDao: PlantDao,
    private val isMockEnabled: Boolean
) : Repository {
    override suspend fun fetchData(): String {
        return grpcClient.testConnection().toString()
    }

    override suspend fun getWateredPlants(): List<PlantOuterClass.Plant> = withContext(Dispatchers.IO) {
        if (isMockEnabled) {
            // Use mock data directly
            val result = grpcClient.getWatered("user123")
            result.getOrThrow().plantsList
        } else {
            // For real implementation, just get plants from local cache
            plantDao.getAllPlants().first().map { it.toPlant() }
        }
    }

    override suspend fun addPlant(userId: String, plant: PlantOuterClass.Plant): String = 
        withContext(Dispatchers.IO) {
            try {
                val response = grpcClient.addPlant(userId, plant)
                if (!isMockEnabled) {
                    // Cache the plant locally after successful server add
                    plantDao.insertPlant(plant.toEntity())
                }
                response.getOrThrow().status
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
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = grpcClient.updatePlant(userId, identifier, information)
            if (!isMockEnabled) {
                // Update the plant in local cache after successful server update
                val updatedPlant = PlantOuterClass.Plant.newBuilder()
                    .setIdentifier(identifier)
                    .setInformation(information)
                    .build()
                plantDao.insertPlant(updatedPlant.toEntity())
            }
            response.getOrThrow().status
        } catch (e: UnknownHostException) {
            throw NetworkException.NoConnection
        } catch (e: Exception) {
            throw NetworkException.ServerError
        }
    }
}
