package com.kdockerck.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class GherkinOutput(
    val documentId: String,
    val feature: Feature,
    val generatedAt: kotlinx.datetime.Instant
)

@Serializable
data class Feature(
    val name: String,
    val description: String? = null,
    val background: Background? = null,
    val scenarios: List<Scenario>
)

@Serializable
data class Background(
    val steps: List<Step>
)

@Serializable
data class Scenario(
    val name: String,
    val description: String? = null,
    val steps: List<Step>,
    val examples: Examples? = null
)

@Serializable
data class Step(
    val keyword: StepKeyword,
    val text: String,
    val argument: StepArgument? = null
)

@Serializable
enum class StepKeyword {
    GIVEN,
    WHEN,
    THEN,
    AND,
    BUT
}

@Serializable
sealed class StepArgument

@Serializable
data class DocStringArgument(
    val contentType: String? = null,
    val content: String
) : StepArgument()

@Serializable
data class DataTableArgument(
    val rows: List<List<String>>
) : StepArgument()

@Serializable
data class Examples(
    val table: DataTableArgument
)