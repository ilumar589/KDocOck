package com.k.kdockerck.shared.rag

import com.kdockerck.shared.config.EmbeddingConfig
import com.kdockerck.shared.domain.WordDocumentContent
import com.kdockerck.shared.domain.Paragraph
import com.kdockerck.shared.domain.ContentMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrossFileContextRetrieverTest {
    private val testConfig = EmbeddingConfig(
        dimension = 768,
        model = "test-model",
        chunkSize = 512,
        chunkOverlap = 50,
        similarityThreshold = 0.5,
        maxResults = 5
    )
    
    @Test
    fun `should retrieve cross-file context`() {
        val mockGenerator = MockEmbeddingGenerator(testConfig)
        val vectorStore = VectorStore(DatabaseConnectionManager.getInstance().value)
        val similaritySearch = VectorSimilaritySearch(
            DatabaseConnectionManager.getInstance().value,
            testConfig
        )
        val contextRetriever = CrossFileContextRetriever(
            mockGenerator,
            similaritySearch,
            testConfig
        )
        
        val document = WordDocumentContent(
            documentId = "doc-test",
            metadata = ContentMetadata(),
            paragraphs = listOf(
                Paragraph(
                    id = "p1",
                    text = "Test paragraph content",
                    style = null,
                    level = 0
                )
            ),
            tables = emptyList()
        )
        
        val result = contextRetriever.retrieveContext(document)
        
        assertTrue(result.isRight())
    }
    
    @Test
    fun `should exclude current document from context`() {
        val mockGenerator = MockEmbeddingGenerator(testConfig)
        val vectorStore = VectorStore(DatabaseConnectionManager.getInstance().value)
        val similaritySearch = VectorSimilaritySearch(
            DatabaseConnectionManager.getInstance().value,
            testConfig
        )
        val contextRetriever = CrossFileContextRetriever(
            mockGenerator,
            similaritySearch,
            testConfig
        )
        
        val document = WordDocumentContent(
            documentId = "doc-exclude",
            metadata = ContentMetadata(),
            paragraphs = listOf(
                Paragraph(
                    id = "p1",
                    text = "Test paragraph content",
                    style = null,
                    level = 0
                )
            ),
            tables = emptyList()
        )
        
        val result = contextRetriever.retrieveContext(document, excludeDocumentId = "doc-exclude")
        
        assertTrue(result.isRight())
        result.value.forEach { similarityResult ->
            assertTrue(similarityResult.embedding.documentId != "doc-exclude")
        }
    }
    
    @Test
    fun `should retrieve context with metadata`() {
        val mockGenerator = MockEmbeddingGenerator(testConfig)
        val vectorStore = VectorStore(DatabaseConnectionManager.getInstance().value)
        val similaritySearch = VectorSimilaritySearch(
            DatabaseConnectionManager.getInstance().value,
            testConfig
        )
        val contextRetriever = CrossFileContextRetriever(
            mockGenerator,
            similaritySearch,
            testConfig
        )
        
        val document = WordDocumentContent(
            documentId = "doc-metadata",
            metadata = ContentMetadata(),
            paragraphs = listOf(
                Paragraph(
                    id = "p1",
                    text = "Test paragraph content",
                    style = null,
                    level = 0
                )
            ),
            tables = emptyList()
        )
        
        val result = contextRetriever.retrieveContextWithMetadata(document)
        
        assertTrue(result.isRight())
        assertEquals("doc-metadata", result.value.queryDocumentId)
        assertTrue(result.value.contextText.isNotEmpty())
    }
    
    @Test
    fun `should retrieve context summary`() {
        val mockGenerator = MockEmbeddingGenerator(testConfig)
        val vectorStore = VectorStore(DatabaseConnectionManager.getInstance().value)
        val similaritySearch = VectorSimilaritySearch(
            DatabaseConnectionManager.getInstance().value,
            testConfig
        )
        val contextRetriever = CrossFileContextRetriever(
            mockGenerator,
            similaritySearch,
            testConfig
        )
        
        val document = WordDocumentContent(
            documentId = "doc-summary",
            metadata = ContentMetadata(),
            paragraphs = listOf(
                Paragraph(
                    id = "p1",
                    text = "Test paragraph content",
                    style = null,
                    level = 0
                )
            ),
            tables = emptyList()
        )
        
        val result = contextRetriever.retrieveContextSummary(document)
        
        assertTrue(result.isRight())
        assertTrue(result.value.contains("Cross-file context retrieved"))
    }
}