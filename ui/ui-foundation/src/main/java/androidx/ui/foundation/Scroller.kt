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

package androidx.ui.foundation

import androidx.animation.AnimationEndReason
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.remember
import androidx.ui.core.Alignment
import androidx.ui.core.Clip
import androidx.ui.core.Constraints
import androidx.ui.unit.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.unit.Px
import androidx.ui.core.RepaintBoundary
import androidx.ui.unit.ipx
import androidx.ui.unit.min
import androidx.ui.unit.px
import androidx.ui.unit.round
import androidx.ui.unit.toPx
import androidx.ui.foundation.animation.AnimatedValueHolder
import androidx.ui.foundation.animation.FlingConfig
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.Draggable
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.layout.Constraints
import androidx.ui.layout.Container
import androidx.ui.semantics.ScrollTo
import androidx.ui.semantics.Semantics

/**
 * Create and [remember] the state for a [VerticalScroller] or [HorizontalScroller] based on the
 * currently appropriate scroll configuration to allow changing scroll position or observing
 * scroll behavior.
 */
@Composable
fun ScrollerPosition(
    initial: Float = 0f
): ScrollerPosition =
    FlingConfig().let { remember(it) { ScrollerPosition(flingConfig = it, initial = initial) } }

/**
 * This is the state of a [VerticalScroller] and [HorizontalScroller] that
 * allows the developer to change the scroll position by calling methods on this object.
 */
@Model
class ScrollerPosition(
    /** Configuration that specifies fling logic when scrolling ends with velocity. */
    val flingConfig: FlingConfig,
    initial: Float = 0f
) {

    internal val holder = AnimatedValueHolder(-initial)

    /**
     * maxPosition this scroller that consume this ScrollerPosition can reach, or [Px.Infinity]
     * if still unknown
     */
    var maxPosition: Px = Px.Infinity
        internal set

    /**
     * current position for scroller
     */
    val value: Px
        get() = -holder.value.px

    // TODO(b/145693559) This will likely be rendered obsolete when AnimatedFloat exposes a +state
    //  "isAnimating" or "isRunning" property.
    /**
     * whether this [ScrollerPosition] is currently animating
     */
    var isAnimating: Boolean = false
        private set

    /**
     * Smooth scroll to position in pixels
     *
     * @param value target value to smooth scroll to
     */
    // TODO (malkov/tianliu) : think about allowing to scroll with custom animation timings/curves
    fun smoothScrollTo(
        value: Px,
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        isAnimating = true
        holder.animatedFloat.animateTo(-value.value) { endReason, finishValue ->
            isAnimating = false
            onEnd(endReason, finishValue)
        }
    }

    /**
     * Smooth scroll by some amount of pixels
     *
     * @param value delta to scroll by
     */
    fun smoothScrollBy(
        value: Px,
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        smoothScrollTo(this.value + value, onEnd)
    }

    /**
     * Instantly jump to position in pixels
     *
     * @param value target value to jump to
     */
    fun scrollTo(value: Px) {
        holder.animatedFloat.snapTo(-value.value)
    }

    /**
     * Instantly jump by some amount of pixels
     *
     * @param value delta to jump by
     */
    fun scrollBy(value: Px) {
        scrollTo(this.value + value)
    }

    /**
     * Starts a fling animation with the specified starting velocity and the previously set
     * [flingConfig]
     *
     * @param startVelocity Starting velocity of the fling animation
     */
    fun fling(startVelocity: Float) {

        // TODO(b/146054789): It would be more efficient to create and cache this object whenever
        //  the `flingConfig` property is set, but doing so currently causes a
        //  `java.lang.IllegalStateException: Not in a frame` exception, which is a bug
        //  and is tracked by b/146054789.
        val flingConfig = FlingConfig(
            flingConfig.decayAnimation,
            { endReason, endValue, remainingVelocity ->
                isAnimating = false
                flingConfig.onAnimationEnd?.invoke(endReason, endValue, remainingVelocity)
            },
            flingConfig.adjustTarget
        )

        isAnimating = true
        holder.fling(
            flingConfig,
            startVelocity
        )
    }
}

// TODO(malkov): Test behavior during animation more extensively (including pressing on the scroller
//  during an animation when b/144878730 is fixed.
/**
 * A container that composes all of its contents and lays it out, fitting the width of the child.
 * If the child's height is less than the [Constraints.maxHeight], the child's height is used,
 * or the [Constraints.maxHeight] otherwise. If the contents don't fit the height, the drag gesture
 * allows scrolling its content vertically. The contents of the VerticalScroller are clipped to
 * the VerticalScroller's bounds.
 *
 * @sample androidx.ui.foundation.samples.VerticalScrollerSample
 *
 * @param scrollerPosition state of this Scroller that holds current scroll position and provides
 * user with useful methods like smooth scrolling
 * @param modifier Modifier to be applied to the Scroller content layout
 * @param isScrollable param to enabled or disable touch input scrolling, default is true
 */
