/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.room.migration.bundle

/** Utility class to run schema equality on collections. */
internal object SchemaEqualityUtil {
    fun <T, K : SchemaEquality<K>> checkSchemaEquality(
        map1: Map<T, K>?,
        map2: Map<T, K>?
    ): Boolean {
        return when {
            map1 == null -> map2 == null
            map2 == null -> false
            map1.size != map2.size -> false
            else -> map1.entries.all { checkSchemaEquality(it.value, map2.get(it.key)) }
        }
    }

    fun <K : SchemaEquality<K>> checkSchemaEquality(list1: List<K>?, list2: List<K>?): Boolean {
        return when {
            list1 == null -> list2 == null
            list2 == null -> false
            list1.size != list2.size -> false
            else -> list1.all { item1 -> list2.any { item2 -> checkSchemaEquality(item1, item2) } }
        }
    }

    fun <K : SchemaEquality<K>> checkSchemaEquality(item1: K?, item2: K?): Boolean {
        return when {
            item1 == null -> item2 == null
            item2 == null -> false
            else -> item1.isSchemaEqual(item2)
        }
    }

    inline fun <K, reified R> Map<K, *>.filterValuesInstance(): Map<K, R> = buildMap {
        this@filterValuesInstance.forEach { (key, value) -> if (value is R) put(key, value) }
    }
}
