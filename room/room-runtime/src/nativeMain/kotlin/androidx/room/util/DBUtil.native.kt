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
): R = db.useConnection(isReadOnly) { transactor ->
    if (inTransaction) {
        val type = if (isReadOnly) {
            Transactor.SQLiteTransactionType.DEFERRED
        } else {
            Transactor.SQLiteTransactionType.IMMEDIATE
        }
        // TODO(b/309990302): Notify Invalidation Tracker before and after transaction block.
        transactor.withTransaction(type) {
            val rawConnection = (this as RawConnectionAccessor).rawConnection
            block.invoke(rawConnection)
        }
    } else {
        val rawConnection = (transactor as RawConnectionAccessor).rawConnection
        block.invoke(rawConnection)
    }
}
