// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import XCTest
@testable import SoulCalcDomain

final class LineClassifierTests: XCTestCase {

    // MARK: - Empty Lines

    func testEmptyString() {
        XCTAssertEqual(LineClassifier.classify(""), .empty)
    }

    func testWhitespaceOnly() {
        XCTAssertEqual(LineClassifier.classify("   "), .empty)
    }

    func testTabsOnly() {
        XCTAssertEqual(LineClassifier.classify("\t\t"), .empty)
    }

    // MARK: - Comments

    func testComment() {
        XCTAssertEqual(LineClassifier.classify("# this is a comment"), .comment)
    }

    func testCommentWithLeadingSpace() {
        XCTAssertEqual(LineClassifier.classify("  # indented comment"), .comment)
    }

    func testHashOnly() {
        XCTAssertEqual(LineClassifier.classify("#"), .comment)
    }

    // MARK: - Expressions

    func testSimpleExpression() {
        XCTAssertEqual(LineClassifier.classify("1 + 2"), .expression)
    }

    func testVariable() {
        XCTAssertEqual(LineClassifier.classify("$tax = 0.08"), .expression)
    }

    func testNumber() {
        XCTAssertEqual(LineClassifier.classify("42"), .expression)
    }

    // MARK: - Helper Methods

    func testShouldEvaluate() {
        XCTAssertTrue(LineClassifier.shouldEvaluate("1 + 2"))
        XCTAssertFalse(LineClassifier.shouldEvaluate(""))
        XCTAssertFalse(LineClassifier.shouldEvaluate("# comment"))
    }

    func testIsComment() {
        XCTAssertTrue(LineClassifier.isComment("# comment"))
        XCTAssertFalse(LineClassifier.isComment("1 + 2"))
        XCTAssertFalse(LineClassifier.isComment(""))
    }

    func testIsEmpty() {
        XCTAssertTrue(LineClassifier.isEmpty(""))
        XCTAssertTrue(LineClassifier.isEmpty("   "))
        XCTAssertFalse(LineClassifier.isEmpty("1"))
        XCTAssertFalse(LineClassifier.isEmpty("# comment"))
    }

    // MARK: - Edge Cases

    func testHashInMiddle() {
        XCTAssertEqual(LineClassifier.classify("100 # comment"), .expression)
    }

    func testMultipleSpaces() {
        XCTAssertEqual(LineClassifier.classify("     "), .empty)
    }

    func testMixedWhitespace() {
        XCTAssertEqual(LineClassifier.classify(" \t "), .empty)
    }

    func testCommentWithNoSpace() {
        XCTAssertEqual(LineClassifier.classify("#comment"), .comment)
    }

    func testExpressionStartingWithNumber() {
        XCTAssertEqual(LineClassifier.classify("42 + 8"), .expression)
    }
}
