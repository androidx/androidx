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

package androidx.room.support

import android.database.sqlite.SQLiteTransactionListener
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.room.RoomDatabase
import androidx.room.Transactor
import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.getOrSet
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal class RoomSupportSQLiteSession(val roomDatabase: RoomDatabase) {
    private val isClosed = AtomicBoolean(false)

    // The thread confined transaction object. If this thread local is set, then there is an active
    // transaction in this thread and a coroutine holding the connection with the transaction is
    // active and awaiting for the transaction to end.
    private val threadTransaction = ThreadLocal<ThreadTransaction>()

    private val threadTransactionContext: ThreadLocal<CoroutineContext>
        get() = roomDatabase.suspendingTransactionContext

    private val threadWaiters = AtomicInteger(0)

    val path: String?
        get() = roomDatabase.path

    /** Use a reader connection blocking. */
    fun <T> useReaderBlocking(block: suspend (Transactor) -> T) =
        useConnectionBlocking(isReadOnly = true, block)

    /** Use a writer connection blocking. */
    fun <T> useWriterBlocking(block: suspend (Transactor) -> T) =
        useConnectionBlocking(isReadOnly = false, block)

    /**
     * Use a database connection blocking. If there is an active transaction in this thread, then
     * the transaction connection will be re-used.
     */
    private fun <T> useConnectionBlocking(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> T,
    ): T {
        val context = roomDatabase.suspendingTransactionContext.get() ?: EmptyCoroutineContext
        return runBlockingNonInterruptible(context) {
            roomDatabase.useConnection(isReadOnly, block)
        }
    }

    /** A version of [runBlocking] that is not interruptible. */
    private fun <T> runBlockingNonInterruptible(
        context: CoroutineContext,
        action: suspend CoroutineScope.() -> T,
    ): T {
        val result = AtomicReference<Result<T>>()
        try {
            runBlocking(context) {
                withContext(NonCancellable) { result.set(runCatching { action() }) }
            }
        } catch (_: InterruptedException) {
            // Restore the interrupted flag
            Thread.currentThread().interrupt()
        }
        return result.get().getOrThrow()
    }

    fun beginTransaction(type: SQLiteTransactionType, listener: SQLiteTransactionListener? = null) {
        val currentTransaction =
            threadTransaction.getOrSet {
                try {
                    threadWaiters.incrementAndGet()
                    ThreadTransaction.acquire(roomDatabase, type).also {
                        threadTransaction.set(it)
                        threadTransactionContext.set(it.getCoroutineContext())
                    }
                } finally {
                    threadWaiters.decrementAndGet()
                }
            }
        currentTransaction.begin(listener)
        try {
            currentTransaction.listener?.onBegin()
        } catch (ex: Throwable) {
            currentTransaction.mark(success = false)
            endTransaction()
            throw ex
        }
    }

    fun endTransaction() {
        val currentTransaction =
            threadTransaction.get()
                ?: error("Cannot perform this operation because there is no current transaction.")
        try {
            if (currentTransaction.isSuccessful()) {
                currentTransaction.listener?.onCommit()
            } else {
                currentTransaction.listener?.onRollback()
            }
        } catch (ex: Throwable) {
            currentTransaction.mark(success = false)
            throw ex
        } finally {
            val transactionEnded = currentTransaction.end()
            if (transactionEnded) {
                threadTransactionContext.set(null)
                threadTransaction.set(null)
            }
        }
    }

    fun setTransactionSuccessful() {
        val currentTransaction =
            threadTransaction.get()
                ?: error("Cannot perform this operation because there is no current transaction.")
        check(!currentTransaction.isMarkedSuccessful) {
            "Cannot perform this operation because the transaction has already been marked" +
                "successful."
        }
        currentTransaction.mark(success = true)
    }

    fun inTransaction(): Boolean {
        return threadTransaction.get() != null
    }

    fun yieldIfContended(sleepAfterYieldDelayMillis: Long = 0): Boolean {
        val currentTransaction =
            threadTransaction.get()
                ?: error("Cannot perform this operation because there is no current transaction.")
        check(!currentTransaction.isNested) {
            "Cannot perform this operation because a nested transaction is in progress."
        }
        check(!currentTransaction.isMarkedSuccessful) {
            "Cannot perform this operation because the transaction has already been marked" +
                "successful."
        }

        // TODO: Consider tracking reader vs writer waiters separately
        if (threadWaiters.get() == 0) {
            return false
        }

        val transactionType = currentTransaction.type
        val transactionListener = currentTransaction.listener
        setTransactionSuccessful()
        endTransaction()
        try {
            @Suppress("BanThreadSleep") // Intended sleep to yield for other transactions
            Thread.sleep(sleepAfterYieldDelayMillis)
        } catch (_: InterruptedException) {
            // got interrupted
        }
        beginTransaction(transactionType, transactionListener)
        return true
    }

    // TODO(b/409102321): Sync the isOpen / isClosed state with RoomDatabase
    fun isClosed(): Boolean {
        return isClosed.get()
    }

    fun close() {
        if (isClosed.compareAndSet(false, true)) {
            threadTransaction.get()?.end()
            roomDatabase.close()
        }
    }
}

