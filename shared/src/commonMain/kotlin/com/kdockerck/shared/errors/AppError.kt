package com.kdockerck.shared.errors

import arrow.core.Either

typealias AppResult<T> = Either<AppError, T>

sealed interface AppError {
    val message: String
    val cause: Throwable?
}

data class ParsingError(
    override val message: String,
    override val cause: Throwable? = null,
    val filePath: String? = null,
    val lineNumber: Int? = null
) : AppError

data class LLMError(
    override val message: String,
    override val cause: Throwable? = null,
    val operation: String? = null
) : AppError

data class DatabaseError(
    override val message: String,
    override val cause: Throwable? = null,
    val query: String? = null
) : AppError

data class ValidationError(
    override val message: String,
    override val cause: Throwable? = null,
    val field: String? = null
) : AppError

data class ConfigurationError(
    override val message: String,
    override val cause: Throwable? = null,
    val configKey: String? = null
) : AppError

data class NetworkError(
    override val message: String,
    override val cause: Throwable? = null,
    val url: String? = null
) : AppError