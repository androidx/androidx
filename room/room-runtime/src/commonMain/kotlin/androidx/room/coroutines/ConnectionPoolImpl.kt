/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.concurrent.ThreadLocal
import androidx.room.concurrent.asContextElement
import androidx.room.concurrent.currentThreadId
import androidx.room.util.SQLiteResultCode.SQLITE_BUSY
import androidx.room.util.SQLiteResultCode.SQLITE_ERROR
import androidx.room.util.SQLiteResultCode.SQLITE_MISUSE
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import androidx.sqlite.throwSQLiteException
import androidx.sqlite.use
import kotlin.collections.removeLast as removeLastKt
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class ConnectionPoolImpl : ConnectionPool {
    private val driver: SQLiteDriver
    private val readers: Pool
    private val writers: Pool

    private val threadLocal = ThreadLocal<PooledConnectionImpl>()

    private val _isClosed = atomic(false)
    private val isClosed by _isClosed

    // Amount of time to wait to acquire a connection before throwing, Android uses 30 seconds in
    // its pool, so we do too here, but IDK if that is a good number. This timeout is unrelated to
    // the busy handler.
    // TODO: Allow configuration
    private val timeout = 30.seconds

    constructor(driver: SQLiteDriver, fileName: String) {
        this.driver = driver
        this.readers = Pool(capacity = 1, connectionFactory = { driver.open(fileName) })
        this.writers = readers
    }

    constructor(
        driver: SQLiteDriver,
        fileName: String,
        maxNumOfReaders: Int,
        maxNumOfWriters: Int,
    ) {
        require(maxNumOfReaders > 0) { "Maximum number of readers must be greater than 0" }
        require(maxNumOfWriters > 0) { "Maximum number of writers must be greater than 0" }
        this.driver = driver
        this.readers =
            Pool(
                capacity = maxNumOfReaders,
                connectionFactory = {
                    driver.open(fileName).also { newConnection ->
                        // Enforce to be read only (might be disabled by a YOLO developer)
                        newConnection.execSQL("PRAGMA query_only = 1")
                    }
                }
            )
        this.writers =
            Pool(capacity = maxNumOfWriters, connectionFactory = { driver.open(fileName) })
    }

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R
    ): R {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "Connection pool is closed")
        }
        val confinedConnection =
            threadLocal.get() ?: coroutineContext[ConnectionElement]?.connectionWrapper
        if (confinedConnection != null) {
            if (!isReadOnly && confinedConnection.isReadOnly) {
                throwSQLiteException(
                    SQLITE_ERROR,
                    "Cannot upgrade connection from reader to writer"
                )
            }
            return if (coroutineContext[ConnectionElement] == null) {
                // Reinstall the connection context element if it is missing. We are likely in
                // a new coroutine but were able to transfer the connection via the thread local.
                withContext(createConnectionContext(confinedConnection)) {
                    block.invoke(confinedConnection)
                }
            } else {
                block.invoke(confinedConnection)
            }
        }
        val pool =
            if (isReadOnly) {
                readers
            } else {
                writers
            }
        val result: R
        var exception: Throwable? = null
        var connection: PooledConnectionImpl? = null
        try {
            val (acquiredConnection, acquireError) = pool.acquireWithTimeout()
            // Always try to create a wrapper even if an error occurs, so it can be recycled.
            connection =
                acquiredConnection?.let {
                    PooledConnectionImpl(
                        delegate = it.markAcquired(coroutineContext),
                        isReadOnly = readers !== writers && isReadOnly
                    )
                }
            if (acquireError is TimeoutCancellationException) {
                throwTimeoutException(isReadOnly)
            } else if (acquireError != null) {
                throw acquireError
            }
            requireNotNull(connection)
            result = withContext(createConnectionContext(connection)) { block.invoke(connection) }
        } catch (ex: Throwable) {
            exception = ex
            throw ex
        } finally {
            try {
                connection?.let { usedConnection ->
                    usedConnection.markRecycled()
                    pool.recycle(usedConnection.delegate)
                }
            } catch (error: Throwable) {
                exception?.addSuppressed(error)
            }
        }
        return result
    }

    private suspend inline fun Pool.acquireWithTimeout(): Pair<ConnectionWithLock?, Throwable?> {
        // Following async timeout with resources recommendation:
        // https://kotlinlang.org/docs/cancellation-and-timeouts.html#asynchronous-timeout-and-resources
        var connection: ConnectionWithLock? = null
        var exceptionThrown: Throwable? = null
        try {
            withTimeout(timeout) { connection = this@acquireWithTimeout.acquire() }
        } catch (ex: Throwable) {
            exceptionThrown = ex
        }
        return connection to exceptionThrown
    }

    private fun createConnectionContext(connection: PooledConnectionImpl) =
        ConnectionElement(connection) + threadLocal.asContextElement(connection)

    private fun throwTimeoutException(isReadOnly: Boolean): Nothing {
        val readOrWrite = if (isReadOnly) "reader" else "writer"
        val message = buildString {
            appendLine("Timed out attempting to acquire a $readOrWrite connection.")
            appendLine()
            appendLine("Writer pool:")
            writers.dump(this)
            appendLine("Reader pool:")
            readers.dump(this)
        }
        throwSQLiteException(SQLITE_BUSY, message)
    }

    // TODO: (b/319657104): Make suspending so pool closes when all connections are recycled.
    override fun close() {
        if (_isClosed.compareAndSet(expect = false, update = true)) {
            readers.close()
            writers.close()
        }
    }
}

