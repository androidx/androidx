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
@file:Suppress("DEPRECATION")

package androidx.compose.foundation

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.Composable
import androidx.compose.Stable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.structuralEqualityPolicy
import androidx.ui.animation.asDisposableClock
import androidx.ui.core.Alignment
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.clipToBounds
import androidx.ui.core.composed
import androidx.ui.core.gesture.scrollorientationlocking.Orientation
import androidx.ui.core.semantics.semantics
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.defaultFlingConfig
import androidx.compose.foundation.gestures.ScrollableController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Constraints
import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.ui.semantics.scrollBy
import androidx.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Create and [remember] the [ScrollState] based on the currently appropriate scroll
 * configuration to allow changing scroll position or observing scroll behavior.
 *
 * Learn how to control [ScrollableColumn] or [ScrollableRow]:
 * @sample androidx.compose.foundation.samples.ControlledScrollableRowSample
 *
 * @param initial initial scroller position to start with
 */
@Composable
fun rememberScrollState(initial: Float = 0f): ScrollState {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val config = defaultFlingConfig()
    return rememberSavedInstanceState(
        clock, config,
        saver = ScrollState.Saver(config, clock)
    ) {
        ScrollState(
            flingConfig = config,
            initial = initial,
            animationClock = clock
        )
    }
}

/**
 * State of the scroll. Allows the developer to change the scroll position or get current state by
 * calling methods on this object. To be hosted and passed to [ScrollableRow], [ScrollableColumn],
 * [Modifier.verticalScroll] or [Modifier.horizontalScroll]
 *
 * To create and automatically remember [ScrollState] with default parameters use
 * [rememberScrollState].
 *
 * Learn how to control [ScrollableColumn] or [ScrollableRow]:
 * @sample androidx.compose.foundation.samples.ControlledScrollableRowSample
 *
 * @param initial value of the scroll
 * @param flingConfig fling configuration to use for flinging
 * @param animationClock animation clock to run flinging and smooth scrolling on
 */
@Stable
class ScrollState(
    initial: Float,
    internal val flingConfig: FlingConfig,
    animationClock: AnimationClockObservable
) {

    /**
     * current scroll position value in pixels
     */
    var value by mutableStateOf(initial, structuralEqualityPolicy())
        private set

    /**
     * maximum bound for [value], or [Float.POSITIVE_INFINITY] if still unknown
     */
    var maxValue: Float
        get() = _maxValueState.value
        internal set(newMax) {
            _maxValueState.value = newMax
            if (value > newMax) {
                value = newMax
            }
        }

    private var _maxValueState = mutableStateOf(Float.POSITIVE_INFINITY, structuralEqualityPolicy())

    internal val scrollableController =
        ScrollableController(
            flingConfig = flingConfig,
            animationClock = animationClock,
            consumeScrollDelta = {
                val absolute = (value + it)
                val newValue = absolute.coerceIn(0f, maxValue)
                if (absolute != newValue) stopAnimation()
                val consumed = newValue - value
                value += consumed
                consumed
            })

    /**
     * Stop any ongoing animation, smooth scrolling or fling occurring on this [ScrollState]
     */
    fun stopAnimation() {
        scrollableController.stopAnimation()
    }

    /**
     * whether this [ScrollState] is currently animating/flinging
     */
    val isAnimationRunning
        get() = scrollableController.isAnimationRunning

    /**
     * Smooth scroll to position in pixels
     *
     * @param value target value in pixels to smooth scroll to, value will be coerced to
     * 0..maxPosition
     * @param spec animation curve for smooth scroll animation
     * @param onEnd callback to be invoked when smooth scroll has finished
     */
    fun smoothScrollTo(
        value: Float,
        spec: AnimationSpec<Float> = SpringSpec(),
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        smoothScrollBy(value - this.value, spec, onEnd)
    }

    /**
     * Smooth scroll by some amount of pixels
     *
     * @param value delta in pixels to scroll by, total value will be coerced to 0..maxPosition
     * @param spec animation curve for smooth scroll animation
     * @param onEnd callback to be invoked when smooth scroll has finished
     */
    fun smoothScrollBy(
        value: Float,
        spec: AnimationSpec<Float> = SpringSpec(),
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        scrollableController.smoothScrollBy(value, spec, onEnd)
    }

    /**
     * Instantly jump to position in pixels
     *
     * @param value target value in pixels to jump to, value will be coerced to 0..maxPosition
     */
    fun scrollTo(value: Float) {
        this.value = value.coerceIn(0f, maxValue)
    }

    /**
     * Instantly jump by some amount of pixels
     *
     * @param value delta in pixels to jump by, total value will be coerced to 0..maxPosition
     */
    fun scrollBy(value: Float) {
        scrollTo(this.value + value)
    }

    companion object {
        /**
         * The default [Saver] implementation for [ScrollState].
         */
        fun Saver(
            flingConfig: FlingConfig,
            animationClock: AnimationClockObservable
        ): Saver<ScrollState, *> = Saver<ScrollState, Float>(
            save = { it.value },
            restore = { ScrollState(it, flingConfig, animationClock) }
        )
    }
}

