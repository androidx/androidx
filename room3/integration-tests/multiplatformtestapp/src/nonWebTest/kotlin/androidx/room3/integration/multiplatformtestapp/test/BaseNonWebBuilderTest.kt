/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room3.RoomDatabase
import androidx.room3.useReaderConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BaseNonWebBuilderTest : BaseBuilderTest() {

    abstract fun getRoomDatabaseBuilder(fileName: String): RoomDatabase.Builder<SampleDatabase>

    abstract fun getInMemoryDatabaseBuilder(): RoomDatabase.Builder<SampleDatabase>

    abstract fun getDatabasePath(name: String): String

    abstract fun createCorruptedFile(path: String)

    abstract fun deleteFile(path: String)

    abstract fun fileExists(path: String): Boolean

    abstract fun readFileContent(path: String): String

    @Test
    fun setCustomBusyTimeout() = runTest {
        val tempDatabase = getRoomDatabaseBuilder().build()
        val defaultBusyTimeout = tempDatabase.useReaderConnection { connection ->
            connection.usePrepared("PRAGMA busy_timeout") {
                it.step()
                it.getLong(0)
            }
        }
        assertThat(defaultBusyTimeout).isGreaterThan(0)
        tempDatabase.close()

        val customBusyTimeout = 20000
        val actualDriver = BundledSQLiteDriver()
        val driverWrapper =
            object : SQLiteDriver by actualDriver {
                override fun open(fileName: String): SQLiteConnection {
                    return actualDriver.open(fileName).also { newConnection ->
                        newConnection.execSQL("PRAGMA busy_timeout = $customBusyTimeout")
                    }
                }
            }
        val database = getRoomDatabaseBuilder().setDriver(driverWrapper).build()
        val configuredBusyTimeout = database.useReaderConnection { connection ->
            connection.usePrepared("PRAGMA busy_timeout") {
                it.step()
                it.getLong(0)
            }
        }
        assertThat(configuredBusyTimeout).isEqualTo(customBusyTimeout)

        database.close()
    }

    // Validates that the type of connection created by the given driver are used in callbacks.
    @Test
    fun customConnectionsOnCallbacks() = runTest {
        class MyConnection(private val delegate: SQLiteConnection) : SQLiteConnection by delegate

        val bundledDriver = BundledSQLiteDriver()
        val db =
            getRoomDatabaseBuilder()
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override suspend fun onCreate(connection: SQLiteConnection) {
                            assertThat(connection).isInstanceOf<MyConnection>()
                        }

                        override suspend fun onOpen(connection: SQLiteConnection) {
                            assertThat(connection).isInstanceOf<MyConnection>()
                        }
                    }
                )
                .setDriver(
                    object : SQLiteDriver by bundledDriver {
                        override fun open(fileName: String): SQLiteConnection {
                            return MyConnection(bundledDriver.open(fileName))
                        }
                    }
                )
                .build()
        db.dao().insertItem(1)
        db.close()
    }

    @Test
    fun corruptedFile_dataLossOnRecoveryAllowed_recreatesDatabase() = runTest {
        val path = getDatabasePath("corrupted_recovery")
        try {
            createCorruptedFile(path)
            assertThat(fileExists(path)).isTrue()

            val db = getRoomDatabaseBuilder(path).allowDataLossOnRecovery(true).build()
            try {
                db.dao().insertItem(1)
                val items = db.dao().getItemList()
                assertThat(items).hasSize(1)
                assertThat(items.first().pk).isEqualTo(1)
            } finally {
                db.close()
            }
        } finally {
            deleteFile(path)
        }
    }

    @Test
    fun corruptedFile_dataLossOnRecoveryNotAllowed_throwsSQLiteException() = runTest {
        val path = getDatabasePath("corrupted_no_recovery")
        try {
            createCorruptedFile(path)
            assertThat(fileExists(path)).isTrue()
            val initialContent = readFileContent(path)

            val db = getRoomDatabaseBuilder(path).allowDataLossOnRecovery(false).build()
            try {
                assertThrows<SQLiteException> { db.dao().insertItem(1) }
                assertThat(fileExists(path)).isTrue()
                assertThat(readFileContent(path)).isEqualTo(initialContent)
            } finally {
                db.close()
            }
        } finally {
            deleteFile(path)
        }
    }
}
