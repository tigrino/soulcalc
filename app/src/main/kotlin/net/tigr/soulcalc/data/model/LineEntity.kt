/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single line in a sheet.
 */
@Entity(
    tableName = "lines",
    foreignKeys = [
        ForeignKey(
            entity = SheetEntity::class,
            parentColumns = ["id"],
            childColumns = ["sheetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sheetId")]
)
data class LineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sheetId: String,

    val position: Int,

    val input: String
)
