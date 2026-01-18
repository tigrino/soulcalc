/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.ui.viewmodel

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        viewModel = MainViewModel(repository = null)
    }

    // === Initial State ===

    @Test
    fun `initial state has one empty line`() {
        val state = viewModel.uiState.value
        assertEquals(1, state.lines.size)
        assertEquals("", state.lines[0].input)
        assertTrue(state.lines[0].isEmpty)
    }

    @Test
    fun `initial state uses custom keyboard`() {
        val state = viewModel.uiState.value
        assertTrue(state.useCustomKeyboard)
    }

    @Test
    fun `initial state has no toast message`() {
        val state = viewModel.uiState.value
        assertNull(state.toastMessage)
    }

    // === Line Text Changed ===

    @Test
    fun `typing a number shows result`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))

        val state = viewModel.uiState.value
        assertEquals("100", state.lines[0].input)
        assertEquals("100", state.lines[0].resultText)
        assertFalse(state.lines[0].isError)
    }

    @Test
    fun `typing an expression evaluates it`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "50 + 50"))

        val state = viewModel.uiState.value
        assertEquals("50 + 50", state.lines[0].input)
        assertEquals("100", state.lines[0].resultText)
    }

    @Test
    fun `typing invalid expression shows error`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "1 +"))

        val state = viewModel.uiState.value
        assertTrue(state.lines[0].isError)
    }

    @Test
    fun `typing comment shows empty result`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "# comment"))

        val state = viewModel.uiState.value
        assertTrue(state.lines[0].isComment)
        assertEquals("", state.lines[0].resultText)
    }

    // === New Line ===

    @Test
    fun `new line request adds line`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))

        val state = viewModel.uiState.value
        assertEquals(2, state.lines.size)
        assertEquals("100", state.lines[0].input)
        assertEquals("", state.lines[1].input)
    }

    @Test
    fun `new line focuses on new line`() {
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))

        val state = viewModel.uiState.value
        assertEquals(1, state.focusedLineIndex)
    }

    @Test
    fun `new line returns to custom keyboard`() {
        viewModel.onEvent(MainUiEvent.DollarKeyPressed) // switch to system
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))

        val state = viewModel.uiState.value
        assertTrue(state.useCustomKeyboard)
    }

    // === Delete Line ===

    @Test
    fun `delete line removes it`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.LineTextChanged(1, "200"))

        viewModel.onEvent(MainUiEvent.DeleteLineRequested(1))

        val state = viewModel.uiState.value
        assertEquals(1, state.lines.size)
        assertEquals("100", state.lines[0].input)
    }

    @Test
    fun `delete last remaining line clears it`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))
        viewModel.onEvent(MainUiEvent.DeleteLineRequested(0))

        val state = viewModel.uiState.value
        assertEquals(1, state.lines.size)
        assertEquals("", state.lines[0].input)
    }

    @Test
    fun `delete line updates focus`() {
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.NewLineRequested(1))
        viewModel.onEvent(MainUiEvent.DeleteLineRequested(2))

        val state = viewModel.uiState.value
        assertEquals(1, state.focusedLineIndex)
    }

    // === Variables ===

    @Test
    fun `variable definition and usage`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "\$x = 10"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.LineTextChanged(1, "\$x + 5"))

        val state = viewModel.uiState.value
        assertEquals("10", state.lines[0].resultText)
        assertEquals("15", state.lines[1].resultText)
    }

    @Test
    fun `variable update cascades`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "\$x = 10"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.LineTextChanged(1, "\$x * 2"))

        // Update variable
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "\$x = 20"))

        val state = viewModel.uiState.value
        assertEquals("20", state.lines[0].resultText)
        assertEquals("40", state.lines[1].resultText)
    }

    // === Line References ===

    @Test
    fun `line reference works`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.LineTextChanged(1, "\$1 * 2"))

        val state = viewModel.uiState.value
        assertEquals("100", state.lines[0].resultText)
        assertEquals("200", state.lines[1].resultText)
    }

    // === Keyboard Switching ===

    @Test
    fun `dollar key sets pending insertion and switches to system keyboard`() {
        viewModel.onEvent(MainUiEvent.DollarKeyPressed)

        val state = viewModel.uiState.value
        assertFalse(state.useCustomKeyboard)
        assertEquals("$", state.pendingInsertion)
    }

    @Test
    fun `hash key sets pending insertion and switches to system keyboard`() {
        viewModel.onEvent(MainUiEvent.HashKeyPressed)

        val state = viewModel.uiState.value
        assertFalse(state.useCustomKeyboard)
        assertEquals("#", state.pendingInsertion)
    }

    @Test
    fun `enter key creates new line and switches to custom keyboard`() {
        viewModel.onEvent(MainUiEvent.DollarKeyPressed)
        viewModel.onEvent(MainUiEvent.EnterKeyPressed(0))

        val state = viewModel.uiState.value
        assertTrue(state.useCustomKeyboard)
        assertEquals(2, state.lines.size)
        assertEquals(1, state.focusedLineIndex)
    }

    @Test
    fun `toggle keyboard switches state`() {
        assertTrue(viewModel.uiState.value.useCustomKeyboard)

        viewModel.onEvent(MainUiEvent.ToggleKeyboard)
        assertFalse(viewModel.uiState.value.useCustomKeyboard)

        viewModel.onEvent(MainUiEvent.ToggleKeyboard)
        assertTrue(viewModel.uiState.value.useCustomKeyboard)
    }

    // === Variable Picker ===

    @Test
    fun `show variable picker shows available variables`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "\$tax = 0.08"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.LineTextChanged(1, "\$price = 100"))

        viewModel.onEvent(MainUiEvent.ShowVariablePicker)

        val state = viewModel.uiState.value
        assertTrue(state.showVariablePicker)
        assertTrue(state.availableVariables.contains("tax"))
        assertTrue(state.availableVariables.contains("price"))
    }

    @Test
    fun `variable selected dismisses picker`() {
        viewModel.onEvent(MainUiEvent.ShowVariablePicker)
        viewModel.onEvent(MainUiEvent.VariableSelected("tax"))

        val state = viewModel.uiState.value
        assertFalse(state.showVariablePicker)
    }

    @Test
    fun `variable selected sets pending insertion`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "\$tax = 0.08"))
        viewModel.onEvent(MainUiEvent.ShowVariablePicker)
        viewModel.onEvent(MainUiEvent.VariableSelected("tax"))

        val state = viewModel.uiState.value
        assertEquals("\$tax", state.pendingInsertion)
    }

    @Test
    fun `insertion consumed clears pending insertion`() {
        viewModel.onEvent(MainUiEvent.VariableSelected("tax"))
        viewModel.onEvent(MainUiEvent.InsertionConsumed)

        val state = viewModel.uiState.value
        assertNull(state.pendingInsertion)
    }

    @Test
    fun `dismiss variable picker hides it`() {
        viewModel.onEvent(MainUiEvent.ShowVariablePicker)
        viewModel.onEvent(MainUiEvent.DismissVariablePicker)

        val state = viewModel.uiState.value
        assertFalse(state.showVariablePicker)
    }

    // === Copy Operations ===

    @Test
    fun `copy result shows toast`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))
        viewModel.onEvent(MainUiEvent.CopyResult(0))

        val state = viewModel.uiState.value
        assertNotNull(state.toastMessage)
        assertTrue(state.toastMessage!!.contains("100"))
    }

    @Test
    fun `copy all shows toast`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))
        viewModel.onEvent(MainUiEvent.CopyAll)

        val state = viewModel.uiState.value
        assertNotNull(state.toastMessage)
    }

    @Test
    fun `toast shown clears message`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))
        viewModel.onEvent(MainUiEvent.CopyResult(0))
        viewModel.onEvent(MainUiEvent.ToastShown)

        val state = viewModel.uiState.value
        assertNull(state.toastMessage)
    }

    // === Clear Sheet ===

    @Test
    fun `clear sheet removes all content`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.LineTextChanged(1, "200"))

        viewModel.onEvent(MainUiEvent.ClearSheet)

        val state = viewModel.uiState.value
        assertEquals(1, state.lines.size)
        assertEquals("", state.lines[0].input)
        assertEquals(0, state.focusedLineIndex)
    }

    // === Focus Management ===

    @Test
    fun `line focused updates focused index`() {
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.NewLineRequested(1))

        viewModel.onEvent(MainUiEvent.LineFocused(1))

        val state = viewModel.uiState.value
        assertEquals(1, state.focusedLineIndex)
    }

    // === Number Formatting ===

    @Test
    fun `integer result formatted without decimal`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100"))

        val state = viewModel.uiState.value
        assertEquals("100", state.lines[0].resultText)
    }

    @Test
    fun `decimal result formatted correctly`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "1 / 3"))

        val state = viewModel.uiState.value
        assertTrue(state.lines[0].resultText.startsWith("0.333"))
    }

    @Test
    fun `large integer formatted correctly`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "999999999999"))

        val state = viewModel.uiState.value
        assertEquals("999999999999", state.lines[0].resultText)
    }

    // === Format All Lines ===

    @Test
    fun `format all lines creates proper output`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "\$price = 49.99"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.LineTextChanged(1, "\$qty = 3"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(1))
        viewModel.onEvent(MainUiEvent.LineTextChanged(2, "\$price * \$qty"))

        val formatted = viewModel.formatAllLines()

        assertTrue(formatted.contains("\$price = 49.99 = 49.99"))
        assertTrue(formatted.contains("\$qty = 3 = 3"))
        assertTrue(formatted.contains("\$price * \$qty = 149.97"))
    }

    @Test
    fun `format all lines includes comments as-is`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "# Shopping"))
        viewModel.onEvent(MainUiEvent.NewLineRequested(0))
        viewModel.onEvent(MainUiEvent.LineTextChanged(1, "100"))

        val formatted = viewModel.formatAllLines()

        assertTrue(formatted.contains("# Shopping"))
    }

    // === Get Result for Clipboard ===

    @Test
    fun `get result returns value for valid line`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "42"))

        val result = viewModel.getResultForClipboard(0)
        assertEquals("42", result)
    }

    @Test
    fun `get result returns null for error line`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "1 / 0"))

        val result = viewModel.getResultForClipboard(0)
        assertNull(result)
    }

    @Test
    fun `get result returns null for empty line`() {
        val result = viewModel.getResultForClipboard(0)
        assertNull(result)
    }

    @Test
    fun `get result returns null for invalid index`() {
        val result = viewModel.getResultForClipboard(999)
        assertNull(result)
    }

    // === Percentage Operations ===

    @Test
    fun `percentage in expression evaluated correctly`() {
        viewModel.onEvent(MainUiEvent.LineTextChanged(0, "100 + 10%"))

        val state = viewModel.uiState.value
        assertEquals("110", state.lines[0].resultText)
    }
}
