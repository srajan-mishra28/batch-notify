package com.srajan.batchnotify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class Screen { Home, Apps, Waiting }

/** Tiny navigator: three screens, system back returns home. */
@Composable
fun App() {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.Home -> HomeScreen(
                onOpenApps = { screen = Screen.Apps },
                onOpenWaiting = { screen = Screen.Waiting },
            )
            Screen.Apps -> AppsScreen(onBack = { screen = Screen.Home })
            Screen.Waiting -> WaitingScreen(onBack = { screen = Screen.Home })
        }
    }
}
