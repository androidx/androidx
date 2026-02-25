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

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

internal actual fun newConnectionWrapper(
    connection: SQLiteConnection,
    statementCacheSize: Int,
): ConnectionWrapper {
    return ConnectionWrapperImpl(connection)
}

private class ConnectionWrapperImpl(delegate: SQLiteConnection) :
    ConnectionWrapper(delegate = delegate) {

    override suspend fun prepareAsync(sql: String): SQLiteStatement {
        return delegate.prepareAsync(sql)
    }

    override fun getCache() = null
}

internal actual fun newStatementWrapper(
    statement: SQLiteStatement,
    isRecycled: () -> Boolean,
): StatementWrapper {
    return StatementWrapperImpl(statement, isRecycled)
}

private class StatementWrapperImpl(delegate: SQLiteStatement, isRecycled: () -> Boolean) :
    StatementWrapper(delegate, isRecycled) {
    override suspend fun stepAsync(): Boolean = withStateCheck {
        return delegate.stepAsync()
    }
}
