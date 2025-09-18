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

package androidx.compose.foundation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt

/**
 * Create and [remember] the [ScrollState] based on the currently appropriate scroll configuration
 * to allow changing scroll position or observing scroll behavior.
 *
 * Learn how to control the state of [Modifier.verticalScroll] or [Modifier.horizontalScroll]:
 *
 * @sample androidx.compose.foundation.samples.ControlledScrollableRowSample
 * @param initial initial scroller position to start with
 */
@Composable
fun rememberScrollState(initial: Int = 0): ScrollState {
    return rememberSaveable(saver = ScrollState.Saver) { ScrollState(initial = initial) }
}

/**
 * State of the scroll. Allows the developer to change the scroll position or get current state by
 * calling methods on this object. To be hosted and passed to [Modifier.verticalScroll] or
 * [Modifier.horizontalScroll]
 *
 * To create and automatically remember [ScrollState] with default parameters use
 * [rememberScrollState].
 *
 * Learn how to control the state of [Modifier.verticalScroll] or [Modifier.horizontalScroll]:
 *
 * @sample androidx.compose.foundation.samples.ControlledScrollableRowSample
 * @param initial value of the scroll
 */
@Stable
class ScrollState(initial: Int) : ScrollableState {

    /** current scroll position value in pixels */
    @get:FrequentlyChangingValue
    var value: Int by mutableIntStateOf(initial)
        private set

    /** maximum bound for [value], or [Int.MAX_VALUE] if still unknown */
    var maxValue: Int
        get() = _maxValueState.intValue
        internal set(newMax) {
            _maxValueState.intValue = newMax
            Snapshot.withoutReadObservation {
                if (value > newMax) {
                    value = newMax
                }
            }
        }

    /**
     * Size of the viewport on the scrollable axis, or 0 if still unknown. Note that this value is
     * only populated after the first measure pass.
     */
    var viewportSize: Int by mutableIntStateOf(0)
        internal set

    /**
     * [InteractionSource] that will be used to dispatch drag events when this list is being
     * dragged. If you want to know whether the fling (or smooth scroll) is in progress, use
     * [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = internalInteractionSource

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    private var _maxValueState = mutableIntStateOf(Int.MAX_VALUE)

    /**
     * We receive scroll events in floats but represent the scroll position in ints so we have to
     * manually accumulate the fractional part of the scroll to not completely ignore it.
     */
    private var accumulator: Float = 0f

    private val scrollableState = ScrollableState {
        val absolute = (value + it + accumulator)
        val newValue = absolute.coerceIn(0f, maxValue.toFloat())
        val changed = absolute != newValue
        val consumed = newValue - value
        val consumedInt = consumed.fastRoundToInt()
        value += consumedInt
        accumulator = consumed - consumedInt

        // Avoid floating-point rounding error
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

    @get:Suppress("GetterSetterNames")
    override val lastScrolledForward: Boolean
        get() = scrollableState.lastScrolledForward

    @get:Suppress("GetterSetterNames")
    override val lastScrolledBackward: Boolean
        get() = scrollableState.lastScrolledBackward

    /**
     * Scroll to position in pixels with animation.
     *
     * @param value target value in pixels to smooth scroll to, value will be coerced to
     *   0..maxPosition
     * @param animationSpec animation curve for smooth scroll animation
     */
    suspend fun animateScrollTo(value: Int, animationSpec: AnimationSpec<Float> = SpringSpec()) {
        this.animateScrollBy((value - this.value).toFloat(), animationSpec)
    }

    /**
     * Instantly jump to the given position in pixels.
     *
     * Cancels the currently running scroll, if any, and suspends until the cancellation is
     * complete.
     *
     * @param value number of pixels to scroll by
     * @return the amount of scroll consumed
     * @see animateScrollTo for an animated version
     */
    suspend fun scrollTo(value: Int): Float = this.scrollBy((value - this.value).toFloat())

    companion object {
        /** The default [Saver] implementation for [ScrollState]. */
        val Saver: Saver<ScrollState, *> = Saver(save = { it.value }, restore = { ScrollState(it) })
    }
}

/**
 * Modify element to allow to scroll vertically when height of the content is bigger than max
 * constraints allow.
 *
 * @sample androidx.compose.foundation.samples.VerticalScrollExample
 *
 * In order to use this modifier, you need to create and own [ScrollState]
 *
 * See the other overload in order to provide a custom [OverscrollEffect]
 *
 * @param state state of the scroll
 * @param enabled whether or not scrolling via touch input is enabled
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 *   will mean bottom, when `false`, 0 [ScrollState.value] will mean top
 * @see [rememberScrollState]
 */
fun Modifier.verticalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
) =
    scroll(
        state = state,
        isScrollable = enabled,
        reverseScrolling = reverseScrolling,
        flingBehavior = flingBehavior,
        isVertical = true,
        useLocalOverscrollFactory = true,
    )

