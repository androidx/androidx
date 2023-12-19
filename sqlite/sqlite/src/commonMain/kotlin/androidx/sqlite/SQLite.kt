/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.sqlite

/**
 * Executes a single SQL statement that returns no values.
 */
fun SQLiteConnection.execSQL(sql: String) {
    prepare(sql).use { it.step() }
}

/**
 * Performs an `EXCLUSIVE TRANSACTION`, committing to it if the [block] completes or rolling it back
 * if an exception is thrown.
 */
// TODO(b/304302260): To be replaced with proper threading & transaction APIs.
fun <R> SQLiteConnection.exclusiveTransaction(block: SQLiteConnection.() -> R): R {
    val result: R
    var success = false
    this.beginExclusiveTransaction()
    try {
        result = this.block()
        success = true
    } finally {
        if (success) {
            this.endTransaction()
        } else {
            this.rollbackTransaction()
        }
    }
    return result
}

private fun SQLiteConnection.beginExclusiveTransaction() {
    execSQL("BEGIN EXCLUSIVE TRANSACTION")
}

private fun SQLiteConnection.rollbackTransaction() {
    execSQL("ROLLBACK TRANSACTION")
}

private fun SQLiteConnection.endTransaction() {
    execSQL("END TRANSACTION")
}

/**
 * Use the receiver statement within the [block] and closes it once it is done.
 */
// TODO(b/315461431): Migrate to a Closeable interface in KMP
fun <R> SQLiteStatement.use(block: (SQLiteStatement) -> R): R {
    try {
        return block.invoke(this)
    } finally {
        close()
    }
}

/**
 * Throws a [SQLiteException] with its message formed by the given [errorCode] amd [errorMsg].
 */
fun throwSQLiteException(errorCode: Int, errorMsg: String?): Nothing {
    val message = buildString {
        append("Error code: $errorCode")
        if (errorMsg != null) {
            append(", message: $errorMsg")
        }
    }
    throw SQLiteException(message)
}
