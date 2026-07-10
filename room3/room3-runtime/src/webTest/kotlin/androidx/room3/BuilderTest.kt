/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3

import androidx.kruth.assertThat
import androidx.room3.Room.databaseBuilder
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import kotlin.test.Test

class BuilderTest {

    @Test
    fun defaultPoolConfiguration() {
        val truncateDb =
            databaseBuilder<TestDatabase>(
                    name = "test.db",
                    factory = { BuilderTest_TestDatabase_Impl() },
                )
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .setDriver(FakeDriver())
                .build()
        assertThat(truncateDb.getConfiguration().connectionPoolConfiguration)
            .isEqualTo(SingleConnection)
        truncateDb.close()

        val walDb =
            databaseBuilder<TestDatabase>(
                    name = "test.db",
                    factory = { BuilderTest_TestDatabase_Impl() },
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .setDriver(FakeDriver())
                .build()
        assertThat(walDb.getConfiguration().connectionPoolConfiguration).isEqualTo(SingleConnection)
        walDb.close()
    }

    @Test
    fun setMultiplePoolConnection() {
        val myDb =
            databaseBuilder<TestDatabase>(
                    name = "test.db",
                    factory = { BuilderTest_TestDatabase_Impl() },
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .setDriver(FakeDriver())
                .setMultipleConnectionPool(2, 1)
                .build()
        assertThat(myDb.getConfiguration().connectionPoolConfiguration)
            .isEqualTo(MultipleConnection(2, 1))
        myDb.close()
    }

    abstract class TestDatabase : RoomDatabase()

    class FakeDriver : SQLiteDriver {
        override suspend fun open(fileName: String): SQLiteConnection {
            error("Should not be called")
        }
    }
}
