package com.kdockerck.shared.utils

import arrow.core.Either
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.AppResult
import kotlinx.coroutines.CancellationException

interface Raise<in E> {
    fun raise(error: E): Nothing
}

inline fun <E : AppError, T> raiseCatch(block: Raise<E>.() -> T): AppResult<T> = try {
    Either.Right(block(object : Raise<E> {
        override fun raise(error: E): Nothing {
            throw RaiseException(error)
        }
    }))
} catch (e: RaiseException) {
    Either.Left(e.error)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Either.Left(
        AppError(
            message = e.message ?: "Unknown error",
            cause = e
        ) as E
    )
}

suspend inline fun <E : AppError, T> raiseCatchSuspend(crossinline block: suspend Raise<E>.() -> T): AppResult<T> = try {
    Either.Right(block(object : Raise<E> {
        override fun raise(error: E): Nothing {
            throw RaiseException(error)
        }
    }))
} catch (e: RaiseException) {
    Either.Left(e.error)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Either.Left(
        AppError(
            message = e.message ?: "Unknown error",
            cause = e
        ) as E
    )
}

private class RaiseException(val error: AppError) : Exception()