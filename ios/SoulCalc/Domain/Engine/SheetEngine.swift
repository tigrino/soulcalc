// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Coordinates multi-line evaluation with scope management.
///
/// The SheetEngine evaluates lines top-to-bottom, building up the scope
/// as it goes. Variables defined on earlier lines are available to later
/// lines. Line results are stored and can be referenced via $n syntax.
///
/// When a line changes, all lines from that point forward are re-evaluated
/// to ensure cascade updates work correctly.
class SheetEngine {
    private let lock = NSLock()
    private var lines: [Line] = []
    private var currentScope: Scope = Scope()

    /// Evaluates a list of input strings and returns the resulting lines.
    func evaluate(_ inputs: [String]) -> [Line] {
        lock.lock()
        defer { lock.unlock() }
        return evaluateInternal(inputs)
    }

    /// Internal evaluation without locking.
    private func evaluateInternal(_ inputs: [String]) -> [Line] {
        lines = []
        currentScope = Scope()

        for (index, input) in inputs.enumerated() {
            let line = evaluateLine(index, input)
            lines.append(line)
        }

        return lines
    }

    /// Updates a single line and re-evaluates all affected lines.
    func updateLine(_ index: Int, _ input: String) -> [Line] {
        lock.lock()
        defer { lock.unlock() }

        if index < 0 { return lines }

        // Expand list if needed
        while lines.count <= index {
            lines.append(Line(id: lines.count, position: lines.count, input: "", result: .empty))
        }

        // Re-evaluate from the changed line onwards
        let inputs = lines.enumerated().map { i, line in
            i == index ? input : line.input
        }

        return evaluateInternal(inputs)
    }

    /// Appends a new line and evaluates it.
    func appendLine(_ input: String) -> [Line] {
        lock.lock()
        defer { lock.unlock() }

        let newIndex = lines.count
        let line = evaluateLine(newIndex, input)
        lines.append(line)
        return lines
    }

    /// Removes a line and re-evaluates all subsequent lines.
    /// Updates line references ($N) in all lines to maintain correct references.
    func removeLine(_ index: Int) -> [Line] {
        lock.lock()
        defer { lock.unlock() }

        if index < 0 || index >= lines.count { return lines }

        let removedLineNumber = index + 1
        let inputs = lines.enumerated().compactMap { i, line -> String? in
            if i == index { return nil }
            return updateLineReferencesAfterRemove(line.input, removedLineNumber)
        }

        return evaluateInternal(inputs)
    }

    /// Inserts a new line at the specified index and re-evaluates.
    /// Updates line references ($N) in all lines to maintain correct references.
    func insertLine(_ index: Int, _ input: String) -> [Line] {
        lock.lock()
        defer { lock.unlock() }

        let safeIndex = max(0, min(index, lines.count))
        let insertedLineNumber = safeIndex + 1
        var inputs = lines.map { updateLineReferencesAfterInsert($0.input, insertedLineNumber) }
        inputs.insert(input, at: safeIndex)
        return evaluateInternal(inputs)
    }

    /// Returns the current list of lines.
    func getLines() -> [Line] {
        lock.lock()
        defer { lock.unlock() }
        return lines
    }

    /// Returns the current scope after evaluation.
    func getScope() -> Scope {
        lock.lock()
        defer { lock.unlock() }
        return currentScope
    }

    /// Clears all lines and resets to a single empty line.
    func clear() -> [Line] {
        lock.lock()
        defer { lock.unlock() }

        currentScope = Scope()
        lines = [Line(id: 0, position: 0, input: "", result: .empty)]
        return lines
    }

    // MARK: - Private

    private static let lineReferenceRegex = try! NSRegularExpression(pattern: #"\$(\d+)"#)

    private func evaluateLine(_ index: Int, _ input: String) -> Line {
        let lineNumber = index + 1 // 1-based for user display and references

        let result: Result
        switch LineClassifier.classify(input) {
        case .empty, .comment:
            result = .empty
        case .expression:
            let parseResult = parseExpression(input)
            let evaluator = Evaluator(currentScope)
            let evalResult = evaluator.evaluate(parseResult)

            // Update scope with any new variables
            currentScope = evalResult.newScope

            // Store line result for future references
            if case .success(let value) = evalResult.result {
                currentScope = currentScope.withLineResult(lineNumber, value)
            }

            result = evalResult.result
        }

        return Line(id: index, position: index, input: input, result: result)
    }

    /// Updates line references ($N) after a line is inserted.
    /// All references to lines >= insertedLineNumber are incremented by 1.
    private func updateLineReferencesAfterInsert(_ input: String, _ insertedLineNumber: Int) -> String {
        let nsInput = input as NSString
        let range = NSRange(location: 0, length: nsInput.length)
        let matches = Self.lineReferenceRegex.matches(in: input, range: range).reversed()

        var result = input
        for match in matches {
            guard let numRange = Range(match.range(at: 1), in: input),
                  let refNumber = Int(input[numRange]) else { continue }
            if refNumber >= insertedLineNumber {
                let fullRange = Range(match.range, in: result)!
                result.replaceSubrange(fullRange, with: "$\(refNumber + 1)")
            }
        }
        return result
    }

    /// Updates line references ($N) after a line is removed.
    /// All references to lines > removedLineNumber are decremented by 1.
    private func updateLineReferencesAfterRemove(_ input: String, _ removedLineNumber: Int) -> String {
        let nsInput = input as NSString
        let range = NSRange(location: 0, length: nsInput.length)
        let matches = Self.lineReferenceRegex.matches(in: input, range: range).reversed()

        var result = input
        for match in matches {
            guard let numRange = Range(match.range(at: 1), in: input),
                  let refNumber = Int(input[numRange]) else { continue }
            if refNumber > removedLineNumber {
                let fullRange = Range(match.range, in: result)!
                result.replaceSubrange(fullRange, with: "$\(refNumber - 1)")
            }
        }
        return result
    }
}
