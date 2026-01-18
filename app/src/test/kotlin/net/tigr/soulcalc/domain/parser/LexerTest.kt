/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class LexerTest {

    private fun tokenize(input: String): List<Token> = Lexer(input).tokenize()

    private fun types(input: String): List<TokenType> = tokenize(input).map { it.type }

    private fun values(input: String): List<String> = tokenize(input).map { it.value }

    @Test
    fun `single integer number`() {
        val tokens = tokenize("42")
        assertEquals(listOf(TokenType.NUMBER, TokenType.EOF), types("42"))
        assertEquals("42", tokens[0].value)
    }

    @Test
    fun `decimal number`() {
        val tokens = tokenize("3.14")
        assertEquals(listOf(TokenType.NUMBER, TokenType.EOF), types("3.14"))
        assertEquals("3.14", tokens[0].value)
    }

    @Test
    fun `number starting with decimal`() {
        val tokens = tokenize(".5")
        assertEquals(listOf(TokenType.NUMBER, TokenType.EOF), types(".5"))
        assertEquals(".5", tokens[0].value)
    }

    @Test
    fun `simple addition`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.PLUS, TokenType.NUMBER, TokenType.EOF),
            types("1 + 2")
        )
    }

    @Test
    fun `simple subtraction`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.MINUS, TokenType.NUMBER, TokenType.EOF),
            types("5 - 3")
        )
    }

    @Test
    fun `simple multiplication`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.MULTIPLY, TokenType.NUMBER, TokenType.EOF),
            types("4 * 3")
        )
    }

    @Test
    fun `simple division`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.DIVIDE, TokenType.NUMBER, TokenType.EOF),
            types("10 / 2")
        )
    }

    @Test
    fun `all basic operators`() {
        assertEquals(
            listOf(
                TokenType.NUMBER, TokenType.PLUS,
                TokenType.NUMBER, TokenType.MINUS,
                TokenType.NUMBER, TokenType.MULTIPLY,
                TokenType.NUMBER, TokenType.DIVIDE,
                TokenType.NUMBER, TokenType.POWER,
                TokenType.NUMBER, TokenType.PERCENT,
                TokenType.EOF
            ),
            types("1+2-3*4/5^6%")
        )
    }

    @Test
    fun `unicode minus operator`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.MINUS, TokenType.NUMBER, TokenType.EOF),
            types("5−3") // Using Unicode minus −
        )
    }

    @Test
    fun `unicode multiply operator`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.MULTIPLY, TokenType.NUMBER, TokenType.EOF),
            types("4×3") // Using Unicode ×
        )
    }

    @Test
    fun `unicode divide operator`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.DIVIDE, TokenType.NUMBER, TokenType.EOF),
            types("10÷2") // Using Unicode ÷
        )
    }

    @Test
    fun `all unicode operators`() {
        assertEquals(
            listOf(
                TokenType.NUMBER, TokenType.MULTIPLY,
                TokenType.NUMBER, TokenType.DIVIDE,
                TokenType.NUMBER, TokenType.MINUS,
                TokenType.NUMBER, TokenType.EOF
            ),
            types("1×2÷3−4")
        )
    }

    @Test
    fun `named variable`() {
        val tokens = tokenize("\$tax")
        assertEquals(listOf(TokenType.VARIABLE, TokenType.EOF), types("\$tax"))
        assertEquals("tax", tokens[0].value)
    }

    @Test
    fun `variable with underscore`() {
        val tokens = tokenize("\$tax_rate")
        assertEquals(listOf(TokenType.VARIABLE, TokenType.EOF), types("\$tax_rate"))
        assertEquals("tax_rate", tokens[0].value)
    }

    @Test
    fun `variable with digits`() {
        val tokens = tokenize("\$item1")
        assertEquals(listOf(TokenType.VARIABLE, TokenType.EOF), types("\$item1"))
        assertEquals("item1", tokens[0].value)
    }

    @Test
    fun `line reference single digit`() {
        val tokens = tokenize("\$1")
        assertEquals(listOf(TokenType.LINE_REF, TokenType.EOF), types("\$1"))
        assertEquals("1", tokens[0].value)
    }

    @Test
    fun `line reference multi digit`() {
        val tokens = tokenize("\$123")
        assertEquals(listOf(TokenType.LINE_REF, TokenType.EOF), types("\$123"))
        assertEquals("123", tokens[0].value)
    }

    @Test
    fun `assignment expression`() {
        assertEquals(
            listOf(TokenType.VARIABLE, TokenType.EQUALS, TokenType.NUMBER, TokenType.EOF),
            types("\$x = 5")
        )
    }

    @Test
    fun `sqrt function`() {
        assertEquals(
            listOf(TokenType.SQRT, TokenType.LPAREN, TokenType.NUMBER, TokenType.RPAREN, TokenType.EOF),
            types("sqrt(4)")
        )
    }

    @Test
    fun `sqrt case insensitive`() {
        assertEquals(
            listOf(TokenType.SQRT, TokenType.LPAREN, TokenType.NUMBER, TokenType.RPAREN, TokenType.EOF),
            types("SQRT(4)")
        )
    }

    @Test
    fun `parentheses`() {
        assertEquals(
            listOf(
                TokenType.LPAREN, TokenType.NUMBER, TokenType.PLUS, TokenType.NUMBER,
                TokenType.RPAREN, TokenType.EOF
            ),
            types("(1+2)")
        )
    }

    @Test
    fun `complex expression with variables`() {
        assertEquals(
            listOf(TokenType.VARIABLE, TokenType.MULTIPLY, TokenType.VARIABLE, TokenType.EOF),
            types("\$price × \$qty")
        )
    }

    @Test
    fun `complex expression with line refs`() {
        assertEquals(
            listOf(TokenType.LINE_REF, TokenType.PLUS, TokenType.LINE_REF, TokenType.EOF),
            types("\$1 + \$2")
        )
    }

    @Test
    fun `invalid character`() {
        val tokens = tokenize("@")
        assertEquals(TokenType.ERROR, tokens[0].type)
        assertEquals("@", tokens[0].value)
    }

    @Test
    fun `unknown identifier`() {
        val tokens = tokenize("sin")
        assertEquals(TokenType.ERROR, tokens[0].type)
        assertEquals("sin", tokens[0].value)
    }

    @Test
    fun `empty input`() {
        assertEquals(listOf(TokenType.EOF), types(""))
    }

    @Test
    fun `whitespace only`() {
        assertEquals(listOf(TokenType.EOF), types("   "))
    }

    @Test
    fun `tabs and spaces`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.PLUS, TokenType.NUMBER, TokenType.EOF),
            types("  1  \t +  \t 2  ")
        )
    }

    @Test
    fun `power operator`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.POWER, TokenType.NUMBER, TokenType.EOF),
            types("2^8")
        )
    }

    @Test
    fun `percent operator`() {
        assertEquals(
            listOf(TokenType.NUMBER, TokenType.PERCENT, TokenType.EOF),
            types("10%")
        )
    }

    @Test
    fun `dollar sign alone is error`() {
        val tokens = tokenize("$")
        assertEquals(TokenType.ERROR, tokens[0].type)
    }

    @Test
    fun `preserves token positions`() {
        val tokens = tokenize("10 + 20")
        assertEquals(0, tokens[0].position)  // "10" at position 0
        assertEquals(3, tokens[1].position)  // "+" at position 3
        assertEquals(5, tokens[2].position)  // "20" at position 5
    }

    @Test
    fun `full realistic expression`() {
        val input = "\$price = 100"
        assertEquals(
            listOf(TokenType.VARIABLE, TokenType.EQUALS, TokenType.NUMBER, TokenType.EOF),
            types(input)
        )
        val tokens = tokenize(input)
        assertEquals("price", tokens[0].value)
        assertEquals("=", tokens[1].value)
        assertEquals("100", tokens[2].value)
    }

    @Test
    fun `expression with percentage addition`() {
        assertEquals(
            listOf(
                TokenType.NUMBER, TokenType.PLUS, TokenType.NUMBER, TokenType.PERCENT, TokenType.EOF
            ),
            types("100 + 10%")
        )
    }

    @Test
    fun `nested parentheses with sqrt`() {
        assertEquals(
            listOf(
                TokenType.SQRT, TokenType.LPAREN,
                TokenType.LPAREN, TokenType.NUMBER, TokenType.PLUS, TokenType.NUMBER, TokenType.RPAREN,
                TokenType.RPAREN, TokenType.EOF
            ),
            types("sqrt((1+3))")
        )
    }
}
