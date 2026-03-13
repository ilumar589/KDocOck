package com.kdockerck.shared.rag

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.database.Embeddings
import com.kdockerck.shared.database.DatabaseConnectionManager
import com.kdockerck.shared.domain.Embedding as DomainEmbedding
import com.kdockerck.shared.domain.EmbeddingMetadata
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.DatabaseError
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class VectorStore(
    private val dbManager: DatabaseConnectionManager
) {
    
    suspend fun storeEmbedding(
        embedding: DomainEmbedding
    ): Either<AppError, Unit> {
        return dbManager.executeInTransaction { connection ->
            try {
                transaction(connection) {
                    Embeddings.insert {
                        it[id] = embedding.id
                        it[documentId] = embedding.documentId
                        it[chunkId] = embedding.chunkId
                        it[vector] = embedding.vector
                        it[text] = embedding.text
                        it[chunkIndex] = embedding.metadata.chunkIndex
                        it[totalChunks] = embedding.metadata.totalChunks
                        it[sourceType] = embedding.metadata.sourceType
                        it[createdAt] = java.time.LocalDateTime.now()
                    }
                }
                Unit
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to store embedding: ${e.message}",
                    cause = e,
                    query = "INSERT INTO embeddings"
                )
            }
        }
    }
    
    suspend fun storeEmbeddings(
        embeddings: List<DomainEmbedding>
    ): Either<AppError, Unit> {
        val errors = mutableListOf<AppError>()
        
        for (embedding in embeddings) {
            when (val result = storeEmbedding(embedding)) {
                is Either.Right -> {}
                is Either.Left -> errors.add(result.value)
            }
        }
        
        return if (errors.isNotEmpty()) {
            errors.first().left()
        } else {
            Unit.right()
        }
    }
    
    suspend fun storeEmbeddingsBatch(
        embeddings: List<DomainEmbedding>,
        batchSize: Int = 100
    ): Either<AppError, Unit> {
        val batches = embeddings.chunked(batchSize)
        val errors = mutableListOf<AppError>()
        
        for (batch in batches) {
            when (val result = storeEmbeddings(batch)) {
                is Either.Right -> {}
                is Either.Left -> errors.add(result.value)
            }
        }
        
        return if (errors.isNotEmpty()) {
            errors.first().left()
        } else {
            Unit.right()
        }
    }
    
    suspend fun getEmbeddingById(id: String): Either<AppError, DomainEmbedding?> {
        return dbManager.executeInTransaction { connection ->
            try {
                val result = transaction(connection) {
                    Embeddings.select { Embeddings.id eq id }
                        .map { row ->
                            rowToEmbedding(row)
                        }
                        .firstOrNull()
                }
                result.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to get embedding: ${e.message}",
                    cause = e,
                    query = "SELECT FROM embeddings WHERE id = ?"
                )
            }
        }
    }
    
    suspend fun getEmbeddingsByDocumentId(documentId: String): Either<AppError, List<DomainEmbedding>> {
        return dbManager.executeInTransaction { connection ->
            try {
                val results = transaction(connection) {
                    Embeddings.select { Embeddings.documentId eq documentId }
                        .map { row -> rowToEmbedding(row) }
                        .toList()
                }
                results.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to get embeddings: ${e.message}",
                    cause = e,
                    query = "SELECT FROM embeddings WHERE document_id = ?"
                )
            }
        }
    }
    
    suspend fun deleteEmbedding(id: String): Either<AppError, Unit> {
        return dbManager.executeInTransaction { connection ->
            try {
                transaction(connection) {
                    Embeddings.deleteWhere { Embeddings.id eq id }
                }
                Unit.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to delete embedding: ${e.message}",
                    cause = e,
                    query = "DELETE FROM embeddings WHERE id = ?"
                )
            }
        }
    }
    
    suspend fun deleteEmbeddingsByDocumentId(documentId: String): Either<AppError, Unit> {
        return dbManager.executeInTransaction { connection ->
            try {
                transaction(connection) {
                    Embeddings.deleteWhere { Embeddings.documentId eq documentId }
                }
                Unit.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to delete embeddings: ${e.message}",
                    cause = e,
                    query = "DELETE FROM embeddings WHERE document_id = ?"
                )
            }
        }
    }
    
    suspend fun countEmbeddings(): Either<AppError, Int> {
        return dbManager.executeInTransaction { connection ->
            try {
                val count = transaction(connection) {
                    Embeddings.selectAll().count().toInt()
                }
                count.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to count embeddings: ${e.message}",
                    cause = e,
                    query = "SELECT COUNT(*) FROM embeddings"
                )
            }
        }
    }
    
    suspend fun countEmbeddingsByDocumentId(documentId: String): Either<AppError, Int> {
        return dbManager.executeInTransaction { connection ->
            try {
                val count = transaction(connection) {
                    Embeddings.select { Embeddings.documentId eq documentId }
                        .count()
                        .toInt()
                }
                count.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to count embeddings: ${e.message}",
                    cause = e,
                    query = "SELECT COUNT(*) FROM embeddings WHERE document_id = ?"
                )
            }
        }
    }
    
    private fun rowToEmbedding(row: org.jetbrains.exposed.sql.ResultRow): DomainEmbedding {
        return DomainEmbedding(
            id = row[Embeddings.id],
            documentId = row[Embeddings.documentId],
            chunkId = row[Embeddings.chunkId],
            vector = row[Embeddings.vector],
            text = row[Embeddings.text],
            metadata = EmbeddingMetadata(
                chunkIndex = row[Embeddings.chunkIndex],
                totalChunks = row[Embeddings.totalChunks],
                sourceType = row[Embeddings.sourceType]
            ),
            createdAt = kotlinx.datetime.LocalDateTime.parse(row[Embeddings.createdAt].toString())
        )
    }
}