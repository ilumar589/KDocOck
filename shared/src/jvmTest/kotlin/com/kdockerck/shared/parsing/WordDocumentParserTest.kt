package com.kdockerck.shared.parsing

import com.kdockerck.shared.domain.WordDocumentContent
import com.kdockerck.shared.domain.Paragraph
import com.kdockerck.shared.domain.Table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordDocumentParserTest {
    private val parser = WordDocumentParser()
    
    @Test
    fun `should parse simple Word document`() {
        val testContent = """
            <document>
                <paragraph>Test Paragraph 1</paragraph>
                <paragraph>Test Paragraph 2</paragraph>
            </document>
        """.trimIndent()
        
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.docx", "doc-1")
        
        assertTrue(result.isRight())
        val content = result.value
        assertTrue(content is WordDocumentContent)
        assertEquals("doc-1", content.documentId)
    }
    
    @Test
    fun `should extract paragraphs in order`() {
        val testContent = """
            <document>
                <paragraph>First paragraph</paragraph>
                <paragraph>Second paragraph</paragraph>
                <paragraph>Third paragraph</paragraph>
            </document>
        """.trimIndent()
        
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.docx", "doc-1")
        
        assertTrue(result.isRight())
        val content = result.value as WordDocumentContent
        assertTrue(content.paragraphs.isNotEmpty())
    }
    
    @Test
    fun `should handle empty document`() {
        val testContent = "<document></document>"
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.docx", "doc-1")
        
        assertTrue(result.isRight())
    }
    
    @Test
    fun `should handle invalid file format`() {
        val testContent = "Not a valid Word document"
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.txt", "doc-1")
        
        assertTrue(result.isLeft())
    }
}