/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.support

import android.database.Cursor
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Implements [SupportSQLiteDatabase] for intercepting SQLite queries. */
internal class QueryInterceptorDatabase(
    private val delegate: SupportSQLiteDatabase,
    private val queryCallbackScope: CoroutineScope,
    private val queryCallback: RoomDatabase.QueryCallback
) : SupportSQLiteDatabase by delegate {

    override fun compileStatement(sql: String): SupportSQLiteStatement {
        return QueryInterceptorStatement(
            delegate.compileStatement(sql),
            sql,
            queryCallbackScope,
            queryCallback,
        )
    }

    override fun beginTransaction() {
        queryCallbackScope.launch {
            queryCallback.onQuery("BEGIN EXCLUSIVE TRANSACTION", emptyList())
        }
        delegate.beginTransaction()
    }

    override fun beginTransactionNonExclusive() {
        queryCallbackScope.launch {
            queryCallback.onQuery("BEGIN IMMEDIATE TRANSACTION", emptyList())
        }
        delegate.beginTransactionNonExclusive()
    }

    override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener) {
        queryCallbackScope.launch {
            queryCallback.onQuery("BEGIN EXCLUSIVE TRANSACTION", emptyList())
        }
        delegate.beginTransactionWithListener(transactionListener)
    }

    override fun beginTransactionWithListenerNonExclusive(
        transactionListener: SQLiteTransactionListener
    ) {
        queryCallbackScope.launch {
            queryCallback.onQuery("BEGIN IMMEDIATE TRANSACTION", emptyList())
        }
        delegate.beginTransactionWithListenerNonExclusive(transactionListener)
    }

    override fun endTransaction() {
        queryCallbackScope.launch { queryCallback.onQuery("END TRANSACTION", emptyList()) }
        delegate.endTransaction()
    }

    override fun setTransactionSuccessful() {
        queryCallbackScope.launch { queryCallback.onQuery("TRANSACTION SUCCESSFUL", emptyList()) }
        delegate.setTransactionSuccessful()
    }

    override fun query(query: String): Cursor {
        queryCallbackScope.launch { queryCallback.onQuery(query, emptyList()) }
        return delegate.query(query)
    }

    override fun query(query: String, bindArgs: Array<out Any?>): Cursor {
        val argsCopy = bindArgs.toList()
        queryCallbackScope.launch { queryCallback.onQuery(query, argsCopy) }
        return delegate.query(query, bindArgs)
    }

    override fun query(query: SupportSQLiteQuery): Cursor {
        val queryInterceptorProgram = QueryInterceptorProgram()
        query.bindTo(queryInterceptorProgram)
        queryCallbackScope.launch {
            queryCallback.onQuery(query.sql, queryInterceptorProgram.bindArgsCache)
        }
        return delegate.query(query)
    }

    override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor {
        val queryInterceptorProgram = QueryInterceptorProgram()
        query.bindTo(queryInterceptorProgram)
        queryCallbackScope.launch {
            queryCallback.onQuery(query.sql, queryInterceptorProgram.bindArgsCache)
        }
        return delegate.query(query)
    }

    // Suppress warning about `SQL` in execSQL not being camel case. This is an override function
    // and it can't be renamed.
    @Suppress("AcronymName")
    override fun execSQL(sql: String) {
        queryCallbackScope.launch { queryCallback.onQuery(sql, emptyList()) }
        delegate.execSQL(sql)
    }

    // Suppress warning about `SQL` in execSQL not being camel case. This is an override function
    // and it can't be renamed.
    @Suppress("AcronymName")
    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        val argsCopy = bindArgs.toList()
        queryCallbackScope.launch { queryCallback.onQuery(sql, argsCopy) }
        delegate.execSQL(sql, bindArgs)
    }
}
