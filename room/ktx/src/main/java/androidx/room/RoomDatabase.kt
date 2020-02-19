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

package androidx.room

import androidx.annotation.RestrictTo
import kotlinx.coroutines.Job
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

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
 * The dispatcher used to execute the given [block] will utilize threads from Room's query executor.
 */
suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R {
    // Use inherited transaction context if available, this allows nested suspending transactions.
    val transactionContext =
        coroutineContext[TransactionElement]?.transactionDispatcher ?: createTransactionContext()
    return withContext(transactionContext) {
        val transactionElement = coroutineContext[TransactionElement]!!
        transactionElement.acquire()
        try {
            @Suppress("DEPRECATION")
            beginTransaction()
            try {
                val result = block.invoke()
                @Suppress("DEPRECATION")
                setTransactionSuccessful()
                return@withContext result
            } finally {
                @Suppress("DEPRECATION")
                endTransaction()
            }
        } finally {
            transactionElement.release()
        }
    }
}

/**
 * Creates a [CoroutineContext] for performing database operations within a coroutine transaction.
 *
 * The context is a combination of a dispatcher, a [TransactionElement] and a thread local element.
 *
 * * The dispatcher will dispatch coroutines to a single thread that is taken over from the Room
 * query executor. If the coroutine context is switched, suspending DAO functions will be able to
 * dispatch to the transaction thread.
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
private suspend fun RoomDatabase.createTransactionContext(): CoroutineContext {
    val controlJob = Job()
    // make sure to tie the control job to this context to avoid blocking the transaction if
    // context get cancelled before we can even start using this job. Otherwise, the acquired
    // transaction thread will forever wait for the controlJob to be cancelled.
    // see b/148181325
    coroutineContext[Job]?.invokeOnCompletion {
        controlJob.cancel()
    }
    val dispatcher = transactionExecutor.acquireTransactionThread(controlJob)
    val transactionElement = TransactionElement(controlJob, dispatcher)
    val threadLocalElement =
        suspendingTransactionId.asContextElement(System.identityHashCode(controlJob))
    return dispatcher + transactionElement + threadLocalElement
}

/**
 * Acquires a thread from the executor and returns a [ContinuationInterceptor] to dispatch
 * coroutines to the acquired thread. The [controlJob] is used to control the release of the
 * thread by cancelling the job.
 */
private suspend fun Executor.acquireTransactionThread(controlJob: Job): ContinuationInterceptor {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            // We got cancelled while waiting to acquire a thread, we can't stop our attempt to
            // acquire a thread, but we can cancel the controlling job so once it gets acquired it
            // is quickly released.
            controlJob.cancel()
        }
        try {
            execute {
                runBlocking {
                    // Thread acquired, resume coroutine.
                    continuation.resume(coroutineContext[ContinuationInterceptor]!!)
                    controlJob.join()
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
}
/**
 * A [CoroutineContext.Element] that indicates there is an on-going database transaction.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TransactionElement(
    private val transactionThreadControlJob: Job,
    internal val transactionDispatcher: ContinuationInterceptor
) : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<TransactionElement>

    override val key: CoroutineContext.Key<TransactionElement>
        get() = TransactionElement

    /**
     * Number of transactions (including nested ones) started with this element.
     * Call [acquire] to increase the count and [release] to decrease it. If the count reaches zero
     * when [release] is invoked then the transaction job is cancelled and the transaction thread
     * is released.
     */
    private val referenceCount = AtomicInteger(0)

    fun acquire() {
        referenceCount.incrementAndGet()
    }

    fun release() {
        val count = referenceCount.decrementAndGet()
        if (count < 0) {
            throw IllegalStateException("Transaction was never started or was already released.")
        } else if (count == 0) {
            // Cancel the job that controls the transaction thread, causing it to be released.
            transactionThreadControlJob.cancel()
        }
    }
}