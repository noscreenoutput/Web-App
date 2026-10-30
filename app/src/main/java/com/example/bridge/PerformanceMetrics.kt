package com.example.bridge

data class PerformanceMetrics(
    val fps: Int = 60,
    val frameTimeMs: Float = 16.6f,
    val jsHeapMb: Float = 0f,
    val appHeapMb: Long = 0,
    val fpsHistory: List<Int> = emptyList(),
    val gpuBackend: String = "WebGL 2.0 (Accelerated)"
)
