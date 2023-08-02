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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.datastore.preferences.core

import androidx.annotation.RestrictTo

import androidx.datastore.core.okio.OkioSerializer
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun <K, V> immutableMap(map: Map<K, V>): Map<K, V> {
    return Collections.unmodifiableMap(map)
}

internal actual fun <T> immutableCopyOfSet(set: Set<T>): Set<T> =
    Collections.unmodifiableSet(set.toSet())

internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

internal actual class AtomicBoolean actual constructor(initialValue: Boolean) {
    private val delegate: java.util.concurrent.atomic.AtomicBoolean

    actual fun set(value: Boolean) = delegate.set(value)

    actual fun get(): Boolean = delegate.get()

    init {
        delegate = AtomicBoolean(initialValue)
    }
}

internal actual fun getPreferencesSerializer(): OkioSerializer<Preferences> {
    return PreferencesSerializer
}