@Composable
fun VerticalScroller(
    scrollerPosition: ScrollerPosition = ScrollerPosition(),
    modifier: Modifier = Modifier.None,
    isScrollable: Boolean = true,
    child: @Composable() () -> Unit
) {
    Scroller(scrollerPosition, modifier, true, isScrollable, child)
}

/**
 * A container that composes all of its contents and lays it out, fitting the height of the child.
 * If the child's width is less than the [Constraints.maxWidth], the child's width is used,
 * or the [Constraints.maxWidth] otherwise. If the contents don't fit the width, the drag gesture
 * allows scrolling its content horizontally. The contents of the HorizontalScroller are clipped to
 * the HorizontalScroller's bounds.
 *
 * @sample androidx.ui.foundation.samples.SimpleHorizontalScrollerSample
 *
 * If you want to control scrolling position from the code, e.g smooth scroll to position,
 * you must own memorized instance of [ScrollerPosition] and then use it to call `scrollTo...`
 * functions on it. Same tactic can be applied to the [VerticalScroller]
 *
 * @sample androidx.ui.foundation.samples.ControlledHorizontalScrollerSample
 *
 * @param scrollerPosition state of this Scroller that holds current scroll position and provides
 * user with useful methods like smooth scrolling
 * @param modifier Modifier to be applied to the Scroller content layout
 * @param isScrollable param to enabled or disable touch input scrolling, default is true
 */
@Composable
fun HorizontalScroller(
    scrollerPosition: ScrollerPosition = ScrollerPosition(),
    modifier: Modifier = Modifier.None,
    isScrollable: Boolean = true,
    child: @Composable() () -> Unit
) {
    Scroller(scrollerPosition, modifier, false, isScrollable, child)
}

@Composable
private fun Scroller(
    scrollerPosition: ScrollerPosition,
    modifier: Modifier,
    isVertical: Boolean,
    isScrollable: Boolean,
    child: @Composable() () -> Unit
) {
    val direction =
        if (isVertical) DragDirection.Vertical else DragDirection.Horizontal
    Semantics(container = true, properties = {
        if (isScrollable) {
            ScrollTo(action = { x, y ->
                if (isVertical) {
                    scrollerPosition.scrollBy(y)
                } else {
                    scrollerPosition.scrollBy(x)
                }
            })
        }
    }) {
        Draggable(
            dragValue = scrollerPosition.holder,
            onDragStarted = {
                scrollerPosition.scrollTo(scrollerPosition.value)
            },
            onDragValueChangeRequested = {
                scrollerPosition.holder.animatedFloat.snapTo(it)
            },
            onDragStopped = {
                scrollerPosition.fling(it)
            },
            dragDirection = direction,
            isValueAnimating = scrollerPosition.isAnimating,
            enabled = isScrollable
        ) {
            ScrollerLayout(
                scrollerPosition = scrollerPosition,
                onMaxPositionChanged = {
                    scrollerPosition.holder.setBounds(-it.value, 0f)
                    scrollerPosition.maxPosition = it
                },
                modifier = modifier,
                isVertical = isVertical,
                child = child
            )
        }
    }
}

@Composable
private fun ScrollerLayout(
    scrollerPosition: ScrollerPosition,
    modifier: Modifier,
    onMaxPositionChanged: (Px) -> Unit,
    isVertical: Boolean,
    child: @Composable() () -> Unit
) {
    Layout(
        modifier = modifier,
        children = {
            Clip(RectangleShape) {
                Container(alignment = Alignment.TopLeft) {
                    RepaintBoundary(children = child)
                }
            }
        },
        measureBlock = { measurables, constraints ->
            if (measurables.size > 1) {
                throw IllegalStateException("Only one child is allowed in a VerticalScroller")
            }
            val childConstraints = constraints.copy(
                maxHeight = if (isVertical) IntPx.Infinity else constraints.maxHeight,
                maxWidth = if (isVertical) constraints.maxWidth else IntPx.Infinity
            )
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
                val childHeight = placeable?.height?.toPx() ?: 0.px
                val childWidth = placeable?.width?.toPx() ?: 0.px
                val scrollHeight = childHeight - height.toPx()
                val scrollWidth = childWidth - width.toPx()
                val side = if (isVertical) scrollHeight else scrollWidth
                if (side != scrollerPosition.maxPosition) {
                    onMaxPositionChanged(side)
                }
                val xOffset = if (isVertical) 0.ipx else -scrollerPosition.value.round()
                val yOffset = if (isVertical) -scrollerPosition.value.round() else 0.ipx
                placeable?.place(xOffset, yOffset)
            }
        }
    )
}

private val ScrollerDefaultFriction = 0.35f
private val ScrollerVelocityThreshold = 1000f