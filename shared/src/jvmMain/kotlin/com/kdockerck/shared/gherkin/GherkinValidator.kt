package com.kdockerck.shared.gherkin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.ValidationError
import com.kdockerck.shared.domain.Feature
import com.kdockerck.shared.domain.Scenario
import com.kdockerck.shared.domain.Step
import com.kdockerck.shared.domain.StepKeyword
import com.kdockerck.shared.domain.StepArgument

class GherkinValidator {
    
    fun validate(feature: Feature): Either<AppError, Feature> {
        val errors = mutableListOf<ValidationError>()
        
        if (feature.name.isBlank()) {
            errors.add(ValidationError(
                message = "Feature name cannot be blank",
                field = "feature.name"
            ))
        }
        
        if (feature.scenarios.isEmpty()) {
            errors.add(ValidationError(
                message = "Feature must have at least one scenario",
                field = "feature.scenarios"
            ))
        }
        
        for ((index, scenario) in feature.scenarios.withIndex()) {
            when (val result = validateScenario(scenario, index)) {
                is Either.Right -> {}
                is Either.Left -> errors.add(result.value)
            }
        }
        
        return if (errors.isNotEmpty()) {
            errors.first().left()
        } else {
            feature.right()
        }
    }
    
    fun validateScenario(scenario: Scenario, index: Int): Either<AppError, Scenario> {
        val errors = mutableListOf<ValidationError>()
        
        if (scenario.name.isBlank()) {
            errors.add(ValidationError(
                message = "Scenario name cannot be blank",
                field = "scenarios[$index].name"
            ))
        }
        
        if (scenario.steps.isEmpty()) {
            errors.add(ValidationError(
                message = "Scenario must have at least one step",
                field = "scenarios[$index].steps"
            ))
        }
        
        for ((stepIndex, step) in scenario.steps.withIndex()) {
            when (val result = validateStep(step, index, stepIndex)) {
                is Either.Right -> {}
                is Either.Left -> errors.add(result.value)
            }
        }
        
        return if (errors.isNotEmpty()) {
            errors.first().left()
        } else {
            scenario.right()
        }
    }
    
    fun validateStep(step: Step, scenarioIndex: Int, stepIndex: Int): Either<AppError, Step> {
        val errors = mutableListOf<ValidationError>()
        
        if (step.text.isBlank()) {
            errors.add(ValidationError(
                message = "Step text cannot be blank",
                field = "scenarios[$scenarioIndex].steps[$stepIndex].text"
            ))
        }
        
        if (step.keyword == StepKeyword.AND || step.keyword == StepKeyword.BUT) {
            if (stepIndex == 0) {
                errors.add(ValidationError(
                    message = "First step cannot use And or But keyword",
                    field = "scenarios[$scenarioIndex].steps[$stepIndex].keyword"
                ))
            }
        }
        
        return if (errors.isNotEmpty()) {
            errors.first().left()
        } else {
            step.right()
        }
    }
    
