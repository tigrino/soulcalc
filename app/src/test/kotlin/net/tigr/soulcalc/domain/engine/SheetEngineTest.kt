/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.engine

import net.tigr.soulcalc.domain.model.Result
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SheetEngineTest {

    private lateinit var engine: SheetEngine

    @Before
    fun setUp() {
        engine = SheetEngine()
    }

    private fun resultValue(result: Result): Double {
        assertTrue("Expected Success but got $result", result is Result.Success)
        return (result as Result.Success).value
    }

    private fun resultError(result: Result): String {
        assertTrue("Expected Error but got $result", result is Result.Error)
        return (result as Result.Error).message
    }

    // === Basic Evaluation ===

    @Test
    fun `single number line`() {
        val lines = engine.evaluate(listOf("100"))
        assertEquals(1, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
    }

    @Test
    fun `multiple number lines`() {
        val lines = engine.evaluate(listOf("100", "200", "300"))
        assertEquals(3, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
        assertEquals(200.0, resultValue(lines[1].result), 0.0)
        assertEquals(300.0, resultValue(lines[2].result), 0.0)
    }

    @Test
    fun `expression on line`() {
        val lines = engine.evaluate(listOf("50 + 50"))
        assertEquals(1, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
    }

    @Test
    fun `empty line returns Empty`() {
        val lines = engine.evaluate(listOf(""))
        assertEquals(1, lines.size)
        assertTrue(lines[0].result is Result.Empty)
    }

    @Test
    fun `comment line returns Empty`() {
        val lines = engine.evaluate(listOf("# this is a comment"))
        assertEquals(1, lines.size)
        assertTrue(lines[0].result is Result.Empty)
    }

    @Test
    fun `whitespace only line returns Empty`() {
        val lines = engine.evaluate(listOf("   "))
        assertEquals(1, lines.size)
        assertTrue(lines[0].result is Result.Empty)
    }

    // === Variable Scoping (FR-10) ===

    @Test
    fun `variable defined then used`() {
        val lines = engine.evaluate(listOf(
            "\$x = 10",
            "\$x + 5"
        ))
        assertEquals(2, lines.size)
        assertEquals(10.0, resultValue(lines[0].result), 0.0)
        assertEquals(15.0, resultValue(lines[1].result), 0.0)
    }

    @Test
    fun `variable redefinition updates value`() {
        val lines = engine.evaluate(listOf(
            "\$x = 10",
            "\$x = 20",
            "\$x"
        ))
        assertEquals(3, lines.size)
        assertEquals(10.0, resultValue(lines[0].result), 0.0)
        assertEquals(20.0, resultValue(lines[1].result), 0.0)
        assertEquals(20.0, resultValue(lines[2].result), 0.0)
    }

    @Test
    fun `variable used before definition is error`() {
        val lines = engine.evaluate(listOf(
            "\$x + 5",
            "\$x = 10"
        ))
        assertEquals(2, lines.size)
        assertTrue(lines[0].result is Result.Error)
        assertEquals(10.0, resultValue(lines[1].result), 0.0)
    }

    @Test
    fun `multiple variables in scope`() {
        val lines = engine.evaluate(listOf(
            "\$a = 10",
            "\$b = 20",
            "\$a + \$b"
        ))
        assertEquals(3, lines.size)
        assertEquals(10.0, resultValue(lines[0].result), 0.0)
        assertEquals(20.0, resultValue(lines[1].result), 0.0)
        assertEquals(30.0, resultValue(lines[2].result), 0.0)
    }

    @Test
    fun `variable used in expression`() {
        val lines = engine.evaluate(listOf(
            "\$price = 100",
            "\$tax = 0.08",
            "\$price * \$tax"
        ))
        assertEquals(3, lines.size)
        assertEquals(8.0, resultValue(lines[2].result), 0.0)
    }

    // === Line References (FR-08) ===

    @Test
    fun `simple line reference`() {
        val lines = engine.evaluate(listOf(
            "100",
            "\$1 * 2"
        ))
        assertEquals(2, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
        assertEquals(200.0, resultValue(lines[1].result), 0.0)
    }

    @Test
    fun `multiple line references`() {
        val lines = engine.evaluate(listOf(
            "100",
            "50",
            "\$1 + \$2"
        ))
        assertEquals(3, lines.size)
        assertEquals(150.0, resultValue(lines[2].result), 0.0)
    }

    @Test
    fun `forward line reference is error`() {
        val lines = engine.evaluate(listOf(
            "\$2",
            "100"
        ))
        assertEquals(2, lines.size)
        assertTrue(lines[0].result is Result.Error)
        assertEquals(100.0, resultValue(lines[1].result), 0.0)
    }

    @Test
    fun `line reference to non-existent line is error`() {
        val lines = engine.evaluate(listOf(
            "100",
            "\$99"
        ))
        assertEquals(2, lines.size)
        assertTrue(lines[1].result is Result.Error)
    }

    @Test
    fun `line reference chain`() {
        val lines = engine.evaluate(listOf(
            "10",
            "\$1 * 2",    // 20
            "\$2 * 2",    // 40
            "\$3 * 2"     // 80
        ))
        assertEquals(4, lines.size)
        assertEquals(10.0, resultValue(lines[0].result), 0.0)
        assertEquals(20.0, resultValue(lines[1].result), 0.0)
        assertEquals(40.0, resultValue(lines[2].result), 0.0)
        assertEquals(80.0, resultValue(lines[3].result), 0.0)
    }

    // === Cascade Recalculation (FR-09) ===

    @Test
    fun `update triggers cascade`() {
        // Initial evaluation
        engine.evaluate(listOf(
            "100",
            "\$1 * 2",
            "\$2 + 50"
        ))

        // Update first line
        val lines = engine.updateLine(0, "200")

        assertEquals(3, lines.size)
        assertEquals(200.0, resultValue(lines[0].result), 0.0)
        assertEquals(400.0, resultValue(lines[1].result), 0.0)
        assertEquals(450.0, resultValue(lines[2].result), 0.0)
    }

    @Test
    fun `update middle line cascades`() {
        engine.evaluate(listOf(
            "100",
            "\$1 + 50",
            "\$2 * 2"
        ))

        // Update second line to not depend on first
        val lines = engine.updateLine(1, "1000")

        assertEquals(3, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
        assertEquals(1000.0, resultValue(lines[1].result), 0.0)
        assertEquals(2000.0, resultValue(lines[2].result), 0.0)
    }

    @Test
    fun `variable update cascades`() {
        engine.evaluate(listOf(
            "\$rate = 0.05",
            "1000 * \$rate"
        ))

        val lines = engine.updateLine(0, "\$rate = 0.10")

        assertEquals(2, lines.size)
        assertEquals(0.10, resultValue(lines[0].result), 0.0)
        assertEquals(100.0, resultValue(lines[1].result), 0.0)
    }

    // === Mixed Content ===

    @Test
    fun `realistic budget example`() {
        val lines = engine.evaluate(listOf(
            "# Monthly budget",
            "\$income = 5000",
            "\$rent = 1500",
            "\$utilities = 200",
            "\$income - \$rent - \$utilities"
        ))

        assertEquals(5, lines.size)
        assertTrue(lines[0].result is Result.Empty) // comment
        assertEquals(5000.0, resultValue(lines[1].result), 0.0)
        assertEquals(1500.0, resultValue(lines[2].result), 0.0)
        assertEquals(200.0, resultValue(lines[3].result), 0.0)
        assertEquals(3300.0, resultValue(lines[4].result), 0.0)
    }

    @Test
    fun `comments dont affect line numbers`() {
        val lines = engine.evaluate(listOf(
            "100",
            "# comment",
            "\$1 * 2"  // references line 1 (100), not the comment
        ))

        assertEquals(3, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
        assertTrue(lines[1].result is Result.Empty)
        assertEquals(200.0, resultValue(lines[2].result), 0.0)
    }

    @Test
    fun `empty lines dont break line references`() {
        val lines = engine.evaluate(listOf(
            "100",
            "",
            "\$1 + 50"
        ))

        assertEquals(3, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
        assertTrue(lines[1].result is Result.Empty)
        assertEquals(150.0, resultValue(lines[2].result), 0.0)
    }

    // === Line Operations ===

    @Test
    fun `append line`() {
        engine.evaluate(listOf("100"))
        val lines = engine.appendLine("\$1 * 2")

        assertEquals(2, lines.size)
        assertEquals(200.0, resultValue(lines[1].result), 0.0)
    }

    @Test
    fun `remove line recalculates`() {
        engine.evaluate(listOf(
            "100",
            "200",
            "\$1 + \$2"
        ))

        // Remove second line - now $2 refers to what was $3 (the sum line, which is now $2)
        // Actually, after removal, we have: 100, $1 + $2
        // $1 = 100, $2 is the line itself which creates forward reference error
        val lines = engine.removeLine(1)

        assertEquals(2, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
        // The expression $1 + $2 now fails because $2 doesn't exist yet
        assertTrue(lines[1].result is Result.Error)
    }

    @Test
    fun `insert line recalculates`() {
        engine.evaluate(listOf(
            "100",
            "\$1 * 2"
        ))

        // Insert at position 1
        val lines = engine.insertLine(1, "50")

        assertEquals(3, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
        assertEquals(50.0, resultValue(lines[1].result), 0.0)
        // $1 * 2 still works, references first line
        assertEquals(200.0, resultValue(lines[2].result), 0.0)
    }

    @Test
    fun `clear resets to single empty line`() {
        engine.evaluate(listOf("100", "200"))
        val lines = engine.clear()

        assertEquals(1, lines.size)
        assertEquals(1, engine.getLines().size)
        assertEquals("", lines[0].input)
        assertTrue(lines[0].result is Result.Empty)
    }

    // === Scope Access ===

    @Test
    fun `getScope returns current scope`() {
        engine.evaluate(listOf(
            "\$x = 10",
            "\$y = 20"
        ))

        val scope = engine.getScope()
        assertEquals(10.0, scope.resolveVariable("x"))
        assertEquals(20.0, scope.resolveVariable("y"))
    }

    @Test
    fun `getScope includes line results`() {
        engine.evaluate(listOf("100", "200"))

        val scope = engine.getScope()
        assertEquals(100.0, scope.resolveLineRef(1))
        assertEquals(200.0, scope.resolveLineRef(2))
    }

    // === Error Propagation ===

    @Test
    fun `error on one line doesnt break others`() {
        val lines = engine.evaluate(listOf(
            "100",
            "1 / 0",
            "200"
        ))

        assertEquals(3, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
        assertTrue(lines[1].result is Result.Error)
        assertEquals(200.0, resultValue(lines[2].result), 0.0)
    }

    @Test
    fun `error line result not in scope`() {
        val lines = engine.evaluate(listOf(
            "1 / 0",
            "\$1 + 10"
        ))

        assertEquals(2, lines.size)
        assertTrue(lines[0].result is Result.Error)
        assertTrue(lines[1].result is Result.Error) // Can't reference error line
    }

    // === Line Metadata ===

    @Test
    fun `lines have correct positions`() {
        val lines = engine.evaluate(listOf("a", "b", "c"))

        assertEquals(0, lines[0].position)
        assertEquals(1, lines[1].position)
        assertEquals(2, lines[2].position)
    }

    @Test
    fun `lines have correct ids`() {
        val lines = engine.evaluate(listOf("a", "b", "c"))

        assertEquals(0, lines[0].id)
        assertEquals(1, lines[1].id)
        assertEquals(2, lines[2].id)
    }

    @Test
    fun `lines preserve input`() {
        val lines = engine.evaluate(listOf("100 + 50", "\$x = 10"))

        assertEquals("100 + 50", lines[0].input)
        assertEquals("\$x = 10", lines[1].input)
    }

    // === Percentage in Multi-line ===

    @Test
    fun `percentage with variable base`() {
        val lines = engine.evaluate(listOf(
            "\$price = 100",
            "\$price + 10%"
        ))

        assertEquals(2, lines.size)
        assertEquals(100.0, resultValue(lines[0].result), 0.0)
        assertEquals(110.0, resultValue(lines[1].result), 0.0)
    }

    @Test
    fun `percentage with line reference base`() {
        val lines = engine.evaluate(listOf(
            "200",
            "\$1 - 25%"
        ))

        assertEquals(2, lines.size)
        assertEquals(200.0, resultValue(lines[0].result), 0.0)
        assertEquals(150.0, resultValue(lines[1].result), 0.0)
    }
}
