/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.parser

import org.junit.Assert.*
import org.junit.Test

class ParserTest {

    private fun parse(input: String): Parser.ParseResult = parseExpression(input)

    private fun parseSuccess(input: String): AstNode {
        val result = parse(input)
        assertTrue("Expected Success but got $result", result is Parser.ParseResult.Success)
        return (result as Parser.ParseResult.Success).node
    }

    private fun parseError(input: String): String {
        val result = parse(input)
        assertTrue("Expected Error but got $result", result is Parser.ParseResult.Error)
        return (result as Parser.ParseResult.Error).message
    }

    // === Basic Numbers ===

    @Test
    fun `single integer`() {
        val node = parseSuccess("42")
        assertEquals(NumberNode(42.0), node)
    }

    @Test
    fun `single decimal`() {
        val node = parseSuccess("3.14")
        assertEquals(NumberNode(3.14), node)
    }

    @Test
    fun `decimal starting with dot`() {
        val node = parseSuccess(".5")
        assertEquals(NumberNode(0.5), node)
    }

    // === Binary Operations ===

    @Test
    fun `simple addition`() {
        val node = parseSuccess("1 + 2")
        assertEquals(
            BinaryOpNode(NumberNode(1.0), BinaryOp.ADD, NumberNode(2.0)),
            node
        )
    }

    @Test
    fun `simple subtraction`() {
        val node = parseSuccess("5 - 3")
        assertEquals(
            BinaryOpNode(NumberNode(5.0), BinaryOp.SUBTRACT, NumberNode(3.0)),
            node
        )
    }

    @Test
    fun `simple multiplication`() {
        val node = parseSuccess("4 * 3")
        assertEquals(
            BinaryOpNode(NumberNode(4.0), BinaryOp.MULTIPLY, NumberNode(3.0)),
            node
        )
    }

    @Test
    fun `simple division`() {
        val node = parseSuccess("10 / 2")
        assertEquals(
            BinaryOpNode(NumberNode(10.0), BinaryOp.DIVIDE, NumberNode(2.0)),
            node
        )
    }

    // === Operator Precedence ===

    @Test
    fun `multiplication before addition`() {
        val node = parseSuccess("1 + 2 * 3")
        // Should parse as 1 + (2 * 3)
        assertEquals(
            BinaryOpNode(
                NumberNode(1.0),
                BinaryOp.ADD,
                BinaryOpNode(NumberNode(2.0), BinaryOp.MULTIPLY, NumberNode(3.0))
            ),
            node
        )
    }

    @Test
    fun `division before subtraction`() {
        val node = parseSuccess("10 - 6 / 2")
        // Should parse as 10 - (6 / 2)
        assertEquals(
            BinaryOpNode(
                NumberNode(10.0),
                BinaryOp.SUBTRACT,
                BinaryOpNode(NumberNode(6.0), BinaryOp.DIVIDE, NumberNode(2.0))
            ),
            node
        )
    }

    @Test
    fun `parentheses override precedence`() {
        val node = parseSuccess("(1 + 2) * 3")
        // Should parse as (1 + 2) * 3
        assertEquals(
            BinaryOpNode(
                BinaryOpNode(NumberNode(1.0), BinaryOp.ADD, NumberNode(2.0)),
                BinaryOp.MULTIPLY,
                NumberNode(3.0)
            ),
            node
        )
    }

    @Test
    fun `nested parentheses`() {
        val node = parseSuccess("((1 + 2))")
        assertEquals(
            BinaryOpNode(NumberNode(1.0), BinaryOp.ADD, NumberNode(2.0)),
            node
        )
    }

    @Test
    fun `complex precedence`() {
        val node = parseSuccess("1 + 2 * 3 - 4 / 2")
        // Should parse as (1 + (2 * 3)) - (4 / 2)
        val expected = BinaryOpNode(
            BinaryOpNode(
                NumberNode(1.0),
                BinaryOp.ADD,
                BinaryOpNode(NumberNode(2.0), BinaryOp.MULTIPLY, NumberNode(3.0))
            ),
            BinaryOp.SUBTRACT,
            BinaryOpNode(NumberNode(4.0), BinaryOp.DIVIDE, NumberNode(2.0))
        )
        assertEquals(expected, node)
    }

    // === Unary Minus ===

    @Test
    fun `unary minus`() {
        val node = parseSuccess("-5")
        assertEquals(UnaryMinusNode(NumberNode(5.0)), node)
    }

    @Test
    fun `double negative`() {
        val node = parseSuccess("--5")
        assertEquals(UnaryMinusNode(UnaryMinusNode(NumberNode(5.0))), node)
    }

