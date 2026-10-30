package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class IconPreset(val id: String, val icon: ImageVector, val label: String)

val ICON_PRESETS = listOf(
    IconPreset("widgets", Icons.Default.Widgets, "Widget"),
    IconPreset("code", Icons.Default.Code, "Kode"),
    IconPreset("gamepad", Icons.Default.Gamepad, "Game"),
    IconPreset("chart", Icons.Default.BarChart, "Grafik"),
    IconPreset("terminal", Icons.Default.Terminal, "Terminal"),
    IconPreset("science", Icons.Default.Science, "Sains"),
    IconPreset("speed", Icons.Default.Speed, "Performa"),
    IconPreset("layers", Icons.Default.Layers, "Lapisan")
)

val COLOR_PRESETS = listOf(
    0xFF1976D2.toInt(), // Biru
    0xFF00796B.toInt(), // Teal
    0xFF388E3C.toInt(), // Hijau
    0xFFE64A19.toInt(), // Rust Oranye
    0xFF7B1FA2.toInt(), // Ungu
    0xFFC2185B.toInt(), // Magenta
    0xFF455A64.toInt(), // Slate
    0xFFF57C00.toInt()  // Amber
)

@Composable
fun IconCustomizerDialog(
    initialIconPreset: String,
    initialColorArgb: Int,
    onDismiss: () -> Unit,
    onConfirm: (presetId: String, colorArgb: Int) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(initialIconPreset.ifBlank { "widgets" }) }
    var selectedColor by remember { mutableIntStateOf(if (initialColorArgb != 0) initialColorArgb else COLOR_PRESETS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Sesuaikan Ikon & Warna",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preview
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val currentIcon = ICON_PRESETS.firstOrNull { it.id == selectedPreset }?.icon ?: Icons.Default.Widgets
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color(selectedColor),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = currentIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Pilih Ikon Simbol:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ICON_PRESETS.take(4).forEach { preset ->
                        IconPresetButton(
                            preset = preset,
                            isSelected = selectedPreset == preset.id,
                            onClick = { selectedPreset = preset.id }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ICON_PRESETS.drop(4).take(4).forEach { preset ->
                        IconPresetButton(
                            preset = preset,
                            isSelected = selectedPreset == preset.id,
                            onClick = { selectedPreset = preset.id }
                        )
                    }
                }

                Text(
                    text = "Pilih Warna Aksen:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    COLOR_PRESETS.forEach { colorVal ->
                        val isSelected = selectedColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorVal },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPreset, selectedColor) },
                modifier = Modifier.testTag("apply_icon_button")
            ) {
                Text("Terapkan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun IconPresetButton(
    preset: IconPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = preset.icon,
                contentDescription = preset.label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
