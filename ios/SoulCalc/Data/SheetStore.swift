// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Persists sheet data (lines and focused index) to a JSON file.
/// Simple file-based storage as an alternative to Core Data / SwiftData.
class SheetStore {
    private let fileManager = FileManager.default

    private var storeURL: URL {
        let docs = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        return docs.appendingPathComponent("soulcalc_sheet.json")
    }

    struct SheetData: Codable {
        var lines: [String]
        var focusedLineIndex: Int
    }

    /// Loads saved sheet data, or returns nil if no data exists.
    func load() -> SheetData? {
        guard fileManager.fileExists(atPath: storeURL.path) else { return nil }
        do {
            let data = try Data(contentsOf: storeURL)
            return try JSONDecoder().decode(SheetData.self, from: data)
        } catch {
            return nil
        }
    }

    /// Saves sheet data to disk.
    func save(_ sheet: SheetData) {
        do {
            let data = try JSONEncoder().encode(sheet)
            try data.write(to: storeURL, options: .atomic)
        } catch {
            // Silently fail - non-critical
        }
    }

    /// Saves only the lines.
    func saveLines(_ lines: [String]) {
        let current = load() ?? SheetData(lines: [], focusedLineIndex: 0)
        save(SheetData(lines: lines, focusedLineIndex: current.focusedLineIndex))
    }

    /// Saves only the focused line index.
    func saveFocusedLineIndex(_ index: Int) {
        let current = load() ?? SheetData(lines: [], focusedLineIndex: 0)
        save(SheetData(lines: current.lines, focusedLineIndex: index))
    }
}
