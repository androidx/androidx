/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.carousel

import androidx.compose.foundation.gestures.snapping.SnapPosition
import kotlin.math.roundToInt

/**
 * Calculates the offset from the beginning of the carousel container needed to snap to the item at
 * [itemIndex].
 *
 * This method takes into account the correct keyline list needed to allow the item to be fully
 * visible and located at a focal position.
 */
internal fun getSnapPositionOffset(strategy: Strategy, itemIndex: Int, itemCount: Int): Int {
    if (!strategy.isValid) return 0

    // Default to snapping the first focal keyline to its resting position. This will cover
    // snapping any item index that does not need shifting to bring it into the focal range.
    // Meaning any index that is greater than startStepsSize and less than itemCount - endStepsSize.
    var offset =
        (strategy.defaultKeylines.firstFocal.unadjustedOffset - strategy.itemMainAxisSize / 2F)
            .roundToInt()

    if (itemIndex <= strategy.startKeylineSteps.lastIndex) {
        // Items at the start of the list will always align with their corresponding step index
        // when in focus.
        // Since keyline steps are in reverse order (default step to fist start/end step), get the
        // step from last to first.
        val stepIndex =
            (strategy.startKeylineSteps.lastIndex - itemIndex).coerceIn(
                0,
                strategy.startKeylineSteps.lastIndex,
            )
        val startKeylines = strategy.startKeylineSteps[stepIndex]
        offset =
            (startKeylines.firstFocal.unadjustedOffset - strategy.itemMainAxisSize / 2f)
                .roundToInt()
    }

    val lastItemIndex = itemCount - 1
    if (
        itemIndex >= lastItemIndex - strategy.endKeylineSteps.lastIndex &&
            // If all items fall on focal keylines, skip the end steps
            itemCount > strategy.defaultKeylines.focalCount
    ) {
        // The item index's distance from the end of itemCount will align with its corresponding
        // end step.
        // Since keylines steps are in reverse order (default step to final end step), get the step
        // from last to first
        val stepIndex =
            (strategy.endKeylineSteps.lastIndex - (lastItemIndex - itemIndex)).coerceIn(
                0,
                strategy.endKeylineSteps.lastIndex,
            )
        val endKeylines = strategy.endKeylineSteps[stepIndex]
        offset =
            (endKeylines.firstFocal.unadjustedOffset - strategy.itemMainAxisSize / 2f).roundToInt()
    }

    return offset
}

internal fun KeylineSnapPosition(pageSize: CarouselPageSize): SnapPosition =
    object : SnapPosition {
        override fun position(
            layoutSize: Int,
            itemSize: Int,
            beforeContentPadding: Int,
            afterContentPadding: Int,
            itemIndex: Int,
            itemCount: Int,
        ): Int {
            return getSnapPositionOffset(pageSize.strategy, itemIndex, itemCount)
        }
    }
