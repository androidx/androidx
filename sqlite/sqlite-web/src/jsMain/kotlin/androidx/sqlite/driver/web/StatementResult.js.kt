/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.sqlite.driver.web

import org.khronos.webgl.Int8Array

/* Object container of a prepared statement result. */
internal actual class StatementResult(
    val rows: Array<Array<dynamic>>,
    actual val columnTypes: IntArray,
) {
    actual val size: Int
        get() = rows.size

    actual fun getByteArray(rowIndex: Int, columnIndex: Int): ByteArray {
        // SQLite WASM returns UInt8Array
        // See https://sqlite.org/wasm/doc/trunk/api-oo1.md#stmt-getblob
        val buffer = rows[rowIndex][columnIndex]
        return Int8Array(buffer, buffer.byteOffset, buffer.length).unsafeCast<ByteArray>()
    }

    actual fun getDouble(rowIndex: Int, columnIndex: Int): Double {
        return rows[rowIndex][columnIndex].unsafeCast<Double>()
    }

    actual fun getLong(rowIndex: Int, columnIndex: Int): Long {
        return rows[rowIndex][columnIndex].unsafeCast<Double>().toLong()
    }

    actual fun getString(rowIndex: Int, columnIndex: Int): String {
        return rows[rowIndex][columnIndex].unsafeCast<String>()
    }
}
