package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportZipDialog(
    fileUri: Uri?,
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, entryHtml: String, scale: Float, isImmersive: Boolean, isDesktopMode: Boolean, controlMode: String) -> Unit
) {
    var appName by remember { mutableStateOf(defaultName) }
    var entryHtml by remember { mutableStateOf("index.html") }
    var scale by remember { mutableFloatStateOf(1.0f) }
    var isImmersive by remember { mutableStateOf(true) }
    var isDesktopMode by remember { mutableStateOf(false) } // Nonaktif default sesuai permintaan user
    var controlMode by remember { mutableStateOf("touch") } // "touch" atau "cursor"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Konfigurasi Paket egui",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("Nama Aplikasi") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_name_input")
                )

                OutlinedTextField(
                    value = entryHtml,
                    onValueChange = { entryHtml = it },
                    label = { Text("Berkas Utama (HTML)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_html_input")
                )

                // Slider Skala Viewport Awal — satu slider tunggal bersih tanpa track bertumpuk
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Skala Viewport Awal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("viewport_scale_slider")
                    )
                }

                // Mode Desktop Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(18.dp)
                            )
                            Text(
                                text = "Mode Desktop (Widescreen)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            text = "Menyesuaikan viewport egui agar pas di layar lebar (nonaktif default)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDesktopMode,
                        onCheckedChange = { isDesktopMode = it },
                        modifier = Modifier.testTag("desktop_mode_switch")
                    )
                }

                // Mode Kontrol (Sentuhan vs Kursor Virtual)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Mode Kontrol Input",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = controlMode == "touch",
                            onClick = { controlMode = "touch" },
                            label = { Text("Sentuhan") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_mode_touch")
                        )
                        FilterChip(
                            selected = controlMode == "cursor",
                            onClick = { controlMode = "cursor" },
                            label = { Text("Kursor (LMB/RMB)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Mouse,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_mode_cursor")
                        )
                    }
                }

                // Mode Immersive
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mode Layar Penuh",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Sembunyikan status bar dan bilah navigasi saat aplikasi berjalan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isImmersive,
                        onCheckedChange = { isImmersive = it },
                        modifier = Modifier.testTag("immersive_mode_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = appName.ifBlank { "egui WASM App" }
                    val finalHtml = entryHtml.ifBlank { "index.html" }
                    onConfirm(finalName, finalHtml, scale, isImmersive, isDesktopMode, controlMode)
                },
                modifier = Modifier.testTag("save_and_install_button")
            ) {
                Text("Simpan & Pasang")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_button")
            ) {
                Text("Batal")
            }
        }
    )
}
