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
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.layout.Alignment
import androidx.glance.EmittableWithChildren
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.wrapContentHeight

/**
 * The DSL implementation of a lazy grid layout. It composes only visible rows of the grid.
 *
 * @param gridCells the number of columns in the grid.
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyVerticalGridScope.item] to add a single item or
 * [LazyVerticalGridScope.items] to add a list of items.
 */
@Composable
public fun LazyVerticalGrid(
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

internal fun applyVerticalGridScope(
    alignment: Alignment,
    content: LazyVerticalGridScope.() -> Unit
): @Composable () -> Unit {
    var nextImplicitItemId = ReservedItemIdRangeEnd
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
        itemList.forEach { (itemId, composable) ->
            val id = itemId.takeIf {
              it != LazyVerticalGridScope.UnspecifiedItemId } ?: nextImplicitItemId--
            check(id != LazyVerticalGridScope.UnspecifiedItemId) {
                "Implicit list item ids exhausted."
            }
            LazyVerticalGridItem(id, alignment) {
                object : LazyItemScope { }.apply { composable() }
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
    GlanceNode(
        factory = ::EmittableLazyVerticalGridListItem,
        update = {
            this.set(itemId) { this.itemId = it }
            this.set(alignment) { this.alignment = it }
        },
        content = content
    )
}

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

internal abstract class EmittableLazyVerticalGridList : EmittableWithChildren(
  resetsDepthForChildren = true
  ) {
  override var modifier: GlanceModifier = GlanceModifier
  public var horizontalAlignment: Alignment.Horizontal = Alignment.Start
  public var gridCells: GridCells = GridCells.Adaptive

  override fun toString() =
      "EmittableLazyVerticalGridList(modifier=$modifier, " +
      "horizontalAlignment=$horizontalAlignment, " +
      "numColumn=$gridCells, " +
      "children=[\n${childrenToString()}\n])"
}

internal class EmittableLazyVerticalGridListItem : EmittableWithChildren() {
  override var modifier: GlanceModifier
      get() = children.singleOrNull()?.modifier
          ?: GlanceModifier.wrapContentHeight().fillMaxWidth()
      set(_) {
          throw IllegalAccessError(
            "You cannot set the modifier of an EmittableLazyVerticalGridListItem"
          )
      }
  var itemId: Long = 0
  var alignment: Alignment = Alignment.CenterStart

  override fun toString() =
      "EmittableLazyVerticalGridListItem(" +
      "modifier=$modifier, " +
      "alignment=$alignment, " +
      "children=[\n${childrenToString()}\n])"
}

internal class EmittableLazyVerticalGrid : EmittableLazyVerticalGridList()

/**
 * Defines the number of columns of the GridView.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class GridCells internal constructor(private val value: Int) {
  override fun toString(): String {
      return when (value) {
          0 -> "GridCells.Adaptive"
          else -> "GridCells.Fixed($value)"
      }
  }

  companion object {
      fun Fixed(count: Int): GridCells {
          require(count in 1..5) {
              "Only counts from 1 to 5 are supported."
          }
          return GridCells(count)
      }

      val Adaptive = GridCells(0)
  }
}