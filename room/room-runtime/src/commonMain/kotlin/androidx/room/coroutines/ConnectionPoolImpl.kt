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

import androidx.collection.CircularArray
import androidx.room.TransactionScope
import androidx.room.Transactor
import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.concurrent.AtomicBoolean
import androidx.room.concurrent.ReentrantLock
import androidx.room.concurrent.ThreadLocal
import androidx.room.concurrent.asContextElement
import androidx.room.concurrent.currentThreadId
import androidx.room.concurrent.withLock
import androidx.room.util.SQLiteResultCode.SQLITE_BUSY
import androidx.room.util.SQLiteResultCode.SQLITE_ERROR
import androidx.room.util.SQLiteResultCode.SQLITE_MISUSE
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import androidx.sqlite.throwSQLiteException
import kotlin.collections.removeLast as removeLastKt
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class ConnectionPoolImpl : ConnectionPool {
    private val driver: SQLiteDriver
    private val readers: Pool
    private val writers: Pool

    private val threadLocal = ThreadLocal<PooledConnectionImpl>()

    private val _isClosed = AtomicBoolean(false)
    private val isClosed: Boolean
        get() = _isClosed.get()

    // Amount of time to wait to acquire a connection before logging, Android uses 30 seconds in
    // its pool, so we do too here, but IDK if that is a good number. This timeout is unrelated
    // to the busy handler.
    // TODO(b/404380974): Allow public configuration
    internal var timeout = 30.seconds
    internal var throwOnTimeout = false

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
                },
            )
        this.writers =
            Pool(capacity = maxNumOfWriters, connectionFactory = { driver.open(fileName) })
    }

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R,
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
                    "Cannot upgrade connection from reader to writer",
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
            val currentContext = coroutineContext
            connection =
                PooledConnectionImpl(
                    delegate =
                        pool
                            .acquireWithTimeout(timeout) { onTimeout(isReadOnly) }
                            .markAcquired(currentContext),
                    isReadOnly = readers !== writers && isReadOnly,
                )
            requireNotNull(connection)
            result = withContext(createConnectionContext(connection)) { block.invoke(connection) }
        } catch (ex: Throwable) {
            exception = ex
            throw ex
        } finally {
            try {
                connection?.let { usedConnection ->
                    usedConnection.markRecycled()
                    usedConnection.delegate.markReleased()
                    pool.recycle(usedConnection.delegate)
                }
            } catch (recycleException: Throwable) {
                exception?.addSuppressed(recycleException) ?: throw recycleException
            }
        }
        return result
    }

    private fun createConnectionContext(connection: PooledConnectionImpl) =
        ConnectionElement(connection) + threadLocal.asContextElement(connection)

    private fun onTimeout(isReadOnly: Boolean) {
        val readOrWrite = if (isReadOnly) "reader" else "writer"
        val message = buildString {
            appendLine("Timed out attempting to acquire a $readOrWrite connection.")
            appendLine()
            appendLine("Writer pool:")
            writers.dump(this)
            appendLine("Reader pool:")
            readers.dump(this)
        }
        try {
            throwSQLiteException(SQLITE_BUSY, message)
        } catch (ex: SQLiteException) {
            if (throwOnTimeout) {
                throw ex
            } else {
                ex.printStackTrace()
            }
        }
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
    private val lock = ReentrantLock()
    private var size = 0
    private var isClosed = false
    private val connections = arrayOfNulls<ConnectionWithLock>(capacity)
    private val connectionPermits = Semaphore(permits = capacity)
    private val availableConnections = CircularArray<ConnectionWithLock>(capacity)

    suspend fun acquireWithTimeout(timeout: Duration, onTimeout: () -> Unit): ConnectionWithLock {
        while (true) {
            // Following async timeout with resources recommendation:
            // https://kotlinlang.org/docs/cancellation-and-timeouts.html#asynchronous-timeout-and-resources
            var connection: ConnectionWithLock? = null
            var exceptionThrown: Throwable? = null
            try {
                withTimeout(timeout) { connection = acquire() }
            } catch (ex: Throwable) {
                exceptionThrown = ex
            }
            try {
                if (exceptionThrown is TimeoutCancellationException) {
                    onTimeout.invoke() // might throw
                } else if (exceptionThrown != null) {
                    throw exceptionThrown
                } else if (connection != null) {
                    return connection
                }
            } catch (ex: Throwable) {
                // If any error occurs before returning from this function and acquire() returned
                // such that a connection != null, then we need to recycle it.
                connection?.let { recycle(it) }
                throw ex
            }
        }
    }

    suspend fun acquire(): ConnectionWithLock {
        connectionPermits.acquire()
        try {
            return lock.withLock {
                if (isClosed) {
                    throwSQLiteException(SQLITE_MISUSE, "Connection pool is closed")
                }
                if (availableConnections.isEmpty()) {
                    tryOpenNewConnectionLocked()
                }
                availableConnections.popFirst()
            }
        } catch (ex: Throwable) {
            connectionPermits.release()
            throw ex
        }
    }

    private fun tryOpenNewConnectionLocked() {
        if (size >= capacity) {
            // Capacity reached
            return
        }
        val newConnection = ConnectionWithLock(connectionFactory.invoke())
        connections[size++] = newConnection
        availableConnections.addLast(newConnection)
    }

    fun recycle(connection: ConnectionWithLock) {
        lock.withLock { availableConnections.addLast(connection) }
        connectionPermits.release()
    }

    fun close() {
        lock.withLock {
            isClosed = true
            connections.forEach { it?.close() }
        }
    }

    /* Dumps debug information */
    fun dump(builder: StringBuilder) =
        lock.withLock {
            val availableQueue = buildList {
                for (i in 0 until availableConnections.size()) {
                    add(availableConnections[i])
                }
            }
            builder.append("\t" + super.toString() + " (")
            builder.append("capacity=$capacity, ")
            builder.append("permits=${connectionPermits.availablePermits}, ")
            builder.append(
                "queue=(size=${availableQueue.size})[${availableQueue.joinToString()}], "
            )
            builder.appendLine(")")
            connections.forEachIndexed { index, connection ->
                builder.appendLine("\t\t[${index + 1}] - ${connection?.toString()}")
                connection?.dump(builder)
            }
        }
}

