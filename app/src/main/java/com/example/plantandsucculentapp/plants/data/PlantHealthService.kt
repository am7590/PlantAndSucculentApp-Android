package com.example.plantandsucculentapp.plants.data

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "PlantHealthService"
private const val PLANT_ID_API_URL = "https://api.plant.id/v2/health_assessment"

class PlantHealthService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkPlantHealth(imagePath: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting plant health check")
            
            val imageFile = File(imagePath)
            val bytes = imageFile.readBytes()
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
            
            val jsonPayload = buildString {
                append("{")
                append("\"api_key\":\"S6VUgIM03MvELLMGtMQBEpVuBvtaG0b0UOGoma3iT2oO2OuMYH\",")
                append("\"images\":[\"data:image/jpeg;base64,$base64Image\"],")
                append("\"modifiers\":[\"health_all\",\"disease_similar_images\"],")
                append("\"plant_language\":\"en\",")
                append("\"plant_details\":[\"common_names\",\"url\",\"wiki_description\",\"taxonomy\"]")
                append("}")
            }

            Log.d(TAG, "Created API request payload")

            val request = Request.Builder()
                .url(PLANT_ID_API_URL)
                .post(jsonPayload.toRequestBody(jsonMediaType))
                .build()

            Log.d(TAG, "Sending request to Plant.id API")
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("API call failed: ${response.code}")
                }
                
                val result = response.body?.string() ?: throw Exception("Empty response")
                Log.d(TAG, "Received response: $result")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            throw e
        }
    }
} 