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

package androidx.glance.appwidget.lazy
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.unit.Dp
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
 * The DSL implementation of a lazy grid layout. It composes only visible rows of the grid.
 *
 * @param gridCells the number of columns in the grid.
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyVerticalGridScope.item] to add a single item or [LazyVerticalGridScope.items] to add a list
 * of items. If the item has more than one top-level child, they will be automatically wrapped in a
 * Box.
 */
@Composable
fun LazyVerticalGrid(
    gridCells: GridCells,
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyVerticalGridScope.() -> Unit
) {
    GlanceNode(
        factory = ::EmittableLazyVerticalGrid,
        update = {
            this.set(gridCells) { this.gridCells = it }
            this.set(modifier) { this.modifier = it }
            this.set(horizontalAlignment) { this.horizontalAlignment = it }
        },
        content = applyVerticalGridScope(
            Alignment(horizontalAlignment, Alignment.Vertical.CenterVertically),
            content
        )
    )
}

/**
 * The DSL implementation of a lazy grid layout. It composes only visible rows of the grid.
 *
 * @param gridCells the number of columns in the grid.
 * @param activityOptions Additional options built from an [android.app.ActivityOptions] to apply to
 * an activity start.
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyVerticalGridScope.item] to add a single item or [LazyVerticalGridScope.items] to add a list
 * of items. If the item has more than one top-level child, they will be automatically wrapped in a
 * Box.
 */
@ExperimentalGlanceApi
@Composable
fun LazyVerticalGrid(
    gridCells: GridCells,
    activityOptions: Bundle,
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyVerticalGridScope.() -> Unit
) {
    GlanceNode(
        factory = ::EmittableLazyVerticalGrid,
        update = {
            this.set(gridCells) { this.gridCells = it }
            this.set(modifier) { this.modifier = it }
            this.set(horizontalAlignment) { this.horizontalAlignment = it }
            this.set(activityOptions) { this.activityOptions = it }
        },
        content = applyVerticalGridScope(
            Alignment(horizontalAlignment, Alignment.Vertical.CenterVertically),
            content
        )
    )
}

