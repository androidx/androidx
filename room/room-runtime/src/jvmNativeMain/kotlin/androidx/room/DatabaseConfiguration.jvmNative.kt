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

package androidx.room

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteDriver
import kotlin.coroutines.CoroutineContext

/** Configuration class for a [RoomDatabase]. */
public actual class DatabaseConfiguration(
    /* The name of the database file or null if it is an in-memory database. */
    public actual val name: String?,
    /* Collection of available migrations. */
    public actual val migrationContainer: RoomDatabase.MigrationContainer,
    /* Database callbacks. */
    public actual val callbacks: List<RoomDatabase.Callback>?,
    /* The journal mode for this database. */
    public actual val journalMode: RoomDatabase.JournalMode,
    /* Whether Room should throw an exception for missing migrations. */
    public actual val requireMigration: Boolean,
    /* Whether Room will fallback to destructive migrations on downgrades only .*/
    public actual val allowDestructiveMigrationOnDowngrade: Boolean,
    internal actual val migrationNotRequiredFrom: Set<Int>?,
    /* List of provided type converters. */
    public actual val typeConverters: List<Any>,
    /* List of provided auto migration specs. */
    public actual val autoMigrationSpecs: List<AutoMigrationSpec>,
    /* Whether Room will delete all tables or only known tables during destructive migrations. */
    public actual val allowDestructiveMigrationForAllTables: Boolean,
    /* The SQLite Driver for the database. */
    public actual val sqliteDriver: SQLiteDriver?,
    /* The Coroutine context for the database. */
    public actual val queryCoroutineContext: CoroutineContext?,
)
