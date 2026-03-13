package com.kdockerck.shared.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val ollama: OllamaConfig,
    val database: DatabaseConfig,
    val embedding: EmbeddingConfig,
    val ui: UIConfig
)

@Serializable
data class OllamaConfig(
    val endpoint: String = "http://localhost:11434",
    val model: String = "llama3.2",
    val timeoutMillis: Long = 120000,
    val maxRetries: Int = 3
)

@Serializable
data class DatabaseConfig(
    val url: String = "r2dbc:postgresql://localhost:5432/kdockerck",
    val username: String = "kdockerck",
    val password: String = "kdockerck",
    val poolSize: Int = 10,
    val maxLifetimeMillis: Long = 1800000
)

@Serializable
data class EmbeddingConfig(
    val dimension: Int = 768,
    val model: String = "llama3.2",
    val chunkSize: Int = 512,
    val chunkOverlap: Int = 50,
    val similarityThreshold: Double = 0.7,
    val maxResults: Int = 5
)

@Serializable
data class UIConfig(
    val windowWidth: Int = 1280,
    val windowHeight: Int = 720,
    val logPanelVisible: Boolean = true,
    val outputDirectory: String? = null
)