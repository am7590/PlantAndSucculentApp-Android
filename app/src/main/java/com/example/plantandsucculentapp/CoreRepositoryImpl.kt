package com.example.plantandsucculentapp

class CoreRepositoryImpl(
    private val api: grpcApi
): Repository {
    override suspend fun fetchData(): String {
        api
        return "CoreRepositoryImpl-getData()"
    }
}