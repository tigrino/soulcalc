// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import XCTest
@testable import SoulCalcDomain

final class EvaluatorTests: XCTestCase {

    // MARK: - Basic Arithmetic

    func testAddition() {
        assertEval("1 + 2", equals: 3.0)
    }

    func testSubtraction() {
        assertEval("10 - 3", equals: 7.0)
    }

    func testMultiplication() {
        assertEval("4 * 5", equals: 20.0)
    }

    func testDivision() {
        assertEval("10 / 2", equals: 5.0)
    }

    func testDecimalArithmetic() {
        assertEval("0.1 + 0.2", equals: 0.3, accuracy: 1e-10)
    }

    // MARK: - Operator Precedence

    func testPrecedence() {
        assertEval("2 + 3 * 4", equals: 14.0)
    }

    func testParentheses() {
        assertEval("(2 + 3) * 4", equals: 20.0)
    }

    func testNestedParentheses() {
        assertEval("((1 + 2) * (3 + 4))", equals: 21.0)
    }

    // MARK: - Power

    func testPower() {
        assertEval("2 ^ 8", equals: 256.0)
    }

    func testPowerRightAssociative() {
        assertEval("2 ^ 3 ^ 2", equals: 512.0) // 2^(3^2) = 2^9 = 512
    }

    func testPowerOfZero() {
        assertEval("5 ^ 0", equals: 1.0)
    }

    // MARK: - Unary Minus

    func testUnaryMinus() {
        assertEval("-5", equals: -5.0)
    }

    func testDoubleNegation() {
        assertEval("--5", equals: 5.0)
    }

    func testUnaryInExpression() {
        assertEval("3 + -2", equals: 1.0)
    }

    // MARK: - Percentage

    func testStandalonePercent() {
        assertEval("10%", equals: 0.1)
    }

    func testContextualPercentAdd() {
        // 100 + 10% = 100 + (100 * 10/100) = 100 + 10 = 110
        assertEval("100 + 10%", equals: 110.0)
    }

    func testContextualPercentSubtract() {
        // 200 - 25% = 200 - (200 * 25/100) = 200 - 50 = 150
        assertEval("200 - 25%", equals: 150.0)
    }

    func testPercentOfPercent() {
        assertEval("50%", equals: 0.5)
    }

    // MARK: - Sqrt

    func testSqrt() {
        assertEval("sqrt(16)", equals: 4.0)
    }

    func testSqrtDecimal() {
        assertEval("sqrt(2)", equals: 1.4142135623730951, accuracy: 1e-10)
    }

    func testSqrtNegative() {
        assertError("sqrt(-1)", contains: "NaN")
    }

    func testSqrtZero() {
        assertEval("sqrt(0)", equals: 0.0)
    }

    // MARK: - Division by Zero

    func testDivisionByZero() {
        assertError("1 / 0", contains: "∞")
    }

    func testNegativeDivisionByZero() {
        assertError("-1 / 0", contains: "-∞")
    }

    func testZeroDividedByZero() {
        assertError("0 / 0", contains: "NaN")
    }

    // MARK: - Variables

    func testVariableResolution() {
        let scope = Scope(variables: ["tax": 0.08])
        let result = evaluateExpression("$tax", scope: scope)
        assertResult(result.result, equals: 0.08)
    }

    func testUndefinedVariable() {
        let result = evaluateExpression("$unknown")
        assertResultError(result.result)
    }

    func testAssignment() {
        let result = evaluateExpression("$x = 42")
        assertResult(result.result, equals: 42.0)
        XCTAssertEqual(result.newScope.resolveVariable("x"), 42.0)
    }

    func testAssignmentExpressionValue() {
        let result = evaluateExpression("$price = 10 + 5")
        assertResult(result.result, equals: 15.0)
        XCTAssertEqual(result.newScope.resolveVariable("price"), 15.0)
    }

    // MARK: - Line References

    func testLineRefResolution() {
        let scope = Scope(lineResults: [1: 42.0])
        let result = evaluateExpression("$1", scope: scope)
        assertResult(result.result, equals: 42.0)
    }

    func testUndefinedLineRef() {
        let result = evaluateExpression("$99")
        assertResultError(result.result)
    }

    func testLineRefInExpression() {
        let scope = Scope(lineResults: [1: 10.0, 2: 20.0])
        let result = evaluateExpression("$1 + $2", scope: scope)
        assertResult(result.result, equals: 30.0)
    }

    // MARK: - Complex Real-World Expressions

    func testTaxCalculation() {
        let scope = Scope(variables: ["tax": 0.08])
        let result = evaluateExpression("100 * (1 + $tax)", scope: scope)
        assertResult(result.result, equals: 108.0)
    }

    func testCompoundExpression() {
        assertEval("(2 + 3) * 4 - 1", equals: 19.0)
    }

    func testComplexNested() {
        assertEval("sqrt(4) + 2 ^ 3", equals: 10.0) // 2 + 8
    }

    // MARK: - Empty Input

    func testEmptyInput() {
        let result = evaluateExpression("")
        if case .empty = result.result { } else {
            XCTFail("Expected empty result")
        }
    }

    // MARK: - Percent with Multiply/Divide

    func testMultiplyByPercent() {
        assertEval("100 * 10%", equals: 10.0)
    }

    func testDivideByPercent() {
        assertEval("100 / 10%", equals: 1000.0)
    }

    func testChainedPercentAdd() {
        assertEval("100 + 10% + 5%", equals: 115.5)
    }

    // MARK: - Sqrt Edge Cases

    func testNestedSqrt() {
        assertEval("sqrt(sqrt(16))", equals: 2.0)
    }

    func testSqrtWithExpression() {
        assertEval("sqrt(3^2 + 4^2)", equals: 5.0)
    }

    // MARK: - Decimal Percent

    func testDecimalPercent() {
        assertEval("100 + 7.5%", equals: 107.5)
    }

    // MARK: - Helpers

    private func assertEval(_ input: String, equals expected: Double, accuracy: Double = 1e-10) {
        let result = evaluateExpression(input)
        assertResult(result.result, equals: expected, accuracy: accuracy)
    }

    private func assertError(_ input: String, contains expectedText: String) {
        let result = evaluateExpression(input)
        if case .error(let message) = result.result {
            XCTAssertTrue(message.contains(expectedText),
                          "Error '\(message)' doesn't contain '\(expectedText)'")
        } else {
            XCTFail("Expected error containing '\(expectedText)', got \(result.result)")
        }
    }

    private func assertResult(_ result: Result, equals expected: Double, accuracy: Double = 1e-10) {
        if case .success(let value) = result {
            XCTAssertEqual(value, expected, accuracy: accuracy)
        } else {
            XCTFail("Expected success(\(expected)), got \(result)")
        }
    }

    private func assertResultError(_ result: Result) {
        if case .error = result { } else {
            XCTFail("Expected error result, got \(result)")
        }
    }
}
