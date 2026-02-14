// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import SwiftUI

@main
struct SoulCalcApp: App {
    @StateObject private var themePreferences = ThemePreferences()
    @StateObject private var viewModel = MainViewModel()

    var body: some Scene {
        WindowGroup {
            MainScreen(
                viewModel: viewModel,
                themeMode: $themePreferences.themeMode
            )
            .preferredColorScheme(themePreferences.themeMode.colorScheme)
        }
    }
}
