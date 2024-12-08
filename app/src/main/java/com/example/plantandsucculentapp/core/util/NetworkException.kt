package com.example.plantandsucculentapp.core.util

sealed class NetworkException(message: String) : Exception(message) {
    data object NoConnection : NetworkException("No internet connection")
    data object ServerError : NetworkException("Server error occurred")
    data class ApiError(override val message: String) : NetworkException(message)
} 