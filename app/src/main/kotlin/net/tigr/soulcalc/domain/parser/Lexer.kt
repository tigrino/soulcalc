/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.parser

/**
 * Tokenizes input strings into a sequence of tokens for the parser.
 *
 * Handles:
 * - Numbers (integers and decimals)
 * - Operators (+, -, *, /, ^, %, = and Unicode variants ×, ÷, −)
 * - Parentheses
 * - Variables ($name)
 * - Line references ($1, $2, etc.)
 * - Functions (sqrt)
 */
class Lexer(private val input: String) {
    private var position: Int = 0

    private val currentChar: Char?
        get() = if (position < input.length) input[position] else null

    /**
     * Tokenizes the entire input and returns a list of tokens.
     * The list always ends with an EOF token.
     */
    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()

        while (currentChar != null) {
            val token = nextToken()
            tokens.add(token)
            if (token.type == TokenType.ERROR || token.type == TokenType.EOF) {
                break
            }
        }

        if (tokens.isEmpty() || tokens.last().type != TokenType.EOF) {
            tokens.add(Token(TokenType.EOF, "", position))
        }

        return tokens
    }

    private fun nextToken(): Token {
        skipWhitespace()

        val char = currentChar ?: return Token(TokenType.EOF, "", position)
        val startPos = position

        return when {
            char.isDigit() -> readNumber()
            char == '.' && peekNext()?.isDigit() == true -> readNumber()
            char == '$' -> readDollarToken()
            char == '+' -> singleCharToken(TokenType.PLUS)
            char == '-' || char == '−' -> singleCharToken(TokenType.MINUS)
            char == '*' || char == '×' -> singleCharToken(TokenType.MULTIPLY)
            char == '/' || char == '÷' -> singleCharToken(TokenType.DIVIDE)
            char == '%' -> singleCharToken(TokenType.PERCENT)
            char == '^' -> singleCharToken(TokenType.POWER)
            char == '=' -> singleCharToken(TokenType.EQUALS)
            char == '(' -> singleCharToken(TokenType.LPAREN)
            char == ')' -> singleCharToken(TokenType.RPAREN)
            char.isLetter() -> readIdentifier()
            else -> {
                position++
                Token(TokenType.ERROR, char.toString(), startPos)
            }
        }
    }

    private fun skipWhitespace() {
        while (currentChar?.isWhitespace() == true) {
            position++
        }
    }

    private fun peekNext(): Char? =
        if (position + 1 < input.length) input[position + 1] else null

    private fun singleCharToken(type: TokenType): Token {
        val startPos = position
        val char = currentChar!!
        position++
        return Token(type, char.toString(), startPos)
    }

    private fun readNumber(): Token {
        val startPos = position
        val sb = StringBuilder()
        var hasDecimal = false

        while (currentChar != null) {
            val char = currentChar!!
            when {
                char.isDigit() -> {
                    sb.append(char)
                    position++
                }
                char == '.' && !hasDecimal -> {
                    hasDecimal = true
                    sb.append(char)
                    position++
                }
                else -> break
            }
        }

        return Token(TokenType.NUMBER, sb.toString(), startPos)
    }

    private fun readDollarToken(): Token {
        val startPos = position
        position++ // skip '$'

        val char = currentChar

        return when {
            char == null -> Token(TokenType.ERROR, "$", startPos)
            char.isDigit() -> readLineRef(startPos)
            char.isLetter() -> readVariable(startPos)
            else -> Token(TokenType.ERROR, "$", startPos)
        }
    }

    private fun readLineRef(startPos: Int): Token {
        val sb = StringBuilder()
        while (currentChar?.isDigit() == true) {
            sb.append(currentChar)
            position++
        }
        return Token(TokenType.LINE_REF, sb.toString(), startPos)
    }

    private fun readVariable(startPos: Int): Token {
        val sb = StringBuilder()
        while (currentChar != null && (currentChar!!.isLetterOrDigit() || currentChar == '_')) {
            sb.append(currentChar)
            position++
        }
        return Token(TokenType.VARIABLE, sb.toString(), startPos)
    }

    private fun readIdentifier(): Token {
        val startPos = position
        val sb = StringBuilder()

        while (currentChar != null && (currentChar!!.isLetterOrDigit() || currentChar == '_')) {
            sb.append(currentChar)
            position++
        }

        val value = sb.toString()
        return when (value.lowercase()) {
            "sqrt" -> Token(TokenType.SQRT, value, startPos)
            else -> Token(TokenType.ERROR, value, startPos)
        }
    }
}
