/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import net.tigr.soulcalc.data.preferences.ThemeMode
import net.tigr.soulcalc.ui.screen.MainScreen
import net.tigr.soulcalc.ui.theme.SoulCalcTheme
import net.tigr.soulcalc.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as SoulCalcApp
        MainViewModel.Factory(app.sheetRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SoulCalcApp

        setContent {
            val themeMode by app.themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()

            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SoulCalcTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    viewModel = viewModel,
                    currentThemeMode = themeMode,
                    onThemeModeChanged = { newMode ->
                        scope.launch {
                            app.themePreferences.setThemeMode(newMode)
                        }
                    }
                )
            }
        }
    }
}
