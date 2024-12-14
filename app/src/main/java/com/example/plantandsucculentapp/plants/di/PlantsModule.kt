package com.example.plantandsucculentapp.plants.di

//import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import android.content.Context
import com.example.plantandsucculentapp.core.network.GrpcClient
import com.example.plantandsucculentapp.core.network.GrpcClientInterface
import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.plants.data.PlantsRepositoryImpl
import com.example.plantandsucculentapp.plants.domain.Repository
import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.plantandsucculentapp.BuildConfig
import androidx.room.Room
import com.example.plantandsucculentapp.plants.data.local.PlantDatabase
import org.checkerframework.checker.units.qual.s
import org.koin.android.ext.koin.androidContext
import com.example.plantandsucculentapp.plants.data.PlantHealthService

//
//val plantsModule = module {
//    single {
//        Retrofit.Builder()
//            .addConverterFactory(GsonConverterFactory.create())
//            .baseUrl(grpcApi.BASE_URL)
//            .build()
//            .create(grpcApi::class.java)
//    }
//
//    single<Repository>(qualifier = named("PlantsViewModelRepositoryImpl")) {
//        PlantsRepositoryImpl(get())
//    }
//
//    viewModel {
//        PlantsViewModel(get(named("PlantsViewModelRepositoryImpl")))
//    }
//}



//val plantsModule = module {
//    single { MockGrpcClient() }
//
//    single<Repository>(qualifier = named("PlantsViewModelRepositoryImpl")) {
//        PlantsRepositoryImpl(get())
//    }
//
//    viewModel {
//        PlantsViewModel(get(named("PlantsViewModelRepositoryImpl")))
//    }
//}

val plantsModule = module {
    single {
        Room.databaseBuilder(
            get(),
            PlantDatabase::class.java,
            "plant_db"
        ).build()
    }

    single { get<PlantDatabase>().plantDao }

    single<GrpcClientInterface> {
        if (BuildConfig.USE_REAL_SERVER) {
            println("Using real gRPC client")
            GrpcClient()
        } else {
            println("Using mock gRPC client")
            MockGrpcClient()
        }
    }

    single { PlantHealthService() }

    single<Repository> {
        PlantsRepositoryImpl(
            grpcClient = get(),
            plantDao = get(),
            isMockEnabled = !BuildConfig.USE_REAL_SERVER,
            sharedPreferences = androidContext().getSharedPreferences(
                "shared_prefs",
                Context.MODE_PRIVATE
            ),
            healthService = get()
        )
    }

    viewModel {
        PlantsViewModel(get())
    }
}