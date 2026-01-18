/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.evaluator

import net.tigr.soulcalc.domain.model.Result
import net.tigr.soulcalc.domain.model.Scope
import net.tigr.soulcalc.domain.parser.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Evaluates AST nodes to produce numeric results.
 *
 * The evaluator walks the AST recursively, resolving variables and line
 * references from the provided scope, and computing the final numeric value.
 *
 * Note on scope mutation: The evaluator maintains a mutable scope internally
 * which is updated during evaluation of assignment expressions (e.g., `$x = 5`).
 * The updated scope is returned via [EvalResult.newScope], allowing callers to
 * propagate scope changes to subsequent evaluations. Each Evaluator instance
 * should be used for a single evaluation to avoid unexpected scope state.
 */
class Evaluator(private var scope: Scope = Scope()) {

    /**
     * Result of evaluation including potential scope changes from assignments.
     */
    data class EvalResult(
        val result: Result,
        val newScope: Scope
    )

    /**
     * Evaluates an AST node and returns the result along with any scope updates.
     */
    fun evaluate(node: AstNode): EvalResult {
        return try {
            val value = eval(node)
            EvalResult(Result.Success(value), scope)
        } catch (e: EvalException) {
            EvalResult(Result.Error(e.message ?: "Evaluation error"), scope)
        }
    }

    /**
     * Evaluates a parse result directly.
     */
    fun evaluate(parseResult: Parser.ParseResult): EvalResult {
        return when (parseResult) {
            is Parser.ParseResult.Success -> evaluate(parseResult.node)
            is Parser.ParseResult.Error -> EvalResult(Result.Error(parseResult.message), scope)
            is Parser.ParseResult.Empty -> EvalResult(Result.Empty, scope)
        }
    }

    /**
     * Returns the current scope (useful after evaluating assignments).
     */
    fun getScope(): Scope = scope

    private fun eval(node: AstNode): Double {
        return when (node) {
            is NumberNode -> node.value

            is BinaryOpNode -> evalBinaryOp(node)

            is UnaryMinusNode -> -eval(node.operand)

            is PercentNode -> evalPercent(node)

            is VariableNode -> evalVariable(node)

            is LineRefNode -> evalLineRef(node)

            is AssignmentNode -> evalAssignment(node)

            is FunctionNode -> evalFunction(node)
        }
    }

    private fun evalBinaryOp(node: BinaryOpNode): Double {
        val left = eval(node.left)
        val right = eval(node.right)

        return when (node.operator) {
            BinaryOp.ADD -> left + right
            BinaryOp.SUBTRACT -> left - right
            BinaryOp.MULTIPLY -> left * right
            BinaryOp.DIVIDE -> {
                if (right == 0.0) {
                    if (left == 0.0) {
                        throw EvalException("NaN")
                    }
                    throw EvalException(if (left < 0) "-∞" else "∞")
                }
                left / right
            }
            BinaryOp.POWER -> left.pow(right)
        }
    }

    private fun evalPercent(node: PercentNode): Double {
        val operandValue = eval(node.operand)
        val percentage = operandValue / 100.0

        return if (node.base != null) {
            val baseValue = eval(node.base)
            baseValue * percentage
        } else {
            percentage
        }
    }

    private fun evalVariable(node: VariableNode): Double {
        return scope.resolveVariable(node.name)
            ?: throw EvalException("? \$${node.name}")
    }

    private fun evalLineRef(node: LineRefNode): Double {
        return scope.resolveLineRef(node.lineNumber)
            ?: throw EvalException("? \$${node.lineNumber}")
    }

    /**
     * Evaluates an assignment and updates the internal scope.
     * The updated scope is accessible via [EvalResult.newScope] after evaluation completes.
     */
    private fun evalAssignment(node: AssignmentNode): Double {
        val value = eval(node.expression)
        scope = scope.withVariable(node.variableName, value)
        return value
    }

    private fun evalFunction(node: FunctionNode): Double {
        val argValue = eval(node.argument)

        return when (node.name.lowercase()) {
            "sqrt" -> {
                if (argValue < 0) {
                    throw EvalException("NaN")
                }
                sqrt(argValue)
            }
            else -> throw EvalException("Unknown function: ${node.name}")
        }
    }

    private class EvalException(message: String) : Exception(message)
}

/**
 * Convenience function to evaluate an expression string.
 */
fun evaluateExpression(input: String, scope: Scope = Scope()): Evaluator.EvalResult {
    val parseResult = parseExpression(input)
    return Evaluator(scope).evaluate(parseResult)
}
