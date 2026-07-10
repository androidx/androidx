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

package androidx.camera.integration.featurecombo

import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.MutableObjectLongMap
import androidx.collection.ObjectLongMap

/**
 * This class contains utility functions related to primitive collections like [IntList],
 * [ObjectLongMap], etc. to make it easier to deal with
 * ``androidx.compose.lint.PrimitiveInCollectionDetector` lint detection.
 */
@Suppress("PrimitiveInCollection")
object PrimitiveCollections {
    /** Returns an [IntList] containing only the distinct elements from the given [list]. */
    fun List<Int>.distinctIntList(): IntList =
        MutableIntList().also { distinct().forEach { e -> it.add(e) } }

    /** Returns a list of the keys in the map. */
    fun <K> ObjectLongMap<K>.keyList(): List<K> {
        val keyList = mutableListOf<K>()

        forEach { k, _ -> keyList.add(k) }

        return keyList
    }

    /**
     * Increments the value of the given key by the given value.
     *
     * If the key is not present in the map, it is added with the given value.
     */
    fun <K> MutableObjectLongMap<K>.increment(key: K, value: Long) {
        if (contains(key)) {
            this[key] += value
        } else {
            this[key] = value
        }
    }
}
