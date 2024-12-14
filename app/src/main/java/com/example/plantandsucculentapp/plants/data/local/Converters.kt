package com.example.plantandsucculentapp.plants.data.local

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
        val listType = object : TypeToken<List<PhotoEntity>>() {}.type
        return Gson().fromJson(value, listType)
    }
} 