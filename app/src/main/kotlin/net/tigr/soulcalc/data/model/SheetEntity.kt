/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a calculator sheet.
 */
@Entity(tableName = "sheets")
data class SheetEntity(
    @PrimaryKey
    val id: String,

    val name: String = "",

    val focusedLineIndex: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
