/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.tigr.soulcalc.data.db.LineDao
import net.tigr.soulcalc.data.db.SheetDao
import net.tigr.soulcalc.data.model.LineEntity
import net.tigr.soulcalc.data.model.SheetEntity
import net.tigr.soulcalc.domain.model.Line
import net.tigr.soulcalc.domain.model.Result
import net.tigr.soulcalc.domain.model.Sheet
import java.util.UUID

/**
 * Repository for sheet persistence operations.
 *
 * Handles mapping between domain models and database entities,
 * and provides auto-save functionality.
 */
class SheetRepository(
    private val sheetDao: SheetDao,
    private val lineDao: LineDao
) {

    /**
     * Loads the most recent sheet, or creates a new one if none exists.
     * Returns a default empty sheet if database operations fail.
     */
    suspend fun loadOrCreateSheet(): Sheet {
        return try {
            val sheetEntity = sheetDao.getMostRecentSheet()

            if (sheetEntity != null) {
                val lineEntities = lineDao.getLinesForSheet(sheetEntity.id)
                sheetEntity.toDomain(lineEntities)
            } else {
                val newSheet = Sheet(
                    id = UUID.randomUUID().toString(),
                    lines = listOf(Line(id = 0, position = 0, input = ""))
                )
                saveSheet(newSheet)
                newSheet
            }
        } catch (e: Exception) {
            // Return default sheet if database fails
            Sheet(
                id = UUID.randomUUID().toString(),
                lines = listOf(Line(id = 0, position = 0, input = ""))
            )
        }
    }

    /**
     * Loads a specific sheet by ID.
     */
    suspend fun loadSheet(id: String): Sheet? {
        val sheetEntity = sheetDao.getById(id) ?: return null
        val lineEntities = lineDao.getLinesForSheet(id)
        return sheetEntity.toDomain(lineEntities)
    }

    /**
     * Saves a sheet and all its lines.
     * Uses insert for new sheets or update for existing ones.
     */
    suspend fun saveSheet(sheet: Sheet) {
        val sheetEntity = sheet.toEntity()
        if (sheetDao.exists(sheet.id)) {
            sheetDao.update(sheetEntity)
        } else {
            sheetDao.insert(sheetEntity)
        }

        val lineEntities = sheet.lines.mapIndexed { index, line ->
            LineEntity(
                id = 0, // auto-generate
                sheetId = sheet.id,
                position = index,
                input = line.input
            )
        }
        lineDao.replaceAllForSheet(sheet.id, lineEntities)
    }

    /**
     * Saves only the lines of a sheet (for auto-save during editing).
     * Updates the sheet's timestamp.
     * Silently fails if database operations fail (non-critical auto-save).
     */
    suspend fun saveLines(sheetId: String, inputs: List<String>) {
        try {
            val lineEntities = inputs.mapIndexed { index, input ->
                LineEntity(
                    id = 0,
                    sheetId = sheetId,
                    position = index,
                    input = input
                )
            }
            lineDao.replaceAllForSheet(sheetId, lineEntities)
            sheetDao.updateTimestamp(sheetId)
        } catch (e: Exception) {
            android.util.Log.w("SheetRepository", "saveLines failed", e)
        }
    }

    /**
     * Observes lines for a sheet as a Flow.
     */
    fun observeLines(sheetId: String): Flow<List<String>> {
        return lineDao.observeLinesForSheet(sheetId).map { entities ->
            entities.sortedBy { it.position }.map { it.input }
        }
    }

    /**
     * Deletes a sheet and all its lines.
     */
    suspend fun deleteSheet(id: String) {
        sheetDao.deleteById(id)
    }

    /**
     * Clears all lines in a sheet.
     */
    suspend fun clearSheet(sheetId: String) {
        saveLines(sheetId, listOf(""))
    }

    /**
     * Updates the focused line index (cursor position) for a sheet.
     * Silently fails if database operations fail (non-critical auto-save).
     */
    suspend fun saveFocusedLineIndex(sheetId: String, focusedLineIndex: Int) {
        try {
            sheetDao.updateFocusedLineIndex(sheetId, focusedLineIndex)
        } catch (e: Exception) {
            android.util.Log.w("SheetRepository", "saveFocusedLineIndex failed", e)
        }
    }

    private fun SheetEntity.toDomain(lineEntities: List<LineEntity>): Sheet {
        val lines = lineEntities
            .sortedBy { it.position }
            .mapIndexed { index, entity ->
                Line(
                    id = index,
                    position = index,
                    input = entity.input,
                    result = Result.Empty
                )
            }

        return Sheet(
            id = this.id,
            name = this.name,
            lines = lines.ifEmpty { listOf(Line(id = 0, position = 0, input = "")) },
            focusedLineIndex = this.focusedLineIndex,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun Sheet.toEntity(): SheetEntity {
        return SheetEntity(
            id = this.id,
            name = this.name,
            focusedLineIndex = this.focusedLineIndex,
            createdAt = this.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}
