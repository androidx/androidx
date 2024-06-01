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

package androidx.room.migration

import androidx.sqlite.SQLiteConnection

/**
 * Base class for a database migration.
 *
 * Each migration can move between 2 versions that are defined by [startVersion] and [endVersion].
 *
 * A migration can handle more than 1 version (e.g. if you have a faster path to choose when going
 * version 3 to 5 without going to version 4). If Room opens a database at version 3 and latest
 * version is 5, Room will use the migration object that can migrate from 3 to 5 instead of 3 to 4
 * and 4 to 5.
 *
 * If there are not enough migrations provided to move from the current version to the latest
 * version, Room will might clear the database and recreate if destructive migrations are enabled.
 *
 * @constructor Creates a new migration between [startVersion] and [endVersion] inclusive.
 */
expect abstract class Migration(startVersion: Int, endVersion: Int) {
    val startVersion: Int
    val endVersion: Int

    /**
     * Should run the necessary migrations.
     *
     * This function is already called inside a transaction and that transaction might actually be a
     * composite transaction of all necessary `Migration`s.
     *
     * @param connection The database connection
     */
    // TODO(b/316943027): Try and make abstract without breaking API
    open fun migrate(connection: SQLiteConnection)
}
