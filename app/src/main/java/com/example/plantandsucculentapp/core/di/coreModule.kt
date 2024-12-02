package com.example.plantandsucculentapp.core.di

import android.content.Context
import com.example.plantandsucculentapp.core.data.CoreRepositoryImpl
import com.example.plantandsucculentapp.plants.domain.Repository
import com.example.plantandsucculentapp.core.network.MockGrpcClient
import com.example.plantandsucculentapp.core.presentation.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreModule = module {
    single {
        androidContext().getSharedPreferences(
            "shared_prefs",
            Context.MODE_PRIVATE
        )
    }

    single { MockGrpcClient() }

    single<Repository>(qualifier = named("CoreRepositoryImpl")) {
        CoreRepositoryImpl(get())
    }

    viewModel {
        MainViewModel(get(named("CoreRepositoryImpl")))
    }
}