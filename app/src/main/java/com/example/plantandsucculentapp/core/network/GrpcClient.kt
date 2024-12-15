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
import java.io.File
import android.util.Base64
import io.grpc.StatusRuntimeException

private const val TAG = "GrpcClient"

class GrpcClient : GrpcClientInterface {
    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("10.0.2.2", 9001)
        .usePlaintext()
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveTimeout(10, TimeUnit.SECONDS)
        .maxInboundMessageSize(20 * 1024 * 1024)
        .maxInboundMetadataSize(1024 * 1024)
        .intercept(GrpcLoggingInterceptor())
        .build()

    private val baseStub: PlantServiceGrpc.PlantServiceBlockingStub = PlantServiceGrpc.newBlockingStub(channel)
        .withMaxInboundMessageSize(20 * 1024 * 1024)
        .withMaxOutboundMessageSize(20 * 1024 * 1024)

    private fun getStub(): PlantServiceGrpc.PlantServiceBlockingStub {
        return baseStub.withDeadlineAfter(30, TimeUnit.SECONDS)
    }

    override fun registerOrGetUser(uuid: String): Result<PlantOuterClass.UserResponse> {
        return try {
            val request = PlantOuterClass.UserIdentifier.newBuilder().setUuid(uuid).build()
            Result.success(getStub().registerOrGetUser(request))
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
            Result.success(getStub().getWatered(request))
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
            Result.success(getStub().add(request))
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
            Result.success(getStub().updatePlant(request))
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

    override fun performHealthCheck(
        identifier: PlantOuterClass.PlantIdentifier,
        healthCheckData: String
    ): Result<PlantOuterClass.HealthCheckDataResponse> {
        return try {
            Log.d(TAG, "Starting health check for plant ${identifier.sku}")
            
            val imageFile = File(healthCheckData)
            val bytes = imageFile.readBytes()
            
            if (bytes.size > 10 * 1024 * 1024) {
                Log.e(TAG, "Image too large: ${bytes.size} bytes")
                return Result.failure(Exception("Image file too large (max 10MB)"))
            }
            
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Log.d(TAG, "Converted image to base64 (${base64Image.length} chars)")

            val jsonPayload = buildString {
                append("{")
                append("\"api_key\":\"2b10YPxXYYHtNWGnBbq4cf9V3\",")
                append("\"images\":[\"data:image/jpeg;base64,$base64Image\"],")
                append("\"modifiers\":[\"health_all\",\"disease_similar_images\"],")
                append("\"plant_language\":\"en\",")
                append("\"plant_details\":[\"common_names\",\"url\",\"wiki_description\",\"taxonomy\"]")
                append("}")
            }

            Log.d(TAG, "Created API request payload (${jsonPayload.length} chars)")
            Log.d(TAG, "Request JSON: $jsonPayload")

            val request = PlantOuterClass.HealthCheckDataRequest.newBuilder()
                .setIdentifier(identifier)
                .setHealthCheckInformation(jsonPayload)
                .build()

            Log.d(TAG, "Sending gRPC request")
            val response = getStub().saveHealthCheckData(request)
            Log.d(TAG, "Received health check response: ${response.status}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            if (e is StatusRuntimeException) {
                Log.e(TAG, """
                    gRPC Status: ${e.status}
                    Description: ${e.status.description}
                    Cause: ${e.cause?.message}
                    Trailers: ${e.trailers}
                """.trimIndent())
            }
            Result.failure(e)
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

    override suspend fun saveHealthCheckData(request: PlantOuterClass.HealthCheckDataRequest): Result<PlantOuterClass.HealthCheckDataResponse> {
        return try {
            Result.success(getStub().saveHealthCheckData(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHealthCheckHistory(identifier: PlantOuterClass.PlantIdentifier): Result<PlantOuterClass.HealthCheckInformation> {
        return try {
            Result.success(getStub().healthCheckRequest(
                PlantOuterClass.HealthCheckRequestParam.newBuilder()
                    .setSku(identifier.sku)
                    .build()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
