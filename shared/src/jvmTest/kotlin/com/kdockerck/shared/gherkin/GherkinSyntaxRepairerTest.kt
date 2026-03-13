package com.kdockerck.shared.gherkin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GherkinSyntaxRepairerTest {
    
    @Test
    fun `should repair feature keyword capitalization`() {
        val repairer = GherkinSyntaxRepairer()
        val text = "feature: Test Feature"
        
        val result = repairer.repair(text)
        
        assertTrue(result.isRight())
        assertEquals("Feature: Test Feature", result.value)
    }
    
    @Test
    fun `should repair scenario keyword capitalization`() {
        val repairer = GherkinSyntaxRepairer()
        val text = "scenario: Test Scenario"
        
        val result = repairer.repair(text)
        
        assertTrue(result.isRight())
        assertEquals("Scenario: Test Scenario", result.value)
    }
    
    @Test
    fun `should repair step keywords capitalization`() {
        val repairer = GherkinSyntaxRepairer()
        val text = """
            given test
            when action
            then result
        """.trimIndent()
        
        val result = repairer.repair(text)
        
        assertTrue(result.isRight())
        assertTrue(result.value.contains("Given test"))
        assertTrue(result.value.contains("When action"))
        assertTrue(result.value.contains("Then result"))
    }
    
    @Test
    fun `should repair indentation`() {
        val repairer = GherkinSyntaxRepairer()
        val text = """
            Feature: Test
            Scenario: Test scenario
            Given test
            When action
            Then result
        """.trimIndent()
        
        val result = repairer.repair(text)
        
        assertTrue(result.isRight())
        val lines = result.value.lines()
        assertTrue(lines[0].startsWith("Feature:"))
        assertTrue(lines[1].startsWith("Scenario:"))
        assertTrue(lines[2].startsWith("  Given"))
        assertTrue(lines[3].startsWith("  When"))
        assertTrue(lines[4].startsWith("  Then"))
    }
    
    @Test
    fun `should remove excessive empty lines`() {
        val repairer = GherkinSyntaxRepairer()
        val text = """
            Feature: Test
            
            Scenario: Test scenario
            
            Given test
            
            When action
            
            Then result
        """.trimIndent()
        
        val result = repairer.repair(text)
        
        assertTrue(result.isRight())
        val lines = result.value.lines()
        val emptyLines = (lines.count { it.isBlank() })
        assertTrue(emptyLines < 3)
    }
    
    @Test
    fun `should repair colon spacing`() {
        val repairer = GherkinSyntaxRepairer()
        val text = """
            Feature:Test
            Scenario:Test scenario
            Given:test
        """.trimIndent()
        
        val result = repairer.repair(text)
        
        assertTrue(result.isRight())
        assertTrue(result.value.contains("Feature: Test"))
        assertTrue(result.value.contains("Scenario: Test scenario"))
        assertTrue(result.value.contains("Given: test"))
    }
    
    @Test
    fun `should repair duplicate keywords`() {
        val repairer = GherkinSyntaxRepairer()
        val text = """
            Feature: Test
            Scenario: Test scenario
              Given  test
              When  action
              Then  result
        """.trimIndent()
        
        val result = repairer.repair(text)
        
        assertTrue(result.isRight())
        assertFalse(result.value.contains("Given  test"))
        assertTrue(result.value.contains("Given test"))
    }
    
    @Test
    fun `should detect repairable issues`() {
        val repairer = GherkinSyntaxRepairer()
        val text = "feature: Test\nscenario: Test scenario\ngiven test"
        
        val canRepair = repairer.canRepair(text)
        
        assertTrue(canRepair)
    }
    
    @Test
    fun `should provide repair suggestions`() {
        val repairer = GherkinSyntaxRepairer()
        val text = "feature: Test\nscenario: Test scenario\ngiven test"
        
        val suggestions = repairer.getRepairSuggestions(text)
        
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.contains("Capitalize") })
    }
    
    @Test
    fun `should generate repair report`() {
        val repairer = GherkinSyntaxRepairer()
        val text = "feature: Test\nscenario: Test scenario\ngiven test"
        
        val result = repairer.repairWithReport(text)
        
        assertTrue(result.isRight())
        val (repairedText, repairs) = result.value
        assertTrue(repairs.isNotEmpty())
        assertTrue(repairedText.contains("Feature:"))
    }
}