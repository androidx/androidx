/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.RoomDatabase
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

abstract class BaseBuilderTest {
    abstract fun getRoomDatabaseBuilder(): RoomDatabase.Builder<SampleDatabase>

    @Test
    fun createOpenCallback() = runTest {
        var onCreateInvoked = 0
        var onOpenInvoked = 0
        val testCallback =
            object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    onCreateInvoked++
                }

                override fun onOpen(connection: SQLiteConnection) {
                    onOpenInvoked++
                }
            }

        val builder = getRoomDatabaseBuilder().addCallback(testCallback)

        val db1 = builder.build()

        // No callback invoked, Room opens the database lazily
        assertThat(onCreateInvoked).isEqualTo(0)
        assertThat(onOpenInvoked).isEqualTo(0)

        db1.dao().insertItem(1)

        // Database is created and opened
        assertThat(onCreateInvoked).isEqualTo(1)
        assertThat(onOpenInvoked).isEqualTo(1)

        db1.close()

        val db2 = builder.build()

        db2.dao().insertItem(2)

        // Database was already created, it is now only opened
        assertThat(onCreateInvoked).isEqualTo(1)
        assertThat(onOpenInvoked).isEqualTo(2)

        db2.close()
    }

    @Test
    fun onOpenExactlyOnce() = runTest {
        var onOpenInvoked = 0

        val onOpenBlocker = CompletableDeferred<Unit>()
        val database =
            getRoomDatabaseBuilder()
                .addCallback(
                    object : RoomDatabase.Callback() {
                        // This onOpen callback will block database initialization until the
                        // onOpenLatch is released.
                        override fun onOpen(connection: SQLiteConnection) {
                            onOpenInvoked++
                            runBlocking { onOpenBlocker.await() }
                        }
                    }
                )
                .build()

        // Start 4 concurrent coroutines that try to open the database and use its connections,
        // initialization should be done exactly once
        val launchBlockers = List(4) { CompletableDeferred<Unit>() }
        val jobs =
            List(4) { index ->
                launch(Dispatchers.IO) {
                    launchBlockers[index].complete(Unit)
                    database.useReaderConnection {}
                }
            }

        // Wait all launch coroutines to start then release the latch
        launchBlockers.awaitAll()
        delay(100) // A bit more waiting so useReaderConnection reaches the exclusive lock
        onOpenBlocker.complete(Unit)

        jobs.joinAll()
        database.close()

        // Initialization should be done exactly once
        assertThat(onOpenInvoked).isEqualTo(1)
    }

    @Test
    fun onOpenRecursive() = runTest {
        var database: SampleDatabase? = null
        database =
            getRoomDatabaseBuilder()
                .setQueryCoroutineContext(Dispatchers.Unconfined)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        // Use a bad open callback that will recursively try to open the database
                        // again, this is a user error.
                        override fun onOpen(connection: SQLiteConnection) {
                            runBlocking { checkNotNull(database).dao().getItemList() }
                        }
                    }
                )
                .build()
        assertThrows<IllegalStateException> { database.dao().getItemList() }
            .hasMessageThat()
            .contains("Recursive database initialization detected.")
        database.close()
    }

    @Test
    fun onConfigureConnections() = runTest {
        val database = getRoomDatabaseBuilder().build()
        // Validate that all connections are configured to be use by Room, in this case that they
        // all have foreign keys enables as that is a per-connection PRAGMA.
        val jobs =
            List(4) {
                launch(Dispatchers.IO) {
                    database.useReaderConnection { connection ->
                        connection.usePrepared("PRAGMA foreign_keys") {
                            assertThat(it.step()).isTrue() // SQLITE_ROW
                            assertThat(it.getBoolean(0)).isTrue()
                        }
                    }
                }
            } +
                launch(Dispatchers.IO) {
                    database.useWriterConnection { connection ->
                        connection.usePrepared("PRAGMA foreign_keys") {
                            assertThat(it.step()).isTrue() // SQLITE_ROW
                            assertThat(it.getBoolean(0)).isTrue()
                        }
                    }
                }
        jobs.joinAll()
        database.close()
    }

    @Test
    fun setCoroutineContextWithoutDispatcher() {
        assertThrows<IllegalArgumentException> {
                getRoomDatabaseBuilder().setQueryCoroutineContext(EmptyCoroutineContext)
            }
            .hasMessageThat()
            .contains("It is required that the coroutine context contain a dispatcher.")
    }

    @Test
    fun setJournalModeWal() = runTest {
        val database =
            getRoomDatabaseBuilder()
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()

        val journalMode =
            database.useReaderConnection { connection ->
                connection.usePrepared("PRAGMA journal_mode") {
                    it.step()
                    it.getText(0)
                }
            }
        assertThat(journalMode).isEqualTo("wal")

        val syncMode =
            database.useReaderConnection { connection ->
                connection.usePrepared("PRAGMA synchronous") {
                    it.step()
                    it.getInt(0)
                }
            }
        assertThat(syncMode).isEqualTo(1) // NORMAL mode

        database.close()
    }

    @Test
    fun setJournalModeTruncate() = runTest {
        val database =
            getRoomDatabaseBuilder().setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build()

        val journalMode =
            database.useReaderConnection { connection ->
                connection.usePrepared("PRAGMA journal_mode") {
                    it.step()
                    it.getText(0)
                }
            }
        assertThat(journalMode).isEqualTo("truncate")

        val syncMode =
            database.useReaderConnection { connection ->
                connection.usePrepared("PRAGMA synchronous") {
                    it.step()
                    it.getInt(0)
                }
            }
        assertThat(syncMode).isEqualTo(2) // FULL mode

        database.close()
    }

    @Test
    fun setCustomBusyTimeout() = runTest {
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
        val configuredBusyTimeout =
            database.useReaderConnection { connection ->
                connection.usePrepared("PRAGMA busy_timeout") {
                    it.step()
                    it.getLong(0)
                }
            }
        assertThat(configuredBusyTimeout).isEqualTo(customBusyTimeout)

        database.close()
    }
}
