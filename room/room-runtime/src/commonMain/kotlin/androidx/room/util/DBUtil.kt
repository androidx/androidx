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

@file:JvmMultifileClass
@file:JvmName("DBUtil")

package androidx.room.util

import androidx.annotation.RestrictTo
import androidx.room.PooledConnection
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Performs a single database read operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
expect suspend fun <R> performReadSuspending(
    db: RoomDatabase,
    sql: String,
    block: (SQLiteStatement) -> R
): R

/**
 * Performs a single database read transaction operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
expect suspend fun <R> performReadTransactionSuspending(
    db: RoomDatabase,
    sql: String,
    block: (SQLiteStatement) -> R
): R

/**
 * Drops all FTS content sync triggers created by Room.
 *
 * FTS content sync triggers created by Room are those that are found in the sqlite_master table
 * who's names start with 'room_fts_content_sync_'.
 *
 * @param connection The database connection.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun dropFtsSyncTriggers(connection: SQLiteConnection) {
    val existingTriggers = buildList {
        connection.prepare("SELECT name FROM sqlite_master WHERE type = 'trigger'").use {
            while (it.step()) {
                add(it.getText(0))
            }
        }
    }

    existingTriggers.forEach { triggerName ->
        if (triggerName.startsWith("room_fts_content_sync_")) {
            connection.execSQL("DROP TRIGGER IF EXISTS $triggerName")
        }
    }
}

/**
 * Returns the ROWID of the last row insert from the database connection which invoked the
 * function.
 *
 * See (official SQLite documentation)[http://www.sqlite.org/lang_corefunc.html#last_insert_rowid]
 * for details.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
suspend fun PooledConnection.getLastInsertedRowId(): Long {
    return this.usePrepared("SELECT last_insert_rowid()") {
        it.step()
        it.getLong(0)
    }
}

/**
 * Returns the number of database rows that were changed or inserted or deleted by the most
 * recently completed INSERT, DELETE, or UPDATE statement.
 *
 * See the (official SQLite documentation)[http://www.sqlite.org/lang_corefunc.html#changes] for
 * details.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
suspend fun PooledConnection.getTotalChangedRows(): Long {
    return this.usePrepared("SELECT changes()") {
        it.step()
        it.getLong(0)
    }
}
