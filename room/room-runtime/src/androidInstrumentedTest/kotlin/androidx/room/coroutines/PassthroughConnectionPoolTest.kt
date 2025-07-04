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

package androidx.room.coroutines

import androidx.kruth.assertThat
import androidx.room.Room
import androidx.room.test.TestDatabase
import androidx.room.test.TestDatabase_Impl
import androidx.room.test.createDefaultConfiguration
import androidx.room.useReaderConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

@SmallTest
class PassthroughConnectionPoolTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun usePassthrough_support() {
        val connectionManager =
            TestDatabase_Impl()
                .createConnectionManager(
                    createDefaultConfiguration(instrumentation)
                        .copy(sqliteOpenHelperFactory = FrameworkSQLiteOpenHelperFactory())
                )
        assertThat(connectionManager.connectionPool).isInstanceOf<PassthroughConnectionPool>()
    }

    @Test
    fun usePassthrough_AndroidDriver() {
        val connectionManager =
            TestDatabase_Impl()
                .createConnectionManager(
                    createDefaultConfiguration(instrumentation)
                        .copy(sqliteDriver = AndroidSQLiteDriver())
                )
        assertThat(connectionManager.connectionPool).isInstanceOf<PassthroughConnectionPool>()
    }

    @Test
    fun usePassthrough_wrappedAndroidDriver() {
        val androidDriver = AndroidSQLiteDriver()
        val myDriver = object : SQLiteDriver by androidDriver {}
        val connectionManager =
            TestDatabase_Impl()
                .createConnectionManager(
                    createDefaultConfiguration(instrumentation).copy(sqliteDriver = myDriver)
                )
        assertThat(connectionManager.connectionPool).isInstanceOf<PassthroughConnectionPool>()
    }

    @Test
    fun reusingConnection_instanceCheck() = runTest {
        val db =
            Room.inMemoryDatabaseBuilder<TestDatabase>(instrumentation.targetContext)
                .setDriver(AndroidSQLiteDriver())
                .build()
        val connectionOne =
            db.useReaderConnection { connectionOne ->
                db.useReaderConnection { reusedConnection ->
                    assertThat(connectionOne).isSameInstanceAs(reusedConnection)
                }
                connectionOne
            }
        val connectionTwo = db.useReaderConnection { it }
        assertThat(connectionOne).isNotSameInstanceAs(connectionTwo)
    }
}
