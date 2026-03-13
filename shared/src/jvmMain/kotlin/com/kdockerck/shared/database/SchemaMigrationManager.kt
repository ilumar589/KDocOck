package com.kdockerck.shared.database

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.DatabaseError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class MigrationVersion {
    abstract val version: Int
    abstract val description: String
    abstract val upSql: String
    abstract val downSql: String
    
    data class V1(
        override val version: Int = 1,
        override val description: String = "Create embeddings table",
        override val upSql: String = """
            CREATE TABLE IF NOT EXISTS embeddings (
                id TEXT PRIMARY KEY,
                document_id TEXT NOT NULL,
                chunk_id TEXT,
                vector vector(768) NOT NULL,
                text TEXT NOT NULL,
                chunk_index INTEGER,
                total_chunks INTEGER,
                source_type TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT NOW()
            );
            
            CREATE INDEX IF NOT EXISTS idx_embeddings_document_id ON embeddings(document_id);
            CREATE INDEX IF NOT EXISTS idx_embeddings_source_type ON embeddings(source_type);
        """.trimIndent(),
        override val downSql: String = """
            DROP INDEX IF EXISTS idx_embeddings_source_type;
            DROP INDEX IF EXISTS idx_embeddings_document_id;
            DROP TABLE IF EXISTS embeddings;
        """.trimIndent()
    ) : MigrationVersion()
    
    data class V2(
        override val version: Int = 2,
        override val description: String = "Add HNSW vector index",
        override val upSql: String = """
            CREATE INDEX IF NOT EXISTS embeddings_vector_idx 
            ON embeddings USING hnsw (vector vector_cosine_ops)
            WITH (m = 16, ef_construction = 64);
        """.trimIndent(),
        override val downSql: String = """
            DROP INDEX IF EXISTS embeddings_vector_idx;
        """.trimIndent()
    ) : MigrationVersion()
}

