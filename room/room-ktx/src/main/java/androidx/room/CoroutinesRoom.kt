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

package androidx.room

import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.sqlite.db.SupportSQLiteCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A helper class for supporting Kotlin Coroutines in Room.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CoroutinesRoom private constructor() {

    public companion object {

        @JvmStatic
        public suspend fun <R> execute(
            db: RoomDatabase,
            inTransaction: Boolean,
            callable: Callable<R>
        ): R {
            if (db.isOpen && db.inTransaction()) {
                return callable.call()
            }

            // Use the transaction dispatcher if we are on a transaction coroutine, otherwise
            // use the database dispatchers.
            val context = coroutineContext[TransactionElement]?.transactionDispatcher
                ?: if (inTransaction) db.transactionDispatcher else db.getQueryDispatcher()
            return withContext(context) {
                callable.call()
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        @JvmStatic
        public suspend fun <R> execute(
            db: RoomDatabase,
            inTransaction: Boolean,
            cancellationSignal: CancellationSignal,
            callable: Callable<R>
        ): R {
            if (db.isOpen && db.inTransaction()) {
                return callable.call()
            }

            // Use the transaction dispatcher if we are on a transaction coroutine, otherwise
            // use the database dispatchers.
            val context = coroutineContext[TransactionElement]?.transactionDispatcher
                ?: if (inTransaction) db.transactionDispatcher else db.getQueryDispatcher()
            return suspendCancellableCoroutine<R> { continuation ->
                val job = GlobalScope.launch(context) {
                    try {
                        val result = callable.call()
                        continuation.resume(result)
                    } catch (exception: Throwable) {
                        continuation.resumeWithException(exception)
                    }
                }
                continuation.invokeOnCancellation {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        SupportSQLiteCompat.Api16Impl.cancel(cancellationSignal)
                    }
                    job.cancel()
                }
            }
        }

        @JvmStatic
        public fun <R> createFlow(
            db: RoomDatabase,
            inTransaction: Boolean,
            tableNames: Array<String>,
            callable: Callable<R>
        ): Flow<@JvmSuppressWildcards R> = flow {
            coroutineScope {
                // Observer channel receives signals from the invalidation tracker to emit queries.
                val observerChannel = Channel<Unit>(Channel.CONFLATED)
                val observer = object : InvalidationTracker.Observer(tableNames) {
                    override fun onInvalidated(tables: MutableSet<String>) {
                        observerChannel.trySend(Unit)
                    }
                }
                observerChannel.trySend(Unit) // Initial signal to perform first query.
                val queryContext = coroutineContext[TransactionElement]?.transactionDispatcher
                    ?: if (inTransaction) db.transactionDispatcher else db.getQueryDispatcher()
                val resultChannel = Channel<R>()
                launch(queryContext) {
                    db.invalidationTracker.addObserver(observer)
                    try {
                        // Iterate until cancelled, transforming observer signals to query results
                        // to be emitted to the flow.
                        for (signal in observerChannel) {
                            val result = callable.call()
                            resultChannel.send(result)
                        }
                    } finally {
                        db.invalidationTracker.removeObserver(observer)
                    }
                }

                emitAll(resultChannel)
            }
        }
    }
}

/**
 * Gets the query coroutine dispatcher.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RoomDatabase.getQueryDispatcher(): CoroutineDispatcher {
    return backingFieldMap.getOrPut("QueryDispatcher") {
        queryExecutor.asCoroutineDispatcher()
    } as CoroutineDispatcher
}

/**
 * Gets the transaction coroutine dispatcher.
 *
 * @hide
 */
internal val RoomDatabase.transactionDispatcher: CoroutineDispatcher
    get() = backingFieldMap.getOrPut("TransactionDispatcher") {
        transactionExecutor.asCoroutineDispatcher()
    } as CoroutineDispatcher
