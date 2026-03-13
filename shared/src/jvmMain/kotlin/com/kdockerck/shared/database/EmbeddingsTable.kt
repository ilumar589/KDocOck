package com.kdockerck.shared.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.text
import org.jetbrains.exposed.sql.integer
import org.jetbrains.exposed.sql.TextColumnType

object Embeddings : Table("embeddings") {
    val id: Column<String> = text("id").primaryKey()
    val documentId: Column<String> = text("document_id").index()
    val chunkId: Column<String?> = text("chunk_id").nullable()
    val vector: Column<List<Float>> = registerColumn("vector", VectorColumnType())
    val text: Column<String> = text("text")
    val chunkIndex: Column<Int?> = integer("chunk_index").nullable()
    val totalChunks: Column<Int?> = integer("total_chunks").nullable()
    val sourceType: Column<String> = text("source_type")
    val createdAt: Column<java.time.LocalDateTime> = datetime("created_at")
}

class VectorColumnType : TextColumnType() {
    override fun valueFromDB(value: Any): Any {
        return when (value) {
            is String -> parseVector(value)
            is List<*> -> value
            else -> super.valueFromDB(value)
        }
    }
    
    override fun valueToDB(value: Any?): Any? {
        return when (value) {
            is List<*> -> serializeVector(value as List<Float>)
            else -> super.valueToDB(value)
        }
    }
    
    private fun parseVector(value: String): List<Float> {
        return value.removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().toFloat() }
    }
    
    private fun serializeVector(vector: List<Float>): String {
        return vector.joinToString(",", "[", "]")
    }
    
    override fun sqlType(): String = "vector(768)"
}