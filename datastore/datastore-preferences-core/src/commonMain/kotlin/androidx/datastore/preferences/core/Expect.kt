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

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.coroutines.CoroutineDispatcher

internal expect fun <K, V> immutableMap(map: Map<K, V>): Map<K, V>
internal expect fun <T> immutableCopyOfSet(set: Set<T>): Set<T>

internal expect fun ioDispatcher(): CoroutineDispatcher

internal expect class AtomicBoolean {
    constructor(initialValue: Boolean)
    fun set(value: Boolean)
    fun get(): Boolean
}

internal expect fun getPreferencesSerializer(): OkioSerializer<Preferences>