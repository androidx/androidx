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

package androidx.room.rxjava3

import androidx.annotation.RestrictTo
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.Callable
import java.util.concurrent.Executor

/** Data dispatched by the publisher created by [createFlowable]. */
@JvmField val NOTHING: Any = Any()

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
fun createFlowable(database: RoomDatabase, vararg tableNames: String): Flowable<Any> {
    return Flowable.create(
        { emitter: FlowableEmitter<Any> ->
            val observer =
                object : InvalidationTracker.Observer(tableNames) {
                    override fun onInvalidated(tables: Set<String>) {
                        if (!emitter.isCancelled) {
                            emitter.onNext(NOTHING)
                        }
                    }
                }
            if (!emitter.isCancelled) {
                database.invalidationTracker.addObserver(observer)
                emitter.setDisposable(
                    Disposable.fromAction { database.invalidationTracker.removeObserver(observer) }
                )
            }

            // emit once to avoid missing any data and also easy chaining
            if (!emitter.isCancelled) {
                emitter.onNext(NOTHING)
            }
        },
        BackpressureStrategy.LATEST
    )
}

/**
 * Helper method used by generated code to bind a Callable such that it will be run in our disk io
 * thread and will automatically block null values since RxJava3 does not like null.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <T : Any> createFlowable(
    database: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    callable: Callable<out T>
): Flowable<T> {
    val scheduler = Schedulers.from(getExecutor(database, inTransaction))
    val maybe = Maybe.fromCallable(callable)
    return createFlowable(database, *tableNames)
        .subscribeOn(scheduler)
        .unsubscribeOn(scheduler)
        .observeOn(scheduler)
        .flatMapMaybe { maybe }
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
fun createObservable(database: RoomDatabase, vararg tableNames: String): Observable<Any> {
    return Observable.create { emitter: ObservableEmitter<Any> ->
        val observer =
            object : InvalidationTracker.Observer(tableNames) {
                override fun onInvalidated(tables: Set<String>) {
                    emitter.onNext(NOTHING)
                }
            }
        database.invalidationTracker.addObserver(observer)
        emitter.setDisposable(
            Disposable.fromAction { database.invalidationTracker.removeObserver(observer) }
        )

        // emit once to avoid missing any data and also easy chaining
        emitter.onNext(NOTHING)
    }
}

/**
 * Helper method used by generated code to bind a Callable such that it will be run in our disk io
 * thread and will automatically block null values since RxJava3 does not like null.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <T : Any> createObservable(
    database: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    callable: Callable<out T>
): Observable<T> {
    val scheduler = Schedulers.from(getExecutor(database, inTransaction))
    val maybe = Maybe.fromCallable(callable)
    return createObservable(database, *tableNames)
        .subscribeOn(scheduler)
        .unsubscribeOn(scheduler)
        .observeOn(scheduler)
        .flatMapMaybe { maybe }
}

/**
 * Helper method used by generated code to create a Single from a Callable that will ignore the
 * EmptyResultSetException if the stream is already disposed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <T : Any> createSingle(callable: Callable<out T>): Single<T> {
    return Single.create { emitter ->
        try {
            val result = callable.call()
            if (result != null) {
                emitter.onSuccess(result)
            } else {
                throw EmptyResultSetException("Query returned empty result set.")
            }
        } catch (e: EmptyResultSetException) {
            emitter.tryOnError(e)
        }
    }
}

private fun getExecutor(database: RoomDatabase, inTransaction: Boolean): Executor {
    return if (inTransaction) {
        database.transactionExecutor
    } else {
        database.queryExecutor
    }
}
