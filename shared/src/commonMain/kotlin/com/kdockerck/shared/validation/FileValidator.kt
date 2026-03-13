package com.kdockerck.shared.validation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.Validated
import com.kdockerck.shared.domain.FileType
import com.kdockerck.shared.domain.toFileType
import com.kdockerck.shared.errors.ValidationError
import java.io.File

class FileValidator {
    fun validateFilePath(filePath: String): Either<ValidationError, String> {
        if (filePath.isBlank()) {
            return ValidationError(
                message = "File path cannot be blank",
                field = "filePath"
            ).left()
        }
        
        return filePath.right()
    }
    
    fun validateFileExists(filePath: String): Either<ValidationError, File> {
        val file = File(filePath)
        
        if (!file.exists()) {
            return ValidationError(
                message = "File does not exist: $filePath",
                field = "filePath"
            ).left()
        }
        
        return file.right()
    }
    
    fun validateFileReadable(file: File): Either<ValidationError, File> {
        if (!file.canRead()) {
            return ValidationError(
                message = "File is not readable: ${file.absolutePath}",
                field = "filePath"
            ).left()
        }
        
        return file.right()
    }
    
    fun validateFileSize(file: File, maxSizeBytes: Long = 100 * 1024 * 1024): Either<ValidationError, File> {
        val fileSize = file.length()
        
        if (fileSize > maxSizeBytes) {
            return ValidationError(
                message = "File size exceeds maximum allowed size: ${fileSize} bytes (max: $maxSizeBytes bytes)",
                field = "fileSize"
            ).left()
        }
        
        return file.right()
    }
    
    fun validateFileType(filePath: String, supportedTypes: List<FileType>): Either<ValidationError, FileType> {
        val file = File(filePath)
        val extension = file.extension.lowercase()
        val fileType = extension.toFileType()
        
        if (fileType == FileType.UNKNOWN) {
            return ValidationError(
                message = "Unsupported file type: .$extension",
                field = "fileType"
            ).left()
        }
        
        if (fileType !in supportedTypes) {
            return ValidationError(
                message = "File type not supported: $fileType",
                field = "fileType"
            ).left()
        }
        
        return fileType.right()
    }
    
    fun validateFileContent(filePath: String): Either<ValidationError, Boolean> {
        val file = File(filePath)
        
        if (file.length() == 0L) {
            return ValidationError(
                message = "File is empty: $filePath",
                field = "fileContent"
            ).left()
        }
        
        return true.right()
    }
    
    fun validateAll(
        filePath: String,
        supportedTypes: List<FileType>,
        maxSizeBytes: Long = 100 * 1024 * 1024
    ): Validated<ValidationError, File> {
        val validatedPath = validateFilePath(filePath).toValidated()
        val validatedExists = validatedPath.flatMap { validateFileExists(it).toValidated() }
        val validatedReadable = validatedExists.flatMap { validateFileReadable(it).toValidated() }
        val validatedSize = validatedReadable.flatMap { validateFileSize(it, maxSizeBytes).toValidated() }
        val validatedType = validatedSize.flatMap { validateFileType(filePath, supportedTypes).toValidated().map { it } }
        val validatedContent = validatedType.flatMap { validateFileContent(filePath).toValidated().map { it } }
        
        return validatedContent.map { validatedSize.valueOrNull()!! }
    }
    
    private fun <E, A> Either<E, A>.toValidated(): Validated<E, A> = when (this) {
        is Either.Right -> Validated.Valid(value)
        is Either.Left -> Validated.Invalid(value)
    }
}