package com.example.plantandsucculentapp.plants.data.model

import com.google.gson.annotations.SerializedName

data class PlantIdentificationResponse(
    val suggestions: List<PlantSuggestion>,
    @SerializedName("is_plant") val isPlant: Boolean,
    @SerializedName("is_plant_probability") val isPlantProbability: Double
)

data class PlantSuggestion(
    val id: Long,
    @SerializedName("plant_name") val plantName: String,
    val probability: Double,
    @SerializedName("plant_details") val plantDetails: PlantDetails
)

data class PlantDetails(
    @SerializedName("common_names") val commonNames: List<String>,
    val url: String,
    @SerializedName("wiki_description") val wikiDescription: WikiDescription,
    val taxonomy: Taxonomy
)

data class WikiDescription(
    val value: String,
    val citation: String,
    @SerializedName("license_name") val licenseName: String,
    @SerializedName("license_url") val licenseUrl: String
)

data class Taxonomy(
    val genus: String,
    val family: String,
    val order: String,
    val phylum: String,
    val kingdom: String,
    @SerializedName("class") val taxonomyClass: String
) 