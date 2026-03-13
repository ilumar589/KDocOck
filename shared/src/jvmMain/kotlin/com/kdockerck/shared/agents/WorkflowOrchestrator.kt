package com.kdockerck.shared.agents

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.datetime.Clock
import java.util.UUID
.concurrent.ConcurrentHashMap

data class WorkflowNode(
    val id: String,
    val agent: Agent,
    val input: AgentInput? = null,
    val dependencies: List<String> = emptyList(),
    val parallelWith: List<String> = emptyList()
)

data class WorkflowEdge(
    val from: String,
    val to: String,
    val condition: ((Map<String, AgentOutput>) -> Boolean)? = null
)

data class WorkflowConfig(
    val id: String,
    val name: String,
    val nodes: Map<String, WorkflowNode>,
    val edges: List<WorkflowEdge>,
    val timeoutMillis: Long = 300000
)

data class WorkflowExecution(
    val id: String,
    val workflowId: String,
    val status: WorkflowStatus,
    val startedAt: kotlinx.datetime.Instant,
    val completedAt: kotlinx.datetime.Instant? = null,
    val results: Map<String, AgentOutput> = emptyMap(),
    val errors: Map<String, AppError> = emptyMap(),
    val progress: WorkflowProgress
)

enum class WorkflowStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class WorkflowProgress(
    val totalNodes: Int,
    val completedNodes: Int,
    val currentNodes: List<String> = emptyList(),
    val percentage: Double
) {
    companion object {
        fun from(total: Int, completed: Int, current: List<String> = emptyList()): WorkflowProgress {
            val percentage = if (total > 0) {
                (completed.toDouble() / total.toDouble()) * 100.0
            } else {
                0.0
            }
            
            WorkflowProgress(
                totalNodes = total,
                completedNodes = completed,
                currentNodes = current,
                percentage = percentage
            )
        }
    }
}

