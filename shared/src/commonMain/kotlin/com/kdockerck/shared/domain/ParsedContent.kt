package com.kdockerck.shared.domain

import kotlinx.serialization.Serializable

@Serializable
sealed class ParsedContent {
    abstract val documentId: String
    abstract val metadata: ContentMetadata
}

@Serializable
data class ContentMetadata(
    val title: String? = null,
    val author: String? = null,
    val createdAt: kotlinx.datetime.Instant? = null,
    val modifiedAt: kotlinx.datetime.Instant? = null
)

@Serializable
data class WordDocumentContent(
    override val documentId: String,
    override val metadata: ContentMetadata,
    val paragraphs: List<Paragraph>,
    val tables: List<Table>
) : ParsedContent()

@Serializable
data class Paragraph(
    val id: String,
    val text: String,
    val style: ParagraphStyle? = null,
    val level: Int = 0
)

@Serializable
enum class ParagraphStyle {
    HEADING_1,
    HEADING_2,
    HEADING_3,
    HEADING_4,
    HEADING_5,
    HEADING_6,
    NORMAL,
    TITLE,
    SUBTITLE
}

@Serializable
data class Table(
    val id: String,
    val rows: List<TableRow>,
    val headers: List<String>? = null
)

@Serializable
data class TableRow(
    val id: String,
    val cells: List<TableCell>
)

@Serializable
data class TableCell(
    val id: String,
    val text: String,
    val rowIndex: Int,
    val columnIndex: Int
)

@Serializable
data class ExcelDocumentContent(
    override val documentId: String,
    override val metadata: ContentMetadata,
    val worksheets: List<Worksheet>
) : ParsedContent()

@Serializable
data class Worksheet(
    val id: String,
    val name: String,
    val cells: List<Cell>
)

@Serializable
data class Cell(
    val id: String,
    val reference: String,
    val value: String,
    val formula: String? = null,
    val rowIndex: Int,
    val columnIndex: Int
)

@Serializable
data class VisioDocumentContent(
    override val documentId: String,
    override val metadata: ContentMetadata,
    val pages: List<VisioPage>
) : ParsedContent()

@Serializable
data class VisioPage(
    val id: String,
    val name: String,
    val shapes: List<Shape>,
    val connections: List<Connection>
)

@Serializable
data class Shape(
    val id: String,
    val text: String,
    val type: String,
    val position: Position,
    val size: Size
)

@Serializable
data class Position(
    val x: Double,
    val y: Double
)

@Serializable
data class Size(
    val width: Double,
    val height: Double
)

@Serializable
data class Connection(
    val id: String,
    val fromShapeId: String,
    val toShapeId: String,
    val label: String? = null
)