internal fun applyVerticalGridScope(
    alignment: Alignment,
    content: LazyVerticalGridScope.() -> Unit
): @Composable () -> Unit {
    val itemList = mutableListOf<Pair<Long?, @Composable LazyItemScope.() -> Unit>>()
    val listScopeImpl = object : LazyVerticalGridScope {
        override fun item(itemId: Long, content: @Composable LazyItemScope.() -> Unit) {
            require(itemId == LazyVerticalGridScope.UnspecifiedItemId ||
                    itemId > ReservedItemIdRangeEnd) {
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
            val id = itemId.takeIf { it != LazyVerticalGridScope.UnspecifiedItemId }
                ?: (ReservedItemIdRangeEnd - index)
            check(id != LazyVerticalGridScope.UnspecifiedItemId) {
                "Implicit list item ids exhausted."
            }
            LazyVerticalGridItem(id, alignment) {
                object : LazyItemScope { }.composable()
            }
        }
    }
}

@Composable
private fun LazyVerticalGridItem(
    itemId: Long,
    alignment: Alignment,
    content: @Composable () -> Unit
) {
    // We wrap LazyVerticalGridItem in the key composable to ensure that lambda actions declared
    // within each item's scope will get a unique ID based on the currentCompositeKeyHash.
    key(itemId) {
        GlanceNode(
            factory = ::EmittableLazyVerticalGridListItem,
            update = {
                this.set(itemId) { this.itemId = it }
                this.set(alignment) { this.alignment = it }
            },
            content = content
        )
    }
}

@JvmDefaultWithCompatibility
/**
 * Receiver scope which is used by [LazyColumn].
 */
@LazyScopeMarker
interface LazyVerticalGridScope {
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
inline fun <T> LazyVerticalGridScope.items(
    items: List<T>,
    crossinline itemId: ((item: T) -> Long) = { LazyVerticalGridScope.UnspecifiedItemId },
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
inline fun <T> LazyVerticalGridScope.itemsIndexed(
    items: List<T>,
    crossinline itemId: ((index: Int, item: T) -> Long) =
        { _, _ -> LazyVerticalGridScope.UnspecifiedItemId },
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
inline fun <T> LazyVerticalGridScope.items(
    items: Array<T>,
    noinline itemId: ((item: T) -> Long) = { LazyVerticalGridScope.UnspecifiedItemId },
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
inline fun <T> LazyVerticalGridScope.itemsIndexed(
    items: Array<T>,
    noinline itemId: ((index: Int, item: T) -> Long) = {
      _, _ -> LazyVerticalGridScope.UnspecifiedItemId
    },
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = items(items.size, { index: Int -> itemId(index, items[index]) }) {
    itemContent(it, items[it])
}

internal abstract class EmittableLazyVerticalGridList :
EmittableWithChildren(resetsDepthForChildren = true) {
    override var modifier: GlanceModifier = GlanceModifier
    var horizontalAlignment: Alignment.Horizontal = Alignment.Start
    var gridCells: GridCells = GridCells.Fixed(1)
    var activityOptions: Bundle? = null

    override fun toString(): String =
        "EmittableLazyVerticalGridList(modifier=$modifier, " +
        "horizontalAlignment=$horizontalAlignment, " +
        "numColumn=$gridCells, " +
        "activityOptions=$activityOptions, " +
        "children=[\n${childrenToString()}\n])"
}

internal class EmittableLazyVerticalGridListItem : EmittableLazyItemWithChildren() {
    // Fill max width of the grid cell so that item contents can be aligned per the horizontal
    // alignment.
    override var modifier: GlanceModifier = GlanceModifier.wrapContentHeight().fillMaxWidth()
    var itemId: Long = 0

    override fun copy(): Emittable = EmittableLazyVerticalGridListItem().also {
        it.itemId = itemId
        it.alignment = alignment
        it.children.addAll(children.map { it.copy() })
    }

    override fun toString(): String =
        "EmittableLazyVerticalGridListItem(" +
        "modifier=$modifier, " +
        "alignment=$alignment, " +
        "children=[\n${childrenToString()}\n])"
}

internal class EmittableLazyVerticalGrid : EmittableLazyVerticalGridList() {
    override fun copy(): Emittable = EmittableLazyVerticalGrid().also {
        it.modifier = modifier
        it.horizontalAlignment = horizontalAlignment
        it.gridCells = gridCells
        it.activityOptions = activityOptions
        it.children.addAll(children.map { it.copy() })
    }
}

/**
 * Defines the number of columns of the GridView.
 */
sealed class GridCells {
  /**
   * Defines a fixed number of columns, limited to 1 through 5.
   *
   * For example, [LazyVerticalGrid] Fixed(3) would mean that there are 3 columns 1/3
   * of the parent wide.
   *
   * @param count number of columns in LazyVerticalGrid
   */
  class Fixed(val count: Int) : GridCells() {
      override fun equals(other: Any?): Boolean {
          if (this === other) return true
          if (javaClass != other?.javaClass) return false

          other as Fixed

          if (count != other.count) return false

          return true
      }

      override fun hashCode(): Int {
          return count
      }
  }

  /**
   * Defines a grid with as many columns as possible on the condition that
   * every cell has at least [minSize] space and all extra space distributed evenly.
   *
   * For example, for the vertical [LazyVerticalGrid] Adaptive(20.dp) would mean that
   * there will be as many columns as possible and every column will be at least 20.dp
   * and all the columns will have equal width. If the screen is 88.dp wide then
   * there will be 4 columns 22.dp each.
   *
   * @param minSize fixed width of each column in LazyVerticalGrid
   */
  @RequiresApi(31)
  class Adaptive(val minSize: Dp) : GridCells() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Adaptive

            if (minSize != other.minSize) return false

            return true
        }

        override fun hashCode(): Int {
            return minSize.hashCode()
        }
    }
}
