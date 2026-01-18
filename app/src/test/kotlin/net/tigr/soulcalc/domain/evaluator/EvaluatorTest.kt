/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.evaluator

import net.tigr.soulcalc.domain.model.Result
import net.tigr.soulcalc.domain.model.Scope
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class EvaluatorTest {

    private fun eval(input: String, scope: Scope = Scope()): Evaluator.EvalResult {
        return evaluateExpression(input, scope)
    }

    private fun evalValue(input: String, scope: Scope = Scope()): Double {
        val result = eval(input, scope)
        assertTrue("Expected Success but got ${result.result}", result.result is Result.Success)
        return (result.result as Result.Success).value
    }

    private fun evalError(input: String, scope: Scope = Scope()): String {
        val result = eval(input, scope)
        assertTrue("Expected Error but got ${result.result}", result.result is Result.Error)
        return (result.result as Result.Error).message
    }

    private fun assertApproxEquals(expected: Double, actual: Double, delta: Double = 0.0001) {
        assertTrue(
            "Expected $expected but got $actual",
            abs(expected - actual) < delta
        )
    }

    // === Basic Arithmetic ===

    @Test
    fun `integer literal`() {
        assertEquals(42.0, evalValue("42"), 0.0)
    }

    @Test
    fun `decimal literal`() {
        assertEquals(3.14, evalValue("3.14"), 0.0001)
    }

    @Test
    fun `simple addition`() {
        assertEquals(3.0, evalValue("1 + 2"), 0.0)
    }

    @Test
    fun `simple subtraction`() {
        assertEquals(2.0, evalValue("5 - 3"), 0.0)
    }

    @Test
    fun `simple multiplication`() {
        assertEquals(12.0, evalValue("4 * 3"), 0.0)
    }

    @Test
    fun `simple division`() {
        assertEquals(5.0, evalValue("10 / 2"), 0.0)
    }

    @Test
    fun `power operation`() {
        assertEquals(256.0, evalValue("2^8"), 0.0)
    }

    @Test
    fun `unary minus`() {
        assertEquals(-5.0, evalValue("-5"), 0.0)
    }

    @Test
    fun `double negative`() {
        assertEquals(5.0, evalValue("--5"), 0.0)
    }

    // === Operator Precedence ===

    @Test
    fun `multiplication before addition`() {
        assertEquals(7.0, evalValue("1 + 2 * 3"), 0.0)
    }

    @Test
    fun `division before subtraction`() {
        assertEquals(7.0, evalValue("10 - 6 / 2"), 0.0)
    }

    @Test
    fun `parentheses override precedence`() {
        assertEquals(9.0, evalValue("(1 + 2) * 3"), 0.0)
    }

    @Test
    fun `power before multiplication`() {
        assertEquals(18.0, evalValue("2 * 3^2"), 0.0)
    }

    @Test
    fun `power right associative`() {
        // 2^3^2 = 2^9 = 512 (not (2^3)^2 = 64)
        assertEquals(512.0, evalValue("2^3^2"), 0.0)
    }

    @Test
    fun `complex expression`() {
        // 1 + 2 * 3 - 4 / 2 = 1 + 6 - 2 = 5
        assertEquals(5.0, evalValue("1 + 2 * 3 - 4 / 2"), 0.0)
    }

    // === Percentage Operations (FR-05) ===

    @Test
    fun `standalone percentage`() {
        // 10% = 0.1
        assertEquals(0.1, evalValue("10%"), 0.0)
    }

    @Test
    fun `additive percentage`() {
        // 100 + 10% = 100 + (100 * 0.1) = 110
        assertEquals(110.0, evalValue("100 + 10%"), 0.0)
    }

    @Test
    fun `subtractive percentage`() {
        // 100 - 10% = 100 - (100 * 0.1) = 90
        assertEquals(90.0, evalValue("100 - 10%"), 0.0)
    }

    @Test
    fun `multiplicative percentage`() {
        // 100 * 10% = 100 * 0.1 = 10
        assertEquals(10.0, evalValue("100 * 10%"), 0.0)
    }

    @Test
    fun `divisive percentage`() {
        // 100 / 10% = 100 / 0.1 = 1000
        assertEquals(1000.0, evalValue("100 / 10%"), 0.0)
    }

    @Test
    fun `percentage of parenthesized expression`() {
        // (5 + 5)% = 10% = 0.1
        assertEquals(0.1, evalValue("(5 + 5)%"), 0.0)
    }

    @Test
    fun `chained additive percentage`() {
        // 50 + 50 + 10% = (50 + 50) + 10% of (50+50) = 100 + 10 = 110
        assertEquals(110.0, evalValue("50 + 50 + 10%"), 0.0)
    }

    @Test
    fun `percentage with decimal`() {
        // 100 + 7.5% = 107.5
        assertEquals(107.5, evalValue("100 + 7.5%"), 0.0)
    }

    @Test
    fun `percentage 50 percent`() {
        // 200 - 50% = 100
        assertEquals(100.0, evalValue("200 - 50%"), 0.0)
    }

    // === Functions ===

    @Test
    fun `sqrt of perfect square`() {
        assertEquals(4.0, evalValue("sqrt(16)"), 0.0)
    }

    @Test
    fun `sqrt of 2`() {
        assertApproxEquals(1.41421356, evalValue("sqrt(2)"))
    }

    @Test
    fun `sqrt of 0`() {
        assertEquals(0.0, evalValue("sqrt(0)"), 0.0)
    }

    @Test
    fun `sqrt of negative returns error`() {
        val error = evalError("sqrt(-1)")
        assertEquals("NaN", error)
    }

    @Test
    fun `sqrt in expression`() {
        // 2 * sqrt(9) = 2 * 3 = 6
        assertEquals(6.0, evalValue("2 * sqrt(9)"), 0.0)
    }

    @Test
    fun `nested sqrt`() {
        // sqrt(sqrt(16)) = sqrt(4) = 2
        assertEquals(2.0, evalValue("sqrt(sqrt(16))"), 0.0)
    }

    @Test
    fun `sqrt of expression`() {
        // sqrt(9 + 16) = sqrt(25) = 5
        assertEquals(5.0, evalValue("sqrt(9 + 16)"), 0.0)
    }

    // === Variables ===

    @Test
    fun `resolve simple variable`() {
        val scope = Scope(variables = mapOf("tax" to 0.08))
        assertEquals(0.08, evalValue("\$tax", scope), 0.0)
    }

    @Test
    fun `variable in expression`() {
        val scope = Scope(variables = mapOf("price" to 100.0, "tax" to 0.08))
        assertEquals(8.0, evalValue("\$price * \$tax", scope), 0.0)
    }

    @Test
    fun `undefined variable returns error`() {
        val error = evalError("\$undefined")
        assertTrue(error.contains("\$undefined"))
    }

    @Test
    fun `variable with underscore`() {
        val scope = Scope(variables = mapOf("tax_rate" to 0.07))
        assertEquals(0.07, evalValue("\$tax_rate", scope), 0.0)
    }

    // === Line References ===

    @Test
    fun `resolve simple line reference`() {
        val scope = Scope(lineResults = mapOf(1 to 100.0))
        assertEquals(100.0, evalValue("\$1", scope), 0.0)
    }

    @Test
    fun `line reference in expression`() {
        val scope = Scope(lineResults = mapOf(1 to 100.0, 2 to 50.0))
        assertEquals(150.0, evalValue("\$1 + \$2", scope), 0.0)
    }

    @Test
    fun `undefined line reference returns error`() {
        val error = evalError("\$99")
        assertTrue(error.contains("\$99"))
    }

    @Test
    fun `line reference multiply`() {
        val scope = Scope(lineResults = mapOf(1 to 25.0))
        assertEquals(50.0, evalValue("\$1 * 2", scope), 0.0)
    }

    // === Assignments ===

    @Test
    fun `simple assignment returns value`() {
        assertEquals(5.0, evalValue("\$x = 5"), 0.0)
    }

    @Test
    fun `assignment updates scope`() {
        val result = eval("\$x = 42")
        assertTrue(result.result is Result.Success)
        assertEquals(42.0, result.newScope.resolveVariable("x"))
    }

    @Test
    fun `assignment with expression`() {
        assertEquals(150.0, evalValue("\$total = 100 + 50"), 0.0)
    }

    @Test
    fun `assignment with variable reference`() {
        val scope = Scope(variables = mapOf("x" to 10.0))
        val result = eval("\$y = \$x * 2", scope)
        assertTrue(result.result is Result.Success)
        assertEquals(20.0, (result.result as Result.Success).value, 0.0)
        assertEquals(20.0, result.newScope.resolveVariable("y"))
    }

    @Test
    fun `chained assignment in scope`() {
        val result1 = eval("\$x = 10")
        val result2 = eval("\$y = \$x + 5", result1.newScope)
        assertEquals(15.0, (result2.result as Result.Success).value, 0.0)
    }

    // === Division by Zero ===

    @Test
    fun `division by zero returns infinity error`() {
        val error = evalError("1 / 0")
        assertEquals("∞", error)
    }

    @Test
    fun `negative division by zero returns negative infinity error`() {
        val error = evalError("-5 / 0")
        assertEquals("-∞", error)
    }

    @Test
    fun `expression evaluating to negative divided by zero returns negative infinity`() {
        val error = evalError("(3 - 8) / 0")
        assertEquals("-∞", error)
    }

    @Test
    fun `zero divided by zero returns NaN error`() {
        val error = evalError("0 / 0")
        assertEquals("NaN", error)
    }

    // === Empty Input ===

    @Test
    fun `empty input returns Empty`() {
        val result = eval("")
        assertTrue(result.result is Result.Empty)
    }

    @Test
    fun `whitespace only returns Empty`() {
        val result = eval("   ")
        assertTrue(result.result is Result.Empty)
    }

    // === Complex Real-world Expressions ===

    @Test
    fun `budget calculation`() {
        val scope = Scope(variables = mapOf(
            "income" to 5000.0,
            "rent" to 1500.0,
            "utilities" to 200.0
        ))
        assertEquals(3300.0, evalValue("\$income - \$rent - \$utilities", scope), 0.0)
    }

    @Test
    fun `price with tax`() {
        // $99.99 + 8.5% tax
        val scope = Scope(variables = mapOf("price" to 99.99))
        assertApproxEquals(108.48915, evalValue("\$price + 8.5%", scope))
    }

    @Test
    fun `discount calculation`() {
        // Original price minus 25% discount
        assertEquals(75.0, evalValue("100 - 25%"), 0.0)
    }

    @Test
    fun `compound interest formula simplified`() {
        // Principal * (1 + rate)^years - simplified version
        // 1000 * 1.05^3 = 1157.625
        assertApproxEquals(1157.625, evalValue("1000 * 1.05^3"))
    }

    @Test
    fun `pythagorean theorem`() {
        // sqrt(3^2 + 4^2) = sqrt(9 + 16) = sqrt(25) = 5
        assertEquals(5.0, evalValue("sqrt(3^2 + 4^2)"), 0.0)
    }

    // === Unicode Operators ===

    @Test
    fun `unicode multiply`() {
        assertEquals(12.0, evalValue("4 × 3"), 0.0)
    }

    @Test
    fun `unicode divide`() {
        assertEquals(5.0, evalValue("10 ÷ 2"), 0.0)
    }

    @Test
    fun `unicode minus`() {
        assertEquals(2.0, evalValue("5 − 3"), 0.0)
    }

    // === Edge Cases ===

    @Test
    fun `very large number`() {
        val result = evalValue("999999999999")
        assertEquals(999999999999.0, result, 0.0)
    }

    @Test
    fun `very small decimal`() {
        assertApproxEquals(0.0001, evalValue("0.0001"))
    }

    @Test
    fun `negative power`() {
        assertEquals(0.5, evalValue("2^-1"), 0.0)
    }

    @Test
    fun `fractional power`() {
        // 4^0.5 = sqrt(4) = 2
        assertEquals(2.0, evalValue("4^0.5"), 0.0)
    }

    @Test
    fun `zero to power zero`() {
        // Mathematically controversial, but Kotlin returns 1.0
        assertEquals(1.0, evalValue("0^0"), 0.0)
    }

    @Test
    fun `multiple percentages in chain`() {
        // 100 + 10% + 5% = 110 + 5% of 110 = 115.5
        assertEquals(115.5, evalValue("100 + 10% + 5%"), 0.0)
    }
}
