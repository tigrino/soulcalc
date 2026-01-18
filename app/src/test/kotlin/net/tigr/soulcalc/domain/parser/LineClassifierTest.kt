/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.parser

import net.tigr.soulcalc.domain.parser.LineClassifier.LineType
import org.junit.Assert.*
import org.junit.Test

class LineClassifierTest {

    // === Comment Detection ===

    @Test
    fun `line starting with hash is comment`() {
        assertEquals(LineType.COMMENT, LineClassifier.classify("# comment"))
    }

    @Test
    fun `line starting with hash no space is comment`() {
        assertEquals(LineType.COMMENT, LineClassifier.classify("#comment"))
    }

    @Test
    fun `line with leading whitespace then hash is comment`() {
        assertEquals(LineType.COMMENT, LineClassifier.classify("  # indented comment"))
    }

    @Test
    fun `comment with numbers is still comment`() {
        assertEquals(LineType.COMMENT, LineClassifier.classify("# 100 + 200"))
    }

    @Test
    fun `isComment returns true for comments`() {
        assertTrue(LineClassifier.isComment("# test"))
    }

    @Test
    fun `isComment returns false for expressions`() {
        assertFalse(LineClassifier.isComment("100 + 50"))
    }

    // === Empty Line Detection ===

    @Test
    fun `empty string is empty`() {
        assertEquals(LineType.EMPTY, LineClassifier.classify(""))
    }

    @Test
    fun `spaces only is empty`() {
        assertEquals(LineType.EMPTY, LineClassifier.classify("   "))
    }

    @Test
    fun `tabs only is empty`() {
        assertEquals(LineType.EMPTY, LineClassifier.classify("\t\t"))
    }

    @Test
    fun `mixed whitespace is empty`() {
        assertEquals(LineType.EMPTY, LineClassifier.classify("  \t  \t  "))
    }

    @Test
    fun `newline is empty`() {
        assertEquals(LineType.EMPTY, LineClassifier.classify("\n"))
    }

    @Test
    fun `isEmpty returns true for empty`() {
        assertTrue(LineClassifier.isEmpty(""))
        assertTrue(LineClassifier.isEmpty("   "))
    }

    @Test
    fun `isEmpty returns false for expressions`() {
        assertFalse(LineClassifier.isEmpty("42"))
    }

    // === Expression Detection ===

    @Test
    fun `number is expression`() {
        assertEquals(LineType.EXPRESSION, LineClassifier.classify("42"))
    }

    @Test
    fun `simple expression is expression`() {
        assertEquals(LineType.EXPRESSION, LineClassifier.classify("100 + 50"))
    }

    @Test
    fun `variable assignment is expression`() {
        assertEquals(LineType.EXPRESSION, LineClassifier.classify("\$x = 5"))
    }

    @Test
    fun `line reference is expression`() {
        assertEquals(LineType.EXPRESSION, LineClassifier.classify("\$1 + \$2"))
    }

    @Test
    fun `expression with leading whitespace`() {
        assertEquals(LineType.EXPRESSION, LineClassifier.classify("  100 + 50"))
    }

    @Test
    fun `shouldEvaluate returns true for expressions`() {
        assertTrue(LineClassifier.shouldEvaluate("100 + 50"))
        assertTrue(LineClassifier.shouldEvaluate("\$x = 10"))
    }

    @Test
    fun `shouldEvaluate returns false for comments`() {
        assertFalse(LineClassifier.shouldEvaluate("# comment"))
    }

    @Test
    fun `shouldEvaluate returns false for empty`() {
        assertFalse(LineClassifier.shouldEvaluate(""))
        assertFalse(LineClassifier.shouldEvaluate("   "))
    }

    // === Edge Cases ===

    @Test
    fun `hash in middle of expression is expression`() {
        // This would be a syntax error, but classifier doesn't validate
        assertEquals(LineType.EXPRESSION, LineClassifier.classify("100 # not a comment"))
    }

    @Test
    fun `dollar sign alone is expression`() {
        assertEquals(LineType.EXPRESSION, LineClassifier.classify("$"))
    }

    @Test
    fun `single character is expression`() {
        assertEquals(LineType.EXPRESSION, LineClassifier.classify("x"))
    }
}
