/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.database.AbstractWindowedCursor
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.room.RoomDatabase
import androidx.room.TransactionElement
import androidx.room.driver.SupportSQLiteConnection
import androidx.room.getQueryDispatcher
import androidx.room.transactionDispatcher
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Performs a single database read operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual suspend fun <R> performReadSuspending(
    db: RoomDatabase,
    sql: String,
    block: (SQLiteStatement) -> R
): R {
    return if (db.inCompatibilityMode()) {
        if (db.isOpenInternal && db.inTransaction()) {
            db.perform(true, sql, block)
        }
        val context =
            coroutineContext[TransactionElement]?.transactionDispatcher ?: db.getQueryDispatcher()
        withContext(context) {
            db.perform(true, sql, block)
        }
    } else {
        db.perform(true, sql, block)
    }
}

/**
 * Performs a single database read transaction operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual suspend fun <R> performReadTransactionSuspending(
    db: RoomDatabase,
    sql: String,
    block: (SQLiteStatement) -> R
): R {
    return if (db.inCompatibilityMode()) {
        if (db.isOpenInternal && db.inTransaction()) {
            db.performTransaction(true) { it.usePrepared(sql, block) }
        }
        val context =
            coroutineContext[TransactionElement]?.transactionDispatcher ?: db.transactionDispatcher
        withContext(context) {
            db.performTransaction(true) { it.usePrepared(sql, block) }
        }
    } else {
        db.performTransaction(true) { it.usePrepared(sql, block) }
    }
}

/**
 * Performs a single database read query operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <R> performReadBlocking(
    db: RoomDatabase,
    sql: String,
    block: (SQLiteStatement) -> R
): R {
    db.assertNotMainThread()
    db.assertNotSuspendingTransaction()
    return runBlocking { db.perform(isReadOnly = true, sql, block) }
}

/**
 * Performs a single database read query transaction operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <R> performReadTransactionBlocking(
    db: RoomDatabase,
    sql: String,
    block: (SQLiteStatement) -> R
): R {
    db.assertNotMainThread()
    db.assertNotSuspendingTransaction()
    return runBlocking { db.performTransaction(isReadOnly = true) { it.usePrepared(sql, block) } }
}

/**
 * Performs the SQLiteQuery on the given database.
 *
 * This util method encapsulates copying the cursor if the `maybeCopy` parameter is
 * `true` and either the api level is below a certain threshold or the full result of the
 * query does not fit in a single window.
 *
 * @param db          The database to perform the query on.
 * @param sqLiteQuery The query to perform.
 * @param maybeCopy   True if the result cursor should maybe be copied, false otherwise.
 * @return Result of the query.
 *
 */
@Deprecated(
    "This is only used in the generated code and shouldn't be called directly."
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun query(db: RoomDatabase, sqLiteQuery: SupportSQLiteQuery, maybeCopy: Boolean): Cursor {
    return query(db, sqLiteQuery, maybeCopy, null)
}

/**
 * Performs the SQLiteQuery on the given database.
 *
 * This util method encapsulates copying the cursor if the `maybeCopy` parameter is
 * `true` and either the api level is below a certain threshold or the full result of the
 * query does not fit in a single window.
 *
 * @param db          The database to perform the query on.
 * @param sqLiteQuery The query to perform.
 * @param maybeCopy   True if the result cursor should maybe be copied, false otherwise.
 * @param signal      The cancellation signal to be attached to the query.
 * @return Result of the query.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun query(
    db: RoomDatabase,
    sqLiteQuery: SupportSQLiteQuery,
    maybeCopy: Boolean,
    signal: CancellationSignal?
): Cursor {
    val cursor = db.query(sqLiteQuery, signal)
    if (maybeCopy && cursor is AbstractWindowedCursor) {
        val rowsInCursor = cursor.count // Should fill the window.
        val rowsInWindow = if (cursor.hasWindow()) {
            cursor.window.numRows
        } else {
            rowsInCursor
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || rowsInWindow < rowsInCursor) {
            return copyAndClose(cursor)
        }
    }
    return cursor
}

/**
 * Drops all FTS content sync triggers created by Room.
 *
 * FTS content sync triggers created by Room are those that are found in the sqlite_master table
 * who's names start with 'room_fts_content_sync_'.
 *
 * @param db The database.
 */
@Deprecated("Replaced by dropFtsSyncTriggers(connection: SQLiteConnection)")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun dropFtsSyncTriggers(db: SupportSQLiteDatabase) {
    dropFtsSyncTriggers(SupportSQLiteConnection(db))
}

/**
 * Checks for foreign key violations by executing a PRAGMA foreign_key_check.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun foreignKeyCheck(
    db: SupportSQLiteDatabase,
    tableName: String
) {
    db.query("PRAGMA foreign_key_check(`$tableName`)").useCursor { cursor ->
        if (cursor.count > 0) {
            val errorMsg = processForeignKeyCheckFailure(cursor)
            throw SQLiteConstraintException(errorMsg)
        }
    }
}

/**
 * Reads the user version number out of the database header from the given file.
 *
 * @param databaseFile the database file.
 * @return the database version
 * @throws IOException if something goes wrong reading the file, such as bad database header or
 * missing permissions.
 *
 * @see [User Version
 * Number](https://www.sqlite.org/fileformat.html.user_version_number).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Throws(IOException::class)
fun readVersion(databaseFile: File): Int {
    FileInputStream(databaseFile).channel.use { input ->
        val buffer = ByteBuffer.allocate(4)
        input.tryLock(60, 4, true)
        input.position(60)
        val read = input.read(buffer)
        if (read != 4) {
            throw IOException("Bad database header, unable to read 4 bytes at offset 60")
        }
        buffer.rewind()
        return buffer.int // ByteBuffer is big-endian by default
    }
}

/**
 * This function will create a new instance of [CancellationSignal].
 *
 * @return A new instance of CancellationSignal.
 */
@Deprecated("Use constructor", ReplaceWith("CancellationSignal()", "android.os.CancellationSignal"))
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun createCancellationSignal(): CancellationSignal {
    return CancellationSignal()
}

/**
 * Converts the [Cursor] returned in case of a foreign key violation into a detailed
 * error message for debugging.
 *
 * The foreign_key_check pragma returns one row output for each foreign key violation.
 *
 * The cursor received has four columns for each row output. The first column is the name of
 * the child table. The second column is the rowId of the row that contains the foreign key
 * violation (or NULL if the child table is a WITHOUT ROWID table). The third column is the
 * name of the parent table. The fourth column is the index of the specific foreign key
 * constraint that failed.
 *
 * @param cursor Cursor containing information regarding the FK violation
 * @return Error message generated containing debugging information
 */
private fun processForeignKeyCheckFailure(cursor: Cursor): String {
    return buildString {
        val rowCount = cursor.count
        val fkParentTables = mutableMapOf<String, String>()

        while (cursor.moveToNext()) {
            if (cursor.isFirst) {
                append("Foreign key violation(s) detected in '")
                append(cursor.getString(0)).append("'.\n")
            }
            val constraintIndex = cursor.getString(3)
            if (!fkParentTables.containsKey(constraintIndex)) {
                fkParentTables[constraintIndex] = cursor.getString(2)
            }
        }

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
