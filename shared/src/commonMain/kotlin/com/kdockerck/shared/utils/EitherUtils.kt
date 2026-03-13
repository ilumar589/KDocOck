package com.kdockerck.shared.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.AppResult
import kotlinx.coroutines.CancellationException

inline fun <T> catchAppError(block: () -> T): AppResult<T> = try {
    block().right()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    AppError(
        message = e.message ?: "Unknown error",
        cause = e
    ).left()
}

suspend inline fun <T> catchAppErrorSuspend(crossinline block: suspend () -> T): AppResult<T> = try {
    block().right()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    AppError(
        message = e.message ?: "Unknown error",
        cause = e
    ).left()
}

inline fun <T, E : AppError> Either<E, T>.getOrElse(default: T): T = when (this) {
    is Either.Right -> value
    is Either.Left -> default
}

inline fun <T, E : AppError> Either<E, T>.getOrThrow(): T = when (this) {
    is Either.Right -> value
    is Either.Left -> throw IllegalStateException(value.message, value.cause)
}

inline fun <T, E : AppError, R> Either<E, T>.mapError(transform: (E) -> E): Either<E, R> = when (this) {
    is Either.Right -> Either.Right(value)
    is Either.Left -> Either.Left(transform(value))
}

inline fun <T, E : AppError> Either<E, T>.onTapError(action: (E) -> Unit): Either<E, T> = also {
    if (it is Either.Left) {
        action(it.value)
    }
}

inline fun <T, E : AppError> Either<E, T>.onTapSuccess(action: (T) -> Unit): Either<E, T> = also {
    if (it is Either.Right) {
        action(it.value)
    }
}