/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.tigr.soulcalc.R
import net.tigr.soulcalc.data.preferences.ThemeMode
import net.tigr.soulcalc.ui.component.AboutDialog
import net.tigr.soulcalc.ui.component.CalculatorKeyboard
import net.tigr.soulcalc.ui.component.KeyboardToggle
import net.tigr.soulcalc.ui.component.LineRow
import net.tigr.soulcalc.ui.component.SettingsDialog
import net.tigr.soulcalc.ui.component.VariablePicker
import net.tigr.soulcalc.ui.viewmodel.MainUiEvent
import net.tigr.soulcalc.ui.viewmodel.MainViewModel

/**
 * Main screen of the calculator application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }


    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(MainUiEvent.ToastShown)
        }
    }

    LaunchedEffect(uiState.focusedLineIndex, uiState.lines.size) {
        if (uiState.focusedLineIndex >= 0 && uiState.focusedLineIndex < uiState.lines.size) {
            listState.animateScrollToItem(uiState.focusedLineIndex)
        }
    }

    if (uiState.showVariablePicker) {
        VariablePicker(
            variables = uiState.availableVariables,
            onVariableSelected = { variable ->
                viewModel.onEvent(MainUiEvent.VariableSelected(variable))
            },
            onDismiss = {
                viewModel.onEvent(MainUiEvent.DismissVariablePicker)
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            currentThemeMode = currentThemeMode,
            onThemeModeChanged = onThemeModeChanged,
            onDismiss = { showSettings = false }
        )
    }

    if (showAbout) {
        AboutDialog(
            onDismiss = { showAbout = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    KeyboardToggle(
                        useCustomKeyboard = uiState.useCustomKeyboard,
                        onToggle = { viewModel.onEvent(MainUiEvent.ToggleKeyboard) }
                    )

                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.content_description_menu)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_copy_all)) },
                            onClick = {
                                showMenu = false
                                val text = viewModel.formatAllLines()
                                if (text.isNotEmpty()) {
                                    copyToClipboard(context, text)
                                    viewModel.onEvent(MainUiEvent.CopyAll)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_clear_sheet)) },
                            onClick = {
                                showMenu = false
                                viewModel.onEvent(MainUiEvent.ClearSheet)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_settings)) },
                            onClick = {
                                showMenu = false
                                showSettings = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_about)) },
                            onClick = {
                                showMenu = false
                                showAbout = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(
                    items = uiState.lines,
                    key = { index, line -> "${line.id}_$index" }
                ) { index, line ->
                    LineRow(
                        line = line,
                        lineNumber = index + 1,
                        isFocused = index == uiState.focusedLineIndex,
                        pendingInsertion = if (index == uiState.focusedLineIndex) {
                            uiState.pendingInsertion
                        } else null,
                        pendingBackspace = index == uiState.focusedLineIndex && uiState.pendingBackspace,
                        pendingClearLine = index == uiState.focusedLineIndex && uiState.pendingClearLine,
                        showSystemKeyboard = !uiState.useCustomKeyboard,
                        onTextChanged = { text ->
                            viewModel.onEvent(MainUiEvent.LineTextChanged(index, text))
                        },
                        onNewLine = {
                            viewModel.onEvent(MainUiEvent.NewLineRequested(index))
                        },
                        onDeleteLine = {
                            viewModel.onEvent(MainUiEvent.DeleteLineRequested(index))
                        },
                        onResultTap = {
                            viewModel.getResultForClipboard(index)?.let { result ->
                                copyToClipboard(context, result)
                                viewModel.onEvent(MainUiEvent.CopyResult(index))
                            }
                        },
                        onFocused = {
                            viewModel.onEvent(MainUiEvent.LineFocused(index))
                        },
                        onInsertionConsumed = {
                            viewModel.onEvent(MainUiEvent.InsertionConsumed)
                        },
                        onBackspaceConsumed = {
                            viewModel.onEvent(MainUiEvent.BackspaceConsumed)
                        },
                        onClearLineConsumed = {
                            viewModel.onEvent(MainUiEvent.ClearLineConsumed)
                        }
                    )
                }
            }

            if (uiState.useCustomKeyboard) {
                CalculatorKeyboard(
                    onKeyPress = { key ->
                        viewModel.onEvent(MainUiEvent.KeyPressed(key))
                    },
                    onEnter = {
                        viewModel.onEvent(MainUiEvent.EnterKeyPressed(uiState.focusedLineIndex))
                    },
                    onBackspace = {
                        viewModel.onEvent(MainUiEvent.BackspacePressed)
                    },
                    onClearLine = {
                        viewModel.onEvent(MainUiEvent.ClearLinePressed)
                    },
                    onDollarKey = {
                        viewModel.onEvent(MainUiEvent.DollarKeyPressed)
                    },
                    onDollarLongPress = {
                        viewModel.onEvent(MainUiEvent.ShowVariablePicker)
                    },
                    onHashKey = {
                        viewModel.onEvent(MainUiEvent.HashKeyPressed)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("SoulCalc", text)
    clipboard.setPrimaryClip(clip)
}
