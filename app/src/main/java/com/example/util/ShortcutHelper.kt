package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.data.model.WasmAppEntity

object ShortcutHelper {

    fun pinAppShortcut(context: Context, app: WasmAppEntity): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Peluncur perangkat tidak mendukung pin pintasan", Toast.LENGTH_SHORT).show()
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("EXTRA_APP_ID", app.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Generate circular colored icon bitmap
        val sizePx = 108
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val parsedColor = try {
            Color.parseColor(app.iconBgColorHex)
        } catch (_: Exception) {
            0xFF1976D2.toInt()
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parsedColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

        // Draw initial letter
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = sizePx * 0.45f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val initial = app.name.take(1).uppercase()
        val textBaseline = (sizePx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, sizePx / 2f, textBaseline, textPaint)

        val shortcutIcon = IconCompat.createWithBitmap(bitmap)

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "wasm_app_${app.id}")
            .setShortLabel(app.name)
            .setLongLabel(app.name)
            .setIcon(shortcutIcon)
            .setIntent(launchIntent)
            .build()

        val success = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
        if (success) {
            Toast.makeText(context, "Pintasan '${app.name}' ditambahkan ke Beranda", Toast.LENGTH_SHORT).show()
        }
        return success
    }
}
