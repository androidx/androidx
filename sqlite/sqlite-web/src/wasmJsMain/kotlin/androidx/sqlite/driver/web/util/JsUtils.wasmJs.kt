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

package androidx.sqlite.driver.web.util

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set

internal fun <T : JsAny> initExternal(): T = js("({})")

internal fun JsArray<JsNumber>.toIntArray() = IntArray(length) { this[it]!!.toInt() }

internal fun JsArray<JsString>.toStringArray() = Array(length) { this[it]!!.toString() }

internal fun ByteArray.toUint8Array(): Uint8Array =
    Uint8Array(size).apply { forEachIndexed { index, byte -> this[index] = byte } }
