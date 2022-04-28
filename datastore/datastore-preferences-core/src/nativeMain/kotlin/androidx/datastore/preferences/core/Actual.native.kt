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

package androidx.datastore.preferences.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher

internal actual fun <K, V> immutableMap(map:Map<K, V>):Map<K, V> {
    //todo: Way to guarantee immutability?  kotlinx.collections.immutable?
    return map
}


internal actual fun <T> immutableSet(set:Set<T>):Set<T> {
    //todo: Way to guarantee immutability? kotlinx.collections.immutable?
    return set
}

// TODO this should be IO, not default.
internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Default


//todo: make really atomic
internal actual class AtomicBoolean actual constructor(initialValue: Boolean) {

    private var value = initialValue

    actual fun set(value: Boolean) {
        this.value = value
    }

    actual fun get(): Boolean = this.value
}