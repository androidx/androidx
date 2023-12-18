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

import androidx.sqlite.SQLiteDriver

actual class DatabaseConfiguration(
    /**
     * Collection of available migrations.
     */
    actual val migrationContainer: RoomDatabase.MigrationContainer,
    actual val requireMigration: Boolean,
    actual val allowDestructiveMigrationOnDowngrade: Boolean,
    internal actual val migrationNotRequiredFrom: Set<Int>?,
    actual val sqliteDriver: SQLiteDriver?
)