class WorkflowOrchestrator(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val activeWorkflows = ConcurrentHashMap<String, WorkflowExecution>()
    private val _workflowEvents = MutableSharedFlow<WorkflowEvent>(replay = 100)
    val workflowEvents: Flow<WorkflowEvent> = _workflowEvents.asSharedFlow()
    
    suspend fun execute(
        config: WorkflowConfig,
        initialInputs: Map<String, AgentInput> = emptyMap()
    ): Either<AppError, WorkflowExecution> {
        val executionId = UUID.randomUUID().toString()
        val execution = WorkflowExecution(
            id = executionId,
            workflowId = config.id,
            status = WorkflowStatus.RUNNING,
            startedAt = Clock.System.now(),
            progress = WorkflowProgress.from(config.nodes.size, 0)
        )
        
        activeWorkflows[executionId] = execution
        emitEvent(WorkflowEvent.Started(execution))
        
        return try {
            val results = executeGraph(config, initialInputs, executionId)
            
            when (results) {
                is Either.Right -> {
                    val completedExecution = execution.copy(
                        status = WorkflowStatus.COMPLETED,
                        completedAt = Clock.System.now(),
                        results = results.value,
                        progress = WorkflowProgress.from(config.nodes.size, config.nodes.size)
                    )
                    
                    activeWorkflows[executionId] = completedExecution
                    emitEvent(WorkflowEvent.Completed(completedExecution))
                    
                    completedExecution.right()
                }
                is Either.Left -> {
                    val failedExecution = execution.copy(
                        status = WorkflowStatus.FAILED,
                        completedAt = Clock.System.now(),
                        errors = mapOf(results.value.first.key to results.value.second),
                        progress = execution.progress
                    )
                    
                    activeWorkflows[executionId] = failedExecution
                    emitEvent(WorkflowEvent.Failed(failedExecution, results.value.second))
                    
                    results.value.second.left()
                }
            }
        } catch (e: Exception) {
            val failedExecution = execution.copy(
                status = WorkflowStatus.FAILED,
                completedAt = Clock.System.now(),
                errors = mapOf("workflow" to AppError(
                    message = "Workflow execution failed: ${e.message}",
                    cause = e
                )),
                progress = execution.progress
            )
            
            activeWorkflows[executionId] = failedExecution
            emitEvent(WorkflowEvent.Failed(failedExecution, failedExecution.errors.values.first()))
            
            failedExecution.errors.values.first().left()
        }
    }
    
    private suspend fun executeGraph(
        config: WorkflowConfig,
        initialInputs: Map<String, AgentInput>,
        executionId: String
    ): Either<Pair<String, AppError>, Map<String, AgentOutput>> {
        val results = mutableMapOf<String, AgentOutput>()
        val errors = mutableMapOf<String, AppError>()
        val completedNodes = mutableSetOf<String>()
        
        val executionOrder = determineExecutionOrder(config)
        
        for (batch in executionOrder) {
            updateProgress(executionId, config.nodes.size, completedNodes.size, batch)
            
            val batchResults = executeBatch(batch, config, initialInputs, results, executionId)
            
            when (batchResults) {
                is Either.Right -> {
                    results.putAll(batchResults.value)
                    completedNodes.addAll(batch)
                }
                is Either.Left -> {
                    errors.putAll(batchResults.value)
                    return Pair(batchResults.value.keys.first(), batchResults.value.values.first()).left()
                }
            }
        }
        
        return results.right()
    }
    
    private fun determineExecutionOrder(config: WorkflowConfig): List<List<String>> {
        val batches = mutableListOf<List<String>>()
        val processed = mutableSetOf<String>()
        val allNodes = config.nodes.keys.toMutableSet()
        
        while (processed.size < allNodes.size) {
            val readyNodes = allNodes.filter { nodeId ->
                nodeId !in processed && canExecute(nodeId, processed, config)
            }
            
            if (readyNodes.isEmpty()) {
                break
            }
            
            batches.add(readyNodes)
            processed.addAll(readyNodes)
        }
        
        return batches
    }
    
    private fun canExecute(
        nodeId: String,
        processed: Set<String>,
        config: WorkflowConfig
    ): Boolean {
        val node = config.nodes[nodeId] ?: return false
        return node.dependencies.all { it in processed }
    }
    
    private suspend fun executeBatch(
        batch: List<String>,
        config: WorkflowConfig,
        initialInputs: Map<String, AgentInput>,
        results: Map<String, AgentOutput>,
        executionId: String
    ): Either<Map<String, AppError>, Map<String, AgentOutput>> {
        val batchResults = mutableMapOf<String, AgentOutput>()
        val batchErrors = mutableMapOf<String, AppError>()
        
        val jobs = batch.map { nodeId ->
            scope.async {
                val node = config.nodes[nodeId]!!
                val input = resolveInput(node, initialInputs, results)
                
                emitEvent(WorkflowEvent.NodeStarted(executionId, nodeId))
                
                when (val result = node.agent.execute(input)) {
                    is Either.Right -> {
                        emitEvent(WorkflowEvent.NodeCompleted(executionId, nodeId, result.value))
                        Pair(nodeId, result.value)
                    }
                    is Either.Left -> {
                        emitEvent(WorkflowEvent.NodeFailed(executionId, nodeId, result.value))
                        Pair(nodeId, result.value)
                    }
                }
            }
        }
        
        val results = jobs.awaitAll()
        
        for ((nodeId, output) in results) {
            when (output) {
                is AgentOutput -> batchResults[nodeId] = output
                is AppError -> batchErrors[nodeId] = output
            }
        }
        
        return if (batchErrors.isNotEmpty()) {
            batchErrors.left()
        } else {
            batchResults.right()
        }
    }
    
    private fun resolveInput(
        node: WorkflowNode,
        initialInputs: Map<String, AgentInput>,
        results: Map<String, AgentOutput>
    ): AgentInput {
        return node.input ?: initialInputs[node.id] ?: AgentInput.TextInput("")
    }
    
    private fun updateProgress(
        executionId: String,
        totalNodes: Int,
        completedNodes: Int,
        currentNodes: List<String>
    ) {
        val execution = activeWorkflows[executionId] ?: return
        val progress = WorkflowProgress.from(totalNodes, completedNodes, currentNodes)
        
        activeWorkflows[executionId] = execution.copy(progress = progress)
        emitEvent(WorkflowEvent.Progress(executionId, progress))
    }
    
    suspend fun cancel(executionId: String): Either<AppError, Unit> {
        val execution = activeWorkflows[executionId]
        
        return if (execution != null) {
            val cancelledExecution = execution.copy(
                status = WorkflowStatus.CANCELLED,
                completedAt = Clock.System.now()
            )
            
            activeWorkflows[executionId] = cancelledExecution
            emitEvent(WorkflowEvent.Cancelled(executionId))
            
            Unit.right()
        } else {
            AppError(message = "Workflow execution not found: $executionId").left()
        }
    }
    
    fun getExecution(executionId: String): WorkflowExecution? {
        return activeWorkflows[executionId]
    }
    
    fun getAllExecutions(): List<WorkflowExecution> {
        return activeWorkflows.values.toList()
    }
    
    private fun emitEvent(event: WorkflowEvent) {
        scope.launch {
            _workflowEvents.emit(event)
        }
    }
}

sealed class WorkflowEvent {
    data class Started(val execution: WorkflowExecution) : WorkflowEvent()
    data class Completed(val execution: WorkflowExecution) : WorkflowEvent()
    data class Failed(val execution: WorkflowExecution, val error: AppError) : WorkflowEvent()
    data class Cancelled(val executionId: String) : WorkflowEvent()
    data class NodeStarted(val executionId: String, val nodeId: String) : WorkflowEvent()
    data class NodeCompleted(val executionId: String, val nodeId: String, val output: AgentOutput) : WorkflowEvent()
    data class NodeFailed(val executionId: String, val nodeId: String, val error: AppError) : WorkflowEvent()
    data class Progress(val executionId: String, val progress: WorkflowProgress) : WorkflowEvent()
}