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

package androidx.room

import androidx.room.Transactor.SQLiteTransactionType
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

/**
 * A wrapper of [SQLiteConnection] that belongs to a connection pool and is safe to use in a
 * coroutine.
 */
interface PooledConnection {
    /**
     * Prepares a new SQL statement and use it within the code [block].
     *
     * Using the given [SQLiteStatement] after [block] completes is prohibited. The statement will
     * also be thread confined, attempting to use it from another thread is an error.
     *
     * Using a statement locks the connection it belongs to, therefore try not to do long-running
     * computations within the [block].
     *
     * @param sql The SQL statement to prepare
     * @param block The code to use the statement
     */
    // TODO(b/319653917): Revisit shareable / caching APIs
    suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R
}

/** Executes a single SQL statement that returns no values. */
@Suppress("AcronymName") // SQL is a known term and should remain capitalized
suspend fun PooledConnection.execSQL(sql: String) {
    usePrepared(sql) { it.step() }
}

/** A [PooledConnection] that can perform transactions. */
interface Transactor : PooledConnection {

    /**
     * Begins a transaction and runs the [block] within the transaction. If [block] fails to
     * complete normally i.e., an exception is thrown, or [TransactionScope.rollback] is invoked
     * then the transaction will be rollback, otherwise it is committed.
     *
     * If [inTransaction] returns `true` and this function is invoked it is the equivalent of
     * starting a nested transaction as if [TransactionScope.withNestedTransaction] was invoked and
     * the [type] of the transaction will be ignored since its type will be inherited from the
     * parent transaction.
     *
     * See also [Transaction](https://www.sqlite.org/lang_transaction.html)
     *
     * @param type The type of transaction to begin.
     * @param block The code that will execute within the transaction.
     */
    suspend fun <R> withTransaction(
        type: SQLiteTransactionType,
        block: suspend TransactionScope<R>.() -> R
    ): R

    /** Returns true if this connection has an active transaction, otherwise false. */
    suspend fun inTransaction(): Boolean

    /**
     * Transaction types.
     *
     * @see Transactor.withTransaction
     */
    @Suppress("AcronymName") // SQL is a known term and should remain capitalized
    enum class SQLiteTransactionType {
        /**
         * The transaction mode that does not start the actual transaction until the database is
         * accessed, may it be a read or a write.
         */
        DEFERRED,
        /** The transaction mode that immediately starts a write transaction. */
        IMMEDIATE,
        /**
         * The transaction mode that immediately starts a write transaction and locks the database
         * preventing others from accessing it.
         */
        EXCLUSIVE,
    }
}

/**
 * A [PooledConnection] with an active transaction capable of performing nested transactions.
 *
 * @see Transactor
 */
interface TransactionScope<T> : PooledConnection {

    /**
     * Begins a nested transaction and runs the [block] within the transaction. If [block] fails to
     * complete normally i.e., an exception is thrown, or [rollback] is invoked then the transaction
     * will be rollback, otherwise it is committed.
     *
     * Note that a nested transaction is still governed by its parent transaction and it too must
     * complete successfully for all its children transactions to be committed.
     *
     * See also [Savepoint](https://www.sqlite.org/lang_savepoint.html)
     *
     * @param block The code that will execute within the transaction.
     */
    suspend fun <R> withNestedTransaction(block: suspend TransactionScope<R>.() -> R): R

    /**
     * Rollback the transaction, completing it and returning the [result].
     *
     * @see Transactor.withTransaction
     * @see TransactionScope.withNestedTransaction
     */
    suspend fun rollback(result: T): Nothing
}

/** Performs a [SQLiteTransactionType.DEFERRED] within the [block]. */
suspend fun <R> Transactor.deferredTransaction(block: suspend TransactionScope<R>.() -> R): R =
    withTransaction(SQLiteTransactionType.DEFERRED, block)

/** Performs a [SQLiteTransactionType.IMMEDIATE] within the [block]. */
suspend fun <R> Transactor.immediateTransaction(block: suspend TransactionScope<R>.() -> R): R =
    withTransaction(SQLiteTransactionType.IMMEDIATE, block)

/** Performs a [SQLiteTransactionType.EXCLUSIVE] within the [block]. */
suspend fun <R> Transactor.exclusiveTransaction(block: suspend TransactionScope<R>.() -> R): R =
    withTransaction(SQLiteTransactionType.EXCLUSIVE, block)
