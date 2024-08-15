/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingAppBarDefaults.horizontalEnterTransition
import androidx.compose.material3.FloatingAppBarDefaults.horizontalExitTransition
import androidx.compose.material3.FloatingAppBarDefaults.verticalEnterTransition
import androidx.compose.material3.FloatingAppBarDefaults.verticalExitTransition
import androidx.compose.material3.FloatingAppBarPosition.Companion.Bottom
import androidx.compose.material3.FloatingAppBarPosition.Companion.End
import androidx.compose.material3.FloatingAppBarPosition.Companion.Start
import androidx.compose.material3.FloatingAppBarPosition.Companion.Top
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * @sample androidx.compose.material3.samples.HorizontalFloatingAppBar
 * @param expanded whether the FloatingAppBar is in expanded mode, i.e. showing [leadingContent] and
 *   [trailingContent].
 * @param modifier the [Modifier] to be applied to this FloatingAppBar.
 * @param containerColor the color used for the background of this FloatingAppBar. Use
 *   [Color.Transparent] to have no color.
 * @param contentPadding the padding applied to the content of this FloatingAppBar.
 * @param scrollBehavior a [FloatingAppBarScrollBehavior].
 * @param shape the shape used for this FloatingAppBar.
 * @param leadingContent the leading content of this FloatingAppBar. The default layout here is a
 *   [Row], so content inside will be placed horizontally. Only showing if [expanded] is true.
 * @param trailingContent the trailing content of this FloatingAppBar. The default layout here is a
 *   [Row], so content inside will be placed horizontally. Only showing if [expanded] is true.
 * @param content the main content of this FloatingAppBar. The default layout here is a [Row], so
 *   content inside will be placed horizontally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun HorizontalFloatingAppBar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = FloatingAppBarDefaults.ContainerColor,
    contentPadding: PaddingValues = FloatingAppBarDefaults.ContentPadding,
    scrollBehavior: FloatingAppBarScrollBehavior? = null,
    shape: Shape = FloatingAppBarDefaults.ContainerShape,
    leadingContent: @Composable (RowScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier =
            modifier
                .then(
                    scrollBehavior?.let { with(it) { Modifier.floatingScrollBehavior() } }
                        ?: Modifier
                )
                .height(FloatingAppBarDefaults.ContainerSize)
                .background(color = containerColor, shape = shape)
                .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent?.let {
            val alignment = if (isRtl) Alignment.Start else Alignment.End
            AnimatedVisibility(
                visible = expanded,
                enter = horizontalEnterTransition(expandFrom = alignment),
                exit = horizontalExitTransition(shrinkTowards = alignment),
            ) {
                Row(content = it)
            }
        }
        content()
        trailingContent?.let {
            val alignment = if (isRtl) Alignment.End else Alignment.Start
            AnimatedVisibility(
                visible = expanded,
                enter = horizontalEnterTransition(expandFrom = alignment),
                exit = horizontalExitTransition(shrinkTowards = alignment),
            ) {
                Row(content = it)
            }
        }
    }
}

