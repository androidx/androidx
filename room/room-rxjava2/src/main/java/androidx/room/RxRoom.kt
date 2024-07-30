/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room

import androidx.annotation.RestrictTo
import androidx.room.coroutines.createFlow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteConnection
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.rx2.asObservable

open class RxRoom
@Deprecated("This type should not be instantiated as it contains only utility functions.")
constructor() {

    companion object {

        /** Data dispatched by the publisher created by [createFlowable]. */
        @JvmField val NOTHING: Any = Any()

        /** Helper function used by generated code to create a [Flowable] */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        fun <T : Any> createFlowable(
            db: RoomDatabase,
            inTransaction: Boolean,
            tableNames: Array<String>,
            block: (SQLiteConnection) -> T?
        ): Flowable<T> =
            createObservable(db, inTransaction, tableNames, block)
                .toFlowable(BackpressureStrategy.LATEST)

        /** Helper function used by generated code to create a [Observable] */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        fun <T : Any> createObservable(
            db: RoomDatabase,
            inTransaction: Boolean,
            tableNames: Array<String>,
            block: (SQLiteConnection) -> T?
        ): Observable<T> =
            createFlow(db, inTransaction, tableNames, block)
                .filterNotNull()
                .asObservable(db.getQueryContext())

        /** Helper function used by generated code to create a [Maybe] */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        fun <T : Any> createMaybe(
            db: RoomDatabase,
            isReadOnly: Boolean,
            inTransaction: Boolean,
            block: (SQLiteConnection) -> T?
        ): Maybe<T> = Maybe.fromCallable { performBlocking(db, isReadOnly, inTransaction, block) }

        /** Helper function used by generated code to create a [Completable] */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        fun createCompletable(
            db: RoomDatabase,
            isReadOnly: Boolean,
            inTransaction: Boolean,
            block: (SQLiteConnection) -> Unit
        ): Completable =
            Completable.fromAction { performBlocking(db, isReadOnly, inTransaction, block) }

        /** Helper function used by generated code to create a [Single] */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        fun <T : Any> createSingle(
            db: RoomDatabase,
            isReadOnly: Boolean,
            inTransaction: Boolean,
            block: (SQLiteConnection) -> T?
        ): Single<T> =
            Single.create { emitter ->
                if (emitter.isDisposed) return@create
                try {
                    val result = performBlocking(db, isReadOnly, inTransaction, block)
                    if (result != null) {
                        emitter.onSuccess(result)
                    } else {
                        throw EmptyResultSetException("Query returned empty result set.")
                    }
                } catch (e: EmptyResultSetException) {
                    emitter.tryOnError(e)
                }
            }

        /**
         * Creates a [Flowable] that emits at least once and also re-emits whenever one of the
         * observed tables is updated.
         *
         * You can easily chain a database operation to downstream of this [Flowable] to ensure that
         * it re-runs when database is modified.
         *
         * Since database invalidation is batched, multiple changes in the database may results in
         * just 1 emission.
         *
         * @param database The database instance
         * @param tableNames The list of table names that should be observed
         * @return A [Flowable] which emits [NOTHING] when one of the observed tables is modified
         *   (also once when the invalidation tracker connection is established).
         */
        @JvmStatic
        fun createFlowable(database: RoomDatabase, vararg tableNames: String): Flowable<Any> {
            return Flowable.create(
                { emitter ->
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
                            Disposables.fromAction {
                                database.invalidationTracker.removeObserver(observer)
                            }
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
         * Helper method used by generated code to bind a [Callable] such that it will be run in our
         * disk io thread and will automatically block null values since RxJava2 does not like null.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @Deprecated("No longer used by generated code.")
        fun <T : Any> createFlowable(
            database: RoomDatabase,
            tableNames: Array<String>,
            callable: Callable<out T>
        ): Flowable<T> {
            @Suppress("DEPRECATION") return createFlowable(database, false, tableNames, callable)
        }

        /**
         * Helper method used by generated code to bind a [Callable] such that it will be run in our
         * disk io thread and will automatically block null values since RxJava2 does not like null.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @Deprecated("No longer used by generated code.")
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
         * Creates a [Observable] that emits at least once and also re-emits whenever one of the
         * observed tables is updated.
         *
         * You can easily chain a database operation to downstream of this [Observable] to ensure
         * that it re-runs when database is modified.
         *
         * Since database invalidation is batched, multiple changes in the database may results in
         * just 1 emission.
         *
         * @param database The database instance
         * @param tableNames The list of table names that should be observed
         * @return A [Observable] which emits [.NOTHING] when one of the observed tables is modified
         *   (also once when the invalidation tracker connection is established).
         */
        @JvmStatic
        fun createObservable(database: RoomDatabase, vararg tableNames: String): Observable<Any> {
            return Observable.create { emitter ->
                val observer =
                    object : InvalidationTracker.Observer(tableNames) {
                        override fun onInvalidated(tables: Set<String>) {
                            emitter.onNext(NOTHING)
                        }
                    }
                database.invalidationTracker.addObserver(observer)
                emitter.setDisposable(
                    Disposables.fromAction { database.invalidationTracker.removeObserver(observer) }
                )

                // emit once to avoid missing any data and also easy chaining
                emitter.onNext(NOTHING)
            }
        }

        /**
         * Helper method used by generated code to bind a [Callable] such that it will be run in our
         * disk io thread and will automatically block null values since RxJava2 does not like null.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @Deprecated("No longer used by generated code.")
        fun <T : Any> createObservable(
            database: RoomDatabase,
            tableNames: Array<String>,
            callable: Callable<out T>
        ): Observable<T> {
            @Suppress("DEPRECATION") return createObservable(database, false, tableNames, callable)
        }

        /**
         * Helper method used by generated code to bind a [Callable] such that it will be run in our
         * disk io thread and will automatically block null values since RxJava2 does not like null.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @Deprecated("No longer used by generated code.")
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
         * Helper method used by generated code to create a [Single] from a [Callable] that will
         * ignore the [EmptyResultSetException] if the stream is already disposed.
         */
        @JvmStatic
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
    }
}
