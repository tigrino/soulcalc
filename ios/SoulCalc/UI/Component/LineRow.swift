// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import SwiftUI
#if canImport(AppKit)
import AppKit
#endif

/// A single line row in the calculator.
/// Displays a line number, input field (65% width), and result (30% width).
struct LineRow: View {
    let line: LineUiModel
    let lineNumber: Int
    let isFocused: Bool
    let pendingInsertion: String?
    let pendingBackspace: Bool
    let pendingClearLine: Bool
    let showSystemKeyboard: Bool
    let onTextChanged: (String) -> Void
    let onNewLine: () -> Void
    let onDeleteLine: () -> Void
    let onResultTap: () -> Void
    let onFocused: () -> Void
    let onInsertionConsumed: () -> Void
    let onBackspaceConsumed: () -> Void
    let onClearLineConsumed: () -> Void

    @State private var text: String = ""
    @FocusState private var isTextFieldFocused: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            // Line number
            Text("\(lineNumber)")
                .font(.caption)
                .foregroundStyle(.secondary.opacity(0.6))
                .frame(width: 24, alignment: .trailing)
                .padding(.trailing, 8)
                .padding(.top, 6)

            // Input field
            TextField("", text: $text, axis: .vertical)
                .textFieldStyle(.plain)
                .font(.body)
                .foregroundStyle(line.isComment ? .secondary : .primary)
                .focused($isTextFieldFocused)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(
                    RoundedRectangle(cornerRadius: 4)
                        #if canImport(UIKit)
                        .fill(Color(.systemGray6).opacity(0.5))
                        #else
                        .fill(Color(NSColor.controlBackgroundColor).opacity(0.5))
                        #endif
                )
                .frame(maxWidth: .infinity)
                .onChange(of: text) { _, newValue in
                    if newValue != line.input {
                        onTextChanged(newValue)
                    }
                }
                .onChange(of: isTextFieldFocused) { _, focused in
                    if focused {
                        onFocused()
                    }
                }
                .onSubmit {
                    onNewLine()
                }

            Spacer().frame(width: 8)

            // Result display
            Text(line.resultText)
                .font(.body)
                .foregroundStyle(resultColor)
                .frame(maxWidth: .infinity, alignment: .trailing)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color.accentColor.opacity(0.08))
                )
                .frame(minWidth: 80, maxWidth: 120)
                .onTapGesture {
                    if !line.resultText.isEmpty && !line.isError {
                        onResultTap()
                    }
                }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 2)
        .onAppear {
            text = line.input
        }
        .onChange(of: line.input) { _, newInput in
            if newInput != text {
                text = newInput
            }
        }
        .onChange(of: isFocused) { _, focused in
            if focused && showSystemKeyboard {
                isTextFieldFocused = true
            }
        }
        .onChange(of: showSystemKeyboard) { _, show in
            if show && isFocused {
                isTextFieldFocused = true
            } else if !show {
                isTextFieldFocused = false
            }
        }
        .onChange(of: pendingInsertion) { _, insertion in
            guard let insertion = insertion, isFocused else { return }
            text.append(insertion)
            onTextChanged(text)
            onInsertionConsumed()
        }
        .onChange(of: pendingBackspace) { _, backspace in
            guard backspace, isFocused else { return }
            if text.isEmpty {
                onDeleteLine()
            } else if text.hasSuffix("sqrt(") {
                text.removeLast(5)
                onTextChanged(text)
            } else if !text.isEmpty {
                text.removeLast()
                onTextChanged(text)
            }
            onBackspaceConsumed()
        }
        .onChange(of: pendingClearLine) { _, clear in
            guard clear, isFocused else { return }
            text = ""
            onTextChanged("")
            onClearLineConsumed()
        }
    }

    private var resultColor: Color {
        if line.isError {
            return .red
        } else if line.resultText.isEmpty {
            return .secondary
        } else {
            return .accentColor
        }
    }
}
