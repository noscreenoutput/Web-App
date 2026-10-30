package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.WasmAppEntity
import com.example.ui.ImportUiState
import com.example.ui.LauncherViewModel
import com.example.ui.components.AppDetailsSheet
import com.example.ui.components.AppListItem
import com.example.ui.components.DataExplorerSheet
import com.example.ui.components.IconCustomizerDialog
import com.example.ui.components.ImportZipDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    onLaunchApp: (WasmAppEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appsList by viewModel.appsList.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val selectedAppForDetails by viewModel.selectedAppForDetails.collectAsState()
    val selectedAppForIconCustomizer by viewModel.selectedAppForIconCustomizer.collectAsState()
    val selectedAppForDataExplorer by viewModel.selectedAppForDataExplorer.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }
    var pendingZipName by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".zip") ?: "egui WASM App"
            pendingZipUri = uri
            pendingZipName = fileName
            showImportDialog = true
        }
    }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportUiState.Success -> {
                snackbarHostState.showSnackbar("Aplikasi '${state.app.name}' berhasil dipasang!")
                viewModel.clearImportState()
            }
            is ImportUiState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.clearImportState()
            }
            else -> {}
        }
    }

    val filteredApps = remember(appsList, searchQuery) {
        if (searchQuery.isBlank()) {
            appsList
        } else {
            appsList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "egui WASM Launcher",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Runtime WebGL / WebGPU Akselerasi Hardware",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Action Bar (Import Zip & Install Demo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { zipPickerLauncher.launch("application/zip") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_zip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Impor ZIP")
                }

                FilledTonalButton(
                    onClick = { viewModel.installDemoApp() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("install_demo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Demo bawaan")
                }
            }

            // Search Bar
            if (appsList.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari aplikasi egui...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("search_app_input")
                )
            }

            // Loading state
            if (importState is ImportUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            text = "Mengekstrak dan menyiapkan paket WASM...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // App List or Empty State
            if (appsList.isEmpty() && importState !is ImportUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Text(
                            text = "Belum Ada Aplikasi egui",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Impor berkas ZIP egui (HTML + WebAssembly) atau pasang demo bawaan untuk langsung menguji akselerasi WebGL/WebGPU.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { viewModel.installDemoApp() },
                            modifier = Modifier.testTag("empty_state_install_demo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pasang Demo egui Sekarang")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.id }) { app ->
                        AppListItem(
                            app = app,
                            formattedSize = viewModel.formatSize(app.totalSizeBytes),
                            onLaunch = { onLaunchApp(app) },
                            onOpenDetails = { viewModel.selectAppForDetails(app) }
                        )
                    }
                }
            }
        }
    }

    // Import Dialog
    if (showImportDialog && pendingZipUri != null) {
        ImportZipDialog(
            fileUri = pendingZipUri,
            defaultName = pendingZipName,
            onDismiss = {
                showImportDialog = false
                pendingZipUri = null
            },
            onConfirm = { name, entryHtml, scale, isImmersive, isDesktopMode, controlMode ->
                pendingZipUri?.let { uri ->
                    viewModel.importZip(
                        uri = uri,
                        name = name,
                        description = "Paket egui WASM",
                        isImmersive = isImmersive,
                        isDesktopMode = isDesktopMode,
                        controlMode = controlMode,
                        scale = scale
                    )
                }
                showImportDialog = false
                pendingZipUri = null
            }
        )
    }

    // App Details Sheet
    selectedAppForDetails?.let { app ->
        AppDetailsSheet(
            app = app,
            formattedSize = viewModel.formatSize(app.totalSizeBytes),
            onDismiss = { viewModel.selectAppForDetails(null) },
            onLaunch = {
                viewModel.selectAppForDetails(null)
                onLaunchApp(app)
            },
            onDelete = {
                viewModel.deleteApp(app)
            },
            onOpenIconCustomizer = {
                viewModel.selectAppForIconCustomizer(app)
            },
            onOpenDataExplorer = {
                viewModel.selectAppForDataExplorer(app)
            },
            onUpdateSettings = { isImmersive, scale, isDesktopMode, controlMode ->
                viewModel.updateAppSettings(app.id, isImmersive, scale, isDesktopMode, controlMode)
            }
        )
    }

    // Icon & Color Customizer Dialog
    selectedAppForIconCustomizer?.let { app ->
        val currentPreset = app.iconPreset
        val currentColor = try {
            android.graphics.Color.parseColor(app.iconBgColorHex)
        } catch (_: Exception) {
            0xFF1976D2.toInt()
        }

        IconCustomizerDialog(
            initialIconPreset = currentPreset,
            initialColorArgb = currentColor,
            onDismiss = { viewModel.selectAppForIconCustomizer(null) },
            onConfirm = { presetId, colorArgb ->
                val hexString = String.format("#%06X", (0xFFFFFF and colorArgb))
                viewModel.updateAppIcon(
                    id = app.id,
                    preset = presetId,
                    customPath = "",
                    bgColorHex = hexString,
                    pinToHome = true
                )
            }
        )
    }

    // Data Explorer Sheet
    selectedAppForDataExplorer?.let { app ->
        DataExplorerSheet(
            app = app,
            onDismiss = { viewModel.selectAppForDataExplorer(null) }
        )
    }
}
