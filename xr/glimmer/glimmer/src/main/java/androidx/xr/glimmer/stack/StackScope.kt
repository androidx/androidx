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

package androidx.xr.glimmer.stack

import androidx.collection.MutableScatterMap
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** Receiver scope used by [VerticalStack] that defines a DSL for adding items to the stack. */
@Stable
public sealed interface StackScope {

    /**
     * Adds a single item.
     *
     * @param key a stable and unique key representing the item. If a key is specified, the scroll
     *   position will be maintained based on the key. If items are added/removed before the current
     *   visible item, the item with the given key will be kept as the first visible one. If null is
     *   passed, the position in the stack will represent the key.
     * @param content the content of the item
     */
    public fun item(key: Any? = null, content: @Composable StackItemScope.() -> Unit)

    /**
     * Adds a [count] of items.
     *
     * @param count the item count
     * @param key a factory of stable and unique keys representing the items. If a key is specified,
     *   the scroll position will be maintained based on the key. If items are added/removed before
     *   the current visible item, the item with the given key will be kept as the first visible
     *   one. If null is passed, the position in the stack will represent the key.
     * @param itemContent the content displayed by a single item
     */
    public fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        itemContent: @Composable StackItemScope.(Int) -> Unit,
    )
}

/**
 * Adds a list of items.
 *
 * @param items the list of item data
 * @param key a factory of stable and unique keys representing the items. If a key is specified, the
 *   scroll position will be maintained based on the key. If items are added/removed before the
 *   current visible item, the item with the given key will be kept as the first visible one. If
 *   null is passed, the position in the stack will represent the key.
 * @param itemContent the content displayed by a single item
 */
public inline fun <T> StackScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable StackItemScope.(item: T) -> Unit,
): Unit =
    items(
        count = items.size,
        key = key?.let { { index -> it(items[index]) } },
        itemContent = { index -> itemContent(items[index]) },
    )

/** Stack item holder that implements the item DSL allowing adding items to the stack. */
internal class StackItemHolder(private val state: StackState, content: StackScope.() -> Unit) :
    StackScope {

    val intervals: MutableIntervalList<StackItemInterval> = MutableIntervalList()

    val itemCount: Int
        get() = intervals.size

    init {
        apply(content)
    }

    override fun item(key: Any?, content: @Composable (StackItemScope.() -> Unit)) {
        intervals.addInterval(
            size = 1,
            StackItemInterval(state = state, key = key?.let { { it } }, item = { content() }),
        )
    }

    override fun items(
        count: Int,
        key: ((Int) -> Any)?,
        itemContent: @Composable (StackItemScope.(index: Int) -> Unit),
    ) {
        intervals.addInterval(
            size = count,
            StackItemInterval(state = state, key = key, item = itemContent),
        )
    }

    /**
     * Runs a [block] on the content of the item interval associated with the specified
     * [globalIndex] providing a local index in the given interval.
     */
    inline fun <T> withInterval(
        globalIndex: Int,
        block: (localIntervalIndex: Int, itemInterval: StackItemInterval) -> T,
    ): T {
        val interval = intervals[globalIndex]
        val localIntervalIndex = globalIndex - interval.startIndex
        return block(localIntervalIndex, interval.value)
    }

    /**
     * Returns the key for the item at the specified [globalIndex] if the key was provided,
     * otherwise fallback to returning [globalIndex] as the key.
     */
    fun getKey(globalIndex: Int): Any =
        withInterval(globalIndex) { localIntervalIndex, itemInterval ->
            itemInterval.getKeyOrDefault(
                globalIndex = globalIndex,
                localIntervalIndex = localIntervalIndex,
            )
        }

    internal fun getItemScope(globalIndex: Int): StackItemScopeImpl? =
        withInterval(globalIndex) { localIntervalIndex, itemInterval ->
            val key =
                itemInterval.getKeyOrDefault(
                    globalIndex = globalIndex,
                    localIntervalIndex = localIntervalIndex,
                )
            itemInterval.getItemScope(key)
        }
}

/** Represents an interval of stack items. */
internal class StackItemInterval(
    private val state: StackState,
    val key: ((index: Int) -> Any)?,
    val item: @Composable StackItemScope.(index: Int) -> Unit,
) {
    private val itemScopes = MutableScatterMap<Any, StackItemScopeImpl>()

    internal fun getItemScope(key: Any): StackItemScopeImpl? = itemScopes.get(key)

    internal fun getOrCreateItemScope(key: Any): StackItemScopeImpl =
        itemScopes.getOrPut(key, defaultValue = { StackItemScopeImpl(state) })

    internal fun getKeyOrDefault(globalIndex: Int, localIntervalIndex: Int) =
        // Fallback to the global index if no key is provided, which is the default behavior.
        key?.invoke(localIntervalIndex) ?: globalIndex
}
