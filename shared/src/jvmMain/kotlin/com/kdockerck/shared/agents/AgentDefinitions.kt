package com.kdockerck.shared.agents

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.llm.OllamaClient
import com.kdockerck.shared.llm.OllamaMessage
import com.kdockerck.shared.llm.OllamaOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class AgentType {
    object PARSING : AgentType()
    object EMBEDDING : AgentType()
    object GENERATION : AgentType()
}

data class AgentConfig(
    val name: String,
    val type: AgentType,
    val model: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096
)

sealed class AgentInput {
    data class TextInput(val text: String) : AgentInput()
    data class DocumentInput(val documentId: String, val content: String) : AgentInput()
    data class ContextInput(val context: String, val query: String) : AgentInput()
}

sealed class AgentOutput {
    data class TextOutput(val text: String) : AgentOutput()
    data class StructuredOutput(val data: Map<String, Any>) : AgentOutput()
    data class StreamOutput(val text: String, val isComplete: Boolean) : AgentOutput()
}

interface Agent {
    val config: AgentConfig
    val name: String
        get() = config.name
    
    suspend fun execute(input: AgentInput): Either<AppError, AgentOutput>
    fun executeStream(input: AgentInput): Flow<Either<AppError, AgentOutput>>
    suspend fun validate(input: AgentInput): Either<AppError, Boolean>
}

class ParsingAgent(
    override val config: AgentConfig,
    private val ollamaClient: OllamaClient
) : Agent {
    
    private val systemPrompt = """
        You are a document parsing agent. Your task is to extract structured information from documents.
        
        Analyze the provided text and extract:
        1. Main topics and themes
        2. Key entities and their relationships
        3. Important sections and their purposes
        4. Any requirements or specifications mentioned
        
        Respond in JSON format with the following structure:
        {
            "topics": ["topic1", "topic2"],
            "entities": [{"name": "entity", "type": "type", "description": "desc"}],
            "sections": [{"title": "section", "purpose": "purpose"}],
            "requirements": ["requirement1", "requirement2"]
        }
    """.trimIndent()
    
    override suspend fun execute(input: AgentInput): Either<AppError, AgentOutput> {
        return when (input) {
            is AgentInput.TextInput -> {
                val messages = listOf(
                    OllamaMessage(role = "system", content = systemPrompt),
                    OllamaMessage(role = "user", content = input.text)
                )
                
                val options = OllamaOptions(
                    temperature = config.temperature,
                    max_tokens = config.maxTokens
                )
                
                ollamaClient.chat(messages, options).map { response ->
                    AgentOutput.TextOutput(response.message.content)
                }
            }
            else -> AppError(
                message = "Unsupported input type for ParsingAgent: ${input::class.simpleName}"
            ).left()
        }
    }
    
    override fun executeStream(input: AgentInput): Flow<Either<AppError, AgentOutput>> = flow {
        when (input) {
            is AgentInput.TextInput -> {
                val messages = listOf(
                    OllamaMessage(role = "system", content = systemPrompt),
                    OllamaMessage(role = "user", content = input.text)
                )
                
                val options = OllamaOptions(
                    temperature = config.temperature,
                    max_tokens = config.maxTokens
                )
                
                ollamaClient.chatStream(messages, options).collect { result ->
                    when (result) {
                        is (Either.Right) -> {
                            val chunk = result.value.message?.content ?: ""
                            emit(Either.Right(AgentOutput.StreamOutput(chunk, result.value.done)))
                        }
                        is (Either.Left) -> {
                            emit(Either.Left(result.value))
                        }
                    }
                }
            }
            else -> {
                emit(Either.Left(AppError(
                    message = "Unsupported input type for ParsingAgent: ${input::class.simpleName}"
                )))
            }
        }
    }
    
    override suspend fun validate(input: AgentInput): Either<AppError, Boolean> {
        return when (input) {
            is AgentInput.TextInput -> {
                if (input.text.isBlank()) {
                    AppError(message = "Input text cannot be blank").left()
                } else {
                    true.right()
                }
            }
            else -> AppError(
                message = "Unsupported input type for ParsingAgent: ${input::class.simpleName}"
            ).left()
        }
    }
}

class EmbeddingAgent(
    override val config: AgentConfig,
    private val embeddingGenerator: com.kdockerck.shared.rag.EmbeddingGenerator
) : Agent {
    
    override suspend fun execute(input: AgentInput): Either<AppError, AgentOutput> {
        return when (input) {
            is AgentInput.TextInput -> {
                embeddingGenerator.generateEmbedding(input.text).map { vector ->
                    AgentOutput.StructuredOutput(mapOf(
                        "embedding" to vector,
                        "dimension" to vector.size
                    ))
                }
            }
            else -> AppError(
                message = "Unsupported input type for EmbeddingAgent: ${input::class.simpleName}"
            ).left()
        }
    }
    
    override fun executeStream(input: AgentInput): Flow<Either<AppError, AgentOutput>> = flow {
        when (input) {
            is AgentInput.TextInput -> {
                val result = embeddingGenerator.generateEmbeddingStream(input.text)
                result.collect { emit(it) }
            }
            else -> {
                emit(Either.Left(AppError(
                    message = "Unsupported input type for EmbeddingAgent: ${input::class.simpleName}"
                )))
            }
        }
    }
    
    override suspend fun validate(input: AgentInput): Either Either<AppError, Boolean> {
        return when (input) {
            is AgentInput.TextInput -> {
                if (input.text.isBlank()) {
                    AppError(message = "Input text cannot be blank").left()
                } else {
                    true.right()
                }
            }
            else -> AppError(
                message = "Unsupported input type for EmbeddingAgent: ${input::class.simpleName}"
            ).left()
        }
    }
}

