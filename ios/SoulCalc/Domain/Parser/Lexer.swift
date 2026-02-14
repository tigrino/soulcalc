// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Tokenizes input strings into a sequence of tokens for the parser.
///
/// Handles:
/// - Numbers (integers and decimals)
/// - Operators (+, -, *, /, ^, %, = and Unicode variants ×, ÷, −)
/// - Parentheses
/// - Variables ($name)
/// - Line references ($1, $2, etc.)
/// - Functions (sqrt)
class Lexer {
    private let input: [Character]
    private var position: Int = 0

    init(_ input: String) {
        self.input = Array(input)
    }

    private var currentChar: Character? {
        position < input.count ? input[position] : nil
    }

    /// Tokenizes the entire input and returns a list of tokens.
    /// The list always ends with an EOF token.
    func tokenize() -> [Token] {
        var tokens: [Token] = []

        while currentChar != nil {
            let token = nextToken()
            tokens.append(token)
            if token.type == .error || token.type == .eof {
                break
            }
        }

        if tokens.isEmpty || tokens.last?.type != .eof {
            tokens.append(Token(type: .eof, value: "", position: position))
        }

        return tokens
    }

    private func nextToken() -> Token {
        skipWhitespace()

        guard let char = currentChar else {
            return Token(type: .eof, value: "", position: position)
        }

        let startPos = position

        if char.isNumber {
            return readNumber()
        }
        if char == "." {
            if let next = peekNext(), next.isNumber {
                return readNumber()
            }
        }

        switch char {
        case "$":
            return readDollarToken()
        case "+":
            return singleCharToken(.plus)
        case "-", "\u{2212}": // - or −
            return singleCharToken(.minus)
        case "*", "\u{00D7}": // * or ×
            return singleCharToken(.multiply)
        case "/", "\u{00F7}": // / or ÷
            return singleCharToken(.divide)
        case "%":
            return singleCharToken(.percent)
        case "^":
            return singleCharToken(.power)
        case "=":
            return singleCharToken(.equals)
        case "(":
            return singleCharToken(.lparen)
        case ")":
            return singleCharToken(.rparen)
        default:
            if char.isLetter {
                return readIdentifier()
            }
            position += 1
            return Token(type: .error, value: String(char), position: startPos)
        }
    }

    private func skipWhitespace() {
        while let char = currentChar, char.isWhitespace {
            position += 1
        }
    }

    private func peekNext() -> Character? {
        position + 1 < input.count ? input[position + 1] : nil
    }

    private func singleCharToken(_ type: TokenType) -> Token {
        let startPos = position
        let char = currentChar!
        position += 1
        return Token(type: type, value: String(char), position: startPos)
    }

    private func readNumber() -> Token {
        let startPos = position
        var result = ""
        var hasDecimal = false

        while let char = currentChar {
            if char.isNumber {
                result.append(char)
                position += 1
            } else if char == "." && !hasDecimal {
                hasDecimal = true
                result.append(char)
                position += 1
            } else {
                break
            }
        }

        return Token(type: .number, value: result, position: startPos)
    }

    private func readDollarToken() -> Token {
        let startPos = position
        position += 1 // skip '$'

        guard let char = currentChar else {
            return Token(type: .error, value: "$", position: startPos)
        }

        if char.isNumber {
            return readLineRef(startPos)
        } else if char.isLetter {
            return readVariable(startPos)
        } else {
            return Token(type: .error, value: "$", position: startPos)
        }
    }

    private func readLineRef(_ startPos: Int) -> Token {
        var result = ""
        while let char = currentChar, char.isNumber {
            result.append(char)
            position += 1
        }
        return Token(type: .lineRef, value: result, position: startPos)
    }

    private func readVariable(_ startPos: Int) -> Token {
        var result = ""
        while let char = currentChar, char.isLetter || char.isNumber || char == "_" {
            result.append(char)
            position += 1
        }
        return Token(type: .variable, value: result, position: startPos)
    }

    private func readIdentifier() -> Token {
        let startPos = position
        var result = ""

        while let char = currentChar, char.isLetter || char.isNumber || char == "_" {
            result.append(char)
            position += 1
        }

        switch result.lowercased() {
        case "sqrt":
            return Token(type: .sqrt, value: result, position: startPos)
        default:
            return Token(type: .error, value: result, position: startPos)
        }
    }
}
