package com.example.plantandsucculentapp.plants.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PlantEntity::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class PlantDatabase : RoomDatabase() {
    abstract val plantDao: PlantDao
} 