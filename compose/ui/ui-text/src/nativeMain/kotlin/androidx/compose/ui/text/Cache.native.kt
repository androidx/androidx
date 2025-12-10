/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

internal actual class WeakKeysCache<K : Any, V: Any> {
    // TODO Use WeakHashMap once available https://youtrack.jetbrains.com/issue/KT-48075
    private val cache = HashMap<Key<K>, V>()

    actual inline fun getOrPut(key: K, loader: (K) -> V): V {
        cache.entries.removeAll { !it.key.isAvailable }
        return cache.getOrPut(Key(key)) { loader(key) }
    }

    @OptIn(ExperimentalNativeApi::class)
    internal class Key<K : Any>(key: K) {
        private val ref = WeakReference(key)
        private val hash: Int = key.hashCode()

        val isAvailable get() = ref.value != null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null) return false
            other as Key<*>
            val a = ref.get()
            val b = other.ref.get()
            if (a == null || b == null) {
                // If either side is cleared, they should not be considered equal
                return false
            }
            return a == b
        }

        override fun hashCode(): Int = hash
    }
}