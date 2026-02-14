// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Represents the result of evaluating a single line.
enum Result {
    /// Successfully evaluated to a numeric value.
    case success(Double)

    /// Evaluation failed with an error.
    case error(String)

    /// Line is empty or a comment - no result to display.
    case empty
}
