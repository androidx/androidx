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

package androidx.room3.test

import android.app.Instrumentation
import androidx.room3.Database
import androidx.room3.DatabaseConfiguration
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(entities = [TestEntity::class], version = 1, exportSchema = false)
abstract class TestDatabase : RoomDatabase()

@Entity data class TestEntity(@PrimaryKey val id: Int)

fun createDefaultConfiguration(instrumentation: Instrumentation) =
    DatabaseConfiguration(
        context = instrumentation.targetContext,
        name = null,
        migrationContainer = RoomDatabase.MigrationContainer(),
        callbacks = emptyList(),
        allowMainThreadQueries = true,
        journalMode = RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING,
        multiInstanceInvalidationServiceIntent = null,
        isMigrationRequired = false,
        allowDestructiveMigrationOnDowngrade = false,
        migrationNotRequiredFrom = null,
        prepackagedDatabaseCallback = null,
        typeConverters = emptyList(),
        autoMigrationSpecs = emptyList(),
        allowDestructiveMigrationForAllTables = true,
        sqliteDriver = AndroidSQLiteDriver(),
        queryCoroutineContext = Dispatchers.IO,
    )
