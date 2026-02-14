// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import SwiftUI

/// Dialog for selecting a variable to insert.
struct VariablePickerView: View {
    let variables: [String]
    let onVariableSelected: (String) -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            Group {
                if variables.isEmpty {
                    VStack {
                        Spacer()
                        Text("No variables defined yet")
                            .foregroundStyle(.secondary)
                        Text("Define a variable with $name = value")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                        Spacer()
                    }
                } else {
                    ScrollView {
                        LazyVGrid(
                            columns: [GridItem(.adaptive(minimum: 100), spacing: 8)],
                            spacing: 8
                        ) {
                            ForEach(variables, id: \.self) { variable in
                                Button {
                                    onVariableSelected(variable)
                                } label: {
                                    Text("$\(variable)")
                                        .font(.body)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .frame(maxWidth: .infinity)
                                        .background(Color.accentColor.opacity(0.15))
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Insert Variable")
#if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
#endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                }
            }
        }
    }
}
