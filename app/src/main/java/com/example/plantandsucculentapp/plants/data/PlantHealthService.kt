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
import java.util.Date
import coil.ImageLoader

private const val TAG = "PlantHealthService"
private const val PLANT_ID_API_URL = "https://api.plant.id/v2/health_assessment"
private const val API_KEY = "S6VUgIM03MvELLMGtMQBEpVuBvtaG0b0UOGoma3iT2oO2OuMYH"
private const val IDENTIFICATION_CACHE_PREFIX = "plant_identification_"

class PlantHealthService(
    private val context: Context,
    private val imageLoader: ImageLoader
) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val sharedPreferences = context.getSharedPreferences("plant_id_cache", Context.MODE_PRIVATE)

    suspend fun checkPlantHealth(photoUrl: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Handle file:// URLs by stripping the prefix
                val file = when {
                    photoUrl.startsWith("file://") -> {
                        File(photoUrl.removePrefix("file://"))
                    }
                    else -> {
                        // Try to get from Coil cache first
                        val snapshot = imageLoader.diskCache?.get(photoUrl)
                        snapshot?.data?.toFile() ?: throw Exception("Image not found in cache")
                    }
                }

                if (!file.exists()) {
                    throw Exception("Image file does not exist: $photoUrl")
                }

                // Use the file for health check
                performHealthCheck(file)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform health check", e)
                throw e
            }
        }
    }

    private fun performHealthCheck(file: File): String {
        val photoBytes = file.readBytes()
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

        val request = Request.Builder()
            .url(PLANT_ID_API_URL)
            .post(jsonPayload.toRequestBody(jsonMediaType))
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API call failed: ${response.code}")
            }
            response.body?.string() ?: throw IOException("Empty response")
        }
    }

    suspend fun identifyPlant(imageUrl: String, skipCache: Boolean = false): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Making new identification request for image: $imageUrl")
            
            val photoBytes = when {
                imageUrl.startsWith("file://") -> {
                    File(imageUrl.removePrefix("file://")).readBytes()
                }
                imageUrl.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(imageUrl))?.use { 
                        it.readBytes() 
                    } ?: throw IOException("Failed to open photo URI")
                }
                else -> throw IllegalArgumentException("Unsupported image URL format: $imageUrl")
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

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body?.string() ?: throw IOException("Empty response body")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Plant identification failed", e)
            throw e
        }
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                // Clear shared preferences cache
                sharedPreferences.edit().clear().apply()
                
                // Clear cached files from internal storage
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("plant_photo_") || 
                        file.name.startsWith("temp_")) {
                        file.delete()
                    }
                }
                
                // Clear cache directory
                context.cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
                
                Log.d(TAG, "Successfully cleared all caches")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
            }
        }
    }
} 