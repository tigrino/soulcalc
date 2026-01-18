/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.model

/**
 * Represents a single line in the calculator sheet.
 *
 * @property id Unique identifier for the line
 * @property position Order in the sheet (0-indexed)
 * @property input Raw user input text
 * @property result Evaluation result
 */
data class Line(
    val id: Int,
    val position: Int,
    val input: String,
    val result: Result = Result.Empty
)
