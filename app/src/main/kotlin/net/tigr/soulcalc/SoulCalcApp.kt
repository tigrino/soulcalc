/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc

import android.app.Application
import net.tigr.soulcalc.data.db.AppDatabase
import net.tigr.soulcalc.data.preferences.ThemePreferences
import net.tigr.soulcalc.data.repository.SheetRepository

class SoulCalcApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val sheetRepository: SheetRepository by lazy {
        SheetRepository(
            sheetDao = database.sheetDao(),
            lineDao = database.lineDao()
        )
    }

    val themePreferences: ThemePreferences by lazy {
        ThemePreferences(this)
    }
}
