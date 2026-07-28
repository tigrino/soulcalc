// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import XCTest
@testable import SoulCalcDomain

final class ParserTests: XCTestCase {

    // MARK: - Basic Parsing

    func testNumber() {
        let result = parseExpression("42")
        assertSuccess(result) { node in
            if case .number(let value) = node {
                XCTAssertEqual(value, 42.0)
            } else {
                XCTFail("Expected NumberNode")
            }
        }
    }

    func testAddition() {
        let result = parseExpression("1 + 2")
        assertSuccess(result) { node in
            if case .binaryOp(let left, let op, let right) = node {
                XCTAssertEqual(op, .add)
                assertNumber(left, 1.0)
                assertNumber(right, 2.0)
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    func testSubtraction() {
        let result = parseExpression("5 - 3")
        assertSuccess(result) { node in
            if case .binaryOp(_, let op, _) = node {
                XCTAssertEqual(op, .subtract)
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    func testMultiplication() {
        let result = parseExpression("3 × 4")
        assertSuccess(result) { node in
            if case .binaryOp(_, let op, _) = node {
                XCTAssertEqual(op, .multiply)
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    func testDivision() {
        let result = parseExpression("10 ÷ 2")
        assertSuccess(result) { node in
            if case .binaryOp(_, let op, _) = node {
                XCTAssertEqual(op, .divide)
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    // MARK: - Operator Precedence

    func testMultiplicationBeforeAddition() {
        // 1 + 2 * 3 = 1 + (2 * 3)
        let result = parseExpression("1 + 2 * 3")
        assertSuccess(result) { node in
            if case .binaryOp(_, let op, _) = node {
                XCTAssertEqual(op, .add) // Top-level should be addition
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    func testParenthesesOverridePrecedence() {
        // (1 + 2) * 3
        let result = parseExpression("(1 + 2) * 3")
        assertSuccess(result) { node in
            if case .binaryOp(_, let op, _) = node {
                XCTAssertEqual(op, .multiply) // Top-level should be multiplication
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    // MARK: - Power

    func testPower() {
        let result = parseExpression("2 ^ 3")
        assertSuccess(result) { node in
            if case .binaryOp(_, let op, _) = node {
                XCTAssertEqual(op, .power)
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    func testPowerRightAssociative() {
        // 2^3^4 = 2^(3^4) - right side should be power node
        let result = parseExpression("2 ^ 3 ^ 4")
        assertSuccess(result) { node in
            if case .binaryOp(_, .power, let right) = node {
                if case .binaryOp(_, .power, _) = right {
                    // Correct - right-associative
                } else {
                    XCTFail("Expected right-associative power")
                }
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    // MARK: - Unary Minus

    func testUnaryMinus() {
        let result = parseExpression("-5")
        assertSuccess(result) { node in
            if case .unaryMinus(let operand) = node {
                assertNumber(operand, 5.0)
            } else {
                XCTFail("Expected UnaryMinusNode")
            }
        }
    }

    func testDoubleNegation() {
        let result = parseExpression("--5")
        assertSuccess(result) { node in
            if case .unaryMinus(let inner) = node {
                if case .unaryMinus(_) = inner {
                    // Correct: --5
                } else {
                    XCTFail("Expected nested unary minus")
                }
            } else {
                XCTFail("Expected UnaryMinusNode")
            }
        }
    }

    // MARK: - Percentage

    func testStandalonePercent() {
        let result = parseExpression("10%")
        assertSuccess(result) { node in
            if case .percent(_, let base) = node {
                XCTAssertNil(base) // standalone
            } else {
                XCTFail("Expected PercentNode")
            }
        }
    }

    func testContextualPercent() {
        // 100 + 10% should parse with 100 as base for the percent
        let result = parseExpression("100 + 10%")
        assertSuccess(result) { node in
            if case .binaryOp(_, .add, let right) = node {
                if case .percent(_, let base) = right {
                    XCTAssertNotNil(base) // contextual
                } else {
                    XCTFail("Expected PercentNode on right side")
                }
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    // MARK: - Variables

    func testVariableReference() {
        let result = parseExpression("$tax")
        assertSuccess(result) { node in
            if case .variable(let name) = node {
                XCTAssertEqual(name, "tax")
            } else {
                XCTFail("Expected VariableNode")
            }
        }
    }

    func testAssignment() {
        let result = parseExpression("$tax = 0.08")
        assertSuccess(result) { node in
            if case .assignment(let name, let expr) = node {
                XCTAssertEqual(name, "tax")
                assertNumber(expr, 0.08)
            } else {
                XCTFail("Expected AssignmentNode")
            }
        }
    }

    // MARK: - Line References

    func testLineReference() {
        let result = parseExpression("$1")
        assertSuccess(result) { node in
            if case .lineRef(let num) = node {
                XCTAssertEqual(num, 1)
            } else {
                XCTFail("Expected LineRefNode")
            }
        }
    }

    // MARK: - Functions

    func testSqrt() {
        let result = parseExpression("sqrt(16)")
        assertSuccess(result) { node in
            if case .function(let name, let arg) = node {
                XCTAssertEqual(name, "sqrt")
                assertNumber(arg, 16.0)
            } else {
                XCTFail("Expected FunctionNode")
            }
        }
    }

    // MARK: - Empty and Errors

    func testEmptyInput() {
        let result = parseExpression("")
        if case .empty = result { } else {
            XCTFail("Expected empty result")
        }
    }

    func testUnexpectedToken() {
        let result = parseExpression("1 + + 2")
        if case .error = result { } else {
            XCTFail("Expected parse error")
        }
    }

    func testMissingClosingParen() {
        let result = parseExpression("(1 + 2")
        if case .error = result { } else {
            XCTFail("Expected parse error for missing )")
        }
    }

    func testSqrtMissingParen() {
        let result = parseExpression("sqrt 16")
        if case .error = result { } else {
            XCTFail("Expected parse error for sqrt without parens")
        }
    }

    // MARK: - Alternate Operator Symbols

    func testMultiplicationWithAsterisk() {
        let result = parseExpression("3 * 4")
        assertSuccess(result) { node in
            if case .binaryOp(let left, let op, let right) = node {
                XCTAssertEqual(op, .multiply)
                assertNumber(left, 3.0)
                assertNumber(right, 4.0)
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    func testDivisionWithSlash() {
        let result = parseExpression("10 / 2")
        assertSuccess(result) { node in
            if case .binaryOp(let left, let op, let right) = node {
                XCTAssertEqual(op, .divide)
                assertNumber(left, 10.0)
                assertNumber(right, 2.0)
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    // MARK: - Contextual Percentage Variants

    func testContextualPercentSubtract() {
        // 200 - 25% should parse with 200 as base for the percent
        let result = parseExpression("200 - 25%")
        assertSuccess(result) { node in
            if case .binaryOp(_, .subtract, let right) = node {
                if case .percent(_, let base) = right {
                    XCTAssertNotNil(base) // contextual
                } else {
                    XCTFail("Expected PercentNode on right side")
                }
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    func testPercentInMultiplication() {
        // 100 * 10% should have nil base (standalone percent, no contextual in multiply)
        let result = parseExpression("100 * 10%")
        assertSuccess(result) { node in
            if case .binaryOp(_, .multiply, let right) = node {
                if case .percent(_, let base) = right {
                    XCTAssertNil(base) // standalone in multiply context
                } else {
                    XCTFail("Expected PercentNode on right side")
                }
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    // MARK: - Chained Expressions

    func testChainedAddition() {
        // 1 + 2 + 3 -> top level is add, left is add
        let result = parseExpression("1 + 2 + 3")
        assertSuccess(result) { node in
            if case .binaryOp(let left, let op, let right) = node {
                XCTAssertEqual(op, .add)
                assertNumber(right, 3.0)
                if case .binaryOp(_, let innerOp, _) = left {
                    XCTAssertEqual(innerOp, .add)
                } else {
                    XCTFail("Expected inner BinaryOpNode (add)")
                }
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    // MARK: - Nested Functions

    func testNestedSqrt() {
        // sqrt(sqrt(16)) parses as function(sqrt, function(sqrt, 16))
        let result = parseExpression("sqrt(sqrt(16))")
        assertSuccess(result) { node in
            if case .function(let name, let arg) = node {
                XCTAssertEqual(name, "sqrt")
                if case .function(let innerName, let innerArg) = arg {
                    XCTAssertEqual(innerName, "sqrt")
                    assertNumber(innerArg, 16.0)
                } else {
                    XCTFail("Expected inner FunctionNode")
                }
            } else {
                XCTFail("Expected FunctionNode")
            }
        }
    }

    // MARK: - Assignment Variants

    func testAssignmentWithExpression() {
        // $x = 2 + 3 -> assignment with binary op expression
        let result = parseExpression("$x = 2 + 3")
        assertSuccess(result) { node in
            if case .assignment(let name, let expr) = node {
                XCTAssertEqual(name, "x")
                if case .binaryOp(let left, let op, let right) = expr {
                    XCTAssertEqual(op, .add)
                    assertNumber(left, 2.0)
                    assertNumber(right, 3.0)
                } else {
                    XCTFail("Expected BinaryOpNode in assignment expression")
                }
            } else {
                XCTFail("Expected AssignmentNode")
            }
        }
    }

    // MARK: - Multi-Digit Line References

    func testMultiDigitLineRef() {
        let result = parseExpression("$42")
        assertSuccess(result) { node in
            if case .lineRef(let num) = node {
                XCTAssertEqual(num, 42)
            } else {
                XCTFail("Expected LineRefNode")
            }
        }
    }

    // MARK: - Unary Minus in Expressions

    func testUnaryMinusInExpression() {
        // 3 + -2 -> top-level is add, right is unaryMinus
        let result = parseExpression("3 + -2")
        assertSuccess(result) { node in
            if case .binaryOp(let left, let op, let right) = node {
                XCTAssertEqual(op, .add)
                assertNumber(left, 3.0)
                if case .unaryMinus(let operand) = right {
                    assertNumber(operand, 2.0)
                } else {
                    XCTFail("Expected UnaryMinusNode on right side")
                }
            } else {
                XCTFail("Expected BinaryOpNode")
            }
        }
    }

    // MARK: - Additional Error Cases

    func testInvalidToken() {
        let result = parseExpression("@")
        if case .error = result { } else {
            XCTFail("Expected parse error for invalid token")
        }
    }

    func testExtraTokenAfterExpression() {
        // "1 2" should produce error (unexpected token after complete expression)
        let result = parseExpression("1 2")
        if case .error = result { } else {
            XCTFail("Expected parse error for extra token after expression")
        }
    }

    // MARK: - Precision Limits

    func testLiteralBeyondDoublePrecisionIsRejected() {
        // 10000000000000000.1 collapses to 10000000000000000 as a Double, which
        // made "10000000000000000.1 - 10000000000000000" evaluate to 0.
        let result = parseExpression("10000000000000000.1 - 10000000000000000")
        assertPrecisionError(result)
    }

    func testSixteenSignificantDigitsIsRejected() {
        assertPrecisionError(parseExpression("1234567890123456"))
    }

    func testFifteenSignificantDigitsIsAccepted() {
        assertSuccess(parseExpression("123456789012345")) { node in
            self.assertNumber(node, 123456789012345.0)
        }
    }

    func testTrailingZerosDoNotCountAsSignificant() {
        // 1e18 carries one significant digit however long the literal looks.
        assertSuccess(parseExpression("1000000000000000000")) { node in
            self.assertNumber(node, 1e18)
        }
    }

    func testLeadingZerosDoNotCountAsSignificant() {
        assertSuccess(parseExpression("0.000000000000000001")) { node in
            self.assertNumber(node, 1e-18)
        }
    }

    func testOrdinaryDecimalsAreUnaffected() {
        assertSuccess(parseExpression("0.1")) { node in self.assertNumber(node, 0.1) }
        assertSuccess(parseExpression("3.14")) { node in self.assertNumber(node, 3.14) }
        assertSuccess(parseExpression("999999999999")) { node in
            self.assertNumber(node, 999999999999.0)
        }
    }

    func testSignificantDigitCounting() {
        XCTAssertEqual(Parser.significantDigits("0.00025"), 2)
        XCTAssertEqual(Parser.significantDigits("250000"), 2)
        XCTAssertEqual(Parser.significantDigits("10000000000000000.1"), 18)
        XCTAssertEqual(Parser.significantDigits("0"), 0)
    }

    // MARK: - Helpers

    private func assertPrecisionError(_ result: ParseResult) {
        if case .error(let message, _) = result {
            XCTAssertTrue(
                message.contains("Too many significant digits"),
                "Expected a precision error, got \(message)"
            )
        } else {
            XCTFail("Expected a precision error, got \(result)")
        }
    }

    private func assertSuccess(_ result: ParseResult, _ check: (AstNode) -> Void) {
        if case .success(let node) = result {
            check(node)
        } else {
            XCTFail("Expected success, got \(result)")
        }
    }

    private func assertNumber(_ node: AstNode, _ expected: Double) {
        if case .number(let value) = node {
            XCTAssertEqual(value, expected, accuracy: 1e-10)
        } else {
            XCTFail("Expected NumberNode(\(expected)), got \(node)")
        }
    }
}
