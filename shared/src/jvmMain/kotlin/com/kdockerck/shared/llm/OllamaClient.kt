package com.kdockerck.shared.llm

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.LLMError
import com.kdockerck.shared.errors.NetworkError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

data class OllamaConfig(
    val endpoint: String = "http://localhost:11434",
    val model: String = "llama3.2",
    val timeoutMillis: Long = 120000,
    val maxRetries: Int = 3
)

@Serializable
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val options: OllamaOptions? = null
)

@Serializable
data class OllamaMessage(
    val role: String,
    val content: String
)

@Serializable
data class OllamaOptions(
    val temperature: Double? = null,
    val top_p: Double? = null,
    val max_tokens: Int? = null
)

@Serializable
data class OllamaChatResponse(
    val model: String,
    val message: OllamaMessage,
    val done: Boolean,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Int? = null,
    val eval_count: Int? = null
)

@Serializable
data class OllamaChatStreamChunk(
    val model: String,
    val message: OllamaMessage? = null,
    val done: Boolean = false,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Int? = null,
    val eval_count: Int? = null
)

@Serializable
data class OllamaEmbeddingRequest(
    val model: String,
    val prompt: String
)

@Serializable
data class OllamaEmbeddingResponse(
    val embedding: List<Float>
)

@Serializable
data class OllamaModelInfo(
    val name: String,
    val modified_at: String,
    val size: Long,
    val digest: String
)

@Serializable
data class OllamaModelsResponse(
    val models: List<OllamaModelInfo>
)

class OllamaClient(
    private val config: OllamaConfig
) {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMillis
            connectTimeoutMillis = 10000
            socketTimeoutMillis = config.timeoutMillis
        }
        install(Retry) {
            maxRetries = config.maxRetries
            retryOnExceptionOrServerErrors(maxRetries = config.maxRetries)
            exponentialDelay()
        }
        install(ContentNegotiation) {
            accept(ContentType.Application.Json)
        }
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    suspend fun chat(
        messages: List<OllamaMessage>,
        options: OllamaOptions? = null
    ): Either<AppError, OllamaChatResponse> {
        return try {
            val request = OllamaChatRequest(
                model = config.model,
                messages = messages,
                stream = false,
                options = options
            )
            
            val response = client.post("${config.endpoint}/api/chat") {
                setBody(ContentType.Application.Json, json.encodeToString(request))
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val chatResponse = json.decodeFromString<OllamaChatResponse>(responseBody)
                chatResponse.right()
            } else {
                val errorBody = response.bodyAsText()
                NetworkError(
                    message = "Ollama chat request failed: ${response.status.description}",
                    url = "${config.endpoint}/api/chat"
                ).left()
            }
        } catch (e: Exception) {
            LLMError(
                message = "Failed to execute Ollama chat: ${e.message}",
                cause = e,
                operation = "chat"
            ).left()
        }
    }
    
    suspend fun chatStream(
        messages: List<OllamaMessage>,
        options: OllamaOptions? = null
    ): Flow<Either<AppError, OllamaChatStreamChunk>> = flow {
        try {
            val request = OllamaChatRequest(
                model = config.model,
                messages = messages,
                stream = true,
                options = options
            )
            
            client.post("${config.endpoint}/api/chat") {
                setBody(ContentType.Application.Json, json.encodeToString(request))
            }.bodyAsChannel().toByteReadChannel().use { channel ->
                val buffer = StringBuilder()
                
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line()
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6)
                        if (data == "[DONE]") {
                            break
                        }
                        
                        try {
                            val chunk = json.decodeFromString<OllamaChatStreamChunk>(data)
                            emit(Either.Right(chunk))
                        } catch (e: Exception) {
                            emit(Either.Left(
                                LLMError(
                                    message = "Failed to parse stream chunk: ${e.message}",
                                    cause = e,
                                    operation = "chatStream"
                                )
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit(Either.Left(
                LLMError(
                    message = "Failed to execute Ollama chat stream: ${e.message}",
                    cause = e,
                    operation = "chatStream"
                )
            ))
        }
    }
    
    suspend fun generateEmbedding(
        text: String,
        model: String = config.model
    ): Either<AppError, List<Float>> {
        return try {
            val request = OllamaEmbeddingRequest(
                model = model,
                prompt = text
            )
            
            val response = client.post("${config.endpoint}/api/embeddings") {
                setBody(ContentType.Application.Json, json.encodeToString(request))
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val embeddingResponse = json.decodeFromString<OllamaEmbeddingResponse>(responseBody)
                embeddingResponse.embedding.right()
            } else {
                val errorBody = response.bodyAsText()
                NetworkError(
                    message = "Ollama embedding request failed: ${response.status.description}",
                    url = "${config.endpoint}/api/embeddings"
                ).left()
            }
        } catch (e: Exception) {
            LLMError(
                message = "Failed to generate embedding: ${e.message}",
                cause = e,
                operation = "generateEmbedding"
            ).left()
        }
    }
    
    suspend fun generateEmbeddings(
        texts: List<String>,
        model: String = config.model
    ): Either<AppError, List<List<Float>>> {
        val embeddings = mutableListOf<List<Float>>()
        val errors = mutableListOf<AppError>()
        
        for (text in texts) {
            when (val result = generateEmbedding(text, model)) {
                is Either.Right -> embeddings.add(result.value)
                is Either.Left -> errors.add(result.value)
            }
        }
        
        return if (errors.isNotEmpty()) {
            errors.first().left()
        } else {
            embeddings.right()
        }
    }
    
    suspend fun listModels(): Either<AppError, OllamaModelsResponse> {
        return try {
            val response = client.get("${config.endpoint}/api/tags")
            
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val modelsResponse = json.decodeFromString<OllamaModelsResponse>(responseBody)
                modelsResponse.right()
            } else {
                val errorBody = response.bodyAsText()
                NetworkError(
                    message = "Ollama list models request failed: ${response.status.description}",
                    url = "${config.endpoint}/api/tags"
                ).left()
            }
        } catch (e: Exception) {
            LLMError(
                message = "Failed to list Ollama models: ${e.message}",
                cause = e,
                operation = "listModels"
            ).left()
        }
    }
    
    suspend fun healthCheck(): Either<AppError, Boolean> {
        return try {
            val response = client.get("${config.endpoint}/api/tags")
            
            if (response.status.isSuccess()) {
                true.right()
            } else {
                NetworkError(
                    message = "Ollama health check failed: ${response.status.description}",
                    url = "${config.endpoint}/api/tags"
                ).left()
            }
        } catch (e: Exception) {
            LLMError(
                message = "Ollama health check failed: ${e.message}",
                cause = e,
                operation = "healthCheck"
            ).left()
        }
    }
    
    suspend fun isModelAvailable(model: String): Either<AppError, Boolean> {
        return listModels().map { response ->
            response.models.any { it.name == model }
        }
    }
    
    fun close() {
        client.close()
    }
}