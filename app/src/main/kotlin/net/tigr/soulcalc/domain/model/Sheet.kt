/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.domain.model

import java.util.UUID

/**
 * Represents a calculator sheet containing multiple lines.
 *
 * @property id Unique identifier
 * @property name Optional display name
 * @property lines Ordered list of lines
 * @property focusedLineIndex Index of the focused line (cursor position), restored on app restart
 * @property createdAt Creation timestamp (epoch millis)
 * @property updatedAt Last modification timestamp (epoch millis)
 */
data class Sheet(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val lines: List<Line> = emptyList(),
    val focusedLineIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
