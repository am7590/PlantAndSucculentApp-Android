package com.example.plantandsucculentapp.plants.data.model

data class PlantIdentificationResponse(
    val suggestions: List<PlantSuggestion>,
    val isPlant: Boolean,
    val isPlantProbability: Double
)

data class PlantSuggestion(
    val id: Int,
    val plantName: String,
    val probability: Double,
    val plantDetails: PlantDetails
)

data class PlantDetails(
    val commonNames: List<String>,
    val url: String,
    val wikiDescription: WikiDescription,
    val taxonomy: Taxonomy
)

data class WikiDescription(
    val value: String
)

data class Taxonomy(
    val genus: String,
    val family: String,
    val order: String,
    val phylum: String,
    val kingdom: String
) 