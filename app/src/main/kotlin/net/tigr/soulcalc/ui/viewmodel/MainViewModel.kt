/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.tigr.soulcalc.data.repository.SheetRepository
import net.tigr.soulcalc.domain.engine.SheetEngine
import net.tigr.soulcalc.domain.model.Line
import net.tigr.soulcalc.domain.model.Result
import net.tigr.soulcalc.domain.parser.LineClassifier
import kotlin.math.abs

/**
 * ViewModel for the main calculator screen.
 *
 * Manages UI state and coordinates with SheetEngine for calculations.
 * Handles persistence through SheetRepository with debounced auto-save.
 */
class MainViewModel(
    private val repository: SheetRepository?,
    private val engine: SheetEngine = SheetEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var currentSheetId: String? = null
    private var saveJob: Job? = null
    private var focusSaveJob: Job? = null

    init {
        loadSheet()
    }

    private fun loadSheet() {
        if (repository == null) {
            updateLines(engine.evaluate(listOf("")))
            return
        }

        viewModelScope.launch {
            val sheet = repository.loadOrCreateSheet()
            currentSheetId = sheet.id
            val inputs = sheet.lines.map { it.input }
            val lines = engine.evaluate(inputs.ifEmpty { listOf("") })
            val uiLines = lines.map { it.toUiModel() }
            val restoredFocusIndex = sheet.focusedLineIndex.coerceIn(0, lines.size.coerceAtLeast(1) - 1)
            // Single atomic update for lines and focus
            _uiState.update { it.copy(
                lines = uiLines,
                focusedLineIndex = restoredFocusIndex
            )}
        }
    }

    /**
     * Handles UI events from the view.
     */
    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.LineTextChanged -> handleLineTextChanged(event.lineIndex, event.text)
            is MainUiEvent.NewLineRequested -> handleNewLine(event.afterLineIndex)
            is MainUiEvent.DeleteLineRequested -> handleDeleteLine(event.lineIndex)
            is MainUiEvent.CopyResult -> handleCopyResult(event.lineIndex)
            is MainUiEvent.CopyAll -> handleCopyAll()
            is MainUiEvent.ClearSheet -> handleClearSheet()
            is MainUiEvent.DollarKeyPressed -> handleDollarKey()
            is MainUiEvent.HashKeyPressed -> handleHashKey()
            is MainUiEvent.KeyPressed -> handleKeyPressed(event.key)
            is MainUiEvent.EnterKeyPressed -> handleEnterKey(event.afterLineIndex)
            is MainUiEvent.ToggleKeyboard -> handleToggleKeyboard()
            is MainUiEvent.ShowVariablePicker -> handleShowVariablePicker()
            is MainUiEvent.VariableSelected -> handleVariableSelected(event.variableName)
            is MainUiEvent.DismissVariablePicker -> handleDismissVariablePicker()
            is MainUiEvent.LineFocused -> handleLineFocused(event.lineIndex)
            is MainUiEvent.ToastShown -> handleToastShown()
            is MainUiEvent.InsertionConsumed -> handleInsertionConsumed()
            is MainUiEvent.BackspacePressed -> handleBackspacePressed()
            is MainUiEvent.BackspaceConsumed -> handleBackspaceConsumed()
            is MainUiEvent.ClearLinePressed -> handleClearLinePressed()
            is MainUiEvent.ClearLineConsumed -> handleClearLineConsumed()
        }
    }

    private fun handleLineTextChanged(lineIndex: Int, text: String) {
        val lines = engine.updateLine(lineIndex, text)
        updateLines(lines)
        scheduleSave()
    }

    private fun handleNewLine(afterLineIndex: Int) {
        val lines = engine.insertLine(afterLineIndex + 1, "")
        val uiLines = lines.map { it.toUiModel() }
        // Single atomic update for lines, focus, and keyboard state
        _uiState.update { it.copy(
            lines = uiLines,
            focusedLineIndex = afterLineIndex + 1,
            useCustomKeyboard = true
        )}
        scheduleSave()
    }

    private fun handleDeleteLine(lineIndex: Int) {
        val currentLines = engine.getLines()

        // Validate bounds
        if (lineIndex < 0 || lineIndex >= currentLines.size) return

        if (currentLines.size <= 1) {
            val lines = engine.updateLine(0, "")
            updateLines(lines)
            scheduleSave()
            return
        }

        val lines = engine.removeLine(lineIndex)
        val uiLines = lines.map { it.toUiModel() }
        // Single atomic update for lines and focus
        _uiState.update { it.copy(
            lines = uiLines,
            focusedLineIndex = (lineIndex - 1).coerceAtLeast(0)
        )}
        scheduleSave()
    }

    private fun handleCopyResult(lineIndex: Int) {
        val lines = _uiState.value.lines
        if (lineIndex in lines.indices) {
            val resultText = lines[lineIndex].resultText
            if (resultText.isNotEmpty() && !lines[lineIndex].isError) {
                _uiState.update { it.copy(toastMessage = "Copied: $resultText") }
            }
        }
    }

    private fun handleCopyAll() {
        val formatted = formatAllLines()
        if (formatted.isNotEmpty()) {
            _uiState.update { it.copy(toastMessage = "Copied all lines") }
        }
    }

    private fun handleClearSheet() {
        val lines = engine.clear()
        val uiLines = lines.map { it.toUiModel() }
        // Single atomic update for lines, focus, and keyboard state
        _uiState.update { it.copy(
            lines = uiLines,
            focusedLineIndex = 0,
            useCustomKeyboard = true
        )}
        scheduleSave()
    }

    private fun handleDollarKey() {
        // Set pending insertion for "$" and switch to system keyboard.
        // LineRow will insert at cursor position via pendingInsertion handler.
        _uiState.update { it.copy(pendingInsertion = "$", useCustomKeyboard = false) }
    }

    private fun handleHashKey() {
        // Set pending insertion for "#" and switch to system keyboard.
        // LineRow will insert at cursor position via pendingInsertion handler.
        _uiState.update { it.copy(pendingInsertion = "#", useCustomKeyboard = false) }
    }

    private fun handleEnterKey(afterLineIndex: Int) {
        // Delegate to handleNewLine - both do the same: create line and switch keyboard
        handleNewLine(afterLineIndex)
    }

    private fun handleToggleKeyboard() {
        _uiState.update { it.copy(useCustomKeyboard = !it.useCustomKeyboard) }
    }

    private fun handleShowVariablePicker() {
        val scope = engine.getScope()
        val variables = scope.variables.keys.toList().sorted()
        _uiState.update { it.copy(
            showVariablePicker = true,
            availableVariables = variables
        )}
    }

    private fun handleVariableSelected(variableName: String) {
        _uiState.update { it.copy(
            showVariablePicker = false,
            pendingInsertion = "\$$variableName"
        )}
    }

    private fun handleDismissVariablePicker() {
        _uiState.update { it.copy(showVariablePicker = false) }
    }

    private fun handleLineFocused(lineIndex: Int) {
        _uiState.update { it.copy(focusedLineIndex = lineIndex) }
        scheduleFocusSave(lineIndex)
    }

    private fun handleToastShown() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun handleInsertionConsumed() {
        _uiState.update { it.copy(pendingInsertion = null) }
    }

    private fun handleKeyPressed(key: String) {
        // Set pending insertion - LineRow will insert at cursor position
        _uiState.update { it.copy(pendingInsertion = key) }
    }

    private fun handleBackspacePressed() {
        // Set pending backspace - LineRow will delete at cursor position
        _uiState.update { it.copy(pendingBackspace = true) }
    }

    private fun handleBackspaceConsumed() {
        _uiState.update { it.copy(pendingBackspace = false) }
    }

    private fun handleClearLinePressed() {
        // Set pending clear line - LineRow will clear the entire line
        _uiState.update { it.copy(pendingClearLine = true) }
    }

    private fun handleClearLineConsumed() {
        _uiState.update { it.copy(pendingClearLine = false) }
    }

    private fun updateLines(lines: List<Line>) {
        val uiLines = lines.map { it.toUiModel() }
        _uiState.update { it.copy(lines = uiLines) }
    }

    private fun scheduleSave() {
        if (repository == null || currentSheetId == null) return

        // Capture state snapshot BEFORE delay to avoid race conditions
        val sheetId = currentSheetId ?: return
        val inputs = engine.getLines().map { it.input }

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            repository.saveLines(sheetId, inputs)
        }
    }

    private fun scheduleFocusSave(focusedLineIndex: Int) {
        if (repository == null || currentSheetId == null) return

        // Capture state snapshot BEFORE delay to avoid race conditions
        val sheetId = currentSheetId ?: return

        focusSaveJob?.cancel()
        focusSaveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            repository.saveFocusedLineIndex(sheetId, focusedLineIndex)
        }
    }

    private fun Line.toUiModel(): LineUiModel {
        return LineUiModel(
            id = this.id,
            input = this.input,
            resultText = formatResult(this.result),
            isError = this.result is Result.Error,
            isComment = LineClassifier.isComment(this.input),
            isEmpty = LineClassifier.isEmpty(this.input)
        )
    }

    private fun formatResult(result: Result): String {
        return when (result) {
            is Result.Success -> formatNumber(result.value)
            is Result.Error -> formatError(result.message)
            is Result.Empty -> ""
        }
    }

    private fun formatError(message: String): String {
        return when {
            // Display-ready symbols from Evaluator - pass through as-is
            message == "∞" || message == "-∞" || message == "NaN" -> message
            // Error messages with context
            message.contains("undefined variable", ignoreCase = true) -> {
                val varName = Regex("\\$\\w+").find(message)?.value ?: ""
                "? $varName"
            }
            message.contains("invalid line reference", ignoreCase = true) -> {
                val lineRef = Regex("\\$\\d+").find(message)?.value ?: ""
                "? $lineRef"
            }
            // Evaluator error messages that include context (e.g., "? $varName")
            message.startsWith("?") -> message
            else -> "?"
        }
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"

        if (value == value.toLong().toDouble() && abs(value) < 1e15) {
            return value.toLong().toString()
        }

        val formatted = "%.10g".format(value)
        // Strip trailing zeros and unnecessary decimal point
        return formatted
            .replace(Regex("(\\.\\d*?)0+$"), "$1")
            .replace(Regex("\\.$"), "")
    }

    /**
     * Formats all lines for "Copy All" functionality.
     */
    fun formatAllLines(): String {
        return _uiState.value.lines
            .filter { !it.isEmpty }
            .joinToString("\n") { line ->
                if (line.isComment) {
                    line.input
                } else if (line.resultText.isNotEmpty()) {
                    "${line.input} = ${line.resultText}"
                } else {
                    line.input
                }
            }
    }

    /**
     * Gets the result text for a specific line (for clipboard operations).
     */
    fun getResultForClipboard(lineIndex: Int): String? {
        val lines = _uiState.value.lines
        if (lineIndex !in lines.indices) return null
        val line = lines[lineIndex]
        if (line.isError || line.resultText.isEmpty()) return null
        return line.resultText
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 500L
    }

    class Factory(private val repository: SheetRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
