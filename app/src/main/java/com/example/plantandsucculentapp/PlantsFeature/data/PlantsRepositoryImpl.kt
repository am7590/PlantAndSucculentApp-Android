package com.example.plantandsucculentapp.PlantsFeature.data

import com.example.plantandsucculentapp.PlantsFeature.grpcApi
import com.example.plantandsucculentapp.PlantsFeature.domain.Repository

class PlantsRepositoryImpl(
    private val api: grpcApi
): Repository {
    override suspend fun fetchData(): String {
        api
        return "PlantsRepositoryImpl-getData()"
    }
}