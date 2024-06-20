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

package androidx.room.driver

import androidx.room.TransactionScope
import androidx.room.Transactor
import androidx.room.coroutines.ConnectionPool
import androidx.room.coroutines.RawConnectionAccessor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.use

/**
 * An implementation of a connection pool used in compatibility mode. This impl doesn't do any
 * connection management since the SupportSQLite* APIs already internally do.
 */
internal class SupportSQLiteConnectionPool(internal val supportDriver: SupportSQLiteDriver) :
    ConnectionPool {
    private val supportConnection by
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            val fileName = supportDriver.openHelper.databaseName ?: ":memory:"
            SupportSQLitePooledConnection(supportDriver.open(fileName))
        }

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R
    ): R {
        return block.invoke(supportConnection)
    }

    override fun close() {
        supportDriver.openHelper.close()
    }
}

private class SupportSQLitePooledConnection(val delegate: SupportSQLiteConnection) :
    Transactor, RawConnectionAccessor {

    private var currentTransactionType: Transactor.SQLiteTransactionType? = null

    override val rawConnection: SQLiteConnection
        get() = delegate

    override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R {
        return delegate.prepare(sql).use { block.invoke(it) }
    }

    override suspend fun <R> withTransaction(
        type: Transactor.SQLiteTransactionType,
        block: suspend TransactionScope<R>.() -> R
    ): R {
        return transaction(type, block)
    }

    private suspend fun <R> transaction(
        type: Transactor.SQLiteTransactionType,
        block: suspend TransactionScope<R>.() -> R
    ): R {
        val db = delegate.db
        if (!db.inTransaction()) {
            currentTransactionType = type
        }
        when (type) {
            Transactor.SQLiteTransactionType.DEFERRED -> db.beginTransactionReadOnly()
            Transactor.SQLiteTransactionType.IMMEDIATE -> db.beginTransactionNonExclusive()
            Transactor.SQLiteTransactionType.EXCLUSIVE -> db.beginTransaction()
        }
        try {
            val result = SupportSQLiteTransactor<R>().block()
            db.setTransactionSuccessful()
            return result
        } catch (rollback: ConnectionPool.RollbackException) {
            @Suppress("UNCHECKED_CAST") return rollback.result as R
        } finally {
            db.endTransaction()
            if (!db.inTransaction()) {
                currentTransactionType = null
            }
        }
    }

    override suspend fun inTransaction(): Boolean {
        return delegate.db.inTransaction()
    }

    private inner class SupportSQLiteTransactor<T> : TransactionScope<T>, RawConnectionAccessor {

        override val rawConnection: SQLiteConnection
            get() = this@SupportSQLitePooledConnection.rawConnection

        override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R {
            return this@SupportSQLitePooledConnection.usePrepared(sql, block)
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
