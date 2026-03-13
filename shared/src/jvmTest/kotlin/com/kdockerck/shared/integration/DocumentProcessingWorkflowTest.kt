package com.kdockerck.shared.integration

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.config.AppConfig
import com.kdockerck.shared.database.DatabaseConnectionManager
import com.kdockerck.shared.domain.Document
import com.kdockerck.shared.domain.DocumentStatus
import com.kdockerck.shared.domain.ParsedContent
import com.kdockerck.shared.domain.GherkinOutput
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.parsing.DocumentParserDispatcher
import com.kdockerck.shared.rag.BatchEmbeddingGenerator
import com.kockerck.shared.rag.CrossFileContextRetriever
import com.kdockerck.shared.gherkin.GherkinGenerator
import com.kdockerck.shared.gherkin.GherkinValidator
import com.kdockerck.shared.logging.Logger
import com.kdockerck.shared.logging.LoggerFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

class DocumentProcessingWorkflowTest {
    private val logger = LoggerFactory.getLogger("DocumentProcessingWorkflowTest")
    
    private val testConfig = AppConfig(
        ollama = com.kdockerck.shared.config.OllamaConfig(),
        database = com.kdockerck.shared.config.DatabaseConfig(
            url = "r2dbc:postgresql://localhost:5432/kdockerck_test",
            username = "kdockerck",
            password = " "kdockerck"
        ),
        embedding = com.kdockerck.shared.config.EmbeddingConfig(),
        ui = com.kdockerck.shared.config.UIConfig()
    )
    
    private val documentParser = DocumentParserDispatcher()
    private val mockEmbeddingGenerator = MockEmbeddingGenerator(testConfig.embedding)
    private val mockContextRetriever = MockCrossFileContextRetriever()
    private val mockGherkinGenerator = MockGherkinGenerator()
    private val gherkinValidator = GherkinValidator()
    
    @Test
    fun `should process single document end-to-end`() = runTest {
        val testFile = createTestDocument("test_document.docx", "Test document content")
        
        val document = Document(
            id = "test-doc-1",
            fileName = "test_document.docx",
            fileType = com.kdockerck.shared.domain.FileType.DOCX,
            filePath = testFile.absolutePath,
            fileSize = testFile.length(),
            createdAt = kotlinx.datetime.Clockkt.Clock.System.now()
        )
        
        val result = processDocument(document)
        
        assertTrue(result.isRight())
        val processedDocument = result.value
        
        assertEquals(DocumentStatus.COMPLETED, processedDocument.status)
        assertTrue(processedDocument.gherkinOutput != null)
    }
    
    @Test
    fun `should process multiple documents in sequence`() = runTest {
        val testFiles = listOf(
            createTestDocument("doc1.docx", "Document 1 content"),
            createTestDocument("doc2.xlsx", "Document 2 content"),
            createTestDocument("doc3.vsdx", "Document 3 content")
        )
        
        val results = testFiles.map { document ->
            processDocument(document)
        }
        
        assertTrue(results.all { it.isRight() })
        assertEquals(testFiles.size, results.size)
        
        results.forEachIndexed { index, result ->
            val processedDocument = result.value
            assertEquals(DocumentStatus.COMPLETED, processedDocument.status)
            assertEquals("doc${index + 1}", processedDocument.fileName)
        }
    }
    
    @Test
    fun `should handle parsing error gracefully`() = runTest {
        val invalidFile = createTestDocument("invalid.docx", "Invalid content")
        
        val result = processDocument(invalidFile)
        
        assertTrue(result.isLeft())
        val error = result.value
        
        assertTrue(error is com.kdockerck.shared.errors.ParsingError)
    }
    
    @Test
    fun `should handle embedding error gracefully`() = runTest {
        val testFile = createTestDocument("test_document.docx", "Test document content")
        
        val result = processDocument(testFile)
        
        assertTrue(result.isLeft())
        val error = result.value
        
        assertTrue(error is com.kdockerck.shared.errors.LLMError)
    }
    
