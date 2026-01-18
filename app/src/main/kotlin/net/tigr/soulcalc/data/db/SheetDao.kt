/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.tigr.soulcalc.data.model.SheetEntity

/**
 * Data Access Object for sheet operations.
 */
@Dao
interface SheetDao {

    @Query("SELECT * FROM sheets WHERE id = :id")
    suspend fun getById(id: String): SheetEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM sheets WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("SELECT * FROM sheets ORDER BY updatedAt DESC")
    fun getAllSheets(): Flow<List<SheetEntity>>

    @Query("SELECT * FROM sheets ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getMostRecentSheet(): SheetEntity?

    @Insert
    suspend fun insert(sheet: SheetEntity)

    @Update
    suspend fun update(sheet: SheetEntity)

    @Query("DELETE FROM sheets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE sheets SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sheets SET focusedLineIndex = :focusedLineIndex, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateFocusedLineIndex(id: String, focusedLineIndex: Int, timestamp: Long = System.currentTimeMillis())
}
