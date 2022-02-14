/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.sqlite.db.framework

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@LargeTest
class OpenHelperRecoveryTest {

    private val dbName = "test.db"
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun writeOver() {
        val openHelper = FrameworkSQLiteOpenHelper(context, dbName, EmptyCallback(), false, false)
        openHelper.writableDatabase.use { db ->
            db.execSQL("CREATE TABLE Foo (id INTEGER PRIMARY KEY)")
            db.query("SELECT * FROM sqlite_master WHERE name = 'Foo'").use {
                assertThat(it.count).isEqualTo(1)
            }
        }

        val dbFile = context.getDatabasePath(dbName)
        assertThat(dbFile.exists()).isTrue()
        assertThat(dbFile.length()).isGreaterThan(0)
        dbFile.writeText("malas vibra")

        try {
            openHelper.writableDatabase
            fail("Database should have failed to open.")
        } catch (ex: SQLiteException) {
            // Expected
        }
    }

    @Test
    fun writeOver_allowDataLossOnRecovery() {
        val openHelper = FrameworkSQLiteOpenHelper(context, dbName, EmptyCallback(), false, true)
        openHelper.writableDatabase.use { db ->
            db.execSQL("CREATE TABLE Foo (id INTEGER PRIMARY KEY)")
            db.query("SELECT * FROM sqlite_master WHERE name = 'Foo'").use {
                assertThat(it.count).isEqualTo(1)
            }
        }

        val dbFile = context.getDatabasePath(dbName)
        assertThat(dbFile.exists()).isTrue()
        assertThat(dbFile.length()).isGreaterThan(0)
        dbFile.writeText("malas vibra")

        openHelper.writableDatabase.use { db ->
            db.query("SELECT * FROM sqlite_master WHERE name = 'Foo'").use {
                assertThat(it.count).isEqualTo(0)
            }
        }
    }

    @Test
    fun allowDataLossOnRecovery_onCreateError() {
        var createAttempts = 0
        val badCallback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                if (createAttempts++ < 2) {
                    throw RuntimeException("Not an SQLiteException")
                }
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        val openHelper = FrameworkSQLiteOpenHelper(context, dbName, badCallback, false, true)
        try {
            openHelper.writableDatabase
            fail("Database should have failed to open.")
        } catch (ex: RuntimeException) {
            // Expected
            assertThat(ex.message).contains("Not an SQLiteException")
        }
        assertThat(createAttempts).isEqualTo(2)
    }

    @Test
    fun allowDataLossOnRecovery_onUpgradeError() {
        // Create DB at version 1, open and close it
        FrameworkSQLiteOpenHelper(context, dbName, EmptyCallback(1), false, true).let {
            it.writableDatabase.close()
        }

        // A callback to open DB at version 2, it has a bad migration.
        val badCallback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {}
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                db.execSQL("SELECT * FROM bad_table")
            }
        }
        val openHelper = FrameworkSQLiteOpenHelper(context, dbName, badCallback, false, true)
        try {
            openHelper.writableDatabase
            fail("Database should have failed to open.")
        } catch (ex: SQLiteException) {
            // Expected
            assertThat(ex.message).contains("no such table: bad_table")
        }
    }

    @Test
    fun allowDataLossOnRecovery_onOpenNonSQLiteError() {
        var openAttempts = 0
        val badCallback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {}
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            override fun onOpen(db: SupportSQLiteDatabase) {
                if (openAttempts++ < 2) {
                    throw RuntimeException("Not an SQLiteException")
                }
            }
        }
        val openHelper = FrameworkSQLiteOpenHelper(context, dbName, badCallback, false, true)
        try {
            openHelper.writableDatabase
            fail("Database should have failed to open.")
        } catch (ex: RuntimeException) {
            // Expected
            assertThat(ex.message).contains("Not an SQLiteException")
        }
        assertThat(openAttempts).isEqualTo(2)
    }

    @Test
    fun allowDataLossOnRecovery_onOpenSQLiteError_intermediate() {
        FrameworkSQLiteOpenHelper(context, dbName, EmptyCallback(), false, false)
            .writableDatabase.use { db ->
                db.execSQL("CREATE TABLE Foo (id INTEGER PRIMARY KEY)")
            }

        var openAttempts = 0
        val badCallback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {}
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            override fun onOpen(db: SupportSQLiteDatabase) {
                if (openAttempts++ < 1) {
                    db.execSQL("SELECT * FROM bad_table")
                }
            }
        }
        // With only 1 onOpen error, the database is opened without being deleted, simulates an
        // intermediate error.
        val openHelper = FrameworkSQLiteOpenHelper(context, dbName, badCallback, false, true)
        openHelper.writableDatabase.use { db ->
            db.query("SELECT * FROM sqlite_master WHERE name = 'Foo'").use {
                assertThat(it.count).isEqualTo(1)
            }
        }
        assertThat(openAttempts).isEqualTo(2)
    }

    @Test
    fun allowDataLossOnRecovery_onOpenSQLiteError_recoverable() {
        FrameworkSQLiteOpenHelper(context, dbName, EmptyCallback(), false, false)
            .writableDatabase.use { db ->
                db.execSQL("CREATE TABLE Foo (id INTEGER PRIMARY KEY)")
            }

        var openAttempts = 0
        val badCallback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {}
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            override fun onOpen(db: SupportSQLiteDatabase) {
                if (openAttempts++ < 2) {
                    db.execSQL("SELECT * FROM bad_table")
                }
            }
        }
        // With 2 onOpen error, the database is opened by deleting it, simulating a recoverable
        // error.
        val openHelper = FrameworkSQLiteOpenHelper(context, dbName, badCallback, false, true)
        openHelper.writableDatabase.use { db ->
            db.query("SELECT * FROM sqlite_master WHERE name = 'Foo'").use {
                assertThat(it.count).isEqualTo(0)
            }
        }
        assertThat(openAttempts).isEqualTo(3)
    }

    @Test
    fun allowDataLossOnRecovery_onOpenSQLiteError_permanent() {
        var openAttempts = 0
        val badCallback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {}
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            override fun onOpen(db: SupportSQLiteDatabase) {
                openAttempts++
                db.execSQL("SELECT * FROM bad_table")
            }
        }
        // Consistent onOpen error, might be a user bug or an actual SQLite permanent error,
        // nothing we can do here, expect failure
        val openHelper = FrameworkSQLiteOpenHelper(context, dbName, badCallback, false, true)
        try {
            openHelper.writableDatabase
            fail("Database should have failed to open.")
        } catch (ex: SQLiteException) {
            // Expected
            assertThat(ex.message).contains("no such table: bad_table")
        }
        assertThat(openAttempts).isEqualTo(3)
    }

    class EmptyCallback(version: Int = 1) : SupportSQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SupportSQLiteDatabase) {
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
        }

        override fun onCorruption(db: SupportSQLiteDatabase) {
        }
    }
}