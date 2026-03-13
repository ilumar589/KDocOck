package com.kdockerck.shared.gherkin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GherkinPromptTemplatesTest {
    
    @Test
    fun `should generate base prompt`() {
        val prompt = GherkinPromptTemplates.generatePrompt(
            documentContent = "Test document content",
            context = null
        )
        
        assertTrue(prompt.contains("Document Content:"))
        assertTrue(prompt.contains("Test document content"))
        assertTrue(prompt.contains("Generate a Gherkin feature file"))
    }
    
    @Test
    fun `should generate prompt with context`() {
        val prompt = GherkinPromptTemplates.generatePrompt(
            documentContent = "Test document",
            context = "Previous context"
        )
        
        assertTrue(prompt.contains("Context from Previous Documents:"))
        assertTrue(prompt.contains("Previous context"))
    }
    
    @Test
    fun `should generate prompt with examples`() {
        val testData = listOf(
            mapOf("username" to "user1", "password" to "pass1"),
            mapOf("username" to "user2", "password" to "pass2")
        )
        
        val prompt = GherkinPromptTemplates.generatePromptWithExamples(
            documentContent = "Test document",
            testData = testData,
            context = null
        )
        
        assertTrue(prompt.contains("Test Data Examples:"))
        assertTrue(prompt.contains("Example 1:"))
        assertTrue(prompt.contains("username: user1"))
        assertTrue(prompt.contains("Generate a Gherkin feature file with Scenario Outlines"))
    }
    
    @Test
    fun `should generate prompt for Excel`() {
        val cellData = mapOf(
            "A1" to "Username",
            "B1" to "Password",
            "A2" to "john",
            "B2" to "secret"
        )
        
        val prompt = GherkinPromptTemplates.generatePromptForExcel(
            worksheetName = "Users",
            cellData = cellData,
            context = null
        )
        
        assertTrue(prompt.contains("Excel Worksheet: Users"))
        assertTrue(prompt.contains("Cell Data:"))
        assertTrue(prompt.contains("A1: Username"))
        assertTrue(prompt.contains("Generate a Gherkin feature file from Excel worksheet data"))
    }
    
    @Test
    fun `should generate prompt for Visio`() {
        val shapes = listOf(
            GherkinPromptTemplates.ShapeInfo("Start", "Ellipse"),
            GherkinPromptTemplates.ShapeInfo("End", "Ellipse")
        )
        
        val connections = listOf(
            GherkinPromptTemplates.ConnectionInfo("Start", "End")
        )
        
        val prompt = GherkinPromptTemplates.generatePromptForVisio(
            pageName = "Flowchart",
            shapes = shapes,
            connections = connections,
            context = null
        )
        
        assertTrue(prompt.contains("Visio Page: Flowchart"))
        assertTrue(prompt.contains("Shapes:"))
        assertTrue(prompt.contains("- Start (Ellipse)"))
        assertTrue(prompt.contains("Connections:"))
        assertTrue(prompt.contains("- Start -> End"))
    }
    
    @Test
    fun `should generate prompt for Word`() {
        val paragraphs = listOf(
            GherkinPromptTemplates.ParagraphInfo("Introduction", 0),
            GherkinPromptTemplates.ParagraphInfo("Details", 1)
        )
        
        val tables = listOf(
            GherkinPromptTemplates.TableInfo("Users", listOf(
                listOf("Name", "Email"),
                listOf("John", "john@example.com")
            ))
        )
        
        val prompt = GherkinPromptTemplates.generatePromptForWord(
            paragraphs = paragraphs,
            tables = tables,
            context = null
        )
        
        assertTrue(prompt.contains("Word Document Content:"))
        assertTrue(prompt.contains("Paragraphs:"))
        assertTrue(prompt.contains("- Introduction"))
        assertTrue(prompt.contains("  Details"))
        assertTrue(prompt.contains("Tables:"))
        assertTrue(prompt.contains("  Table: Users"))
    }
}