    @Test
    fun `unary minus in expression`() {
        val node = parseSuccess("10 + -5")
        assertEquals(
            BinaryOpNode(
                NumberNode(10.0),
                BinaryOp.ADD,
                UnaryMinusNode(NumberNode(5.0))
            ),
            node
        )
    }

    // === Power Operator ===

    @Test
    fun `simple power`() {
        val node = parseSuccess("2^3")
        assertEquals(
            BinaryOpNode(NumberNode(2.0), BinaryOp.POWER, NumberNode(3.0)),
            node
        )
    }

    @Test
    fun `power right associative`() {
        val node = parseSuccess("2^3^4")
        // Should parse as 2^(3^4), not (2^3)^4
        assertEquals(
            BinaryOpNode(
                NumberNode(2.0),
                BinaryOp.POWER,
                BinaryOpNode(NumberNode(3.0), BinaryOp.POWER, NumberNode(4.0))
            ),
            node
        )
    }

    @Test
    fun `power higher precedence than multiply`() {
        val node = parseSuccess("2 * 3^2")
        // Should parse as 2 * (3^2)
        assertEquals(
            BinaryOpNode(
                NumberNode(2.0),
                BinaryOp.MULTIPLY,
                BinaryOpNode(NumberNode(3.0), BinaryOp.POWER, NumberNode(2.0))
            ),
            node
        )
    }

    // === Percentage ===

    @Test
    fun `standalone percentage`() {
        val node = parseSuccess("10%")
        assertEquals(PercentNode(NumberNode(10.0), null), node)
    }

    @Test
    fun `additive percentage`() {
        val node = parseSuccess("100 + 10%")
        // The 10% should have base=100 for contextual percentage
        val expected = BinaryOpNode(
            NumberNode(100.0),
            BinaryOp.ADD,
            PercentNode(NumberNode(10.0), NumberNode(100.0))
        )
        assertEquals(expected, node)
    }

    @Test
    fun `subtractive percentage`() {
        val node = parseSuccess("100 - 10%")
        // The 10% should have base=100 for contextual percentage
        val expected = BinaryOpNode(
            NumberNode(100.0),
            BinaryOp.SUBTRACT,
            PercentNode(NumberNode(10.0), NumberNode(100.0))
        )
        assertEquals(expected, node)
    }

    @Test
    fun `multiplicative percentage no context`() {
        val node = parseSuccess("100 * 10%")
        // Multiplication doesn't apply percentage context
        val expected = BinaryOpNode(
            NumberNode(100.0),
            BinaryOp.MULTIPLY,
            PercentNode(NumberNode(10.0), null)
        )
        assertEquals(expected, node)
    }

    @Test
    fun `percentage in parentheses`() {
        val node = parseSuccess("(5 + 5)%")
        val expected = PercentNode(
            BinaryOpNode(NumberNode(5.0), BinaryOp.ADD, NumberNode(5.0)),
            null
        )
        assertEquals(expected, node)
    }

    // === Variables ===

    @Test
    fun `simple variable`() {
        val node = parseSuccess("\$tax")
        assertEquals(VariableNode("tax"), node)
    }

    @Test
    fun `variable in expression`() {
        val node = parseSuccess("\$a + \$b")
        assertEquals(
            BinaryOpNode(VariableNode("a"), BinaryOp.ADD, VariableNode("b")),
            node
        )
    }

    @Test
    fun `variable with number`() {
        val node = parseSuccess("\$price * 2")
        assertEquals(
            BinaryOpNode(VariableNode("price"), BinaryOp.MULTIPLY, NumberNode(2.0)),
            node
        )
    }

    // === Line References ===

    @Test
    fun `simple line reference`() {
        val node = parseSuccess("\$1")
        assertEquals(LineRefNode(1), node)
    }

    @Test
    fun `line reference in expression`() {
        val node = parseSuccess("\$1 + \$2")
        assertEquals(
            BinaryOpNode(LineRefNode(1), BinaryOp.ADD, LineRefNode(2)),
            node
        )
    }

    @Test
    fun `line reference multi digit`() {
        val node = parseSuccess("\$123")
        assertEquals(LineRefNode(123), node)
    }

    // === Assignments ===

    @Test
    fun `simple assignment`() {
        val node = parseSuccess("\$x = 5")
        assertEquals(AssignmentNode("x", NumberNode(5.0)), node)
    }

    @Test
    fun `assignment with expression`() {
        val node = parseSuccess("\$total = 100 + 50")
        assertEquals(
            AssignmentNode(
                "total",
                BinaryOpNode(NumberNode(100.0), BinaryOp.ADD, NumberNode(50.0))
            ),
            node
        )
    }

    @Test
    fun `assignment with variable reference`() {
        val node = parseSuccess("\$y = \$x * 2")
        assertEquals(
            AssignmentNode(
                "y",
                BinaryOpNode(VariableNode("x"), BinaryOp.MULTIPLY, NumberNode(2.0))
            ),
            node
        )
    }