class SchemaMigrationManager(
    private val dbManager: DatabaseConnectionManager
) {
    private val migrations = listOf(
        MigrationVersion.V1(),
        MigrationVersion.V2()
    ).sortedBy { it.version }
    
    suspend fun getCurrentVersion(): Either<AppError, Int> {
        return dbManager.executeInTransaction { connection ->
            try {
                val checkTableSql = """
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.tables 
                        WHERE table_name = 'schema_migrations'
                    )
                """.trimIndent()
                
                val statement = connection.createStatement(checkTableSql)
                val result = statement.execute()
                val tableExists = result.map { row -> row.get(0) as Boolean }.blockFirst() ?: false
                
                if (!tableExists) {
                    0.right()
                } else {
                    val getVersionSql = """
                        SELECT MAX(version) FROM schema_migrations
                    """.trimIndent()
                    
                    val versionStatement = connection.createStatement(getVersionSql)
                    val versionResult = versionStatement.execute()
                    val version = versionResult.map { row -> row.get(0) as Int? }.blockFirst() ?: 0
                    
                    version.right()
                }
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Failed to get current schema version: ${e.message}",
                    cause = e,
                    query = "SELECT MAX(version) FROM schema_migrations"
                )
            }
        }
    }
    
    suspend fun migrateTo(targetVersion: Int? = null): Either<AppError, List<MigrationVersion>> {
        val currentVersion = getCurrentVersion()
        
        return currentVersion.flatMap { current ->
            val target = targetVersion ?: migrations.last().version
            
            if (current >= target) {
                emptyList<MigrationVersion>().right()
            } else {
                val migrationsToRun = migrations.filter { 
                    it.version > current && it.version <= target 
                }
                
                runMigrations(migrationsToRun)
            }
        }
    }
    
    suspend fun migrateToLatest(): Either<AppError, List<MigrationVersion>> {
        return migrateTo(null)
    }
    
    suspend fun rollbackTo(targetVersion: Int): Either<AppError, List<MigrationVersion>> {
        val currentVersion = getCurrentVersion()
        
        return currentVersion.flatMap { current ->
            if (current <= targetVersion) {
                emptyList<MigrationVersion>().right()
            } else {
                val migrationsToRollback = migrations.filter { 
                    it.version > targetVersion && it.version <= current 
                }.reversed()
                
                rollbackMigrations(migrationsToRollback)
            }
        }
    }
    
    suspend fun rollbackOne(): Either<AppError, MigrationVersion?> {
        val currentVersion = getCurrentVersion()
        
        return currentVersion.flatMap { current ->
            if (current == 0) {
                null.right()
            } else {
                val migration = migrations.find { it.version == current }
                
                if (migration != null) {
                    rollbackMigrations(listOf(migration)).map { migration }
                } else {
                    DatabaseError(
                        message = "Migration not found for version: $current",
                        query = "ROLLBACK"
                    ).left()
                }
            }
        }
    }
    
    suspend fun getMigrationStatus(): Either<AppError, MigrationStatus> {
        val currentVersion = getCurrentVersion()
        
        return currentVersion.flatMap { current ->
            val latestVersion = migrations.lastOrNull()?.version ?: 0
            val pendingMigrations = migrations.filter { it.version > current }
            val appliedMigrations = migrations.filter { it.version <= current }
            
            MigrationStatus(
                currentVersion = current,
                latestVersion = latestVersion,
                pendingMigrations = pendingMigrations,
                appliedMigrations = appliedMigrations,
                isUpToDate = current == latestVersion
            ).right()
        }
    }
    
    suspend fun migrateStream(targetVersion: Int? = null): Flow<Either<AppError, MigrationEvent>> = flow {
        val currentVersion = getCurrentVersion()
        
        when (currentVersion) {
            is Either.Right -> {
                val target = targetVersion ?: migrations.last().version
                val migrationsToRun = migrations.filter { 
                    it.version > currentVersion.value && it.version <= target 
                }
                
                for (migration in migrationsToRun) {
                    emit(Either.Right(MigrationEvent.Start(migration)))
                    
                    when (val result = runMigration(migration)) {
                        is Either.Right -> {
                            emit(Either.Right(MigrationEvent.Complete(migration)))
                        }
                        is Either.Left -> {
                            emit(Either.Left(result.value))
                            return@flow
                        }
                    }
                }
                        is Either.Left -> {
                            emit(Either.Left(result.value))
                            return@flow
                        }
                    }
                }
            }
            is Either.Left -> {
                emit(Either.Left(currentVersion.value))
            }
        }
    }
    
    private suspend fun runMigrations(migrations: List<MigrationVersion>): Either<AppError, List<MigrationVersion>> {
        val executedMigrations = mutableListOf<MigrationVersion>()
        
        for (migration in migrations) {
            when (val result = runMigration(migration)) {
                is Either.Right -> {
                    executedMigrations.add(migration)
                }
                is Either.Left -> {
                    return result.left()
                }
            }
        }
        
        return executedMigrations.right()
    }
    
    private suspend fun rollbackMigrations(migrations: List<MigrationVersion>): Either<AppError, List<MigrationVersion>> {
        val rolledBackMigrations = mutableListOf<MigrationVersion>()
        
        for (migration in migrations) {
            when (val result = rollbackMigration(migration)) {
                is Either.Right -> {
                    rolledBackMigrations.add(migration)
                }
                is Either.Left -> {
                    return result.left()
                }
            }
        }
        
        return rolledBackMigrations.right()
    }
    
    private suspend fun runMigration(migration: MigrationVersion): Either<AppError, Unit> {
        return dbManager.executeInTransaction { connection ->
            try {
                val statement = connection.createStatement(migration.upSql)
                statement.execute().blockFirst()
                
                recordMigration(connection, migration)
                
                Unit.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Migration failed: ${migration.description} - ${e.message}",
                    cause = e,
                    query = migration.upSql
                )
            }
        }
    }
    
    private suspend fun rollbackMigration(migration: MigrationVersion): Either<AppError, Unit> {
        return dbManager.executeInTransaction { connection ->
            try {
                val statement = connection.createStatement(migration.downSql)
                statement.execute().blockFirst()
                
                removeMigrationRecord(connection, migration)
                
                Unit.right()
            } catch (e: Exception) {
                throw DatabaseError(
                    message = "Rollback failed: ${migration.description} - ${e.message}",
                    cause = e,
                    query = migration.downSql
                )
            }
        }
    }
    
    private suspend fun recordMigration(connection: io.r2dbc.spi.Connection, migration: MigrationVersion) {
        val createTableSql = """
            CREATE TABLE IF NOT EXISTS schema_migrations (
                version INTEGER PRIMARY KEY,
                description TEXT NOT NULL,
                applied_at TIMESTAMP NOT NULL DEFAULT NOW()
            )
        """.trimIndent()
        
        val createStatement = connection.createStatement(createTableSql)
        createStatement.execute().blockFirst()
        
        val insertSql = """
            INSERT INTO schema_migrations (version, description) 
            VALUES (${migration.version}, '${migration.description}')
            ON CONFLICT (version) DO UPDATE 
            SET description = '${migration.description}'
        """.trimIndent()
        
        val insertStatement = connection.createStatement(insertSql)
        insertStatement.execute().blockFirst()
    }
    
    private suspend fun removeMigrationRecord(connection: io.r2dbc.spi.Connection, migration: MigrationVersion) {
        val deleteSql = """
            DELETE FROM schema_migrations WHERE version = ${migration.version}
        """.trimIndent()
        
        val statement = connection.createStatement(deleteSql)
        statement.execute().blockFirst()
    }
}

data class MigrationStatus(
    val currentVersion: Int,
    val latestVersion: Int,
    val pendingMigrations: List<MigrationVersion>,
    val appliedMigrations: List<MigrationVersion>,
    val isUpToDate: Boolean
)

sealed class MigrationEvent {
    data class Start(val migration: MigrationVersion) : MigrationEvent()
    data class Complete(val migration: MigrationVersion) : MigrationEvent()
    data class Error(val migration: MigrationVersion, val error: AppError) : MigrationEvent()
}