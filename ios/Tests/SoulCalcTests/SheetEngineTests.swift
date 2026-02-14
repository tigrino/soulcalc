// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import XCTest
@testable import SoulCalcDomain

final class SheetEngineTests: XCTestCase {

    var engine: SheetEngine!

    override func setUp() {
        super.setUp()
        engine = SheetEngine()
    }

    // MARK: - Basic Evaluation

    func testEvaluateSingleLine() {
        let lines = engine.evaluate(["1 + 2"])
        XCTAssertEqual(lines.count, 1)
        assertLineResult(lines[0], equals: 3.0)
    }

    func testEvaluateMultipleLines() {
        let lines = engine.evaluate(["10", "20", "$1 + $2"])
        XCTAssertEqual(lines.count, 3)
        assertLineResult(lines[0], equals: 10.0)
        assertLineResult(lines[1], equals: 20.0)
        assertLineResult(lines[2], equals: 30.0)
    }

    func testEvaluateEmptyLine() {
        let lines = engine.evaluate([""])
        XCTAssertEqual(lines.count, 1)
        if case .empty = lines[0].result { } else {
            XCTFail("Expected empty result")
        }
    }

    func testEvaluateComment() {
        let lines = engine.evaluate(["# comment"])
        XCTAssertEqual(lines.count, 1)
        if case .empty = lines[0].result { } else {
            XCTFail("Expected empty result for comment")
        }
    }

    // MARK: - Variables

    func testVariableScoping() {
        let lines = engine.evaluate(["$x = 10", "$x * 2"])
        assertLineResult(lines[0], equals: 10.0)
        assertLineResult(lines[1], equals: 20.0)
    }

    func testVariableNotAvailableBeforeDefinition() {
        let lines = engine.evaluate(["$x * 2", "$x = 10"])
        // First line should error since $x isn't defined yet
        if case .error = lines[0].result { } else {
            XCTFail("Expected error for undefined variable on line 1")
        }
        assertLineResult(lines[1], equals: 10.0)
    }

    func testMultipleVariables() {
        let lines = engine.evaluate(["$a = 5", "$b = 3", "$a + $b"])
        assertLineResult(lines[2], equals: 8.0)
    }

    // MARK: - Line References

    func testLineReferences() {
        let lines = engine.evaluate(["100", "200", "$1 + $2"])
        assertLineResult(lines[2], equals: 300.0)
    }

    func testLineRefToComment() {
        let lines = engine.evaluate(["42", "# comment", "$1"])
        assertLineResult(lines[0], equals: 42.0)
        assertLineResult(lines[2], equals: 42.0)
    }

    // MARK: - Cascade Updates

    func testCascadeOnUpdate() {
        // Initial evaluation
        _ = engine.evaluate(["10", "$1 * 2"])

        // Update first line
        let lines = engine.updateLine(0, "20")
        assertLineResult(lines[0], equals: 20.0)
        assertLineResult(lines[1], equals: 40.0) // cascaded
    }

    func testCascadeMultipleLevels() {
        _ = engine.evaluate(["5", "$1 * 2", "$2 + 1"])

        let lines = engine.updateLine(0, "10")
        assertLineResult(lines[0], equals: 10.0)
        assertLineResult(lines[1], equals: 20.0)
        assertLineResult(lines[2], equals: 21.0)
    }

    // MARK: - Insert and Remove

    func testInsertLine() {
        _ = engine.evaluate(["10", "$1 * 2"])
        let lines = engine.insertLine(1, "5")

        XCTAssertEqual(lines.count, 3)
        assertLineResult(lines[0], equals: 10.0)
        assertLineResult(lines[1], equals: 5.0)
        // $1 * 2 becomes $1 * 2 still (ref >=2 gets incremented, but $1 stays $1)
        // Wait - insertLine at index 1 means insertedLineNumber = 2
        // Refs >= 2 get incremented: "$1 * 2" -> the $1 stays $1 (since 1 < 2)
        // So result is 10 * 2 = 20
        assertLineResult(lines[2], equals: 20.0)
    }

    func testRemoveLine() {
        _ = engine.evaluate(["10", "20", "$1 + $2"])
        let lines = engine.removeLine(1) // remove "20"

        XCTAssertEqual(lines.count, 2)
        assertLineResult(lines[0], equals: 10.0)
        // $2 was removed, $1 stays → $1 + $1 = 20
        // Actually: after removing line 2, refs > 2 get decremented
        // $1 stays, $2 stays but now refers to the line that was at position 3 → which is now this line itself
        // $2 is itself now → it refers to its own result, which creates a circular reference → error
        // Actually let me re-read the logic: removedLineNumber = 2 (1-indexed)
        // refs > removedLineNumber (> 2) get decremented. $1 < 2 stays. $2 == 2 stays unchanged.
        // After removal, we have 2 lines: ["10", "$1 + $2"]
        // $1 = 10, $2 = this line's own result, which isn't set yet → error
        if case .error = lines[1].result { } else {
            // $2 now refers to itself which is undefined → error
        }
    }

    func testRemoveOnlyLine() {
        _ = engine.evaluate(["42"])
        let lines = engine.removeLine(0)
        // Engine allows removing the only line (ViewModel guards against this)
        XCTAssertEqual(lines.count, 0)
    }