    // === Functions ===

    @Test
    fun `sqrt function`() {
        val node = parseSuccess("sqrt(16)")
        assertEquals(FunctionNode("sqrt", NumberNode(16.0)), node)
    }

    @Test
    fun `sqrt with expression`() {
        val node = parseSuccess("sqrt(1 + 3)")
        assertEquals(
            FunctionNode(
                "sqrt",
                BinaryOpNode(NumberNode(1.0), BinaryOp.ADD, NumberNode(3.0))
            ),
            node
        )
    }

    @Test
    fun `sqrt in expression`() {
        val node = parseSuccess("2 * sqrt(4)")
        assertEquals(
            BinaryOpNode(
                NumberNode(2.0),
                BinaryOp.MULTIPLY,
                FunctionNode("sqrt", NumberNode(4.0))
            ),
            node
        )
    }

    @Test
    fun `nested sqrt`() {
        val node = parseSuccess("sqrt(sqrt(16))")
        assertEquals(
            FunctionNode("sqrt", FunctionNode("sqrt", NumberNode(16.0))),
            node
        )
    }

    // === Complex Expressions ===

    @Test
    fun `realistic budget expression`() {
        val node = parseSuccess("\$income - \$rent - \$utilities")
        val expected = BinaryOpNode(
            BinaryOpNode(
                VariableNode("income"),
                BinaryOp.SUBTRACT,
                VariableNode("rent")
            ),
            BinaryOp.SUBTRACT,
            VariableNode("utilities")
        )
        assertEquals(expected, node)
    }

    @Test
    fun `price with tax percentage`() {
        val node = parseSuccess("\$price + 8%")
        val expected = BinaryOpNode(
            VariableNode("price"),
            BinaryOp.ADD,
            PercentNode(NumberNode(8.0), VariableNode("price"))
        )
        assertEquals(expected, node)
    }

    // === Empty Input ===

    @Test
    fun `empty input returns Empty`() {
        val result = parse("")
        assertTrue(result is Parser.ParseResult.Empty)
    }

    @Test
    fun `whitespace only returns Empty`() {
        val result = parse("   ")
        assertTrue(result is Parser.ParseResult.Empty)
    }

    // === Error Cases ===

    @Test
    fun `missing operand`() {
        val error = parseError("1 +")
        assertTrue(error.contains("end of input") || error.contains("Unexpected"))
    }

    @Test
    fun `unmatched left paren`() {
        val error = parseError("(1 + 2")
        assertTrue(error.contains(")") || error.contains("paren"))
    }

    @Test
    fun `unmatched right paren`() {
        val error = parseError("1 + 2)")
        assertTrue(error.contains("Unexpected"))
    }

    @Test
    fun `sqrt without parens`() {
        val error = parseError("sqrt 4")
        assertTrue(error.contains("("))
    }

    @Test
    fun `invalid token`() {
        val error = parseError("1 @ 2")
        assertTrue(error.contains("Invalid") || error.contains("@"))
    }

    @Test
    fun `consecutive operators`() {
        val error = parseError("1 + + 2")
        // Should fail because + is not a valid primary
        assertTrue(error.contains("Unexpected"))
    }

    @Test
    fun `percentage followed by number without operator`() {
        // "8%2" should be an error - % is a postfix operator, not infix modulo
        // The parser sees "8%" as a complete expression, then "2" is unexpected
        val error = parseError("8%2")
        assertTrue("Expected error for '8%2' but got: $error",
            error.contains("Unexpected") || error.contains("end") || error.isNotEmpty())
    }

    @Test
    fun `line reference overflow`() {
        // Very large line reference numbers should produce error, not crash
        val error = parseError("\$9999999999999999999")
        assertTrue("Expected error for overflow line reference but got: $error",
            error.contains("Invalid line reference") || error.isNotEmpty())
    }

    // === Unicode Operators ===

    @Test
    fun `unicode multiply`() {
        val node = parseSuccess("4 × 3")
        assertEquals(
            BinaryOpNode(NumberNode(4.0), BinaryOp.MULTIPLY, NumberNode(3.0)),
            node
        )
    }

    @Test
    fun `unicode divide`() {
        val node = parseSuccess("10 ÷ 2")
        assertEquals(
            BinaryOpNode(NumberNode(10.0), BinaryOp.DIVIDE, NumberNode(2.0)),
            node
        )
    }

    @Test
    fun `unicode minus`() {
        val node = parseSuccess("5 − 3")
        assertEquals(
            BinaryOpNode(NumberNode(5.0), BinaryOp.SUBTRACT, NumberNode(3.0)),
            node
        )
    }
}
