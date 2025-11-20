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

package androidx.room3.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room3.ExperimentalRoomApi
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.immediateTransaction
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class InMemoryTrackingModeTest(private val useDriver: UseDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)

        private const val DB_NAME = "test.db"
        private const val TRACKING_TABLE_NAME = "room_table_modification_log"
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun persistedTrackingTable() = runTest {
        val database = createDatabase(false)

        assertThat(findCreateSql(database, "sqlite_master")).isNotEmpty()
        assertThat(findCreateSql(database, "sqlite_temp_master")).isNull()
        database.close()
    }

    @Test
    fun temporaryTrackingTable() = runTest {
        val database = createDatabase(true)

        assertThat(findCreateSql(database, "sqlite_master")).isNull()
        assertThat(findCreateSql(database, "sqlite_temp_master")).isNotEmpty()
        database.close()
    }

    @OptIn(ExperimentalRoomApi::class)
    private fun createDatabase(inMemoryTrackingMode: Boolean) =
        Room.databaseBuilder<TestDatabase>(context, DB_NAME)
            .setInMemoryTrackingMode(inMemoryTrackingMode)
            .setDriver(
                when (useDriver) {
                    UseDriver.ANDROID -> AndroidSQLiteDriver()
                    UseDriver.BUNDLED -> BundledSQLiteDriver()
                }
            )
            .build()

    private suspend fun findCreateSql(database: RoomDatabase, masterTable: String) =
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                usePrepared("SELECT name, sql FROM $masterTable") { stmt ->
                    while (stmt.step()) {
                        if (stmt.getText(0) == TRACKING_TABLE_NAME) {
                            return@usePrepared stmt.getText(1)
                        }
                    }
                    return@usePrepared null
                }
            }
        }
}
