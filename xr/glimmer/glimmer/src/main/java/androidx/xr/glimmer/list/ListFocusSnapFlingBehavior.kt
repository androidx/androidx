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

package androidx.xr.glimmer.list

import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.sign

/**
 * List snapping aligns the focus line with the center of the closest item. Combined with adaptive
 * scrolling, this ensures that users have to move the same distance when swipes between items of
 * the similar size, resulting in predictable movement.
 *
 * @param state The [ListState] to observe for layout and focus information.
 * @return A focus-aware [SnapLayoutInfoProvider] instance.
 */
public fun SnapLayoutInfoProvider(state: ListState): SnapLayoutInfoProvider =
    object : SnapLayoutInfoProvider {
        /**
         * Calculates the approach offset for the decay animation before starting the snapping
         * phase. The returned offset is one item less than the suggested [decayOffset] to ensure a
         * smoother animation.
         */
        override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float {
            val layoutInfo = state.layoutInfoState.value
            if (layoutInfo.totalItemsCount == 0) {
                return 0f
            }
            val averageItemSize = layoutInfo.visibleItemsAverageSize
            return (abs(decayOffset) - averageItemSize).coerceAtLeast(0.0f) * decayOffset.sign
        }

        /**
         * Calculates the final snap offset to align the center of the closest item with the focus
         * line.
         */
        override fun calculateSnapOffset(velocity: Float): Float {
            if (state.layoutInfo.totalItemsCount == 0) {
                return 0f
            }
            val autoFocusMeasureResult = state.autoFocusState.properties ?: return 0f
            val focusScroll = autoFocusMeasureResult.focusScroll.toFloat()

            var lowerBoundOffset = Float.NEGATIVE_INFINITY
            var upperBoundOffset = Float.POSITIVE_INFINITY

            state.layoutInfo.visibleItemsInfo.fastForEach { item ->
                // Measure the distance between the center of the item and the focus line.
                val offset = item.offset + item.size / 2 - focusScroll

                // Find the closest item before the focus line
                if (offset <= 0 && offset > lowerBoundOffset) {
                    lowerBoundOffset = offset
                }

                // Find the closest item after the focus line
                if (0 <= offset && offset < upperBoundOffset) {
                    upperBoundOffset = offset
                }
            }

            return if (abs(lowerBoundOffset) <= upperBoundOffset) {
                lowerBoundOffset
            } else {
                upperBoundOffset
            }
        }
    }
