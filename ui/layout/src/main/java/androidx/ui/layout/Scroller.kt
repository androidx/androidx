/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.layout

import androidx.ui.core.Constraints
import androidx.ui.core.Direction
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.Px
import androidx.ui.core.PxPosition
import androidx.ui.core.coerceIn
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.ipx
import androidx.ui.core.min
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.toPx
import androidx.ui.core.toRect
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.RepaintBoundary

/**
 * Tracks the vertical drag gesture offset, allowing a range between `0.px` and [max].
 * When the offset changes, [offsetChange] is called with the new offset.
 */
@Composable
private fun VerticalDragGestureDetector(
    max: Px = Px.Infinity,
    offsetChange: (Px) -> Unit,
    @Children children: @Composable() () -> Unit
) {
    val offset = +state { 0.px }
    DragGestureDetector(
        dragObserver = object : DragObserver {
            override fun onDrag(dragDistance: PxPosition): PxPosition {
                val dragPosition = -offset.value + dragDistance.y
                val targetPosition = dragPosition.coerceIn(-max, 0.px)
                if (targetPosition != -offset.value) {
                    offset.value = -targetPosition
                    offsetChange(offset.value)
                }
                val consumed = dragDistance.y - (targetPosition - dragPosition)
                return PxPosition(0.px, consumed)
            }
        },
        canDrag = { direction ->
            when (direction) {
                Direction.DOWN -> offset.value > 0.px
                Direction.UP -> offset.value < max
                else -> false
            }
        }) { children() }
}

/**
 * This is the state of a [VerticalScroller] that allows the developer to change the scroll
 * position. [position] must be between `0` and `maxPosition` in `onScrollChanged`'s `maxPosition`
 * parameter.
 */
@Model
class ScrollerPosition {
    /**
     * The amount of scrolling, between `0` and `maxPosition` in `onScrollChanged`'s `maxPosition`
     * parameter.
     */
    var position: Px = 0.px
}

/**
 * A container that composes all of its contents and lays it out, fitting the width of the child.
 * If the child's height is less than the [Constraints.maxHeight], the child's height is used,
 * or the [Constraints.maxHeight] otherwise. If the contents don't fit the height, the drag gesture
 * allows scrolling its content vertically. The contents of the VerticalScroller are clipped to
 * the VerticalScroller's bounds.
 */
// TODO(mount): Add fling support
@Composable
fun VerticalScroller(
    scrollerPosition: ScrollerPosition = +memo { ScrollerPosition() },
    onScrollChanged: (position: Px, maxPosition: Px) -> Unit = { position, _ ->
        scrollerPosition.position = position
    },
    @Children child: @Composable() () -> Unit
) {
    val maxPosition = +state { 0.px }
    VerticalDragGestureDetector(
        max = maxPosition.value,
        offsetChange = { newOffset -> onScrollChanged(newOffset, maxPosition.value) }) {
        Layout(children = {
            Draw { canvas, parentSize ->
                // TODO (njawad) replace with save lambda when multi children DrawNodes are supported
                canvas.nativeCanvas.save()
                canvas.clipRect(parentSize.toRect())
            }
            RepaintBoundary(name = "VerticalScroller") {
                child()
            }
            Draw { canvas, _ ->
                // TODO (njawad) replace with save lambda when multi children DrawNodes are supported
                canvas.nativeCanvas.restore()
            }
        }, layoutBlock = { measurables, constraints ->
            if (measurables.size > 1) {
                throw IllegalStateException("Only one child is allowed in a VerticalScroller")
            }
            val childConstraints = constraints.copy(maxHeight = IntPx.Infinity)
            val childMeasurable = measurables.firstOrNull()
            val placeable = childMeasurable?.measure(childConstraints)
            val width: IntPx
            val height: IntPx
            if (placeable == null) {
                width = constraints.minWidth
                height = constraints.minHeight
            } else {
                width = placeable.width
                height = min(placeable.height, constraints.maxHeight)
            }
            layout(width, height) {
                val childHeight = placeable?.height?.toPx() ?: 0.px
                val newMaxPosition = childHeight - height.toPx()
                if (maxPosition.value != newMaxPosition) {
                    maxPosition.value = newMaxPosition
                    onScrollChanged(scrollerPosition.position, maxPosition.value)
                }
                placeable?.place(0.ipx, -scrollerPosition.position.round())
            }
        })
    }
}
