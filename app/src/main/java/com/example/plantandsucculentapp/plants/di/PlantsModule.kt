package com.example.plantandsucculentapp.plants.di

import android.content.Context
import androidx.room.Room
import com.example.plantandsucculentapp.BuildConfig
import com.example.plantandsucculentapp.core.network.GrpcClient
import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.plants.data.PlantHealthService
import com.example.plantandsucculentapp.plants.data.PlantsRepositoryImpl
import com.example.plantandsucculentapp.plants.data.local.PlantDatabase
import com.example.plantandsucculentapp.plants.data.local.PlantHealthHistoryManager
import com.example.plantandsucculentapp.plants.domain.Repository
import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val plantsModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            PlantDatabase::class.java,
            "plants.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    single { get<PlantDatabase>().plantDao() }

    single<GrpcClientInterface> {
        if (BuildConfig.USE_REAL_SERVER) {
            println("Using real gRPC client")
            GrpcClient()
        } else {
            println("Using mock gRPC client")
            MockGrpcClient()
        }
    }

    single { 
        PlantHealthService(
            context = androidContext(),
            imageLoader = get()
        ) 
    }

    single<Repository> {
        PlantsRepositoryImpl(
            grpcClient = get(),
            plantDao = get(),
            isMockEnabled = !BuildConfig.USE_REAL_SERVER,
            sharedPreferences = androidContext().getSharedPreferences(
                "shared_prefs",
                Context.MODE_PRIVATE
            ),
            healthService = get(),
            gson = get()
        )
    }

    single { PlantHealthHistoryManager(get()) }

    single { Gson() }

    viewModel { 
        PlantsViewModel(
            repository = get(),
            database = get()
        )
    }
}