package com.kdockerck.shared.parsing

import com.kdockerck.shared.domain.ExcelDocumentContent
import com.kdockerck.shared.domain.Worksheet
import com.kdockerck.shared.domain.Cell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExcelDocumentParserTest {
    private val parser = ExcelDocumentParser()
    
    @Test
    fun `should parse simple Excel document`() {
        val testContent = """
            <workbook>
                <worksheet name="Sheet1">
                    <row>
                        <cell>A1</cell>
                        <cell>B1</cell>
                    </row>
                </worksheet>
            </workbook>
        """.trimIndent()
        
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.xlsx", "doc-1")
        
        assertTrue(result.isRight())
        val content = result.value
        assertTrue(content is ExcelDocumentContent)
        assertEquals("doc-1", content.documentId)
    }
    
    @Test
    fun `should extract all worksheets`() {
        val testContent = """
            <workbook>
                <worksheet name="Sheet1">
                    <row>
                        <cell>A1</cell>
                    </row>
                </worksheet>
                <worksheet name="Sheet2">
                    <row>
                        <cell>B1</cell>
                    </row>
                </worksheet>
            </workbook>
        """.trimIndent()
        
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.xlsx", "doc-1")
        
        assertTrue(result.isRight())
        val content = result.value as ExcelDocumentContent
        assertTrue(content.worksheets.isNotEmpty())
    }
    
    @Test
    fun `should extract cell references`() {
        val testContent = """
            <workbook>
                <worksheet name="Sheet1">
                    <row>
                        <cell>A1</cell>
                        <cell>B1</cell>
                    </row>
                    <row>
                        <cell>A2</cell>
                        <cell>B2</cell>
                    </row>
                </worksheet>
            </workbook>
        """.trimIndent()
        
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.xlsx", "doc-1")
        
        assertTrue(result.isRight())
        val content = result.value as ExcelDocumentContent
        assertTrue(content.worksheets.isNotEmpty())
    }
    
    @Test
    fun `should handle empty workbook`() {
        val testContent = "<workbook></workbook>"
        val inputStream = testContent.byteInputStream()
        val result = parser.parse(inputStream, "test.xlsx", "doc-1")
        
        assertTrue(result.isRight())
    }
}