// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Enumeration of all token types recognized by the lexer.
enum TokenType {
    // Literals
    case number         // 123, 45.67

    // Operators
    case plus           // +
    case minus          // - or −
    case multiply       // * or ×
    case divide         // / or ÷
    case percent        // %
    case power          // ^
    case equals         // =

    // Delimiters
    case lparen         // (
    case rparen         // )

    // Identifiers
    case variable       // $name (named variable)
    case lineRef        // $1, $2 (line reference)

    // Functions
    case sqrt           // sqrt

    // Special
    case eof            // End of input
    case error          // Invalid token
}

/// Represents a token produced by the lexer.
struct Token {
    let type: TokenType
    let value: String
    let position: Int
}
