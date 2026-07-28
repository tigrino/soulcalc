// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import XCTest
@testable import SoulCalcDomain

@MainActor
final class MainViewModelTests: XCTestCase {

    var viewModel: MainViewModel!

    override func setUp() {
        super.setUp()
        viewModel = MainViewModel(store: nil) // no persistence for tests
    }

    // MARK: - Initial State

    func testInitialState() {
        XCTAssertEqual(viewModel.state.lines.count, 1)
        XCTAssertEqual(viewModel.state.focusedLineIndex, 0)
        XCTAssertTrue(viewModel.state.useCustomKeyboard)
        XCTAssertFalse(viewModel.state.showVariablePicker)
        XCTAssertNil(viewModel.state.toastMessage)
        XCTAssertNil(viewModel.state.pendingInsertion)
        XCTAssertFalse(viewModel.state.pendingBackspace)
        XCTAssertFalse(viewModel.state.pendingClearLine)
    }

    // MARK: - Line Text Changed

    func testLineTextChanged() {
        viewModel.lineTextChanged(0, "42")
        XCTAssertEqual(viewModel.state.lines[0].resultText, "42")
    }

    func testLineTextChangedExpression() {
        viewModel.lineTextChanged(0, "2 + 3")
        XCTAssertEqual(viewModel.state.lines[0].resultText, "5")
    }

    func testLineTextChangedError() {
        viewModel.lineTextChanged(0, "1 / 0")
        XCTAssertTrue(viewModel.state.lines[0].isError)
    }

    // MARK: - New Line

    func testNewLine() {
        viewModel.lineTextChanged(0, "10")
        viewModel.newLineRequested(0)

        XCTAssertEqual(viewModel.state.lines.count, 2)
        XCTAssertEqual(viewModel.state.focusedLineIndex, 1)
        XCTAssertTrue(viewModel.state.useCustomKeyboard)
    }

    func testNewLinePreservesExisting() {
        viewModel.lineTextChanged(0, "10")
        viewModel.newLineRequested(0)

        XCTAssertEqual(viewModel.state.lines[0].resultText, "10")
        XCTAssertEqual(viewModel.state.lines[1].resultText, "")
    }

    // MARK: - Delete Line

    func testDeleteLine() {
        viewModel.lineTextChanged(0, "10")
        viewModel.newLineRequested(0)
        viewModel.lineTextChanged(1, "20")

        viewModel.deleteLineRequested(1)
        XCTAssertEqual(viewModel.state.lines.count, 1)
        XCTAssertEqual(viewModel.state.lines[0].resultText, "10")
    }

    func testDeleteOnlyLineClearsIt() {
        viewModel.lineTextChanged(0, "42")
        viewModel.deleteLineRequested(0)

        XCTAssertEqual(viewModel.state.lines.count, 1)
        XCTAssertEqual(viewModel.state.lines[0].input, "")
    }

    // MARK: - Clear Sheet

    func testClearSheet() {
        viewModel.lineTextChanged(0, "10")
        viewModel.newLineRequested(0)
        viewModel.lineTextChanged(1, "20")

        viewModel.clearSheet()

        XCTAssertEqual(viewModel.state.lines.count, 1)
        XCTAssertEqual(viewModel.state.lines[0].input, "")
        XCTAssertEqual(viewModel.state.focusedLineIndex, 0)
    }

    // MARK: - Keyboard Toggle

    func testToggleKeyboard() {
        XCTAssertTrue(viewModel.state.useCustomKeyboard)
        viewModel.toggleKeyboard()
        XCTAssertFalse(viewModel.state.useCustomKeyboard)
        viewModel.toggleKeyboard()
        XCTAssertTrue(viewModel.state.useCustomKeyboard)
    }

    // MARK: - Dollar and Hash Keys

    func testDollarKey() {
        viewModel.dollarKeyPressed()
        XCTAssertEqual(viewModel.state.pendingInsertion, "$")
        XCTAssertFalse(viewModel.state.useCustomKeyboard)
    }

    func testHashKey() {
        viewModel.hashKeyPressed()
        XCTAssertEqual(viewModel.state.pendingInsertion, "#")
        XCTAssertFalse(viewModel.state.useCustomKeyboard)
    }

    // MARK: - Key Pressed

    func testKeyPressed() {
        viewModel.keyPressed("5")
        XCTAssertEqual(viewModel.state.pendingInsertion, "5")
    }

    func testKeyPressedOperator() {
        viewModel.keyPressed("+")
        XCTAssertEqual(viewModel.state.pendingInsertion, "+")
    }

    // MARK: - Backspace and Clear Line

    func testBackspacePressed() {
        viewModel.backspacePressed()
        XCTAssertTrue(viewModel.state.pendingBackspace)
        viewModel.backspaceConsumed()
        XCTAssertFalse(viewModel.state.pendingBackspace)
    }

    func testClearLinePressed() {
        viewModel.clearLinePressed()
        XCTAssertTrue(viewModel.state.pendingClearLine)
        viewModel.clearLineConsumed()
        XCTAssertFalse(viewModel.state.pendingClearLine)
    }

    // MARK: - Variable Picker

    func testShowVariablePicker() {
        viewModel.lineTextChanged(0, "$x = 10")
        viewModel.showVariablePicker()

        XCTAssertTrue(viewModel.state.showVariablePicker)
        XCTAssertEqual(viewModel.state.availableVariables, ["x"])
    }

    func testVariableSelected() {
        viewModel.lineTextChanged(0, "$x = 10")
        viewModel.showVariablePicker()
        viewModel.variableSelected("x")

        XCTAssertFalse(viewModel.state.showVariablePicker)
        XCTAssertEqual(viewModel.state.pendingInsertion, "$x")
    }

