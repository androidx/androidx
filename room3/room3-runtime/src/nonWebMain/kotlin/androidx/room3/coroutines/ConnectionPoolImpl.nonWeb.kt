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

package androidx.room3.coroutines

import androidx.room3.util.PlatformType
import androidx.room3.util.platform
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

internal actual fun newConnectionWrapper(
    connection: SQLiteConnection,
    statementCacheSize: Int,
): ConnectionWrapper {
    return ConnectionWrapperImpl(connection, statementCacheSize)
}

private class ConnectionWrapperImpl(delegate: SQLiteConnection, statementCacheSize: Int) :
    ConnectionWrapper(delegate = delegate) {
    private val preparedStatementCache: PreparedStatementCacheImpl? =
        if (platform != PlatformType.WEB && statementCacheSize > 0) {
            PreparedStatementCacheImpl(connection = delegate, maxSize = statementCacheSize)
        } else {
            null
        }

    override fun prepare(sql: String): SQLiteStatement {
        if (preparedStatementCache != null) {
            return preparedStatementCache.get(sql)
        }
        return delegate.prepare(sql)
    }

    override fun getCache() = preparedStatementCache
}

private class PreparedStatementCacheImpl(connection: SQLiteConnection, maxSize: Int) :
    BasePreparedStatementCache(connection, maxSize) {
    fun get(sql: String): SQLiteStatement {
        val statement = cache[sql] ?: connection.prepare(sql).also { cache.put(sql, it) }
        return CachedStatement(statement)
    }
}

internal actual fun newStatementWrapper(
    statement: SQLiteStatement,
    isRecycled: () -> Boolean,
): StatementWrapper {
    return StatementWrapperImpl(statement, isRecycled)
}

private class StatementWrapperImpl(delegate: SQLiteStatement, isRecycled: () -> Boolean) :
    StatementWrapper(delegate, isRecycled) {
    override fun step(): Boolean = withStateCheck {
        return delegate.step()
    }
}