/**
 * Variation of [Column] that scrolls when content is bigger than its height.
 *
 * The content of the [ScrollableColumn] is clipped to its bounds.
 *
 * @sample androidx.compose.foundation.samples.ScrollableColumnSample
 *
 * @param modifier modifier for this [ScrollableColumn]
 * @param scrollState state of the scroll, such as current offset and max offset
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalGravity The horizontal gravity of the layout's children
 * @param reverseScrollDirection reverse the direction of scrolling, when `true`, [ScrollState
 * .value] = 0 will mean bottom, when `false`, [ScrollState.value] = 0 will mean top
 * @param isScrollEnabled param to enable or disable touch input scrolling. If you own
 * [ScrollState], you still can call [ScrollState.smoothScrollTo] and other methods on it.
 * @param contentPadding convenience param to specify padding around content. This will add
 * padding for the content after it has been clipped, which is not possible via [modifier] param
 */
@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(0f),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalGravity: Alignment.Horizontal = Alignment.Start,
    reverseScrollDirection: Boolean = false,
    isScrollEnabled: Boolean = true,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    children: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(
                scrollState,
                isScrollEnabled,
                reverseScrolling = reverseScrollDirection
            )
            .clipToBounds()
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalGravity = horizontalGravity,
        children = children
    )
}

/**
 * Variation of [Row] that scrolls when content is bigger than its width.
 *
 * The content of the [ScrollableRow] is clipped to its bounds.
 *
 * @sample androidx.compose.foundation.samples.ScrollableRowSample
 *
 * @param modifier modifier for this [ScrollableRow]
 * @param scrollState state of the scroll, such as current offset and max offset
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param verticalGravity The vertical gravity of the layout's children
 * @param reverseScrollDirection reverse the direction of scrolling, when `true`, [ScrollState
 * .value] = 0 will mean right, when `false`, [ScrollState.value] = 0 will mean left
 * @param isScrollEnabled param to enable or disable touch input scrolling. If you own
 * [ScrollState], you still can call [ScrollState.smoothScrollTo] and other methods on it.
 * @param contentPadding convenience param to specify padding around content. This will add
 * padding for the content after it has been clipped, which is not possible via [modifier] param.
 */
@Composable
fun ScrollableRow(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(0f),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalGravity: Alignment.Vertical = Alignment.Top,
    reverseScrollDirection: Boolean = false,
    isScrollEnabled: Boolean = true,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    children: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .horizontalScroll(
                scrollState,
                isScrollEnabled,
                reverseScrolling = reverseScrollDirection
            )
            .clipToBounds()
            .padding(contentPadding),
        horizontalArrangement = horizontalArrangement,
        verticalGravity = verticalGravity,
        children = children
    )
}

/**
 * Modify element to allow to scroll vertically when height of the content is bigger than max
 * constraints allow.
 *
 * @sample androidx.compose.foundation.samples.VerticalScrollExample
 *
 * In order to use this modifier, you need to create and own [ScrollState]
 * @see [rememberScrollState]
 *
 * @param state state of the scroll
 * @param enabled whether or not scrolling via touch input is enabled
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 * will mean bottom, when `false`, 0 [ScrollState.value] will mean top
 */
fun Modifier.verticalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    reverseScrolling: Boolean = false
) = scroll(
    state = state,
    isScrollable = enabled,
    reverseScrolling = reverseScrolling,
    isVertical = true
)

/**
 * Modify element to allow to scroll horizontally when width of the content is bigger than max
 * constraints allow.
 *
 * @sample androidx.compose.foundation.samples.HorizontalScrollSample
 *
 * In order to use this modifier, you need to create and own [ScrollState]
 * @see [rememberScrollState]
 *
 * @param state state of the scroll
 * @param enabled whether or not scrolling via touch input is enabled
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 * will mean right, when `false`, 0 [ScrollState.value] will mean left
 */
fun Modifier.horizontalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    reverseScrolling: Boolean = false
) = scroll(
    state = state,
    isScrollable = enabled,
    reverseScrolling = reverseScrolling,
    isVertical = false
)

private fun Modifier.scroll(
    state: ScrollState,
    reverseScrolling: Boolean,
    isScrollable: Boolean,
    isVertical: Boolean
) = composed {
    val semantics = Modifier.semantics {
        if (isScrollable) {
            // when b/156389287 is fixed, this should be proper scrollTo with reverse handling
            scrollBy(action = { x: Float, y: Float ->
                if (isVertical) {
                    state.scrollBy(y)
                } else {
                    state.scrollBy(x)
                }
                return@scrollBy true
            })
        }
    }
    val scrolling = Modifier.scrollable(
        orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
        reverseDirection = !reverseScrolling,
        enabled = isScrollable,
        controller = state.scrollableController
    )
    val layout = ScrollingLayoutModifier(state, reverseScrolling, isVertical)
    semantics.plus(scrolling).clipToBounds().plus(layout)
}

