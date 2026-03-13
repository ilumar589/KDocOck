package com.kdockerck.shared.parsing

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.domain.ParsedContent
import com.kdockerck.shared.domain.WordDocumentContent
import com.kdockerck.shared.domain.Paragraph
import com.kdockerck.shared.domain.Table
import com.kdockerck.shared.domain.TableRow
import com.kdockerck.shared.domain.TableCell
import com.kdockerck.shared.domain.ContentMetadata
import com.kdockerck.shared.domain.ParagraphStyle
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.ParsingError
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID

class WordDocumentParser {
    fun parse(filePath: String, documentId: String): Either<AppError, ParsedContent> = try {
        FileInputStream(filePath).use { inputStream ->
            parse(inputStream, filePath, documentId)
        }
    } catch (e: Exception) {
        ParsingError(
            message = "Failed to read Word document: ${e.message}",
            cause = e,
            filePath = filePath
        ).left()
    }
    
    fun parse(inputStream: InputStream, filePath: String, documentId: String): Either<AppError, ParsedContent> = try {
        val document = XWPFDocument(inputStream)
        
        val paragraphs = document.paragraphs.mapIndexed { index, para ->
            parseParagraph(para, index)
        }.filter { it.text.isNotBlank() }
        
        val tables = document.tables.mapIndexed { index, table ->
            parseTable(table, index)
        }
        
        val metadata = ContentMetadata(
            title = extractTitle(document),
            author = document.properties.coreProperties.creator
        )
        
        WordDocumentContent(
            documentId = documentId,
            metadata = metadata,
            paragraphs = paragraphs,
            tables = tables
        ).right()
    } catch (e: Exception) {
        ParsingError(
            message = "Failed to parse Word document: ${e.message}",
            cause = e,
            filePath = filePath
        ).left()
    }
    
    private fun parseParagraph(paragraph: XWPFParagraph, index: Int): Paragraph {
        val text = paragraph.text()
        val style = paragraph.style?.let { detectStyle(it) }
        val level = detectHeadingLevel(style)
        
        return Paragraph(
            id = UUID.randomUUID().toString(),
            text = text,
            style = style,
            level = level
        )
    }
    
    private fun parseTable(table: XWPFTable, index: Int): Table {
        val rows = table.rows.mapIndexed { rowIndex, row ->
            TableRow(
                id = UUID.randomUUID().toString(),
                cells = row.tableCells.mapIndexed { cellIndex, cell ->
                    TableCell(
                        id = UUID.randomUUID().toString(),
                        text = cell.text,
                        rowIndex = rowIndex,
                        columnIndex = cellIndex
                    )
                }
            )
        }
        
        val headers = if (rows.isNotEmpty()) {
            rows.first().cells.map { it.text }
        } else {
            null
        }
        
        return Table(
            id = UUID.randomUUID().toString(),
            rows = rows,
            headers = headers
        )
    }
    
    private fun extractTitle(document: XWPFDocument): String? {
        return document.paragraphs.firstOrNull { para ->
            para.style?.contains("Heading", ignoreCase = true) == true ||
            para.style?.contains("Title", ignoreCase = true) == true
        }?.text?.takeIf { it.isNotBlank() }
    }
    
    private fun detectStyle(styleName: String): ParagraphStyle? = when {
        styleName.contains("Heading1", ignoreCase = true) -> ParagraphStyle.HEADING_1
        styleName.contains("Heading2", ignoreCase = true) -> ParagraphStyle.HEADING_2
        styleName.contains("Heading3", ignoreCase = true) -> ParagraphStyle.HEADING_3
        styleName.contains("Heading4", ignoreCase = true) -> ParagraphStyle.HEADING_4
        styleName.contains("Heading5", ignoreCase = true) -> ParagraphStyle.HEADING_5
        styleName.contains("Heading6", ignoreCase = true) -> ParagraphStyle.HEADING_6
        styleName.contains("Title", ignoreCase = true) -> ParagraphStyle.TITLE
        styleName.contains("Subtitle", ignoreCase = true) -> ParagraphStyle.SUBTITLE
        else -> ParagraphStyle.NORMAL
    }
    
    private fun detectHeadingLevel(style: ParagraphStyle?): Int = when (style) {
        ParagraphStyle.HEADING_1 -> 1
        ParagraphStyle.HEADING_2 -> 2
        ParagraphStyle.HEADING_3 -> 3
        ParagraphStyle.HEADING_4 -> 4
        ParagraphStyle.HEADING_5 -> 5
        ParagraphStyle.HEADING_6 -> 6
        ParagraphStyle.TITLE -> 0
        ParagraphStyle.SUBTITLE -> 1
        else -> 0
    }
}