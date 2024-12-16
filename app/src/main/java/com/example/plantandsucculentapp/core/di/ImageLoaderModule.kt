package com.example.plantandsucculentapp.core.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val imageLoaderModule = module {
    single {
        createImageLoader(androidContext())
    }
}

private fun createImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.25)
                .build()
        }
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(true)
        .allowHardware(true)
        .build()
} 