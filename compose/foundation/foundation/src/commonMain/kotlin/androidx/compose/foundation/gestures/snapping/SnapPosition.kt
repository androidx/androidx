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

import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Describes the general positioning of a given snap item in its containing layout.
 */
@ExperimentalFoundationApi
fun interface SnapPosition {
    /**
     * Calculates the anchor reference position where items will be snapped to in a snapping
     * container. For instance, if [SnapPosition.Center] is used, once the snapping finishes
     * one of the items in the snapping container will be aligned exactly to the position
     * returned by [position]. The value returned will be applied to the item's current offset
     * to generate its final positioning.
     *
     * The reference point is with respect to the start of the layout (including the content
     * padding).
     *
     * @param layoutSize The main axis layout size within which an item can be positioned.
     * @param itemSize The main axis size for the item being positioned within this snapping
     * layout.
     * @param beforeContentPadding The content padding in pixels applied before this Layout's
     * content.
     * @param afterContentPadding The content padding in pixels applied after this Layout's
     * content.
     * @param itemIndex The index of the item being positioned.
     */
    fun position(
        layoutSize: Int,
        itemSize: Int,
        beforeContentPadding: Int,
        afterContentPadding: Int,
        itemIndex: Int
    ): Int

    companion object {
        /**
         * Aligns the center of the item with the center of the containing layout.
         */
        val Center =
            SnapPosition { layoutSize, itemSize, beforeContentPadding, afterContentPadding, _ ->
                val availableLayoutSpace = layoutSize - beforeContentPadding - afterContentPadding
                // we use availableLayoutSpace / 2 as the main anchor point and we discount half
                // an item size so the item appear aligned with the center of the container.
                availableLayoutSpace / 2 - itemSize / 2
            }

        /**
         * Aligns the start of the item with the start of the containing layout.
         */
        val Start = SnapPosition { _, _, _, _, _ -> 0 }

        /**
         * Aligns the end of the item with the end of the containing layout.
         */
        val End =
            SnapPosition { layoutSize, itemSize, beforeContentPadding, afterContentPadding, _ ->
                val availableLayoutSpace = layoutSize - beforeContentPadding - afterContentPadding
                // the snap position for the item is the end of the layout, discounting the item
                // size
                availableLayoutSpace - itemSize
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun calculateDistanceToDesiredSnapPosition(
    mainAxisViewPortSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    itemSize: Int,
    itemOffset: Int,
    itemIndex: Int,
    snapPosition: SnapPosition
): Float {
    val desiredDistance = with(snapPosition) {
        position(
            mainAxisViewPortSize,
            itemSize,
            beforeContentPadding,
            afterContentPadding,
            itemIndex,
        )
    }.toFloat()

    return itemOffset - desiredDistance
}
