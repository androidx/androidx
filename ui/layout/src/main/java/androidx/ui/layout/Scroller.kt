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
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.Model
import com.google.r4a.composer
import com.google.r4a.memo
import com.google.r4a.state
import com.google.r4a.unaryPlus

/**
 * Tracks the vertical drag gesture offset, allowing a range between `0.px` and [max].
 * When the offset changes, [offsetChange] is called with the new offset.
 */
private class VerticalDragGestureDetector(
    var max: Px = Px.Infinity,
    var offsetChange: (Px) -> Unit,
    @Children var children: @Composable() () -> Unit
) : Component() {
    private var offset: Px = 0.px

    override fun compose() {
        <DragGestureDetector
            dragObserver=object : DragObserver {
                override fun onDrag(dragDistance: PxPosition): PxPosition {
                    val dragPosition = -offset + dragDistance.y
                    val targetPosition = dragPosition.coerceIn(-max, 0.px)
                    if (targetPosition != -offset) {
                        offset = -targetPosition
                        offsetChange(offset)
                    }
                    val consumed = dragDistance.y - (targetPosition - dragPosition)
                    return PxPosition(0.px, consumed)
                }
            }
            canDrag={ direction ->
                when (direction) {
                    Direction.DOWN -> offset > 0.px
                    Direction.UP -> offset < max
                    else -> false
                }
            }
        >
            <children />
        </DragGestureDetector>
    }
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
@Suppress("FunctionName")
@Composable
fun VerticalScroller(
    scrollerPosition: ScrollerPosition = +memo { ScrollerPosition() },
    onScrollChanged: (position: Px, maxPosition: Px) -> Unit = { position, _ ->
        scrollerPosition.position = position
    },
    @Children child: () -> Unit
) {
    val maxPosition = +state { 0.px }
    <VerticalDragGestureDetector
        max=maxPosition.value
        offsetChange={ newOffset -> onScrollChanged(newOffset, maxPosition.value) }>
        <Layout layoutBlock={ measurables, constraints ->
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
            val childHeight = placeable?.height?.toPx() ?: 0.px
            val newMaxPosition = childHeight - height.toPx()
            if (maxPosition.value != newMaxPosition) {
                maxPosition.value = newMaxPosition
                onScrollChanged(scrollerPosition.position, maxPosition.value)
            }
            layout(width, height) {
                placeable?.place(0.ipx, -scrollerPosition.position.round())
            }
        }>
            <Draw> canvas, parentSize ->
                canvas.save()
                canvas.clipRect(parentSize.toRect())
            </Draw>
            <child />
            <Draw> canvas, _ ->
                canvas.restore()
            </Draw>
        </Layout>
    </VerticalDragGestureDetector>
}
