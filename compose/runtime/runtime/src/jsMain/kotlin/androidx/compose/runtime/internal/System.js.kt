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

private external interface WeakMap {
    fun set(key: Any, value: Int)

    fun get(key: Any): Int?
}

private val weakMap: WeakMap = js("new WeakMap()")

@NoLiveLiterals
private fun memoizeIdentityHashCode(instance: Any): Int {
    val value = nextHash++

    weakMap.set(instance, value)

    return value
}

internal actual fun identityHashCode(instance: Any?): Int {
    if (instance == null) {
        return 0
    }

    return weakMap.get(instance) ?: memoizeIdentityHashCode(instance)
}
