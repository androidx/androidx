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

import androidx.animation.AnimationClockObservable
import androidx.animation.AnimationEndReason
import androidx.compose.Composable
import androidx.compose.Stable
import androidx.compose.StructurallyEqual
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.compose.setValue
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.clipToBounds
import androidx.ui.foundation.TextFieldValue.Companion.Saver
import androidx.ui.foundation.animation.FlingConfig
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.ScrollableState
import androidx.ui.foundation.gestures.scrollable
import androidx.ui.layout.Column
import androidx.ui.layout.ColumnScope
import androidx.ui.layout.Constraints
import androidx.ui.layout.Row
import androidx.ui.layout.RowScope
import androidx.ui.savedinstancestate.Saver
import androidx.ui.savedinstancestate.rememberSavedInstanceState
import androidx.ui.semantics.ScrollTo
import androidx.ui.semantics.Semantics
import androidx.ui.unit.IntPx
import androidx.ui.unit.ipx
import androidx.ui.unit.min
import androidx.ui.unit.toPx
import kotlin.math.roundToInt

/**
 * Create and [remember] the state for a [VerticalScroller] or [HorizontalScroller] based on the
 * currently appropriate scroll configuration to allow changing scroll position or observing
 * scroll behavior.
 *
 * @param initial initial scroller position to start with
 * @param isReversed whether position will be reversed, e.g. 0 will mean bottom for
 * [VerticalScroller] and end for [HorizontalScroller]
 */
@Composable
fun ScrollerPosition(
    initial: Float = 0f,
    isReversed: Boolean = false
): ScrollerPosition {
    val clock = AnimationClockAmbient.current
    val config = FlingConfig()
    return rememberSavedInstanceState(
        clock, config,
        saver = ScrollerPosition.Saver(config, isReversed, clock)
    ) {
        ScrollerPosition(
            flingConfig = config,
            initial = initial,
            animationClock = clock,
            isReversed = isReversed
        )
    }
}

/**
 * This is the state of a [VerticalScroller] and [HorizontalScroller] that
 * allows the developer to change the scroll position by calling methods on this object.
 *
 * @param flingConfig configuration that specifies fling logic when scrolling ends with velocity
 * @param initial initial scroller position in pixels to start with
 * @param isReversed whether position will be reversed, e.g. 0 will mean bottom for
 * [VerticalScroller] and end for [HorizontalScroller]
 * @param animationClock clock observable to run animation on. Consider querying
 * [AnimationClockAmbient] to get current composition value
 */