class GenerationAgent(
    override val config: AgentConfig,
    private val ollamaClient: OllamaClient
) : Agent {
    
    private val systemPrompt = """
        You are a Gherkin feature file generation agent. Your task is to convert document content into Gherkin feature files.
        
        Guidelines:
        1. Create meaningful Feature names based on the document content
        2. Write clear, concise Scenarios that capture the requirements
        3. Use Given-When-Then format for steps
        4. Include Background sections for common setup steps
        5. Use Examples tables for test data variations
        6. Ensure the Gherkin syntax is valid and follows best practices
        
        Generate Gherkin feature files that can be used with Cucumber or similar testing frameworks.
    """.trimIndent()
    
    override suspend fun execute(input: AgentInput): Either<AppError, AgentOutput> {
        return when (input) {
            is AgentInput.ContextInput -> {
                val messages = buildList {
                    add(OllamaMessage(role = "system", content = systemPrompt))
                    
                    if (input.context.isNotBlank()) {
                        add(OllamaMessage(
                            role = "system",
                            content = "Context from previous documents:\n${input.context}"
                        ))
                    }
                    
                    add(OllamaMessage(role = "user", content = input.query))
                }
                
                val options = OllamaOptions(
                    temperature = config.temperature,
                    max_tokens = config.maxTokens
                )
                
                ollamaClient.chat(messages, options).map { response ->
                    AgentOutput.TextOutput(response.message.content)
                }
            }
            is AgentInput.TextInput -> {
                val messages = listOf(
                    OllamaMessage(role = "system", content = systemPrompt),
                    OllamaMessage(role = "user", content = input.text)
                )
                
                val options = OllamaOptions(
                    temperature = config.temperature,
                    max_tokens = config.maxTokens
                )
                
                ollamaClient.chat(messages, options).map { response ->
                    AgentOutput.TextOutput(response.message.content)
                }
            }
            else -> AppError(
                message = "Unsupported input type for GenerationAgent: ${input::class.simpleName}"
            ).left()
        }
    }
    
    override fun executeStream(input: AgentInput): Flow<Either<AppError, AgentOutput>> = flow {
        when (input) {
            is AgentInput.ContextInput -> {
                val messages = buildList {
                    add(OllamaMessage(role = "system", content = systemPrompt))
                    
                    if (input.context.isNotBlank()) {
                        add(OllamaMessage(
                            role = "system",
                            content = "Context from previous documents:\n${input.context}"
                        ))
                    }
                    
                    add(OllamaMessage(role = "user", content = input.query))
                }
                
                val options = OllamaOptions(
                    temperature = config.temperature,
                    max_tokens = config.maxTokens
                )
                
                ollamaClient.chatStream(messages, options).collect { result ->
                    when (result) {
                        is (Either.Right) -> {
                            val chunk = result.value.message?.content ?: ""
                            emit(Either.Right(AgentOutput.StreamOutput(chunk, result.value.done)))
                        }
                        is (Either.Left) -> {
                            emit(Either.Left(result.value))
                        }
    }
                }
            }
            is AgentInput.TextInput -> {
                val messages = listOf(
                    OllamaMessage(role = "system", content = systemPrompt),
                    OllamaMessage(role = "user", content = input.text)
                )
                
                val options = OllamaOptions(
                    temperature = config.temperature,
                    max_tokens = config.maxTokens
                )
                
                ollamaClient.chatStream(messages, options).collect { result ->
                    when (result) {
                        is (Either.Right) -> {
                            val chunk = result.value.message?.content ?: ""
                            emit(Either.Right(AgentOutput.StreamOutput(chunk, result.value.done)))
                        }
                        is (Either.Left) -> {
                            emit(Either.Left(result.value))
                        }
                    }
                }
            }
            else -> {
                emit(Either.Left(AppError(
                    message = "Unsupported input type for GenerationAgent: ${input::class.simpleName}"
                )))
            }
        }
    }
    
    override suspend fun validate(input: AgentInput): Either<AppError, Boolean> {
        return when (input) {
            is AgentInput.TextInput -> {
                if (input.text.isBlank()) {
                    AppError(message = "Input text cannot be blank").left()
                } else {
                    true.right()
                }
            }
            is AgentInput.ContextInput -> {
                if (input.query.isBlank()) {
                    AppError(message = "Query cannot be blank").left()
                } else {
                    true.right()
                }
            }
            else -> AppError(
                message = "Unsupported input type for GenerationAgent: ${input::class.simpleName}"
            ).left()
        }
    }
}