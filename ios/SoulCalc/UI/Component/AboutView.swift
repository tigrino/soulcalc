// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import SwiftUI

/// About dialog showing app info.
struct AboutView: View {
    @Environment(\.dismiss) private var dismiss

    private let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    private let githubURL = URL(string: "https://github.com/tigrino/soulcalc")!

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: "function")
                    .font(.system(size: 60))
                    .foregroundStyle(Color.accentColor)

                Text("SoulCalc")
                    .font(.title)
                    .fontWeight(.bold)

                VStack(spacing: 8) {
                    Text("Version \(version)")
                        .foregroundStyle(.secondary)

                    Text("Developed by Albert Zenkoff")
                        .foregroundStyle(.secondary)

                    Text("MIT License")
                        .foregroundStyle(.secondary)
                }

                Link("Source Code on GitHub", destination: githubURL)

                Spacer()
            }
            .padding(.top, 40)
            .navigationTitle("About")
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
}

