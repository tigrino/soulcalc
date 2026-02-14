// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Albert Zenkoff

import Foundation

/// Binary operators.
enum BinaryOp {
    case add        // +
    case subtract   // -
    case multiply   // *
    case divide     // /
    case power      // ^
}

/// Abstract Syntax Tree nodes representing parsed expressions.
indirect enum AstNode {
    /// A numeric literal.
    case number(Double)

    /// A binary operation (e.g., 1 + 2, 3 * 4).
    case binaryOp(left: AstNode, op: BinaryOp, right: AstNode)

    /// Unary minus (e.g., -5).
    case unaryMinus(AstNode)

    /// Percentage operation.
    /// `base` is the base value for contextual percentage (e.g., 100 in "100 + 10%").
    /// nil for standalone percentage (e.g., "10%" -> 0.1).
    case percent(operand: AstNode, base: AstNode?)

    /// A named variable reference (e.g., $tax).
    case variable(String)

    /// A line reference (e.g., $1, $2).
    case lineRef(Int)

    /// A variable assignment (e.g., $x = 5).
    case assignment(variableName: String, expression: AstNode)

    /// A function call (e.g., sqrt(16)).
    case function(name: String, argument: AstNode)
}
