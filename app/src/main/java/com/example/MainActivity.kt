package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.LauncherViewModel
import com.example.ui.RunnerViewModel
import com.example.ui.screens.LauncherScreen
import com.example.ui.screens.WasmRunnerScreen
import com.example.ui.theme.EguiLauncherTheme

class MainActivity : ComponentActivity() {

    private val launcherViewModel: LauncherViewModel by viewModels()
    private val runnerViewModel: RunnerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable hardware accelerated window rendering
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        enableEdgeToEdge()

        // Handle Shortcut launch
        handleIntent(intent)

        setContent {
            EguiLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val activeApp by launcherViewModel.activeApp.collectAsState()

                    AnimatedContent(
                        targetState = activeApp,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { targetApp ->
                        if (targetApp != null) {
                            WasmRunnerScreen(
                                app = targetApp,
                                viewModel = runnerViewModel,
                                onExit = { launcherViewModel.closeActiveApp() }
                            )
                        } else {
                            LauncherScreen(
                                viewModel = launcherViewModel,
                                onLaunchApp = { app -> launcherViewModel.launchApp(app) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val appId = intent?.getStringExtra("EXTRA_APP_ID")
        if (!appId.isNullOrBlank()) {
            launcherViewModel.loadAndLaunchAppById(appId)
        }
    }
}
