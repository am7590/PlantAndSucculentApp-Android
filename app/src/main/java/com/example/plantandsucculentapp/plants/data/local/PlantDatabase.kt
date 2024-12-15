package com.example.plantandsucculentapp.plants.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlantEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PlantDatabase : RoomDatabase() {
    abstract val plantDao: PlantDao

    companion object {
        fun create(context: Context): PlantDatabase {
            return Room.databaseBuilder(
                context,
                PlantDatabase::class.java,
                "plants.db"
            )
            .addMigrations(MIGRATION_2_3)
            .build()
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add healthCheckHistory column with default empty list
                database.execSQL(
                    "ALTER TABLE plants ADD COLUMN healthCheckHistory TEXT DEFAULT '[]' NOT NULL"
                )
            }
        }
    }
} 