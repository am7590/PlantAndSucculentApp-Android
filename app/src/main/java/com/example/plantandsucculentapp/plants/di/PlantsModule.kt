package com.example.plantandsucculentapp.plants.di

import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.plants.data.PlantsRepositoryImpl
//import com.example.plantandsucculentapp.plants.grpcApi
import com.example.plantandsucculentapp.plants.domain.Repository
import com.example.plantandsucculentapp.plants.presentation.PlantsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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



val plantsModule = module {
    single { MockGrpcClient() }

    single<Repository>(qualifier = named("PlantsViewModelRepositoryImpl")) {
        PlantsRepositoryImpl(get())
    }

    viewModel {
        PlantsViewModel(get(named("PlantsViewModelRepositoryImpl")))
    }
}