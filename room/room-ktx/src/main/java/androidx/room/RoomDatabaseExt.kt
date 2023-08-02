/*
 * Copyright 2019 The Android Open Source Project
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
@file:JvmName("RoomDatabaseKt")

package androidx.room

import androidx.annotation.RestrictTo
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Calls the specified suspending [block] in a database transaction. The transaction will be
 * marked as successful unless an exception is thrown in the suspending [block] or the coroutine
 * is cancelled.
 *
 * Room will only perform at most one transaction at a time, additional transactions are queued
 * and executed on a first come, first serve order.
 *
 * Performing blocking database operations is not permitted in a coroutine scope other than the
 * one received by the suspending block. It is recommended that all [Dao] function invoked within
 * the [block] be suspending functions.
 *
 * The internal dispatcher used to execute the given [block] will block an utilize a thread from
 * Room's transaction executor until the [block] is complete.
 */
public suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R {
    val transactionBlock: suspend CoroutineScope.() -> R = transaction@{
        val transactionElement = coroutineContext[TransactionElement]!!
        transactionElement.acquire()
        try {
            @Suppress("DEPRECATION")
            beginTransaction()
            try {
                val result = block.invoke()
                @Suppress("DEPRECATION")
                setTransactionSuccessful()
                return@transaction result
            } finally {
                @Suppress("DEPRECATION")
                endTransaction()
            }
        } finally {
            transactionElement.release()
        }
    }
    // Use inherited transaction context if available, this allows nested suspending transactions.
    val transactionDispatcher = coroutineContext[TransactionElement]?.transactionDispatcher
    return if (transactionDispatcher != null) {
        withContext(transactionDispatcher, transactionBlock)
    } else {
        startTransactionCoroutine(coroutineContext, transactionBlock)
    }
}

/**
 * Suspend caller coroutine and start the transaction coroutine in a thread from the
 * [RoomDatabase.transactionExecutor], resuming the caller coroutine with the result once done.
 * The [context] will be a parent of the started coroutine to propagating cancellation and release
 * the thread when cancelled.
 */
private suspend fun <R> RoomDatabase.startTransactionCoroutine(
    context: CoroutineContext,
    transactionBlock: suspend CoroutineScope.() -> R
): R = suspendCancellableCoroutine { continuation ->
    try {
        transactionExecutor.execute {
            try {
                // Thread acquired, start the transaction coroutine using the parent context.
                // The started coroutine will have an event loop dispatcher that we'll use for the
                // transaction context.
                runBlocking(context.minusKey(ContinuationInterceptor)) {
                    val dispatcher = coroutineContext[ContinuationInterceptor]!!
                    val transactionContext = createTransactionContext(dispatcher)
                    continuation.resume(
                        withContext(transactionContext, transactionBlock)
                    )
                }
            } catch (ex: Throwable) {
                // If anything goes wrong, propagate exception to the calling coroutine.
                continuation.cancel(ex)
            }
        }
    } catch (ex: RejectedExecutionException) {
        // Couldn't acquire a thread, cancel coroutine.
        continuation.cancel(
            IllegalStateException(
                "Unable to acquire a thread to perform the database transaction.", ex
            )
        )
    }
}

/**
 * Creates a [CoroutineContext] for performing database operations within a coroutine transaction.
 *
 * The context is a combination of a dispatcher, a [TransactionElement] and a thread local element.
 *
 * * The dispatcher will dispatch coroutines to a single thread that is taken over from the Room
 * transaction executor. If the coroutine context is switched, suspending DAO functions will be able
 * to dispatch to the transaction thread. In reality the dispatcher is the event loop of a
 * [runBlocking] started on the dedicated thread.
 *
 * * The [TransactionElement] serves as an indicator for inherited context, meaning, if there is a
 * switch of context, suspending DAO methods will be able to use the indicator to dispatch the
 * database operation to the transaction thread.
 *
 * * The thread local element serves as a second indicator and marks threads that are used to
 * execute coroutines within the coroutine transaction, more specifically it allows us to identify
 * if a blocking DAO method is invoked within the transaction coroutine. Never assign meaning to
 * this value, for now all we care is if its present or not.
 */
private fun RoomDatabase.createTransactionContext(
    dispatcher: ContinuationInterceptor
): CoroutineContext {
    val transactionElement = TransactionElement(dispatcher)
    val threadLocalElement =
        suspendingTransactionId.asContextElement(System.identityHashCode(transactionElement))
    return dispatcher + transactionElement + threadLocalElement
}

/**
 * A [CoroutineContext.Element] that indicates there is an on-going database transaction.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TransactionElement(
    internal val transactionDispatcher: ContinuationInterceptor
) : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<TransactionElement>

    override val key: CoroutineContext.Key<TransactionElement>
        get() = TransactionElement

    /**
     * Number of transactions (including nested ones) started with this element.
     * Call [acquire] to increase the count and [release] to decrease it.
     */
    private val referenceCount = AtomicInteger(0)

    fun acquire() {
        referenceCount.incrementAndGet()
    }

    fun release() {
        val count = referenceCount.decrementAndGet()
        if (count < 0) {
            throw IllegalStateException("Transaction was never started or was already released.")
        }
    }
}

/**
 * Creates a [Flow] that listens for changes in the database via the [InvalidationTracker] and emits
 * sets of the tables that were invalidated.
 *
 * The Flow will emit at least one value, a set of all the tables registered for observation to
 * kick-start the stream unless [emitInitialState] is set to `false`.
 *
 * If one of the tables to observe does not exist in the database, this Flow throws an
 * [IllegalArgumentException] during collection.
 *
 * The returned Flow can be used to create a stream that reacts to changes in the database:
 * ```
 * fun getArtistTours(from: Date, to: Date): Flow<Map<Artist, TourState>> {
 *   return db.invalidationTrackerFlow("Artist").map { _ ->
 *     val artists = artistsDao.getAllArtists()
 *     val tours = tourService.fetchStates(artists.map { it.id })
 *     associateTours(artists, tours, from, to)
 *   }
 * }
 * ```
 *
 * @param tables The name of the tables or views to observe.
 * @param emitInitialState Set to `false` if no initial emission is desired. Default value is
 *                         `true`.
 */
public fun RoomDatabase.invalidationTrackerFlow(
    vararg tables: String,
    emitInitialState: Boolean = true
): Flow<Set<String>> = callbackFlow {
    // Flag to ignore invalidation until the initial state is sent.
    val ignoreInvalidation = AtomicBoolean(emitInitialState)
    val observer = object : InvalidationTracker.Observer(tables) {
        override fun onInvalidated(tables: Set<String>) {
            if (ignoreInvalidation.get()) {
                return
            }
            trySend(tables)
        }
    }
    val queryContext =
        coroutineContext[TransactionElement]?.transactionDispatcher ?: getQueryDispatcher()
    val job = launch(queryContext) {
        invalidationTracker.addObserver(observer)
        try {
            if (emitInitialState) {
                // Initial invalidation of all tables, to kick-start the flow
                trySend(tables.toSet())
            }
            ignoreInvalidation.set(false)
            awaitCancellation()
        } finally {
            invalidationTracker.removeObserver(observer)
        }
    }
    awaitClose {
        job.cancel()
    }
}
