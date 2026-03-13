package com.kdockerck.shared.database

import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.jetbrains.exposed.sql.transactions.transaction
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.DatabaseError

class VectorIndexManager {
    
    suspend fun createHNSWIndex(connection: Connection): Either<AppError, Unit> = try {
        val createIndexSql = """
            CREATE INDEX IF NOT EXISTS embeddings_vector_idx 
            ON embeddings USING hnsw (vector vector_cosine_ops)
            WITH (m = 16, ef_construction = 64)
        """.trimIndent()
        
        val statement: Statement = connection.createStatement(createIndexSql)
        statement.execute().awaitSingle()
        
        Unit.right()
    } catch (e: Exception) {
        DatabaseError(
            message = "Failed to create HNSW index: ${e.message}",
            cause = e,
            query = "CREATE INDEX embeddings_vector_idx"
        ).left()
    }
    
    suspend fun dropIndex(connection: Connection): Either<AppError, Unit> = try {
        val dropIndexSql = "DROP INDEX IF EXISTS embeddings_vector_idx"
        
        val statement: Statement = connection.createStatement(dropIndexSql)
        statement.execute().awaitSingle()
        
        Unit.right()
    } catch (e: Exception) {
        DatabaseError(
            message = "Failed to drop HNSW index: ${e.message}",
            cause = e,
            query = "DROP INDEX embeddings_vector_idx"
        ).left()
    }
    
    suspend fun indexExists(connection: Connection): Either<AppError, Boolean> = try {
        val checkIndexSql = """
            SELECT EXISTS (
                SELECT 1 FROM pg_indexes 
                WHERE indexname = 'embeddings_vector_idx'
            )
        """.trimIndent()
        
        val statement: Statement = connection.createStatement(checkIndexSql)
        val result = statement.execute().awaitSingle()
        
        val exists = result.map { row ->
            row.get(0) as Boolean
        }.blockFirst() ?: false
        
        exists.right()
    } catch (e: Exception) {
        DatabaseError(
            message = "Failed to check HNSW index existence: ${e.message}",
            cause = e,
            query = checkIndexSql
        ).left()
    }
    
    suspend fun recreateIndex(connection: Connection): Either<AppError, Unit> {
        return dropIndex(connection).flatMap {
            createHNSWIndex(connection)
        }
    }
    
    suspend fun optimizeIndex(connection: Connection, efRuntime: Int = 64): Either<AppError, Unit> = try {
        val optimizeSql = """
            SET hnsw.ef_search = $efRuntime
        """.trimIndent()
        
        val statement: Statement = connection.createStatement(optimizeSql)
        statement.execute().awaitSingle()
        
        Unit.right()
    } catch (e: Exception) {
        DatabaseError(
            message = "Failed to optimize HNSW index: ${e.message}",
            cause = e,
            query = optimizeSql
        ).left()
    }
    
    companion object {
        const val DEFAULT_M = 16
        const val DEFAULT_EF_CONSTRUCTION = 64
        const val DEFAULT_EEF_RUNTIME = 64
        
        fun getCreateIndexSql(m: Int = DEFAULT_M, efConstruction: Int = DEFAULT_EF_CONSTRUCTION): String {
            return """
                CREATE INDEX IF NOT EXISTS embeddings_vector_idx 
                ON embeddings USING hnsw (vector vector_cosine_ops)
                WITH (m = $m, ef_construction = $efConstruction)
            """.trimIndent()
        }
    }
}