/**
 * Modify element to allow to scroll vertically when height of the content is bigger than max
 * constraints allow.
 *
 * @sample androidx.compose.foundation.samples.VerticalScrollExample
 *
 * In order to use this modifier, you need to create and own [ScrollState]
 *
 * @param state state of the scroll
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   modifier. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param enabled whether or not scrolling via touch input is enabled
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 *   will mean bottom, when `false`, 0 [ScrollState.value] will mean top
 * @see [rememberScrollState]
 */
fun Modifier.verticalScroll(
    state: ScrollState,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
) =
    scroll(
        state = state,
        isScrollable = enabled,
        reverseScrolling = reverseScrolling,
        flingBehavior = flingBehavior,
        isVertical = true,
        useLocalOverscrollFactory = false,
        overscrollEffect = overscrollEffect,
    )

/**
 * Modify element to allow to scroll horizontally when width of the content is bigger than max
 * constraints allow.
 *
 * @sample androidx.compose.foundation.samples.HorizontalScrollSample
 *
 * In order to use this modifier, you need to create and own [ScrollState]
 *
 * See the other overload in order to provide a custom [OverscrollEffect]
 *
 * @param state state of the scroll
 * @param enabled whether or not scrolling via touch input is enabled
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 *   will mean right, when `false`, 0 [ScrollState.value] will mean left
 * @see [rememberScrollState]
 */
fun Modifier.horizontalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
) =
    scroll(
        state = state,
        isScrollable = enabled,
        reverseScrolling = reverseScrolling,
        flingBehavior = flingBehavior,
        isVertical = false,
        useLocalOverscrollFactory = true,
    )

/**
 * Modify element to allow to scroll horizontally when width of the content is bigger than max
 * constraints allow.
 *
 * @sample androidx.compose.foundation.samples.HorizontalScrollSample
 *
 * In order to use this modifier, you need to create and own [ScrollState]
 *
 * @param state state of the scroll
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   modifier. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param enabled whether or not scrolling via touch input is enabled
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 *   will mean right, when `false`, 0 [ScrollState.value] will mean left
 * @see [rememberScrollState]
 */
fun Modifier.horizontalScroll(
    state: ScrollState,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
) =
    scroll(
        state = state,
        isScrollable = enabled,
        reverseScrolling = reverseScrolling,
        flingBehavior = flingBehavior,
        isVertical = false,
        useLocalOverscrollFactory = false,
        overscrollEffect = overscrollEffect,
    )

private fun Modifier.scroll(
    state: ScrollState,
    reverseScrolling: Boolean,
    flingBehavior: FlingBehavior?,
    isScrollable: Boolean,
    isVertical: Boolean,
    useLocalOverscrollFactory: Boolean,
    overscrollEffect: OverscrollEffect? = null,
): Modifier {
    val orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal
    return scrollingContainer(
            state = state,
            orientation = orientation,
            enabled = isScrollable,
            reverseScrolling = reverseScrolling,
            flingBehavior = flingBehavior,
            interactionSource = state.internalInteractionSource,
            useLocalOverscrollFactory = useLocalOverscrollFactory,
            overscrollEffect = overscrollEffect,
        )
        .then(ScrollingLayoutElement(state, reverseScrolling, isVertical))
}

