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

@file:JvmName("GuavaRoom")
@file:Suppress("UNUSED_PARAMETER")

package androidx.room.guava

import android.os.CancellationSignal
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteQuery
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Callable
import java.util.concurrent.Executor

class GuavaRoomArtifactMarker private constructor()

fun <T> createListenableFuture(
        db: RoomDatabase,
        isReadOnly: Boolean,
        inTransaction: Boolean,
        block: (SQLiteConnection) -> T
): ListenableFuture<T> {
    TODO()
}

fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    inTransaction: Boolean,
    callable: Callable<T>,
    query: RoomSQLiteQuery,
    releaseQuery: Boolean
): ListenableFuture<T> {
    TODO()
}

fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    inTransaction: Boolean,
    callable: Callable<T>,
    query: RoomSQLiteQuery,
    releaseQuery: Boolean,
    cancellationSignal: CancellationSignal?
): ListenableFuture<T> {
    TODO()
}

fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    inTransaction: Boolean,
    callable: Callable<T>,
    query: SupportSQLiteQuery,
    releaseQuery: Boolean,
    cancellationSignal: CancellationSignal?
): ListenableFuture<T> {
    TODO()
}

fun <T> createListenableFuture(
    executor: Executor,
    callable: Callable<T>,
    query: SupportSQLiteQuery,
    releaseQuery: Boolean,
    cancellationSignal: CancellationSignal?
): ListenableFuture<T> {
    TODO()
}

fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    inTransaction: Boolean,
    callable: Callable<T>
): ListenableFuture<T> {
    TODO()
}