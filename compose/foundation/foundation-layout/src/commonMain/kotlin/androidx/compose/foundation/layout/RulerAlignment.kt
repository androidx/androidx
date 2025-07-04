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
package androidx.compose.foundation.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.RectRulers
import androidx.compose.ui.layout.Ruler
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.roundToInt

/**
 * Fits the contents within [rulers]. This only works when [Constraints] have
 * [fixed width][Constraints.hasFixedWidth] and [fixed height][Constraints.hasFixedHeight]. This can
 * be accomplished, for example, by having [Modifier.size], or [Modifier.fillMaxSize], or other size
 * modifier before [fitInside]. If the [Constraints] sizes aren't fixed, [fitInside] will size the
 * child to the [Constraints] and try to center the content within [rulers].
 *
 * @sample androidx.compose.foundation.layout.samples.FitInsideOutsideExample
 * @see fitOutside
 */
fun Modifier.fitInside(rulers: RectRulers): Modifier = layout { measurable, constraints ->
    if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            val left = rulers.left.current(0f).roundToInt().fastCoerceIn(0, width)
            val top = rulers.top.current(0f).roundToInt().fastCoerceIn(0, height)
            val right = rulers.right.current(width.toFloat()).roundToInt().fastCoerceIn(0, width)
            val bottom =
                rulers.bottom.current(height.toFloat()).roundToInt().fastCoerceIn(0, height)

            val childConstraints = Constraints.fixed(right - left, bottom - top)
            val placeable = measurable.measure(childConstraints)
            placeable.place(left, top)
        }
    } else {
        // Can't use the rulers because we don't know the size
        val placeable = measurable.measure(constraints)
        val width = placeable.width
        val height = placeable.height
        layout(width, height) {
            val left = rulers.left.current(0f).roundToInt().fastCoerceIn(0, width)
            val top = rulers.top.current(0f).roundToInt().fastCoerceIn(0, height)
            val right = rulers.right.current(width.toFloat()).roundToInt().fastCoerceIn(0, width)
            val bottom =
                rulers.bottom.current(height.toFloat()).roundToInt().fastCoerceIn(0, height)
            // center in the available space
            placeable.place((left + right - width) / 2, (top + bottom - height) / 2)
        }
    }
}

/**
 * If one of the [Ruler]s in [rulers] has a value within the bounds of the Layout, this sizes the
 * content to that [Ruler] and the edge. If multiple [Ruler]s have a value within the space, only
 * one is chosen, in this order: [RectRulers.left], [RectRulers.top], [RectRulers.right],
 * [RectRulers.bottom]. This only works when [Constraints] have
 * [fixed width][Constraints.hasFixedWidth] and [fixed height][Constraints.hasFixedHeight]. This can
 * be accomplished, for example, by having [Modifier.size], or [Modifier.fillMaxSize], or other size
 * modifier before [fitOutside]. If the [Constraints] sizes aren't fixed, or there are no [Ruler]s
 * within the bounds of the layout, [fitOutside] will size the content area to 0x0.
 *
 * @sample androidx.compose.foundation.layout.samples.FitInsideOutsideExample
 * @see fitInside
 */
fun Modifier.fitOutside(rulers: RectRulers): Modifier = layout { measurable, constraints ->
    if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            val left = rulers.left.current(0f).roundToInt()
            val top = rulers.top.current(0f).roundToInt()
            val right = rulers.right.current(width.toFloat()).roundToInt()
            val bottom = rulers.bottom.current(height.toFloat()).roundToInt()

            var childWidth = width
            var childHeight = height
            var placeLeft = 0
            var placeTop = 0
            if (left > 0) {
                // layout to the left edge
                childWidth = left
            } else if (top > 0) {
                childHeight = top
            } else if (right < width) {
                placeLeft = right
                childWidth = width - right
            } else if (bottom < height) {
                placeTop = bottom
                childHeight = height - bottom
            } else {
                childWidth = 0
                childHeight = 0
            }
            val childConstraints = Constraints.fixed(childWidth, childHeight)
            val placeable = measurable.measure(childConstraints)
            placeable.place(placeLeft, placeTop)
        }
    } else {
        // Can't use the rulers because we don't know the size
        layout(0, 0) { measurable.measure(Constraints.fixed(0, 0)).place(0, 0) }
    }
}