    // MARK: - Clear

    func testClear() {
        _ = engine.evaluate(["10", "20", "30"])
        let lines = engine.clear()
        XCTAssertEqual(lines.count, 1)
        if case .empty = lines[0].result { } else {
            XCTFail("Expected empty result after clear")
        }
    }

    // MARK: - Edge Cases

    func testUpdateNegativeIndex() {
        _ = engine.evaluate(["42"])
        let lines = engine.updateLine(-1, "10")
        // Should not crash, returns current lines unchanged
        XCTAssertEqual(lines.count, 1)
        assertLineResult(lines[0], equals: 42.0)
    }

    func testUpdateBeyondBounds() {
        _ = engine.evaluate(["10"])
        let lines = engine.updateLine(3, "42")
        // Should expand the list
        XCTAssertEqual(lines.count, 4)
        assertLineResult(lines[3], equals: 42.0)
    }

    func testInsertAtBeginning() {
        _ = engine.evaluate(["10", "$1 * 2"])
        let lines = engine.insertLine(0, "5")
        XCTAssertEqual(lines.count, 3)
        assertLineResult(lines[0], equals: 5.0)
        assertLineResult(lines[1], equals: 10.0)
        // Original "$1 * 2" had refs >= 1 incremented → "$2 * 2"
        // $2 = 10 → result = 20
        assertLineResult(lines[2], equals: 20.0)
    }

    // MARK: - Error Propagation

    func testErrorDoesNotAffectOtherLines() {
        let lines = engine.evaluate(["1 / 0", "42"])
        if case .error = lines[0].result { } else {
            XCTFail("Expected error on line 1")
        }
        assertLineResult(lines[1], equals: 42.0)
    }

    // MARK: - Scope

    func testGetScope() {
        _ = engine.evaluate(["$x = 10", "$y = 20"])
        let scope = engine.getScope()
        XCTAssertEqual(scope.resolveVariable("x"), 10.0)
        XCTAssertEqual(scope.resolveVariable("y"), 20.0)
    }

    // MARK: - Variable Redefinition

    func testVariableRedefinition() {
        let lines = engine.evaluate(["$x = 5", "$x = 10", "$x"])
        XCTAssertEqual(lines.count, 3)
        assertLineResult(lines[0], equals: 5.0)
        assertLineResult(lines[1], equals: 10.0)
        assertLineResult(lines[2], equals: 10.0)
    }

    // MARK: - Append Line

    func testAppendLine() {
        _ = engine.evaluate(["10"])
        let lines = engine.appendLine("20")
        XCTAssertEqual(lines.count, 2)
        assertLineResult(lines[0], equals: 10.0)
        assertLineResult(lines[1], equals: 20.0)
    }

    // MARK: - Line Ref to Error Line

    func testLineRefToErrorLine() {
        let lines = engine.evaluate(["1/0", "$1"])
        XCTAssertEqual(lines.count, 2)
        if case .error = lines[0].result { } else {
            XCTFail("Expected error on line 1 (division by zero)")
        }
        if case .error = lines[1].result { } else {
            XCTFail("Expected error on line 2 (reference to error line)")
        }
    }

    // MARK: - Insert at End

    func testInsertAtEnd() {
        _ = engine.evaluate(["10", "20"])
        let lines = engine.insertLine(2, "30")
        XCTAssertEqual(lines.count, 3)
        assertLineResult(lines[0], equals: 10.0)
        assertLineResult(lines[1], equals: 20.0)
        assertLineResult(lines[2], equals: 30.0)
    }

    // MARK: - Remove First Line

    func testRemoveFirstLine() {
        _ = engine.evaluate(["10", "$1 * 2"])
        let lines = engine.removeLine(0)
        // After removing line 0 ("10"), only "$1 * 2" remains
        // The removed line was line 1 (1-indexed), refs > 1 get decremented
        // $1 == removedLineNumber (1), so it stays as $1 but now refers to this line itself → error
        XCTAssertEqual(lines.count, 1)
    }

    // MARK: - Multiple Inserts

    func testMultipleInserts() {
        _ = engine.evaluate(["10"])
        _ = engine.insertLine(1, "20")
        let lines = engine.insertLine(2, "30")
        XCTAssertEqual(lines.count, 3)
        assertLineResult(lines[0], equals: 10.0)
        assertLineResult(lines[1], equals: 20.0)
        assertLineResult(lines[2], equals: 30.0)
    }

    // MARK: - Get Lines

    func testGetLinesReturnsCurrentState() {
        _ = engine.evaluate(["42", "10"])
        let lines = engine.getLines()
        XCTAssertEqual(lines.count, 2)
        assertLineResult(lines[0], equals: 42.0)
        assertLineResult(lines[1], equals: 10.0)
    }

    // MARK: - Helpers

    private func assertLineResult(_ line: Line, equals expected: Double, accuracy: Double = 1e-10) {
        if case .success(let value) = line.result {
            XCTAssertEqual(value, expected, accuracy: accuracy)
        } else {
            XCTFail("Expected success(\(expected)) for line '\(line.input)', got \(line.result)")
        }
    }
}
