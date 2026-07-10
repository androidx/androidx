/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/**
 * Provides the first item to layout for [TransformingLazyColumn].
 *
 * During a measurement pass, [TransformingLazyColumn] uses the item returned by this provider as
 * the initial placement reference. [TransformingLazyColumn] measures this item and places its
 * requested [ItemInfo.itemEdge] visual edge at the exact screen coordinate defined by
 * [ItemInfo.offset], prior to accounting for active scroll deltas. Once this initial item is
 * positioned, all other visible items are sequentially composed and placed above and below it.
 *
 * Providing this interface allows controlling how the list places its children during content
 * updates like additions, removals, or item size changes.
 *
 * @sample androidx.wear.compose.foundation.samples.TransformingLazyColumnFirstLayoutItemProviderSample
 */
public fun interface TransformingLazyColumnFirstLayoutItemProvider {
    /**
     * Returns the [ItemInfo] for the first item to layout in [TransformingLazyColumn].
     *
     * Note: This method is executed internally inside a `Snapshot.withoutReadObservation` block.
     * Any Compose state reads performed inside this callback will not trigger layout observation.
     *
     * If the returned [ItemInfo] cannot be fully resolved (e.g., the key is not found or the index
     * is out of bounds), [TransformingLazyColumn] falls back:
     * - If the [ItemInfo.key] is not found, it falls back to the [ItemInfo.index].
     * - The final resolved index is coerced to stay within the valid list bounds.
     *
     * @param centerItem The [ItemInfo] that [TransformingLazyColumn] would currently use for the
     *   first layout item if no provider is supplied. Returning this item preserves the default
     *   layout behavior. In most cases, this is the item closest to the center of the viewport from
     *   [TransformingLazyColumnLayoutInfo.visibleItems].
     * @return The [ItemInfo] of the item to use as the first item to layout.
     */
    public fun getFirstLayoutItem(centerItem: ItemInfo): ItemInfo

    /** Represents the visual edge of [ItemInfo] (Start or End) to which the offset refers. */
    @Immutable
    @JvmInline
    public value class ItemEdge internal constructor(internal val type: Int) {
        public companion object {
            /**
             * The start edge of the item.
             *
             * For normal layout this will be the visual top edge of the item. For reverseLayout it
             * will be the visual bottom edge.
             *
             * When requested, [TransformingLazyColumn] starts laying out items from this edge. For
             * example, during item size animations (such as expansion), the item expands towards
             * the logical end of the list relative to this start edge.
             */
            public val Start: ItemEdge = ItemEdge(0)

            /**
             * The end edge of the item.
             *
             * For normal layout this will be the visual bottom edge of the item. For reverseLayout
             * it will be the visual top edge.
             *
             * When requested, [TransformingLazyColumn] starts laying out items from this edge. For
             * example, during item size animations (such as expansion), the item expands towards
             * the logical start of the list relative to this end edge.
             */
            public val End: ItemEdge = ItemEdge(1)
        }

        override fun toString(): String {
            return when (this) {
                Start -> "ItemEdge.Start"
                End -> "ItemEdge.End"
                else -> "ItemEdge.Unknown"
            }
        }
    }

    /**
     * Holds information about the first item to layout in [TransformingLazyColumn].
     *
     * During measurement, [TransformingLazyColumn] resolves this item to its index, measures it,
     * and arranges all other items relative to its [offset] screen coordinate.
     *
     * @property index The index of the item. When [key] is provided, this value acts as the last
     *   known index. The [key] is prioritized to identify the item in the presence of dataset
     *   changes. If [key] is null or not found, this index is used instead. The final index used
     *   for layout is coerced to stay within the list bounds.
     * @property itemEdge The logical edge of the item (Start or End) to which [offset] refers.
     * @property offset The offset (in pixels) of the item's edge defined by [itemEdge].
     * @property key The stable key of the item, if one was provided. When specified, this key is
     *   prioritized over [index] to identify the item across content changes (such as item
     *   additions or removals), falling back to [index] only if the key is not found.
     */
    public class ItemInfo(
        public val index: Int,
        public val itemEdge: ItemEdge,
        public val offset: Int,
        public val key: Any? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ItemInfo) return false

            if (index != other.index) return false
            if (itemEdge != other.itemEdge) return false
            if (offset != other.offset) return false
            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + itemEdge.hashCode()
            result = 31 * result + offset
            result = 31 * result + (key?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "TransformingLazyColumnFirstLayoutItemProvider.ItemInfo(index=$index, " +
                "itemEdge=$itemEdge, offset=$offset, key=$key)"
        }
    }
}