/**
 * @sample androidx.compose.material3.samples.VerticalFloatingAppBar
 * @param expanded whether the FloatingAppBar is in expanded mode, i.e. showing [leadingContent] and
 *   [trailingContent].
 * @param modifier the [Modifier] to be applied to this FloatingAppBar.
 * @param containerColor the color used for the background of this FloatingAppBar. Use
 *   Color.Transparent] to have no color.
 * @param contentPadding the padding applied to the content of this FloatingAppBar.
 * @param scrollBehavior a [FloatingAppBarScrollBehavior].
 * @param shape the shape used for this FloatingAppBar.
 * @param leadingContent the leading content of this FloatingAppBar. The default layout here is a
 *   [Column], so content inside will be placed vertically. Only showing if [expanded] is true.
 * @param trailingContent the trailing content of this FloatingAppBar. The default layout here is a
 *   [Column], so content inside will be placed vertically. Only showing if [expanded] is true.
 * @param content the main content of this FloatingAppBar. The default layout here is a [Column], so
 *   content inside will be placed vertically.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun VerticalFloatingAppBar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = FloatingAppBarDefaults.ContainerColor,
    contentPadding: PaddingValues = FloatingAppBarDefaults.ContentPadding,
    scrollBehavior: FloatingAppBarScrollBehavior? = null,
    shape: Shape = FloatingAppBarDefaults.ContainerShape,
    leadingContent: @Composable (ColumnScope.() -> Unit)? = null,
    trailingContent: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier =
            modifier
                .then(
                    scrollBehavior?.let { with(it) { Modifier.floatingScrollBehavior() } }
                        ?: Modifier
                )
                .width(FloatingAppBarDefaults.ContainerSize)
                .background(color = containerColor, shape = shape)
                .padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        leadingContent?.let {
            AnimatedVisibility(
                visible = expanded,
                enter = verticalEnterTransition(expandFrom = Alignment.Bottom),
                exit = verticalExitTransition(shrinkTowards = Alignment.Bottom),
            ) {
                Column(content = it)
            }
        }
        content()
        trailingContent?.let {
            AnimatedVisibility(
                visible = expanded,
                enter = verticalEnterTransition(expandFrom = Alignment.Top),
                exit = verticalExitTransition(shrinkTowards = Alignment.Top),
            ) {
                Column(content = it)
            }
        }
    }
}

/**
 * A FloatingAppBarScrollBehavior defines how a floating app bar should behave when the content
 * under it is scrolled.
 *
 * @see [FloatingAppBarDefaults.exitAlwaysScrollBehavior]
 */
@ExperimentalMaterial3ExpressiveApi
@Stable
sealed interface FloatingAppBarScrollBehavior : NestedScrollConnection {

    /** Indicates the position relative to the screen. */
    val position: FloatingAppBarPosition

    /** The offset from the edge of the screen. */
    val screenOffset: Dp

    /**
     * A [FloatingAppBarState] that is attached to this behavior and is read and updated when
     * scrolling happens.
     */
    val state: FloatingAppBarState

    /**
     * An [AnimationSpec] that defines how the floating app bar snaps to either fully collapsed or
     * fully extended state when a fling or a drag scrolled it into an intermediate position.
     */
    val snapAnimationSpec: AnimationSpec<Float>

    /**
     * An [DecayAnimationSpec] that defines how to fling the floating app bar when the user flings
     * the app bar itself, or the content below it.
     */
    val flingAnimationSpec: DecayAnimationSpec<Float>

    /** A [Modifier] that is attached to this behavior. */
    @Composable fun Modifier.floatingScrollBehavior(): Modifier
}

/**
 * A [FloatingAppBarScrollBehavior] that adjusts its properties to affect the size of a floating app
 * bar.
 *
 * A floating app bar that is set up with this [FloatingAppBarScrollBehavior] will immediately
 * collapse when the nested content is pulled up, and will immediately appear when the content is
 * pulled down.
 *
 * @param position indicates the position relative to the screen
 * @param screenOffset offset from the edge of the screen
 * @param state a [FloatingAppBarState]
 * @param snapAnimationSpec an [AnimationSpec] that defines how the floating app bar snaps to either
 *   fully collapsed or fully extended state when a fling or a drag scrolled it into an intermediate
 *   position
 * @param flingAnimationSpec an [DecayAnimationSpec] that defines how to fling the floating app bar
 *   when the user flings the app bar itself, or the content below it
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class ExitAlwaysFloatingAppBarScrollBehavior(
    override val position: FloatingAppBarPosition,
    override val screenOffset: Dp,
    override val state: FloatingAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>,
    override val flingAnimationSpec: DecayAnimationSpec<Float>,
) : FloatingAppBarScrollBehavior {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        state.contentOffset += consumed.y
        if (state.offset == 0f || state.offset == state.offsetLimit) {
            if (consumed.y == 0f && available.y > 0f) {
                // Reset the total content offset to zero when scrolling all the way down.
                // This will eliminate some float precision inaccuracies.
                state.contentOffset = 0f
            }
        }
        state.offset += consumed.y
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val superConsumed = super.onPostFling(consumed, available)
        return superConsumed +
            settleFloatingAppBar(state, available.y, snapAnimationSpec, flingAnimationSpec)
    }

    @Composable
    override fun Modifier.floatingScrollBehavior(): Modifier {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val orientation =
            when (position) {
                Start,
                End -> Orientation.Horizontal
                else -> Orientation.Vertical
            }

        return this.layout { measurable, constraints ->
                // Sets the app bar's offset to collapse the entire bar's when content scrolled.
                val placeable = measurable.measure(constraints)
                val limit =
                    when (position) {
                        Start,
                        End -> placeable.width + screenOffset.toPx()
                        else -> placeable.height + screenOffset.toPx()
                    }
                state.offsetLimit = -limit

                val offset =
                    if (position in listOf(Start, End) && isRtl) -state.offset else state.offset
                layout(placeable.width, placeable.height) {
                    when (position) {
                        Start -> placeable.placeWithLayer(offset.roundToInt(), 0)
                        End -> placeable.placeWithLayer(-offset.roundToInt(), 0)
                        Top -> placeable.placeWithLayer(0, offset.roundToInt())
                        Bottom -> placeable.placeWithLayer(0, -offset.roundToInt())
                    }
                }
            }
            .draggable(
                orientation = orientation,
                state =
                    rememberDraggableState { delta ->
                        val offset = if (position in listOf(Start, End) && isRtl) -delta else delta
                        when (position) {
                            Start,
                            Top -> state.offset += offset
                            End,
                            Bottom -> state.offset -= offset
                        }
                    },
                onDragStopped = { velocity ->
                    settleFloatingAppBar(state, velocity, snapAnimationSpec, flingAnimationSpec)
                }
            )
    }
}

// TODO tokens
/** Contains default values used for the floating app bar implementations. */
@ExperimentalMaterial3ExpressiveApi
object FloatingAppBarDefaults {

