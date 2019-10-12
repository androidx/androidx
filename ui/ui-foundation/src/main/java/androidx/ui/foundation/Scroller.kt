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

import androidx.animation.AnimatedFloat
import androidx.animation.AnimationEndReason
import androidx.animation.ExponentialDecay
import androidx.animation.ValueHolder
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.Model
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Clip
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.Px
import androidx.ui.core.RepaintBoundary
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.core.ipx
import androidx.ui.core.min
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.toPx
import androidx.ui.foundation.animation.AnimatedFloatDragController
import androidx.ui.foundation.animation.FlingConfig
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.Draggable
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.layout.Constraints
import androidx.ui.layout.Container
import androidx.ui.lerp

/**
 * This is the state of a [VerticalScroller] and [HorizontalScroller] that
 * allows the developer to change the scroll position.
 * [value] must be between `0` and `maxPosition` in `onScrollPositionChanged`'s `maxPosition`
 * parameter.
 */
@Model
class ScrollerPosition {

    /**
     * The amount of scrolling, between `0` and `maxPosition` in `onScrollPositionChanged`'s
     * `maxPosition` parameter.
     */
    var value: Px = 0.px

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
        controller.animatedFloat.animateTo(-value.value, onEnd)
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
        controller.onDrag(-value.value)
    }

    /**
     * Instantly jump by some amount of pixels
     *
     * @param value delta to jump by
     */
    fun scrollBy(value: Px) {
        scrollTo(this.value + value)
    }

    // TODO (malkov/tianliu): Open this for customization
    private val flingConfig = FlingConfig(
        decayAnimation = ExponentialDecay(
            frictionMultiplier = ScrollerDefaultFriction,
            absVelocityThreshold = ScrollerVelocityThreshold
        )
    )

    internal val controller =
        ScrollerDragValueController({ lh.lambda.invoke(-it) }, flingConfig)

    // This is needed to take instant value we're currently dragging
    // and avoid reading @Model var field
    internal val instantValue
        get() = -controller.currentValue

    // This is needed to avoid var (read of which will cause unnecessary recompose in Scroller)
    internal val lh = LambdaHolder { value = it.px }
}

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
 * @param onScrollPositionChanged callback to be invoked when scroll position is about to be
 * changed or max bound of scrolling has changed
 * @param isScrollable param to enabled or disable touch input scrolling, default is true
 */
@Composable
fun VerticalScroller(
    scrollerPosition: ScrollerPosition = +memo { ScrollerPosition() },
    onScrollPositionChanged: (position: Px, maxPosition: Px) -> Unit = { position, _ ->
        scrollerPosition.value = position
    },
    isScrollable: Boolean = true,
    child: @Composable() () -> Unit
) {
    Scroller(scrollerPosition, onScrollPositionChanged, true, isScrollable, child)
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
 * @param onScrollPositionChanged callback to be invoked when scroll position is about to be
 * changed or max bound of scrolling has changed
 * @param isScrollable param to enabled or disable touch input scrolling, default is true
 */
@Composable
fun HorizontalScroller(
    scrollerPosition: ScrollerPosition = +memo { ScrollerPosition() },
    onScrollPositionChanged: (position: Px, maxPosition: Px) -> Unit = { position, _ ->
        scrollerPosition.value = position
    },
    isScrollable: Boolean = true,
    child: @Composable() () -> Unit
) {
    Scroller(scrollerPosition, onScrollPositionChanged, false, isScrollable, child)
}

@Composable
private fun Scroller(
    scrollerPosition: ScrollerPosition,
    onScrollPositionChanged: (position: Px, maxPosition: Px) -> Unit,
    isVertical: Boolean,
    isScrollable: Boolean,
    child: @Composable() () -> Unit
) {
    val maxPosition = +state { Px.Infinity }
    val direction = if (isVertical) DragDirection.Vertical else DragDirection.Horizontal
    scrollerPosition.controller.enabled = isScrollable
    scrollerPosition.lh.lambda = { onScrollPositionChanged(it.px, maxPosition.value) }
    PressGestureDetector(onPress = { scrollerPosition.scrollTo(scrollerPosition.value) }) {
        Draggable(
            dragDirection = direction,
            minValue = -maxPosition.value.value,
            maxValue = 0f,
            valueController = scrollerPosition.controller
        ) {
            ScrollerLayout(
                scrollerPosition = scrollerPosition,
                maxPosition = maxPosition.value,
                onMaxPositionChanged = {
                    maxPosition.value = it
                    onScrollPositionChanged(scrollerPosition.value, it)
                },
                isVertical = isVertical,
                child = child
            )
        }
    }
}

@Composable
private fun ScrollerLayout(
    scrollerPosition: ScrollerPosition,
    maxPosition: Px,
    onMaxPositionChanged: (Px) -> Unit,
    isVertical: Boolean,
    child: @Composable() () -> Unit
) {
    Layout(children = {
        Clip(RectangleShape) {
            Container {
                RepaintBoundary {
                    child()
                }
            }
        }
    }) { measurables, constraints ->
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
            if (side != maxPosition) {
                onMaxPositionChanged(side)
            }
            val xOffset = if (isVertical) 0.ipx else -scrollerPosition.value.round()
            val yOffset = if (isVertical) -scrollerPosition.value.round() else 0.ipx
            placeable?.place(xOffset, yOffset)
        }
    }
}

private fun ScrollerDragValueController(
    onValueChanged: (Float) -> Unit,
    flingConfig: FlingConfig? = null
) = AnimatedFloatDragController(
    AnimatedFloat(ScrollPositionValueHolder(0f, onValueChanged)),
    flingConfig
)

private class ScrollPositionValueHolder(
    var current: Float,
    val onValueChanged: (Float) -> Unit
) : ValueHolder<Float> {
    override val interpolator: (start: Float, end: Float, fraction: Float) -> Float = ::lerp
    override var value: Float
        get() = current
        set(value) {
            current = value
            onValueChanged(value)
        }
}

internal data class LambdaHolder(var lambda: (Float) -> Unit)

private val ScrollerDefaultFriction = 0.35f
private val ScrollerVelocityThreshold = 1000f