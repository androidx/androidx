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

package androidx.collection.integration

import androidx.collection.LruCache

/**
 * Integration (actually build) test that LruCache can be subclassed.
 */
@Suppress("unused")
class LruCacheKotlin<K : Any, V : Any>(maxSize: Int) : LruCache<K, V>(maxSize) {
    override fun resize(maxSize: Int) {
        super.resize(maxSize)
    }

    override fun trimToSize(maxSize: Int) {
        super.trimToSize(maxSize)
    }

    override fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {
        super.entryRemoved(evicted, key, oldValue, newValue)
    }

    override fun create(key: K): V? {
        return super.create(key)
    }

    override fun sizeOf(key: K, value: V): Int {
        return super.sizeOf(key, value)
    }
}

@Suppress("unused")
fun sourceCompatibility(): Int {
    val lruCache = LruCache<Int, Int>(10)
    return lruCache[1]!! + lruCache.size()
}
