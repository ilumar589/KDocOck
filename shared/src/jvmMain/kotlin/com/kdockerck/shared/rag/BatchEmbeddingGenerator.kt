package com.kdockerck.shared.rag

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.Validated
import arrow.core.Invalid
import arrow.core.Valid
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.utils.parallelMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class BatchEmbeddingGenerator(
    private val embeddingGenerator: EmbeddingGenerator,
    private val textChunker: TextChunker
) {
    
    suspend fun generateBatchEmbeddings(
        text: String,
        documentId: String,
        chunkId: String? = null
    ): Either<AppError, List<EmbeddingResult>> {
        val chunks = textChunker.chunkTextWithMetadata(text)
        
        return generateBatchEmbeddings(
            chunks = chunks.map { it.text },
            documentId = documentId,
            chunkId = chunkId
        ).map { embeddings ->
            embeddings.mapIndexed { index, embedding ->
                EmbeddingResult(
                    text = chunks[index].text,
                    embedding = embedding,
                    chunkIndex = chunks[index].index,
                    totalChunks = chunks[index].totalChunks
                )
            }
        }
    }
    
    suspend fun generateBatchEmbeddings(
        chunks: List<String>,
        documentId: String,
        chunkId: String? = null
    ): Either<AppError, List<List<Float>>> {
        return withContext(Dispatchers.Default) {
            chunks.parallelMap { chunk ->
                embeddingGenerator.generateEmbedding(chunk)
            }.let { results ->
                val errors = results.filterIsInstance<Either.Left<AppError>>()
                if (errors.isNotEmpty()) {
                    errors.first().value.left()
                } else {
                    results.filterIsInstance<Either.Right<List<Float>>>()
                        .map { it.value }
                        .right()
                }
            }
        }
    }
    
    suspend fun generateBatchEmbeddingsValidated(
        chunks: List<String>,
        documentId: String,
        chunkId: String? = null
    ): Validated<List<AppError>, List<EmbeddingResult>> {
        val results = coroutineScope {
            chunks.parallelMap { chunk ->
                Pair(chunk, embeddingGenerator.generateEmbedding(chunk))
            }
        }
        
        val errors = mutableListOf<AppError>()
        val embeddings = mutableListOf<EmbeddingResult>()
        
        results.forEachIndexed { index, (text, result) ->
            when (result) {
                is Either.Right -> {
                    embeddings.add(
                        EmbeddingResult(
                            text = text,
                            embedding = result.value,
                            chunkIndex = index,
                            totalChunks = chunks.size
                        )
                    )
                }
                is Either.Left -> {
                    errors.add(result.value)
                }
            }
        }
        
        return if (errors.isNotEmpty()) {
            invalid(errors)
        } else {
            valid(embeddings)
        }
    }
    
    suspend fun generateBatchEmbeddingsParallel(
        texts: List<String>,
        concurrency: Int = 4
    ): Either<AppError, List<List<Float>>> {
        return withContext(Dispatchers.Default) {
            val chunks = texts.chunked(concurrency)
            val allEmbeddings = mutableListOf<List<Float>>()
            
            for (chunk in chunks) {
                val results = chunk.parallelMap { text ->
                    embeddingGenerator.generateEmbedding(text)
                }
                
                val errors = results.filterIsInstance<Either.Left<AppError>>()
                if (errors.isNotEmpty()) {
                    return@withContext errors.first().value.left()
                }
                
                allEmbeddings.addAll(
                    results.filterIsInstance<Either.Right<List<Float>>>()
                        .map { it.value }
                )
            }
            
            allEmbeddings.right()
        }
    }
    
    suspend fun generateEmbeddingsWithRetry(
        texts: List<String>,
        maxRetries: Int = 3
    ): Either<AppError, List<List<Float>>> {
        var attempt = 0
        var lastError: AppError? = null
        
        while (attempt < maxRetries) {
            val result = generateBatchEmbeddings(texts, "", null)
            
            if (result.isRight()) {
                return result
            }
            
            lastError = (result as Either.Left).value
            attempt++
            
            if (attempt < maxRetries) {
                kotlinx.coroutines.delay(1000L * attempt)
            }
        }
        
        return lastError?.left() ?: AppError(
            message = "Max retries exceeded"
        ).left()
    }
}

data class EmbeddingResult(
    val text: String,
    val embedding: List<Float>,
    val chunkIndex: Int,
    val totalChunks: Int
)