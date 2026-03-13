package com.kdockerck.shared.logging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)

interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
    
    val logFlow: Flow<LogEntry>
}

class ConsoleLogger : Logger {
    private val _logFlow = MutableSharedFlow<LogEntry>(replay = 100)
    override val logFlow: Flow<LogEntry> = _logFlow.asSharedFlow()
    
    override fun debug(tag: String, message: String) {
        log(LogLevel.DEBUG, tag, message)
    }
    
    override fun info(tag: String, message: String) {
        log(LogLevel.INFO, tag, message)
    }
    
    override fun warn(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.WARN, tag, message, throwable)
    }
    
    override fun error(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestamp = Clock.System.now(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        _logFlow.tryEmit(entry)
        printLogEntry(entry)
    }
    
    private fun printLogEntry(entry: LogEntry) {
        val timestamp = entry.timestamp.toString()
        val level = entry.level.name.padEnd(5)
        val tag = entry.tag.padEnd(20)
        
        val message = buildString {
            append("$timestamp [$level] $tag: ${entry.message}")
            entry.throwable?.let {
                append("\n")
                append(it.stackTraceToString())
            }
        }
        
        when (entry.level) {
            LogLevel.DEBUG -> println(message)
            LogLevel.INFO -> println(message)
            LogLevel.WARN -> println(message)
            LogLevel.ERROR -> System.err.println(message)
        }
    }
}

class CompositeLogger(private val loggers: List<Logger>) : Logger {
    override val logFlow: Flow<LogEntry> = loggers.first().logFlow
    
    override fun debug(tag: String, message: String) {
        loggers.forEach { it.debug(tag, message) }
    }
    
    override fun info(tag: String, message: String) {
        loggers.forEach { it.info(tag, message) }
    }
    
    override fun warn(tag: String, message: String, throwable: Throwable?) {
        loggers.forEach { it.warn(tag, message, throwable) }
    }
    
    override fun error(tag: String, message: String, throwable: Throwable?) {
        loggers.forEach { it.error(tag, message, throwable) }
    }
}

object LoggerFactory {
    private val loggers = mutableMapOf<String, Logger>()
    private val defaultLogger = ConsoleLogger()
    
    fun getLogger(tag: String): Logger {
        return loggers.getOrPut(tag) { defaultLogger }
    }
    
    fun setLogger(tag: String, logger: Logger) {
        loggers[tag] = logger
    }
        
    fun setDefaultLogger(logger: Logger) {
        loggers.clear()
        loggers["default"] = logger
    }
}

inline fun debug(tag: String, message: () -> String) {
    if (isDebugEnabled(tag)) {
        LoggerFactory.getLogger(tag).debug(tag, message())
    }
}

inline fun info(tag: String, message: () -> String) {
    if (isInfoEnabled(tag)) {
        LoggerFactory.getLogger(tag).info(tag, message())
    }
}

inline fun warn(tag: String, message: () -> String, throwable: Throwable? = null) {
    if (isWarnEnabled(tag)) {
        LoggerFactory.getLogger(tag).warn(tag, message(), throwable)
    }
}

inline fun error(tag: String, message: () -> String, throwable: Throwable? = null) {
    if (isErrorEnabled(tag)) {
        LoggerFactory.getLogger(tag).error(tag, message(), throwable)
    }
}

fun isDebugEnabled(tag: String): Boolean = true
fun isInfoEnabled(tag: String): Boolean = true
fun isWarnEnabled(tag: String): Boolean = true
fun isErrorEnabled(tag: String): Boolean = true