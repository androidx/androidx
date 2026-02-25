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

import androidx.sqlite.driver.web.util.toUint8Array

/* Object container of a prepared statement binding parameters. */
internal actual class StatementBindings {

    var array: JsArray<JsAny?> = JsArray()

    actual fun setBlob(index: Int, value: ByteArray) {
        bind(index, value.toUint8Array())
    }

    actual fun setDouble(index: Int, value: Double) {
        bind(index, value.toJsNumber())
    }

    actual fun setLong(index: Int, value: Long) {
        // TODO: Use BigInt to support large values outside the range of a JsNumber
        bind(index, value.toDouble().toJsNumber())
    }

    actual fun setText(index: Int, value: String) {
        bind(index, value.toJsString())
    }

    actual fun setNull(index: Int) {
        bind(index, null)
    }

    actual fun clear() {
        array = JsArray()
    }

    private fun bind(index: Int, value: JsAny?) {
        // JavaScript arrays grow in size when assigning a value at an index greater than its length
        array[index] = value
    }
}
