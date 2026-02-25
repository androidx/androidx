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

package androidx.room3.coroutines

import androidx.collection.LruCache
import androidx.room3.TransactionScope
import androidx.room3.Transactor
import androidx.room3.Transactor.SQLiteTransactionType
import androidx.room3.concurrent.ReentrantLock
import androidx.room3.concurrent.ThreadLocal
import androidx.room3.concurrent.asContextElement
import androidx.room3.concurrent.currentThreadId
import androidx.room3.concurrent.withLock
import androidx.room3.util.SQLiteResultCode.SQLITE_BUSY
import androidx.room3.util.SQLiteResultCode.SQLITE_ERROR
import androidx.room3.util.SQLiteResultCode.SQLITE_MISUSE
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.executeSQL
import androidx.sqlite.prepare
import androidx.sqlite.throwSQLiteException
import kotlin.collections.removeLast as removeLastKt
import kotlin.concurrent.Volatile
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal const val THROW_TIMEOUT_EXCEPTION = 1
internal const val LOG_TIMEOUT_EXCEPTION = 2

internal typealias ConnectionFactory = suspend () -> SQLiteConnection

internal class ConnectionPoolImpl : ConnectionPool {
    private val connectionFactory: ConnectionFactory
    private val readers: Pool
    private val writers: Pool

    private val connectionElementKey = ConnectionElementKey()
    private val connectionThreadLocal = ThreadLocal<PooledConnectionImpl>()

    @Volatile private var isClosed: Boolean = false

    // Amount of time to wait to acquire a connection before logging, Android uses 30 seconds in
    // its pool, so we do too here, but IDK if that is a good number. This timeout is unrelated
    // to the busy handler.
    // TODO(b/404380974): Allow public configuration
    internal var timeout = 30.seconds
    internal var onTimeout = LOG_TIMEOUT_EXCEPTION

    constructor(connectionFactory: ConnectionFactory, statementCacheSize: Int) {
        this.connectionFactory = connectionFactory
        this.readers =
            Pool(
                capacity = 1,
                connectionFactory = connectionFactory,
                statementCacheSize = statementCacheSize,
            )
        this.writers = readers
    }

