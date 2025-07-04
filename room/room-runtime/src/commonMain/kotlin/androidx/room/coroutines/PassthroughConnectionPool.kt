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

package androidx.room.coroutines

import androidx.room.TransactionScope
import androidx.room.Transactor
import androidx.room.concurrent.AtomicInt
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
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
    private val driver: SQLiteDriver,
    private val fileName: String,
    private val transactionWrapper: TransactionWrapper<*>? = null,
) : ConnectionPool {

    private val connection = lazy { driver.open(fileName) }

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R,
    ): R {
        val confinedConnection = coroutineContext[ConnectionElement]?.connectionWrapper
        if (confinedConnection != null) {
            return block.invoke(confinedConnection)
        }

        val connectionWrapper = PassthroughConnection(transactionWrapper, connection.value)
        return withContext(ConnectionElement(connectionWrapper)) { block.invoke(connectionWrapper) }
    }

    override fun close() {
        if (connection.isInitialized()) {
            connection.value.close()
        }
    }

    private class ConnectionElement(val connectionWrapper: PassthroughConnection) :
        CoroutineContext.Element {
        companion object Key : CoroutineContext.Key<ConnectionElement>

        override val key: CoroutineContext.Key<ConnectionElement>
            get() = ConnectionElement
    }
}

private class PassthroughConnection(
    val transactionWrapper: TransactionWrapper<*>?,
    val delegate: SQLiteConnection,
) : Transactor, RawConnectionAccessor {

    private var nestedTransactionCount = AtomicInt(0)
    private var currentTransactionType: Transactor.SQLiteTransactionType? = null

    override val rawConnection: SQLiteConnection
        get() = delegate

    override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R {
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
                delegate.execSQL("BEGIN DEFERRED TRANSACTION")
            Transactor.SQLiteTransactionType.IMMEDIATE ->
                delegate.execSQL("BEGIN IMMEDIATE TRANSACTION")
            Transactor.SQLiteTransactionType.EXCLUSIVE ->
                delegate.execSQL("BEGIN EXCLUSIVE TRANSACTION")
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
                    delegate.execSQL("END TRANSACTION")
                } else {
                    delegate.execSQL("ROLLBACK TRANSACTION")
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

        override val rawConnection: SQLiteConnection
            get() = this@PassthroughConnection.rawConnection

        override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R {
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