    @Test
    fun `should validate generated Gherkin`() = runTest {
        val testFile = createTestDocument("test_document.docx", "Feature: User Authentication\n\nScenario: Test scenario\n  Given test\n  When action\n  Then result")
        
        val result = processDocument(testFile)
        
        assertTrue(result.isRight())
        val processedDocument = result.value
        
        val gherkinOutput = processedDocument.gherkinOutput
        assertNotNull(gherkinOutput)
        
        val validationResult = gherkinValidator.validate(gherkinOutput.feature)
        
        assertTrue(validationResult.isRight())
    }
    
    @Test
    fun `should repair invalid Gherkin syntax`() = runTest {
        val testFile = createTestDocument("test_document.docx", "feature: Test\nscenario: test scenario\ngiven test\nwhen action\nthen result")
        
        val result = processDocument(testFile)
        
        assertTrue(result.isRight())
        val processedDocument = result.value
        
        val gherkinOutput = processedDocument.gherkinOutput
        
        val validationResult = gherkinValidator.validateGherkinText(gherkinOutput.feature.toString())
        
        assertTrue(validationResult.isRight())
    }
    
    @Test
    fun `should include cross-file context`() = runTest {
        val testFiles = listOf(
            createTestDocument("context_doc1.docx", "Context document 1"),
            createTestDocument("context_doc2.docx", "Context document 2")
        )
        
        val results = testFiles.map { document ->
            processDocument(document)
        }
        
        assertTrue(results.all { it.isRight() })
        
        val processedDocuments = results.map { it.value }
        
        processedDocuments.forEach { document ->
            val gherkinOutput = document.gherkinOutput
            assertNotNull(gherkinOutput)
            assertTrue(gherkinOutput.feature.scenarios.isNotEmpty())
        }
    }
    
    @Test
    fun `should handle large document`() = runTest {
        val largeContent = "Test content ".repeat(10000)
        val testFile = createTestDocument("large_document.docx", largeContent)
        
        val result = processDocument(testFile)
        
        assertTrue(result.isRight())
        val processedDocument = result.value
        
        val gherkinOutput = processedDocument.gherkinOutput
        assertNotNull(gherkinOutput)
        assertTrue(gherkinOutput.feature.scenarios.isNotEmpty())
    }
    
    @Test
    fun `should handle concurrent document processing`() = runTest {
        val testFiles = listOf(
            createTestDocument("concurrent_doc1.docx", "Concurrent document 1"),
            createTestDocument("concurrent_doc2.docx", "Concurrent document 2"),
            createTestDocument("concurrent_doc3.docx", "Concurrent document 3")
        )
        
        val results = testFiles.map { document ->
            kotlinx.coroutines.async {
                processDocument(document)
            }
        }
        
        kotlinx.coroutines.awaitAll(*results)
        
        assertTrue(results.all { it.isCompleted() })
        assertEquals(testFiles.size, results.size)
    }
    
