/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Albert Zenkoff
 */

package net.tigr.soulcalc.data.db

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.tigr.soulcalc.data.model.LineEntity
import net.tigr.soulcalc.data.model.SheetEntity

/**
 * Room database for SoulCalc.
 */
@Database(
    entities = [SheetEntity::class, LineEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sheetDao(): SheetDao
    abstract fun lineDao(): LineDao

    companion object {
        private const val DATABASE_NAME = "soulcalc.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sheets ADD COLUMN focusedLineIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        /**
         * Clears the singleton instance. For testing only.
         */
        @VisibleForTesting
        fun clearInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
