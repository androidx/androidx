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
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.room.RoomDatabase
import androidx.room.TransactionElement
import androidx.room.coroutines.RawConnectionAccessor
import androidx.room.driver.SupportSQLiteConnection
import androidx.room.withTransactionContext
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** Performs a database operation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual suspend fun <R> performSuspending(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> R
): R =
    db.compatCoroutineExecute(inTransaction) {
        db.internalPerform(isReadOnly, inTransaction) { connection ->
            val rawConnection = (connection as RawConnectionAccessor).rawConnection
            block.invoke(rawConnection)
        }
    }

/** Blocking version of [performSuspending] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <R> performBlocking(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> R
): R {
    db.assertNotMainThread()
    db.assertNotSuspendingTransaction()
    return runBlocking {
        db.internalPerform(isReadOnly, inTransaction) { connection ->
            val rawConnection = (connection as RawConnectionAccessor).rawConnection
            block.invoke(rawConnection)
        }
    }
}

/**
 * Utility function to wrap a suspend block in Room's transaction coroutine.
 *
 * This function should only be invoked from generated code and is needed to support `@Transaction`
 * delegates in Java and Kotlin. It is preferred to use the other 'perform' functions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual suspend fun <R> performInTransactionSuspending(db: RoomDatabase, block: suspend () -> R): R =
    if (db.inCompatibilityMode()) {
        db.withTransactionContext {
            db.internalPerform(isReadOnly = false, inTransaction = true) { block.invoke() }
        }
    } else {
        withContext(db.getCoroutineScope().coroutineContext) {
            db.internalPerform(isReadOnly = false, inTransaction = true) { block.invoke() }
        }
    }

/**
 * Compatibility suspend function execution with driver usage. This will maintain the dispatcher
 * behaviour in [androidx.room.CoroutinesRoom.execute] when Room is in compatibility mode executing
 * driver codegen utility functions.
 */
private suspend inline fun <R> RoomDatabase.compatCoroutineExecute(
    inTransaction: Boolean,
    crossinline block: suspend () -> R
): R {
    if (inCompatibilityMode() && isOpenInternal && inTransaction()) {
        return block.invoke()
    }
    return withContext(getCoroutineContext(inTransaction)) { block.invoke() }
}

/**
 * Gets the database [CoroutineContext] to perform database operation on utility functions. Prefer
 * using this function over directly accessing [RoomDatabase.getCoroutineScope] as it has platform
 * compatibility behaviour.
 */
internal actual suspend fun RoomDatabase.getCoroutineContext(
    inTransaction: Boolean
): CoroutineContext {
    return if (inCompatibilityMode()) {
        // If in compatibility mode check if we are on a transaction coroutine, if so combine
        // it with the database context, otherwise use the database dispatchers.
        coroutineContext[TransactionElement]?.transactionDispatcher?.let { getQueryContext() + it }
            ?: if (inTransaction) getTransactionContext() else getQueryContext()
    } else {
        getCoroutineScope().coroutineContext
    }
}

/**
 * Performs the SQLiteQuery on the given database.
 *
 * This util method encapsulates copying the cursor if the `maybeCopy` parameter is `true` and
 * either the api level is below a certain threshold or the full result of the query does not fit in
 * a single window.
 *
 * @param db The database to perform the query on.
 * @param sqLiteQuery The query to perform.
 * @param maybeCopy True if the result cursor should maybe be copied, false otherwise.
 * @return Result of the query.
 */
@Deprecated("This is only used in the generated code and shouldn't be called directly.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun query(db: RoomDatabase, sqLiteQuery: SupportSQLiteQuery, maybeCopy: Boolean): Cursor {
    return query(db, sqLiteQuery, maybeCopy, null)
}

/**
 * Performs the SQLiteQuery on the given database.
 *
 * This util method encapsulates copying the cursor if the `maybeCopy` parameter is `true` and
 * either the api level is below a certain threshold or the full result of the query does not fit in
 * a single window.
 *
 * @param db The database to perform the query on.
 * @param sqLiteQuery The query to perform.
 * @param maybeCopy True if the result cursor should maybe be copied, false otherwise.
 * @param signal The cancellation signal to be attached to the query.
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
        val rowsInWindow =
            if (cursor.hasWindow()) {
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

/** Checks for foreign key violations by executing a PRAGMA foreign_key_check. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun foreignKeyCheck(db: SupportSQLiteDatabase, tableName: String) {
    foreignKeyCheck(SupportSQLiteConnection(db), tableName)
}

/**
 * Reads the user version number out of the database header from the given file.
 *
 * @param databaseFile the database file.
 * @return the database version
 * @throws IOException if something goes wrong reading the file, such as bad database header or
 *   missing permissions.
 * @see [User Version Number](https://www.sqlite.org/fileformat.html.user_version_number).
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