/**
 * An object representing a database transaction.
 *
 * The transaction itself is an active coroutine to which database operations are dispatched and is
 * started via [RoomSupportSQLiteSession.beginTransaction]. Once a transaction is ended via
 * [RoomSupportSQLiteSession.endTransaction] the coroutine is finished which marks the end of the
 * transaction.
 *
 * @property completable A completable to finish the transaction, the value will determine if the
 *   transaction is committed or rolled back.
 */
private class ThreadTransaction
private constructor(
    val type: SQLiteTransactionType,
    private val connectionContext: CoroutineContext,
    private val completable: TransactionCompletable,
) {
    private val stack = ArrayDeque<TransactionItem>()

    val isMarkedSuccessful: Boolean
        get() {
            check(stack.isNotEmpty())
            return stack[stack.lastIndex].markedSuccessful
        }

    val isNested: Boolean
        get() {
            check(stack.isNotEmpty())
            return stack.size > 1
        }

    val listener: SQLiteTransactionListener?
        get() {
            check(stack.isNotEmpty())
            return stack[stack.lastIndex].listener
        }

    fun getCoroutineContext(): CoroutineContext {
        return connectionContext
    }

    fun begin(listener: SQLiteTransactionListener?) {
        stack.addLast(TransactionItem(listener))
    }

    fun mark(success: Boolean) {
        check(stack.isNotEmpty())
        stack[stack.lastIndex].markedSuccessful = success
    }

    fun end(): Boolean {
        check(stack.isNotEmpty())
        val item = stack.removeLast()
        val successful = item.markedSuccessful && !item.childFailed
        if (stack.isEmpty()) {
            completable.complete(successful)
            return true
        } else {
            stack[stack.lastIndex].childFailed = !successful
            return false
        }
    }

    fun isSuccessful(): Boolean {
        check(stack.isNotEmpty())
        val item = stack[stack.lastIndex]
        return item.markedSuccessful && !item.childFailed
    }

    /**
     * A [CompletableDeferred] that also awaits for the [latch] which is only released when the
     * transaction coroutine is completed.
     *
     * @property delegate The actual [CompletableDeferred] that once completed will release the
     *   active transaction coroutine.
     * @property latch The latch to await for the completion of the transaction coroutine.
     */
    private class TransactionCompletable(
        private val delegate: CompletableDeferred<Boolean>,
        private val latch: CountDownLatch,
    ) : CompletableDeferred<Boolean> by delegate {
        override fun complete(value: Boolean): Boolean {
            check(delegate.complete(value))
            latch.await()
            return true
        }
    }

    /**
     * An object that represent a transaction in the stack of nested transactions. All child nested
     * transactions must complete successfully for the actual transaction to be committed.
     */
    // TODO(b/408061556): Use and implement SQLiteTransactionListener
    private class TransactionItem(val listener: SQLiteTransactionListener?) {
        var markedSuccessful = false
        var childFailed = false
    }

    companion object {
        fun acquire(db: RoomDatabase, type: SQLiteTransactionType): ThreadTransaction {
            val transactionFuture =
                CallbackToFutureAdapter.getFuture { completer ->
                    // a latch to wait (blocking) for the coroutine to finish
                    val completionLatch = CountDownLatch(1)
                    val transactionBlock: suspend (Transactor) -> Unit = {
                        it.withTransaction(type) {
                            // a completable to commit or rollback the transaction
                            val successSignal = CompletableDeferred<Boolean>()
                            // the transaction object that will be stored in a thread local
                            val threadTransaction =
                                ThreadTransaction(
                                    type = type,
                                    connectionContext = coroutineContext,
                                    completable =
                                        TransactionCompletable(successSignal, completionLatch),
                                )
                            completer.set(threadTransaction)
                            val success = successSignal.await()
                            if (!success) {
                                rollback(Unit)
                            }
                        }
                    }
                    // launch a transaction coroutine that will be kept alive until the transaction
                    // object ends.
                    db.getCoroutineScope()
                        .launch(CoroutineName("RoomSupportSQLiteTransaction")) {
                            if (type == SQLiteTransactionType.DEFERRED) {
                                db.useReaderConnection(transactionBlock)
                            } else {
                                db.useWriterConnection(transactionBlock)
                            }
                        }
                        .invokeOnCompletion { error ->
                            if (error != null) {
                                completer.setException(error)
                            }
                            completionLatch.countDown()
                        }
                }
            try {
                // wait for the transaction coroutine to be started, this includes waiting for an
                // available database connection to be acquired plus the start of the transaction
                // on the connection.
                return transactionFuture.get()
            } catch (e: ExecutionException) {
                throw e.cause ?: IllegalStateException("Failed to begin transaction")
            }
        }
    }
}
