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

package androidx.room3.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/**
 * This test validates that a [Database] declared in the Jvm source set does not required the usage
 * of [androidx.room3.ConstructedBy] nor [androidx.room3.RoomDatabaseConstructor].
 */
class JvmOnlyDatabaseDeclarationTest {

    @Test
    fun buildJvmOnlyRoomDatabase() = runTest {
        val database =
            Room.inMemoryDatabaseBuilder<TestDatabase>().setDriver(BundledSQLiteDriver()).build()
        val dbVersion =
            database.useReaderConnection { connection ->
                connection.usePrepared("PRAGMA user_version") {
                    it.step()
                    it.getLong(0)
                }
            }
        assertThat(dbVersion).isEqualTo(1)
    }

    @Database(entities = [TestEntity::class], version = 1, exportSchema = false)
    abstract class TestDatabase : RoomDatabase()

    @Entity data class TestEntity(@PrimaryKey val id: Long)
}
