package com.example.ui.components

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun VirtualCursorOverlay(
    webView: WebView?,
    enabled: Boolean,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        var cursorX by remember { mutableFloatStateOf(maxWidthPx / 2f) }
        var cursorY by remember { mutableFloatStateOf(maxHeightPx / 2f) }
        var isDragging by remember { mutableStateOf(false) }

        fun dispatchMouseEvent(eventType: String, button: Int) {
            if (webView == null) return
            val script = """
                (function() {
                    const el = document.elementFromPoint($cursorX, $cursorY) || document.body;
                    const eventInit = {
                        bubbles: true,
                        cancelable: true,
                        view: window,
                        clientX: $cursorX,
                        clientY: $cursorY,
                        screenX: $cursorX,
                        screenY: $cursorY,
                        button: $button,
                        buttons: ${if (button == 0) 1 else if (button == 2) 2 else 0}
                    };
                    if ('$eventType' === 'click') {
                        el.dispatchEvent(new MouseEvent('mousedown', eventInit));
                        setTimeout(() => {
                            el.dispatchEvent(new MouseEvent('mouseup', eventInit));
                            el.dispatchEvent(new MouseEvent('click', eventInit));
                        }, 20);
                    } else if ('$eventType' === 'contextmenu') {
                        el.dispatchEvent(new MouseEvent('mousedown', eventInit));
                        setTimeout(() => {
                            el.dispatchEvent(new MouseEvent('mouseup', eventInit));
                            el.dispatchEvent(new MouseEvent('contextmenu', eventInit));
                        }, 20);
                    } else {
                        el.dispatchEvent(new MouseEvent('$eventType', eventInit));
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }

        // Touch surface to move the cursor relative to drag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            cursorX = (cursorX + dragAmount.x).coerceIn(0f, maxWidthPx)
                            cursorY = (cursorY + dragAmount.y).coerceIn(0f, maxHeightPx)
                            // dispatch mousemove
                            val moveScript = """
                                (function() {
                                    const el = document.elementFromPoint($cursorX, $cursorY) || document.body;
                                    el.dispatchEvent(new MouseEvent('mousemove', {
                                        bubbles: true,
                                        cancelable: true,
                                        view: window,
                                        clientX: $cursorX,
                                        clientY: $cursorY
                                    }));
                                })();
                            """.trimIndent()
                            webView?.evaluateJavascript(moveScript, null)
                        }
                    )
                }
        )

        // Visual Cursor (Arrow / Dot with target ring)
        val cursorOffset = with(density) {
            IntOffset(cursorX.roundToInt(), cursorY.roundToInt())
        }

        Box(
            modifier = Modifier
                .offset { cursorOffset }
                .size(28.dp)
                .offset(x = (-6).dp, y = (-6).dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
        }

        // Ergonomic Mouse Buttons Bar (Bottom-Center or Bottom-Left)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.testTag("virtual_mouse_controls")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { dispatchMouseEvent("click", 0) },
                        modifier = Modifier.testTag("lmb_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdsClick,
                            contentDescription = "Klik Kiri (LMB)",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = " Klik Kiri (LMB)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    FilledTonalButton(
                        onClick = { dispatchMouseEvent("contextmenu", 2) },
                        modifier = Modifier.testTag("rmb_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = "Klik Kanan (RMB)",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = " Klik Kanan (RMB)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    IconButton(
                        onClick = onToggleMode,
                        modifier = Modifier.testTag("toggle_touch_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Kembali ke Sentuhan Langsung"
                        )
                    }
                }
            }
        }
    }
}
