/*
 * Copyright 2020 The Android Open Source Project
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
@file:JvmName("RxRoom")

package androidx.room3.rxjava3

import androidx.annotation.RestrictTo
import androidx.room3.RoomDatabase
import androidx.room3.coroutines.createFlow
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteConnection
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.rx3.rxMaybe
import kotlinx.coroutines.rx3.rxSingle

/** Marker class used by annotation processor to identify dependency is in the classpath. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public class Rx3RoomArtifactMarker private constructor()

/** Data dispatched by the publisher created by [createFlowable]. */
@JvmField public val NOTHING: Any = Any()

/** Helper function used by generated code to create a [Flowable] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun <T : Any> createFlowable(
    db: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    block: suspend (SQLiteConnection) -> T?,
): Flowable<T> =
    createObservable(db, inTransaction, tableNames, block).toFlowable(BackpressureStrategy.LATEST)

/** Helper function used by generated code to create a [Observable] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun <T : Any> createObservable(
    db: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    block: suspend (SQLiteConnection) -> T?,
): Observable<T> =
    createFlow(db, inTransaction, tableNames, block)
        .filterNotNull()
        .asObservable(db.getQueryContext())

/** Helper function used by generated code to create a [Maybe] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun <T : Any> createMaybe(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: suspend (SQLiteConnection) -> T?,
): Maybe<T> =
    rxMaybe(db.getQueryContext().minusKey(Job)) {
        performSuspending(db, isReadOnly, inTransaction, block)
    }

/** Helper function used by generated code to create a [Completable] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun createCompletable(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: suspend (SQLiteConnection) -> Unit,
): Completable =
    rxCompletable(db.getQueryContext().minusKey(Job)) {
        performSuspending(db, isReadOnly, inTransaction, block)
    }

/** Helper function used by generated code to create a [Single] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun <T : Any> createSingle(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: suspend (SQLiteConnection) -> T?,
): Single<T> =
    rxSingle(db.getQueryContext().minusKey(Job)) {
        performSuspending(db, isReadOnly, inTransaction, block)
            ?: throw EmptyResultSetException("Query returned empty result set.")
    }

/**
 * Creates a [Flowable] that emits at least once and also re-emits whenever one of the observed
 * tables is updated.
 *
 * You can easily chain a database operation to downstream of this [Flowable] to ensure that it
 * re-runs when database is modified.
 *
 * Since database invalidation is batched, multiple changes in the database may results in just 1
 * emission.
 *
 * @param database The database instance
 * @param tableNames The list of table names that should be observed
 * @return A [Flowable] which emits [NOTHING] when one of the observed tables is modified (also once
 *   when the invalidation tracker connection is established).
 */
public fun createFlowable(database: RoomDatabase, vararg tableNames: String): Flowable<Any> {
    return createObservable(database, *tableNames).toFlowable(BackpressureStrategy.LATEST)
}

/**
 * Creates a [Observable] that emits at least once and also re-emits whenever one of the observed
 * tables is updated.
 *
 * You can easily chain a database operation to downstream of this [Observable] to ensure that it
 * re-runs when database is modified.
 *
 * Since database invalidation is batched, multiple changes in the database may results in just 1
 * emission.
 *
 * @param database The database instance
 * @param tableNames The list of table names that should be observed
 * @return A [Observable] which emits [NOTHING] when one of the observed tables is modified (also
 *   once when the invalidation tracker connection is established).
 */
public fun createObservable(database: RoomDatabase, vararg tableNames: String): Observable<Any> {
    return database.invalidationTracker
        .createFlow(*tableNames)
        .map { NOTHING }
        .asObservable(database.getQueryContext())
}