private class ConnectionWithLock(
    private val delegate: SQLiteConnection,
    private val lock: Mutex = Mutex(),
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
private class PooledConnectionImpl(val delegate: ConnectionWithLock, val isReadOnly: Boolean) :
    Transactor, RawConnectionAccessor {
    private val transactionStack = ArrayDeque<TransactionItem>()

    private val _isRecycled = AtomicBoolean(false)
    private val isRecycled: Boolean
        get() = _isRecycled.get()

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
        block: suspend TransactionScope<R>.() -> R,
    ): R = withStateCheck { transaction(type, block) }

    override suspend fun inTransaction(): Boolean = withStateCheck {
        return transactionStack.isNotEmpty() || delegate.inTransaction()
    }

    fun markRecycled() {
        if (_isRecycled.compareAndSet(expect = false, update = true)) {
            // Perform a rollback in case there is an active transaction so that the connection
            // is in a clean state when it is recycled.
            if (delegate.inTransaction()) {
                delegate.execSQL("ROLLBACK TRANSACTION")
            }
        }
    }

    private suspend fun <R> transaction(
        type: SQLiteTransactionType?,
        block: suspend TransactionScope<R>.() -> R,
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
                @Suppress("UNCHECKED_CAST")
                return (ex.result as R)
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
                "Attempted to use connection on a different coroutine",
            )
        }
        return block.invoke()
    }

    private inner class StatementWrapper(private val delegate: SQLiteStatement) : SQLiteStatement {

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
                    "Attempted to use statement on a different thread",
                )
            }
            return block.invoke()
        }
    }
}
