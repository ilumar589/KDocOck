package com.kdockerck.shared.gherkin

object GherkinPromptTemplates {
    
    val BASE_SYSTEM_PROMPT = """
        You are a Gherkin feature file generation expert. Your task is to convert document content into well-structured Gherkin feature files that can be used with Cucumber or similar BDD testing frameworks.
        
        Guidelines for Gherkin generation:
        1. Create meaningful and descriptive Feature names
        2. Write clear, concise Scenario names that capture the behavior
        3. Use Given-When-Then format for steps
        4. Make steps declarative and focused on business logic
        5. Include Background sections for common setup steps
        6. Use Examples tables for test data variations
        7. Ensure proper indentation and formatting
        8. Follow Gherkin syntax strictly
        9. Use And/But keywords appropriately for readability
        10. Keep scenarios independent and self-contained
        
        Gherkin Keywords to use:
        - Feature: High-level description of the feature
        - Scenario: Specific test case
        - Given: Initial context or setup
        - When: Action or event
        - Then: Expected outcome
        - And: Additional Given/When/Then steps
        - But: Alternative outcome
        - Background: Common steps for all scenarios
        - Examples: Data table for scenario outlines
        
        Generate only valid Gherkin syntax. Do not include any explanations or comments outside the Gherkin structure.
    """.trimIndent()
    
    fun generatePrompt(
        documentContent: String,
        context: String? = null
    ): String {
        return buildString {
            appendLine("Document Content:")
            appendLine(documentContent)
            appendLine()
            
            if (!context.isNullOrBlank()) {
                appendLine("Context from Previous Documents:")
                appendLine(context)
                appendLine()
            }
            
            appendLine("Task: Generate a Gherkin feature file from the above document content.")
            appendLine("Include relevant context from previous documents to maintain consistency.")
            appendLine("Ensure the generated Gherkin is syntactically valid and follows best practices.")
        }
    }
    
    fun generatePromptWithExamples(
        documentContent: String,
        testData: List<Map<String, String>>,
        context: String? = null
    ): String {
        return buildString {
            appendLine("Document Content:")
            appendLine(documentContent)
            appendLine()
            
            if (!context.isNullOrBlank()) {
                appendLine("Context from Previous Documents:")
                appendLine(context)
                appendLine()
            }
            
            appendLine("Test Data Examples:")
            testData.forEachIndexed { index, data ->
                appendLine("Example ${index + 1}:")
                data.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }
                appendLine()
            }
            
            appendLine("Task: Generate a Gherkin feature file with Scenario Outlines using the provided test data.")
            appendLine("Use Examples tables to parameterize scenarios with the test data.")
        }
    }
    
    fun generatePromptForExcel(
        worksheetName: String,
        cellData: Map<String, String>,
        context: String? = null
    ): String {
        return buildString {
            appendLine("Excel Worksheet: $worksheetName")
            appendLine()
            
            appendLine("Cell Data:")
            cellData.forEach { (reference, value) ->
                appendLine("  $reference: $value")
            }
            appendLine()
            
            if (!context.isNullOrBlank()) {
                appendLine("Context from Previous Documents:")
                appendLine(context)
                appendLine()
            }
            
            appendLine("Task: Generate a Gherkin feature file from the Excel worksheet data.")
            appendLine("Use Examples tables to represent the tabular data.")
            appendLine("Create scenarios that test the functionality described by the data.")
        }
    }
    
    fun generatePromptForVisio(
        pageName: String,
        shapes: List<ShapeInfo>,
        connections: List<ConnectionInfo>,
        context: String? = null
    ): String {
        return buildString {
            appendLine("Visio Page: $pageName")
            appendLine()
            
            appendLine("Shapes:")
            shapes.forEach { shape ->
                appendLine("  - ${shape.text} (${shape.type})")
            }
            appendLine()
            
            if (connections.isNotEmpty()) {
                appendLine("Connections:")
                connections.forEach { connection ->
                    appendLine("  - ${connection.fromShape} -> ${connection.toShape}")
                }
                appendLine()
            }
            
            if (!context.isNullOrBlank()) {
                appendLine("Context from Previous Documents:")
                appendLine(context)
                appendLine()
            }
            
            appendLine("Task: Generate a Gherkin feature file from the Visio diagram.")
            appendLine("Represent the flowchart/process as scenarios with Given-When-Then steps.")
            appendLine("Use the shape text to describe the steps and outcomes.")
        }
    }
    
    fun generatePromptForWord(
        paragraphs: List<ParagraphInfo>,
        tables: List<TableInfo>,
        context: String? = null
    ): String {
        return buildString {
            appendLine("Word Document Content:")
            appendLine()
            
            if (paragraphs.isNotEmpty()) {
                appendLine("Paragraphs:")
                paragraphs.forEach { paragraph ->
                    val indent = "  ".repeat(paragraph.level)
                    appendLine("$indent- ${paragraph.text}")
                }
                appendLine()
            }
            
            if (tables.isNotEmpty()) {
                appendLine("Tables:")
                tables.forEach { table ->
                    appendLine("  Table: ${table.name}")
                    table.rows.forEach { row ->
                        appendLine("    ${row.joinToString(" | ")}")
                    }
                }
                appendLine()
            }
            
            if (!context.isNullOrBlank()) {
                appendLine("Context from Previous Documents:")
                appendLine(context)
                appendLine()
            }
            
            appendLine("Task: Generate a Gherkin feature file from the Word document.")
            appendLine("Extract requirements and scenarios from the paragraphs and tables.")
            appendLine("Use tables as Examples data where appropriate.")
        }
    }
    
    data class ShapeInfo(
        val text: String,
        val type: String
    )
    
    data class ConnectionInfo(
        val fromShape: String,
        val toShape: String
    )
    
    data class ParagraphInfo(
        val text: String,
        val level: Int
    )
    
    data class TableInfo(
        val name: String,
        val rows: List<List<String>>
    )
}