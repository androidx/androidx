/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.sqlite.db.SupportSQLiteStatement
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents a prepared SQLite state that can be re-used multiple times.
 *
 * This class is used by generated code. After it is used, `release` must be called so that
 * it can be used by other threads.
 *
 * To avoid re-entry even within the same thread, this class allows only 1 time access to the shared
 * statement until it is released.
 *
 * @constructor Creates an SQLite prepared statement that can be re-used across threads. If it is
 * in use, it automatically creates a new one.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
abstract class SharedSQLiteStatement(private val database: RoomDatabase) {
    private val lock = AtomicBoolean(false)

    private val stmt: SupportSQLiteStatement by lazy {
        createNewStatement()
    }

    /**
     * Create the query.
     *
     * @return The SQL query to prepare.
     */
    protected abstract fun createQuery(): String

    protected open fun assertNotMainThread() {
        database.assertNotMainThread()
    }

    private fun createNewStatement(): SupportSQLiteStatement {
        val query = createQuery()
        return database.compileStatement(query)
    }

    private fun getStmt(canUseCached: Boolean): SupportSQLiteStatement {
        val stmt = if (canUseCached) {
            stmt
        } else {
            // it is in use, create a one off statement
            createNewStatement()
        }
        return stmt
    }

    /**
     * Call this to get the statement. Must call [.release] once done.
     */
    open fun acquire(): SupportSQLiteStatement {
        assertNotMainThread()
        return getStmt(lock.compareAndSet(false, true))
    }

    /**
     * Must call this when statement will not be used anymore.
     *
     * @param statement The statement that was returned from acquire.
     */
    open fun release(statement: SupportSQLiteStatement) {
        if (statement === stmt) {
            lock.set(false)
        }
    }
}
