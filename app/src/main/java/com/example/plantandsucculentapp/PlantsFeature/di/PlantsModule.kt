package com.example.plantandsucculentapp.PlantsFeature.di

import com.example.plantandsucculentapp.PlantsFeature.data.PlantsRepositoryImpl
import com.example.plantandsucculentapp.PlantsFeature.grpcApi
import com.example.plantandsucculentapp.PlantsFeature.domain.Repository
import com.example.plantandsucculentapp.PlantsFeature.presentation.PlantsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val plantsModule = module {
    single {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(grpcApi.BASE_URL)
            .build()
            .create(grpcApi::class.java)
    }

    single<Repository>(qualifier = named("PlantsViewModelRepositoryImpl")) {
        PlantsRepositoryImpl(get())
    }

    viewModel {
        PlantsViewModel(get(named("PlantsViewModelRepositoryImpl")))
    }
}
