// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import SwiftUI

/// Settings dialog for theme configuration.
struct SettingsView: View {
    @Binding var currentThemeMode: ThemeMode
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            themeList
                .navigationTitle("Settings")
                .inlineNavBarIfAvailable()
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") { dismiss() }
                    }
                }
        }
    }

    private var themeList: some View {
        List {
            Section("Theme") {
                ForEach(ThemeMode.allCases, id: \.self) { mode in
                    themeRow(mode)
                }
            }
        }
    }

    private func themeRow(_ mode: ThemeMode) -> some View {
        HStack {
            Text(mode.displayName)
            Spacer()
            if mode == currentThemeMode {
                Image(systemName: "checkmark")
                    .foregroundStyle(Color.accentColor)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            currentThemeMode = mode
        }
    }
}

private extension View {
    @ViewBuilder
    func inlineNavBarIfAvailable() -> some View {
        #if os(iOS)
        self.navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }
}
