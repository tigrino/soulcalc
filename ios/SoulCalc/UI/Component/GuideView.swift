// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import SwiftUI

/// Guide dialog with usage instructions.
struct GuideView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    guideSection(
                        title: "Basics",
                        content: """
                        Type mathematical expressions and see results instantly.

                        Supported operations:
                        • Addition: 10 + 5
                        • Subtraction: 10 - 5
                        • Multiplication: 10 × 5
                        • Division: 10 ÷ 5
                        • Power: 2 ^ 8
                        • Square root: sqrt(16)
                        • Percentage: 10% or 100 + 10%
                        • Parentheses: (2 + 3) × 4
                        """
                    )

                    guideSection(
                        title: "Variables",
                        content: """
                        Define named variables with $name = value:
                        • $tax = 0.08
                        • $price = 100
                        • $price × (1 + $tax)

                        Variables defined on earlier lines are available on later lines.
                        """
                    )

                    guideSection(
                        title: "Line References",
                        content: """
                        Reference results from other lines with $N:
                        • $1 refers to the result of line 1
                        • $2 refers to the result of line 2

                        Line references update automatically when lines are added or removed.
                        """
                    )

                    guideSection(
                        title: "Keyboard",
                        content: """
                        The custom keyboard provides quick access to all operations.

                        Long-press shortcuts:
                        • Long-press ^ for sqrt(
                        • Long-press ⌫ to clear the entire line
                        • Long-press $ to pick from defined variables

                        Toggle between the custom and system keyboards using the keyboard icon in the toolbar.
                        """
                    )
                }
                .padding()
            }
            .navigationTitle("Guide")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("OK") { dismiss() }
                }
            }
        }
    }

    @ViewBuilder
    private func guideSection(title: String, content: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.headline)
            Text(content)
                .font(.body)
                .foregroundStyle(.secondary)
        }
    }
}