private class Pool(val capacity: Int, val connectionFactory: () -> SQLiteConnection) {
    private val size = atomic(0)
    private val connections = arrayOfNulls<ConnectionWithLock>(capacity)
    private val channel =
        Channel<ConnectionWithLock>(capacity = capacity, onUndeliveredElement = { recycle(it) })

    suspend fun acquire(): ConnectionWithLock {
        val receiveResult = channel.tryReceive()
        return if (receiveResult.isSuccess) {
            receiveResult.getOrThrow()
        } else {
            tryOpenNewConnection()
            channel.receive()
        }
    }

    private fun tryOpenNewConnection() {
        val currentSize = size.value
        if (currentSize >= capacity) {
            // Capacity reached
            return
        }
        if (size.compareAndSet(currentSize, currentSize + 1)) {
            val newConnection = ConnectionWithLock(connectionFactory.invoke())
            val sendResult = channel.trySend(newConnection)
            if (sendResult.isSuccess) {
                connections[currentSize] = newConnection
            } else {
                newConnection.close()
                if (!sendResult.isClosed) {
                    // Failed to send but channel is not closed, this means a race condition with
                    // the size and capacity checks.
                    error("Couldn't send a new connection for acquisition")
                }
            }
        } else {
            // Another thread went ahead and created a new connection, try again
            tryOpenNewConnection()
        }
    }

    fun recycle(connection: ConnectionWithLock) {
        val sendResult = channel.trySend(connection)
        if (!sendResult.isSuccess) {
            connection.close()
            if (!sendResult.isClosed) {
                // Failed to send but channel is not closed. Likely a race condition...
                // did open connections exceeded capacity? Maybe a `finally` block didn't run?
                error("Couldn't recycle connection")
            }
        }
    }

    fun close() {
        channel.close()
        connections.forEach { it?.close() }
    }

    /* Dumps debug information */
    fun dump(builder: StringBuilder) {
        builder.appendLine("\t" + super.toString() + " (capacity=$capacity)")
        connections.forEachIndexed { index, connection ->
            builder.appendLine("\t\t[${index + 1}] - ${connection?.toString()}")
            connection?.dump(builder)
        }
    }
}

private class ConnectionWithLock(
    private val delegate: SQLiteConnection,
    private val lock: Mutex = Mutex()
) : SQLiteConnection by delegate, Mutex by lock {

    private var acquireCoroutineContext: CoroutineContext? = null
    private var acquireThrowable: Throwable? = null

    fun markAcquired(context: CoroutineContext) = apply {
        acquireCoroutineContext = context
        acquireThrowable = Throwable()
    }

    fun markReleased() = apply {
        acquireCoroutineContext = null
        acquireThrowable = null
    }

    /* Dumps debug information */
    fun dump(builder: StringBuilder) {
        if (acquireCoroutineContext != null || acquireThrowable != null) {
            builder.appendLine("\t\tStatus: Acquired connection")
            acquireCoroutineContext?.let { builder.appendLine("\t\tCoroutine: $it") }
            acquireThrowable?.let {
                builder.appendLine("\t\tAcquired:")
                it.stackTraceToString().lines().drop(1).forEach { line ->
                    builder.appendLine("\t\t$line")
                }
            }
        } else {
            builder.appendLine("\t\tStatus: Free connection")
        }
    }

    override fun toString(): String {
        return delegate.toString()
    }
}

