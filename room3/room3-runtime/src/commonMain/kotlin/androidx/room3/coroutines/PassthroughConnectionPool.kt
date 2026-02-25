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

import androidx.room3.TransactionScope
import androidx.room3.Transactor
import androidx.room3.concurrent.AtomicInt
import androidx.room3.concurrent.ReentrantLock
import androidx.room3.concurrent.currentThreadId
import androidx.room3.util.PlatformType
import androidx.room3.util.SQLiteResultCode.SQLITE_MISUSE
import androidx.room3.util.platform
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.executeSQL
import androidx.sqlite.prepare
import androidx.sqlite.throwSQLiteException
import kotlin.concurrent.Volatile
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal typealias TransactionWrapper<T> = suspend (suspend () -> T) -> T

/**
 * An implementation of a connection pool that doesn't do any connection management.
 *
 * Used in two scenarios:
 * * When Room is in compatibility mode (no driver is provided to Room).
 * * When a driver that reports having a connection pool (returns `true` from
 *   [SQLiteDriver.hasConnectionPool]) is provided to Room.
 */
internal class PassthroughConnectionPool(
    private val connectionFactory: ConnectionFactory,
    private val transactionWrapper: TransactionWrapper<Any?>? = null,
) : ConnectionPool {

    private val lock = ReentrantLock()
    private val mutex = Mutex()
    private lateinit var connection: SQLiteConnection

    @Volatile private var isClosed = false

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R,
    ): R {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "Connection pool is closed")
        }
        val confinedConnection = currentCoroutineContext()[ConnectionElement]?.connectionWrapper
        if (confinedConnection != null) {
            return block.invoke(confinedConnection)
        }
        if (!::connection.isInitialized) {
            withInitializationLock {
                if (!::connection.isInitialized) {
                    connection = connectionFactory.invoke()
                    if (isClosed) {
                        // Pool was closed in-between opening a new connection, close it and throw.
                        connection.close()
                        throwSQLiteException(SQLITE_MISUSE, "Connection pool is closed")
                    }
                }
            }
        }
        val connectionWrapper = PassthroughConnection(transactionWrapper, connection)
        return withContext(ConnectionElement(connectionWrapper)) { block.invoke(connectionWrapper) }
    }

    /**
     * Executes the connection initialization [block] around a lock.
     *
     * For most situations this simply uses the [mutex] to protect connection initialization.
     * However, it also contains protection logic to specifically support Room's SupportSQLite
     * wrapper and its ability to avoid thread hops during a SupportSQLite transaction that is
     * thread confined and backed by a SupportSQLite driver.
     *
     * TODO(b/441117897): Consider removing this cross-module implicit logic.
     */
    private suspend fun withInitializationLock(block: suspend () -> Unit) {
        if (
            platform == PlatformType.ANDROID &&
                currentCoroutineContext()[ContinuationInterceptor] === Dispatchers.Unconfined &&
                currentCoroutineContext()[CoroutineName]?.name == "RoomSupportSQLiteTransaction"
        ) {
            val lockThreadId = currentThreadId()
            lock.lock()
            try {
                return block()
            } finally {
                val unlockThreadId = currentThreadId()
                check(lockThreadId == unlockThreadId) {
                    "Resumed on a different thread during connection pool initialization. This can " +
                        "occur when Room's SupportSQLite wrapper is used with a suspending " +
                        "driver. Please report this issue at $BUG_LINK."
                }
                lock.unlock()
            }
        } else {
            mutex.withReentrantLock { block() }
        }
    }

    override fun close() {
        isClosed = true
        if (::connection.isInitialized) {
            connection.close()
        }
    }

    private class ConnectionElement(val connectionWrapper: PassthroughConnection) :
        CoroutineContext.Element {
        companion object Key : CoroutineContext.Key<ConnectionElement>

        override val key: CoroutineContext.Key<ConnectionElement>
            get() = ConnectionElement
    }

    private companion object {
        const val BUG_LINK =
            "https://issuetracker.google.com/issues/new?component=413107&template=1096568"
    }
}

private class PassthroughConnection(
    val transactionWrapper: TransactionWrapper<Any?>?,
    val delegate: SQLiteConnection,
) : Transactor, RawConnectionAccessor {

    private var nestedTransactionCount = AtomicInt(0)
    private var currentTransactionType: Transactor.SQLiteTransactionType? = null

    override suspend fun <R> useRawConnection(block: suspend (SQLiteConnection) -> R): R {
        return block.invoke(delegate)
    }

    override suspend fun <R> usePrepared(sql: String, block: suspend (SQLiteStatement) -> R): R {
        return if (inTransaction() && transactionWrapper != null) {
            @Suppress("UNCHECKED_CAST") // Safe to cast since it just pipes the result
            transactionWrapper.invoke { delegate.prepare(sql).use { block.invoke(it) } } as R
        } else {
            delegate.prepare(sql).use { block.invoke(it) }
        }
    }

    override suspend fun <R> withTransaction(
        type: Transactor.SQLiteTransactionType,
        block: suspend TransactionScope<R>.() -> R,
    ): R {
        return if (transactionWrapper != null) {
            @Suppress("UNCHECKED_CAST") // Safe to cast since it just pipes the result
            transactionWrapper.invoke { transaction(type, block) } as R
        } else {
            transaction(type, block)
        }
    }

    private suspend fun <R> transaction(
        type: Transactor.SQLiteTransactionType,
        block: suspend TransactionScope<R>.() -> R,
    ): R {
        when (type) {
            Transactor.SQLiteTransactionType.DEFERRED ->
                delegate.executeSQL("BEGIN DEFERRED TRANSACTION")
            Transactor.SQLiteTransactionType.IMMEDIATE ->
                delegate.executeSQL("BEGIN IMMEDIATE TRANSACTION")
            Transactor.SQLiteTransactionType.EXCLUSIVE ->
                delegate.executeSQL("BEGIN EXCLUSIVE TRANSACTION")
        }
        if (nestedTransactionCount.incrementAndGet() > 0) {
            currentTransactionType = type
        }
        var success = true
        var exception: Throwable? = null
        try {
            return PassthroughTransactor<R>().block()
        } catch (ex: Throwable) {
            success = false
            if (ex is ConnectionPool.RollbackException) {
                @Suppress("UNCHECKED_CAST")
                return (ex.result as R)
            } else {
                exception = ex
                throw ex
            }
        } finally {
            try {
                if (nestedTransactionCount.decrementAndGet() == 0) {
                    currentTransactionType = null
                }
                if (success) {
                    delegate.executeSQL("END TRANSACTION")
                } else {
                    delegate.executeSQL("ROLLBACK TRANSACTION")
                }
            } catch (ex: SQLiteException) {
                exception?.addSuppressed(ex) ?: throw ex
            }
        }
    }

    override suspend fun inTransaction(): Boolean {
        return currentTransactionType != null || delegate.inTransaction()
    }

    private inner class PassthroughTransactor<T> : TransactionScope<T>, RawConnectionAccessor {

        override suspend fun <R> useRawConnection(block: suspend (SQLiteConnection) -> R): R =
            this@PassthroughConnection.useRawConnection(block)

        override suspend fun <R> usePrepared(
            sql: String,
            block: suspend (SQLiteStatement) -> R,
        ): R {
            return this@PassthroughConnection.usePrepared(sql, block)
        }

        override suspend fun <R> withNestedTransaction(
            block: suspend (TransactionScope<R>) -> R
        ): R {
            return transaction(checkNotNull(currentTransactionType), block)
        }

        override suspend fun rollback(result: T): Nothing {
            throw ConnectionPool.RollbackException(result)
        }
    }
}
