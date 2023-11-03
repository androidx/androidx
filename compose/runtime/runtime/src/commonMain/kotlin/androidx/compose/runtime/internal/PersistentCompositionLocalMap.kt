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

package androidx.compose.runtime.internal

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.PersistentCompositionLocalMap
import androidx.compose.runtime.State
import androidx.compose.runtime.external.kotlinx.collections.immutable.ImmutableSet
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMapBuilder
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableMap.TrieNode
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.MutabilityOwnership
import androidx.compose.runtime.mutate
import androidx.compose.runtime.read

internal class PersistentCompositionLocalHashMap(
    node: TrieNode<CompositionLocal<Any?>, State<Any?>>,
    size: Int
) : PersistentHashMap<CompositionLocal<Any?>, State<Any?>>(node, size),
    PersistentCompositionLocalMap {

    override val entries: ImmutableSet<Map.Entry<CompositionLocal<Any?>, State<Any?>>>
        get() = super.entries

    override fun <T> get(key: CompositionLocal<T>): T = read(key)

    override fun putValue(
        key: CompositionLocal<Any?>,
        value: State<Any?>
    ): PersistentCompositionLocalMap {
        val newNodeResult = node.put(key.hashCode(), key, value, 0) ?: return this
        return PersistentCompositionLocalHashMap(
            newNodeResult.node,
            size + newNodeResult.sizeDelta
        )
    }

    override fun builder(): Builder {
        return Builder(this)
    }

    class Builder(
        internal var map: PersistentCompositionLocalHashMap
    ) : PersistentHashMapBuilder<CompositionLocal<Any?>, State<Any?>>(map),
        PersistentCompositionLocalMap.Builder {
        override fun build(): PersistentCompositionLocalHashMap {
            map = if (node === map.node) {
                map
            } else {
                ownership = MutabilityOwnership()
                PersistentCompositionLocalHashMap(node, size)
            }
            return map
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        val Empty = PersistentCompositionLocalHashMap(
            node = TrieNode.EMPTY as TrieNode<CompositionLocal<Any?>, State<Any?>>,
            size = 0
        )
    }
}

internal fun persistentCompositionLocalHashMapOf() = PersistentCompositionLocalHashMap.Empty

internal fun persistentCompositionLocalHashMapOf(
    vararg pairs: Pair<CompositionLocal<Any?>, State<Any?>>
): PersistentCompositionLocalMap = PersistentCompositionLocalHashMap.Empty.mutate { it += pairs }
