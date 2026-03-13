package com.kdockerck.shared.gherkin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.domain.Feature
import com.kdockerck.shared.domain.Scenario
import com.kdockerck.shared.domain.Step
import com.kdockerck.shared.domain.StepKeyword
import com.kdockerck.shared.errors.ValidationError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GherkinValidatorTest {
    
    @Test
    fun `should validate valid feature`() {
        val validator = GherkinValidator()
        val feature = Feature(
            name = "User Authentication",
            description = "User can authenticate with valid credentials",
            background = null,
            scenarios = listOf(
                Scenario(
                    name = "Successful login",
                    description = null,
                    steps = listOf(
                        Step(StepKeyword.GIVEN, "user is on login page"),
                        Step(StepKeyword.WHEN, "user enters valid credentials"),
                        Step(StepKeyword.THEN, "user is redirected to dashboard")
                    ),
                    examples = null
                )
            )
        )
        
        val result = validator.validate(feature)
        
        assertTrue(result.isRight())
        assertEquals("User Authentication", result.value.name)
    }
    
    @Test
    fun `should reject feature with blank name`() {
        val validator = GherkinValidator()
        val feature = Feature(
            name = "",
            description = null,
            background = null,
            scenarios = listOf(
                Scenario(
                    name = "Test scenario",
                    description = null,
                    steps = listOf(Step(StepKeyword.GIVEN, "test")),
                    examples = null
                )
            )
        )
        
        val result = validator.validate(feature)
        
        assertTrue(result.isLeft())
        assertTrue(result.value is ValidationError)
    }
    
    @Test
    fun `should reject feature with no scenarios`() {
        val validator = GherkinValidator()
        val feature = Feature(
            name = "Test Feature",
            description = null,
            background = null,
            scenarios = emptyList()
        )
        
        val result = validator.validate(feature)
        
        assertTrue(result.isLeft())
        assertTrue(result.value is ValidationError)
    }
    
    @Test
    fun `should reject scenario with blank name`() {
        val validator = GherkinValidator()
        val feature = Feature(
            name = "Test Feature",
            description = null,
            background = null,
            scenarios = listOf(
                Scenario(
                    name = "",
                    description = null,
                    steps = listOf(Step(StepKeyword.GIVEN, "test")),
                    examples = null
                )
            )
        )
        
        val result = validator.validate(feature)
        
        assertTrue(result.isLeft())
        assertTrue(result.value is ValidationError)
    }
    
    @Test
    fun `should reject scenario with no steps`() {
        val validator = GherkinValidator()
        val feature = Feature(
            name = "Test Feature",
            description = null,
            background = null,
            scenarios = listOf(
                Scenario(
                    name = "Test scenario",
                    description = null,
                    steps = emptyList(),
                    examples = null
                )
            )
        )
        
        val result = validator.validate(feature)
        
        assertTrue(result.isLeft())
        assertTrue(result.value is ValidationError)
    }
    
    @Test
    fun `should reject step with blank text`() {
        val validator = GherkinValidator()
        val feature = Feature(
            name = "Test Feature",
            description = null,
            background = null,
            scenarios = listOf(
                Scenario(
                    name = "Test scenario",
                    description = null,
                    steps = listOf(Step(StepKeyword.GIVEN, "")),
                    examples = null
                )
            )
        )
        
        val result = validator.validate(feature)
        
        assertTrue(result.isLeft())
        assertTrue(result.value is ValidationError)
    }
    
    @Test
    fun `should reject first step with And keyword`() {
        val validator = GherkinValidator()
        val feature = Feature(
            name = "Test Feature",
            description = null,
            background = null,
            scenarios = listOf(
                Scenario(
                    name = "Test scenario",
                    description = null,
                    steps = listOf(Step(StepKeyword.AND, "test")),
                    examples = null
                )
            )
        )
        
        val result = validator.validate(feature)
        
        assertTrue(result.isLeft())
        assertTrue(result.value is ValidationError)
    }
    
    @Test
    fun `should validate Gherkin text`() {
        val validator = GherkinValidator()
        val text = """
            Feature: User Authentication
            
            Scenario: Successful login
              Given user is on login page
              When user enters valid credentials
              Then user is redirected to dashboard
        """.trimIndent()
        
        val result = validator.validateGherkinText(text)
        
        assertTrue(result.isRight())
    }
    
    @Test
    fun `should reject invalid Gherkin text`() {
        val validator = GherkinValidator()
        val text = """
            feature: User Authentication
            
            Scenario: 
              Given user is on login page
        """.trimIndent()
        
        val result = validator.validateGherkinText(text)
        
        assertTrue(result.isLeft())
    }
    
    @Test
    fun `should validate syntax`() {
        val validator = GherkinValidator()
        val text = """
            Feature: Test
            
            Scenario: Test scenario
              Given test
        """.trimIndent()
        
        val result = validator.validateSyntax(text)
        
        assertTrue(result.isRight())
        assertTrue(result.value)
    }
    
    @Test
    fun `should generate validation summary`() {
        val validator = GherkinValidator()
        val feature = Feature(
            name = "Test Feature",
            description = null,
            background = null,
            scenarios = listOf(
                Scenario(
                    name = "Test scenario",
                    description = null,
                    steps = listOf(
                        Step(StepKeyword.GIVEN, "test"),
                        Step(StepKeyword.WHEN, "action"),
                        Step(StepKeyword.THEN, "result")
                    ),
                    examples = null
                )
            )
        )
        
        val summary = validator.getValidationSummary(feature)
        
        assertTrue(summary.contains("Gherkin Validation Summary"))
        assertTrue(summary.contains("✓ No issues found"))
    }
}