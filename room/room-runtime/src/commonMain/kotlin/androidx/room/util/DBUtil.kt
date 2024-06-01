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
import androidx.room.Transactor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/** Performs a database operation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
expect suspend fun <R> performSuspending(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> R
): R

internal suspend inline fun <R> RoomDatabase.internalPerform(
    isReadOnly: Boolean,
    inTransaction: Boolean,
    crossinline block: suspend (PooledConnection) -> R
): R =
    useConnection(isReadOnly) { transactor ->
        if (inTransaction) {
            val type =
                if (isReadOnly) {
                    Transactor.SQLiteTransactionType.DEFERRED
                } else {
                    Transactor.SQLiteTransactionType.IMMEDIATE
                }
            if (!isReadOnly && !transactor.inTransaction()) {
                invalidationTracker.sync()
            }
            val result = transactor.withTransaction(type) { block.invoke(this) }
            if (!isReadOnly && !transactor.inTransaction()) {
                invalidationTracker.refreshAsync()
            }
            result
        } else {
            block.invoke(transactor)
        }
    }

/**
 * Gets the database [CoroutineContext] to perform database operation on utility functions. Prefer
 * using this function over directly accessing [RoomDatabase.getCoroutineScope] as it has platform
 * compatibility behaviour.
 */
internal expect suspend fun RoomDatabase.getCoroutineContext(
    inTransaction: Boolean
): CoroutineContext

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

/** Checks for foreign key violations by executing a PRAGMA foreign_key_check. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun foreignKeyCheck(db: SQLiteConnection, tableName: String) {
    db.prepare("PRAGMA foreign_key_check(`$tableName`)").use { stmt ->
        if (stmt.step()) {
            val errorMsg = processForeignKeyCheckFailure(stmt)
            throw SQLiteException(errorMsg)
        }
    }
}

/**
 * Converts the [SQLiteStatement] returned in case of a foreign key violation into a detailed error
 * message for debugging.
 *
 * The foreign_key_check pragma returns one row output for each foreign key violation.
 *
 * The cursor received has four columns for each row output. The first column is the name of the
 * child table. The second column is the rowId of the row that contains the foreign key violation
 * (or NULL if the child table is a WITHOUT ROWID table). The third column is the name of the parent
 * table. The fourth column is the index of the specific foreign key constraint that failed.
 *
 * @param stmt SQLiteStatement containing information regarding the FK violation
 * @return Error message generated containing debugging information
 */
private fun processForeignKeyCheckFailure(stmt: SQLiteStatement): String {
    return buildString {
        var rowCount = 0
        val fkParentTables = mutableMapOf<String, String>()
        do {
            if (rowCount == 0) {
                append("Foreign key violation(s) detected in '")
                append(stmt.getText(0)).append("'.\n")
            }
            val constraintIndex = stmt.getText(3)
            if (!fkParentTables.containsKey(constraintIndex)) {
                fkParentTables[constraintIndex] = stmt.getText(2)
            }
            rowCount++
        } while (stmt.step())

        append("Number of different violations discovered: ")
        append(fkParentTables.keys.size).append("\n")
        append("Number of rows in violation: ")
        append(rowCount).append("\n")
        append("Violation(s) detected in the following constraint(s):\n")

        for ((key, value) in fkParentTables) {
            append("\tParent Table = ")
            append(value)
            append(", Foreign Key Constraint Index = ")
            append(key).append("\n")
        }
    }
}
