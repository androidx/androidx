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

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsReference
import kotlin.js.get
import kotlin.js.toJsReference
import kotlin.js.unsafeCast

// We can't use Js WeakMap https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakMap
// because it uses identity equality for keys, but the usage of WeakKeysCache in SkikoParagraph requires that the
// keys would have structural equality.
@OptIn(ExperimentalWasmJsInterop::class)
internal actual class WeakKeysCache<K : Any, V: Any>  {

    private val cache = HashMap<Key<K>, V>()

    // When K is finalized, we'll eventually receive a callback.
    // And we use it to remove the Key(WeakReference<K>) from the HashMap.
    private val registry = FinalizationRegistry { keyJsReference ->
        val key: Key<K> = keyJsReference.unsafeCast<JsReference<Key<K>>>().get()
        cache.remove(key)
    }

    actual inline fun getOrPut(key: K, loader: (K) -> V): V {
        // Here we don't run a loop over the cache keys to clear the unavailable (after GC) keys.
        // The loop would be not ignorably cheap, because it would make a js-interop call for deref on
        // every iteration - a concern for scrolls with different text items (getOrPut is called often).
        // Here we rely on FinalizationRegistry to clean the unavailable (after GC) keys.
        val weakKey = Key(key)

        val existing = cache[weakKey]
        if (existing != null) return existing

        val value = loader(key)
        cache[weakKey] = value

        registry.register(key.toJsReference(), weakKey.toJsReference())

        return value
    }

    internal class Key<K : Any>(key: K) {
        private val ref = WeakReference(key)
        private val hash: Int = key.hashCode()

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

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakRef
@OptIn(ExperimentalWasmJsInterop::class)
private external class WeakRef {
    constructor(target: JsAny)
    fun deref(): JsAny?
}

@OptIn(ExperimentalWasmJsInterop::class)
private class WeakReference<T : Any>(reference: T) {
    private var weakRef: WeakRef? = WeakRef(reference.toJsReference())
    fun get(): T? = weakRef?.deref()?.unsafeCast<JsReference<T>>()?.get()
}

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/FinalizationRegistry
@OptIn(ExperimentalWasmJsInterop::class)
internal external class FinalizationRegistry(cleanup: (JsAny) -> Unit) {
    fun register(target: JsAny, heldValue: JsAny)
}