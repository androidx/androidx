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

@file:JvmName("RelationUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)

package androidx.room.util

import androidx.annotation.RestrictTo
import androidx.collection.ArrayMap
import androidx.collection.LongSparseArray
import androidx.room.RoomDatabase

/**
 * Utility function used in generated code to recursively fetch relationships when the amount of
 * keys exceed [RoomDatabase.MAX_BIND_PARAMETER_CNT].
 *
 * @param map - The map containing the relationship keys to fill-in.
 * @param isRelationCollection - True if [V] is a [Collection] which means it is non null.
 * @param fetchBlock - A lambda for calling the generated _fetchRelationship function.
 */
fun <K : Any, V> recursiveFetchHashMap(
    map: HashMap<K, V>,
    isRelationCollection: Boolean,
    fetchBlock: (HashMap<K, V>) -> Unit
) {
    val tmpMap = HashMap<K, V>(RoomDatabase.MAX_BIND_PARAMETER_CNT)
    var count = 0
    for (key in map.keys) {
        // Safe because `V` is a nullable type arg when isRelationCollection == false and vice versa
        @Suppress("UNCHECKED_CAST")
        if (isRelationCollection) {
            tmpMap[key] = map[key] as V
        } else {
            tmpMap[key] = null as V
        }
        count++
        if (count == RoomDatabase.MAX_BIND_PARAMETER_CNT) {
            // recursively load that batch
            fetchBlock(tmpMap)
            // for non collection relation, put the loaded batch in the original map,
            // not needed when dealing with collections since references are passed
            if (!isRelationCollection) {
                map.putAll(tmpMap)
            }
            tmpMap.clear()
            count = 0
        }
    }
    if (count > 0) {
        // load the last batch
        fetchBlock(tmpMap)
        // for non collection relation, put the last batch in the original map
        if (!isRelationCollection) {
            map.putAll(tmpMap)
        }
    }
}

/**
 * Same as [recursiveFetchHashMap] but for [LongSparseArray].
 */
fun <V> recursiveFetchLongSparseArray(
    map: LongSparseArray<V>,
    isRelationCollection: Boolean,
    fetchBlock: (LongSparseArray<V>) -> Unit
) {
    val tmpMap = LongSparseArray<V>(RoomDatabase.MAX_BIND_PARAMETER_CNT)
    var count = 0
    var mapIndex = 0
    val limit = map.size()
    while (mapIndex < limit) {
        if (isRelationCollection) {
            tmpMap.put(map.keyAt(mapIndex), map.valueAt(mapIndex))
        } else {
            // Safe because `V` is a nullable type arg when isRelationCollection == false
            @Suppress("UNCHECKED_CAST")
            tmpMap.put(map.keyAt(mapIndex), null as V)
        }
        mapIndex++
        count++
        if (count == RoomDatabase.MAX_BIND_PARAMETER_CNT) {
            fetchBlock(tmpMap)
            if (!isRelationCollection) {
                map.putAll(tmpMap)
            }
            tmpMap.clear()
            count = 0
        }
    }
    if (count > 0) {
        fetchBlock(tmpMap)
        if (!isRelationCollection) {
            map.putAll(tmpMap)
        }
    }
}

/**
 * Same as [recursiveFetchHashMap] but for [ArrayMap].
 */
fun <K : Any, V> recursiveFetchArrayMap(
    map: ArrayMap<K, V>,
    isRelationCollection: Boolean,
    fetchBlock: (ArrayMap<K, V>) -> Unit
) {
    val tmpMap = ArrayMap<K, V>(RoomDatabase.MAX_BIND_PARAMETER_CNT)
    var count = 0
    var mapIndex = 0
    val limit = map.size
    while (mapIndex < limit) {
        if (isRelationCollection) {
            tmpMap[map.keyAt(mapIndex)] = map.valueAt(mapIndex)
        } else {
            tmpMap[map.keyAt(mapIndex)] = null
        }
        mapIndex++
        count++
        if (count == RoomDatabase.MAX_BIND_PARAMETER_CNT) {
            fetchBlock(tmpMap)
            if (!isRelationCollection) {
                // Cast needed to disambiguate from putAll(SimpleArrayMap)
                map.putAll(tmpMap as Map<K, V>)
            }
            tmpMap.clear()
            count = 0
        }
    }
    if (count > 0) {
        fetchBlock(tmpMap)
        if (!isRelationCollection) {
            // Cast needed to disambiguate from putAll(SimpleArrayMap)
            map.putAll(tmpMap as Map<K, V>)
        }
    }
}
