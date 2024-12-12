package com.example.plantandsucculentapp.core.network

import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import plant.PlantOuterClass
import plant.PlantServiceGrpc
import java.util.concurrent.TimeUnit

private const val TAG = "GrpcClient"

class GrpcClient {
    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("10.10.242.10", 9001)
        .usePlaintext()
        .build()

    private val stub: PlantServiceGrpc.PlantServiceBlockingStub = PlantServiceGrpc.newBlockingStub(channel)

    fun registerOrGetUser(uuid: String): Result<PlantOuterClass.UserResponse> {
        return try {
            val request = PlantOuterClass.UserIdentifier.newBuilder().setUuid(uuid).build()
            Result.success(stub.registerOrGetUser(request))
        } catch (e: StatusException) {
            handleGrpcError(e, "registerOrGetUser")
        } catch (e: Exception) {
            handleGenericError(e, "registerOrGetUser")
        }
    }

    fun getWatered(userId: String): Result<PlantOuterClass.ListOfPlants> {
        return try {
            val request = PlantOuterClass.GetWateredRequest.newBuilder()
                .setUuid(userId)
                .build()
            Result.success(stub.getWatered(request))
        } catch (e: StatusException) {
            handleGrpcError(e, "getWatered")
        } catch (e: Exception) {
            handleGenericError(e, "getWatered")
        }
    }

    fun addPlant(userId: String, plant: PlantOuterClass.Plant): Result<PlantOuterClass.PlantResponse> {
        return try {
            val request = PlantOuterClass.AddPlantRequest.newBuilder()
                .setUserId(userId)
                .setPlant(plant)
                .build()
            Result.success(stub.add(request))
        } catch (e: StatusException) {
            handleGrpcError(e, "addPlant")
        } catch (e: Exception) {
            handleGenericError(e, "addPlant")
        }
    }

    fun updatePlant(
        userId: String,
        identifier: PlantOuterClass.PlantIdentifier,
        information: PlantOuterClass.PlantInformation
    ): Result<PlantOuterClass.PlantUpdateResponse> {
        return try {
            val request = PlantOuterClass.PlantUpdateRequest.newBuilder()
                .setUuid(userId)
                .setIdentifier(identifier)
                .setInformation(information)
                .build()
            Result.success(stub.updatePlant(request))
        } catch (e: StatusException) {
            handleGrpcError(e, "updatePlant")
        } catch (e: Exception) {
            handleGenericError(e, "updatePlant")
        }
    }

    fun testConnection(): Boolean {
        return try {
            val result = registerOrGetUser("test-connection")
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }

    private fun <T : Any> handleGrpcError(e: StatusException, methodName: String): Result<T> {
        val errorMessage = when (e.status.code) {
            Status.Code.UNAVAILABLE -> "Server is unavailable"
            Status.Code.DEADLINE_EXCEEDED -> "Request timed out"
            Status.Code.UNAUTHENTICATED -> "Authentication failed"
            Status.Code.PERMISSION_DENIED -> "Permission denied"
            Status.Code.NOT_FOUND -> "Resource not found"
            Status.Code.ALREADY_EXISTS -> "Resource already exists"
            Status.Code.INVALID_ARGUMENT -> "Invalid argument provided"
            else -> "gRPC error: ${e.status.code}"
        }
        Log.e(TAG, "[gRPC] $methodName failed: $errorMessage", e)
        return Result.failure(e)
    }

    private fun <T> handleGenericError(e: Exception, methodName: String): Result<T> {
        Log.e(TAG, "$methodName failed with unexpected error", e)
        return Result.failure(e)
    }

    fun shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down gRPC channel", e)
        }
    }
}