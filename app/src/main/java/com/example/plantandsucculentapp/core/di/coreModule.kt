package com.example.plantandsucculentapp.core.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single {
        androidContext().getSharedPreferences(
            "shared_prefs",
            Context.MODE_PRIVATE
        )
    }
}