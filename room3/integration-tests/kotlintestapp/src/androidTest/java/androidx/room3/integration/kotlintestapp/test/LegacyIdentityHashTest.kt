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

import android.content.Context
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.RoomMasterTable
import androidx.room3.execSQL
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LegacyIdentityHashTest {
    private val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "legacy-test.db"

    @Before
    fun setup() {
        targetContext.deleteDatabase(databaseName)
    }

    @After
    fun teardown() {
        targetContext.deleteDatabase(databaseName)
    }

    @Test
    fun openDatabaseWithLegacyHash() = runTest {
        val dbBuilder =
            Room.databaseBuilder<LegacyDatabase>(targetContext, "legacy-test.db")
                .setDriver(AndroidSQLiteDriver())
        val newDb = dbBuilder.build()
        newDb.useWriterConnection {
            val insertQuery = RoomMasterTable.createInsertQuery("7814fcab45e43fa07f359c1be74bd6c9")
            it.execSQL(insertQuery)
        }
        newDb.close()

        val legacyDb = dbBuilder.build()
        legacyDb.useReaderConnection {
            // opens the database
        }
        legacyDb.close()
    }

    @Database(entities = [TestDataEntity::class], version = 1, exportSchema = false)
    abstract class LegacyDatabase : RoomDatabase()

    @Entity data class TestDataEntity(@PrimaryKey var id: Long? = null)
}
