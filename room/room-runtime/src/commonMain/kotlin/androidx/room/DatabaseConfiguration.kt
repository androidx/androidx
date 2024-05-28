/*
 * Copyright 2023 The Android Open Source Project
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
expect class DatabaseConfiguration {
    /* The name of the database file or null if it is an in-memory database. */
    val name: String?
    /* Collection of available migrations. */
    val migrationContainer: RoomDatabase.MigrationContainer
    /* Database callbacks. */
    val callbacks: List<RoomDatabase.Callback>?
    /* The journal mode for this database. */
    val journalMode: RoomDatabase.JournalMode
    /* Whether Room should throw an exception for missing migrations. */
    val requireMigration: Boolean
    /* Whether Room will fallback to destructive migrations on downgrades only .*/
    val allowDestructiveMigrationOnDowngrade: Boolean
    internal val migrationNotRequiredFrom: Set<Int>?
    /* List of provided type converters. */
    val typeConverters: List<Any>
    /* List of provided auto migration specs. */
    val autoMigrationSpecs: List<AutoMigrationSpec>
    /* Whether Room will delete all tables or only known tables during destructive migrations. */
    val allowDestructiveMigrationForAllTables: Boolean
    /* The SQLite Driver for the database. */
    val sqliteDriver: SQLiteDriver?
    /* The Coroutine context for the database. */
    val queryCoroutineContext: CoroutineContext?
}
