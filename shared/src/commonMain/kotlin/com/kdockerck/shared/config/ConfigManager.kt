package com.kdockerck.shared.config

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.errors.AppError
import com.kdockerck.shared.errors.AppResult
import com.kdockerck.shared.errors.ConfigurationError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ConfigManager(
    private val configLoader: ConfigLoader,
    private val configSaver: ConfigSaver
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private var cachedConfig: AppConfig? = null
    
    fun loadConfig(): AppResult<AppConfig> = try {
        cachedConfig?.let { return Either.Right(it) }
        
        val configJson = configLoader.load()
        val config = json.decodeFromString<AppConfig>(configJson)
        cachedConfig = config
        Either.Right(config)
    } catch (e: SerializationException) {
        Either.Left(
            ConfigurationError(
                message = "Failed to parse configuration: ${e.message}",
                cause = e
            )
        )
    } catch (e: Exception) {
        Either.Left(
            ConfigurationError(
                message = "Failed to load configuration: ${e.message}",
                cause = e
            )
        )
    }
    
    fun saveConfig(config: AppConfig): AppResult<Unit> = try {
        val configJson = json.encodeToString(config)
        configSaver.save(configJson)
        cachedConfig = config
        Either.Right(Unit)
    } catch (e: Exception) {
        Either.Left(
            ConfigurationError(
                message = "Failed to save configuration: ${e.message}",
                cause = e
            )
        )
    }
    
    fun updateConfig(update: (AppConfig) -> AppConfig): AppResult<AppConfig> {
        val currentConfig = loadConfig()
        return currentConfig.flatMap { config ->
            val updatedConfig = update(config)
            saveConfig(updatedConfig).map { updatedConfig }
        }
    }
    
    fun getDefaultConfig(): AppConfig = AppConfig(
        ollama = OllamaConfig(),
        database = DatabaseConfig(),
        embedding = EmbeddingConfig(),
        ui = UIConfig()
    )
}

interface ConfigLoader {
    fun load(): String
}

interface ConfigSaver {
    fun save(configJson: String)
}

class StringConfigLoader(
    private val configJson: String
) : ConfigLoader {
    override fun load(): String = configJson
}

class StringConfigSaver(
    private val onSave: (String) -> Unit
) : ConfigSaver {
    override fun save(configJson: String) = onSave(configJson)
}