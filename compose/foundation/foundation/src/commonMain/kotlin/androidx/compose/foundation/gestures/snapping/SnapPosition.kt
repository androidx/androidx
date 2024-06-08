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

package androidx.compose.foundation.gestures.snapping

import androidx.compose.runtime.Stable

/**
 * Describes the snapping positioning (i.e. final positioning after snapping animation finishes) of
 * a given snap item in its containing layout.
 */
@Stable
interface SnapPosition {
    /**
     * Calculates the snap position where items will be aligned to in a snapping container. For
     * instance, if [SnapPosition.Center] is used, once the snapping finishes the center of one of
     * the items in the snapping container will be aligned exactly to the center of the snapping
     * container, that is because the value returned by [position] was calculated as such a way that
     * when one applies it to the item's current offset it will generate that final positioning.
     *
     * The reference point is with respect to the start of the layout (including the content
     * padding)
     *
     * @sample androidx.compose.foundation.samples.SnapFlingBehaviorSnapPosition
     * @param layoutSize The main axis layout size within which an item can be positioned.
     * @param itemSize The main axis size for the item being positioned within this snapping layout.
     * @param beforeContentPadding The content padding in pixels applied before this Layout's
     *   content.
     * @param afterContentPadding The content padding in pixels applied after this Layout's content.
     * @param itemIndex The index of the item being positioned.
     * @param itemCount The total amount of items in the snapping container.
     * @return The offset of the snap position where items will be aligned to in a snapping
     *   container.
     */
    fun position(
        layoutSize: Int,
        itemSize: Int,
        beforeContentPadding: Int,
        afterContentPadding: Int,
        itemIndex: Int,
        itemCount: Int
    ): Int

    /** Aligns the center of the item with the center of the containing layout. */
    object Center : SnapPosition {
        override fun position(
            layoutSize: Int,
            itemSize: Int,
            beforeContentPadding: Int,
            afterContentPadding: Int,
            itemIndex: Int,
            itemCount: Int
        ): Int {
            val availableLayoutSpace = layoutSize - beforeContentPadding - afterContentPadding
            // we use availableLayoutSpace / 2 as the main anchor point and we discount half
            // an item size so the item appear aligned with the center of the container.
            return availableLayoutSpace / 2 - itemSize / 2
        }

        override fun toString(): String {
            return "Center"
        }
    }

    /** Aligns the start of the item with the start of the containing layout. */
    object Start : SnapPosition {
        override fun position(
            layoutSize: Int,
            itemSize: Int,
            beforeContentPadding: Int,
            afterContentPadding: Int,
            itemIndex: Int,
            itemCount: Int
        ): Int = 0

        override fun toString(): String {
            return "Start"
        }
    }

    /** Aligns the end of the item with the end of the containing layout. */
    object End : SnapPosition {
        override fun position(
            layoutSize: Int,
            itemSize: Int,
            beforeContentPadding: Int,
            afterContentPadding: Int,
            itemIndex: Int,
            itemCount: Int
        ): Int {
            val availableLayoutSpace = layoutSize - beforeContentPadding - afterContentPadding
            // the snap position for the item is the end of the layout, discounting the item
            // size
            return availableLayoutSpace - itemSize
        }

        override fun toString(): String {
            return "End"
        }
    }
}

internal fun calculateDistanceToDesiredSnapPosition(
    mainAxisViewPortSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    itemSize: Int,
    itemOffset: Int,
    itemIndex: Int,
    snapPosition: SnapPosition,
    itemCount: Int
): Float {
    val desiredDistance =
        with(snapPosition) {
                position(
                    mainAxisViewPortSize,
                    itemSize,
                    beforeContentPadding,
                    afterContentPadding,
                    itemIndex,
                    itemCount
                )
            }
            .toFloat()

    return itemOffset - desiredDistance
}
