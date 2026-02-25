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

import androidx.room3.Transactor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException

/**
 * A container that manages [SQLiteConnection]s.
 *
 * Connection pools provides the means of bounding and managing the resources that are database
 * connections. They provide safe usage of connections in a multi-threaded application and usually
 * provide improved performance when performing asynchronous and concurrent database interactions.
 *
 * An implementation instance can be created via the factory function [newConnectionPool] and
 * [newSingleConnectionPool].
 */
internal interface ConnectionPool : AutoCloseable {

    /**
     * Acquires a connection, suspending while waiting if none is available and then calling the
     * [block] to use the connection once it is acquired. The connection to use in the [block] is an
     * instance of [Transactor] that provides the capabilities for performing nested transactions.
     *
     * Using the connection after [block] completes is prohibited.
     *
     * The connection will be confined to the coroutine on which [block] executes, attempting to use
     * the connection from a different coroutine will result in an error.
     *
     * If the current coroutine calling this function already has a confined connection, then that
     * connection is used as long as it isn't required to be upgraded to a writer. If an upgrade is
     * required then a [SQLiteException] is thrown.
     *
     * If a caller has to wait too long to acquire a connection a [SQLiteException] will be thrown
     * due to a timeout.
     *
     * @param isReadOnly Whether to use a reader or a writer connection.
     * @param block The code to use the connection.
     * @throws SQLiteException when the pool is closed or a thread confined connection needs to be
     *   upgraded or there is a timeout acquiring a connection.
     */
    suspend fun <R> useConnection(isReadOnly: Boolean, block: suspend (Transactor) -> R): R

    /**
     * Closes the pool and any opened connections, attempting to use connections is an error once
     * the pool is closed.
     */
    override fun close()

    /** Internal exception thrown to rollback a transaction. */
    class RollbackException(val result: Any?) : Throwable()
}

/**
 * Creates a new [ConnectionPool] with a single connection.
 *
 * A pool containing a single connection that is used for both reading and writing is useful for
 * in-memory databases whose schema and data are isolated to a database connection.
 *
 * @param connectionFactory The factory function from which to request the connection to be opened.
 * @param statementCacheSize The maximum number of prepared statements to be cached.
 * @return The newly created connection pool
 * @see newConnectionPool
 */
internal fun newSingleConnectionPool(
    connectionFactory: ConnectionFactory,
    statementCacheSize: Int = 25,
): ConnectionPool = ConnectionPoolImpl(connectionFactory, statementCacheSize)

/**
 * Creates a new [ConnectionPool] with multiple connections separated by readers and writers.
 *
 * If the database journal mode is Write-Ahead Logging (WAL) then it is recommended to create a pool
 * of one writer and multiple readers. If the database journal mode is not WAL (e.g. TRUNCATE,
 * DELETE or PERSIST) then a single connection pool is recommended.
 *
 * @param connectionFactory The factory function from which to request the connections to be opened.
 * @param maxNumOfReaders The maximum number of connections to be opened and used as readers.
 * @param maxNumOfWriters The maximum number of connections to be opened and used as writers.
 * @param statementCacheSize The maximum number of prepared statements to be cached.
 * @return The newly created connection pool
 * @see newSingleConnectionPool
 */
internal fun newConnectionPool(
    connectionFactory: ConnectionFactory,
    maxNumOfReaders: Int,
    maxNumOfWriters: Int,
    statementCacheSize: Int = 25,
): ConnectionPool =
    ConnectionPoolImpl(connectionFactory, maxNumOfReaders, maxNumOfWriters, statementCacheSize)

/** Defines an object that provides 'raw' access to a connection. */
internal interface RawConnectionAccessor {

    suspend fun <R> useRawConnection(block: suspend (SQLiteConnection) -> R): R
}
