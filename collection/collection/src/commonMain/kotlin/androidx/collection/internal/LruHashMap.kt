/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.collection.internal

/**
 * There is no equivalent to Java `LinkedHashMap(initialCapacity, loadFactor, accessOrder)` in
 * Kotlin/Native. This class provides only necessary things, so it doesn't implement the whole
 * `MutableMap` interface.
 *
 * See [KT-52183](https://youtrack.jetbrains.com/issue/KT-52183).
 */
internal expect class LruHashMap<K : Any, V : Any>(
    initialCapacity: Int = 16,
    loadFactor: Float = 0.75F,
) {

    constructor(original: LruHashMap<out K, V>)

    val isEmpty: Boolean
    val entries: Set<Map.Entry<K, V>>

    operator fun get(key: K): V?

    fun put(key: K, value: V): V?

    fun remove(key: K): V?
}