    fun validateGherkinText(text: String): Either<AppError, List<ValidationError>> {
        val errors = mutableListOf<ValidationError>()
        val lines = text.lines()
        
        var hasFeature = false
        var hasScenario = false
        var currentScenarioHasSteps = false
        
        for ((lineIndex, line) in lines.withIndex()) {
            val trimmedLine = line.trim()
            
            when {
                trimmedLine.startsWith("Feature:", ignoreCase = true) -> {
                    hasFeature = true
                    hasScenario = false
                    currentScenarioHasSteps = false
                }
                trimmedLine.startsWith("Scenario:", ignoreCase = true) -> {
                    if (!hasFeature) {
                        errors.add(ValidationError(
                            message = "Scenario found before Feature",
                            field = "line ${lineIndex + 1}"
                        ))
                    }
                    
                    if (!currentScenarioHasSteps && hasScenario) {
                        errors.add(ValidationError(
                            message = "Previous scenario has no steps",
                            field = "line ${lineIndex + 1}"
                        ))
                    }
                    
                    hasScenario = true
                    currentScenarioHasSteps = false
                }
                trimmedLine.startsWith("Given", ignoreCase = true) ||
                trimmedLine.startsWith("When", ignoreCase = true) ||
                trimmedLine.startsWith("Then", ignoreCase = true) -> {
                    if (!hasScenario) {
                        errors.add(ValidationError(
                            message = "Step found outside of scenario",
                            field = "line ${lineIndex + 1}"
                        ))
                    }
                    
                    currentScenarioHasSteps = true
                }
                trimmedLine.startsWith("And", ignoreCase = true) ||
                trimmedLine.startsWith("But", ignoreCase = true) -> {
                    if (!currentScenarioHasSteps) {
                        errors.add(ValidationError(
                            message = "And/But found without previous step",
                            field = "line ${lineIndex + 1}"
                        ))
                    }
                    
                    currentScenarioHasSteps = true
                }
            }
        }
        
        if (!hasFeature) {
            errors.add(ValidationError(
                message = "No Feature found in Gherkin file",
                field = "file"
            ))
        }
        
        if (!hasScenario) {
            errors.add(ValidationError(
                message = "No Scenario found in Gherkin file",
                field = "file"
            ))
        }
        
        if (!currentScenarioHasSteps && hasScenario) {
            errors.add(ValidationError(
                message = "Last scenario has no steps",
                field = "file"
            ))
        }
        
        return if (errors.isNotEmpty()) {
            errors.left()
        } else {
            emptyList<ValidationError>().right()
        }
    }
    
    fun validateSyntax(text: String): Either<AppError, Boolean> {
        return validateGherkinText(text).map { errors ->
            errors.isEmpty()
        }
    }
    
    fun getValidationSummary(feature: Feature): String {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (feature.name.isBlank()) {
            errors.add("Feature name is blank")
        }
        
        if (feature.scenarios.isEmpty()) {
            errors.add("No scenarios in feature")
        }
        
        if (feature.description.isNullOrBlank()) {
            warnings.add("Feature has no description")
        }
        
        for ((index, scenario) in feature.scenarios.withIndex()) {
            if (scenario.name.isBlank()) {
                errors.add("Scenario $index has blank name")
            }
            
            if (scenario.steps.isEmpty()) {
                errors.add("Scenario '$scenario.name' has no steps")
            }
            
            if (scenario.description.isNullOrBlank()) {
                warnings.add("Scenario '$scenario.name' has no description")
            }
            
            val hasGiven = scenario.steps.any { it.keyword == StepKeyword.GIVEN }
            val hasWhen = scenario.steps.any { it.keyword == StepKeyword.WHEN }
            val hasThen = scenario.steps.any { it.keyword == StepKeyword.THEN }
            
            if (!hasGiven) {
                warnings.add("Scenario '$scenario.name' has no Given steps")
            }
            
            if (!hasWhen) {
                warnings.add("Scenario '$scenario.name' has no When steps")
            }
            
            if (!hasThen) {
                errors.add("Scenario '$scenario.name' has no Then steps")
            }
        }
        
        return buildString {
            appendLine("Gherkin Validation Summary")
            appendLine("=" .repeat(50))
            appendLine()
            
            if (errors.isNotEmpty()) {
                appendLine("Errors (${errors.size}):")
                errors.forEach { appendLine("  - $it") }
                appendLine()
            }
            
            if (warnings.isNotEmpty()) {
                appendLine("Warnings (${warnings.size}):")
                warnings.forEach { appendLine("  - $it") }
                appendLine()
            }
            
            if (errors.isEmpty() && warnings.isEmpty()) {
                appendLine("✓ No issues found")
            } else {
                appendLine("✗ Found ${errors.size} error(s) and ${warnings.size} warning(s)")
            }
        }
    }
}