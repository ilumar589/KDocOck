package com.kdockerck.shared.gherkin

import arrow.core.Either
import arrow.core.right
import com.kdockerck.shared.domain.WordDocumentContent
import com.kdockerck.shared.domain.Paragraph
import com.kdockerck.shared.domain.ContentMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextInjectorTest {
    
    @Test
    fun `should inject context successfully`() {
        val contextRetriever = MockCrossFileContextRetriever()
        val contextInjector = ContextInjector(contextRetriever)
        
        val document = WordDocumentContent(
            documentId = "doc-1",
            metadata = ContentMetadata(),
            paragraphs = listOf(
                Paragraph(id = "p1", text = "Test content", style = null, level = 0)
            ),
            tables = emptyList()
        )
        
        val result = contextInjector.injectContext(document)
        
        assertTrue(result.isRight())
        assertTrue(result.value.contains("Cross-file context retrieved"))
    }
    
    @Test
    fun `should truncate long context`() {
        val contextRetriever = MockCrossFileContextRetriever()
        val contextInjector = ContextInjector(contextRetriever)
        
        val document = WordDocumentContent(
            documentId = "doc-1",
            metadata = ContentMetadata(),
            paragraphs = listOf(
                Paragraph(id = "p1", text = "Test content", style = null, level = 0)
            ),
            tables = emptyList()
        )
        
        val result = contextInjector.injectContext(document, maxContextLength = 100)
        
        assertTrue(result.isRight())
        assertTrue(result.value.length <= 120) // 100 + "... (context truncated)"
    }
    
    @Test
    fun `should inject context with metadata`() {
        val contextRetriever = MockCrossFileContextRetriever()
        val contextInjector = ContextInjector(contextRetriever)
        
        val document = WordDocumentContent(
            documentId = "doc-1",
            metadata = ContentMetadata(),
            paragraphs = listOf(
                Paragraph(id = "p1", text = "Test content", style = null, level = 0)
            ),
            tables = emptyList()
        )
        
        val result = contextInjector.injectContextWithMetadata(document)
        
        assertTrue(result.isRight())
        assertEquals("doc-1", result.value.queryDocumentId)
        assertTrue(result.value.contextText.isNotEmpty())
    }
    
    @Test
    fun `should inject relevant context`() {
        val contextRetriever = MockCrossFileContextRetriever()
        val contextInjector = ContextInjector(contextRetriever)
        
        val document = WordDocumentContent(
            documentId = "doc-1",
            metadata = ContentMetadata(),
            paragraphs = listOf(
                Paragraph(id = "p1", text = "Test content", style = null, level = 0)
            ),
            tables = emptyList()
        )
        
        val result = contextInjector.injectRelevantContext(document, "user authentication")
        
        assertTrue(result.isRight())
        assertTrue(result.value.contains("Relevant Context:"))
    }
    
    @Test
    fun `should format context for prompt`() {
        val contextInjector = ContextInjector(MockCrossFileContextRetriever())
        val context = "This is the context"
        
        val formatted = contextInjector.formatContextForPrompt(context)
        
        assertTrue(formatted.contains("---"))
        assertTrue(formatted.contains("Cross-File Context:"))
        assertTrue(formatted.contains("This is the context"))
        assertTrue(formatted.contains("End of Context"))
    }
    
    @Test
    fun `should format context with sources`() {
        val contextInjector = ContextInjector(MockCrossFileContextRetriever())
        val context = "This is the context"
        val sources = listOf("doc-1", "doc-2")
        
        val formatted = contextInjector.formatContextWithSources(context, sources)
        
        assertTrue(formatted.contains("---"))
        assertTrue(formatted.contains("Cross-File Context:"))
        assertTrue(formatted.contains("Sources:"))
        assertTrue(formatted.contains("- doc-1"))
        assertTrue(formatted.contains("- doc-2"))
    }
}

class MockCrossFileContextRetriever : CrossFileContextRetriever {
    override suspend fun retrieveContext(
        currentDocument: com.kdockerck.shared.domain.ParsedContent,
        excludeDocumentId: String?
    ): Either<com.kdockerck.shared.errors.AppError, List<com.kdockerck.shared.domain.SimilarityResult>> {
        return emptyList<com.kdockerck.shared.domain.SimilarityResult>().right()
    }
    
    override fun retrieveContextStream(
        currentDocument: com.kdockerck.shared.domain.ParsedContent,
        excludeDocumentId: String?
    ): kotlinx.coroutines.flow.Flow<Either<com.kdockerck.shared.errors.AppError, com.kdockerck.shared.domain.SimilarityResult>> {
        return kotlinx.coroutines.flow.flowOf()
    }
    
    override suspend fun retrieveContextForMultipleDocuments(
        documents: List<com.kdockerck.shared.domain.ParsedContent>,
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
        currentDocument: com.kdockerck.shared.domain.ParsedContent,
        excludeDocumentId: String?,
        includeMetadata: Boolean
    ): Either<com.kdockerck.shared.errors.AppError, ContextWithMetadata> {
        return ContextWithMetadata(
            contextText = "Cross-file context retrieved:\nNumber of relevant documents: 0\n",
            similarityResults = emptyList(),
            queryDocumentId = currentDocument.documentId,
            retrievedAt = kotlinx.datetime.Clock.System.now()
        ).right()
    }
    
    override suspend fun retrieveContextSummary(
        currentDocument: com.kdockerck.shared.domain.ParsedContent,
        excludeDocumentId: String?
    ): Either<com.kdockerck.shared.errors.AppError, String> {
        return """
            Cross-file context retrieved:
            Number of relevant documents: 0
            
        """.trimIndent().right()
    }
}