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

import androidx.sqlite.db.SupportSQLiteProgram

/**
 * A program implementing an [SupportSQLiteProgram] API to record bind arguments.
 */
internal class QueryInterceptorProgram : SupportSQLiteProgram {
    internal val bindArgsCache = mutableListOf<Any?>()

    override fun bindNull(index: Int) {
        saveArgsToCache(index, null)
    }

    override fun bindLong(index: Int, value: Long) {
        saveArgsToCache(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        saveArgsToCache(index, value)
    }

    override fun bindString(index: Int, value: String?) {
        saveArgsToCache(index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray?) {
        saveArgsToCache(index, value)
    }

    override fun clearBindings() {
        bindArgsCache.clear()
    }

    override fun close() {}

    private fun saveArgsToCache(bindIndex: Int, value: Any?) {
        // The index into bind methods are 1...n
        val index = bindIndex - 1
        if (index >= bindArgsCache.size) {
            for (i in bindArgsCache.size..index) {
                bindArgsCache.add(null)
            }
        }
        bindArgsCache[index] = value
    }
}
