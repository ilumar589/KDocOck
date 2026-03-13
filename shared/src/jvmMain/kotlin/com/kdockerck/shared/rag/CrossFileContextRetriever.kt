package com.kdockerck.shared.rag

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.domain.ParsedContent
import com.kdockerck.shared.domain.SimilarityResult
import com.kdockerck.shared.errors.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CrossFileContextRetriever(
    private val embeddingGenerator: EmbeddingGenerator,
    private val vectorSimilaritySearch: VectorSimilaritySearch,
    private val config: com.kdockerck.shared.config.EmbeddingConfig
) {
    
    suspend fun retrieveContext(
        currentDocument: ParsedContent,
        excludeDocumentId: String? = null
    ): Either<AppError, List<SimilarityResult>> {
        val documentText = extractTextFromDocument(currentDocument)
        val embeddingResult = embeddingGenerator.generateEmbedding(documentText)
        
        return embeddingResult.flatMap { queryVector ->
            val searchResults = vectorSimilaritySearch.searchSimilar(query)
            
            searchResults.map { results ->
                results.filter { it.score >= config.similarityThreshold }
                    .sortedByDescending { it.score }
                    .take(config.maxResults)
            }
        }
    }
    
    suspend fun retrieveContextStream(
        currentDocument: ParsedContent,
        excludeDocumentId: String? = null
    ): Flow<Either<AppError, SimilarityResult>> = flow {
        val documentText = extractTextFromDocument(currentDocument)
        
        when (val embeddingResult = embeddingGenerator.generateEmbedding(documentText)) {
            is Either.Right -> {
                val queryVector = embeddingResult.value
            val searchResults = vectorSimilaritySearch.searchSimilar(queryVector)
                
                when (searchResults) {
                    is Either.Right -> {
                        searchResults.value
                            .filter { it.score >= config.similarityThreshold }
                            .sortedByDescending { it.score }
                            .take(config.maxResults)
                            .forEach { result ->
                                emit(Either.Right(result))
                            }
                    }
                    is Either.Left -> {
                        emit(Either.Left(searchResults.value))
                    }
                }
            }
            is Either.Left -> {
                emit(Either.Left(embeddingResult.value))
            }
        }
    }
    
    suspend fun retrieveContextForMultipleDocuments(
        documents: List<ParsedContent>,
        excludeDocumentIds: Set<String> = emptySet()
    ): Either<AppError, Map<String, List<SimilarityResult>>> {
        val contextMap = mutableMapOf<String, List<SimilarityResult>>()
        val errors = mutableListOf<AppError>()
        
        for (document in documents) {
            val excludeId = if (document.documentId in excludeDocumentIds) {
                document.documentId
            } else {
                null
            }
            
            when (val result = retrieveContext(document, excludeId)) {
                is Either.Right -> {
                    contextMap[document.documentId] = result.value
                }
                is Either.Left -> {
                    errors.add(result.value)
                }
            }
        }
        
        return if (errors.isNotEmpty()) {
            errors.first().left()
        } else {
            contextMap.right()
        }
    }
    
    suspend fun retrieveRelevantContext(
        query: String,
        maxResults: Int = config.maxResults,
        threshold: Double = config.similarityThreshold
    ): Either<AppError, List<SimilarityResult>> {
        val embeddingResult = embeddingGenerator.generateEmbedding(query)
        
        return embeddingResult.flatMap { queryVector ->
            vectorSimilaritySearch.searchSimilar(queryVector, maxResults, threshold)
        }
    }
    
    suspend fun retrieveContextWithMetadata(
        currentDocument: ParsedContent,
        excludeDocumentId: String? = null,
        includeMetadata: Boolean = true
    ): Either<AppError, ContextWithMetadata> {
        val documentText = extractTextFromDocument(currentDocument)
        val embeddingResult = embeddingGenerator.generateEmbedding(documentText)
        
        return embeddingResult.flatMap { queryVector ->
            val searchResults = vectorSimilaritySearch.searchSimilar(queryVector)
            
            searchResults.map { results ->
                val filteredResults = results
                    .filter { 
                        it.score >= config.similarityThreshold &&
                        (excludeDocumentId == null || it.embedding.documentId != excludeDocumentId)
                    }
                    .sortedByDescending { it.score }
                    .take(config.maxResults)
                
                val contextText = filteredResults
                    .joinToString("\n\n") { 
                        "--- Document: ${it.embedding.documentId} (Score: ${it.score}) ---\n${it.embedding.text}"
                    }
                
                ContextWithMetadata(
                    contextText = contextText,
                    similarityResults = filteredResults,
                    queryDocumentId = currentDocument.documentId,
                    retrievedAt = kotlinx.datetime.Clock.System.now()
                )
            }
        }
                    .sortedByDescending { it.score }
                    .take(config.maxResults)
            }
        }
            } else {
                vectorSimilaritySearch.searchSimilar(queryVector)
            }
            
            searchResults.map { results ->
                val filteredResults = results
                    .filter { it.score >= config.similarityThreshold }
                    .sortedByDescending { it.score }
                    .take(config.maxResults)
                
                val contextText = filteredResults
                    .joinToString("\n\n") { 
                        "--- Document: ${it.embedding.documentId} (Score: ${it.score}) ---\n${it.embedding.text}"
                    }
                
                ContextWithMetadata(
                    contextText = contextText,
                    similarityResults = filteredResults,
                    queryDocumentId = currentDocument.documentId,
                    retrievedAt = kotlinx.datetime.Clock.System.now()
                )
            }
        }
    }
    
    suspend fun retrieveContextSummary(
        currentDocument: ParsedContent,
        excludeDocumentId: String? = null
    ): Either<AppError, String> {
        return retrieveContext(currentDocument, excludeDocumentId).map { results ->
            buildString {
                appendLine("Cross-file context retrieved:")
                appendLine("Number of relevant documents: ${results.size}")
                appendLine()
                
                results.forEach { result ->
                    appendLine("Document: ${result.embedding.documentId}")
                    appendLine("Similarity Score: ${"%.2f".format(result.score)}")
                    appendLine("Content Preview: ${result.embedding.text.take(200)}...")
                    appendLine()
                }
            }
        }
    }
    
    private fun extractTextFromDocument(document: ParsedContent): String {
        return when (document) {
            is com.kdockerck.shared.domain.WordDocumentContent -> {
                document.paragraphs.joinToString("\n") { it.text } +
                document.tables.joinToString("\n") { table ->
                    table.rows.joinToString("\n") { row ->
                        row.cells.joinToString(" | ") { it.text }
                    }
                }
            }
            is com.kdockerck.shared.domain.ExcelDocumentContent -> {
                document.worksheets.joinToString("\n") { worksheet ->
                    "Worksheet: ${worksheet.name}\n" +
                    worksheet.cells.joinToString("\n") { cell ->
                        "${cell.reference}: ${cell.value}"
                    }
                }
            }
            is com.kdockerck.shared.domain.VisioDocumentContent -> {
                document.pages.joinToString("\n") { page ->
                    "Page: ${page.name}\n" +
                    page.shapes.joinToString("\n") { shape ->
                        "Shape: ${shape.text}"
                    }
                }
            }
        }
    }
}

data class ContextWithMetadata(
    val contextText: String,
    val similarityResults: List<SimilarityResult>,
    val queryDocumentId: String,
    val retrievedAt: kotlinx.datetime.Instant
)