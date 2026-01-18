/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui.viewmodel

/**
 * UI state for the main calculator screen.
 */
data class MainUiState(
    /** List of lines to display */
    val lines: List<LineUiModel> = listOf(LineUiModel()),

    /** Index of the currently focused line */
    val focusedLineIndex: Int = 0,

    /** Whether the custom calculator keyboard is shown (vs system keyboard) */
    val useCustomKeyboard: Boolean = true,

    /** Whether the variable picker popup is visible */
    val showVariablePicker: Boolean = false,

    /** List of available variables for the picker */
    val availableVariables: List<String> = emptyList(),

    /** Toast/snackbar message to show (null = no message) */
    val toastMessage: String? = null,

    /** Text to insert at cursor position (consumed by UI, then cleared) */
    val pendingInsertion: String? = null,

    /** Whether a backspace operation is pending (consumed by UI, then cleared) */
    val pendingBackspace: Boolean = false,

    /** Whether a clear line operation is pending (consumed by UI, then cleared) */
    val pendingClearLine: Boolean = false
)

/**
 * UI model for a single line in the calculator.
 */
data class LineUiModel(
    /** Unique identifier for the line */
    val id: Int = 0,

    /** The raw input text */
    val input: String = "",

    /** Formatted result string to display */
    val resultText: String = "",

    /** Whether this line has an error */
    val isError: Boolean = false,

    /** Whether this line is a comment */
    val isComment: Boolean = false,

    /** Whether this line is empty */
    val isEmpty: Boolean = true
)

/**
 * Events that can be triggered from the UI.
 */
sealed class MainUiEvent {
    /** User typed or modified text on a line */
    data class LineTextChanged(val lineIndex: Int, val text: String) : MainUiEvent()

    /** User pressed Enter/newline key */
    data class NewLineRequested(val afterLineIndex: Int) : MainUiEvent()

    /** User wants to delete a line (e.g., backspace on empty line) */
    data class DeleteLineRequested(val lineIndex: Int) : MainUiEvent()

    /** User tapped a result to copy it */
    data class CopyResult(val lineIndex: Int) : MainUiEvent()

    /** User requested to copy all lines */
    data object CopyAll : MainUiEvent()

    /** User requested to clear the sheet */
    data object ClearSheet : MainUiEvent()

    /** User pressed $ key - insert $ and switch to system keyboard */
    data object DollarKeyPressed : MainUiEvent()

    /** User pressed # key - insert # and switch to system keyboard */
    data object HashKeyPressed : MainUiEvent()

    /** User pressed a key on the custom keyboard - insert at cursor position */
    data class KeyPressed(val key: String) : MainUiEvent()

    /** User pressed Enter - create new line and switch to custom keyboard */
    data class EnterKeyPressed(val afterLineIndex: Int) : MainUiEvent()

    /** User toggled keyboard manually */
    data object ToggleKeyboard : MainUiEvent()

    /** User long-pressed $ - show variable picker */
    data object ShowVariablePicker : MainUiEvent()

    /** User selected a variable from the picker */
    data class VariableSelected(val variableName: String) : MainUiEvent()

    /** User dismissed the variable picker */
    data object DismissVariablePicker : MainUiEvent()

    /** User focused on a specific line */
    data class LineFocused(val lineIndex: Int) : MainUiEvent()

    /** Toast message was shown, clear it */
    data object ToastShown : MainUiEvent()

    /** Pending insertion was consumed by UI */
    data object InsertionConsumed : MainUiEvent()

    /** User pressed backspace on custom keyboard */
    data object BackspacePressed : MainUiEvent()

    /** Pending backspace was consumed by UI */
    data object BackspaceConsumed : MainUiEvent()

    /** User pressed clear line (long-press backspace) */
    data object ClearLinePressed : MainUiEvent()

    /** Pending clear line was consumed by UI */
    data object ClearLineConsumed : MainUiEvent()
}
