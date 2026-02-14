// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// Custom calculator keyboard.
///
/// Layout:
/// ```
///  ^    #   $   =   ⌫
///  7    8   9   ÷   (
///  4    5   6   ×   )
///  1    2   3   −   ⏎ (tall)
///  %    0   .   +   ⏎ (tall)
/// ```
///
/// Long-press ^ for sqrt(
/// Long-press ⌫ to clear line
/// Long-press $ to show variable picker
struct CalculatorKeyboard: View {
    let onKeyPress: (String) -> Void
    let onEnter: () -> Void
    let onBackspace: () -> Void
    let onClearLine: () -> Void
    let onDollarKey: () -> Void
    let onDollarLongPress: () -> Void
    let onHashKey: () -> Void

    private let topRows: [[KeyDef]] = [
        [
            KeyDef("^", .power),
            KeyDef("#", .specialHash),
            KeyDef("$", .specialDollar),
            KeyDef("=", .operator_),
            KeyDef("⌫", .backspace)
        ],
        [
            KeyDef("7", .digit),
            KeyDef("8", .digit),
            KeyDef("9", .digit),
            KeyDef("÷", .operator_),
            KeyDef("(", .operator_)
        ],
        [
            KeyDef("4", .digit),
            KeyDef("5", .digit),
            KeyDef("6", .digit),
            KeyDef("×", .operator_),
            KeyDef(")", .operator_)
        ]
    ]

    private let bottomRows: [[KeyDef]] = [
        [
            KeyDef("1", .digit),
            KeyDef("2", .digit),
            KeyDef("3", .digit),
            KeyDef("−", .operator_)
        ],
        [
            KeyDef("%", .operator_),
            KeyDef("0", .digit),
            KeyDef(".", .digit),
            KeyDef("+", .operator_)
        ]
    ]

    private let spacing: CGFloat = 4
    private let buttonHeight: CGFloat = 56
    private let columns = 5

    var body: some View {
        GeometryReader { geometry in
            let totalSpacing = spacing * CGFloat(columns - 1)
            let buttonWidth = (geometry.size.width - totalSpacing) / CGFloat(columns)

            VStack(spacing: spacing) {
                // Top 3 rows (5 buttons each)
                ForEach(0..<topRows.count, id: \.self) { rowIdx in
                    HStack(spacing: spacing) {
                        ForEach(topRows[rowIdx], id: \.label) { keyDef in
                            KeyButton(
                                keyDef: keyDef,
                                onTap: { handleTap(keyDef) },
                                onLongPress: longPressAction(for: keyDef),
                                width: buttonWidth,
                                height: buttonHeight
                            )
                        }
                    }
                }

                // Bottom 2 rows with tall Enter button
                HStack(spacing: spacing) {
                    // Left side: 4 buttons per row
                    VStack(spacing: spacing) {
                        ForEach(0..<bottomRows.count, id: \.self) { rowIdx in
                            HStack(spacing: spacing) {
                                ForEach(bottomRows[rowIdx], id: \.label) { keyDef in
                                    KeyButton(
                                        keyDef: keyDef,
                                        onTap: { onKeyPress(keyDef.label) },
                                        onLongPress: nil,
                                        width: buttonWidth,
                                        height: buttonHeight
                                    )
                                }
                            }
                        }
                    }

                    // Tall Enter button
                    KeyButton(
                        keyDef: KeyDef("⏎", .enter),
                        onTap: onEnter,
                        onLongPress: nil,
                        width: buttonWidth,
                        height: buttonHeight * 2 + spacing
                    )
                }
            }
        }
        .frame(height: buttonHeight * 5 + spacing * 4)
        .padding(4)
        #if canImport(UIKit)
        .background(Color(.systemGray5))
        #else
        .background(Color(NSColor.windowBackgroundColor))
        #endif
    }

    private func handleTap(_ keyDef: KeyDef) {
        switch keyDef.type {
        case .digit, .operator_:
            onKeyPress(keyDef.label)
        case .power:
            onKeyPress("^")
        case .backspace:
            onBackspace()
        case .enter:
            onEnter()
        case .specialDollar:
            onDollarKey()
        case .specialHash:
            onHashKey()
        }
    }

    private func longPressAction(for keyDef: KeyDef) -> (() -> Void)? {
        switch keyDef.type {
        case .backspace:
            return onClearLine
        case .specialDollar:
            return onDollarLongPress
        case .power:
            return { onKeyPress("sqrt(") }
        default:
            return nil
        }
    }
}

// MARK: - Key Types

private enum KeyType {
    case digit
    case operator_
    case power
    case backspace
    case enter
    case specialDollar
    case specialHash
}

private struct KeyDef {
    let label: String
    let type: KeyType

    init(_ label: String, _ type: KeyType) {
        self.label = label
        self.type = type
    }
}

// MARK: - Key Button

private struct KeyButton: View {
    let keyDef: KeyDef
    let onTap: () -> Void
    let onLongPress: (() -> Void)?
    var width: CGFloat
    var height: CGFloat

    @GestureState private var isLongPressing = false

    var body: some View {
        let displayLabel = isLongPressing && keyDef.type == .power ? "√" : keyDef.label

        Button {
            if onLongPress == nil {
                #if canImport(UIKit)
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                #endif
                onTap()
            }
        } label: {
            Text(displayLabel)
                .font(.title3)
                .fontWeight(.medium)
                .foregroundStyle(textColor)
                .frame(width: width, height: height)
                .background(backgroundColor)
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .simultaneousGesture(
            onLongPress != nil ?
            LongPressGesture(minimumDuration: 0.4)
                .updating($isLongPressing) { currentState, gestureState, _ in
                    gestureState = true
                }
                .onEnded { _ in
                    #if canImport(UIKit)
                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                    #endif
                    onLongPress?()
                }
            : nil
        )
        .highPriorityGesture(
            onLongPress != nil ?
            TapGesture()
                .onEnded {
                    #if canImport(UIKit)
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    #endif
                    onTap()
                }
            : nil
        )
        .accessibilityLabel(accessibilityLabel)
    }

    private var backgroundColor: Color {
        switch keyDef.type {
        case .digit:
            #if canImport(UIKit)
            return Color(.systemBackground)
            #else
            return Color(NSColor.controlBackgroundColor)
            #endif
        case .operator_, .power, .specialDollar, .specialHash:
            #if canImport(UIKit)
            return Color(.secondarySystemBackground)
            #else
            return Color(NSColor.controlColor)
            #endif
        case .backspace, .enter:
            return Color.red.opacity(0.15)
        }
    }

    private var textColor: Color {
        switch keyDef.type {
        case .digit:
            return .primary
        case .operator_, .power, .specialDollar, .specialHash:
            return .accentColor
        case .backspace, .enter:
            return .red
        }
    }

    private var accessibilityLabel: String {
        switch keyDef.label {
        case "+": return "Add"
        case "−": return "Subtract"
        case "×": return "Multiply"
        case "÷": return "Divide"
        case "^": return "Power, long press for square root"
        case "%": return "Percent"
        case "=": return "Equals"
        case "(": return "Open parenthesis"
        case ")": return "Close parenthesis"
        case ".": return "Decimal point"
        case "⌫": return "Backspace, long press to clear line"
        case "⏎": return "Enter"
        case "$": return "Dollar, long press for variable picker"
        case "#": return "Hash for comment"
        default: return keyDef.label
        }
    }
}
