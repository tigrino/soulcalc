/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.model

/**
 * Represents the result of evaluating a single line.
 */
sealed class Result {
    /** Successfully evaluated to a numeric value. */
    data class Success(val value: Double) : Result()

    /** Evaluation failed with an error. */
    data class Error(val message: String) : Result()

    /** Line is empty or a comment - no result to display. */
    data object Empty : Result()
}
