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

package androidx.glance.appwidget.lazy

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.glance.Emittable
import androidx.glance.EmittableLazyItemWithChildren
import androidx.glance.EmittableWithChildren
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.wrapContentHeight

/**
 * A vertical scrolling list that only lays out the currently visible items. The [content] block
 * defines a DSL which allows you to emit different list items.
 *
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items. If the
 * item has more than one top-level child, they will be automatically wrapped in a Box.
 */
// TODO(b/198618359): interaction handling
@Composable
fun LazyColumn(
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    GlanceNode(
        factory = ::EmittableLazyColumn,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(horizontalAlignment) { this.horizontalAlignment = it }
        },
        content = applyListScope(
            Alignment(horizontalAlignment, Alignment.Vertical.CenterVertically),
            content
        )
    )
}

/**
 * A vertical scrolling list that only lays out the currently visible items. The [content] block
 * defines a DSL which allows you to emit different list items.
 *
 * @param activityOptions Additional options built from an [android.app.ActivityOptions] to apply to
 * an activity start.
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items. If the
 * item has more than one top-level child, they will be automatically wrapped in a Box.
 */
@ExperimentalGlanceApi
@Composable
fun LazyColumn(
    activityOptions: Bundle,
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    GlanceNode(
        factory = ::EmittableLazyColumn,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(horizontalAlignment) { this.horizontalAlignment = it }
            this.set(activityOptions) { this.activityOptions = it }
        },
        content = applyListScope(
            Alignment(horizontalAlignment, Alignment.Vertical.CenterVertically),
            content
        )
    )
}

private fun applyListScope(
    alignment: Alignment,
    content: LazyListScope.() -> Unit
): @Composable () -> Unit {
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
        itemList.forEachIndexed { index, (itemId, composable) ->
            val id = itemId.takeIf { it != LazyListScope.UnspecifiedItemId }
                ?: (ReservedItemIdRangeEnd - index)
            check(id != LazyListScope.UnspecifiedItemId) { "Implicit list item ids exhausted." }
            LazyListItem(id, alignment) {
                object : LazyItemScope { }.apply { composable() }
            }
        }
    }
}

@Composable
private fun LazyListItem(
    itemId: Long,
    alignment: Alignment,
    content: @Composable () -> Unit
) {
    // We wrap LazyListItem in the key composable to ensure that lambda actions declared within each
    // item's scope will get a unique ID based on the currentCompositeKeyHash.
    key(itemId) {
        GlanceNode(
            factory = ::EmittableLazyListItem,
            update = {
                this.set(itemId) { this.itemId = it }
                this.set(alignment) { this.alignment = it }
            },
            content = content
        )
    }
}

/**
 * Values between -2^63 and -2^62 are reserved for list items whose id has not been explicitly
 * defined.
 */
internal const val ReservedItemIdRangeEnd = -0x4_000_000_000_000_000L

@DslMarker
annotation class LazyScopeMarker

/**
 * Receiver scope being used by the item content parameter of [LazyColumn].
 */
@LazyScopeMarker
interface LazyItemScope

@JvmDefaultWithCompatibility
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

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param itemId a factory of stable and unique ids representing the item. The value may not be
 * less than or equal to -2^62, as these values are reserved by the Glance API. Specifying
 * the list item ids will maintain the scroll position through app widget updates in Android
 * S and higher devices.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.items(
    items: List<T>,
    crossinline itemId: ((item: T) -> Long) = { LazyListScope.UnspecifiedItemId },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(items.size, { index: Int -> itemId(items[index]) }) {
    itemContent(items[it])
}

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param itemId a factory of stable and unique ids representing the item. The value may not be
 * less than or equal to -2^62, as these values are reserved by the Glance API. Specifying
 * the list item ids will maintain the scroll position through app widget updates in Android
 * S and higher devices.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.itemsIndexed(
    items: List<T>,
    crossinline itemId: ((index: Int, item: T) -> Long) =
        { _, _ -> LazyListScope.UnspecifiedItemId },
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = items(items.size, { index: Int -> itemId(index, items[index]) }) {
    itemContent(it, items[it])
}

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param itemId a factory of stable and unique list item ids. Using the same itemId for multiple
 * items in the array is not allowed. When you specify the itemId, the scroll position will be
 * maintained based on the itemId, which means if you add/remove items before the current visible
 * item the item with the given itemId will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.items(
    items: Array<T>,
    noinline itemId: ((item: T) -> Long) = { LazyListScope.UnspecifiedItemId },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(items.size, { index: Int -> itemId(items[index]) }) {
    itemContent(items[it])
}

/**
 * Adds a array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param itemId a factory of stable and unique list item ids. Using the same itemId for multiple
 * items in the array is not allowed. When you specify the itemId the scroll position will be
 * maintained based on the itemId, which means if you add/remove items before the current visible
 * item the item with the given itemId will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.itemsIndexed(
    items: Array<T>,
    noinline itemId: ((index: Int, item: T) -> Long) = { _, _ -> LazyListScope.UnspecifiedItemId },
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = items(items.size, { index: Int -> itemId(index, items[index]) }) {
    itemContent(it, items[it])
}

internal abstract class EmittableLazyList : EmittableWithChildren(resetsDepthForChildren = true) {
    override var modifier: GlanceModifier = GlanceModifier
    var horizontalAlignment: Alignment.Horizontal = Alignment.Start
    var activityOptions: Bundle? = null

    override fun toString() =
        "EmittableLazyList(modifier=$modifier, horizontalAlignment=$horizontalAlignment, " +
            "activityOptions=$activityOptions, children=[\n${childrenToString()}\n])"
}

internal class EmittableLazyListItem : EmittableLazyItemWithChildren() {
    // Fill max width of the lazy column so that item contents can be aligned per the horizontal
    // alignment.
    override var modifier: GlanceModifier = GlanceModifier.wrapContentHeight().fillMaxWidth()
    var itemId: Long = 0

    override fun copy(): Emittable = EmittableLazyListItem().also {
        it.itemId = itemId
        it.alignment = alignment
        it.children.addAll(children.map { it.copy() })
    }

    override fun toString() =
        "EmittableLazyListItem(modifier=$modifier, alignment=$alignment, " +
            "children=[\n${childrenToString()}\n])"
}

internal class EmittableLazyColumn : EmittableLazyList() {
    override fun copy(): Emittable = EmittableLazyColumn().also {
        it.modifier = modifier
        it.horizontalAlignment = horizontalAlignment
        it.activityOptions = activityOptions
        it.children.addAll(children.map { it.copy() })
    }
}
