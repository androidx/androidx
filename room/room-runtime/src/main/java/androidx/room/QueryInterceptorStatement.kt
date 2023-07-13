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

package androidx.room

import androidx.sqlite.db.SupportSQLiteStatement
import java.util.concurrent.Executor

/**
 * Implements an instance of [SupportSQLiteStatement] for SQLite queries.
 */
internal class QueryInterceptorStatement(
    private val delegate: SupportSQLiteStatement,
    private val sqlStatement: String,
    private val queryCallbackExecutor: Executor,
    private val queryCallback: RoomDatabase.QueryCallback,
) : SupportSQLiteStatement by delegate {

    private val bindArgsCache = mutableListOf<Any?>()

    override fun execute() {
        queryCallbackExecutor.execute {
            queryCallback.onQuery(sqlStatement, bindArgsCache)
        }
        delegate.execute()
    }

    override fun executeUpdateDelete(): Int {
        queryCallbackExecutor.execute {
            queryCallback.onQuery(sqlStatement, bindArgsCache)
        }
        return delegate.executeUpdateDelete()
    }

   override fun executeInsert(): Long {
        queryCallbackExecutor.execute {
            queryCallback.onQuery(sqlStatement, bindArgsCache)
        }
        return delegate.executeInsert()
    }

    override fun simpleQueryForLong(): Long {
        queryCallbackExecutor.execute {
            queryCallback.onQuery(sqlStatement, bindArgsCache)
        }
        return delegate.simpleQueryForLong()
    }

    override fun simpleQueryForString(): String? {
        queryCallbackExecutor.execute {
            queryCallback.onQuery(sqlStatement, bindArgsCache)
        }
        return delegate.simpleQueryForString()
    }

    override fun bindNull(index: Int) {
        saveArgsToCache(index, null)
        delegate.bindNull(index)
    }

    override fun bindLong(index: Int, value: Long) {
        saveArgsToCache(index, value)
        delegate.bindLong(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        saveArgsToCache(index, value)
        delegate.bindDouble(index, value)
    }

    override fun bindString(index: Int, value: String) {
        saveArgsToCache(index, value)
        delegate.bindString(index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        saveArgsToCache(index, value)
        delegate.bindBlob(index, value)
    }

    override fun clearBindings() {
        bindArgsCache.clear()
        delegate.clearBindings()
    }

    private fun saveArgsToCache(bindIndex: Int, value: Any?) {
        val index = bindIndex - 1
        if (index >= bindArgsCache.size) {
            // Add null entries to the list until we have the desired # of indices
            repeat(index - bindArgsCache.size + 1) {
                bindArgsCache.add(null)
            }
        }
        bindArgsCache[index] = value
    }
}
