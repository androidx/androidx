/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.datastore.preferences.core

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun <K, V> immutableMap(map: Map<K, V>): Map<K, V> {
    // TODO:(b/239829063) Find a replacement for java's unmodifyable map.  For now just make a copy.
    return map.toMap()
}

// TODO:(b/239829063) Find a replacement for java's unmodifyable set.  For now just make a copy.
internal actual fun <T> immutableCopyOfSet(set: Set<T>): Set<T> = set.toSet()

internal actual class AtomicBoolean actual constructor(initialValue: Boolean) {
    private var delegate: kotlinx.atomicfu.AtomicBoolean = atomic(initialValue)
    private var property by delegate

    actual fun get(): Boolean = property

    actual fun set(value: Boolean) {
        property = value
    }
}

// TODO(b/234049307): Pick a better dispatcher for IO
internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Default
