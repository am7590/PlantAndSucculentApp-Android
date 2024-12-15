package com.example.plantandsucculentapp.plants.data

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import java.io.File

private const val TAG = "PlantHealthService"
private const val PLANT_ID_API_URL = "https://api.plant.id/v2/health_assessment"
private const val API_KEY = "S6VUgIM03MvELLMGtMQBEpVuBvtaG0b0UOGoma3iT2oO2OuMYH"

class PlantHealthService(private val context: Context) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkPlantHealth(photoPath: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting plant health check with path: $photoPath")
            
            // Read photo bytes - handle both content URIs and file paths
            val photoBytes = if (photoPath.startsWith("content://")) {
                // Handle content URI
                context.contentResolver.openInputStream(Uri.parse(photoPath))?.use { 
                    it.readBytes() 
                } ?: throw IOException("Failed to open photo URI")
            } else {
                // Handle file path
                java.io.File(photoPath).readBytes()
            }
            
            val base64Image = Base64.encodeToString(photoBytes, Base64.NO_WRAP)
            
            val jsonPayload = """
                {
                    "api_key": "$API_KEY",
                    "images": ["data:image/jpeg;base64,$base64Image"],
                    "modifiers": ["health_only", "similar_images"],
                    "plant_language": "en",
                    "plant_details": ["common_names", "url", "wiki_description", "taxonomy"]
                }
            """.trimIndent()

            Log.d(TAG, "Created API request payload")

            val request = Request.Builder()
                .url(PLANT_ID_API_URL)
                .post(jsonPayload.toRequestBody(jsonMediaType))
                .build()

            Log.d(TAG, "Sending request to Plant.id API")
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("API call failed: ${response.code}")
                }
                
                val result = response.body?.string() ?: throw IOException("Empty response")
                Log.d(TAG, "Received response: $result")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            throw e
        }
    }

//    suspend fun identifyPlant(imageUrl: String): String {
//        val imageFile = File(imageUrl)
//        val bytes = imageFile.readBytes()
//        val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
//
//        val jsonPayload = buildString {
//            append("{")
//            append("\"api_key\":\"S6VUgIM03MvELLMGtMQBEpVuBvtaG0b0UOGoma3iT2oO2OuMYH\",")
//            append("\"images\":[\"data:image/jpeg;base64,$base64Image\"],")
//            append("\"plant_details\":[\"common_names\",\"url\",\"wiki_description\",\"taxonomy\"],")
//            append("\"plant_language\":\"en\"")
//            append("}")
//        }
//
//        val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
//
//        val request = Request.Builder()
//            .url("https://api.plant.id/v2/identify")
//            .post(requestBody)
//            .build()
//
//        return client.newCall(request).execute().use { response ->
//            if (!response.isSuccessful) throw IOException("Unexpected code $response")
//            response.body?.string() ?: throw IOException("Empty response body")
//        }
//    }
} 