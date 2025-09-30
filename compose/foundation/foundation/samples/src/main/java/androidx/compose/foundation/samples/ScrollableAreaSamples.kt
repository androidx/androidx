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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.scrollableArea
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.abs

@Sampled
@Composable
fun ScrollableAreaSample() {
    // This sample demonstrates how to create custom scrollable containers using the scrollableArea
    // modifier.

    // This ScrollableAreaSampleScrollState holds the scroll position and other relevant
    // information. It implements the ScrollableState interface, making it compatible with the
    // scrollableArea modifier, and is similar in function to the ScrollState used with
    // Modifier.verticalScroll.
    val scrollState = rememberScrollableAreaSampleScrollState()

    // For lists with many items, consider using a LazyLayout instead
    Layout(
        modifier =
            Modifier.size(150.dp)
                .scrollableArea(scrollState, Orientation.Vertical)
                .background(Color.LightGray),
        content = {
            repeat(40) {
                Text(
                    modifier = Modifier.padding(vertical = 2.dp),
                    text = "Item $it",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) { measurables, constraints ->
        var totalHeight = 0

        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables =
            measurables.map { measurable ->
                val placeable = measurable.measure(childConstraints)
                totalHeight += placeable.height
                placeable
            }

        val viewportHeight = constraints.maxHeight
        // Update the maximum scroll value to not scroll beyond limits and stop when scroll
        // reaches the end.
        scrollState.maxValue = (totalHeight - viewportHeight).coerceAtLeast(0)

        // Position the children within the layout.
        layout(constraints.maxWidth, viewportHeight) {
            // The current vertical scroll position, in pixels.
            val scrollY = scrollState.value
            val viewportCenterY = scrollY + viewportHeight / 2

            var placeableLayoutPositionY = 0
            placeables.forEach { placeable ->
                // This sample applies a scaling effect to items based on their distance
                // from the center, creating a wheel-like effect.
                val itemCenterY = placeableLayoutPositionY + placeable.height / 2
                val distanceFromCenter = abs(itemCenterY - viewportCenterY)
                val normalizedDistance =
                    (distanceFromCenter / (viewportHeight / 2f)).fastCoerceIn(0f, 1f)

                // Items scale between 0.4 at the edges of the viewport and 1 at the center.
                val scaleFactor = 1f - (normalizedDistance * 0.6f)

                // Place the item horizontally centered with a layer transformation for
                // scaling to achieve wheel-like effect.
                placeable.placeRelativeWithLayer(
                    x = constraints.maxWidth / 2 - placeable.width / 2,
                    // Offset y by the scroll position to make placeable visible in the viewport.
                    y = placeableLayoutPositionY - scrollY,
                ) {
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                }
                // Move to the next item's vertical position.
                placeableLayoutPositionY += placeable.height
            }
        }
    }
}

/*
 * A custom implementation of ScrollableState that manages a scroll position and its maximum allowed
 * value.
 *
 * This is a simplified version of the `ScrollState` used by `Modifier.verticalScroll` and
 * `Modifier.horizontalScroll`, demonstrating how to implement a custom state for custom scrollable
 * containers.
 */
private class ScrollableAreaSampleScrollState(initial: Int) : ScrollableState {

    // The current integer scroll position in pixels.
    // This is backed by a mutableStateOf to trigger recomposition when it changes.
    @get:FrequentlyChangingValue
    var value by mutableIntStateOf(initial)
        private set

    // The maximum scroll position allowed. This is typically derived from the content size minus
    // viewport size.
    var maxValue: Int
        get() = _maxValueState.intValue
        set(newMax) {
            _maxValueState.intValue = newMax
            Snapshot.withoutReadObservation {
                if (value > newMax) {
                    value = newMax
                }
            }
        }

    private var _maxValueState = mutableIntStateOf(Int.MAX_VALUE)

    // Accumulates sub-pixel scroll deltas. This ensures that even small, fractional scroll
    // movements are accounted for and contribute to the total scroll position over time, preventing
    // loss of precision.
    private var accumulator: Float = 0f

    // The underlying ScrollableState that handles the actual scroll consumption logic. This lambda
    // is invoked when a scroll delta is received.
    private val scrollableState = ScrollableState {
        val absolute = (value + it + accumulator)
        val newValue = absolute.coerceIn(0f, maxValue.toFloat())
        val changed = absolute != newValue
        val consumed = newValue - value
        val consumedInt = consumed.fastRoundToInt()
        value += consumedInt
        accumulator = consumed - consumedInt

        if (changed) consumed else it
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ): Unit = scrollableState.scroll(scrollPriority, block)

    override fun dispatchRawDelta(delta: Float): Float = scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override val canScrollForward: Boolean by derivedStateOf { value < maxValue }

    override val canScrollBackward: Boolean by derivedStateOf { value > 0 }

    override val lastScrolledForward: Boolean
        get() = scrollableState.lastScrolledForward

    override val lastScrolledBackward: Boolean
        get() = scrollableState.lastScrolledBackward

    companion object {
        // Saver for CustomSampleScrollState, allowing it to be saved and restored across
        // process death or configuration changes. Only the current scroll 'value' is saved.
        val Saver: Saver<ScrollableAreaSampleScrollState, *> =
            Saver(save = { it.value }, restore = { ScrollableAreaSampleScrollState(it) })
    }
}

@Composable
private fun rememberScrollableAreaSampleScrollState(
    initial: Int = 0
): ScrollableAreaSampleScrollState {
    return rememberSaveable(saver = ScrollableAreaSampleScrollState.Saver) {
        ScrollableAreaSampleScrollState(initial = initial)
    }
}
