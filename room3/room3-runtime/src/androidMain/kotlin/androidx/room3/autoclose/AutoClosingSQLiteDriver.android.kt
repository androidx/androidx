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

package androidx.room3.autoclose

import androidx.annotation.GuardedBy
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.throwSQLiteException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A driver that along with its [AutoCloser] automatically close connections that have not been used
 * after a certain amount of time.
 *
 * This driver is only used if [androidx.room3.RoomDatabase.Builder.setAutoCloseTimeout] is invoked
 * in the builder.
 */
internal class AutoClosingSQLiteDriver(
    private val autoCloser: AutoCloser,
    private val delegateDriver: SQLiteDriver,
) : SQLiteDriver by delegateDriver {
    override fun open(fileName: String): SQLiteConnection {
        return AutoClosingSQLiteConnection(
            autoCloser = autoCloser,
            delegateDriver = delegateDriver,
            fileName = fileName,
            delegateConnection = delegateDriver.open(fileName),
        )
    }
}

/** A connection that can be auto-close by its [AutoCloser]. */
internal class AutoClosingSQLiteConnection(
    private val autoCloser: AutoCloser,
    private val delegateDriver: SQLiteDriver,
    private val fileName: String,
    delegateConnection: SQLiteConnection,
) : SQLiteConnection {

    private val lock = ReentrantLock()

    /**
     * The real database connection that will be closed on an auto close timeout and will be
     * re-opened and reassigned if need be.
     */
    @GuardedBy("lock") private var liveConnection: SQLiteConnection? = delegateConnection

    /**
     * The amount of active statements created by this connection. Used to avoid auto closing a
     * connection that has prepared statements that have not been closed.
     */
    @GuardedBy("lock") private var liveStatements = 0

    /** Tracks explicit [close] calls and not auto close. */
    @Volatile private var isClosed = false

    init {
        autoCloser.register(this)
    }

    override fun inTransaction(): Boolean {
        return lock.withLock { liveConnection?.inTransaction() ?: false }
    }

    override fun prepare(sql: String): SQLiteStatement {
        if (isClosed) {
            throwSQLiteException(21, "connection is closed")
        }
        autoCloser.incrementCount()
        lock.withLock {
            liveStatements++
            var delegateConnection = liveConnection
            if (delegateConnection != null) {
                return AutoClosingSQLiteStatement(delegateConnection.prepare(sql))
            }
            // Real connection was auto-closed, re-open a new one.
            delegateConnection = delegateDriver.open(fileName).also { liveConnection = it }
            autoCloser.dispatchOnAutoOpenCallback(delegateConnection)
            return AutoClosingSQLiteStatement(delegateConnection.prepare(sql))
        }
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            lock.withLock {
                liveConnection?.close()
                liveConnection = null
            }
            autoCloser.unregister(this)
        }
    }

    /** Checks if this connection has been auto closed (different from explicitly closed). */
    fun isAutoClosed() = liveConnection == null

    /**
     * Performs an auto close by closing the real connection, a new one will be opened if necessary.
     */
    fun autoClose() {
        lock.withLock {
            if (liveStatements > 0 || liveConnection == null) {
                return
            }
            check(liveStatements == 0) { "Unbalanced live statement count." }
            liveConnection?.close()
            liveConnection = null
        }
    }

    private inner class AutoClosingSQLiteStatement(private val delegateStatement: SQLiteStatement) :
        SQLiteStatement by delegateStatement {

        @Volatile private var isClosed = false

        override fun close() {
            if (!isClosed) {
                isClosed = true
                delegateStatement.close()
                lock.withLock { liveStatements-- }
                autoCloser.decrementCount()
            }
        }
    }
}
