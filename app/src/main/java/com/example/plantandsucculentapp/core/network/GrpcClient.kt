package com.example.plantandsucculentapp.core.network

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import plant.PlantOuterClass
import plant.PlantServiceGrpc

class GrpcClient {

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("api.example.com", 443)
        .useTransportSecurity()
        .build()

    private val stub: PlantServiceGrpc.PlantServiceBlockingStub = PlantServiceGrpc.newBlockingStub(channel)

    fun registerOrGetUser(uuid: String): PlantOuterClass.UserResponse {
        val request = PlantOuterClass.UserIdentifier.newBuilder().setUuid(uuid).build()
        return stub.registerOrGetUser(request)
    }

    // Implement other RPC methods as needed
}
