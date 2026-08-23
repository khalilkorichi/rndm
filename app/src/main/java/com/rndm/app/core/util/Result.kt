package com.rndm.app.core.util

sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(val exception: Throwable? = null, val message: String = "") : Result<Nothing>
    data object Loading : Result<Nothing>
}
