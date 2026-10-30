package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.manager.WasmFileManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("egui Launcher", appName)
    }

    @Test
    fun `test format file size`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = WasmFileManager(context)
        assertEquals("500 B", manager.formatFileSize(500L))
        assertEquals("1.5 KB", manager.formatFileSize(1536L))
        assertEquals("2.00 MB", manager.formatFileSize(2097152L))
    }
}
