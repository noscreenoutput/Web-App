package com.example.bridge

enum class LogLevel {
    INFO, LOG, WARN, ERROR
}

data class ConsoleLogItem(
    val level: LogLevel = LogLevel.LOG,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
