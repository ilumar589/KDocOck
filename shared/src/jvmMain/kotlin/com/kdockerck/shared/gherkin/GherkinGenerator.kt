package com.kdockerck.shared.gherkin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.agents.GenerationAgent
import com.kdockerck.shared.agents.AgentInput
import com.kdockerck.shared.agents.AgentOutput
import com.kdockerck.shared.llm.OllamaClient
import com.kdockerck.shared.llm.OllamaMessage
import com.kdockerck.shared.llm.OllamaOptions
import com.kdockerck.shared.domain.GherkinOutput
import com.kdockerck.shared.domain.Feature
import com.kdockerck.shared.domain.Scenario
import com.kdockerck.shared.domain.Step
import com.kdockerck.shared.domain.StepKeyword
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.LLMError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.datetime.Clock
import com.kdockerck.shared.domain

class GherkinGenerator(
    private val ollamaClient: OllamaClient,
    private val model: String = "llama3.2"
) {
    private val _generationEvents = MutableSharedFlow<GenerationEvent>(replay = 100)
    val generationEvents: Flow<GenerationEvent> = _generationEvents.asSharedFlow()
    
    suspend fun generate(
        documentContent: String,
        documentId: String,
        context: String? = null
    ): Either<AppError, GherkinOutput> {
        emitEvent(GenerationEvent.Started(documentId))
        
        val messages = buildList {
            add(OllamaMessage(
                role = "system",
                content = GherkinPromptTemplates.BASE_SYSTEM_PROMPT
            ))
            
            if (!context.isNullOrBlank()) {
                add(OllamaMessage(
                    role = "system",
                    content = "Context from previous documents:\n$context"
                ))
            }
            
            add(OllamaMessage(
                role = "user",
                content = GherkinPromptTemplates.generatePrompt(documentContent, context)
            ))
        }
        
        val options = OllamaOptions(
            temperature = 0.7,
            max_tokens = 4096
        )
        
        return try {
            val response = ollamaClient.chat(messages, options)
            
            when (response) {
                is Either.Right -> {
                    val gherkinText = response.value.message.content
                    val feature = parseGherkinText(gherkinText)
                    
                    val output = GherkinOutput(
                        documentId = documentId,
                        feature = feature,
                        generatedAt = Clock.System.now()
                    )
                    
                    emitEvent(GenerationEvent.Completed(documentId, output))
                    output.right()
                }
                is Either.Left -> {
                    emitEvent(GenerationEvent.Failed(documentId, response.value))
                    response.value.left()
                }
            }
        } catch (e: Exception) {
            val error = LLMError(
                message = "Failed to generate Gherkin: ${e.message}",
                cause = e,
                operation = "generate"
            )
            emitEvent(GenerationEvent.Failed(documentId, error))
            error.left()
        }
    }
    
    fun generateStream(
        documentContent: String,
        documentId: String,
        context: String? = null
    ): Flow<Either<AppError, GenerationEvent>> = flow {
        emit(Either.Right(GenerationEvent.Started(documentId)))
        
        val messages = buildList {
            add(OllamaMessage(
                role = "system",
                content = GherkinPromptTemplates.BASE_SYSTEM_PROMPT
            ))
            
            if (!context.isNullOrBlank()) {
                add(OllamaMessage(
                    role = "system",
                    content = "Context from previous documents:\n$context"
                ))
            }
            
            add(OllamaMessage(
                role = "user",
                content = GherkinPromptTemplates.generatePrompt(documentContent, context)
            ))
        }
        
        val options = OllamaOptions(
            temperature = 0.7,
            max_tokens = 4096
        )
        
        try {
            ollamaClient.chatStream(messages, options).collect { result ->
                when (result) {
                    is Either.Right -> {
                        val chunk = result.value.message?.content ?: ""
                        if (chunk.isNotEmpty()) {
                            emit(Either.Right(GenerationEvent.TokenGenerated(documentId, chunk)))
                        }
                        
                        if (result.value.done) {
                            emit(Either.Right(GenerationEvent.StreamCompleted(documentId)))
                        }
                    }
                    is Either.Left -> {
                        emit(Either.Left(result.value))
                        emit(Either.Right(GenerationEvent.Failed(documentId, result.value)))
                    }
                }
            }
        } catch (e: Exception) {
            val = LLMError(
                message = "Failed to stream Gherkin generation: ${e.message}",
                cause = e,
                operation = "generateStream"
            )
            emit(Either.Left(error))
            emit(Either.Right(GenerationEvent.Failed(documentId, error)))
        }
    }
    
    suspend fun generateWithRetry(
        documentContent: String,
        documentId: String,
        context: String? = null,
        maxRetries: Int = 3
    ): Either<AppError, GherkinOutput> {
        var attempt = 0
        var lastError: AppError? = null
        
        while (attempt < maxRetries) {
            val result = generate(documentContent, documentId, context)
            
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
    
    private fun parseGherkinText(text: String): Feature {
        val lines = text.lines()
        var featureName = "Untitled Feature"
        var featureDescription: String? = null
        val scenarios = mutableListOf<Scenario>()
        var currentScenario: Scenario? = null
        var currentSteps = mutableListOf<Step>()
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            when {
                trimmedLine.startsWith("Feature:", ignoreCase = true) -> {
                    featureName = trimmedLine.substringAfter(":").trim()
                }
                trimmedLine.startsWith("Scenario:", ignoreCase = true) -> {
                    if (currentScenario != null) {
                        currentScenario = currentScenario.copy(steps = currentSteps.toList())
                        scenarios.add(currentScenario)
                    }
                    
                    val scenarioName = trimmedLine.substringAfter(":").trim()
                    currentScenario = Scenario(
                        name = scenarioName,
                        description = null,
                        steps = emptyList(),
                        examples = null
                    )
                    currentSteps.clear()
                }
                trimmedLine.startsWith("Given", ignoreCase = true) -> {
                    val stepText = trimmedLine.substringAfter("Given").trim()
                    currentSteps.add(Step(
                        keyword = StepKeyword.GIVEN,
                        text = stepText,
                        argument = null
                    ))
                }
                trimmedLine.startsWith("When", ignoreCase = true) -> {
                    val stepText = trimmedLine.substringAfter("When").trim()
                    currentSteps.add(Step(
                        keyword = StepKeyword.WHEN,
                        text = stepText,
                        argument = null
                    ))
                }
                trimmedLine.startsWith("Then", ignoreCase = true) -> {
                    val stepText = trimmedLine.substringAfter("Then").trim()
                    currentSteps.add(Step(
                        keyword = StepKeyword.THEN,
                        text = stepText,
                        argument = null
                    ))
                }
                trimmedLine.startsWith("And", ignoreCase = true) -> {
                    val stepText = trimmedLine.substringAfter("And").trim()
                    val lastKeyword = currentSteps.lastOrNull()?.keyword ?: StepKeyword.GIVEN
                    currentSteps.add(Step(
                        keyword = lastKeyword,
                        text = stepText,
                        argument = null
                    ))
                }
                trimmedLine.startsWith("But", ignoreCase = true) -> {
                    val stepText = trimmedLine.substringAfter("But").trim()
                    val lastKeyword = currentSteps.lastOrNull()?.keyword ?: StepKeyword.THEN
                    currentSteps.add(Step(
                        keyword = lastKeyword,
                        text = stepText,
                        argument = null
                    ))
                }
            }
        }
        
        if (currentScenario) {
            currentScenario = currentScenario!!.copy(steps = currentSteps.toList())
            scenarios.add(currentScenario)
        }
        
        return Feature(
            name = featureName,
            description = featureDescription,
            background = null,
            scenarios = scenarios
        )
    }
    
    private fun emitEvent(event: GenerationEvent) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            _generationEvents.emit(event)
        }
    }
}

sealed class GenerationEvent {
    data class Started(val documentId: String) : GenerationEvent()
    data class TokenGenerated(val documentId: String, val token: String) : GenerationEvent()
    data class StreamCompleted(val documentId: String) : GenerationEvent()
    data class Completed(val documentId: String, val output: GherkinOutput) : GenerationEvent()
    data class Failed(val documentId: String, val error: AppError) : GenerationEvent()
}