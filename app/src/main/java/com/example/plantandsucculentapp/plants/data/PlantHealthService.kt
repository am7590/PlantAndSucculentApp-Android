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
import android.content.Intent
import java.util.concurrent.TimeUnit
import android.provider.MediaStore
import android.content.pm.PackageManager

private const val TAG = "PlantHealthService"
private const val PLANT_ID_API_URL = "https://api.plant.id/v2/health_assessment"
private const val API_KEY = "S6VUgIM03MvELLMGtMQBEpVuBvtaG0b0UOGoma3iT2oO2OuMYH"
private const val IDENTIFICATION_CACHE_PREFIX = "plant_identification_"

class PlantHealthService(private val context: Context) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val sharedPreferences = context.getSharedPreferences("plant_id_cache", Context.MODE_PRIVATE)

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

    suspend fun identifyPlant(imageUrl: String, skipCache: Boolean = false): String = withContext(Dispatchers.IO) {
        try {
            // Check cache first (unless skipCache is true)
            val cacheKey = "$IDENTIFICATION_CACHE_PREFIX${imageUrl.hashCode()}"
            
            if (!skipCache) {
                // Only use cache if it's less than 1 hour old
                val cachedResult = sharedPreferences.getString(cacheKey, null)
                val cacheTimestamp = sharedPreferences.getLong("${cacheKey}_timestamp", 0)
                val cacheAge = System.currentTimeMillis() - cacheTimestamp
                
                if (cachedResult != null && cacheAge < TimeUnit.HOURS.toMillis(1)) {
                    Log.d(TAG, "Returning cached identification result")
                    return@withContext cachedResult
                }
            }
            
            Log.d(TAG, "Making new identification request for: $imageUrl")
            
            val photoBytes = when {
                imageUrl.startsWith("content://") -> {
                    val uri = Uri.parse(imageUrl)
                    try {
                        // First try to read directly
                        context.contentResolver.openInputStream(uri)?.use { 
                            it.readBytes() 
                        } ?: throw IOException("Failed to open photo URI")
                    } catch (e: SecurityException) {
                        // If that fails, try to copy to our internal storage first
                        val fileName = "temp_${System.currentTimeMillis()}.jpg"
                        val internalFile = File(context.cacheDir, fileName)
                        
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            internalFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        // Read from our internal copy
                        internalFile.readBytes().also {
                            internalFile.delete() // Clean up
                        }
                    }
                }
                imageUrl.startsWith("/") -> File(imageUrl).readBytes()
                else -> throw IllegalArgumentException("Unsupported image URL format")
            }
            
            val base64Image = Base64.encodeToString(photoBytes, Base64.NO_WRAP)

            val jsonPayload = """
                {
                    "api_key": "$API_KEY",
                    "images": ["data:image/jpeg;base64,$base64Image"],
                    "plant_details": ["common_names", "url", "wiki_description", "taxonomy"],
                    "plant_language": "en",
                    "modifiers": ["crops_fast", "similar_images"]
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("https://api.plant.id/v2/identify")
                .post(jsonPayload.toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")
                .build()

            val result = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body?.string() ?: throw IOException("Empty response body")
            }

            // Cache the result with timestamp
            sharedPreferences.edit()
                .putString(cacheKey, result)
                .putLong("${cacheKey}_timestamp", System.currentTimeMillis())
                .apply()
            Log.d(TAG, "Cached new identification result")

            result
        } catch (e: Exception) {
            Log.e(TAG, "Plant identification failed", e)
            throw e
        }
    }
} 