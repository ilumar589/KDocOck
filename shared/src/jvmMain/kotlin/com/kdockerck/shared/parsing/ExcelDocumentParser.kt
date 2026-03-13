package com.kdockerck.shared.parsing

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.domain.ParsedContent
import com.kdockerck.shared.domain.ExcelDocumentContent
import com.kdockerck.shared.domain.Worksheet
import com.kdockerck.shared.domain.Cell
import com.kdockerck.shared.domain.ContentMetadata
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.ParsingError
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.util.CellReference
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID

class ExcelDocumentParser {
    private val dataFormatter = DataFormatter()
    
    fun parse(filePath: String, documentId: String): Either<AppError, ParsedContent> = try {
        FileInputStream(filePath).use { inputStream ->
            parse(inputStream, filePath, documentId)
        }
    } catch (e: Exception) {
        ParsingError(
            message = "Failed to read Excel document: ${e.message}",
            cause = e,
            filePath = filePath
        ).left()
    }
    
    fun parse(inputStream: InputStream, filePath: String, documentId: String): Either<AppError, ParsedContent> = try {
        val workbook = WorkbookFactory.create(inputStream)
        
        val worksheets = (0 until workbook.numberOfSheets).map { sheetIndex ->
            parseWorksheet(workbook.getSheetAt(sheetIndex), sheetIndex)
        }.filter { it.cells.isNotEmpty() }
        
        val metadata = ContentMetadata(
            title = extractTitle(workbook),
            author = workbook.properties.coreProperties.creator
        )
        
        ExcelDocumentContent(
            documentId = documentId,
            metadata = metadata,
            worksheets = worksheets
        ).right()
    } catch (e: Exception) {
        ParsingError(
            message = "Failed to parse Excel document: ${e.message}",
            cause = e,
            filePath = filePath
        ).left()
    }
    
    private fun parseWorksheet(sheet: Sheet, sheetIndex: Int): Worksheet {
        val cells = mutableListOf<Cell>()
        
        sheet.forEach { row ->
            row.forEach { cell ->
                val parsedCell = parseCell(cell)
                if (parsedCell != null) {
                    cells.add(parsedCell)
                }
            }
        }
        
        return Worksheet(
            id = UUID.randomUUID().toString(),
            name = sheet.sheetName,
            cells = cells
        )
    }
    
    private fun parseCell(cell: org.apache.poi.ss.usermodel.Cell): Cell? {
        val cellValue = when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> dataFormatter.formatCellValue(cell)
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.cachedFormulaResultValue?.let { result ->
                        when (result.cellType) {
                            CellType.STRING -> result.stringCellValue
                            CellType.NUMERIC -> dataFormatter.formatCellValue(result)
                            CellType.BOOLEAN -> result.booleanCellValue.toString()
                            else -> ""
                        }
                    } ?: ""
                } catch (e: Exception) {
                    ""
                }
            }
            CellType.BLANK -> return null
            else -> ""
        }
        
        val cellReference = CellReference(cell).formatAsString()
        
        return Cell(
            id = UUID.randomUUID().toString(),
            reference = cellReference,
            value = cellValue,
            formula = if (cell.cellType == CellType.FORMULA) cell.cellFormula else null,
            rowIndex = cell.rowIndex,
            columnIndex = cell.columnIndex
        )
    }
    
    private fun extractTitle(workbook: Workbook): String? {
        return workbook.getSheetAt(0).sheetName.takeIf { it.isNotBlank() }
    }
}