// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import SwiftUI
import Combine

// MARK: - UI Models

/// UI model for a single line in the calculator.
struct LineUiModel: Identifiable {
    let id: Int
    let input: String
    let resultText: String
    let isError: Bool
    let isComment: Bool
    let isEmpty: Bool

    init(id: Int = 0, input: String = "", resultText: String = "",
         isError: Bool = false, isComment: Bool = false, isEmpty: Bool = true) {
        self.id = id
        self.input = input
        self.resultText = resultText
        self.isError = isError
        self.isComment = isComment
        self.isEmpty = isEmpty
    }
}

/// UI state for the main calculator screen.
struct MainUiState {
    var lines: [LineUiModel] = [LineUiModel()]
    var focusedLineIndex: Int = 0
    var useCustomKeyboard: Bool = true
    var showVariablePicker: Bool = false
    var availableVariables: [String] = []
    var toastMessage: String? = nil
    var pendingInsertion: String? = nil
    var pendingBackspace: Bool = false
    var pendingClearLine: Bool = false
}

// MARK: - ViewModel

/// ViewModel for the main calculator screen.
///
/// Manages UI state and coordinates with SheetEngine for calculations.
/// Handles persistence through SheetStore with debounced auto-save.
@MainActor
class MainViewModel: ObservableObject {
    @Published var state = MainUiState()

    private let engine = SheetEngine()
    private let store: SheetStore?
    private var saveTask: Task<Void, Never>?
    private var focusSaveTask: Task<Void, Never>?

    private static let saveDebounceNs: UInt64 = 500_000_000 // 500ms

    init(store: SheetStore? = SheetStore()) {
        self.store = store
        loadSheet()
    }

    private func loadSheet() {
        guard let store = store, let data = store.load(), !data.lines.isEmpty else {
            let lines = engine.evaluate([""])
            updateLines(lines)
            return
        }

        let lines = engine.evaluate(data.lines)
        let uiLines = lines.map { $0.toUiModel() }
        let restoredFocus = max(0, min(data.focusedLineIndex, lines.count - 1))
        state.lines = uiLines
        state.focusedLineIndex = restoredFocus
    }

    // MARK: - Event Handlers

    func lineTextChanged(_ lineIndex: Int, _ text: String) {
        let lines = engine.updateLine(lineIndex, text)
        updateLines(lines)
        scheduleSave()
    }

    func newLineRequested(_ afterLineIndex: Int) {
        let lines = engine.insertLine(afterLineIndex + 1, "")
        let uiLines = lines.map { $0.toUiModel() }
        state.lines = uiLines
        state.focusedLineIndex = afterLineIndex + 1
        state.useCustomKeyboard = true
        scheduleSave()
    }

    func deleteLineRequested(_ lineIndex: Int) {
        let currentLines = engine.getLines()
        if lineIndex < 0 || lineIndex >= currentLines.count { return }

        if currentLines.count <= 1 {
            let lines = engine.updateLine(0, "")
            updateLines(lines)
            scheduleSave()
            return
        }

        let lines = engine.removeLine(lineIndex)
        let uiLines = lines.map { $0.toUiModel() }
        state.lines = uiLines
        state.focusedLineIndex = max(lineIndex - 1, 0)
        scheduleSave()
    }

    func copyResult(_ lineIndex: Int) -> String? {
        let lines = state.lines
        guard lineIndex >= 0, lineIndex < lines.count else { return nil }
        let line = lines[lineIndex]
        guard !line.isError, !line.resultText.isEmpty else { return nil }
        state.toastMessage = "Copied: \(line.resultText)"
        return line.resultText
    }

    func copyAll() -> String {
        let formatted = formatAllLines()
        if !formatted.isEmpty {
            state.toastMessage = "Copied all lines"
        }
        return formatted
    }

    func clearSheet() {
        let lines = engine.clear()
        let uiLines = lines.map { $0.toUiModel() }
        state.lines = uiLines
        state.focusedLineIndex = 0
        state.useCustomKeyboard = true
        scheduleSave()
    }

