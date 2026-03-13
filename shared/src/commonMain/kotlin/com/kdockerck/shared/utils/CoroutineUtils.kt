package com.kdockerck.shared.utils

import arrow.core.Either
import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

suspend fun <T, R> Iterable<T>.parallelMap(
    context: CoroutineContext = Dispatchers.Default,
    transform: suspend (T) -> R
): List<R> = withContext(context) {
    map { async { transform(it) } }.awaitAll()
}

suspend fun <T, E : AppError, R> Iterable<T>.parTraverse(
    context: CoroutineContext = Dispatchers.Default,
    transform: suspend (T) -> AppResult<R>
): AppResult<List<R>> = coroutineScope {
    val results = map { async { transform(it) } }.awaitAll()
    
    val errors = results.filterIsInstance<Either.Left<E>>()
    if (errors.isNotEmpty()) {
        Either.Left(errors.first().value)
    } else {
        Either.Right(results.filterIsInstance<Either.Right<R>>().map { it.value })
    }
}

suspend fun <T, E : AppError> Iterable<T>.parTraverseValidated(
    context: CoroutineContext = Dispatchers.Default,
    transform: suspend (T) -> AppResult<T>
): Validated<List<E>, List<T>> = coroutineScope {
    val results = map { async { transform(it) } }.awaitAll()
    
    val errors = results.mapNotNull {
        when (it) {
            is Either.Left -> it.value
            is Either.Right -> null
        }
    }
    
    if (errors.isNotEmpty()) {
        Validated.Invalid(errors)
    } else {
        Validated.Valid(results.map { (it as Either.Right<T>).value })
    }
}

class Resource<T : AutoCloseable> private constructor(
    private val acquire: suspend () -> T,
    private val release: suspend (T) -> Unit
) {
    suspend fun <R> use(block: suspend (T) -> R): R {
        val resource = acquire()
        try {
            return block(resource)
        } finally {
            release(resource)
        }
    }
    
    companion object {
        fun <T : AutoCloseable> from(
            acquire: suspend () -> T,
            release: suspend (T) -> Unit = { it.close() }
        ): Resource<T> = Resource(acquire, release)
        
        fun <T : AutoCloseable> of(acquire: suspend () -> T): Resource<T> =
            from(acquire) { it.close() }
    }
}

suspend fun <T : AutoCloseable, R> withResource(
    resource: Resource<T>,
    block: suspend (T) -> R
): R = resource.use(block)