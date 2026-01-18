/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.tigr.soulcalc.ui.viewmodel.LineUiModel

/**
 * A single line row in the calculator.
 *
 * Displays an input field (70% width) and result (30% width).
 */
@Composable
fun LineRow(
    line: LineUiModel,
    lineNumber: Int,
    isFocused: Boolean,
    pendingInsertion: String?,
    pendingBackspace: Boolean,
    pendingClearLine: Boolean,
    showSystemKeyboard: Boolean,
    onTextChanged: (String) -> Unit,
    onNewLine: () -> Unit,
    onDeleteLine: () -> Unit,
    onResultTap: () -> Unit,
    onFocused: () -> Unit,
    onInsertionConsumed: () -> Unit,
    onBackspaceConsumed: () -> Unit,
    onClearLineConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Handle keyboard mode changes
    LaunchedEffect(showSystemKeyboard) {
        if (showSystemKeyboard && isFocused) {
            // Switching to system keyboard - request focus and show keyboard
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {
                // Focus request may fail during transitions, safe to ignore
            }
        } else if (!showSystemKeyboard) {
            // Switching to custom keyboard - hide system keyboard
            keyboardController?.hide()
        }
    }

    // Handle focus transitions based on keyboard mode
    LaunchedEffect(isFocused, showSystemKeyboard) {
        if (isFocused) {
            if (showSystemKeyboard) {
                // Only auto-request focus when using system keyboard
                // This avoids the flash of system keyboard with custom keyboard
                try {
                    focusRequester.requestFocus()
                } catch (_: Exception) {
                    // Focus request may fail during transitions, safe to ignore
                }
            }
        } else if (!showSystemKeyboard) {
            // Clear focus when line loses ViewModel focus with custom keyboard active
            // This hides the cursor from the old line
            focusManager.clearFocus()
        }
    }

    // Helper function to create TextFieldValue with properly clamped cursor position.
    // This prevents IllegalStateException when OffsetMapping encounters invalid cursor positions.
    fun createTextFieldValue(text: String, cursorPosition: Int): TextFieldValue {
        val clampedCursor = cursorPosition.coerceIn(0, text.length)
        return TextFieldValue(text, TextRange(clampedCursor))
    }

    // Track the textFieldValue separately from line.input to avoid recreation on every input change.
    // Use key(line.id) to recreate state when line changes, avoiding state mutation during composition.
    var textFieldValue by remember(line.id) {
        mutableStateOf(createTextFieldValue(line.input, line.input.length))
    }

    // Sync textFieldValue when line.input changes externally (not from user typing).
    // Using LaunchedEffect ensures state mutations happen outside composition.
    LaunchedEffect(line.input) {
        if (line.input != textFieldValue.text) {
            // External update detected - sync the text field value
            textFieldValue = createTextFieldValue(line.input, line.input.length)
        }
    }


    LaunchedEffect(pendingInsertion, isFocused) {
        if (pendingInsertion != null && isFocused) {
            val text = textFieldValue.text
            val selStart = textFieldValue.selection.start.coerceIn(0, text.length)
            val selEnd = textFieldValue.selection.end.coerceIn(selStart, text.length)
            val newText = text.substring(0, selStart) +
                    pendingInsertion +
                    text.substring(selEnd)
            val newCursor = selStart + pendingInsertion.length
            textFieldValue = createTextFieldValue(newText, newCursor)
            onTextChanged(newText)
            onInsertionConsumed()
        }
    }

    LaunchedEffect(pendingBackspace, isFocused) {
        if (pendingBackspace && isFocused) {
            val text = textFieldValue.text
            val selStart = textFieldValue.selection.start.coerceIn(0, text.length)
            val selEnd = textFieldValue.selection.end.coerceIn(selStart, text.length)

            val (newText, newCursor) = if (selStart != selEnd) {
                // Selection exists - delete the selection
                val result = text.substring(0, selStart) + text.substring(selEnd)
                result to selStart
            } else if (selStart > 0) {
                // No selection, cursor not at beginning - delete character before cursor
                val result = text.substring(0, selStart - 1) + text.substring(selStart)
                result to (selStart - 1)
            } else if (text.isEmpty()) {
                // Empty line with cursor at beginning - delete the line
                onDeleteLine()
                onBackspaceConsumed()
                return@LaunchedEffect
            } else {
                // Cursor at beginning but line not empty, nothing to delete
                text to 0
            }

            textFieldValue = createTextFieldValue(newText, newCursor)
            if (newText != text) {
                onTextChanged(newText)
            }
            onBackspaceConsumed()
        }
    }

    LaunchedEffect(pendingClearLine, isFocused) {
        if (pendingClearLine && isFocused) {
            textFieldValue = createTextFieldValue("", 0)
            onTextChanged("")
            onClearLineConsumed()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = lineNumber.toString(),
            modifier = Modifier
                .padding(end = 8.dp, top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )

        Box(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Ensure selection is always valid for the text to prevent
                    // IllegalStateException in OffsetMapping during focus operations
                    val safeValue = if (newValue.selection.end > newValue.text.length ||
                        newValue.selection.start > newValue.text.length) {
                        createTextFieldValue(newValue.text, newValue.text.length)
                    } else {
                        newValue
                    }
                    textFieldValue = safeValue
                    if (safeValue.text != line.input) {
                        onTextChanged(safeValue.text)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("line_input")
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            // Immediately hide system keyboard if using custom keyboard
                            // This prevents the brief flash of system keyboard on focus change
                            if (!showSystemKeyboard) {
                                keyboardController?.hide()
                            }
                            onFocused()
                        }
                    },
                textStyle = LocalTextStyle.current.copy(
                    color = if (line.isComment) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onNewLine() }
                ),
                singleLine = false
            )
        }

        Box(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(start = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable(
                    enabled = line.resultText.isNotEmpty() && !line.isError,
                    onClick = onResultTap
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = line.resultText,
                modifier = Modifier.testTag("line_result"),
                textAlign = TextAlign.End,
                color = when {
                    line.isError -> MaterialTheme.colorScheme.error
                    line.resultText.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
