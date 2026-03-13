package com.k.kdockerck.shared.database

import com.kdockerck.shared.config.DatabaseConfig
import com.kdockerck.shared.domain.Embedding
import com.kdockerck.shared.domain.EmbeddingMetadata
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VectorStoreTest {
    private val testConfig = DatabaseConfig(
        url = "r2dbc:postgresql://localhost:5432/kdockerck_test",
        username = "kdockerck",
        password = "kdockerck"
    )
    
    private val dbManager = DatabaseConnectionManager.initialize(testConfig)
    private val vectorStore = VectorStore(dbManager)
    
    @Test
    fun `should store embedding successfully`() {
        val embedding = Embedding(
            id = "test-1",
            documentId = "doc-1",
            vector = listOf(0.1f, 0.2f, 0.3f),
            text = "Test text",
            metadata = EmbeddingMetadata(
                chunkIndex = 0,
                totalChunks = 1,
                sourceType = "test"
            ),
            createdAt = Clock.System.now()
        )
        
        val result = vectorStore.storeEmbedding(embedding)
        
        assertTrue(result.isRight())
    }
    
    @Test
    fun `should retrieve embedding by id`() {
        val embedding = Embedding(
            id = "test-2",
            documentId = "doc-2",
            vector = listOf(0.4f, 0.5f, 0.6f),
            text = "Test text 2",
            metadata = EmbeddingMetadata(
                chunkIndex = 0,
                totalChunks = 1,
                sourceType = "test"
            ),
            createdAt = Clock.System.now()
        )
        
        vectorStore.storeEmbedding(embedding)
        val result = vectorStore.getEmbeddingById("test-2")
        
        assertTrue(result.isRight())
        val retrieved = result.value
        assertEquals("test-2", retrieved?.id)
        assertEquals("Test text 2", retrieved?.text)
    }
    
    @Test
    fun `should retrieve embeddings by document id`() {
        val embedding1 = Embedding(
            id = "test-3",
            documentId = "doc-3",
            vector = listOf(0.1f, 0.2f, 0.3f),
            text = "Chunk 1",
            metadata = EmbeddingMetadata(
                chunkIndex = 0,
                totalChunks = 2,
                sourceType = "test"
            ),
            createdAt = Clock.System.now()
        )
        
        val embedding2 = Embedding(
            id = "test-4",
            documentId = "doc-3",
            vector = listOf(0.4f, 0.5f, 0.6f),
            text = "Chunk 2",
            metadata = EmbeddingMetadata(
                chunkIndex = 1,
                totalChunks = 2,
                sourceType = "test"
            ),
            createdAt = Clock.System.now()
        )
        
        vectorStore.storeEmbedding(embedding1)
        vectorStore.storeEmbedding(embedding2)
        
        val result = vectorStore.getEmbeddingsByDocumentId("doc-3")
        
        assertTrue(result.isRight())
        assertEquals(2, result.value.size)
    }
    
    @Test
    fun `should delete embedding`() {
        val embedding = Embedding(
            id = "test-5",
            documentId = "doc-5",
            vector = listOf(0.1f, 0.2f, 0.3f),
            text = "Test text",
            metadata = EmbeddingMetadata(
                chunkIndex = 0,
                totalChunks = 1,
                sourceType = "test"
            ),
            createdAt = Clock.System.now()
        )
        
        vectorStore.storeEmbedding(embedding)
        val deleteResult = vectorStore.deleteEmbedding("test-5")
        val getResult = vectorStore.getEmbedding("test-5")
        
        assertTrue(deleteResult.isRight())
        assertTrue(getResult.isRight())
        assertEquals(null, getResult.value)
    }
    
    @Test
    fun `should count embeddings`() {
        val result = vectorStore.countEmbeddings()
        
        assertTrue(result.isRight())
        assertTrue(result.value >= 0)
    }
}