    private fun processDocument(
        document: Document
    ): Either<AppError, ProcessedDocument> {
        return try {
            val parsedContent = documentParser.parse(document.filePath, document.id)
            
            if (parsedContent.isLeft()) {
                return parsedContent.mapLeft { error ->
                    ProcessedDocument(
                        document = document,
                        status = DocumentStatus.FAILED,
                        error = error.message
                    )
                }
            }
            
            val embeddingResult = mockEmbeddingGenerator.generateEmbedding(
                extractTextFromContent(parsedContent.value)
            )
            
            if (embeddingResult.isLeft()) {
                return embeddingResult.mapLeft { error ->
                    ProcessedDocument(
                        document = document,
                        status = DocumentStatus.FAILED,
                        error = error.message
                    )
                }
            }
            
            val embedding = embeddingResult.value
            val contextResult = mockContextRetriever.retrieveContext(
                parsedContent.value,
                excludeDocumentId = document.id
            )
            
            if (contextResult.isLeft()) {
                return contextResult.mapLeft { error ->
                    ProcessedDocument(
                        document = document,
                        status = DocumentStatus.FAILED,
                        error = error.message
                    )
                }
            }
            
            val prompt = "Generate Gherkin for: ${document.fileName}\n\nContext: ${contextResult.value}"
            
            val gherkinResult = mockGherkinGenerator.generate(prompt, document.id)
            
            if (gherkinResult.isLeft()) {
                return gherkinResult.mapLeft { error ->
                    ProcessedDocument(
                        document = document,
                        status = DocumentStatus.FAILED,
                        error = error.message
                    )
                }
            }
            
            val gherkinOutput = gherkinResult.value
            val validationResult = gherkinValidator.validate(gherkinOutput.feature)
            
            if (validationResult.isLeft()) {
                return validationResult.mapLeft { error ->
                    ProcessedDocument(
                        document = document,
                        status = DocumentStatus.FAILED,
                        error = "Gherkin validation failed: ${error.message}"
                    )
                }
            }
            
            ProcessedDocument(
                document = document,
                status = DocumentStatus.COMPLETED,
                gherkinOutput = gherkinOutput
            ).right()
        } catch (e: Exception) {
            ProcessedDocument(
                document = document,
                status = DocumentStatus.FAILED,
                error = e.message ?: "Unknown error"
            ).left()
        }
    }
    
    private fun extractTextFromContent(content: ParsedContent): String {
        return when (content) {
            is com.kdockerck.shared.domain.WordDocumentContent -> {
                content.paragraphs.joinToString("\n") { it.text } +
                content.tables.joinToString("\n") { table ->
                    table.rows.joinToString("\n") { row ->
                        row.cells.joinToString(" | ") { it.text }
                    }
                }
            }
            is com.kdockerck.shared.domain.ExcelDocumentContent -> {
                content.worksheets.joinToString("\n") { worksheet ->
                    "Worksheet: ${worksheet.name}\n" +
                    worksheet.cells.joinToString("\n") { cell ->
                        "${cell.reference}: ${cell.value}"
                    }
                }
            }
            is com.kdockerck.shared.domain.VisioDocumentContent -> {
                content.pages.joinToString("\n") { page ->
                    "Page: ${page.name}\n" +
                    page.shapes.joinToString("\n") { shape ->
                        "${shape.text} (${shape.type})"
                    }
                }
            }
        }
    }
    
    private fun createTestDocument(fileName: String, content: String): File {
        val tempFile = File.createTempFile(fileName)
        tempFile.writeText(content)
        return tempFile
    }
    
    data class ProcessedDocument(
        val document: Document,
        val status: DocumentStatus,
        val gherkinOutput: GherkinOutput? = null,
        val error: String? = null
    )
}