    /** Default size used for [HorizontalFloatingAppBar] and [VerticalFloatingAppBar] container */
    val ContainerSize: Dp = 64.dp

    /** Default color used for [HorizontalFloatingAppBar] and [VerticalFloatingAppBar] container */
    val ContainerColor: Color
        @Composable get() = ColorSchemeKeyTokens.PrimaryContainer.value

    /** Default elevation used for [HorizontalFloatingAppBar] and [VerticalFloatingAppBar] */
    val ContainerElevation: Dp = ElevationTokens.Level0

    /** Default shape used for [HorizontalFloatingAppBar] and [VerticalFloatingAppBar] */
    val ContainerShape: Shape
        @Composable get() = ShapeKeyTokens.CornerFull.value

    /**
     * Default padding used for [HorizontalFloatingAppBar] and [VerticalFloatingAppBar] when content
     * are default size (24dp) icons in [IconButton] that meet the minimum touch target (48.dp).
     */
    val ContentPadding = PaddingValues(12.dp)

    /**
     * Default offset from the edge of the screen used for [HorizontalFloatingAppBar] and
     * [VerticalFloatingAppBar].
     */
    val ScreenOffset = 16.dp

    // TODO: note that this scroll behavior may impact assistive technologies making the component
    //  inaccessible. See @sample androidx.compose.material3.samples.HorizontalFloatingAppBar on how
    //  to disable scrolling when touch exploration is enabled.
    /**
     * Returns a [FloatingAppBarScrollBehavior]. A floating app bar that is set up with this
     * [FloatingAppBarScrollBehavior] will immediately collapse when the content is pulled up, and
     * will immediately appear when the content is pulled down.
     *
     * @param position indicates the position relative to the screen
     * @param screenOffset offset from the edge of the screen
     * @param state the state object to be used to control or observe the floating app bar's scroll
     *   state. See [rememberFloatingAppBarState] for a state that is remembered across
     *   compositions.
     * @param snapAnimationSpec an [AnimationSpec] that defines how the floating app bar snaps to
     *   either fully collapsed or fully extended state when a fling or a drag scrolled it into an
     *   intermediate position
     * @param flingAnimationSpec an [DecayAnimationSpec] that defines how to fling the floating app
     *   bar when the user flings the app bar itself, or the content below it
     */
    // TODO Load the motionScheme tokens from the component tokens file
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun exitAlwaysScrollBehavior(
        position: FloatingAppBarPosition,
        screenOffset: Dp = ScreenOffset,
        state: FloatingAppBarState = rememberFloatingAppBarState(),
        snapAnimationSpec: AnimationSpec<Float> = MotionSchemeKeyTokens.DefaultEffects.value(),
        flingAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    ): FloatingAppBarScrollBehavior =
        remember(position, screenOffset, state, snapAnimationSpec, flingAnimationSpec) {
            ExitAlwaysFloatingAppBarScrollBehavior(
                position = position,
                screenOffset = screenOffset,
                state = state,
                snapAnimationSpec = snapAnimationSpec,
                flingAnimationSpec = flingAnimationSpec
            )
        }

