/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.parser

/**
 * Represents a token produced by the lexer.
 *
 * @property type The type of token
 * @property value The string value of the token
 * @property position The starting position in the input string
 */
data class Token(
    val type: TokenType,
    val value: String,
    val position: Int
)

/**
 * Enumeration of all token types recognized by the lexer.
 */
enum class TokenType {
    // Literals
    NUMBER,         // 123, 45.67

    // Operators
    PLUS,           // +
    MINUS,          // - or −
    MULTIPLY,       // * or ×
    DIVIDE,         // / or ÷
    PERCENT,        // %
    POWER,          // ^
    EQUALS,         // =

    // Delimiters
    LPAREN,         // (
    RPAREN,         // )

    // Identifiers
    VARIABLE,       // $name (named variable)
    LINE_REF,       // $1, $2 (line reference)

    // Functions
    SQRT,           // sqrt

    // Special
    EOF,            // End of input
    ERROR           // Invalid token
}
