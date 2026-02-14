// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Represents a single line in the calculator sheet.
struct Line {
    let id: Int
    let position: Int
    let input: String
    let result: Result

    init(id: Int, position: Int, input: String, result: Result = .empty) {
        self.id = id
        self.position = position
        self.input = input
        self.result = result
    }
}
