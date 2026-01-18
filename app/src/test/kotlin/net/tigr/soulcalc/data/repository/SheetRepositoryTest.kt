/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.data.repository

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.tigr.soulcalc.data.db.LineDao
import net.tigr.soulcalc.data.db.SheetDao
import net.tigr.soulcalc.data.model.LineEntity
import net.tigr.soulcalc.data.model.SheetEntity
import net.tigr.soulcalc.domain.model.Line
import net.tigr.soulcalc.domain.model.Sheet
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SheetRepositoryTest {

    private lateinit var repository: SheetRepository
    private lateinit var mockSheetDao: FakeSheetDao
    private lateinit var mockLineDao: FakeLineDao

    @Before
    fun setUp() {
        mockSheetDao = FakeSheetDao()
        mockLineDao = FakeLineDao()
        repository = SheetRepository(mockSheetDao, mockLineDao)
    }

    // === Load or Create Sheet ===

    @Test
    fun `loadOrCreateSheet creates new sheet when none exists`() = runTest {
        val sheet = repository.loadOrCreateSheet()

        assertNotNull(sheet)
        assertNotNull(sheet.id)
        assertEquals(1, sheet.lines.size)
        assertEquals("", sheet.lines[0].input)
        assertTrue(mockSheetDao.insertCalled)
    }

    @Test
    fun `loadOrCreateSheet loads existing sheet`() = runTest {
        val existingSheet = SheetEntity(
            id = "test-id",
            name = "Test Sheet",
            focusedLineIndex = 2
        )
        mockSheetDao.sheets["test-id"] = existingSheet
        mockSheetDao.mostRecent = existingSheet
        mockLineDao.linesBySheet["test-id"] = listOf(
            LineEntity(1, "test-id", 0, "100"),
            LineEntity(2, "test-id", 1, "200"),
            LineEntity(3, "test-id", 2, "300")
        )

        val sheet = repository.loadOrCreateSheet()

        assertEquals("test-id", sheet.id)
        assertEquals("Test Sheet", sheet.name)
        assertEquals(2, sheet.focusedLineIndex)
        assertEquals(3, sheet.lines.size)
        assertEquals("100", sheet.lines[0].input)
        assertEquals("200", sheet.lines[1].input)
        assertEquals("300", sheet.lines[2].input)
    }

    @Test
    fun `loadOrCreateSheet returns empty line when sheet has no lines`() = runTest {
        val existingSheet = SheetEntity(id = "test-id", name = "Empty")
        mockSheetDao.sheets["test-id"] = existingSheet
        mockSheetDao.mostRecent = existingSheet
        mockLineDao.linesBySheet["test-id"] = emptyList()

        val sheet = repository.loadOrCreateSheet()

        assertEquals(1, sheet.lines.size)
        assertEquals("", sheet.lines[0].input)
    }

    // === Load Sheet ===

    @Test
    fun `loadSheet returns null for non-existent sheet`() = runTest {
        val sheet = repository.loadSheet("non-existent")

        assertNull(sheet)
    }

    @Test
    fun `loadSheet returns sheet when exists`() = runTest {
        val existingSheet = SheetEntity(id = "test-id", name = "Test")
        mockSheetDao.sheets["test-id"] = existingSheet
        mockLineDao.linesBySheet["test-id"] = listOf(
            LineEntity(1, "test-id", 0, "42")
        )

        val sheet = repository.loadSheet("test-id")

        assertNotNull(sheet)
        assertEquals("test-id", sheet!!.id)
        assertEquals("42", sheet.lines[0].input)
    }

    // === Save Sheet ===

    @Test
    fun `saveSheet inserts new sheet`() = runTest {
        val sheet = Sheet(
            id = "new-id",
            name = "New Sheet",
            lines = listOf(Line(0, 0, "100"))
        )

        repository.saveSheet(sheet)

        assertTrue(mockSheetDao.insertCalled)
        assertFalse(mockSheetDao.updateCalled)
        assertTrue(mockLineDao.replaceAllCalled)
    }

    @Test
    fun `saveSheet updates existing sheet`() = runTest {
        mockSheetDao.existsIds.add("existing-id")
        val sheet = Sheet(
            id = "existing-id",
            name = "Updated Sheet",
            lines = listOf(Line(0, 0, "200"))
        )

        repository.saveSheet(sheet)

        assertFalse(mockSheetDao.insertCalled)
        assertTrue(mockSheetDao.updateCalled)
        assertTrue(mockLineDao.replaceAllCalled)
    }

    @Test
    fun `saveSheet saves lines correctly`() = runTest {
        val sheet = Sheet(
            id = "test-id",
            lines = listOf(
                Line(0, 0, "line 1"),
                Line(1, 1, "line 2"),
                Line(2, 2, "line 3")
            )
        )

        repository.saveSheet(sheet)

        assertEquals("test-id", mockLineDao.lastReplacedSheetId)
        assertEquals(3, mockLineDao.lastReplacedLines?.size)
    }

    // === Save Lines ===

    @Test
    fun `saveLines updates lines and timestamp`() = runTest {
        val inputs = listOf("a", "b", "c")

        repository.saveLines("sheet-id", inputs)

        assertTrue(mockLineDao.replaceAllCalled)
        assertEquals("sheet-id", mockLineDao.lastReplacedSheetId)
        assertEquals(3, mockLineDao.lastReplacedLines?.size)
        assertTrue(mockSheetDao.timestampUpdated)
    }

    // === Save Focused Line Index ===

    @Test
    fun `saveFocusedLineIndex updates focus position`() = runTest {
        repository.saveFocusedLineIndex("sheet-id", 5)

        assertEquals("sheet-id", mockSheetDao.lastFocusUpdateSheetId)
        assertEquals(5, mockSheetDao.lastFocusUpdateIndex)
    }

    // === Delete Sheet ===

    @Test
    fun `deleteSheet removes sheet`() = runTest {
        repository.deleteSheet("test-id")

        assertEquals("test-id", mockSheetDao.deletedId)
    }

    // === Clear Sheet ===

    @Test
    fun `clearSheet saves single empty line`() = runTest {
        repository.clearSheet("sheet-id")

        assertEquals("sheet-id", mockLineDao.lastReplacedSheetId)
        assertEquals(1, mockLineDao.lastReplacedLines?.size)
        assertEquals("", mockLineDao.lastReplacedLines?.get(0)?.input)
    }

    // === Observe Lines ===

    @Test
    fun `observeLines returns sorted inputs`() = runTest {
        mockLineDao.observeLines = flowOf(listOf(
            LineEntity(3, "sheet-id", 2, "third"),
            LineEntity(1, "sheet-id", 0, "first"),
            LineEntity(2, "sheet-id", 1, "second")
        ))

        val flow = repository.observeLines("sheet-id")
        var result: List<String>? = null
        flow.collect { result = it }

        assertEquals(listOf("first", "second", "third"), result)
    }
}

