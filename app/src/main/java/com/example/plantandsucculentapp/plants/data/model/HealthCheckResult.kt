data class HealthCheckResult(
    val isHealthy: Boolean,
    val healthyProbability: Double,
    val diseases: List<Disease>,
    val similarImages: List<SimilarImage>
)

data class Disease(
    val name: String,
    val probability: Double,
    val details: DiseaseDetails?
)

data class DiseaseDetails(
    val localName: String,
    val description: String?,
    val treatment: String?
)

data class SimilarImage(
    val id: String,
    val url: String,
    val similarity: Double,
    val citation: String?
) 