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
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.AndroidSQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.use

/**
 * An implementation of a connection pool used when an [AndroidSQLiteDriver] is provided. This impl
 * doesn't do any connection management since the Android SQLite APIs already internally do.
 */
internal class AndroidSQLiteDriverConnectionPool(
    private val driver: SQLiteDriver,
    private val fileName: String
) : ConnectionPool {

    private val androidConnection by lazy {
        AndroidSQLiteDriverPooledConnection(driver.open(fileName) as AndroidSQLiteConnection)
    }

    override suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R
    ): R {
        return block.invoke(androidConnection)
    }

    override fun close() {
        androidConnection.delegate.close()
    }
}

private class AndroidSQLiteDriverPooledConnection(val delegate: AndroidSQLiteConnection) :
    Transactor, RawConnectionAccessor {

    private var currentTransactionType: Transactor.SQLiteTransactionType? = null

    override val rawConnection: SQLiteConnection
        get() = delegate

    override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R {
        return delegate.prepare(sql).use { block.invoke(it) }
    }

    // TODO(b/318767291): Add coroutine confinement like RoomDatabase.withTransaction
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
            // TODO(b/288918056): Use Android V API for DEFERRED once it is available
            Transactor.SQLiteTransactionType.DEFERRED -> db.beginTransactionNonExclusive()
            Transactor.SQLiteTransactionType.IMMEDIATE -> db.beginTransactionNonExclusive()
            Transactor.SQLiteTransactionType.EXCLUSIVE -> db.beginTransaction()
        }
        try {
            val result = AndroidSQLiteDriverTransactor<R>().block()
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

    private inner class AndroidSQLiteDriverTransactor<T> :
        TransactionScope<T>, RawConnectionAccessor {

        override val rawConnection: SQLiteConnection
            get() = this@AndroidSQLiteDriverPooledConnection.rawConnection

        override suspend fun <R> usePrepared(sql: String, block: (SQLiteStatement) -> R): R {
            return this@AndroidSQLiteDriverPooledConnection.usePrepared(sql, block)
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
