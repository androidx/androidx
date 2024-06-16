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

@file:JvmName("SQLiteConnectionUtil")

package androidx.room.util

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.use
import kotlin.jvm.JvmName

/**
 * Returns the ROWID of the last row insert from the database connection or `-1` if the most
 * recently executed `INSERT` did not completed. This function should only be called with a
 * connection whose most recent statement was an `INSERT`.
 *
 * See (official SQLite documentation)[http://www.sqlite.org/lang_corefunc.html#last_insert_rowid]
 * for details.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun getLastInsertedRowId(connection: SQLiteConnection): Long {
    if (getTotalChangedRows(connection) == 0) {
        return -1
    }
    return connection.prepare("SELECT last_insert_rowid()").use {
        it.step()
        it.getLong(0)
    }
}

/**
 * Returns the number of database rows that were changed or inserted or deleted by the most recently
 * completed INSERT, DELETE, or UPDATE statement.
 *
 * See the (official SQLite documentation)[http://www.sqlite.org/lang_corefunc.html#changes] for
 * details.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun getTotalChangedRows(connection: SQLiteConnection): Int {
    return connection.prepare("SELECT changes()").use {
        it.step()
        it.getLong(0).toInt()
    }
}
