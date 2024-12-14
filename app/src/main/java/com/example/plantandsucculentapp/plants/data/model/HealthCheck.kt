package com.example.plantandsucculentapp.plants.data.model

data class HealthCheckResponse(
    val status: String,
    val result: HealthCheckResult?
)

data class HealthCheckResult(
    val probability: Double,
    val historicalProbabilities: HistoricalProbabilities
)

data class HistoricalProbabilities(
    val probabilities: List<Probability>
)

data class Probability(
    val id: String,
    val name: String,
    val probability: Double,
    val date: Long
) 