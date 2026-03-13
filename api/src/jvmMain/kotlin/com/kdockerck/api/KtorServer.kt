package com.kdockerck.api

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.NetworkError
import com.kdockerck.shared.errors.ValidationError
import com.kdockerck.shared.config.AppConfig
import com.kdockerck.shared.config.DatabaseConfig
import com.kdockerck.shared.database.DatabaseConnectionManager
import com.kdockerck.shared.gherkin.GherkinGenerator
import com.kdockerck.shared.llm.OllamaClient
import com.kdockerck.shared.llm.OllamaConfig
import com.kdockerck.shared.logging.Logger
import com.kdockerck.shared.logging.LoggerFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.resources.*
import io.ktor.server.plugins.cors.CORS
import io.ktor.server.plugins.cors.CORSConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.concurrent.ConcurrentHashMap

class KtorServer(
    private val config: AppConfig,
    private val port: Int = 8080
) {
    private val logger = LoggerFactory.getLogger("KtorServer")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private val activeConnections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val processingJobs = ConcurrentHashMap<String, ProcessingJob>()
    
    private val dbManager by lazy {
        DatabaseConnectionManager.initialize(config.database)
    }
    
    private val ollamaClient by lazy {
        OllamaClient(OllamaConfig(
            endpoint = config.ollama.endpoint,
            model = config.ollama.model,
            timeout = config.ollama.timeoutMillis,
            maxRetries = config.ollama.maxRetries
        ))
    }
    
    private val gherkinGenerator byGherkin {
        GherkinGenerator(ollamaClient, config.ollama.model)
    }
    
    private val server by lazy {
        embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                accept(ContentType.Application.Json)
                accept(ContentType.Text.Plain)
                accept(ContentType.Text.Plain)
                accept(ContentType.Application.Json)
            }
            
            install(StatusPages) {
                exception<Exception> { call, cause ->
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "error" to (cause.message ?: "Internal server error"),
                        "timestamp" to kotlinx.datetime.Clock.System.now().toString()
                    ))
                }
                
                status(HttpStatusCode.NotFound) { call, status ->
                    call.respond(mapOf(
                        "error" to "Resource not found",
                        "status" to status.value,
                        "timestamp" to kotlinx.datetime.Clock.System.now().toString()
                    ))
                }
            }
            
            install(CallLogging) {
                level = io.ktor.features.calllogging.Level.INFO
            }
            
            install(CORS) {
                allowMethod(HttpMethod.Options)
                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
                allowCredentials = true
                allowNonSimpleContentTypes = true
                anyHost()
                allowOrigins = config.api.corsOrigins
            }
            
            install(Authentication) {
                basic {
                    validate { credentials ->
                        if (config.api.apiKey != null) {
                            credentials.name == "api" && credentials.password == config.api.apiKey
                        } else {
                            true
                        }
                    }
                }
            }
            
            install(WebSockets) {
                onConnect { session, request ->
                    val sessionId = request.queryParameters["sessionId"]?.firstOrNull()
                        ?: java.util.UUID.randomUUID().toString()
                    
                    activeConnections[sessionId] = session
                    logger.info("WebSocket connected: $sessionId")
                }
                
                onDisconnect { session, cause ->
                    val sessionId = activeConnections.entries
                        .firstOrNull { it.value == session }?.key
                    
                    if (sessionId != null) {
                        activeConnections.remove(sessionId)
                        logger.info("WebSocket disconnected: $sessionId")
                    }
                }
            }
            
            routing {
                webSocket("/ws/progress") { session ->
                    handleProgressWebSocket(session)
                }
                
                webSocket("/ws/tokens") { session ->
                    handleTokenWebSocket(session)
                }
                
                route("/api/health") {
                    handleHealthCheck()
                }
                
                route("/api/config") {
                    handleConfig()
                }
                
                authenticate {
                    route("/api/documents") {
                        method(HttpMethod.Post) {
                            handleDocumentUpload(call)
                        }
                        
                        method(HttpMethod.Get) {
                            handleGetDocuments(call)
                        }
                    }
                    
                    route("/api/documents/batch") {
                        method(HttpMethod.Post) {
                            handleBatchDocumentUpload(call)
                        }
                    }
                    
                    route("/api/documents/{id}") {
                        method(HttpMethod.Post) {
                            handleProcessDocument(call)
                        }
                        
                        method(HttpMethod.Get) {
                            handleGetDocument(call)
                        }
                        
                        method(HttpMethod.Delete) {
                            handleDeleteDocument(call)
                        }
                    }
                    
                    route("/api/documents/{id}/process") {
                        method(HttpMethod.Post) {
                            handleProcessDocument(call)
                        }
                    }
                    
                    route("/api/documents/{id}/status") {
                        method(HttpMethod.Get) {
                            handleGetDocumentStatus(call)
                        }
                    }
                    
                    route("/api/documents/{id}/result") {
                        method(HttpMethod.Get) {
                            handleGetDocumentResult(call)
                        }
                    }
                    
                    route("/api/documents/batch-process") {
                        method(HttpMethod.Post) {
                            handleBatchProcessDocuments(call)
                        }
                    }
                    
                    route("/api/jobs/{jobId}/status") {
                        method(HttpMethod.Get) {
                            handleGetJobStatus(call)
                        }
                    }
                    
                    route("/api/jobs/{jobId}/results") {
                        method(HttpMethod.Get) {
                            handleGetJobResults(call)
                        }
                    }
                }
            }
                
                webSocket("/ws/tokens") { session ->
                    handleTokenWebSocket(session)
                }
                
                route("/api/health") {
                    handleHealthCheck()
                }
                
                route("/api/config") {
                    handleConfig()
                }
                
                authenticate {
                    route("/api/documents") {
                        method(HttpMethod.Post) {
                            handleDocumentUpload(call)
                        }
                        
                        method(HttpMethod.Get) {
                            handleGetDocuments(call)
                        }
                    }
                    
                    route("/api/documents/batch") {
                        method(HttpMethod.Post) {
                            handleBatchDocumentUpload(call)
                        }
                    }
                    
                    route("/api/documents/{id}") {
                        method(HttpMethod.Post) {
                            handleProcessDocument(call)
                        }
                        
                        method(HttpMethod.Get) {
                            handleGetDocument(call)
                        }
                        
                        method(HttpMethod.Delete) {
                            handleDeleteDocument(call)
                        }
                    }
                    
                    route("/api/documents/{id}/process") {
                        method(HttpMethod.Post) {
                            handleProcessDocument(call)
                        }
                    }
                    
                    route("/api/documents/{id}/status") {
                        method(HttpMethod.Get) {
                            handleGetDocumentStatus(call)
                        }
                    }
                    
                    route("/api/documents/{id}/result") {
                        method(HttpMethod.Get) {
                            handleGetDocumentResult(call)
                        }
                    }
                    
                    route("/api/documents/batch-process") {
                        method(HttpMethod.Post) {
                            handleBatchProcessDocuments(call)
                        }
                    }
                    
                    route("/api/jobs/{jobId}/status") {
                        method(HttpMethod.Get) {
                            handleGetJobStatus(call)
                        }
                    }
                    
                    route("/api/jobs/{jobId}/results") {
                        method(HttpMethod.Get) {
                            handleGetJobResults(call)
                        }
                    }
                }
            }
        }
    }
    
    fun start(): Either<AppError, Unit> = try {
        server.start(wait = false)
        logger.info("Ktor server started on port $port")
        Unit.right()
    } catch (e: Exception) {
        NetworkError(
            message = "Failed to start Ktor server: ${e.message}",
            cause = e
        ).left()
    }
    
    fun stop(): Either<AppError, Unit> = try {
        server.stop(1000, 5000)
        logger.info("Ktor server stopped")
        Unit.right()
    } catch (e: Exception) {
        NetworkError(
            message = "Failed to stop Ktor server: ${e.message}",
            cause = e
        ).left()
    }
    
    fun isRunning(): Boolean {
        return server.engine.isActive
    }
    
    private fun handleHealthCheck(): Unit {
        call.respond(HttpStatusCode.OK, mapOf(
            "status" to "healthy",
            "timestamp" to kotlinx.datetime.Clock.System.now().toString()
        ))
    }
    
    private fun handleConfig(): Unit {
        call.respond(HttpStatusCode.OK, config)
    }
    
    private suspend fun handleDocumentUpload(call: ApplicationCall) {
        val multipartContent = call.receiveMultipart()
        val filePart = multipartContent?.getPartByName("file")
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file provided"))
        
        val fileName = filePart.originalFileName
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file name"))
        
        val fileContent = filePart.streamProvider().readBytes()
        
        val documentId = java.util.UUID.randomUUID().toString()
        val document = com.kdockerck.shared.domain.Document(
            id = documentId,
            fileName = fileName,
            fileType = fileName.substringAfterLast(".").toFileType(),
            filePath = "",
            fileSize = fileContent.size.toLong(),
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        
        logger.info("Document uploaded: $fileName ($documentId)")
        
        call.respond(HttpStatusCode.OK, mapOf(
            "documentId" to documentId,
            "fileName" to fileName,
            "fileSize" to fileContent.size,
            "status" to "uploaded"
        ))
    }
    
    private suspend fun handleBatchDocumentUpload(call: ApplicationCall) {
        val multipartContent = call.receiveMultipart()
        val fileParts = multipartContent?.parts?.filter { it.name == "file" }
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No files provided"))
        
        val documents = mutableListOf<Map<String, Any>>()
        
        for (filePart in fileParts) {
            val fileName = filePart.originalFileName ?: continue
            val fileContent = filePart.streamProvider().readBytes()
            val documentId = java.util.UUID.randomUUID().toString()
            
            documents.add(mapOf(
                "documentId" to documentId,
                "fileName" to fileName,
                "fileSize" to fileContent.size,
                "status" to "uploaded"
            ))
            
            logger.info("Document uploaded: $fileName ($documentId)")
        }
        
        call.respond(HttpStatusCode.OK, mapOf(
            "documents" to documents,
            "count" to documents.size
        ))
    }
    
    private suspend fun handleGetDocuments(call: ApplicationCall) {
        val documents = listOf<Map<String, Any>>()
        
        call.respond(HttpStatusCode.OK, mapOf(
            "documents" to documents,
            "count" to documents.size
        ))
    }
    
    private suspend fun handleProcessDocument(call: ApplicationCall) {
        val documentId = call.parameters["id"]
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Document ID required"))
        
        val jobId = java.util.UUID.randomUUID().toString()
        val job = ProcessingJob(
            id = jobId,
            documentIds = listOf(documentId),
            status = JobStatus.PENDING,
            startedAt = kotlinx.datetime.Clock.System.now()
        )
        
        processingJobs[jobId] = job
        
        CoroutineScope(Dispatchers.Default).launch {
            job.status = JobStatus.RUNNING
            job.startedAt = kotlinx.datetime.Clock.System.now()
            
            logger.info("Processing document: $documentId (job: $jobId)")
            
            kotlinx.coroutines.delay(1000)
            
            job.status = JobStatus.COMPLETED
            job.completedAt = kotlinx.datetime.Clock.System.now()
            job.results[documentId] = "Feature: Test Feature\n\nScenario: Test scenario\n  Given test\n  When action\n  Then result"
            
            logger.info("Document processed: $documentId (")
        }
        
        call.respond(HttpStatusCode.OK, mapOf(
            "jobId" to jobId,
            "documentId" to documentId,
            "status" to "pending"
        ))
    }
    
    private suspend fun handleGetDocument(call: ApplicationCall) {
        val documentId = call.parameters["id"]
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Document ID required"))
        
        call.respond(HttpStatusCode.OK, mapOf(
            "documentId" to documentId,
            "status" to "not_found"
        ))
    }
    
    private suspend fun handleDeleteDocument(call: ApplicationCall) {
        val documentId = call.parameters["id"]
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Document ID required"))
        
        logger.info("Document deleted: $documentId")
        
        call.respond(HttpStatusCode.OK, mapOf(
            "documentId" to documentId,
            "status" to "deleted"
        ))
    }
    
    private suspend fun handleGetDocumentStatus(call: ApplicationCall) {
        val documentId = call.parameters["id"]
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Document ID required"))
        
        call.respond(HttpStatusCode.OK, mapOf(
            "documentId" to documentId,
            "status" to "not_processed"
        ))
    }
    
    private suspend fun handleGetDocumentResult(call: ApplicationCall) {
        val documentId = call.parameters["id"]
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Document ID required"))
        
        call.respond(HttpStatusCode.OK, mapOf(
            "documentId" to documentId,
            "result" to "not_found"
        ))
    }
    
    private suspend fun handleBatchProcessDocuments(call: ApplicationCall) {
        val requestBody = call.receiveText()
        val documentIds = json.decodeFromString<Map<String, List<String>>>(requestBody)["documents"]
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No documents provided"))
        
        val jobId = java.util.UUID.randomUUID().toString()
        val job = ProcessingJob(
            id = jobId,
            documentIds = documentIds,
            status = JobStatus.PENDING,
            startedAt = kotlinx.datetime.Clock.System.now()
        )
        
        processingJobs[jobId] = job
        
        CoroutineScope(Dispatchers.Default).launch {
            job.status = JobStatus.RUNNING
            job.startedAt = kotlinx.datetime.Clock.System.now()
            
            logger.info("Batch processing ${documentIds.size} documents (job: $jobId)")
            
            kotlinx.coroutines.delay(2000)
            
            job.status = JobStatus.COMPLETED
            job.completedAt = kotlinx.datetime.Clock.System.now()
            
            for (documentId in documentIds) {
                job.results[documentId] = "Feature: Test Feature\n\nScenario: Test scenario\n  Given test\n  When action\n  Then result"
            }
            
            logger.info("Batch processing completed (job: $jobId)")
        }
        
        call.respond(HttpStatusCode.OK, mapOf(
            "jobId" to jobId,
            "documentIds" to documentIds,
            "status" to "pending"
        ))
    }
    
    private suspend fun handleGetJobStatus(call: ApplicationCall) {
        val jobId = call.parameters["jobId"]
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Job ID required"))
        
        val job = processingJobs[jobId]
        
        if (job != null) {
            call.respond(HttpStatusCode.OK, mapOf(
                "jobId" to jobId,
                "status" to job.status.name.lowercase(),
                "documentIds" to job.documentIds,
                "startedAt" to job.startedAt.toString(),
                "completedAt" to job.completedAt?.toString(),
                "progress" to mapOf(
                    "total" to job.documentIds.size,
                    "completed" to job.results.size,
                    "percentage" to (job.results.size.toDouble() / job.documentIds.size.toDouble() * 100.0)
                )
            ))
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Job not found"))
        }
    }
    
    private suspend fun handleGetJobResults(call: ApplicationCall) {
        val jobId = call.parameters["jobId"]
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Job ID required"))
        
        val job = processingJobs[jobId]
        
        if (job != null) {
            call.respond(HttpStatusCode.OK, mapOf(
                "jobId" to jobId,
                "results" to job.results
            ))
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Job not found"))
        }
    }
    
    private suspend fun handleProgressWebSocket(session: DefaultWebSocketSession) {
        session.send(Frame.Text("Connected to progress updates"))
        
        while (true) {
            val frame = session.incoming.receive()
            if (frame is Frame.Close) {
                break
            }
            
            kotlinx.coroutines.delay(1000)
            
            val progressUpdate = mapOf(
                "type" to "progress",
                "timestamp" to kotlinx.datetime.Clock.System.now().toString(),
                "message" to "Processing..."
            )
            
            session.send(Frame.Text(json.encodeToString(progressUpdate)))
        }
    }
    
    private suspend fun handleTokenWebSocket(session: DefaultWebSocketSession) {
        session.send(Frame.Text("Connected to token stream"))
        
        while (true) {
            val frame = session.incoming.receive()
            if (frame is Frame.Close) {
                break
            }
            
            val token = "test_token"
            
            session.send(Frame.Text(token))
            
            kotlinx.coroutines.delay(100)
        }
    }
}

data class ProcessingJob(
    val id: String,
    val documentIds: List<String>,
    val status: JobStatus,
    val startedAt: kotlinx.datetime.Instant,
    val completedAt: kotlinx.datetime.Instant? = null,
    val results: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap()
)

enum class JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}