    /** Default enter transition used for [HorizontalFloatingAppBar] when expanding */
    @Composable
    fun horizontalEnterTransition(expandFrom: Alignment.Horizontal) =
        expandHorizontally(
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
            expandFrom = expandFrom,
        )

    /** Default enter transition used for [VerticalFloatingAppBar] when expanding */
    @Composable
    fun verticalEnterTransition(expandFrom: Alignment.Vertical) =
        expandVertically(
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
            expandFrom = expandFrom,
        )

    /** Default exit transition used for [HorizontalFloatingAppBar] when shrinking */
    @Composable
    fun horizontalExitTransition(shrinkTowards: Alignment.Horizontal) =
        shrinkHorizontally(
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
            shrinkTowards = shrinkTowards,
        )

    /** Default exit transition used for [VerticalFloatingAppBar] when shrinking */
    @Composable
    fun verticalExitTransition(shrinkTowards: Alignment.Vertical) =
        shrinkVertically(
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
            shrinkTowards = shrinkTowards,
        )
}

/**
 * Creates a [FloatingAppBarState] that is remembered across compositions.
 *
 * @param initialOffsetLimit the initial value for [FloatingAppBarState.offsetLimit], which
 *   represents the pixel limit that a floating app bar is allowed to collapse when the scrollable
 *   content is scrolled.
 * @param initialOffset the initial value for [FloatingAppBarState.offset]. The initial offset
 *   should be between zero and [initialOffsetLimit].
 * @param initialContentOffset the initial value for [FloatingAppBarState.contentOffset]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun rememberFloatingAppBarState(
    initialOffsetLimit: Float = -Float.MAX_VALUE,
    initialOffset: Float = 0f,
    initialContentOffset: Float = 0f
): FloatingAppBarState {
    return rememberSaveable(saver = FloatingAppBarState.Saver) {
        FloatingAppBarState(initialOffsetLimit, initialOffset, initialContentOffset)
    }
}

/**
 * A state object that can be hoisted to control and observe the floating app bar state. The state
 * is read and updated by a [FloatingAppBarScrollBehavior] implementation.
 *
 * In most cases, this state will be created via [rememberFloatingAppBarState].
 */
@ExperimentalMaterial3ExpressiveApi
interface FloatingAppBarState {

    /**
     * The floating app bar's offset limit in pixels, which represents the limit that a floating app
     * bar is allowed to collapse to.
     *
     * Use this limit to coerce the [offset] value when it's updated.
     */
    var offsetLimit: Float

    /**
     * The floating app bar's current offset in pixels. This offset is applied to the fixed size of
     * the app bar to control the displayed size when content is being scrolled.
     *
     * Updates to the [offset] value are coerced between zero and [offsetLimit].
     */
    var offset: Float

    /**
     * The total offset of the content scrolled under the floating app bar.
     *
     * This value is updated by a [FloatingAppBarScrollBehavior] whenever a nested scroll connection
     * consumes scroll events. A common implementation would update the value to be the sum of all
     * [NestedScrollConnection.onPostScroll] `consumed` values.
     */
    var contentOffset: Float

    companion object {
        /** The default [Saver] implementation for [FloatingAppBarState]. */
        internal val Saver: Saver<FloatingAppBarState, *> =
            listSaver(
                save = { listOf(it.offsetLimit, it.offset, it.contentOffset) },
                restore = {
                    FloatingAppBarState(
                        initialOffsetLimit = it[0],
                        initialOffset = it[1],
                        initialContentOffset = it[2]
                    )
                }
            )
    }
}

