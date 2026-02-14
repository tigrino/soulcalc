// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Types of lines that can be identified.
enum LineType {
    /// Line starts with # - a comment, not evaluated
    case comment

    /// Line is empty or contains only whitespace
    case empty

    /// Line contains an expression to be parsed and evaluated
    case expression
}

/// Classifies input lines before parsing.
enum LineClassifier {

    /// Classifies the given input line.
    static func classify(_ input: String) -> LineType {
        let trimmed = input.trimmingCharacters(in: .whitespaces)

        if trimmed.isEmpty {
            return .empty
        } else if trimmed.hasPrefix("#") {
            return .comment
        } else {
            return .expression
        }
    }

    /// Returns true if the line should be parsed and evaluated.
    static func shouldEvaluate(_ input: String) -> Bool {
        classify(input) == .expression
    }

    /// Returns true if the line is a comment.
    static func isComment(_ input: String) -> Bool {
        classify(input) == .comment
    }

    /// Returns true if the line is empty or whitespace-only.
    static func isEmpty(_ input: String) -> Bool {
        classify(input) == .empty
    }
}
