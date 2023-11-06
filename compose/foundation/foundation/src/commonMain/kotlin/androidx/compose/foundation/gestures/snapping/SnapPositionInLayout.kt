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
fun interface SnapPositionInLayout {
    /**
     * Calculates an offset positioning between a container and an element within this container.
     * The offset calculation is the necessary diff that should be applied to the item offset to
     * align the item with a position within the container. As a base line, if we wanted to align
     * the start of the container and the start of the item, we would return 0 in this function.
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
        val CenterToCenter =
            SnapPositionInLayout {
                layoutSize, itemSize, beforeContentPadding, afterContentPadding, _ ->
                val availableLayoutSpace = layoutSize - beforeContentPadding - afterContentPadding
                availableLayoutSpace / 2 - itemSize / 2
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
    snapPositionInLayout: SnapPositionInLayout
): Float {
    val desiredDistance = with(snapPositionInLayout) {
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
