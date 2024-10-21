package com.example.plantandsucculentapp.core.data

import com.example.plantandsucculentapp.PlantsFeature.domain.Repository
import com.example.plantandsucculentapp.PlantsFeature.grpcApi

class CoreRepositoryImpl(
    private val api: grpcApi
): Repository {
    override suspend fun fetchData(): String {
        api
        return "CoreRepositoryImpl-getData()"
    }
}