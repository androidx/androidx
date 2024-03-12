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

@file:JvmMultifileClass
@file:JvmName("DBUtil")

package androidx.room.util

import androidx.annotation.RestrictTo
import androidx.room.PooledConnection
import androidx.room.RoomDatabase
import androidx.room.Transactor
import androidx.room.coroutines.RawConnectionAccessor
import androidx.sqlite.SQLiteConnection
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Performs a database operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual suspend fun <R> performSuspending(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> R
): R = db.internalPerform(isReadOnly, inTransaction) { connection ->
    val rawConnection = (connection as RawConnectionAccessor).rawConnection
    block.invoke(rawConnection)
}

/**
 * Utility function to wrap a suspend block in Room's transaction coroutine.
 *
 * This function should only be invoked from generated code and is needed to support `@Transaction`
 * delegates in Java and Kotlin. It is preferred to use the other 'perform' functions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual suspend fun <R> performInTransactionSuspending(
    db: RoomDatabase,
    block: suspend () -> R
): R = db.internalPerform(isReadOnly = false, inTransaction = true) {
    block.invoke()
}

private suspend inline fun <R> RoomDatabase.internalPerform(
    isReadOnly: Boolean,
    inTransaction: Boolean,
    crossinline block: suspend (PooledConnection) -> R
): R = useConnection(isReadOnly) { transactor ->
    if (inTransaction) {
        val type = if (isReadOnly) {
            Transactor.SQLiteTransactionType.DEFERRED
        } else {
            Transactor.SQLiteTransactionType.IMMEDIATE
        }
        // TODO(b/309990302): Notify Invalidation Tracker before and after transaction block.
        transactor.withTransaction(type) { block.invoke(this) }
    } else {
        block.invoke(transactor)
    }
}
