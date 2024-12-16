package com.example.plantandsucculentapp

import android.app.Application
import com.example.plantandsucculentapp.core.di.coreModule
import com.example.plantandsucculentapp.core.di.imageLoaderModule
import com.example.plantandsucculentapp.plants.di.plantsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PlantApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@PlantApplication)
            modules(listOf(
                coreModule,
                plantsModule,
                imageLoaderModule,
            ))
        }
    }
} 