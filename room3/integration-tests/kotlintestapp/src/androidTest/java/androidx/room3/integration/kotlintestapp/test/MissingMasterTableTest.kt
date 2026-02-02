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
import androidx.kruth.assertThrows
import androidx.room3.Room
import androidx.room3.execSQL
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.vo.Email
import androidx.room3.integration.kotlintestapp.vo.User
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MissingMasterTableTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun deleteDb() {
        context.deleteDatabase("existingDb.db")
    }

    @Test
    fun openDatabaseWithMissingRoomMasterTable() = runTest {
        val allowVersionUpdates = AtomicBoolean(true)
        val actualDriver = BundledSQLiteDriver()
        val driver =
            object : SQLiteDriver by actualDriver {
                override fun open(fileName: String): SQLiteConnection {
                    val actualConnection = actualDriver.open(fileName)
                    return object : SQLiteConnection by actualConnection {
                        override fun prepare(sql: String): SQLiteStatement {
                            // The INSERT SQL should not go through if version updates are disabled.
                            // This mimics app halt scenario, where a master table is created but
                            // not initialized.
                            if (
                                sql.startsWith("INSERT OR REPLACE INTO room_master_table") &&
                                    !allowVersionUpdates.get()
                            ) {
                                throw RuntimeException("no-version-updates")
                            }
                            return actualConnection.prepare(sql)
                        }
                    }
                }
            }
        val user = User(1, Email("e1", "email address 1"), Email("e2", "email address 2"))

        fun openDb(): TestDatabase {
            return Room.databaseBuilder<TestDatabase>(context = context, name = "existingDb.db")
                .setDriver(driver)
                .build()
        }
        // Open first version of the database, and remove the room_master_table. This will cause
        // a failure next time we open the database, due to a verification failure.
        val db1 = openDb()
        db1.usersDao().insertUser(user)
        // Delete the room_master_table
        db1.useWriterConnection { it.execSQL("DROP TABLE room_master_table;") }
        db1.close()
        allowVersionUpdates.set(false)

        // Second version of the database fails at open, the failed transaction should be rolled
        // back, allowing the third version to open successfully.
        val db2 = openDb()
        assertThrows<Throwable> { db2.usersDao().getUsers() }
            .hasMessageThat()
            .contains("no-version-updates")
        db2.close()
        allowVersionUpdates.set(true)

        // Third version of the database is expected to open with no failures.
        val db3 = openDb()
        assertThat(db3.usersDao().getUsers()).containsExactly(user)
        db3.close()
    }
}
