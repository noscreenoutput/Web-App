package com.example

import com.example.bridge.PerformanceMetrics
import com.example.data.model.WasmAppEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun defaultWasmAppEntity_hasDesktopDisabledByDefault() {
        val app = WasmAppEntity(
            id = "test_1",
            name = "Test App",
            directoryName = "test_dir"
        )
        assertFalse(app.isDesktopMode)
        assertEquals("touch", app.controlMode)
        assertEquals(1.0f, app.preferredScale)
    }

    @Test
    fun performanceMetrics_defaultsAreValid() {
        val metrics = PerformanceMetrics()
        assertEquals(60, metrics.fps)
        assertTrue(metrics.gpuBackend.contains("WebGL"))
    }
}
