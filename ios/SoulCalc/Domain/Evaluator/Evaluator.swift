// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Result of evaluation including potential scope changes from assignments.
struct EvalResult {
    let result: Result
    let newScope: Scope
}

/// Evaluates AST nodes to produce numeric results.
///
/// The evaluator walks the AST recursively, resolving variables and line
/// references from the provided scope, and computing the final numeric value.
class Evaluator {
    private var scope: Scope

    init(_ scope: Scope = Scope()) {
        self.scope = scope
    }

    /// Evaluates an AST node and returns the result along with any scope updates.
    func evaluate(_ node: AstNode) -> EvalResult {
        do {
            let value = try eval(node)
            return EvalResult(result: .success(value), newScope: scope)
        } catch let error as EvalError {
            return EvalResult(result: .error(error.message), newScope: scope)
        } catch {
            return EvalResult(result: .error("Evaluation error"), newScope: scope)
        }
    }

    /// Evaluates a parse result directly.
    func evaluate(_ parseResult: ParseResult) -> EvalResult {
        switch parseResult {
        case .success(let node):
            return evaluate(node)
        case .error(let message, _):
            return EvalResult(result: .error(message), newScope: scope)
        case .empty:
            return EvalResult(result: .empty, newScope: scope)
        }
    }

    /// Returns the current scope.
    func getScope() -> Scope { scope }

    private func eval(_ node: AstNode) throws -> Double {
        switch node {
        case .number(let value):
            return value

        case .binaryOp(let left, let op, let right):
            return try evalBinaryOp(left: left, op: op, right: right)

        case .unaryMinus(let operand):
            return try -eval(operand)

        case .percent(let operand, let base):
            return try evalPercent(operand: operand, base: base)

        case .variable(let name):
            return try evalVariable(name)

        case .lineRef(let lineNumber):
            return try evalLineRef(lineNumber)

        case .assignment(let variableName, let expression):
            return try evalAssignment(variableName: variableName, expression: expression)

        case .function(let name, let argument):
            return try evalFunction(name: name, argument: argument)
        }
    }

    private func evalBinaryOp(left: AstNode, op: BinaryOp, right: AstNode) throws -> Double {
        let leftVal = try eval(left)
        let rightVal = try eval(right)

        switch op {
        case .add:
            return leftVal + rightVal
        case .subtract:
            return leftVal - rightVal
        case .multiply:
            return leftVal * rightVal
        case .divide:
            if rightVal == 0.0 {
                if leftVal == 0.0 {
                    throw EvalError("NaN")
                }
                throw EvalError(leftVal < 0 ? "-∞" : "∞")
            }
            return leftVal / rightVal
        case .power:
            return pow(leftVal, rightVal)
        }
    }

    private func evalPercent(operand: AstNode, base: AstNode?) throws -> Double {
        let operandValue = try eval(operand)
        let percentage = operandValue / 100.0

        if let base = base {
            let baseValue = try eval(base)
            return baseValue * percentage
        } else {
            return percentage
        }
    }

    private func evalVariable(_ name: String) throws -> Double {
        guard let value = scope.resolveVariable(name) else {
            throw EvalError("? $\(name)")
        }
        return value
    }

    private func evalLineRef(_ lineNumber: Int) throws -> Double {
        guard let value = scope.resolveLineRef(lineNumber) else {
            throw EvalError("? $\(lineNumber)")
        }
        return value
    }

    private func evalAssignment(variableName: String, expression: AstNode) throws -> Double {
        let value = try eval(expression)
        scope = scope.withVariable(variableName, value)
        return value
    }

    private func evalFunction(name: String, argument: AstNode) throws -> Double {
        let argValue = try eval(argument)

        switch name.lowercased() {
        case "sqrt":
            if argValue < 0 {
                throw EvalError("NaN")
            }
            return Foundation.sqrt(argValue)
        default:
            throw EvalError("Unknown function: \(name)")
        }
    }
}

/// Error thrown during evaluation.
private struct EvalError: Error {
    let message: String

    init(_ message: String) {
        self.message = message
    }
}

/// Convenience function to evaluate an expression string.
func evaluateExpression(_ input: String, scope: Scope = Scope()) -> EvalResult {
    let parseResult = parseExpression(input)
    return Evaluator(scope).evaluate(parseResult)
}
