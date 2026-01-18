/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import net.tigr.soulcalc.data.model.LineEntity

/**
 * Data Access Object for line operations.
 */
@Dao
interface LineDao {

    @Query("SELECT * FROM lines WHERE sheetId = :sheetId ORDER BY position ASC")
    suspend fun getLinesForSheet(sheetId: String): List<LineEntity>

    @Query("SELECT * FROM lines WHERE sheetId = :sheetId ORDER BY position ASC")
    fun observeLinesForSheet(sheetId: String): Flow<List<LineEntity>>

    @Insert
    suspend fun insertAll(lines: List<LineEntity>)

    @Query("DELETE FROM lines WHERE sheetId = :sheetId")
    suspend fun deleteAllForSheet(sheetId: String)

    /**
     * Replaces all lines for a sheet atomically.
     */
    @Transaction
    suspend fun replaceAllForSheet(sheetId: String, lines: List<LineEntity>) {
        deleteAllForSheet(sheetId)
        insertAll(lines)
    }
}