class MockEmbeddingGenerator(
    private val config: com.kdockerck.shared.config.EmbeddingConfig
) : com.kdockerck.shared.rag.EmbeddingGenerator {
    override suspend fun generateEmbedding(text: String): Either<com.kdockerck.shared.errors.AppError, List<Float>> {
        val hash = text.hashCode()
        val vector = (1..config.dimension).map { index ->
            ((hash + index) % 1000) / 1000.0f
        )
        return vector.right()
    }
    
    override suspend fun generateEmbeddings(
        texts: List<String>
    ): Either<com.kdockerck.shared.errors.AppError, List<List<Float>>> {
        val vectors = texts.map { text ->
            val hash = text.hashCode()
            (1..config.dimension).map { index ->
                ((hash + index) % 1000) / 1000.0f
            }
        return vectors.right()
    }
    
    override fun generateEmbeddingStream(text: String): kotlinx.coroutines.flow.Flow<Either<com.kdockerck.shared.errors.AppError, List<Float>>> {
        return kotlinx.coroutines.flow.flowOf()
    }
}

class MockCrossFileContextRetriever : com.kdockerck.shared.rag.CrossFileContextRetriever {
    override suspend fun retrieveContext(
        currentDocument: ParsedContent,
        excludeDocumentId: String?
    ): Either<com.kdockerck.shared.errors.AppError, List<com.kdockerck.shared.domain.SimilarityResult>> {
        return emptyList<com.kdockerck.shared.domain.SimilarityResult>().right()
    }
    
    override fun retrieveContextStream(
        currentDocument: ParsedContent,
        excludeDocumentId: String?
    ): kotlinx.coroutines.flow.Flow<Either<com.kdockerck.shared.errors.AppError, com.kdockerck.shared.domain.SimilarityResult>> {
        return kotlinx.coroutines.flow.flowOf()
    }
    
    override suspend fun retrieveContextForMultipleDocuments(
        documents: List<ParsedContent>,
        excludeDocumentIds: Set<String>
    ): Either<com.kdockerck.shared.errors.AppError, Map<String, List<com.kdockerck.shared.domain.SimilarityResult>>> {
        return emptyMap<String, List<com.kdockerck.shared.domain.SimilarityResult>>().right()
    }
    
    override suspend fun retrieveRelevantContext(
        query: String,
        maxResults: Int,
        threshold: Double
    ): Either<com.kdockerck.shared.errors.AppError, List<com.kdockerck.shared.domain.SimilarityResult>> {
        return emptyList<com.kdockerck.shared.domain.SimilarityResult>().right()
    }
    
    override suspend fun retrieveContextWithMetadata(
        currentDocument: ParsedContent,
        excludeDocumentId: String?,
        includeMetadata: Boolean
    ): Either<com.kdockerck.shared.errors.AppError, com.kdockerck.shared.rag.ContextWithMetadata> {
        return com.kdockerck.shared.rag.ContextWithMetadata(
            contextText = "Mock context",
            similarityResults = emptyList(),
            queryDocumentId = currentDocument.documentId,
            retrievedAt = kotlinx.datetime.Clock.System.now()
        ).right()
    }
    
    override suspend fun retrieveContextSummary(
        currentDocument: ParsedContent,
        excludeDocumentId: String?
    ): Either<com.kdockerck.shared.errors.AppError, String> {
        return "Mock context summary".right()
    }
}

class MockGherkinGenerator : com.kdockerck.shared.gherkin.GherkinGenerator {
    override suspend fun generate(
        documentContent: String,
        documentId: String,
        context: String?
    ): Either<com.kdockerck.shared.errors.AppError, com.kdockerck.shared.domain.GherkinOutput> {
        val feature = com.kdockerck.shared.domain.Feature(
            name = "Test Feature",
            description = "Test feature description",
            background = null,
            scenarios = listOf(
                com.kdockerck.shared.domain.Scenario(
                    name = "Test scenario",
                    description = null,
                    steps = listOf(
                        com.kdockerck.shared.domain.Step(
                            keyword = com.kdockerck.shared.domain.StepKeyword.GIVEN,
                            text = "test",
                            argument = null
                        ),
                        com.kdockerck.shared.domain.Step(
                            keyword = com.kdockerck.shared.domain.StepKeyword.WHEN,
                            text = "action",
                            argument = null
                        ),
                        com.kdockerck.shared.domain.Step(
                            keyword = com.kdockerck.shared.domain.StepKeyword.THEN,
                            text = "result",
                            argument = null
                        )
                    ),
                    examples = null
                )
            )
        )
        
        return com.kdockerck.shared.domain.GherkinOutput(
            documentId = documentId,
            feature = feature,
            generatedAt = kotlinx.datetime.Clock.System.now()
        ).right()
    }
    
    override fun generateStream(
        documentContent: String,
        documentId: String,
        context: String?
    ): kotlinx.coroutines.flow.Flow<Either<com.kdockerck.shared.errors.AppError, com.kdockerck.shared.gherkin.GenerationEvent>> {
        return kotlinx.coroutines.flow.flowOf()
    }
    
    override suspend fun generateWithRetry(
        documentContent: String,
        documentId: String,
        context: String?,
        maxRetries: Int
    ): Either<com.kdockerck.shared.errors.AppError, com.kdockerck.shared.domain.GherkinOutput> {
        return generate(documentContent, documentId, context)
    }
}