    constructor(
        connectionFactory: ConnectionFactory,
        maxNumOfReaders: Int,
        maxNumOfWriters: Int,
        statementCacheSize: Int,
    ) {
        require(maxNumOfReaders > 0) { "Maximum number of readers must be greater than 0" }
        require(maxNumOfWriters > 0) { "Maximum number of writers must be greater than 0" }
        this.connectionFactory = connectionFactory
        this.readers =
            Pool(
                capacity = maxNumOfReaders,
                connectionFactory = {
                    connectionFactory.invoke().also { newConnection ->
                        // Enforce to be read only (might be disabled by a YOLO developer)
                        // This is called before the connection is delivered to the pool and is not
                        // cached
                        newConnection.executeSQL("PRAGMA query_only = 1")
                    }
                },
                statementCacheSize = statementCacheSize,
            )
        this.writers =
            Pool(
                capacity = maxNumOfWriters,
                connectionFactory = connectionFactory,
                statementCacheSize = statementCacheSize,
            )
    }

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R,
    ): R {
        if (isClosed) {
            throwSQLiteException(SQLITE_MISUSE, "Connection pool is closed")
        }
        val confinedConnection =
            connectionThreadLocal.get()
                ?: currentCoroutineContext()[connectionElementKey]?.connectionWrapper
        if (confinedConnection != null) {
            if (!isReadOnly && confinedConnection.isReadOnly) {
                throwSQLiteException(
                    SQLITE_ERROR,
                    "Cannot upgrade connection from reader to writer",
                )
            }
            return if (currentCoroutineContext()[connectionElementKey] == null) {
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
            val currentContext = currentCoroutineContext()
            connection =
                PooledConnectionImpl(
                    connectionElementKey = connectionElementKey,
                    delegate =
                        pool
                            .acquireWithTimeout(timeout) { onTimeout(isReadOnly, currentContext) }
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
        ConnectionElement(connectionElementKey, connection) +
            connectionThreadLocal.asContextElement(connection)

    private fun onTimeout(isReadOnly: Boolean, requestContext: CoroutineContext) {
        val readOrWrite = if (isReadOnly) "reader" else "writer"
        val message = buildString {
            appendLine("Timed out attempting to acquire a $readOrWrite connection.")
            appendLine()
            appendLine("Request coroutine: $requestContext")
            appendLine()
            appendLine("Writer pool:")
            writers.dump(this)
            appendLine("Reader pool:")
            readers.dump(this)
        }
        try {
            throwSQLiteException(SQLITE_BUSY, message)
        } catch (ex: SQLiteException) {
            when (onTimeout) {
                THROW_TIMEOUT_EXCEPTION -> throw ex
                LOG_TIMEOUT_EXCEPTION -> ex.printStackTrace()
            }
        }
    }

    // TODO: (b/319657104): Make suspending so pool closes when all connections are recycled.
    override fun close() {
        if (!isClosed) {
            isClosed = true
            readers.close()
            writers.close()
        }
    }
}

private class Pool(
    val capacity: Int,
    val connectionFactory: ConnectionFactory,
    val statementCacheSize: Int,
) {
    private val lock = ReentrantLock()
    private val mutex = Mutex()
    private var size = 0
    private var isClosed = false
    private val connections = arrayOfNulls<ConnectionWrapper>(capacity)
    private val connectionPermits = Semaphore(permits = capacity)
    // The available connections as a stack to maximize cache hits.
    private val availableConnections = ArrayDeque<ConnectionWrapper>(capacity)

    suspend fun acquireWithTimeout(timeout: Duration, onTimeout: () -> Unit): ConnectionWrapper {
        while (true) {
            // Following async timeout with resources recommendation:
            // https://kotlinlang.org/docs/cancellation-and-timeouts.html#asynchronous-timeout-and-resources
            var connection: ConnectionWrapper? = null
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

    suspend fun acquire(): ConnectionWrapper {
        connectionPermits.acquire()
        try {
            lock.withLock {
                if (isClosed) {
                    throwSQLiteException(SQLITE_MISUSE, "Connection pool is closed")
                }
                if (availableConnections.isNotEmpty()) {
                    return availableConnections.removeLast()
                }
            }
            // At this point a permit was acquired but there is no available connections therefore
            // the pool is not at capacity and a new connection is needed to satisfy the permit.
            check(size < capacity)
            val newConnection =
                mutex.withReentrantLock {
                    newConnectionWrapper(connectionFactory.invoke(), statementCacheSize)
                }
            lock.withLock {
                if (isClosed) {
                    // Pool was closed in-between opening a new connection, close it and throw.
                    newConnection.close()
                    throwSQLiteException(SQLITE_MISUSE, "Connection pool is closed")
                }
                // Add the new connection to the pool and return it, once recycled it will be
                // added to the available stack.
                connections[size++] = newConnection
                return newConnection
            }
        } catch (ex: Throwable) {
            connectionPermits.release()
            throw ex
        }
    }

    fun recycle(connection: ConnectionWrapper) {
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
                for (i in 0 until availableConnections.size) {
                    add(availableConnections[i])
                }
            }
            builder.append("\t" + super.toString() + " (")
            builder.append("capacity=$capacity, ")
            builder.append("permits=${connectionPermits.availablePermits}, ")
            builder.append("queue=(size=${availableQueue.size})[${availableQueue.joinToString()}]")
            builder.appendLine(")")
            connections.forEachIndexed { index, connection ->
                builder.appendLine("\t\t[${index + 1}] - ${connection?.toString()}")
                connection?.dump(builder)
            }
        }
}

internal expect fun newConnectionWrapper(
    connection: SQLiteConnection,
    statementCacheSize: Int,
): ConnectionWrapper

internal abstract class ConnectionWrapper
private constructor(protected val delegate: SQLiteConnection, private val lock: Mutex) :
    SQLiteConnection, Mutex by lock {

    private var acquireCoroutineContext: CoroutineContext? = null
    private var acquireThrowable: Throwable? = null

    protected constructor(delegate: SQLiteConnection) : this(delegate, Mutex())

    override fun inTransaction(): Boolean = delegate.inTransaction()

    override fun close() {
        getCache()?.evictAll()
        delegate.close()
    }

    abstract fun getCache(): BasePreparedStatementCache?

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
        getCache()?.let { builder.appendLine("\t\tPrepared Statement Cache Size: ${it.size()}") }
    }

    override fun toString(): String {
        return delegate.toString()
    }
}

internal abstract class BasePreparedStatementCache(
    protected val connection: SQLiteConnection,
    maxSize: Int,
) {
    protected val cache =
        object : LruCache<String, SQLiteStatement>(maxSize) {
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: SQLiteStatement,
                newValue: SQLiteStatement?,
            ) {
                // Statement was removed from cache, we need to close it now so it can't be reused.
                check(oldValue !is CachedStatement)
                oldValue.close()
                super.entryRemoved(evicted, key, oldValue, newValue)
            }
        }

    fun evictAll() = cache.evictAll()

    fun size() = cache.size()

    protected class CachedStatement(val delegate: SQLiteStatement) : SQLiteStatement by delegate {
        override fun close() {
            // Reset the statement so that it can be reused from the cache
            delegate.reset()
            delegate.clearBindings()
        }
    }
}

private class ConnectionElement(
    override val key: CoroutineContext.Key<ConnectionElement>,
    val connectionWrapper: PooledConnectionImpl,
) : CoroutineContext.Element

private class ConnectionElementKey : CoroutineContext.Key<ConnectionElement>

/**
 * A connection wrapper to enforce pool contract and implement transactions.
 *
 * Actual connection interactions are serialized via a limited dispatcher, specifically compiling a
 * statement and using it is serialized as to prevent a coroutine from concurrently using the
 * statement between multiple different threads.
 */
private class PooledConnectionImpl(
    val connectionElementKey: ConnectionElementKey,
    val delegate: ConnectionWrapper,
    val isReadOnly: Boolean,
) : Transactor, RawConnectionAccessor {
    private val transactionStack = ArrayDeque<TransactionItem>()

    @Volatile private var isRecycled: Boolean = false

    override suspend fun <R> useRawConnection(block: suspend (SQLiteConnection) -> R): R {
        return delegate.withLock { block.invoke(delegate) }
    }

    override suspend fun <R> usePrepared(sql: String, block: suspend (SQLiteStatement) -> R): R =
        withStateCheck {
            return delegate.withLock {
                newStatementWrapper(delegate.prepare(sql), ::isRecycled).use { block.invoke(it) }
            }
        }

    override suspend fun <R> withTransaction(
        type: SQLiteTransactionType,
        block: suspend TransactionScope<R>.() -> R,
    ): R = withStateCheck { transaction(type, block) }

    override suspend fun inTransaction(): Boolean = withStateCheck {
        return transactionStack.isNotEmpty() || delegate.inTransaction()
    }

    suspend fun markRecycled() {
        if (!isRecycled) {
            isRecycled = true
            // Perform a rollback in case there is an active transaction so that the connection
            // is in a clean state when it is recycled.
            if (delegate.inTransaction()) {
                delegate.executeSQL("ROLLBACK TRANSACTION")
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
                    SQLiteTransactionType.DEFERRED ->
                        delegate.executeSQL("BEGIN DEFERRED TRANSACTION")
                    SQLiteTransactionType.IMMEDIATE ->
                        delegate.executeSQL("BEGIN IMMEDIATE TRANSACTION")
                    SQLiteTransactionType.EXCLUSIVE ->
                        delegate.executeSQL("BEGIN EXCLUSIVE TRANSACTION")
                }
            } else {
                delegate.executeSQL("SAVEPOINT '$newTransactionId'")
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
                    delegate.executeSQL("END TRANSACTION")
                } else {
                    delegate.executeSQL("RELEASE SAVEPOINT '${transaction.id}'")
                }
            } else {
                if (transactionStack.isEmpty()) {
                    delegate.executeSQL("ROLLBACK TRANSACTION")
                } else {
                    delegate.executeSQL("ROLLBACK TRANSACTION TO SAVEPOINT '${transaction.id}'")
                }
            }
        }

    private class TransactionItem(val id: Int, var shouldRollback: Boolean)

    private inner class TransactionImpl<T> : TransactionScope<T>, RawConnectionAccessor {

        override suspend fun <R> useRawConnection(block: suspend (SQLiteConnection) -> R): R =
            this@PooledConnectionImpl.useRawConnection(block)

        override suspend fun <R> usePrepared(
            sql: String,
            block: suspend (SQLiteStatement) -> R,
        ): R = this@PooledConnectionImpl.usePrepared(sql, block)

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
        val connectionElement = currentCoroutineContext()[connectionElementKey]
        if (connectionElement == null || connectionElement.connectionWrapper !== this) {
            throwSQLiteException(
                SQLITE_MISUSE,
                "Attempted to use connection on a different coroutine",
            )
        }
        return block.invoke()
    }
}

internal expect fun newStatementWrapper(
    statement: SQLiteStatement,
    isRecycled: () -> Boolean,
): StatementWrapper

internal abstract class StatementWrapper(
    protected val delegate: SQLiteStatement,
    private val isRecycled: () -> Boolean,
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

    override fun reset() = withStateCheck { delegate.reset() }

    override fun clearBindings() = withStateCheck { delegate.clearBindings() }

    override fun close() = withStateCheck { delegate.close() }

    protected inline fun <R> withStateCheck(block: () -> R): R {
        if (isRecycled()) {
            throwSQLiteException(SQLITE_MISUSE, "Statement is recycled")
        }
        return block.invoke()
    }
}
