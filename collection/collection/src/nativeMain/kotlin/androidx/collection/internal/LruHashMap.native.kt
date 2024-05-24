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

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual class LruHashMap<K : Any, V : Any>
actual constructor(
    initialCapacity: Int,
    loadFactor: Float,
) {

    actual constructor(
        original: LruHashMap<out K, V>
    ) : this(
        /*
         * We can't call the primary constructor without passing values,
         * even though the expect constructor has all default values.
         * See https://youtrack.jetbrains.com/issue/KT-52193.
         */
        initialCapacity = 16,
        loadFactor = 0.75F,
    ) {
        for ((key, value) in original.entries) {
            put(key, value)
        }
    }

    private val map = LinkedHashMap<K, V>(initialCapacity, loadFactor)

    actual val isEmpty: Boolean
        get() = map.isEmpty()

    actual val entries: Set<Map.Entry<K, V>>
        get() = map.entries

    /**
     * Works similarly to Java LinkedHashMap with LRU order enabled. Removes the existing item from
     * the map if there is one, and then adds it back, so the item is moved to the end.
     */
    actual operator fun get(key: K): V? {
        val item = map.remove(key)
        if (item != null) {
            map[key] = item
        }

        return item
    }

    /**
     * Works similarly to Java LinkedHashMap with LRU order enabled. Removes the existing item from
     * the map if there is one, then inserts the new item to the map, so the item is moved to the
     * end.
     */
    actual fun put(key: K, value: V): V? {
        val item = map.remove(key)
        map[key] = value

        return item
    }

    actual fun remove(key: K): V? = map.remove(key)
}
