package com.tk.toolkit.tv.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.toolkit.tv.launcher.ui.HomeScreen
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import com.tk.toolkit.tv.ui.theme.ToolkitTheme

/**
 * App shell. The launcher home is the root screen; future tools get their own
 * screens here and are reached from the home header.
 */
@Composable
fun ToolkitApp() {
    ToolkitTheme {
        Box(Modifier.fillMaxSize().background(ToolkitColors.Background)) {
            HomeScreen()
        }
    }
}
