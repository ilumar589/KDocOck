package com.kdockerck.shared.gherkin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError

class GherkinSyntaxRepairer {
    
    fun repair(text: String): Either<AppError, String> {
        var repairedText = text
        val repairs = mutableListOf<String>()
        
        repairedText = repairFeatureKeyword(repairedText)
        repairedText = repairScenarioKeyword(repairedText)
        repairedText = repairStepKeywords(repairedText)
        repairedText = repairIndentation(repairedText)
        repairedText = repairEmptyLines(repairedText)
        repairedText = repairColonSpacing(repairedText)
        repairedText = repairDuplicateKeywords(repairedText)
        
        return repairedText.right()
    }
    
    fun repairWithReport(text: String): Either<AppError, Pair<String, List<String>>> {
        val repairs = mutableListOf<String>()
        var repairedText = text
        
        val originalFeatureKeyword = extractFeatureKeyword(repairedText)
        repairedText = repairFeatureKeyword(repairedText)
        if (originalFeatureKeyword != extractFeatureKeyword(repairedText)) {
            repairs.add("Repaired Feature keyword capitalization")
        }
        
        val originalScenarioKeyword = extractScenarioKeywords(repairedText)
        repairedText = repairScenarioKeyword(repairedText)
        if (originalScenarioKeyword != extractScenarioKeywords(repairedText)) {
            repairs.add("Repaired Scenario keyword capitalization")
        }
        
        val originalStepKeywords = extractStepKeywords(repairedText)
        repairedText = repairStepKeywords(repairedText)
        if (originalStepKeywords != extractStepKeywords(repairedText)) {
            repairs.add("Repaired step keyword capitalization")
        }
        
        repairedText = repairIndentation(repairedText)
        repairs.add("Fixed indentation")
        
        repairedText = repairEmptyLines(repairedText)
        repairs.add("Removed excessive empty lines")
        
        repairedText = repairColonSpacing(repairedText)
        repairs.add("Fixed colon spacing")
        
        repairedText = repairDuplicateKeywords(repairedText)
        repairs.add("Removed duplicate keywords")
        
        return Pair(repairedText, repairs).right()
    }
    
    private fun repairFeatureKeyword(text: String): String {
        return text.replace(Regex("""(?i)^(feature):"""), "Feature:")
    }
    
    private fun repairScenarioKeyword(text: String): String {
        return text.replace(Regex("""(?i)^(scenario):"""), "Scenario:")
    }
    
    private fun repairStepKeywords(text: String): String {
        var repaired = text
        
        repaired = repaired.replace(Regex("""(?i)^(given)"""), "Given")
        repaired = repaired.replace(Regex("""(?i)^(when)"""), "When")
        repaired = repaired.replace(Regex("""(?i)^(then)"""), "Then")
        repaired = repaired.replace(Regex("""(?i)^(and)"""), "And")
        repaired = repaired.replace(Regex("""(?i)^(but)"""), "But")
        repaired = repaired.replace(Regex("""(?i)^(background):"""), "Background:")
        repaired = repaired.replace(Regex("""(?i)^(examples):"""), "Examples:")
        
        return repaired
    }
    
    private fun repairIndentation(text: String): String {
        val lines = text.lines()
        val repairedLines = mutableListOf<String>()
        var currentIndent = 0
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            when {
                trimmedLine.startsWith("Feature:") || trimmedLine.startsWith("Scenario:") -> {
                    currentIndent = 0
                    repairedLines.add(trimmedLine)
                }
                trimmedLine.startsWith("Given") || trimmedLine.startsWith("When") ||
                trimmedLine.startsWith("Then") || trimmedLine.startsWith("And") ||
                trimmedLine.startsWith("But") -> {
                    currentIndent = 2
                    repairedLines.add("  ".repeat(currentIndent) + trimmedLine)
                }
                trimmedLine.startsWith("Background:") -> {
                    currentIndent = 2
                    repairedLines.add("  ".repeat(currentIndent) + trimmedLine)
                }
                trimmedLine.startsWith("Examples:") -> {
                    currentIndent = 4
                    repairedLines.add("  ".repeat(currentIndent) + trimmedLine)
                }
                trimmedLine.startsWith("|") -> {
                    repairedLines.add("    " + trimmedLine)
                }
                trimmedLine.isBlank() -> {
                    repairedLines.add("")
                }
                else -> {
                    repairedLines.add("  ".repeat(currentIndent) + trimmedLine)
                }
            }
        }
        
