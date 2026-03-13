package com.kdockerck.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String,
    val fileName: String,
    val fileType: FileType,
    val filePath: String,
    val fileSize: Long,
    val createdAt: kotlinx.datetime.Instant,
    val status: DocumentStatus = DocumentStatus.PENDING
)

@Serializable
enum class FileType {
    DOCX,
    XLSX,
    VSDX,
    UNKNOWN
}

@Serializable
enum class DocumentStatus {
    PENDING,
    PARSING,
    PARSED,
    EMBEDDING,
    EMBEDDED,
    GENERATING,
    COMPLETED,
    FAILED
}

fun String.toFileType(): FileType = when (lowercase()) {
    ".docx" -> FileType.DOCX
    ".xlsx" -> FileType.XLSX
    ".vsdx" -> FileType.VSDX
    else -> FileType.UNKNOWN
}