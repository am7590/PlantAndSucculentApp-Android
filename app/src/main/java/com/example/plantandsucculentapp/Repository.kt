package com.example.plantandsucculentapp

interface Repository {
    suspend fun fetchData(): String
}