/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.parser

/**
 * Classifies input lines before parsing.
 *
 * This allows early detection of comments and empty lines,
 * avoiding unnecessary parsing overhead.
 */
object LineClassifier {

    /**
     * Types of lines that can be identified.
     */
    enum class LineType {
        /** Line starts with # - a comment, not evaluated */
        COMMENT,

        /** Line is empty or contains only whitespace */
        EMPTY,

        /** Line contains an expression to be parsed and evaluated */
        EXPRESSION
    }

    /**
     * Classifies the given input line.
     *
     * @param input The raw input string
     * @return The type of line
     */
    fun classify(input: String): LineType {
        val trimmed = input.trim()

        return when {
            trimmed.isEmpty() -> LineType.EMPTY
            trimmed.startsWith("#") -> LineType.COMMENT
            else -> LineType.EXPRESSION
        }
    }

    /**
     * Returns true if the line should be parsed and evaluated.
     */
    fun shouldEvaluate(input: String): Boolean {
        return classify(input) == LineType.EXPRESSION
    }

    /**
     * Returns true if the line is a comment.
     */
    fun isComment(input: String): Boolean {
        return classify(input) == LineType.COMMENT
    }

    /**
     * Returns true if the line is empty or whitespace-only.
     */
    fun isEmpty(input: String): Boolean {
        return classify(input) == LineType.EMPTY
    }
}
