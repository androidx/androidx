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

/**
 * Configuration class for a [RoomDatabase].
 */
expect class DatabaseConfiguration {
    /* The name of the database file or null if it is an in-memory database. */
    val name: String?
    /* Collection of available migrations. */
    val migrationContainer: RoomDatabase.MigrationContainer
    val journalMode: RoomDatabase.JournalMode
    val requireMigration: Boolean
    val allowDestructiveMigrationOnDowngrade: Boolean
    internal val migrationNotRequiredFrom: Set<Int>?
    val typeConverters: List<Any>
    val autoMigrationSpecs: List<AutoMigrationSpec>
    val sqliteDriver: SQLiteDriver?
}
