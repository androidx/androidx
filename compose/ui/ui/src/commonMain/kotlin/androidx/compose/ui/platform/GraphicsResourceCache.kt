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

package androidx.compose.ui.platform

import androidx.collection.MutableScatterMap
import androidx.compose.ui.node.WeakReference

/**
 * A thread-safe cache for managing the lifecycle of graphics resources using [WeakReference]s. This
 * cache ensures that expensive graphics resources are created only when necessary and are
 * automatically reclaimed by the garbage collector when they are no longer strongly reachable.
 */
internal class GraphicsResourceCache {
    private val lock = makeSynchronizedObject(Any())
    private val cache = MutableScatterMap<Long, WeakReference<Any>>()

    private val maxSizeBeforeCleanup = 100

    /** Acquire a cached resource, creating it if necessary. */
    inline fun <T : Any> acquire(key: Long, create: () -> T): T {
        return synchronized(lock) {
            val weakRef = cache[key]

            val cachedValue = weakRef?.get()

            @Suppress("UNCHECKED_CAST")
            if (cachedValue != null) {
                return cachedValue as T
            }

            // Clean if cache is getting large
            if (cache.size >= maxSizeBeforeCleanup) {
                prune()
            }

            val newResource = create()
            cache[key] = WeakReference(newResource)
            newResource
        }
    }

    fun contains(key: Long): Boolean {
        return synchronized(lock) { cache[key]?.get() != null }
    }

    fun clear() {
        synchronized(lock) { cache.clear() }
    }

    /**
     * Iterates through the cache and removes any entries where the WeakReference has been cleared
     * by the garbage collector.
     */
    private fun prune() {
        cache.removeIf { _, value -> value.get() == null }
    }
}
