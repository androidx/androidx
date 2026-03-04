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
import androidx.room3.coroutines.RawConnectionAccessor
import androidx.room3.coroutines.TransactionElement
import androidx.room3.coroutines.runBlockingUninterruptible
import androidx.sqlite.SQLiteConnection
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

/** Blocking version of [performSuspending] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public fun <R> performBlocking(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: suspend (SQLiteConnection) -> R,
): R {
    db.assertNotMainThread()
    val context = db.suspendingTransactionContext.get() ?: EmptyCoroutineContext
    return runBlockingUninterruptible {
        withContext(context) {
            db.internalPerform(isReadOnly, inTransaction) { connection ->
                (connection as RawConnectionAccessor).useRawConnection { rawConnection ->
                    block.invoke(rawConnection)
                }
            }
        }
    }
}

/** Blocking version of [performInTransactionSuspending] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public fun <R> performInTransactionBlocking(db: RoomDatabase, block: () -> R): R {
    db.assertNotMainThread()
    val context = db.suspendingTransactionContext.get() ?: EmptyCoroutineContext
    return runBlockingUninterruptible {
        withContext(context) {
            db.internalPerform(isReadOnly = false, inTransaction = true) { block.invoke() }
        }
    }
}

/**
 * Gets the database [CoroutineContext] to perform database operation on utility functions. Prefer
 * using this function over directly accessing [RoomDatabase.getCoroutineScope] as it has platform
 * compatibility behaviour.
 */
internal actual suspend fun RoomDatabase.getCoroutineContext(
    inTransaction: Boolean
): CoroutineContext {
    val transactionDispatcher = currentCoroutineContext()[TransactionElement]?.transactionDispatcher
    return getQueryContext() + (transactionDispatcher ?: EmptyCoroutineContext)
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
