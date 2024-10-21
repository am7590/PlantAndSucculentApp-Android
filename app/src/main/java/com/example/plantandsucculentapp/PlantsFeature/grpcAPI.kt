package com.example.plantandsucculentapp.PlantsFeature

import retrofit2.http.GET

interface grpcApi {
    @GET("data")
    suspend fun fetchData(): String

    @GET("premium")
    suspend fun fetchPremiumData(): String

    companion object {
        const val BASE_URL = "https://api.example.com/"
    }
}