// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Result of parsing: either a valid AST, an error message, or empty.
enum ParseResult {
    case success(AstNode)
    case error(String, Int)
    case empty
}

/// Recursive descent parser for calculator expressions.
///
/// Grammar:
/// ```
/// expression    → assignment | computation
/// assignment    → VARIABLE "=" computation
/// computation   → term (("+"|"-") term)*
/// term          → factor (("×"|"*"|"÷"|"/") factor)*
/// factor        → power ("%")?
/// power         → unary ("^" unary)*
/// unary         → ("-")? primary
/// primary       → NUMBER | VARIABLE | LINE_REF | "(" computation ")" | function
/// function      → "sqrt" "(" computation ")"
/// ```
class Parser {
    private let tokens: [Token]
    private var position: Int = 0

    init(_ tokens: [Token]) {
        self.tokens = tokens
    }

    private var currentToken: Token? {
        position < tokens.count ? tokens[position] : nil
    }

    private var currentType: TokenType? {
        currentToken?.type
    }

    /// Parses the token stream and returns a ParseResult.
    func parse() -> ParseResult {
        if tokens.isEmpty || (tokens.count == 1 && tokens[0].type == .eof) {
            return .empty
        }

        do {
            let node = try parseExpression()
            if currentType != .eof {
                return .error("Unexpected token: \(currentToken?.value ?? "")", currentToken?.position ?? 0)
            }
            return .success(node)
        } catch let error as ParseError {
            return .error(error.message, error.position)
        } catch {
            return .error("Parse error", 0)
        }
    }

    private func parseExpression() throws -> AstNode {
        // Check for assignment: VARIABLE "=" computation
        if currentType == .variable {
            let varToken = currentToken!
            let nextPos = position + 1
            if nextPos < tokens.count && tokens[nextPos].type == .equals {
                position += 1 // consume variable
                position += 1 // consume equals
                let expr = try parseComputation()
                return .assignment(variableName: varToken.value, expression: expr)
            }
        }
        return try parseComputation()
    }

    private func parseComputation() throws -> AstNode {
        var left = try parseTerm()

        while currentType == .plus || currentType == .minus {
            let opType = currentType!
            position += 1 // consume operator

            let right = try parseTerm()

            // Handle percentage context: if right side ends with %, it uses left as base
            let adjustedRight = applyPercentageContext(right, base: left)

            let op: BinaryOp = opType == .plus ? .add : .subtract
            left = .binaryOp(left: left, op: op, right: adjustedRight)
        }

        return left
    }

    private func parseTerm() throws -> AstNode {
        var left = try parseFactor()

        while currentType == .multiply || currentType == .divide {
            let op: BinaryOp = currentType == .multiply ? .multiply : .divide
            position += 1 // consume operator
            let right = try parseFactor()
            left = .binaryOp(left: left, op: op, right: right)
        }

        return left
    }

    private func parseFactor() throws -> AstNode {
        let base = try parsePower()

        // Check for postfix %
        if currentType == .percent {
            position += 1 // consume %
            // Standalone percentage (context will be applied by parseComputation if needed)
            return .percent(operand: base, base: nil)
        }

        return base
    }

    private func parsePower() throws -> AstNode {
        let base = try parseUnary()

        // Power is right-associative: 2^3^4 = 2^(3^4)
        if currentType == .power {
            position += 1 // consume ^
            let exponent = try parsePower() // right-recursive for right associativity
            return .binaryOp(left: base, op: .power, right: exponent)
        }

        return base
    }

    private func parseUnary() throws -> AstNode {
        if currentType == .minus {
            position += 1 // consume -
            let operand = try parseUnary()
            return .unaryMinus(operand)
        }
        return try parsePrimary()
    }

    private func parsePrimary() throws -> AstNode {
        guard let token = currentToken else {
            throw ParseError("Unexpected end of input", position)
        }

        switch token.type {
        case .number:
            position += 1
            if Parser.significantDigits(token.value) > Parser.maxSignificantDigits {
                throw ParseError("Too many significant digits: \(token.value)", token.position)
            }
            guard let value = Double(token.value) else {
                throw ParseError("Invalid number: \(token.value)", token.position)
            }
            return .number(value)

        case .variable:
            position += 1
            return .variable(token.value)

        case .lineRef:
            position += 1
            guard let lineNum = Int(token.value) else {
                throw ParseError("Invalid line reference: $\(token.value)", token.position)
            }
            return .lineRef(lineNum)

        case .lparen:
            position += 1 // consume (
            let expr = try parseComputation()
            if currentType != .rparen {
                throw ParseError("Expected ')'", currentToken?.position ?? position)
            }
            position += 1 // consume )
            return expr

        case .sqrt:
            position += 1 // consume sqrt
            if currentType != .lparen {
                throw ParseError("Expected '(' after sqrt", currentToken?.position ?? position)
            }
            position += 1 // consume (
            let arg = try parseComputation()
            if currentType != .rparen {
                throw ParseError("Expected ')'", currentToken?.position ?? position)
            }
            position += 1 // consume )
            return .function(name: "sqrt", argument: arg)

        case .eof:
            throw ParseError("Unexpected end of input", token.position)

        case .error:
            throw ParseError("Invalid token: \(token.value)", token.position)

        default:
            throw ParseError("Unexpected token: \(token.value)", token.position)
        }
    }

    /// The most significant decimal digits a Double can carry unambiguously.
    ///
    /// A decimal of this length or shorter survives conversion to a Double and
    /// back unchanged. Beyond it the surplus digits are silently discarded --
    /// `10000000000000000.1` becomes exactly `10000000000000000`, so subtracting
    /// the two yields 0 rather than 0.1. Such a literal is rejected instead of
    /// being quietly altered.
    static let maxSignificantDigits = 15

    /// Counts the significant digits in a numeric literal.
    ///
    /// Leading zeros only locate the decimal point and trailing zeros only scale
    /// the value, so neither adds precision and both are excluded. "0.00025" and
    /// "250000" each count as two; "10000000000000000.1" counts as eighteen.
    static func significantDigits(_ literal: String) -> Int {
        let digits = Array(literal.filter { $0.isASCII && $0.isNumber })
        guard let first = digits.firstIndex(where: { $0 != "0" }),
              let last = digits.lastIndex(where: { $0 != "0" }) else {
            return 0
        }
        return last - first + 1
    }

    /// Applies percentage context to a node.
    /// If the node is a PercentNode without a base, applies the given base.
    private func applyPercentageContext(_ node: AstNode, base: AstNode) -> AstNode {
        if case .percent(let operand, nil) = node {
            return .percent(operand: operand, base: base)
        }
        return node
    }
}

/// Error thrown during parsing.
private struct ParseError: Error {
    let message: String
    let position: Int

    init(_ message: String, _ position: Int) {
        self.message = message
        self.position = position
    }
}

/// Convenience function to parse an input string.
func parseExpression(_ input: String) -> ParseResult {
    let tokens = Lexer(input).tokenize()
    return Parser(tokens).parse()
}
