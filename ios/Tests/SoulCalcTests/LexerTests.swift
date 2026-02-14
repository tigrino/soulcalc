// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import XCTest
@testable import SoulCalcDomain

final class LexerTests: XCTestCase {

    // MARK: - Numbers

    func testInteger() {
        let tokens = Lexer("42").tokenize()
        XCTAssertEqual(tokens.count, 2)
        XCTAssertEqual(tokens[0].type, .number)
        XCTAssertEqual(tokens[0].value, "42")
        XCTAssertEqual(tokens[1].type, .eof)
    }

    func testDecimalNumber() {
        let tokens = Lexer("3.14").tokenize()
        XCTAssertEqual(tokens[0].type, .number)
        XCTAssertEqual(tokens[0].value, "3.14")
    }

    func testLeadingDecimalPoint() {
        let tokens = Lexer(".5").tokenize()
        XCTAssertEqual(tokens[0].type, .number)
        XCTAssertEqual(tokens[0].value, ".5")
    }

    func testLargeNumber() {
        let tokens = Lexer("123456789.123456789").tokenize()
        XCTAssertEqual(tokens[0].type, .number)
        XCTAssertEqual(tokens[0].value, "123456789.123456789")
    }

    // MARK: - Operators

    func testBasicOperators() {
        let tokens = Lexer("+ - * / % ^ =").tokenize()
        let types = tokens.map { $0.type }
        XCTAssertEqual(types, [.plus, .minus, .multiply, .divide, .percent, .power, .equals, .eof])
    }

    func testUnicodeOperators() {
        let tokens = Lexer("× ÷ −").tokenize()
        let types = tokens.map { $0.type }
        XCTAssertEqual(types, [.multiply, .divide, .minus, .eof])
    }

    func testParentheses() {
        let tokens = Lexer("(1 + 2)").tokenize()
        let types = tokens.map { $0.type }
        XCTAssertEqual(types, [.lparen, .number, .plus, .number, .rparen, .eof])
    }

    // MARK: - Variables

    func testVariable() {
        let tokens = Lexer("$tax").tokenize()
        XCTAssertEqual(tokens[0].type, .variable)
        XCTAssertEqual(tokens[0].value, "tax")
    }

    func testVariableWithNumbers() {
        let tokens = Lexer("$var123").tokenize()
        XCTAssertEqual(tokens[0].type, .variable)
        XCTAssertEqual(tokens[0].value, "var123")
    }

    func testVariableWithUnderscore() {
        let tokens = Lexer("$my_var").tokenize()
        XCTAssertEqual(tokens[0].type, .variable)
        XCTAssertEqual(tokens[0].value, "my_var")
    }

    // MARK: - Line References

    func testLineReference() {
        let tokens = Lexer("$1").tokenize()
        XCTAssertEqual(tokens[0].type, .lineRef)
        XCTAssertEqual(tokens[0].value, "1")
    }

    func testMultiDigitLineReference() {
        let tokens = Lexer("$42").tokenize()
        XCTAssertEqual(tokens[0].type, .lineRef)
        XCTAssertEqual(tokens[0].value, "42")
    }

    // MARK: - Functions

    func testSqrt() {
        let tokens = Lexer("sqrt").tokenize()
        XCTAssertEqual(tokens[0].type, .sqrt)
        XCTAssertEqual(tokens[0].value, "sqrt")
    }

    func testSqrtCaseInsensitive() {
        let tokens = Lexer("SQRT").tokenize()
        XCTAssertEqual(tokens[0].type, .sqrt)
    }

    // MARK: - Complex Expressions

    func testFullExpression() {
        let tokens = Lexer("$tax = 0.08").tokenize()
        let types = tokens.map { $0.type }
        XCTAssertEqual(types, [.variable, .equals, .number, .eof])
        XCTAssertEqual(tokens[0].value, "tax")
        XCTAssertEqual(tokens[2].value, "0.08")
    }

    func testExpressionWithLineRef() {
        let tokens = Lexer("$1 + $2 * 3").tokenize()
        let types = tokens.map { $0.type }
        XCTAssertEqual(types, [.lineRef, .plus, .lineRef, .multiply, .number, .eof])
    }

    func testSqrtExpression() {
        let tokens = Lexer("sqrt(16)").tokenize()
        let types = tokens.map { $0.type }
        XCTAssertEqual(types, [.sqrt, .lparen, .number, .rparen, .eof])
    }

    // MARK: - Whitespace

    func testWhitespaceHandling() {
        let tokens = Lexer("  1  +  2  ").tokenize()
        let types = tokens.map { $0.type }
        XCTAssertEqual(types, [.number, .plus, .number, .eof])
    }

    func testEmptyInput() {
        let tokens = Lexer("").tokenize()
        XCTAssertEqual(tokens.count, 1)
        XCTAssertEqual(tokens[0].type, .eof)
    }

    // MARK: - Errors

    func testInvalidCharacter() {
        let tokens = Lexer("@").tokenize()
        XCTAssertEqual(tokens[0].type, .error)
    }

    func testDollarAlone() {
        let tokens = Lexer("$").tokenize()
        XCTAssertEqual(tokens[0].type, .error)
    }

    func testUnknownIdentifier() {
        let tokens = Lexer("foo").tokenize()
        XCTAssertEqual(tokens[0].type, .error)
        XCTAssertEqual(tokens[0].value, "foo")
    }

    // MARK: - Token Positions

    func testPositions() {
        let tokens = Lexer("1 + 2").tokenize()
        XCTAssertEqual(tokens[0].position, 0)
        XCTAssertEqual(tokens[1].position, 2)
        XCTAssertEqual(tokens[2].position, 4)
    }
}
