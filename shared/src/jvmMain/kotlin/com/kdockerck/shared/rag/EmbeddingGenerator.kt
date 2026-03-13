package com.kdockerck.shared.rag

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.config.EmbeddingConfig
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.LLMError
import com.kdockerck.shared.llm.OllamaClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface EmbeddingGenerator {
    suspend fun generateEmbedding(text: String): Either<AppError, List<Float>>
    suspend fun generateEmbeddings(texts: List<String>): Either<AppError, List<List<Float>>>
    fun generateEmbeddingStream(text: String): Flow<Either<AppError, List<Float>>>
}

class OllamaEmbeddingGenerator(
    private val config: EmbeddingConfig,
    private val ollamaClient: OllamaClient
) : EmbeddingGenerator {
    
    override suspend fun generateEmbedding(text: String): Either<AppError, List<Float>> {
        return ollamaClient.generateEmbedding(text, config.model)
    }
    
    override suspend fun generateEmbeddings(texts: List<String>): Either<AppError, List<List<Float>>> {
        val results = mutableListOf<List<Float>>()
        val errors = mutableListOf<AppError>()
        
        for (text in texts) {
            when (val result = generateEmbedding(text)) {
                is Either.Right -> results.add(result.value)
                is Either.Left -> errors.add(result.value)
            }
        }
        
        return if (errors.isNotEmpty()) {
            errors.first().left()
        } else {
            results.right()
        }
    }
    
    override fun generateEmbeddingStream(text: String): Flow<Either<AppError, List<Float>>> = flow {
        emit(generateEmbedding(text))
    }
}

class MockEmbeddingGenerator(
    private val config: EmbeddingConfig
) : EmbeddingGenerator {
    
    override suspend fun generateEmbedding(text: String): Either<AppError, List<Float>> {
        return try {
            val hash = text.hashCode()
            val vector = (1..config.dimension).map { index ->
                ((hash + index) % 1000) / 1000.0f
            }
            vector.right()
        } catch (e: Exception) {
            LLMError(
                message = "Failed to generate mock embedding: ${e.message}",
                cause = e,
                operation = "generateEmbedding"
            ).left()
        }
    }
    
    override suspend fun generateEmbeddings(texts: List<String>): Either<AppError, List<List<Float>>> {
        return try {
            val vectors = texts.map { text ->
                val hash = text.hashCode()
                (1..config.dimension).map { index ->
                    ((hash + index) % 1000) / 1000.0f
                }
            }
            vectors.right()
        } catch (e: Exception) {
            LLMError(
                message = "Failed to generate mock embeddings: ${e.message}",
                cause = e,
                operation = "generateEmbeddings"
            ).left()
        }
    }
    
    override fun generateEmbeddingStream(text: String): Flow<Either<AppError, List<Float>>> = flow {
        emit(generateEmbedding(text))
    }
}



class TextChunker(
    private val config: EmbeddingConfig
) {
    
    fun chunkText(text: String): List<String> {
        if (text.length <= config.chunkSize) {
            return listOf(text)
        }
        
        val chunks = mutableListOf<String>()
        var currentIndex = 0
        
        while (currentIndex < text.length) {
            val endIndex = minOf(currentIndex + config.chunkSize, text.length)
            var chunkEnd = endIndex
            
            if (endIndex < text.length) {
                val lastSpace = text.lastIndexOf(' ', endIndex)
                val lastNewline = text.lastIndexOf('\n', endIndex)
                val lastPeriod = text.lastIndexOf('.', endIndex)
                
                val lastBreak = maxOf(lastSpace, lastNewline, lastPeriod)
                if (lastBreak > currentIndex) {
                    chunkEnd = lastBreak + 1
                }
            }
            
            chunks.add(text.substring(currentIndex, chunkEnd).trim())
            currentIndex = chunkEnd - config.chunkOverlap
        }
        
        return chunks.filter { it.isNotBlank() }
    }
    
    fun chunkTextWithMetadata(text: String): List<ChunkMetadata> {
        val chunks = chunkText(text)
        return chunks.mapIndexed { index, chunk ->
            ChunkMetadata(
                text = chunk,
                index = index,
                totalChunks = chunks.size,
                startIndex = if (index == 0) 0 else chunks[index - 1].length - config.chunkOverlap,
                endIndex = chunks[index].length
            )
        }
    }
}

data class ChunkMetadata(
    val text: String,
    val index: Int,
    val totalChunks: Int,
    val startIndex: Int,
    val endIndex: Int
)