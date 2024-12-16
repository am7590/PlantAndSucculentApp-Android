package com.example.plantandsucculentapp.plants.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlantEntity::class, PhotoEntity::class],
    version = 2
)
@TypeConverters(Converters::class)
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE plants ADD COLUMN lastIdentificationResult TEXT"
                )

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS plant_photos (
                        id TEXT PRIMARY KEY NOT NULL,
                        plantSku TEXT NOT NULL,
                        url TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        FOREIGN KEY(plantSku) REFERENCES plants(sku) ON DELETE CASCADE
                    )
                """)

                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_plant_photos_plantSku ON plant_photos(plantSku)"
                )
            }
        }

        fun create(context: Context): PlantDatabase {
            return Room.databaseBuilder(
                context,
                PlantDatabase::class.java,
                "plant_database"
            )
            .addMigrations(MIGRATION_1_2)
            .build()
        }
    }
} 