private data class ScrollingLayoutModifier(
    val scrollerState: ScrollState,
    val isReversed: Boolean,
    val isVertical: Boolean
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val childConstraints = constraints.copy(
            maxHeight = if (isVertical) Constraints.Infinity else constraints.maxHeight,
            maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity
        )
        val placeable = measurable.measure(childConstraints)
        val width = placeable.width.coerceAtMost(constraints.maxWidth)
        val height = placeable.height.coerceAtMost(constraints.maxHeight)
        val scrollHeight = placeable.height.toFloat() - height.toFloat()
        val scrollWidth = placeable.width.toFloat() - width.toFloat()
        val side = if (isVertical) scrollHeight else scrollWidth
        return layout(width, height) {
            scrollerState.maxValue = side
            val scroll = scrollerState.value.coerceIn(0f, side)
            val absScroll = if (isReversed) scroll - side else -scroll
            val xOffset = if (isVertical) 0 else absScroll.roundToInt()
            val yOffset = if (isVertical) absScroll.roundToInt() else 0
            placeable.place(xOffset, yOffset)
        }
    }
}

/**
 * Create and [remember] the state for a [VerticalScroller] or [HorizontalScroller] based on the
 * currently appropriate scroll configuration to allow changing scroll position or observing
 * scroll behavior.
 *
 * @param initial initial scroller position to start with
 * @param isReversed whether position will be reversed, e.g. 0 will mean bottom for
 * [VerticalScroller] and end for [HorizontalScroller]
 */
@Deprecated(
    "Use rememberScrollState instead", replaceWith = ReplaceWith(
        "rememberScrollState(initial = initial",
        "androidx.compose.foundation.rememberScrollState"
    )
)
@Composable
fun ScrollerPosition(
    initial: Float = 0f,
    isReversed: Boolean = false
): ScrollerPosition {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val config = defaultFlingConfig()
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
@Deprecated(
    "Use ScrollState instead", replaceWith = ReplaceWith(
        "ScrollState(" +
                "initial = initial," +
                " flingConfig = flingConfig, " +
                "animationClock = animationClock)", "androidx.compose.foundation.ScrollState"
    )
)
class ScrollerPosition(
    /** Configuration that specifies fling logic when scrolling ends with velocity. */
    internal val flingConfig: FlingConfig,
    initial: Float = 0f,
    internal val isReversed: Boolean = false,
    internal val animationClock: AnimationClockObservable
) {

    private fun directionalValue(value: Float) = if (isReversed) value else -value

    internal val state = ScrollState(initial, flingConfig, animationClock)

    /**
     * current scroller position value in pixels
     */
    var value: Float
        get() = state.value
        set(value) {
            state.scrollTo(value)
        }

    /**
     * maxPosition this scroller that consume this ScrollerPosition can reach in pixels, or
     * [Float.POSITIVE_INFINITY] if still unknown
     */
    var maxPosition: Float
        get() = state.maxValue
        set(newMax) {
            state.maxValue = newMax
        }

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
        state.smoothScrollBy(
            value = directionalValue(value),
            onEnd = onEnd
        )
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

@Deprecated(
    "Use ScrollableColumn instead", replaceWith = ReplaceWith(
        "ScrollableColumn(modifier = modifier, children = children)",
        "androidx.compose.foundation.ScrollableColumn"
    )
)
@Composable
fun VerticalScroller(
    scrollerPosition: ScrollerPosition = ScrollerPosition(),
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    children: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollerPosition.state, isScrollable)
            .clipToBounds(),
        children = children
    )
}

/**
 * A container that composes all of its contents and lays it out, fitting the height of the child.
 * If the child's width is less than the [Constraints.maxWidth], the child's width is used,
 * or the [Constraints.maxWidth] otherwise. If the contents don't fit the width, the drag gesture
 * allows scrolling its content horizontally. The contents of the HorizontalScroller are clipped to
 * the HorizontalScroller's bounds.
 *
 * If you want to control scrolling position from the code, e.g smooth scroll to position,
 * you must own memorized instance of [ScrollerPosition] and then use it to call `scrollTo...`
 * functions on it. Same tactic can be applied to the [VerticalScroller]
 *
 * @param scrollerPosition state of this Scroller that holds current scroll position and provides
 * user with useful methods like smooth scrolling
 * @param modifier Modifier to be applied to the Scroller content layout
 * @param isScrollable param to enabled or disable touch input scrolling, default is true
 */
@Deprecated(
    "Use ScrollableRow instead", replaceWith = ReplaceWith(
        "ScrollableRow(modifier = modifier, children = children)",
        "androidx.compose.foundation.ScrollableRow"
    )
)
@Composable
fun HorizontalScroller(
    scrollerPosition: ScrollerPosition = ScrollerPosition(),
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    children: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .horizontalScroll(scrollerPosition.state, isScrollable)
            .clipToBounds(),
        children = children
    )
}