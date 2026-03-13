package com.k.kdockerck.shared.rag

import com.kdockerck.shared.config.EmbeddingConfig
import com.kdockerck.shared.domain.Embedding
import com.kdockerck.shared.domain.EmbeddingMetadata
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VectorSimilaritySearchTest {
    private val testConfig = EmbeddingConfig(
        dimension = 768,
        model = "test-model",
        chunkSize = 512,
        chunkOverlap = 50,
        similarityThreshold = 0.5,
        maxResults = 5
    )
    
    @Test
    fun `should search similar vectors`() {
        val mockGenerator = MockEmbeddingGenerator(testConfig)
        val vectorStore = VectorStore(DatabaseConnectionManager.getInstance().value)
        val similaritySearch = VectorSimilaritySearch(
            DatabaseConnectionManager.getInstance().value,
            testConfig
        )
        
        val queryVector = listOf(0.1f, 0.2f, 0.3f)
        val result = similaritySearch.searchSimilar(queryVector)
        
        assertTrue(result.isRight())
    }
    
    @Test
    fun `should filter results by similarity threshold`() {
        val mockGenerator = MockEmbeddingGenerator(testConfig)
        val vectorStore = VectorStore(DatabaseConnectionktManager.getInstance().value)
        val similaritySearch = VectorSimilaritySearch(
            DatabaseConnectionManager.getInstance().value,
            testConfig
        )
        
        val queryVector = listOf(0.1f, 0.2f, 0.3f)
        val result = similaritySearch.searchSimilar(queryVector, threshold = 0.8)
        
        assertTrue(result.isRight())
        result.value.forEach { similarityResult ->
            assertTrue(similarityResult.score >= 0.8)
        }
    }
    
    @Test
    fun `should limit results`() {
        val mockGenerator = MockEmbeddingGenerator(testConfig)
        val vectorStore = VectorStore(DatabaseConnectionManager.getInstance().value)
        val similaritySearch = VectorSimilaritySearch(
            DatabaseConnectionManager.getInstance().value,
            testConfig
        )
        
        val queryVector = listOf(0.1f, 0.2f, 0.3f)
        val result = similaritySearch.searchSimilar(queryVector, limit = 3)
        
        assertTrue(result.isRight())
        assertTrue(result.value.size <= 3)
    }
    
    @Test
    fun `should search by document id`() {
        val mockGenerator = MockEmbeddingGenerator(testConfig)
        val vectorStore = VectorStore(DatabaseConnectionManager.getInstance().value)
        val similaritySearch = VectorSimilaritySearch(
            DatabaseConnectionManager.getInstance().value,
            testConfig
        )
        
        val queryVector = listOf(0.1f, 0.2f, 0.3f)
        val result = similaritySearch.searchSimilarByDocumentId(queryVector, "doc-1")
        
        assertTrue(result.isRight())
    }
    
    @Test
    fun `should search by source type`() {
        val mockGenerator = MockEmbeddingGenerator(testConfig)
        val vectorStore = VectorStore(DatabaseConnectionManager.getInstance().value)
        val similaritySearch = VectorSimilaritySearch(
            DatabaseConnectionManager.getInstance().value,
            testConfig
        )
        
        val queryVector = listOf(0.1f, 0.2f, 0.3f)
        val result = similaritySearch.searchSimilarBySourceType(queryVector, "word")
        
        assertTrue(result.isRight())
    }
}