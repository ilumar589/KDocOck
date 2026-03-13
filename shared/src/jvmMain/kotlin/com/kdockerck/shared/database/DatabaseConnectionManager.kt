package com.kdockerck.shared.database

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.config.DatabaseConfig
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.DatabaseError
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ConcurrentHashMap

class DatabaseConnectionManager(
    private val config: DatabaseConfig
) {
    private val connectionFactory: ConnectionFactory by lazy {
        val factories = ConnectionFactories.get()
        val factory = factories.find { it.type == "postgresql" }
            ?: throw IllegalStateException("PostgreSQL R2DBC driver not found")
        factory
    }
    
    private val connectionPool = ConcurrentHashMap<String, Connection>()
    
    suspend fun connect(): Either<AppError, Database> = try {
        val connection = connectionFactory.create(
            mapOf(
                "url" to config.url,
                "user" to config.username,
                "password" to config.password,
                "poolSize" to config.poolSize.toString(),
                "maxLifetime" to config.maxLifetimeMillis.toString()
            )
        )
        
        connectionPool["default"] = connection
        
        val database = Database.connect(
            url = config.url,
            driver = { connection }
        )
        
        database.right()
    } catch (e: Exception) {
        DatabaseError(
            message = "Failed to connect to database: ${e.message}",
            cause = e,
            query = "CONNECT"
        ).left()
    }
    
    suspend fun disconnect(): Either<AppError, Unit> = try {
        connectionPool.values.forEach { connection ->
            connection.close().awaitFirstOrNull()
        }
        connectionPool.clear()
        
        Unit.right()
    } catch (e: Exception) {
        DatabaseError(
            message = "Failed to disconnect from database: ${e.message}",
            cause = e
        ).left()
    }
    
    fun getConnection(): Either<AppError, Connection> {
        val connection = connectionPool["default"]
        return if (connection != null) {
            connection.right()
        } else {
            DatabaseError(
                message = "No active database connection",
                query = "GET_CONNECTION"
            ).left()
        }
    }
    
    suspend fun <T> executeInTransaction(block: suspend (Connection) -> T): Either<AppError, T> {
        return getConnection().flatMap { connection ->
            try {
                val result = block(connection)
                result.right()
            } catch (e: Exception) {
                DatabaseError(
                    message = "Transaction failed: ${e.message}",
                    cause = e
                ).left()
            }
        }
    }
    
    suspend fun executeSql(sql: String): Either<AppError, Unit> {
        return getConnection().flatMap { connection ->
            try {
                val statement = connection.createStatement(sql)
                statement.execute().awaitFirstOrNull()
                Unit.right()
            } catch (e: Exception) {
                DatabaseError(
                    message = "Failed to execute SQL: ${e.message}",
                    cause = e,
                    query = sql
                ).left()
            }
        }
    }
    
    suspend fun healthCheck(): Either<AppError, Boolean> {
        return getConnection().flatMap { connection ->
            try {
                val statement = connection.createStatement("SELECT 1")
                val result = statement.execute().awaitFirstOrNull()
                val isHealthy = result != null
                isHealthy.right()
            } catch (e: Exception) {
                DatabaseError(
                    message = "Database health check failed: ${e.message}",
                    cause = e,
                    query = "SELECT 1"
                ).left()
            }
        }
    }
    
    fun isConnected(): Boolean {
        return connectionPool["default"] != null
    }
    
    companion object {
        private var instance: DatabaseConnectionManager? = null
        
        fun initialize(config: DatabaseConfig): DatabaseConnectionManager {
            instance = DatabaseConnectionManager(config)
            return instance!!
        }
        
        fun getInstance(): Either<AppError, DatabaseConnectionManager> {
            return instance?.right() ?: DatabaseError(
                message = "Database connection manager not initialized"
            ).left()
        }
    }
}