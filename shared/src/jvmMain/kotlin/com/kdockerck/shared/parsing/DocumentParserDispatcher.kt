package com.kdockerck.shared.parsing

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.domain.FileType
import com.kdockerck.shared.domain.ParsedContent
import com.kdockerck.shared.domain.toFileType
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.ParsingError
import com.kdockerck.shared.errors.ValidationError
import java.io.File

class DocumentParserDispatcher {
    private val wordParser = WordDocumentParser()
    private val excelParser = ExcelDocumentParser()
    private val visioParser = VisioDocumentParser()
    
    fun parse(filePath: String, documentId: String): Either<AppError, ParsedContent> {
        val file = File(filePath)
        
        if (!file.exists()) {
            return ParsingError(
                message = "File not found: $filePath",
                filePath = filePath
            ).left()
        }
        
        if (!file.canRead()) {
            return ParsingError(
                message = "Cannot read file: $filePath",
                filePath = filePath
            ).left()
        }
        
        val fileType = file.extension.toFileType()
        
        if (fileType == FileType.UNKNOWN) {
            return ValidationError(
                message = "Unsupported file type: ${file.extension}",
                field = "fileType"
            ).left()
        }
        
        return when (fileType) {
            FileType.DOCX -> wordParser.parse(filePath, documentId)
            FileType.XLSX -> excelParser.parse(filePath, documentId)
            FileType.VSDX -> visioParser.parse(filePath, documentId)
            FileType.UNKNOWN -> ValidationError(
                message = "Unsupported file type: ${file.extension}",
                field = "fileType"
            ).left()
        }
    }
    
    fun detectFileType(filePath: String): FileType {
        val file = File(filePath)
        return file.extension.toFileType()
    }
    
    fun isSupportedFileType(filePath: String): Boolean {
        return detectFileType(filePath) != FileType.UNKNOWN
    }
    
    fun getSupportedFileExtensions(): List<String> {
        return listOf(".docx", ".xlsx", ".vsdx")
    }
    
    fun getSupportedFileTypes(): List<FileType> {
        return listOf(FileType.DOCX, FileType.XLSX, FileType.VSDX)
    }
}