/**
 * Creates a [FloatingAppBarState].
 *
 * @param initialOffsetLimit the initial value for [FloatingAppBarState.offsetLimit], which
 *   represents the pixel limit that a floating app bar is allowed to collapse when the scrollable
 *   content is scrolled.
 * @param initialOffset the initial value for [FloatingAppBarState.offset]. The initial offset
 *   should be between zero and [initialOffsetLimit].
 * @param initialContentOffset the initial value for [FloatingAppBarState.contentOffset]
 */
@ExperimentalMaterial3ExpressiveApi
fun FloatingAppBarState(
    initialOffsetLimit: Float,
    initialOffset: Float,
    initialContentOffset: Float
): FloatingAppBarState =
    FloatingAppBarStateImpl(initialOffsetLimit, initialOffset, initialContentOffset)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Stable
private class FloatingAppBarStateImpl(
    initialOffsetLimit: Float,
    initialOffset: Float,
    initialContentOffset: Float
) : FloatingAppBarState {

    override var offsetLimit by mutableFloatStateOf(initialOffsetLimit)

    override var offset: Float
        get() = _offset.floatValue
        set(newOffset) {
            _offset.floatValue = newOffset.coerceIn(minimumValue = offsetLimit, maximumValue = 0f)
        }

    override var contentOffset by mutableFloatStateOf(initialContentOffset)

    private var _offset = mutableFloatStateOf(initialOffset)
}

/**
 * Settles the app bar by flinging, in case the given velocity is greater than zero, and snapping
 * after the fling settles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private suspend fun settleFloatingAppBar(
    state: FloatingAppBarState,
    velocity: Float,
    snapAnimationSpec: AnimationSpec<Float>,
    flingAnimationSpec: DecayAnimationSpec<Float>
): Velocity {
    // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
    // and just return Zero Velocity.
    // Note that we don't check for 0f due to float precision with the collapsedFraction
    // calculation.
    val collapsedFraction = state.collapsedFraction()
    if (collapsedFraction < 0.01f || collapsedFraction == 1f) {
        return Velocity.Zero
    }
    var remainingVelocity = velocity
    // In case there is an initial velocity that was left after a previous user fling, animate to
    // continue the motion to expand or collapse the app bar.
    if (abs(velocity) > 1f) {
        var lastValue = 0f
        AnimationState(
                initialValue = 0f,
                initialVelocity = velocity,
            )
            .animateDecay(flingAnimationSpec) {
                val delta = value - lastValue
                val initialOffset = state.offset
                state.offset = initialOffset + delta
                val consumed = abs(initialOffset - state.offset)
                lastValue = value
                remainingVelocity = this.velocity
                // avoid rounding errors and stop if anything is unconsumed
                if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
            }
    }

    if (state.offset < 0 && state.offset > state.offsetLimit) {
        AnimationState(initialValue = state.offset).animateTo(
            if (state.collapsedFraction() < 0.5f) {
                0f
            } else {
                state.offsetLimit
            },
            animationSpec = snapAnimationSpec
        ) {
            state.offset = value
        }
    }

    return Velocity(0f, remainingVelocity)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun FloatingAppBarState.collapsedFraction() =
    if (offsetLimit != 0f) {
        offset / offsetLimit
    } else {
        0f
    }

/**
 * The possible positions for a [HorizontalFloatingAppBar] or [VerticalFloatingAppBar], used to
 * determine the direction when a [FloatingAppBarScrollBehavior] is attached.
 */
@ExperimentalMaterial3ExpressiveApi
@kotlin.jvm.JvmInline
value class FloatingAppBarPosition
internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /** Position FloatingAppBar at the bottom of the screen */
        val Bottom = FloatingAppBarPosition(0)

        /** Position FloatingAppBar at the top of the screen */
        val Top = FloatingAppBarPosition(1)

        /** Position FloatingAppBar at the start of the screen */
        val Start = FloatingAppBarPosition(2)

        /** Position FloatingAppBar at the end of the screen */
        val End = FloatingAppBarPosition(3)
    }

    override fun toString(): String {
        return when (this) {
            Bottom -> "FloatingAppBarPosition.Bottom"
            Top -> "FloatingAppBarPosition.Top"
            Start -> "FloatingAppBarPosition.Start"
            else -> "FloatingAppBarPosition.End"
        }
    }
}
