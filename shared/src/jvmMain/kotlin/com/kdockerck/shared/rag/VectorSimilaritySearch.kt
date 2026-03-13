package com.kdockerck.shared.rag

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.database.Embeddings
import com.kdockerck.shared.database.DatabaseConnectionManager
import com.kdockerck.shared.domain.SimilarityResult
import com.kdockerck.shared.domain.Embedding as DomainEmbedding
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.DatabaseError
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.sqrt

class VectorSimilaritySearch(
    private val dbManager: DatabaseConnectionManager,
    private val config: com.kdockerck.shared.config.EmbeddingConfig
) {
    
    suspend fun searchSimilar(
        queryVector: List<Float>,
        limit: Int = config.maxResults,
        threshold: Double = config.similarityThreshold
    ): Either<AppError, List<SimilarityResult>> {
        return dbManager.executeInTransaction { connection ->
            try {
                val results = transaction(connection) {
                    Embeddings.selectAll()
                        .map { row ->
                            val embedding = rowToEmbedding(row)
                            val score = cosineSimilarity(queryVector, embedding.vector)
                            Pair(embedding, score)
                        }
                        .toList()
                        .filter { it.second >= threshold }
                        .sortedByDescending { it.second }
                        .take(limit)
                        .mapIndexed { index, (embedding, score) ->
                            SimilarityResult(
                                embedding = embedding,
                                score = score,
                                rank = index + 1
                            )
                        }
                }
                results.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to search similar vectors: ${e.message}",
                    cause = e,
                    query = "SELECT FROM embeddings ORDER BY similarity"
                )
            }
        }
    }
    
    suspend fun searchSimilarByDocumentId(
        queryVector: List<Float>,
        documentId: String,
        limit: Int = config.maxResults,
        threshold: Double = config.similarityThreshold
    ): Either<AppError, List<SimilarityResult>> {
        return dbManager.executeInTransaction { connection ->
            try {
                val results = transaction(connection) {
                    Embeddings.select { Embeddings.documentId eq documentId }
                        .map { row ->
                            val embedding = rowToEmbedding(row)
                            val score = cosineSimilarity(queryVector, embedding.vector)
                            Pair(embedding, score)
                        }
                        .toList()
                        .filter { it.second >= threshold }
                        .sortedByDescending { it.second }
                        .take(limit)
                        .mapIndexed { index, (embedding, score) ->
                            SimilarityResult(
                                embedding = embedding,
                                score = score,
                                rank = index + 1
                            )
                        }
                }
                results.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to search similar vectors: ${e.message}",
                    cause = e,
                    query = "SELECT FROM embeddings WHERE document_id = ? ORDER BY similarity"
                )
            }
        }
    }
    
    suspend fun searchSimilarBySourceType(
        queryVector: List<Float>,
        sourceType: String,
        limit: Int = config.maxResults,
        threshold: Double = config.similarityThreshold
    ): Either<AppError, List<SimilarityResult>> {
        return dbManager.executeInTransaction { connection ->
            try {
                val results = transaction(connection) {
                    Embeddings.select { Embeddings.sourceType eq sourceType }
                        .map { row ->
                            val embedding = rowToEmbedding(row)
                            val score = cosineSimilarity(queryVector, embedding.vector)
                            Pair(embedding, score)
                        }
                        .toList()
                        .filter { it.second >= threshold }
                        .sortedByDescending { it.second }
                        .take(limit)
                        .mapIndexed { index, (embedding, score) ->
                            SimilarityResult(
                                embedding = embedding,
                                score = score,
                                rank = index + 1
                            )
                        }
                }
                results.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to search similar vectors: ${e.message}",
                    cause = e,
                    query = "SELECT FROM embeddings WHERE source_type = ? ORDER BY similarity"
                )
            }
        }
    }
    
    suspend fun searchSimilarPgVector(
        queryVector: List<Float>,
        limit: Int = config.maxResults,
        threshold: Double = config.similarityThreshold
    ): Either<AppError, List<SimilarityResult>> {
        return dbManager.executeInTransaction { connection ->
            try {
                val vectorStr = queryVector.joinToString(",", "[", "]")
                val sql = """
                    SELECT id, document_id, chunk_id, vector, text, chunk_index, total_chunks, source_type, created_at,
                           1 - (vector <=> '$vectorStr'::vector) as similarity
                    FROM embeddings
                    WHERE 1 - (vector <=> '$vectorStr'::vector) >= $threshold
                    ORDER BY similarity DESC
                    LIMIT $limit
                """.trimIndent()
                
                val statement = connection.createStatement(sql)
                val result = statement.execute()
                
                val similarityResults = result.map { row ->
                    val embedding = rowToEmbedding(row)
                    val score = row.get("similarity") as Double
                    SimilarityResult(
                        embedding = embedding,
                        score = score,
                        rank = 0
                    )
                }.toList().mapIndexed { index, it ->
                    it.copy(rank = index + 1)
                }
                
                similarityResults.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to search similar vectors with pgvector: ${e.message}",
                    cause = e,
                    query = "SELECT FROM embeddings ORDER BY vector <=> query"
                )
            }
        }
    }
    
    private fun cosineSimilarity(vector1: List<Float>, vector2: List<Float>): Double {
        if (vector1.size != vector2.size) {
            return 0.0
        }
        
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        
        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            norm1 += vector1[i] * vector1[i]
            norm2 += vector2[i] * vector2[i]
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0
        }
        
        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }
    
    private fun rowToEmbedding(row: org.jetbrains.exposed.sql.ResultRow): DomainEmbedding {
        return DomainEmbedding(
            id = row[Embeddings.id],
            documentId = row[Embeddings.documentId],
            chunkId = row[Embeddings.chunkId],
            vector = row[Embeddings.vector],
            text = row[Embeddings.text],
            metadata = com.kdockerck.shared.domain.EmbeddingMetadata(
                chunkIndex = row[Embeddings.chunkIndex],
                totalChunks = row[Embeddings.totalChunks],
                sourceType = row[Embeddings.sourceType]
            ),
            createdAt = kotlinx.datetime.LocalDateTime.parse(row[Embeddings.createdAt].toString())
        )
    }
}