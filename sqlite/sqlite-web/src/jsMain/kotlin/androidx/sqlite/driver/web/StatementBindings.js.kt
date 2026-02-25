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

/* Object container of a prepared statement binding parameters. */
internal actual class StatementBindings {

    var array: Array<dynamic> = emptyArray()

    actual fun setBlob(index: Int, value: ByteArray) {
        bind(index, value)
    }

    actual fun setDouble(index: Int, value: Double) {
        bind(index, value)
    }

    actual fun setLong(index: Int, value: Long) {
        // TODO(b/485611476): Avoid conversion to double and support big integers
        bind(index, value.toDouble())
    }

    actual fun setText(index: Int, value: String) {
        bind(index, value)
    }

    actual fun setNull(index: Int) {
        bind(index, null)
    }

    actual fun clear() {
        array = emptyArray()
    }

    private fun bind(index: Int, value: Any?) {
        ensureCapacity(index)
        array[index] = value
    }

    private fun ensureCapacity(index: Int) {
        val requiredSize = index + 1
        if (array.size < requiredSize) {
            array = array.copyOf(requiredSize)
        }
    }
}
