package com.example.plantandsucculentapp

class PlantsRepositoryImpl(
    private val api: grpcApi
): Repository {
    override suspend fun fetchData(): String {
        api
        return "PlantsRepositoryImpl-getData()"
    }
}