/**
 * Fake implementation of SheetDao for testing.
 */
private class FakeSheetDao : SheetDao {
    val sheets = mutableMapOf<String, SheetEntity>()
    val existsIds = mutableSetOf<String>()
    var mostRecent: SheetEntity? = null
    var insertCalled = false
    var updateCalled = false
    var timestampUpdated = false
    var deletedId: String? = null
    var lastFocusUpdateSheetId: String? = null
    var lastFocusUpdateIndex: Int? = null

    override suspend fun getById(id: String): SheetEntity? = sheets[id]

    override suspend fun exists(id: String): Boolean = existsIds.contains(id) || sheets.containsKey(id)

    override fun getAllSheets() = flowOf(sheets.values.toList())

    override suspend fun getMostRecentSheet(): SheetEntity? = mostRecent

    override suspend fun insert(sheet: SheetEntity) {
        insertCalled = true
        sheets[sheet.id] = sheet
    }

    override suspend fun update(sheet: SheetEntity) {
        updateCalled = true
        sheets[sheet.id] = sheet
    }

    override suspend fun deleteById(id: String) {
        deletedId = id
        sheets.remove(id)
    }

    override suspend fun updateTimestamp(id: String, timestamp: Long) {
        timestampUpdated = true
    }

    override suspend fun updateFocusedLineIndex(id: String, focusedLineIndex: Int, timestamp: Long) {
        lastFocusUpdateSheetId = id
        lastFocusUpdateIndex = focusedLineIndex
    }
}

/**
 * Fake implementation of LineDao for testing.
 */
private class FakeLineDao : LineDao {
    val linesBySheet = mutableMapOf<String, List<LineEntity>>()
    var replaceAllCalled = false
    var lastReplacedSheetId: String? = null
    var lastReplacedLines: List<LineEntity>? = null
    var observeLines = flowOf<List<LineEntity>>(emptyList())

    override suspend fun getLinesForSheet(sheetId: String): List<LineEntity> {
        return linesBySheet[sheetId] ?: emptyList()
    }

    override fun observeLinesForSheet(sheetId: String) = observeLines

    override suspend fun insertAll(lines: List<LineEntity>) {}

    override suspend fun deleteAllForSheet(sheetId: String) {}

    override suspend fun replaceAllForSheet(sheetId: String, lines: List<LineEntity>) {
        replaceAllCalled = true
        lastReplacedSheetId = sheetId
        lastReplacedLines = lines
    }
}
