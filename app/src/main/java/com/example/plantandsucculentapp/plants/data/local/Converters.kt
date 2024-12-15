package com.example.plantandsucculentapp.plants.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromPhotoList(value: List<PhotoEntity>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toPhotoList(value: String): List<PhotoEntity> {
        val type = object : TypeToken<List<PhotoEntity>>() {}.type
        return try {
            Gson().fromJson(value, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromHealthCheckList(value: List<HealthCheckEntity>): String {
        return try {
            val json = Gson().toJson(value)
            Log.d("Converters", "Converting health check list to string: $json")
            json
        } catch (e: Exception) {
            Log.e("Converters", "Failed to convert health check list to string", e)
            "[]"
        }
    }

    @TypeConverter
    fun toHealthCheckList(value: String): List<HealthCheckEntity> {
        return try {
            val type = object : TypeToken<List<HealthCheckEntity>>() {}.type
            val result = Gson().fromJson<List<HealthCheckEntity>>(value, type) ?: emptyList()
            Log.d("Converters", "Converting string to health check list. Input: $value, Output size: ${result.size}")
            result
        } catch (e: Exception) {
            Log.e("Converters", "Failed to convert string to health check list. Input: $value", e)
            emptyList()
        }
    }
} 