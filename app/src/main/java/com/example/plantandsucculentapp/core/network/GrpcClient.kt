package com.example.plantandsucculentapp.core.network

import android.util.Log
import io.grpc.CallOptions
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import plant.PlantOuterClass
import plant.PlantServiceGrpc
import java.util.concurrent.TimeUnit
import java.net.ConnectException

private const val TAG = "GrpcClient"

class GrpcClient : GrpcClientInterface {
    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("10.0.2.2", 50051)
        .usePlaintext()
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveTimeout(10, TimeUnit.SECONDS)
        .build()

    private val stub: PlantServiceGrpc.PlantServiceBlockingStub = PlantServiceGrpc.newBlockingStub(channel)

    override fun registerOrGetUser(uuid: String): Result<PlantOuterClass.UserResponse> {
        return try {
            val request = PlantOuterClass.UserIdentifier.newBuilder().setUuid(uuid).build()
            Result.success(stub.registerOrGetUser(request))
        } catch (e: StatusException) {
            handleGrpcError(e, "registerOrGetUser")
        } catch (e: Exception) {
            handleGenericError(e, "registerOrGetUser")
        }
    }

    override fun getWatered(userId: String): Result<PlantOuterClass.ListOfPlants> {
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

    override fun addPlant(userId: String, plant: PlantOuterClass.Plant): Result<PlantOuterClass.PlantResponse> {
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

    override fun updatePlant(
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

    override fun testConnection(): Boolean {
        return try {
            val result = registerOrGetUser("test-connection")
            when {
                result.isSuccess -> {
                    Log.d(TAG, "Successfully connected to gRPC server")
                    true
                }
                result.isFailure -> {
                    val exception = result.exceptionOrNull()
                    when (exception) {
                        is StatusException -> {
                            Log.e(TAG, "gRPC status error: ${exception.status}", exception)
                            when (exception.status.code) {
                                Status.Code.UNAVAILABLE -> {
                                    val cause = exception.cause
                                    if (cause is ConnectException) {
                                        Log.e(TAG, "Connection refused. Is the server running on port 50051?", cause)
                                    } else {
                                        Log.e(TAG, "Server unavailable", cause)
                                    }
                                }
                                else -> Log.e(TAG, "Unexpected status: ${exception.status}", exception)
                            }
                            false
                        }
                        else -> {
                            Log.e(TAG, "Unexpected error during connection test", exception)
                            false
                        }
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed with exception", e)
            false
        }
    }

    private fun <T> handleGrpcError(e: StatusException, methodName: String): Result<T> {
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
        Log.e(TAG, "$methodName failed: $errorMessage", e)
        return Result.failure(e)
    }

    private fun <T> handleGenericError(e: Exception, methodName: String): Result<T> {
        Log.e(TAG, "$methodName failed with unexpected error", e)
        return Result.failure(e)
    }

    override fun shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down gRPC channel", e)
        }
    }
}
