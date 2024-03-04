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
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Performs a database operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
expect suspend fun <R> performSuspending(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> R
): R

/**
 * Utility function to wrap a suspend block in Room's transaction coroutine.
 *
 * This function should only be invoked from generated code and is needed to support `@Transaction`
 * delegates in Java and Kotlin. It is preferred to use the other 'perform' functions.
 */
// TODO(b/309996304): Replace with proper suspending transaction API for common.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
expect suspend fun <R> performInTransactionSuspending(db: RoomDatabase, block: suspend () -> R): R

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