private class ConnectionElement(val connectionWrapper: PooledConnectionImpl) :
    CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<ConnectionElement>

    override val key: CoroutineContext.Key<ConnectionElement>
        get() = ConnectionElement
}

/**
 * A connection wrapper to enforce pool contract and implement transactions.
 *
 * Actual connection interactions are serialized via a limited dispatcher, specifically compiling a
 * statement and using it is serialized as to prevent a coroutine from concurrently using the
 * statement between multiple different threads.
 */
private class PooledConnectionImpl(
    val delegate: ConnectionWithLock,
    val isReadOnly: Boolean,
) : Transactor, RawConnectionAccessor {
    private val transactionStack = ArrayDeque<TransactionItem>()

    private val _isRecycled = atomic(false)
    private val isRecycled by _isRecycled

    override val rawConnection: SQLiteConnection
        get() = delegate

    override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R =
        withStateCheck {
            return delegate.withLock {
                StatementWrapper(delegate.prepare(sql)).use { block.invoke(it) }
            }
        }

    override suspend fun <R> withTransaction(
        type: SQLiteTransactionType,
        block: suspend TransactionScope<R>.() -> R
    ): R = withStateCheck { transaction(type, block) }

    override suspend fun inTransaction(): Boolean = withStateCheck {
        return transactionStack.isNotEmpty()
    }

    fun markRecycled() {
        delegate.markReleased()
        if (_isRecycled.compareAndSet(expect = false, update = true)) {
            // Perform a rollback in case there is an active transaction so that the connection
            // is in a clean state when it is recycled. We don't know for sure if there is an
            // unfinished transaction, hence we always try the rollback.
            // TODO(b/319627988): Try to *really* check if there is an active transaction with the
            //     C APIs sqlite3_txn_state or sqlite3_get_autocommit and possibly throw an error
            //     if there is an unfinished transaction.
            try {
                delegate.execSQL("ROLLBACK TRANSACTION")
            } catch (_: SQLiteException) {
                // ignored
            }
        }
    }

    private suspend fun <R> transaction(
        type: SQLiteTransactionType?,
        block: suspend TransactionScope<R>.() -> R
    ): R {
        beginTransaction(type ?: SQLiteTransactionType.DEFERRED)
        var success = true
        var exception: Throwable? = null
        try {
            return TransactionImpl<R>().block()
        } catch (ex: Throwable) {
            success = false
            if (ex is ConnectionPool.RollbackException) {
                // Type arguments in exception subclasses is not allowed but the exception is always
                // created with the correct type.
                @Suppress("UNCHECKED_CAST") return (ex.result as R)
            } else {
                exception = ex
                throw ex
            }
        } finally {
            try {
                endTransaction(success)
            } catch (ex: SQLiteException) {
                exception?.addSuppressed(ex) ?: throw ex
            }
        }
    }

    private suspend fun beginTransaction(type: SQLiteTransactionType) =
        delegate.withLock {
            val newTransactionId = transactionStack.size
            if (transactionStack.isEmpty()) {
                when (type) {
                    SQLiteTransactionType.DEFERRED -> delegate.execSQL("BEGIN DEFERRED TRANSACTION")
                    SQLiteTransactionType.IMMEDIATE ->
                        delegate.execSQL("BEGIN IMMEDIATE TRANSACTION")
                    SQLiteTransactionType.EXCLUSIVE ->
                        delegate.execSQL("BEGIN EXCLUSIVE TRANSACTION")
                }
            } else {
                delegate.execSQL("SAVEPOINT '$newTransactionId'")
            }
            transactionStack.addLast(TransactionItem(id = newTransactionId, shouldRollback = false))
        }

    private suspend fun endTransaction(success: Boolean) =
        delegate.withLock {
            if (transactionStack.isEmpty()) {
                error("Not in a transaction")
            }
            val transaction = transactionStack.removeLastKt()
            if (success && !transaction.shouldRollback) {
                if (transactionStack.isEmpty()) {
                    delegate.execSQL("END TRANSACTION")
                } else {
                    delegate.execSQL("RELEASE SAVEPOINT '${transaction.id}'")
                }
            } else {
                if (transactionStack.isEmpty()) {
                    delegate.execSQL("ROLLBACK TRANSACTION")
                } else {
                    delegate.execSQL("ROLLBACK TRANSACTION TO SAVEPOINT '${transaction.id}'")
                }
            }
        }

    private class TransactionItem(val id: Int, var shouldRollback: Boolean)

    private inner class TransactionImpl<T> : TransactionScope<T>, RawConnectionAccessor {

        override val rawConnection: SQLiteConnection
            get() = this@PooledConnectionImpl.rawConnection

        override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R =
            this@PooledConnectionImpl.usePrepared(sql, block)

        override suspend fun <R> withNestedTransaction(
            block: suspend (TransactionScope<R>) -> R
        ): R = withStateCheck { transaction(null, block) }

        override suspend fun rollback(result: T): Nothing = withStateCheck {
            if (transactionStack.isEmpty()) {
                error("Not in a transaction")
            }
            delegate.withLock { transactionStack.last().shouldRollback = true }
            throw ConnectionPool.RollbackException(result)
        }
    }

    private suspend inline fun <R> withStateCheck(block: () -> R): R {
        if (isRecycled) {
            throwSQLiteException(SQLITE_MISUSE, "Connection is recycled")
        }
        val connectionElement = coroutineContext[ConnectionElement]
        if (connectionElement == null || connectionElement.connectionWrapper !== this) {
            throwSQLiteException(
                SQLITE_MISUSE,
                "Attempted to use connection on a different coroutine"
            )
        }
        return block.invoke()
    }

    private inner class StatementWrapper(
        private val delegate: SQLiteStatement,
    ) : SQLiteStatement {

        private val threadId = currentThreadId()

        override fun bindBlob(index: Int, value: ByteArray): Unit = withStateCheck {
            delegate.bindBlob(index, value)
        }

        override fun bindDouble(index: Int, value: Double): Unit = withStateCheck {
            delegate.bindDouble(index, value)
        }

        override fun bindLong(index: Int, value: Long): Unit = withStateCheck {
            delegate.bindLong(index, value)
        }

        override fun bindText(index: Int, value: String): Unit = withStateCheck {
            delegate.bindText(index, value)
        }

        override fun bindNull(index: Int): Unit = withStateCheck { delegate.bindNull(index) }

        override fun getBlob(index: Int): ByteArray = withStateCheck { delegate.getBlob(index) }

        override fun getDouble(index: Int): Double = withStateCheck { delegate.getDouble(index) }

        override fun getLong(index: Int): Long = withStateCheck { delegate.getLong(index) }

        override fun getText(index: Int): String = withStateCheck { delegate.getText(index) }

        override fun isNull(index: Int): Boolean = withStateCheck { delegate.isNull(index) }

        override fun getColumnCount(): Int = withStateCheck { delegate.getColumnCount() }

        override fun getColumnName(index: Int) = withStateCheck { delegate.getColumnName(index) }

        override fun getColumnType(index: Int) = withStateCheck { delegate.getColumnType(index) }

        override fun step(): Boolean = withStateCheck { delegate.step() }

        override fun reset() = withStateCheck { delegate.reset() }

        override fun clearBindings() = withStateCheck { delegate.clearBindings() }

        override fun close() = withStateCheck { delegate.close() }

        private inline fun <R> withStateCheck(block: () -> R): R {
            if (isRecycled) {
                throwSQLiteException(SQLITE_MISUSE, "Statement is recycled")
            }
            if (threadId != currentThreadId()) {
                throwSQLiteException(
                    SQLITE_MISUSE,
                    "Attempted to use statement on a different thread"
                )
            }
            return block.invoke()
        }
    }
}
