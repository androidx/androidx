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

import androidx.sqlite.SQLiteStatement
import kotlin.jvm.JvmOverloads

/**
 * A query with an argument binding function.
 *
 * @see [RawQuery]
 */
class RoomRawQuery
@JvmOverloads
constructor(
    /**
     * The SQL query.
     *
     * The query can have placeholders (?) to bind arguments.
     */
    val sql: String,
    /**
     * The function that receives a [SQLiteStatement] and binds arguments.
     *
     * Only `bind*()` calls should be invoked on the received statement.
     */
    onBindStatement: (SQLiteStatement) -> Unit = {}
) {
    private val bindingFunction: (SQLiteStatement) -> Unit = {
        onBindStatement.invoke(BindOnlySQLiteStatement(it))
    }

    fun getBindingFunction(): (SQLiteStatement) -> Unit = bindingFunction
}

private class BindOnlySQLiteStatement(delegate: SQLiteStatement) : SQLiteStatement by delegate {

    override fun getBlob(index: Int): ByteArray {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun getDouble(index: Int): Double {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun getLong(index: Int): Long {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun getText(index: Int): String {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun isNull(index: Int): Boolean {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun getColumnCount(): Int {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun getColumnName(index: Int): String {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun getColumnType(index: Int): Int {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun step(): Boolean {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun reset() {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    override fun close() {
        error(ONLY_BIND_CALLS_ALLOWED_ERROR)
    }

    companion object {
        private const val ONLY_BIND_CALLS_ALLOWED_ERROR =
            "Only bind*() calls are allowed on the RoomRawQuery received statement."
    }
}
