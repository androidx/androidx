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

package androidx.compose.runtime.internal

import androidx.compose.runtime.NoLiveLiterals

private var nextHash = 1

// @OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private external interface WeakMap {
    fun set(key: JsAny, value: Int)

    fun get(key: JsAny): Int?
}

// @OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private val weakMap: WeakMap = js("new WeakMap()")

// @OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@NoLiveLiterals
private fun memoizeIdentityHashCode(instance: JsAny): Int {
    val value = nextHash++

    weakMap.set(instance.toJsReference(), value)

    return value
}

// @OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
internal actual fun identityHashCode(instance: Any?): Int {
    if (instance == null) {
        return 0
    }

    val jsRef = instance.toJsReference()
    return weakMap.get(jsRef).takeIf { it != 0 } ?: memoizeIdentityHashCode(jsRef)
}