        return repairedLines.joinToString("\n")
    }
    
    private fun repairEmptyLines(text: String): String {
        val lines = text.lines()
        val repairedLines = mutableListOf<String>()
        var previousWasEmpty = false
        
        for (line in lines) {
            if (line.isBlank()) {
                if (!previousWasEmpty) {
                    repairedLines.add("")
                }
                previousWasEmpty = true
            } else {
                repairedLines.add(line)
                previousWasEmpty = false
            }
        }
        
        while (repairedLines.lastOrNull()?.isBlank() == true) {
            repairedLines.removeLastOrNull()
        }
        
        return repairedLines.joinToString("\n")
    }
    
    private fun repairColonSpacing(text: String): String {
        var repaired = text
        
        repaired = repaired.replace(Regex("""(Feature|Scenario|Background|Examples):\s*"""), "$1: ")
        repaired = repaired.replace(Regex("""(Given|When|Then|And|But)\s+"""), "$1 ")
        
        return repaired
    }
    
    private fun repairDuplicateKeywords(text: String): String {
        val lines = text.lines()
        val repairedLines = mutableListOf<String>()
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            val hasDuplicateKeyword = Regex("""^(Given|When|Then|And|But)\s+\1\s+""").find(trimmedLine) != null
            
            if (hasDuplicateKeyword) {
                val keyword = trimmedLine.split(" ").first()
                val rest = trimmedLine.substringAfter(keyword).trim()
                repairedLines.add("  " + keyword + " " + rest)
            } else {
                repairedLines.add(line)
            }
        }
        
        return repairedLines.joinToString("\n")
    }
    
    private fun extractFeatureKeyword(text: String): String {
        val match = Regex("""(?i)^(feature):""").find(text)
        return match?.value ?: ""
    }
    
    private fun extractScenarioKeywords(text: String): String {
        val matches = Regex("""(?i)^(scenario):""").findAll(text)
        return matches.joinToString(",") { it.value }
    }
    
    private fun extractStepKeywords(text: String): String {
        val matches = Regex("""(?i)^(given|when|then|and|but)""").findAll(text)
        return matches.joinToString(",") { it.value }
    }
    
    fun canRepair(text: String): Boolean {
        val issues = mutableListOf<String>()
        
        if (Regex("""(?i)^(feature):""").find(text) != null) {
            issues.add("Feature keyword capitalization")
        }
        
        if (Regex("""(?i)^(scenario):""").find(text) != null) {
            issues.add("Scenario keyword capitalization")
        }
        
        if (Regex("""(?i)^(given|when|then|and|but)""").find(text) != null) {
            issues.add("Step keyword capitalization")
        }
        
        if (Regex("""\s{3,}""").find(text) != null) {
            issues.add("Indentation issues")
        }
        
        if (Regex("""\n{3,}""").find(text) != null) {
            issues.add("Excessive empty lines")
        }
        
        return issues.isNotEmpty()
    }
    
    fun getRepairSuggestions(text: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (Regex("""(?i)^(feature):""").find(text) != null) {
            suggestions.add("Capitalize 'Feature' keyword")
        }
        
        if (Regex("""(?i)^(scenario):""").find(text) != null) {
            suggestions.add("Capitalize 'Scenario' keyword")
        }
        
        if (Regex("""(?i)^(given|when|then|and|but)""").find(text) != null) {
            suggestions.add("Capitalize step keywords (Given, When, Then, And, But)")
        }
        
        if (Regex("""\s{3,}""").find(text) != null) {
            suggestions.add("Fix indentation (use 2 spaces for steps)")
        }
        
        if (Regex("""\n{3,}""").find(text) != null) {
            suggestions.add("Remove excessive empty lines")
        }
        
        if (Regex("""(Feature|Scenario|Background|Examples):\s{0,}""").find(text) != null) {
            suggestions.add("Add space after colon in keywords")
        }
        
        return suggestions
    }
}