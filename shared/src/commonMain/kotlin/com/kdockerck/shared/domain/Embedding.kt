package com.kdockerck.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class Embedding(
    val id: String,
    val documentId: String,
    val chunkId: String? = null,
    val vector: List<Float>,
    val text: String,
    val metadata: EmbeddingMetadata,
    val createdAt: kotlinx.datetime.Instant
)

@Serializable
data class EmbeddingMetadata(
    val chunkIndex: Int? = null,
    val totalChunks: Int? = null,
    val sourceType: String,
    val additionalInfo: Map<String, String> = emptyMap()
)

@Serializable
data class SimilarityResult(
    val embedding: Embedding,
    val score: Double,
    val rank: Int
)

@Serializable
data class EmbeddingConfig(
    val dimension: Int,
    val model: String,
    val chunkSize: Int = 512,
    val chunkOverlap: Int = 50
)