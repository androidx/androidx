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

package androidx.room3.util

import androidx.annotation.RestrictTo
import androidx.room3.RoomDatabase
import androidx.room3.TransactionElement
import androidx.room3.coroutines.RawConnectionAccessor
import androidx.room3.coroutines.runBlockingUninterruptible
import androidx.room3.withTransactionContext
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.SupportSQLiteConnection
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext

/** Performs a database operation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public actual suspend fun <R> performSuspending(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> R,
): R =
    db.compatCoroutineExecute(inTransaction) {
        db.internalPerform(isReadOnly, inTransaction) { connection ->
            (connection as RawConnectionAccessor).useRawConnection { rawConnection ->
                block.invoke(rawConnection)
            }
        }
    }

/** Blocking version of [performSuspending] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public fun <R> performBlocking(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> R,
): R {
    db.assertNotMainThread()
    db.assertNotSuspendingTransaction()
    val context = db.suspendingTransactionContext.get() ?: EmptyCoroutineContext
    return runBlockingUninterruptible {
        withContext(context) {
            // If in compatibility mode and the database is already in a transaction, then do not
            // start a nested transaction to avoid the overhead and because the SupportSQLite APIs
            // do not support real SAVEPOINT-based nested transactions.
            val inTransaction = !(db.inCompatibilityMode() && db.inTransaction()) && inTransaction
            db.internalPerform(isReadOnly, inTransaction) { connection ->
                (connection as RawConnectionAccessor).useRawConnection { rawConnection ->
                    block.invoke(rawConnection)
                }
            }
        }
    }
}

/**
 * Utility function to wrap a suspend block in Room's transaction coroutine.
 *
 * This function should only be invoked from generated code and is needed to support `@Transaction`
 * delegates in Java and Kotlin. It is preferred to use the other 'perform' functions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public actual suspend fun <R> performInTransactionSuspending(
    db: RoomDatabase,
    block: suspend () -> R,
): R =
    if (db.inCompatibilityMode()) {
        db.withTransactionContext {
            db.internalPerform(isReadOnly = false, inTransaction = true) { block.invoke() }
        }
    } else {
        db.compatCoroutineExecute(true) {
            db.internalPerform(isReadOnly = false, inTransaction = true) { block.invoke() }
        }
    }

/** Blocking version of [performInTransactionSuspending] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public fun <R> performInTransactionBlocking(db: RoomDatabase, block: () -> R): R {
    db.assertNotMainThread()
    db.assertNotSuspendingTransaction()
    val context = db.suspendingTransactionContext.get() ?: EmptyCoroutineContext
    return runBlockingUninterruptible {
        withContext(context) {
            // If in compatibility mode and the database is already in a transaction, then do not
            // start a nested transaction to avoid the overhead and because the SupportSQLite APIs
            // do not support real SAVEPOINT-based nested transactions.
            val inTransaction = !db.inCompatibilityMode() || !db.inTransaction()
            db.internalPerform(false, inTransaction) { block.invoke() }
        }
    }
}

/**
 * Compatibility suspend function execution with driver usage. This will maintain the dispatcher
 * behaviour in [androidx.room3.CoroutinesRoom.execute] when Room is in compatibility mode executing
 * driver codegen utility functions.
 */
private suspend inline fun <R> RoomDatabase.compatCoroutineExecute(
    inTransaction: Boolean,
    crossinline block: suspend () -> R,
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
    val transactionDispatcher = coroutineContext[TransactionElement]?.transactionDispatcher
    return if (inCompatibilityMode()) {
        // If in compatibility mode check if we are on a transaction coroutine, if so combine
        // it with the database context, otherwise use the database dispatchers.
        if (transactionDispatcher != null) {
            getQueryContext() + transactionDispatcher
        } else if (inTransaction) {
            getTransactionContext()
        } else {
            getQueryContext()
        }
    } else {
        getQueryContext() + (transactionDispatcher ?: EmptyCoroutineContext)
    }
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
@Throws(IOException::class)
internal fun readVersion(databaseFile: File): Int {
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public fun toSQLiteConnection(db: SupportSQLiteDatabase): SQLiteConnection {
    return SupportSQLiteConnection(db)
}
