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

import androidx.ui.core.coerceIn
import androidx.ui.core.Constraints
import androidx.ui.core.Direction
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.Px
import androidx.ui.core.PxPosition
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.min
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.toPx
import androidx.ui.core.toRect
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Clip
import androidx.ui.core.Density
import androidx.ui.core.PxSize
import androidx.ui.core.RepaintBoundary
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.Shape

/**
 * Tracks the vertical drag gesture offset, allowing a range between `0.px` and [max].
 * When the offset changes, [offsetChange] is called with the new offset.
 */
@Composable
private fun DirectionalDragGestureDetector(
    vertical: Boolean,
    max: Px = Px.Infinity,
    offsetChange: (Px) -> Unit,
    children: @Composable() () -> Unit
) {
    val offset = +state { 0.px }
    DragGestureDetector(
        dragObserver = object : DragObserver {
            override fun onDrag(dragDistance: PxPosition): PxPosition {
                val draggedAmount = if (vertical) dragDistance.y else dragDistance.x
                val dragPosition = -offset.value + draggedAmount
                val targetPosition = dragPosition.coerceIn(-max, 0.px)
                if (targetPosition != -offset.value) {
                    offset.value = -targetPosition
                    offsetChange(offset.value)
                }
                val consumed = draggedAmount + (targetPosition - dragPosition)
                return if (vertical) PxPosition(0.px, consumed) else PxPosition(consumed, 0.px)
            }
        },
        canDrag = { direction ->
            when (direction) {
                Direction.DOWN -> if (vertical) offset.value > 0.px else false
                Direction.UP -> if (vertical) offset.value < max else false
                Direction.RIGHT -> if (vertical) false else offset.value > 0.px
                Direction.LEFT -> if (vertical) false else offset.value < max
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
    child: @Composable() () -> Unit
) {
    Scroller(
        vertical = true,
        scrollerPosition = scrollerPosition,
        onScrollChanged = onScrollChanged,
        child = child
    )
}

/**
 * A container that composes all of its contents and lays it out, fitting the height of the child.
 * If the child's width is less than the [Constraints.maxWidth], the child's width is used,
 * or the [Constraints.maxWidth] otherwise. If the contents don't fit the width, the drag gesture
 * allows scrolling its content horizontally. The contents of the HorizontalScroller are clipped to
 * the HorizontalScroller's bounds.
 */
// TODO(mount): Add fling support
@Composable
fun HorizontalScroller(
    scrollerPosition: ScrollerPosition = +memo { ScrollerPosition() },
    onScrollChanged: (position: Px, maxPosition: Px) -> Unit = { position, _ ->
        scrollerPosition.position = position
    },
    child: @Composable() () -> Unit
) {
    Scroller(
        vertical = false,
        scrollerPosition = scrollerPosition,
        onScrollChanged = onScrollChanged,
        child = child
    )
}

@Composable
private fun Scroller(
    vertical: Boolean,
    scrollerPosition: ScrollerPosition = +memo { ScrollerPosition() },
    onScrollChanged: (position: Px, maxPosition: Px) -> Unit = { position, _ ->
        scrollerPosition.position = position
    },
    child: @Composable() () -> Unit
) {
    val maxPosition = +state { 0.px }
    Layout(children = {
        Clip(RectangleShape) {
            DirectionalDragGestureDetector(
                vertical = vertical,
                max = maxPosition.value,
                offsetChange = { newOffset -> onScrollChanged(newOffset, maxPosition.value) }) {
                Container {
                    RepaintBoundary {
                        child()
                    }
                }
            }
        }
    }) { measurables, constraints ->
        val childConstraints = if (vertical) {
            constraints.copy(maxHeight = IntPx.Infinity)
        } else {
            constraints.copy(maxWidth = IntPx.Infinity)
        }
        val childMeasurable = measurables.firstOrNull()
        val placeable = childMeasurable?.measure(childConstraints)
        val width: IntPx
        val height: IntPx
        if (placeable == null) {
            width = constraints.minWidth
            height = constraints.minHeight
        } else {
            width = min(placeable.width, constraints.maxWidth)
            height = min(placeable.height, constraints.maxHeight)
        }
        layout(width, height) {
            if (placeable != null) {
                val childSize = if (vertical) {
                    placeable.height.toPx()
                } else {
                    placeable.width.toPx()
                }
                val newMaxPosition = childSize - if (vertical) height.toPx() else width.toPx()
                if (maxPosition.value != newMaxPosition) {
                    maxPosition.value = newMaxPosition
                    onScrollChanged(scrollerPosition.position, maxPosition.value)
                }
                val x: IntPx
                val y: IntPx
                if (vertical) {
                    x = IntPx.Zero
                    y = -scrollerPosition.position.round()
                } else {
                    x = -scrollerPosition.position.round()
                    y = IntPx.Zero
                }
                placeable.place(x, y)
            }
        }
    }
}

// TODO(andreykulikov): make RectangleShape from ui-foundation accessible to this class
private val RectangleShape: Shape = object : Shape {
    override fun createOutline(size: PxSize, density: Density) =
        Outline.Rectangle(size.toRect())
}
