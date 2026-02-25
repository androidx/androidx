/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room3.integration.kotlintestapp.test

import android.content.Context
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@MediumTest
@RunWith(Parameterized::class)
class DatabaseCallbackTest(private val useDriver: UseDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    @Test
    fun createAndOpen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("test")

        with(TestDatabaseCallback()) {
            val db =
                Room.databaseBuilder<TestDatabase>(context, "test")
                    .setDriver(
                        when (useDriver) {
                            UseDriver.ANDROID -> AndroidSQLiteDriver()
                            UseDriver.BUNDLED -> BundledSQLiteDriver()
                        }
                    )
                    .addCallback(this)
                    .build()
            assertThat(created).isFalse()
            assertThat(opened).isFalse()

            db.booksDao().insertPublisher("p1", "pub1")
            assertThat(created).isTrue()
            assertThat(opened).isTrue()

            db.close()
        }

        with(TestDatabaseCallback()) {
            val db =
                Room.databaseBuilder<TestDatabase>(context, "test")
                    .setDriver(
                        when (useDriver) {
                            UseDriver.ANDROID -> AndroidSQLiteDriver()
                            UseDriver.BUNDLED -> BundledSQLiteDriver()
                        }
                    )
                    .addCallback(this)
                    .build()
            assertThat(created).isFalse()
            assertThat(opened).isFalse()
            assertThat(db.booksDao().getPublisher("p1").name).isEqualTo("pub1")
            assertThat(created).isFalse() // Not called; already created by first db instance
            assertThat(opened).isTrue()

            db.close()
        }
    }

    @Test
    fun writeOnCreate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db =
            Room.inMemoryDatabaseBuilder<TestDatabase>(context)
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override suspend fun onCreate(connection: SQLiteConnection) {
                            connection.execSQL(
                                "INSERT INTO publisher (publisherId, name) VALUES ('p1', 'pub1')"
                            )
                        }
                    }
                )
                .build()
        assertThat(db.booksDao().getPublisher("p1").name).isEqualTo("pub1")
        db.close()
    }

    @Test
    fun exceptionOnCreate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db =
            Room.inMemoryDatabaseBuilder<TestDatabase>(context)
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .addCallback(
                    object : RoomDatabase.Callback() {
                        var isBadInsertDone = false

                        override suspend fun onCreate(connection: SQLiteConnection) {
                            if (!isBadInsertDone) {
                                isBadInsertDone = true
                                connection.execSQL("INSERT INTO fake_table (c1) VALUES (1)")
                            }
                        }
                    }
                )
                .build()

        // Simulate user catching DB exceptions.
        assertThrows<SQLiteException> { db.booksDao().getAllBooks() }

        // Database should recover if on create was intermediate issue.
        assertThat(db.booksDao().getAllBooks()).isEmpty()

        db.close()
    }

    @Test
    fun closeConnectionOnException() {
        class CloseTrackingConnection(val delegate: SQLiteConnection) :
            SQLiteConnection by delegate {
            var isClosed = false

            override fun close() {
                delegate.close()
                isClosed = true
            }
        }
        val connections = mutableListOf<CloseTrackingConnection>()
        val actualDriver =
            when (useDriver) {
                UseDriver.ANDROID -> AndroidSQLiteDriver()
                UseDriver.BUNDLED -> BundledSQLiteDriver()
            }
        val driver =
            object : SQLiteDriver by actualDriver {
                override fun open(fileName: String): SQLiteConnection {
                    return CloseTrackingConnection(actualDriver.open(fileName)).also {
                        connections.add(it)
                    }
                }
            }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db =
            Room.inMemoryDatabaseBuilder<TestDatabase>(context)
                .setDriver(driver)
                .addCallback(
                    object : RoomDatabase.Callback() {

                        override suspend fun onCreate(connection: SQLiteConnection) {
                            connection.execSQL("INSERT INTO fake_table (c1) VALUES (1)")
                        }
                    }
                )
                .build()

        // Open database, catching callback error
        assertThrows<SQLiteException> { db.booksDao().getAllBooks() }
        // Check all opened connections were closed due to bad startup (no connection leaks)
        assertThat(connections.all { it.isClosed }).isTrue()

        db.close()
    }

    class TestDatabaseCallback : RoomDatabase.Callback() {
        var created = false
        var opened = false

        override suspend fun onCreate(connection: SQLiteConnection) {
            created = true
        }

        override suspend fun onOpen(connection: SQLiteConnection) {
            opened = true
        }
    }
}
