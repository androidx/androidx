/*
 * Copyright 2018 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@file:JvmName("GuavaRoom")

package androidx.room.guava

import android.annotation.SuppressLint
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.concurrent.futures.ResolvableFuture
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Callable
import java.util.concurrent.Executor

/**
 * Returns a [ListenableFuture] created by submitting the input `callable` to [ArchTaskExecutor]'s
 * background-threaded Executor.
 */
@SuppressLint("LambdaLast")
@Deprecated("No longer used by generated code.")
public fun <T> createListenableFuture(
    callable: Callable<T>,
    query: RoomSQLiteQuery,
    releaseQuery: Boolean
): ListenableFuture<T> {
    return createListenableFuture(
        executor = ArchTaskExecutor.getIOThreadExecutor(),
        callable = callable,
        query = query,
        releaseQuery = releaseQuery,
        cancellationSignal = null
    )
}

/**
 * Returns a [ListenableFuture] created by submitting the input `callable` to [RoomDatabase]'s
 * [java.util.concurrent.Executor].
 */
@SuppressLint("LambdaLast")
@Deprecated("No longer used by generated code.")
public fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    callable: Callable<T>,
    query: RoomSQLiteQuery,
    releaseQuery: Boolean
): ListenableFuture<T> {
    return createListenableFuture(
        executor = roomDatabase.queryExecutor,
        callable = callable,
        query = query,
        releaseQuery = releaseQuery,
        cancellationSignal = null
    )
}

/**
 * Returns a [ListenableFuture] created by submitting the input `callable` to [RoomDatabase]'s
 * [java.util.concurrent.Executor].
 */
@SuppressLint("LambdaLast")
public fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    inTransaction: Boolean,
    callable: Callable<T>,
    query: RoomSQLiteQuery,
    releaseQuery: Boolean
): ListenableFuture<T> {
    return createListenableFuture(
        executor = getExecutor(roomDatabase, inTransaction),
        callable = callable,
        query = query,
        releaseQuery = releaseQuery,
        cancellationSignal = null
    )
}

/**
 * Returns a [ListenableFuture] created by submitting the input `callable` to [RoomDatabase]'s
 * [java.util.concurrent.Executor].
 */
@SuppressLint("LambdaLast")
public fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    inTransaction: Boolean,
    callable: Callable<T>,
    query: RoomSQLiteQuery,
    releaseQuery: Boolean,
    cancellationSignal: CancellationSignal?
): ListenableFuture<T> {
    return createListenableFuture(
        executor = getExecutor(roomDatabase, inTransaction),
        callable = callable,
        query = query,
        releaseQuery = releaseQuery,
        cancellationSignal = cancellationSignal
    )
}

/**
 * Returns a [ListenableFuture] created by submitting the input `callable` to [RoomDatabase]'s
 * [java.util.concurrent.Executor].
 */
@SuppressLint("LambdaLast")
public fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    inTransaction: Boolean,
    callable: Callable<T>,
    query: SupportSQLiteQuery,
    releaseQuery: Boolean,
    cancellationSignal: CancellationSignal?
): ListenableFuture<T> {
    return createListenableFuture(
        executor = getExecutor(roomDatabase, inTransaction),
        callable = callable,
        query = query,
        releaseQuery = releaseQuery,
        cancellationSignal = cancellationSignal
    )
}

private fun <T> createListenableFuture(
    executor: Executor,
    callable: Callable<T>,
    query: SupportSQLiteQuery,
    releaseQuery: Boolean,
    cancellationSignal: CancellationSignal?
): ListenableFuture<T> {
    val future = createListenableFuture(executor, callable)
    if (cancellationSignal != null) {
        future.addListener(
            {
                if (future.isCancelled) {
                    cancellationSignal.cancel()
                }
            },
            directExecutor
        )
    }

    if (releaseQuery) {
        future.addListener(
            {
                if (query is RoomSQLiteQuery) {
                    query.release()
                }
            },
            directExecutor
        )
    }

    return future
}

/**
 * Returns a [ListenableFuture] created by submitting the input `callable` to [RoomDatabase]'s
 * [java.util.concurrent.Executor].
 */
@Deprecated("No longer used by generated code.")
public fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    callable: Callable<T>
): ListenableFuture<T> {
    return createListenableFuture(
        roomDatabase = roomDatabase,
        inTransaction = false,
        callable = callable
    )
}

/**
 * Returns a [ListenableFuture] created by submitting the input `callable` to [RoomDatabase]'s
 * [java.util.concurrent.Executor].
 */
public fun <T> createListenableFuture(
    roomDatabase: RoomDatabase,
    inTransaction: Boolean,
    callable: Callable<T>
): ListenableFuture<T> {
    return createListenableFuture(
        executor = getExecutor(roomDatabase, inTransaction),
        callable = callable
    )
}

/**
 * Returns a [ListenableFuture] created by submitting the input `callable` to an
 * [java.util.concurrent.Executor].
 */
private fun <T> createListenableFuture(
    executor: Executor,
    callable: Callable<T>
): ListenableFuture<T> {
    val future = ResolvableFuture.create<T>()
    executor.execute {
        try {
            val result = callable.call()
            future.set(result)
        } catch (throwable: Throwable) {
            future.setException(throwable)
        }
    }

    return future
}

/** A Direct Executor. */
private val directExecutor = Executor { runnable -> runnable.run() }

private fun getExecutor(database: RoomDatabase, inTransaction: Boolean): Executor {
    return if (inTransaction) {
        database.transactionExecutor
    } else {
        database.queryExecutor
    }
}
