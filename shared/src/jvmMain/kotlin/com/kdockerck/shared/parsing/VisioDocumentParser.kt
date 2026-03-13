package com.kdockerck.shared.parsing

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.domain.ParsedContent
import com.kdockerck.shared.domain.VisioDocumentContent
import com.kdockerck.shared.domain.VisioPage
import com.kdockerck.shared.domain.Shape
import com.kdockerck.shared.domain.Connection
import com.kdockerck.shared.domain.Position
import com.kdockerck.shared.domain.Size
import com.kdockerck.shared.domain.ContentMetadata
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.ParsingError
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import java.util.UUID
import kotlin.io.use

class VisioDocumentParser {
    fun parse(filePath: String, documentId: String): Either<AppError, ParsedContent> = try {
        FileInputStream(filePath).use { fileInputStream ->
            parse(fileInputStream, filePath, documentId)
        }
    } catch (e: Exception) {
        ParsingError(
            message = "Failed to read Visio document: ${e.message}",
            cause = e,
            filePath = filePath
        ).left()
    }
    
    fun parse(inputStream: java.io.InputStream, filePath: String, documentId: String): Either<AppError, ParsedContent> = try {
        val pages = mutableListOf<VisioPage>()
        val connections = mutableListOf<Connection>()
        
        ZipInputStream(inputStream).use { zipInputStream ->
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".xml") && entry.name.contains("page")) {
                    val xmlContent = zipInputStream.readBytes().toString(Charsets.UTF_8)
                    val page = parsePageXml(xmlContent, documentId)
                    if (page != null) {
                        pages.add(page)
                        connections.addAll(page.connections)
                    }
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        }
        
        val metadata = ContentMetadata(
            title = extractTitle(filePath)
        )
        
        VisioDocumentContent(
            documentId = documentId,
            metadata = metadata,
            pages = pages
        ).right()
    } catch (e: Exception) {
        ParsingError(
            message = "Failed to parse Visio document: ${e.message}",
            cause = e,
            filePath = filePath
        ).left()
    }
    
    private fun parsePageXml(xml: String, documentId: String): VisioPage? {
        try {
            val shapes = mutableListOf<Shape>()
            val pageConnections = mutableListOf<Connection>()
            
            val shapeRegex = Regex("""<Shape[^>]*ID="(\d+)"[^>]*>(.*?)</Shape>""", RegexOption.DOT_MATCHES_ALL)
            val shapeMatches = shapeRegex.findAll(xml)
            
            for (match in shapeMatches) {
                val shapeId = match.groupValues[1]
                val shapeContent = match.groupValues[2]
                val shape = parseShape(shapeContent, shapeId)
                if (shape != null) {
                    shapes.add(shape)
                }
            }
            
            val connectRegex = Regex("""<Connect[^>]*FromSheet="(\d+)"[^>]*ToSheet="(\d+)"[^>]*>""")
            val connectMatches = connectRegex.findAll(xml)
            
            for (match in connectMatches) {
                val fromId = match.groupValues[1]
                val toId = match.groupValues[2]
                val connection = Connection(
                    id = UUID.randomUUID().toString(),
                    fromShapeId = fromId,
                    toShapeId = toId,
                    label = null
                )
                pageConnections.add(connection)
            }
            
            val pageName = extractPageName(xml) ?: "Page ${shapes.size}"
            
            return VisioPage(
                id = UUID.randomUUID().toString(),
                name = pageName,
                shapes = shapes,
                connections = pageConnections
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun parseShape(content: String, id: String): Shape? {
        try {
            val textRegex = Regex("""<Text[^>]*>(.*?)</Text>""")
            val textMatch = textRegex.find(content)
            val text = textMatch?.groupValues?.get(1)?.replace(Regex)("""<[^>]+>""", "")?.trim() ?: ""
            
            val typeRegex = Regex("""<Type[^>]*>(.*?)</Type>""")
            val typeMatch = typeRegex.find(content)
            val type = typeMatch?.groupValues?.get(1) ?: "Shape"
            
            val xPosRegex = Regex("""<PinX[^>]*>(.*?)</PinX>""")
            val yPosRegex = Regex("""<PinY[^>]*>(.*?)</PinY>""")
            val widthRegex = Regex("""<Width[^>]*>(.*?)</Width>""")
            val heightRegex = Regex("""<Height[^>]*>(.*?)</Height>""")
            
            val x = xPosRegex.find(content)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val y = yPosRegex.find(content)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val width = widthRegex.find(content)?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0
            val height = heightRegex.find(content)?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0
            
            return Shape(
                id = id,
                text = text,
                type = type,
                position = Position(x = x, y = y),
                size = Size(width = width, height = height)
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun extractPageName(xml: String): String? {
        val nameRegex = Regex("""<Name[^>]*>(.*?)</Name>""")
        val match = nameRegex.find(xml)
        return match?.groupValues?.get(1)?.trim()
    }
    
    private fun extractTitle(filePath: String): String? {
        return filePath.substringAfterLast("/").substringAfterLast("\\")
            .removeSuffix(".vsdx")
            .removeSuffix(".vsd")
    }
}