internal class ScrollingLayoutElement(
    val scrollState: ScrollState,
    val reverseScrolling: Boolean,
    val isVertical: Boolean,
) : ModifierNodeElement<ScrollNode>() {
    override fun create(): ScrollNode {
        return ScrollNode(
            state = scrollState,
            reverseScrolling = reverseScrolling,
            isVertical = isVertical,
        )
    }

    override fun update(node: ScrollNode) {
        node.state = scrollState
        node.reverseScrolling = reverseScrolling
        node.isVertical = isVertical
    }

    override fun hashCode(): Int {
        var result = scrollState.hashCode()
        result = 31 * result + reverseScrolling.hashCode()
        result = 31 * result + isVertical.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ScrollingLayoutElement) return false
        return scrollState == other.scrollState &&
            reverseScrolling == other.reverseScrolling &&
            isVertical == other.isVertical
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scroll"
        properties["state"] = scrollState
        properties["reverseScrolling"] = reverseScrolling
        properties["isVertical"] = isVertical
    }
}

internal class ScrollNode(
    var state: ScrollState,
    var reverseScrolling: Boolean,
    var isVertical: Boolean,
) : LayoutModifierNode, SemanticsModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        checkScrollableContainerConstraints(
            constraints,
            if (isVertical) Orientation.Vertical else Orientation.Horizontal,
        )

        val childConstraints =
            constraints.copy(
                maxHeight = if (isVertical) Constraints.Infinity else constraints.maxHeight,
                maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity,
            )
        val placeable = measurable.measure(childConstraints)
        val width = placeable.width.coerceAtMost(constraints.maxWidth)
        val height = placeable.height.coerceAtMost(constraints.maxHeight)
        val scrollHeight = placeable.height - height
        val scrollWidth = placeable.width - width
        val side = if (isVertical) scrollHeight else scrollWidth
        // The max value must be updated before returning from the measure block so that any other
        // chained RemeasurementModifiers that try to perform scrolling based on the new
        // measurements inside onRemeasured are able to scroll to the new max based on the newly-
        // measured size.
        state.maxValue = side
        state.viewportSize = if (isVertical) height else width
        return layout(width, height) {
            val scroll = state.value.fastCoerceIn(0, side)
            val absScroll = if (reverseScrolling) scroll - side else -scroll
            val xOffset = if (isVertical) 0 else absScroll
            val yOffset = if (isVertical) absScroll else 0

            // Tagging as direct manipulation, such that consumers of this offset can decide whether
            // to exclude this offset on their coordinates calculation. Such as whether an
            // `approachLayout` will animate it or directly apply the offset without animation.
            withMotionFrameOfReferencePlacement {
                placeable.placeRelativeWithLayer(xOffset, yOffset)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        return measurable.minIntrinsicWidth(if (isVertical) Constraints.Infinity else height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        return measurable.minIntrinsicHeight(if (isVertical) width else Constraints.Infinity)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        return measurable.maxIntrinsicWidth(if (isVertical) Constraints.Infinity else height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        return measurable.maxIntrinsicHeight(if (isVertical) width else Constraints.Infinity)
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        isTraversalGroup = true
        val accessibilityScrollState =
            ScrollAxisRange(
                value = { state.value.toFloat() },
                maxValue = { state.maxValue.toFloat() },
                reverseScrolling = reverseScrolling,
            )
        if (isVertical) {
            this.verticalScrollAxisRange = accessibilityScrollState
        } else {
            this.horizontalScrollAxisRange = accessibilityScrollState
        }
    }
}