@Stable
class ScrollerPosition(
    /** Configuration that specifies fling logic when scrolling ends with velocity. */
    flingConfig: FlingConfig,
    initial: Float = 0f,
    internal val isReversed: Boolean = false,
    animationClock: AnimationClockObservable
) {

    private fun directionalValue(value: Float) = if (isReversed) value else -value

    private val consumeDelta: (Float) -> Float = {
        val reverseDelta = directionalValue(it)
        val newValue = value + reverseDelta
        val max = maxPosition
        val min = 0f
        val consumed = when {
            newValue > max -> max - value
            newValue < min -> min - value
            else -> reverseDelta
        }
        value += consumed
        directionalValue(consumed)
    }

    internal val scrollableState =
        ScrollableState(consumeDelta, flingConfig, animationClock)

    /**
     * current scroller position value in pixels
     */
    var value by mutableStateOf(initial, StructurallyEqual)
        private set

    /**
     * whether this [ScrollerPosition] is currently animating/flinging
     */
    val isAnimating
        get() = scrollableState.isAnimating

    /**
     * maxPosition this scroller that consume this ScrollerPosition can reach in pixels, or
     * [Float.POSITIVE_INFINITY] if still unknown
     */
    var maxPosition by mutableStateOf(Float.POSITIVE_INFINITY, StructurallyEqual)
        private set

    /**
     * Smooth scroll to position in pixels
     *
     * @param value target value in pixels to smooth scroll to, value will be coerced to
     * 0..maxPosition
     */
    // TODO (malkov/tianliu) : think about allowing to scroll with custom animation timings/curves
    fun smoothScrollTo(
        value: Float,
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        smoothScrollBy(value - this.value, onEnd)
    }

    /**
     * Smooth scroll by some amount of pixels
     *
     * @param value delta in pixels to scroll by, total value will be coerced to 0..maxPosition
     */
    fun smoothScrollBy(
        value: Float,
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        scrollableState.smoothScrollBy(directionalValue(value), onEnd)
    }

    /**
     * Instantly jump to position in pixels
     *
     * @param value target value in pixels to jump to, value will be coerced to 0..maxPosition
     */
    fun scrollTo(value: Float) {
        this.value = value.coerceIn(0f, maxPosition)
    }

    /**
     * Instantly jump by some amount of pixels
     *
     * @param value delta in pixels to jump by, total value will be coerced to 0..maxPosition
     */
    fun scrollBy(value: Float) {
        scrollTo(this.value + value)
    }

    internal fun updateMaxPosition(newMax: Float) {
        maxPosition = newMax
        if (value > newMax) {
            value = newMax
        }
    }

    companion object {
        /**
         * The default [Saver] implementation for [ScrollerPosition].
         */
        @Composable
        fun Saver(
            flingConfig: FlingConfig,
            isReversed: Boolean,
            animationClock: AnimationClockObservable
        ): Saver<ScrollerPosition, *> = Saver<ScrollerPosition, Float>(
            save = { it.value },
            restore = { ScrollerPosition(flingConfig, it, isReversed, animationClock) }
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
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    children: @Composable ColumnScope.() -> Unit
) {
    Scroller(scrollerPosition, modifier, true, isScrollable) {
        Column(
            modifier = Modifier.clipToBounds(),
            children = children
        )
    }
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
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    children: @Composable RowScope.() -> Unit
) {
    Scroller(scrollerPosition, modifier, false, isScrollable) {
        Row(
            modifier = Modifier.clipToBounds(),
            children = children
        )
    }
}

@Composable
private fun Scroller(
    scrollerPosition: ScrollerPosition,
    modifier: Modifier,
    isVertical: Boolean,
    isScrollable: Boolean,
    child: @Composable () -> Unit
) {
    val direction =
        if (isVertical) DragDirection.Vertical else DragDirection.Horizontal
    Semantics(container = true, properties = {
        if (isScrollable) {
            // when b/156389287 is fixed, this should be proper scrollTo with reverse handling
            ScrollTo(action = { x, y ->
                if (isVertical) {
                    scrollerPosition.scrollBy(y)
                } else {
                    scrollerPosition.scrollBy(x)
                }
                return@ScrollTo true
            })
        }
    }) {
        ScrollerLayout(
            scrollerPosition = scrollerPosition,
            modifier = modifier.scrollable(
                scrollableState = scrollerPosition.scrollableState,
                dragDirection = direction,
                enabled = isScrollable
            ),
            isVertical = isVertical,
            child = child
        )
    }
}

@Composable
private fun ScrollerLayout(
    scrollerPosition: ScrollerPosition,
    modifier: Modifier,
    isVertical: Boolean,
    child: @Composable () -> Unit
) {
    Layout(
        modifier = modifier.clipToBounds(),
        children = child,
        measureBlock = { measurables, constraints, _ ->
            val childConstraints = constraints.copy(
                maxHeight = if (isVertical) IntPx.Infinity else constraints.maxHeight,
                maxWidth = if (isVertical) constraints.maxWidth else IntPx.Infinity
            )
            require(measurables.size == 1)
            val placeable = measurables.first().measure(childConstraints)
            val width = min(placeable.width, constraints.maxWidth)
            val height = min(placeable.height, constraints.maxHeight)
            val scrollHeight = placeable.height.toPx() - height.toPx()
            val scrollWidth = placeable.width.toPx() - width.toPx()
            val side = if (isVertical) scrollHeight else scrollWidth
            layout(width, height) {
                scrollerPosition.updateMaxPosition(side.value)
                val scroll = scrollerPosition.value.coerceIn(0f, side.value)
                val absScroll =
                    if (scrollerPosition.isReversed) scroll - side.value else -scroll
                val xOffset = if (isVertical) 0 else absScroll.roundToInt()
                val yOffset = if (isVertical) absScroll.roundToInt() else 0
                placeable.place(xOffset.ipx, yOffset.ipx)
            }
        }
    )
}
