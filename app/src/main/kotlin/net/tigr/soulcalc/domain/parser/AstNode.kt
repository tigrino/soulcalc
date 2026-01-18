/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.parser

/**
 * Abstract Syntax Tree nodes representing parsed expressions.
 */
sealed class AstNode

/**
 * A numeric literal.
 */
data class NumberNode(val value: Double) : AstNode()

/**
 * A binary operation (e.g., 1 + 2, 3 * 4).
 */
data class BinaryOpNode(
    val left: AstNode,
    val operator: BinaryOp,
    val right: AstNode
) : AstNode()

/**
 * Binary operators.
 */
enum class BinaryOp {
    ADD,        // +
    SUBTRACT,   // -
    MULTIPLY,   // *
    DIVIDE,     // /
    POWER       // ^
}

/**
 * Unary minus (e.g., -5).
 */
data class UnaryMinusNode(val operand: AstNode) : AstNode()

/**
 * Percentage operation.
 *
 * @property operand The value to convert to percentage
 * @property base The base value for contextual percentage (e.g., 100 in "100 + 10%").
 *                Null for standalone percentage (e.g., "10%" -> 0.1).
 */
data class PercentNode(
    val operand: AstNode,
    val base: AstNode? = null
) : AstNode()

/**
 * A named variable reference (e.g., $tax).
 */
data class VariableNode(val name: String) : AstNode()

/**
 * A line reference (e.g., $1, $2).
 */
data class LineRefNode(val lineNumber: Int) : AstNode()

/**
 * A variable assignment (e.g., $x = 5).
 */
data class AssignmentNode(
    val variableName: String,
    val expression: AstNode
) : AstNode()

/**
 * A function call (e.g., sqrt(16)).
 */
data class FunctionNode(
    val name: String,
    val argument: AstNode
) : AstNode()
