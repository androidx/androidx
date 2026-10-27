/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.coroutines

import androidx.room3.RoomDatabase
import androidx.room3.RoomExternalOperationElement
import androidx.room3.Transactor.SQLiteTransactionType
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Context holder for executing thread-confined transactions with
 * [androidx.sqlite.driver.AndroidSQLiteDriver] or drivers that report having a connection pool.
 */
internal class ThreadConfinedTransactionContext(
    internal val limitedExecutor: Executor,
    internal val writeMutex: Mutex,
    internal val readSemaphore: Semaphore?,
)

/**
 * Calls the specified suspending [block] with Room's thread confined transaction context.
 *
 * This function is for executing suspending transactions with a driver using the
 * [PassthroughConnectionPool]. This will maintain the dispatcher behaviour of `withTransaction`
 * (now a deleted API) when Room has a thread confined driver like the
 * [androidx.sqlite.driver.AndroidSQLiteDriver] or any driver that reports has having a connection
 * pool (via [androidx.sqlite.SQLiteDriver.hasConnectionPool].
 *
 * This function adds a coroutine context (see [createTransactionContext]) that enables dispatching
 * database operation on an acquired thread so that thread confinement operations are part of the
 * same transaction.
 *
 * As a performance optimization, if transaction coroutine is not external (does not have the
 * [RoomExternalOperationElement] marker) then the transaction block is invoked immediately since
 * Room internal transactions do not change dispatchers.
 *
 * For further reading on the origin of this function see
 * [Threading models in Coroutines and Android SQLite API](https://medium.com/androiddevelopers/threading-models-in-coroutines-and-android-sqlite-api-6cab11f7eb90)
 */
internal suspend fun <R> RoomDatabase.withTransactionContext(
    type: SQLiteTransactionType,
    block: suspend () -> R,
): R {
    if (currentCoroutineContext()[RoomExternalOperationElement] == null) {
        return block.invoke()
    }
    val transactionBlock: suspend CoroutineScope.() -> R = transaction@{
        checkNotNull(coroutineContext[TransactionElement]) {
            "Expected a TransactionElement in the CoroutineContext but none was found."
        }
        return@transaction block.invoke()
    }
    // Use inherited transaction context if available, this allows nested suspending transactions.
    val transactionDispatcher = currentCoroutineContext()[TransactionElement]?.transactionDispatcher
    return if (transactionDispatcher != null) {
        withContext(transactionDispatcher, transactionBlock)
    } else {
        startTransactionCoroutine(type, transactionBlock)
    }
}

/**
 * Rate limits and starts the transaction coroutine based on the given transaction [type].
 *
 * Concurrency is controlled in coroutine-space before claiming a thread from
 * [ThreadConfinedTransactionContext.limitedExecutor] to avoid exhausting executor threads when
 * concurrent transactions wait for connections:
 * - Write transactions ([SQLiteTransactionType.IMMEDIATE] and [SQLiteTransactionType.EXCLUSIVE])
 *   acquire [ThreadConfinedTransactionContext.writeMutex] to ensure only a single writer runs.
 * - Read transactions ([SQLiteTransactionType.DEFERRED]) acquire a permit from
 *   [ThreadConfinedTransactionContext.readSemaphore] in WAL mode to match the number of read
 *   connections, or [ThreadConfinedTransactionContext.writeMutex] in TRUNCATE mode where only a
 *   single connection is available.
 */
private suspend fun <R> RoomDatabase.startTransactionCoroutine(
    type: SQLiteTransactionType,
    transactionBlock: suspend CoroutineScope.() -> R,
): R =
    when (type) {
        SQLiteTransactionType.DEFERRED -> {
            val semaphore = threadConfinedTransactionContext.readSemaphore
            if (semaphore != null) {
                semaphore.withPermit { startTransactionCoroutine(transactionBlock) }
            } else {
                threadConfinedTransactionContext.writeMutex.withLock {
                    startTransactionCoroutine(transactionBlock)
                }
            }
        }
        SQLiteTransactionType.IMMEDIATE,
        SQLiteTransactionType.EXCLUSIVE -> {
            threadConfinedTransactionContext.writeMutex.withLock {
                startTransactionCoroutine(transactionBlock)
            }
        }
    }

/**
 * Suspend caller coroutine and start the transaction coroutine in a thread from the
 * [ThreadConfinedTransactionContext.limitedExecutor] in
 * [RoomDatabase.threadConfinedTransactionContext], resuming the caller coroutine with the result
 * once done. The caller's `context` will be a parent of the started coroutine to propagating
 * cancellation and release the thread when cancelled.
 */
private suspend fun <R> RoomDatabase.startTransactionCoroutine(
    transactionBlock: suspend CoroutineScope.() -> R
): R = suspendCancellableCoroutine { continuation ->
    try {
        threadConfinedTransactionContext.limitedExecutor.execute {
            if (!continuation.isActive) {
                // If the coroutine got canceled while the runnable was in the queue, then free
                // the thread quickly by returning early.
                return@execute
            }
            try {
                // Thread acquired, start the transaction coroutine using the parent context.
                // The started coroutine will have an event loop dispatcher that we'll use for the
                // transaction context.
                runBlocking(continuation.context.minusKey(ContinuationInterceptor)) {
                    val dispatcher = coroutineContext[ContinuationInterceptor]!!
                    val transactionContext = createTransactionContext(dispatcher)
                    continuation.resume(withContext(transactionContext, transactionBlock))
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
                "Unable to acquire a thread to perform the database transaction.",
                ex,
            )
        )
    }
}

/**
 * Creates a [CoroutineContext] for performing database operations within a coroutine transaction.
 *
 * The context is a combination of a dispatcher, a [TransactionElement] and a thread local element.
 * * The dispatcher will dispatch coroutines to a single thread that is taken over from the Room
 *   transaction executor. If the coroutine context is switched, suspending DAO functions will be
 *   able to dispatch to the transaction thread. In reality the dispatcher is the event loop of a
 *   [runBlocking] started on the dedicated thread.
 * * The [TransactionElement] serves as an indicator for inherited context, meaning, if there is a
 *   switch of context, suspending DAO methods will be able to use the indicator to dispatch the
 *   database operation to the transaction thread.
 * * The thread local element serves as a second indicator and marks threads that are used to
 *   execute coroutines within the coroutine transaction, more specifically it allows us to identify
 *   if a blocking DAO method is invoked within the transaction coroutine.
 */
private fun RoomDatabase.createTransactionContext(
    dispatcher: ContinuationInterceptor
): CoroutineContext {
    val baseContext = dispatcher + TransactionElement(dispatcher)
    val threadLocalElement = suspendingTransactionContext.asContextElement(baseContext)
    return baseContext + threadLocalElement
}

/**
 * A [CoroutineContext.Element] that indicates there is an on-going database transaction.
 *
 * Even though all this element contains is a [ContinuationInterceptor], it is required since its
 * key will be unique which prevents the interceptor to be overridden during a context folding.
 */
internal class TransactionElement(internal val transactionDispatcher: ContinuationInterceptor) :
    CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<TransactionElement>

    override val key: CoroutineContext.Key<TransactionElement>
        get() = TransactionElement
}
