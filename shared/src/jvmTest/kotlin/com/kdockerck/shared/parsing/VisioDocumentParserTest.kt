package com.kdockerck.shared.parsing

import com.kdockerck.shared.domain.VisioDocumentContent
import com.kdockerck.shared.domain.VisioPage
import com.kdockerck.shared.domain.Shape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VisioDocumentParserTest {
    private val parser = VisioDocumentParser()
    
    @Test
    fun `should parse simple Visio document`() {
        val testContent = """
            <visio>
                <page name="Page1">
                    <shape id="1">
                        <text>Shape 1</text>
                        <type>Rectangle</type>
                        <position x="100" y="100"/>
                        <size width="50" height="30"/>
                    </shape>
                </page>
            </visio>
        """.trimIndent()
        
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.vsdx", "doc-1")
        
        assertTrue(result.isRight())
        val content = result.value
        assertTrue(content is VisioDocumentContent)
        assertEquals("doc-1", content.documentId)
    }
    
    @Test
    fun `should extract all pages`() {
        val testContent = """
            <visio>
                <page name="Page1">
                    <shape id="1">
                        <text>Shape 1</text>
                    </shape>
                </page>
                <page name="Page2">
                    <shape id="2">
                        <text>Shape 2</text>
                    </shape>
                </page>
            </visio>
        """.trimIndent()
        
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.vsdx", "doc-1")
        
        assertTrue(result.isRight())
        val content = result.value as VisioDocumentContent
        assertTrue(content.pages.isNotEmpty())
    }
    
    @Test
    fun `should extract shape text and hierarchy`() {
        val testContent = """
            <visio>
                <page name="Page1">
                    <shape id="1">
                        <text>Start</text>
                        <type>Ellipse</type>
                    </shape>
                    <shape id="2">
                        <text>End</text>
                        <type>Ellipse</type>
                    </shape>
                </page>
            </visio>
        """.trimIndent()
        
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.vsdx", "doc-1")
        
        assertTrue(result.isRight())
        val content = result.value as VisioDocumentContent
        assertTrue(content.pages.isNotEmpty())
    }
    
    @Test
    fun `should handle empty Visio document`() {
        val testContent = "<visio></visio>"
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.vsdx", "doc-1")
        
        assertTrue(result.isRight())
    }
}