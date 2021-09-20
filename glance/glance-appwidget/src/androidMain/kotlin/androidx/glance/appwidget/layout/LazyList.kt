@file:OptIn(GlanceInternalApi::class)
/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.layout

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.glance.Applier
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.layout.Alignment

/**
 * A vertical scrolling list that only lays out the currently visible items. The [content] block
 * defines a DSL which allows you to emit different list items.
 *
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items.
 *
 * TODO(b/198618359): apply modifiers for column and column items, support alignment, click handling
 */
@Composable
public fun LazyColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    ComposeNode<EmittableLazyColumn, Applier>(
        factory = ::EmittableLazyColumn,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(horizontalAlignment) { this.horizontalAlignment = it }
        },
        content = applyListScope(content)
    )
}

private fun applyListScope(content: LazyListScope.() -> Unit): @Composable () -> Unit {
    var nextImplicitItemId = ReservedItemIdRangeEnd
    val itemList = mutableListOf<Pair<Long?, @Composable LazyItemScope.() -> Unit>>()
    val listScopeImpl = object : LazyListScope {
        override fun item(itemId: Long, content: @Composable LazyItemScope.() -> Unit) {
            require(itemId == LazyListScope.UnspecifiedItemId || itemId > ReservedItemIdRangeEnd) {
                """
                    You may not specify item ids less than $ReservedItemIdRangeEnd in a Glance
                    widget. These are reserved.
                """.trimIndent()
            }
            itemList.add(itemId to content)
        }

        override fun items(
            count: Int,
            itemId: ((index: Int) -> Long),
            itemContent: @Composable LazyItemScope.(index: Int) -> Unit
        ) {
            repeat(count) { index ->
                item(itemId(index)) { itemContent(index) }
            }
        }
    }
    listScopeImpl.apply(content)
    return {
        itemList.forEach { (itemId, composable) ->
            val id = itemId.takeIf { it != LazyListScope.UnspecifiedItemId } ?: nextImplicitItemId--
            check(id != LazyListScope.UnspecifiedItemId) { "Implicit list item ids exhausted." }
            LazyListItem(id) {
                object : LazyItemScope { }.apply { composable() }
            }
        }
    }
}

@OptIn(GlanceInternalApi::class)
@Composable
private fun LazyListItem(
    itemId: Long,
    content: @Composable () -> Unit
) {
    ComposeNode<EmittableLazyListItem, Applier>(
        factory = ::EmittableLazyListItem,
        update = {
            this.set(itemId) { this.itemId = it }
        },
        content = content
    )
}

/**
 * Values between -2^63 and -2^62 are reserved for list items whose id has not been explicitly
 * defined.
 */
@VisibleForTesting
internal const val ReservedItemIdRangeEnd = -0x4_000_000_000_000_000L

@DslMarker
annotation class LazyScopeMarker

/**
 * Receiver scope being used by the item content parameter of [LazyColumn].
 */
@LazyScopeMarker
interface LazyItemScope

/**
 * Receiver scope which is used by [LazyColumn].
 */
@LazyScopeMarker
interface LazyListScope {
    /**
     * Adds a single item.
     *
     * @param itemId a stable and unique id representing the item. The value may not be less than
     * or equal to -2^62, as these values are reserved by the Glance API. Specifying the list
     * item ids will maintain the scroll position through app widget updates in Android S and
     * higher devices.
     * @param content the content of the item
     */
    fun item(itemId: Long = UnspecifiedItemId, content: @Composable LazyItemScope.() -> Unit)

    /**
     * Adds a [count] of items.
     *
     * @param count the count of items
     * @param itemId a factory of stable and unique ids representing the item. The value may not be
     * less than or equal to -2^62, as these values are reserved by the Glance API. Specifying
     * the list item ids will maintain the scroll position through app widget updates in Android
     * S and higher devices.
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        itemId: ((index: Int) -> Long) = { UnspecifiedItemId },
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    )

    companion object {
        const val UnspecifiedItemId = Long.MIN_VALUE
    }
}

// TODO(b/198618359): Add inline helper functions with List<T> and Array<T> params.

@OptIn(GlanceInternalApi::class)
internal abstract class EmittableLazyList : EmittableWithChildren() {
    override var modifier: Modifier = Modifier
    public var horizontalAlignment: Alignment.Horizontal = Alignment.Start

    override fun toString() =
        "EmittableLazyList(modifier=$modifier, children=[\n${childrenToString()}\n])"
}

@OptIn(GlanceInternalApi::class)
internal class EmittableLazyListItem : EmittableWithChildren() {
    override var modifier: Modifier = Modifier
    var itemId: Long = 0

    override fun toString() =
        "EmittableLazyListItem(modifier=$modifier, children=[\n${childrenToString()}\n])"
}

internal class EmittableLazyColumn : EmittableLazyList()