    func dollarKeyPressed() {
        state.pendingInsertion = "$"
        state.useCustomKeyboard = false
    }

    func hashKeyPressed() {
        state.pendingInsertion = "#"
        state.useCustomKeyboard = false
    }

    func keyPressed(_ key: String) {
        state.pendingInsertion = key
    }

    func enterKeyPressed() {
        newLineRequested(state.focusedLineIndex)
    }

    func toggleKeyboard() {
        state.useCustomKeyboard.toggle()
    }

    func showVariablePicker() {
        let scope = engine.getScope()
        let variables = scope.variables.keys.sorted()
        state.showVariablePicker = true
        state.availableVariables = variables
    }

    func variableSelected(_ name: String) {
        state.showVariablePicker = false
        state.pendingInsertion = "$\(name)"
    }

    func dismissVariablePicker() {
        state.showVariablePicker = false
    }

    func lineFocused(_ lineIndex: Int) {
        state.focusedLineIndex = lineIndex
        scheduleFocusSave(lineIndex)
    }

    func toastShown() {
        state.toastMessage = nil
    }

    func insertionConsumed() {
        state.pendingInsertion = nil
    }

    func backspacePressed() {
        state.pendingBackspace = true
    }

    func backspaceConsumed() {
        state.pendingBackspace = false
    }

    func clearLinePressed() {
        state.pendingClearLine = true
    }

    func clearLineConsumed() {
        state.pendingClearLine = false
    }

    // MARK: - Formatting

    func formatAllLines() -> String {
        state.lines
            .filter { !$0.isEmpty }
            .map { line in
                if line.isComment {
                    return line.input
                } else if !line.resultText.isEmpty {
                    return "\(line.input) = \(line.resultText)"
                } else {
                    return line.input
                }
            }
            .joined(separator: "\n")
    }

    // MARK: - Private

    private func updateLines(_ lines: [Line]) {
        state.lines = lines.map { $0.toUiModel() }
    }

    private func scheduleSave() {
        guard let store = store else { return }
        let inputs = engine.getLines().map { $0.input }

        saveTask?.cancel()
        saveTask = Task {
            try? await Task.sleep(nanoseconds: Self.saveDebounceNs)
            guard !Task.isCancelled else { return }
            store.saveLines(inputs)
        }
    }

    private func scheduleFocusSave(_ index: Int) {
        guard store != nil else { return }

        focusSaveTask?.cancel()
        focusSaveTask = Task {
            try? await Task.sleep(nanoseconds: Self.saveDebounceNs)
            guard !Task.isCancelled else { return }
            store?.saveFocusedLineIndex(index)
        }
    }
}

// MARK: - Line → UI Model conversion

private extension Line {
    func toUiModel() -> LineUiModel {
        LineUiModel(
            id: self.id,
            input: self.input,
            resultText: Self.formatResult(self.result),
            isError: {
                if case .error = self.result { return true }
                return false
            }(),
            isComment: LineClassifier.isComment(self.input),
            isEmpty: LineClassifier.isEmpty(self.input)
        )
    }

    static func formatResult(_ result: Result) -> String {
        switch result {
        case .success(let value):
            return formatNumber(value)
        case .error(let message):
            return formatError(message)
        case .empty:
            return ""
        }
    }

    static func formatError(_ message: String) -> String {
        if message == "∞" || message == "-∞" || message == "NaN" {
            return message
        }
        if message.hasPrefix("?") {
            return message
        }
        return "?"
    }

    static func formatNumber(_ value: Double) -> String {
        if value.isNaN { return "NaN" }
        if value.isInfinite { return value > 0 ? "∞" : "-∞" }

        // Display integers without decimal point
        if value == Double(Int64(value)) && abs(value) < 1e15 {
            return String(Int64(value))
        }

        // Format with up to 10 significant digits, strip trailing zeros
        let formatted = String(format: "%.10g", value)
        return formatted
            .replacingOccurrences(of: "(\\.\\d*?)0+$", with: "$1", options: .regularExpression)
            .replacingOccurrences(of: "\\.$", with: "", options: .regularExpression)
    }
}