    func testDismissVariablePicker() {
        viewModel.showVariablePicker()
        viewModel.dismissVariablePicker()
        XCTAssertFalse(viewModel.state.showVariablePicker)
    }

    // MARK: - Line Focus

    func testLineFocused() {
        viewModel.newLineRequested(0)
        viewModel.lineFocused(0)
        XCTAssertEqual(viewModel.state.focusedLineIndex, 0)
    }

    // MARK: - Toast

    func testToastShown() {
        viewModel.lineTextChanged(0, "42")
        _ = viewModel.copyResult(0)
        XCTAssertNotNil(viewModel.state.toastMessage)

        viewModel.toastShown()
        XCTAssertNil(viewModel.state.toastMessage)
    }

    // MARK: - Copy

    func testCopyResult() {
        viewModel.lineTextChanged(0, "42")
        let result = viewModel.copyResult(0)
        XCTAssertEqual(result, "42")
    }

    func testCopyResultError() {
        viewModel.lineTextChanged(0, "1 / 0")
        let result = viewModel.copyResult(0)
        XCTAssertNil(result)
    }

    func testCopyResultEmpty() {
        let result = viewModel.copyResult(0)
        XCTAssertNil(result)
    }

    // MARK: - Format All Lines

    func testFormatAllLines() {
        viewModel.lineTextChanged(0, "10")
        viewModel.newLineRequested(0)
        viewModel.lineTextChanged(1, "20")

        let formatted = viewModel.formatAllLines()
        XCTAssertTrue(formatted.contains("10 = 10"))
        XCTAssertTrue(formatted.contains("20 = 20"))
    }

    func testFormatAllLinesWithComment() {
        viewModel.lineTextChanged(0, "# Total")
        viewModel.newLineRequested(0)
        viewModel.lineTextChanged(1, "42")

        let formatted = viewModel.formatAllLines()
        XCTAssertTrue(formatted.contains("# Total"))
        XCTAssertTrue(formatted.contains("42 = 42"))
    }

    // MARK: - Number Formatting

    func testIntegerFormatting() {
        viewModel.lineTextChanged(0, "2 + 3")
        XCTAssertEqual(viewModel.state.lines[0].resultText, "5")
    }

    func testDecimalFormatting() {
        viewModel.lineTextChanged(0, "1 / 3")
        let result = viewModel.state.lines[0].resultText
        XCTAssertTrue(result.hasPrefix("0.333333333"))
    }

    func testLargeIntegerFormatting() {
        viewModel.lineTextChanged(0, "1000000")
        XCTAssertEqual(viewModel.state.lines[0].resultText, "1000000")
    }

    func testLiteralBeyondDoublePrecisionShowsDigitsError() {
        viewModel.lineTextChanged(0, "10000000000000000.1 - 10000000000000000")
        XCTAssertEqual(viewModel.state.lines[0].resultText, "? digits")
        XCTAssertTrue(viewModel.state.lines[0].isError)
    }

    // MARK: - Insertion Consumed

    func testInsertionConsumed() {
        viewModel.keyPressed("5")
        XCTAssertNotNil(viewModel.state.pendingInsertion)
        viewModel.insertionConsumed()
        XCTAssertNil(viewModel.state.pendingInsertion)
    }

    // MARK: - Enter Key

    func testEnterKey() {
        viewModel.lineTextChanged(0, "10")
        viewModel.enterKeyPressed()

        XCTAssertEqual(viewModel.state.lines.count, 2)
        XCTAssertEqual(viewModel.state.focusedLineIndex, 1)
    }

    // MARK: - Clear Sheet Forces Custom Keyboard

    func testClearSheetForcesCustomKeyboard() {
        viewModel.lineTextChanged(0, "10")
        viewModel.toggleKeyboard() // now useCustomKeyboard = false
        XCTAssertFalse(viewModel.state.useCustomKeyboard)

        viewModel.clearSheet()

        XCTAssertTrue(viewModel.state.useCustomKeyboard)
    }

    // MARK: - New Line Forces Custom Keyboard

    func testNewLineForcesCustomKeyboard() {
        viewModel.lineTextChanged(0, "10")
        viewModel.toggleKeyboard() // now useCustomKeyboard = false
        XCTAssertFalse(viewModel.state.useCustomKeyboard)

        viewModel.newLineRequested(0)

        XCTAssertTrue(viewModel.state.useCustomKeyboard)
    }

    // MARK: - Delete Line Focuses Previous Line

    func testDeleteLineFocusesPreviousLine() {
        viewModel.lineTextChanged(0, "10")
        viewModel.newLineRequested(0)
        viewModel.lineTextChanged(1, "20")
        viewModel.newLineRequested(1)
        viewModel.lineTextChanged(2, "30")

        XCTAssertEqual(viewModel.state.lines.count, 3)

        viewModel.deleteLineRequested(2)

        XCTAssertEqual(viewModel.state.lines.count, 2)
        XCTAssertEqual(viewModel.state.focusedLineIndex, 1)
    }

    // MARK: - Copy All Formats Correctly

    func testCopyAllFormatsCorrectly() {
        viewModel.lineTextChanged(0, "10")
        viewModel.newLineRequested(0)
        viewModel.lineTextChanged(1, "$1 + 5")

        let result = viewModel.copyAll()

        XCTAssertTrue(result.contains("10 = 10"))
        XCTAssertTrue(result.contains("$1 + 5 = 15"))
    }
}
