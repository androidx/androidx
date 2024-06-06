/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListItemInfo
import androidx.wear.compose.foundation.lazy.ScalingLazyListLayoutInfo
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.materialcore.BoundsLimiter
import androidx.wear.compose.materialcore.isLeftyModeEnabled
import androidx.wear.compose.materialcore.isRoundDevice
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Enum used by adapters to specify if the Position Indicator needs to be shown, hidden, or hidden
 * after a small delay.
 */
@kotlin.jvm.JvmInline
public value class PositionIndicatorVisibility internal constructor(internal val value: Int) {
    companion object {
        /** Show the Position Indicator. */
        val Show = PositionIndicatorVisibility(1)

        /** Hide the Position Indicator. */
        val Hide = PositionIndicatorVisibility(2)

        /** Hide the Position Indicator after a short delay. */
        val AutoHide = PositionIndicatorVisibility(3)
    }
}

/**
 * An object representing the relative position of a scrollbar or rolling side button or rotating
 * bezel position. This interface is implemented by classes that adapt other state information such
 * as [ScalingLazyListState] or [ScrollState] of scrollable containers or to represent the position
 * of say a volume control that can be 'ticked' using a rolling side button or rotating bezel.
 *
 * Implementing classes provide [positionFraction] to determine where in the range [0..1] that the
 * indicator should be displayed and [sizeFraction] to determine the size of the indicator in the
 * range [0..1]. E.g. If a [ScalingLazyListState] had 50 items and the last 5 were visible it would
 * have a position of 1.0f to show that the scroll is positioned at the end of the list and a size
 * of 5 / 50 = 0.1f to indicate that 10% of the visible items are currently visible.
 */
@Stable
interface PositionIndicatorState {
    /**
     * Position of the indicator in the range [0f,1f]. 0f means it is at the top|start, 1f means it
     * is positioned at the bottom|end.
     */
    @get:FloatRange(from = 0.0, to = 1.0) val positionFraction: Float

    /**
     * Size of the indicator in the range [0f,1f]. 1f means it takes the whole space.
     *
     * @param scrollableContainerSizePx the height or width of the container in pixels depending on
     *   orientation of the indicator, (height for vertical, width for horizontal)
     */
    @FloatRange(from = 0.0, to = 1.0)
    fun sizeFraction(@FloatRange(from = 0.0) scrollableContainerSizePx: Float): Float

    /**
     * Should we show the Position Indicator
     *
     * @param scrollableContainerSizePx the height or width of the container in pixels depending on
     *   orientation of the indicator, (height for vertical, width for horizontal)
     */
    fun visibility(
        @FloatRange(from = 0.0) scrollableContainerSizePx: Float
    ): PositionIndicatorVisibility
}

/**
 * Creates an [PositionIndicator] based on the values in a [ScrollState] object. e.g. a [Column]
 * implementing [Modifier.verticalScroll] provides a [ScrollState].
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param scrollState The scrollState to use as the basis for the PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 * @param fadeInAnimationSpec [AnimationSpec] for fade-in animation. Fade-in animation is triggered
 *   when the [PositionIndicator] becomes visible - either when state.visibility changes to Show, or
 *   state.visibility is AutoHide and state.positionFraction/state.sizeFraction are changed. To
 *   disable this animation [snap] AnimationSpec should be passed instead.
 * @param fadeOutAnimationSpec [AnimationSpec] for fade-out animation. The Fade-out animation is
 *   used for hiding the [PositionIndicator] and making it invisible. [PositionIndicator] will be
 *   hidden after a specified delay if no changes in state.positionFraction or state.sizeFraction
 *   were detected. If [fadeOutAnimationSpec] is [snap], then after a delay it will be instantly
 *   hidden.
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes between state.positionFraction and state.sizeFraction of
 *   [PositionIndicatorState]. To disable this animation [snap] AnimationSpec should be passed
 *   instead.
 */
@Composable
public fun PositionIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false,
    fadeInAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    fadeOutAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    positionAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.positionAnimationSpec
) =
    PositionIndicator(
        ScrollStateAdapter(scrollState),
        indicatorHeight = 50.dp,
        indicatorWidth = 4.dp,
        paddingHorizontal = PositionIndicatorDefaults.horizontalPadding,
        modifier = modifier,
        reverseDirection = reverseDirection,
        fadeInAnimationSpec = fadeInAnimationSpec,
        fadeOutAnimationSpec = fadeOutAnimationSpec,
        positionAnimationSpec = positionAnimationSpec
    )

/**
 * Creates an [PositionIndicator] based on the values in a [ScrollState] object. e.g. a [Column]
 * implementing [Modifier.verticalScroll] provides a [ScrollState].
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param scrollState The scrollState to use as the basis for the PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Deprecated(
    "This overload is provided for backwards compatibility with " +
        "Compose for Wear OS 1.2." +
        "A newer overload is available with additional fadeInAnimationSpec, " +
        "fadeOutAnimationSpec and positionAnimationSpec parameters.",
    level = DeprecationLevel.HIDDEN
)
@Composable
public fun PositionIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false
) =
    PositionIndicator(
        scrollState = scrollState,
        modifier = modifier,
        reverseDirection = reverseDirection
    )

/**
 * Creates an [PositionIndicator] based on the values in a [ScalingLazyListState] object that a
 * [ScalingLazyColumn] uses.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param scalingLazyListState the [ScalingLazyListState] to use as the basis for the
 *   PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 * @param fadeInAnimationSpec [AnimationSpec] for fade-in animation. Fade-in animation is triggered
 *   when the [PositionIndicator] becomes visible - either when state.visibility changes to Show, or
 *   state.visibility is AutoHide and state.positionFraction/state.sizeFraction are changed. To
 *   disable this animation [snap] AnimationSpec should be passed instead.
 * @param fadeOutAnimationSpec [AnimationSpec] for fade-out animation. The Fade-out animation is
 *   used for hiding the [PositionIndicator] and making it invisible. [PositionIndicator] will be
 *   hidden after a specified delay if no changes in state.positionFraction or state.sizeFraction
 *   were detected. If [fadeOutAnimationSpec] is [snap], then after a delay it will be instantly
 *   hidden.
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes between state.positionFraction and state.sizeFraction of
 *   [PositionIndicatorState]. To disable this animation [snap] AnimationSpec should be passed
 *   instead.
 */
@Composable
public fun PositionIndicator(
    scalingLazyListState: ScalingLazyListState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false,
    fadeInAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    fadeOutAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    positionAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.positionAnimationSpec
) =
    PositionIndicator(
        state = ScalingLazyColumnStateAdapter(state = scalingLazyListState),
        indicatorHeight = 50.dp,
        indicatorWidth = 4.dp,
        paddingHorizontal = PositionIndicatorDefaults.horizontalPadding,
        modifier = modifier,
        reverseDirection = reverseDirection,
        fadeInAnimationSpec = fadeInAnimationSpec,
        fadeOutAnimationSpec = fadeOutAnimationSpec,
        positionAnimationSpec = positionAnimationSpec
    )

/**
 * Creates an [PositionIndicator] based on the values in a [ScalingLazyListState] object that a
 * [ScalingLazyColumn] uses.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param scalingLazyListState the [ScalingLazyListState] to use as the basis for the
 *   PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Deprecated(
    "This overload is provided for backwards compatibility with " +
        "Compose for Wear OS 1.2." +
        "A newer overload is available with additional fadeInAnimationSpec, " +
        "fadeOutAnimationSpec and positionAnimationSpec parameters.",
    level = DeprecationLevel.HIDDEN
)
@Composable
public fun PositionIndicator(
    scalingLazyListState: ScalingLazyListState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false
) =
    PositionIndicator(
        scalingLazyListState = scalingLazyListState,
        modifier = modifier,
        reverseDirection = reverseDirection
    )

/**
 * Creates an [PositionIndicator] based on the values in a [ScalingLazyListState] object that a
 * [ScalingLazyColumn] uses.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param scalingLazyListState the [ScalingLazyListState] to use as the basis for the
 *   PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Suppress("DEPRECATION")
@Deprecated(
    "This overload is provided for backwards compatibility with Compose for Wear OS 1.1." +
        "A newer overload is available which uses ScalingLazyListState from " +
        "androidx.wear.compose.foundation.lazy package",
    level = DeprecationLevel.WARNING
)
@Composable
public fun PositionIndicator(
    scalingLazyListState: androidx.wear.compose.material.ScalingLazyListState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false
) =
    PositionIndicator(
        state = MaterialScalingLazyColumnStateAdapter(state = scalingLazyListState),
        indicatorHeight = 50.dp,
        indicatorWidth = 4.dp,
        paddingHorizontal = PositionIndicatorDefaults.horizontalPadding,
        modifier = modifier,
        reverseDirection = reverseDirection
    )

/**
 * Creates an [PositionIndicator] based on the values in a [LazyListState] object that a
 * [LazyColumn] uses.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param lazyListState the [LazyListState] to use as the basis for the PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 * @param fadeInAnimationSpec [AnimationSpec] for fade-in animation. Fade-in animation is triggered
 *   when the [PositionIndicator] becomes visible - either when state.visibility changes to Show, or
 *   state.visibility is AutoHide and state.positionFraction/state.sizeFraction are changed. To
 *   disable this animation [snap] AnimationSpec should be passed instead.
 * @param fadeOutAnimationSpec [AnimationSpec] for fade-out animation. The Fade-out animation is
 *   used for hiding the [PositionIndicator] and making it invisible. [PositionIndicator] will be
 *   hidden after a specified delay if no changes in state.positionFraction or state.sizeFraction
 *   were detected. If [fadeOutAnimationSpec] is [snap], then after a delay it will be instantly
 *   hidden.
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes between state.positionFraction and state.sizeFraction of
 *   [PositionIndicatorState]. To disable this animation [snap] AnimationSpec should be passed
 *   instead.
 */
@Composable
public fun PositionIndicator(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false,
    fadeInAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    fadeOutAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    positionAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.positionAnimationSpec
) =
    PositionIndicator(
        state = LazyColumnStateAdapter(state = lazyListState),
        indicatorHeight = 50.dp,
        indicatorWidth = 4.dp,
        paddingHorizontal = PositionIndicatorDefaults.horizontalPadding,
        modifier = modifier,
        reverseDirection = reverseDirection,
        fadeInAnimationSpec = fadeInAnimationSpec,
        fadeOutAnimationSpec = fadeOutAnimationSpec,
        positionAnimationSpec = positionAnimationSpec
    )

/**
 * Creates an [PositionIndicator] based on the values in a [LazyListState] object that a
 * [LazyColumn] uses.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param lazyListState the [LazyListState] to use as the basis for the PositionIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Deprecated(
    "This overload is provided for backwards compatibility with " +
        "Compose for Wear OS 1.2." +
        "A newer overload is available with additional fadeInAnimationSpec, " +
        "fadeOutAnimationSpec and positionAnimationSpec parameters.",
    level = DeprecationLevel.HIDDEN
)
@Composable
public fun PositionIndicator(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false
) =
    PositionIndicator(
        lazyListState = lazyListState,
        modifier = modifier,
        reverseDirection = reverseDirection
    )

/** Specifies where in the screen the Position indicator will be. */
@kotlin.jvm.JvmInline
value class PositionIndicatorAlignment internal constructor(internal val pos: Int) {
    companion object {
        /**
         * Position the indicator at the end of the layout (at the right for LTR and left for RTL)
         * This is the norm for scroll indicators.
         */
        val End = PositionIndicatorAlignment(0)

        // TODO(b/224770222): Add tests.
        /**
         * Position the indicator opposite to the physical rotating side button (RSB). (at the left
         * by default and at the right if the device is rotated 180 degrees) This is the default for
         * RSB indicators as we want to avoid it being obscured when the user is interacting with
         * the RSB.
         */
        val OppositeRsb = PositionIndicatorAlignment(1)

        /**
         * Position the indicator at the left of the screen. This is useful to implement custom
         * positioning, but usually [PositionIndicatorAlignment#End] or
         * [PositionIndicatorAlignment#OppositeRsb] should be used.
         */
        val Left = PositionIndicatorAlignment(2)

        /**
         * Position the indicator at the right of the screen This is useful to implement custom
         * positioning, but usually [PositionIndicatorAlignment#End] or
         * [PositionIndicatorAlignment#OppositeRsb] should be used.
         */
        val Right = PositionIndicatorAlignment(3)
    }

    override fun toString(): String {
        return when (this) {
            End -> "PositionIndicatorAlignment.End"
            OppositeRsb -> "PositionIndicatorAlignment.OppositeRsb"
            Left -> "PositionIndicatorAlignment.Left"
            Right -> "PositionIndicatorAlignment.Right"
            else -> "PositionIndicatorAlignment.unknown"
        }
    }
}

/**
 * Creates a [PositionIndicator] for controls like rotating side button, rotating bezel or slider.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param value Value of the indicator in the [range] where 1 represents the maximum value. E.g. If
 *   displaying a volume value from 0..11 then the [value] will be volume/11.
 * @param range range of values that [value] can take
 * @param modifier Modifier to be applied to the component
 * @param color Color to draw the indicator on.
 * @param reverseDirection Reverses direction of PositionIndicator if true
 * @param position indicates where to put the PositionIndicator in the screen, default is
 *   [PositionIndicatorPosition#OppositeRsb]
 * @param fadeInAnimationSpec [AnimationSpec] for fade-in animation. Fade-in animation is triggered
 *   when the [PositionIndicator] becomes visible - either when state.visibility changes to Show, or
 *   state.visibility is AutoHide and state.positionFraction/state.sizeFraction are changed. To
 *   disable this animation [snap] AnimationSpec should be passed instead.
 * @param fadeOutAnimationSpec [AnimationSpec] for fade-out animation. The Fade-out animation is
 *   used for hiding the [PositionIndicator] and making it invisible. [PositionIndicator] will be
 *   hidden after a specified delay if no changes in state.positionFraction or state.sizeFraction
 *   were detected. If [fadeOutAnimationSpec] is [snap], then after a delay it will be instantly
 *   hidden.
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes between state.positionFraction and state.sizeFraction of
 *   [PositionIndicatorState]. To disable this animation [snap] AnimationSpec should be passed
 *   instead.
 */
@Composable
public fun PositionIndicator(
    value: () -> Float,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    color: Color = MaterialTheme.colors.onBackground,
    reverseDirection: Boolean = false,
    position: PositionIndicatorAlignment = PositionIndicatorAlignment.OppositeRsb,
    fadeInAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    fadeOutAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    positionAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.positionAnimationSpec
) =
    PositionIndicator(
        state =
            FractionPositionIndicatorState {
                (value() - range.start) / (range.endInclusive - range.start)
            },
        indicatorHeight = 76.dp,
        indicatorWidth = 6.dp,
        paddingHorizontal = 5.dp,
        color = color,
        modifier = modifier,
        reverseDirection = reverseDirection,
        position = position,
        fadeInAnimationSpec = fadeInAnimationSpec,
        fadeOutAnimationSpec = fadeOutAnimationSpec,
        positionAnimationSpec = positionAnimationSpec
    )

/**
 * Creates a [PositionIndicator] for controls like rotating side button, rotating bezel or slider.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param value Value of the indicator in the [range] where 1 represents the maximum value. E.g. If
 *   displaying a volume value from 0..11 then the [value] will be volume/11.
 * @param range range of values that [value] can take
 * @param modifier Modifier to be applied to the component
 * @param color Color to draw the indicator on.
 * @param reverseDirection Reverses direction of PositionIndicator if true
 * @param position indicates where to put the PositionIndicator in the screen, default is
 *   [PositionIndicatorPosition#OppositeRsb]
 */
@Deprecated(
    "This overload is provided for backwards compatibility with " +
        "Compose for Wear OS 1.2." +
        "A newer overload is available with additional fadeInAnimationSpec, " +
        "fadeOutAnimationSpec and positionAnimationSpec parameters.",
    level = DeprecationLevel.HIDDEN
)
@Composable
public fun PositionIndicator(
    value: () -> Float,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    color: Color = MaterialTheme.colors.onBackground,
    reverseDirection: Boolean = false,
    position: PositionIndicatorAlignment = PositionIndicatorAlignment.OppositeRsb
) =
    PositionIndicator(
        value = value,
        modifier = modifier,
        range = range,
        color = color,
        reverseDirection = reverseDirection,
        position = position
    )

/**
 * An indicator on one side of the screen to show the current [PositionIndicatorState].
 *
 * Typically used with the [Scaffold] but can be used to decorate any full screen situation.
 *
 * This composable should only be used to fill the whole screen as Wear Material Design language
 * requires the placement of the position indicator to be right center of the screen as the
 * indicator is curved on circular devices.
 *
 * It detects if the screen is round or square and draws itself as a curve or line.
 *
 * Note that the composable will take the whole screen, but it will be drawn with the given
 * dimensions [indicatorHeight] and [indicatorWidth], and position with respect to the edge of the
 * screen according to [paddingHorizontal]
 *
 * This [PositionIndicator] has 3 separate animation specs to control different animations.
 * - [fadeInAnimationSpec] - controls fade-in animation.
 * - [fadeOutAnimationSpec] - controls fade-out animation.
 * - [positionAnimationSpec] - controls position change animation.
 *
 * For performance reasons and for UX consistency, when [PositionIndicator] is used with scrollable
 * list, we recommend to switch off fade-in and position animations by passing [snap] spec into
 * [fadeInAnimationSpec] and [positionAnimationSpec] parameters. If [PositionIndicator] is used as a
 * standalone indicator, for example as volume control, then we recommend to have all 3 animations
 * turned on.
 *
 * If color of [PositionIndicator] is not white and position animation is enabled - a short
 * highlight animation will be triggered on any position change. This is a short animation accenting
 * [PositionIndicator] with white color with 33% opacity.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param state the [PositionIndicatorState] of the state we are displaying.
 * @param indicatorHeight the height of the position indicator in Dp.
 * @param indicatorWidth the width of the position indicator in Dp.
 * @param paddingHorizontal the padding to apply between the indicator and the border of the screen.
 * @param modifier The modifier to be applied to the component.
 * @param background the color to draw the non-active part of the position indicator.
 * @param color the color to draw the active part of the indicator in.
 * @param reverseDirection Reverses direction of PositionIndicator if true.
 * @param position indicates where to put the PositionIndicator on the screen, default is
 *   [PositionIndicatorPosition#End]
 * @param fadeInAnimationSpec [AnimationSpec] for fade-in animation. Fade-in animation is triggered
 *   when the [PositionIndicator] becomes visible - either when state.visibility changes to Show, or
 *   state.visibility is AutoHide and state.positionFraction/state.sizeFraction are changed. To
 *   disable this animation [snap] AnimationSpec should be passed instead.
 * @param fadeOutAnimationSpec [AnimationSpec] for fade-out animation. The Fade-out animation is
 *   used for hiding the [PositionIndicator] and making it invisible. [PositionIndicator] will be
 *   hidden after a specified delay if no changes in state.positionFraction or state.sizeFraction
 *   were detected. If [fadeOutAnimationSpec] is [snap], then after a delay it will be instantly
 *   hidden.
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes between state.positionFraction and state.sizeFraction of
 *   [PositionIndicatorState]. To disable animation [snap] should be passed.
 */
@Composable
public fun PositionIndicator(
    state: PositionIndicatorState,
    indicatorHeight: Dp,
    indicatorWidth: Dp,
    paddingHorizontal: Dp,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.3f),
    color: Color = MaterialTheme.colors.onBackground,
    reverseDirection: Boolean = false,
    position: PositionIndicatorAlignment = PositionIndicatorAlignment.End,
    fadeInAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    fadeOutAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.visibilityAnimationSpec,
    positionAnimationSpec: AnimationSpec<Float> = PositionIndicatorDefaults.positionAnimationSpec
) {
    val isScreenRound = isRoundDevice()
    val layoutDirection = LocalLayoutDirection.current
    val leftyMode = isLeftyModeEnabled()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val alphaValue = remember { mutableFloatStateOf(0f) }
    val animateAlphaChannel = remember { Channel<Float>(2, BufferOverflow.DROP_OLDEST) }

    var highlightAlpha by remember { mutableFloatStateOf(0f) }
    val highlightChannel = remember { Channel<Boolean>(2, BufferOverflow.DROP_OLDEST) }
    // Showing white highlight only when color is not White
    val shouldShowHighlight = color != Color.White

    val positionFractionAnimatable = remember { Animatable(0f) }
    val sizeFractionAnimatable = remember { Animatable(0f) }

    val indicatorOnTheRight =
        when (position) {
            PositionIndicatorAlignment.End -> layoutDirection == LayoutDirection.Ltr
            PositionIndicatorAlignment.Left -> false
            PositionIndicatorAlignment.Right -> true
            PositionIndicatorAlignment.OppositeRsb -> leftyMode
            else -> true
        }

    val boundsSize: Density.() -> IntSize = {
        IntSize(
            ((if (isScreenRound) {
                    // r is the distance from the center of the container to the arc we draw the
                    // position indicator on (the center of the arc, which is indicatorWidth wide).
                    val r =
                        containerSize.width.toFloat() / 2 -
                            paddingHorizontal.toPx() -
                            indicatorWidth.toPx() / 2
                    // The sqrt is the size of the projection on the x axis of line between center
                    // of
                    // the container and the point where we start the arc.
                    // The coerceAtLeast is needed while initializing since containerSize.width is 0
                    r - sqrt((sqr(r) - sqr(indicatorHeight.toPx() / 2)).coerceAtLeast(0f))
                } else 0f) + paddingHorizontal.toPx() + indicatorWidth.toPx())
                .roundToInt(),
            (indicatorHeight.toPx() + indicatorWidth.toPx()).roundToInt()
        )
    }

    val boundsOffset: Density.() -> IntOffset = {
        // Note that indicators are on right or left, centered vertically.
        val indicatorSize = boundsSize()
        IntOffset(
            if (indicatorOnTheRight) containerSize.width - indicatorSize.width else 0,
            (containerSize.height - indicatorSize.height) / 2
        )
    }

    val updatedFadeInAnimationSpec by rememberUpdatedState(fadeInAnimationSpec)
    val updatedFadeOutAnimationSpec by rememberUpdatedState(fadeOutAnimationSpec)
    val updatedPositionAnimationSpec by rememberUpdatedState(positionAnimationSpec)

    LaunchedEffect(state) {
        // We don't want to trigger first animation when we receive position or size
        // for the first time, because initial position and size are equal to 0.
        var skipFirstPositionAnimation = true

        // Skip first alpha animation only when initial visibility is not Hide
        var skipFirstAlphaAnimation =
            state.visibility(containerSize.height.toFloat()) != PositionIndicatorVisibility.Hide

        launch {
            // This snapshotFlow listens to changes in position, size and visibility
            // of PositionIndicatorState and starts necessary animations if needed
            snapshotFlow {
                    DisplayState(
                        state.positionFraction,
                        state.sizeFraction(containerSize.height.toFloat()),
                        state.visibility(containerSize.height.toFloat())
                    )
                }
                .collectLatest {
                    // Workaround for b/315149417. When visibility is Hide and other values equal to
                    // 0,
                    // we consider that as non-initialized state.
                    // It means that we skip first alpha animation, and also ignore these values.
                    if (
                        skipFirstPositionAnimation &&
                            it.visibility == PositionIndicatorVisibility.Hide &&
                            it.position == 0f &&
                            it.size == 0f
                    ) {
                        skipFirstAlphaAnimation = true
                        return@collectLatest
                    }

                    if (skipFirstPositionAnimation || updatedPositionAnimationSpec is SnapSpec) {
                        sizeFractionAnimatable.snapTo(it.size)
                        positionFractionAnimatable.snapTo(it.position)
                        skipFirstPositionAnimation = false
                    } else {
                        launch {
                            sizeFractionAnimatable.animateTo(
                                it.size,
                                animationSpec = updatedPositionAnimationSpec
                            )
                        }
                        launch {
                            positionFractionAnimatable.animateTo(
                                it.position,
                                animationSpec = updatedPositionAnimationSpec
                            )
                        }

                        if (shouldShowHighlight) {
                            launch {
                                highlightChannel.trySend(true)
                                delay(150)
                                highlightChannel.trySend(false)
                            }
                        }
                    }

                    when (it.visibility) {
                        PositionIndicatorVisibility.Hide -> {
                            handleFadeOut(
                                updatedFadeOutAnimationSpec,
                                animateAlphaChannel,
                                alphaValue
                            )
                        }

                        // PositionIndicatorVisibility.Show and
                        // PositionIndicatorVisibility.AutoHide cases
                        else -> {
                            // If fadeInAnimationSpec is SnapSpec or we skip the first animation,
                            // then we change alphaValue directly here
                            if (updatedFadeInAnimationSpec is SnapSpec || skipFirstAlphaAnimation) {
                                alphaValue.floatValue = 1f
                                skipFirstAlphaAnimation = false
                            } else {
                                // Otherwise we send an event to animation channel
                                animateAlphaChannel.trySend(1f)
                            }

                            if (it.visibility == PositionIndicatorVisibility.AutoHide) {
                                // Waiting for 2000ms and changing alpha value to 0f
                                delay(2000)
                                handleFadeOut(
                                    updatedFadeInAnimationSpec,
                                    animateAlphaChannel,
                                    alphaValue
                                )
                            }
                        }
                    }
                }
        }
    }

    LaunchedEffect(shouldShowHighlight, positionAnimationSpec) {
        // Listens to events in [highlightChannel] and triggers
        // highlight animations to specified value.
        if (shouldShowHighlight && positionAnimationSpec !is SnapSpec) {
            launch {
                highlightChannel.receiveAsFlow().distinctUntilChanged().collectLatest {
                    showHighlight ->
                    if (showHighlight) {
                        animate(
                            highlightAlpha,
                            0.33f,
                            animationSpec =
                                tween(
                                    durationMillis = 150,
                                    easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
                                )
                        ) { value, _ ->
                            highlightAlpha = value
                        }
                    } else {
                        animate(
                            highlightAlpha,
                            0f,
                            animationSpec =
                                tween(
                                    durationMillis = 500,
                                    easing = CubicBezierEasing(0.25f, 0f, 0.75f, 1f)
                                )
                        ) { value, _ ->
                            highlightAlpha = value
                        }
                    }
                }
            }
        }

        // Listens to events in [animateAlphaChannel] and triggers
        // alpha animations to specified value.
        animateAlphaChannel.receiveAsFlow().distinctUntilChanged().collectLatest { targetValue ->
            animate(
                alphaValue.floatValue,
                targetValue,
                animationSpec =
                    if (targetValue >= 1f) updatedFadeInAnimationSpec
                    else updatedFadeOutAnimationSpec
            ) { value, _ ->
                alphaValue.floatValue = value
            }
        }
    }

    BoundsLimiter(boundsOffset, boundsSize, modifier, onSizeChanged = { containerSize = it }) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = alphaValue.floatValue }
                    .drawWithCache {
                        // We need to invert reverseDirection when the screen is round and we are on
                        // the left.
                        val actualReverseDirection =
                            if (isScreenRound && !indicatorOnTheRight) {
                                !reverseDirection
                            } else {
                                reverseDirection
                            }

                        val indicatorPosition =
                            if (actualReverseDirection) {
                                1 - positionFractionAnimatable.value
                            } else {
                                positionFractionAnimatable.value
                            }

                        val indicatorWidthPx = indicatorWidth.toPx()

                        // We want position = 0 be the indicator aligned at the top of its area and
                        // position = 1 be aligned at the bottom of the area.
                        val indicatorStart = indicatorPosition * (1 - sizeFractionAnimatable.value)

                        val diameter =
                            max(containerSize.width.toFloat(), containerSize.height.toFloat())

                        val paddingHorizontalPx = paddingHorizontal.toPx()
                        onDrawWithContent {
                            if (isScreenRound) {
                                val usableHalf = diameter / 2f - paddingHorizontalPx
                                val sweepDegrees =
                                    (2 * asin((indicatorHeight.toPx() / 2) / usableHalf))
                                        .toDegrees()

                                drawCurvedIndicator(
                                    color,
                                    background,
                                    paddingHorizontalPx,
                                    indicatorOnTheRight,
                                    sweepDegrees,
                                    indicatorWidthPx,
                                    indicatorStart,
                                    sizeFractionAnimatable.value,
                                    highlightAlpha
                                )
                            } else {
                                drawStraightIndicator(
                                    color,
                                    background,
                                    paddingHorizontalPx,
                                    indicatorOnTheRight,
                                    indicatorWidthPx,
                                    indicatorHeight.toPx(),
                                    indicatorStart,
                                    sizeFractionAnimatable.value,
                                    highlightAlpha
                                )
                            }
                        }
                    }
        )
    }
}

/**
 * An indicator on one side of the screen to show the current [PositionIndicatorState].
 *
 * Typically used with the [Scaffold] but can be used to decorate any full screen situation.
 *
 * This composable should only be used to fill the whole screen as Wear Material Design language
 * requires the placement of the position indicator to be right center of the screen as the
 * indicator is curved on circular devices.
 *
 * It detects if the screen is round or square and draws itself as a curve or line.
 *
 * Note that the composable will take the whole screen, but it will be drawn with the given
 * dimensions [indicatorHeight] and [indicatorWidth], and position with respect to the edge of the
 * screen according to [paddingHorizontal]
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param state the [PositionIndicatorState] of the state we are displaying.
 * @param indicatorHeight the height of the position indicator in Dp.
 * @param indicatorWidth the width of the position indicator in Dp.
 * @param paddingHorizontal the padding to apply between the indicator and the border of the screen.
 * @param modifier The modifier to be applied to the component.
 * @param background the color to draw the non-active part of the position indicator.
 * @param color the color to draw the active part of the indicator in.
 * @param reverseDirection Reverses direction of PositionIndicator if true.
 * @param position indicates where to put the PositionIndicator on the screen, default is
 *   [PositionIndicatorPosition#End]
 */
@Deprecated(
    "This overload is provided for backwards compatibility with " +
        "Compose for Wear OS 1.2." +
        "A newer overload is available with additional fadeInAnimationSpec, " +
        "fadeOutAnimationSpec and positionAnimationSpec parameters.",
    level = DeprecationLevel.HIDDEN
)
@Composable
public fun PositionIndicator(
    state: PositionIndicatorState,
    indicatorHeight: Dp,
    indicatorWidth: Dp,
    paddingHorizontal: Dp,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.3f),
    color: Color = MaterialTheme.colors.onBackground,
    reverseDirection: Boolean = false,
    position: PositionIndicatorAlignment = PositionIndicatorAlignment.End
) {
    PositionIndicator(
        state = state,
        indicatorHeight = indicatorHeight,
        indicatorWidth = indicatorWidth,
        paddingHorizontal = paddingHorizontal,
        modifier = modifier,
        background = background,
        color = color,
        reverseDirection = reverseDirection,
        position = position
    )
}

/** Contains the default values used for [PositionIndicator]. */
public object PositionIndicatorDefaults {
    /**
     * [AnimationSpec] used for position animation. To disable this animation, pass [snap]
     * AnimationSpec instead
     */
    val positionAnimationSpec: AnimationSpec<Float> =
        tween(durationMillis = 500, easing = CubicBezierEasing(0f, 0f, 0f, 1f))

    /**
     * [AnimationSpec] used for visibility (fade-in and fade-out) animations. To disable this
     * animation, pass [snap] AnimationSpec instead
     */
    val visibilityAnimationSpec: AnimationSpec<Float> =
        spring(stiffness = Spring.StiffnessMediumLow)

    /** Horizontal padding from the PositionIndicator to the screen edge. */
    internal val horizontalPadding = 2.dp
}

internal fun handleFadeOut(
    fadeOutAnimationSpec: AnimationSpec<Float>,
    animateAlphaChannel: Channel<Float>,
    alphaValue: MutableFloatState
) {
    // Sending 0f to the channel, or changing alphaValue directly here
    if (fadeOutAnimationSpec is SnapSpec) {
        alphaValue.floatValue = 0f
    } else {
        animateAlphaChannel.trySend(0f)
    }
}

@Immutable
internal class DisplayState(
    val position: Float,
    val size: Float,
    val visibility: PositionIndicatorVisibility
) {
    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + visibility.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DisplayState

        if (position != other.position) return false
        if (size != other.size) return false
        if (visibility != other.visibility) return false

        return true
    }
}

/**
 * An implementation of [PositionIndicatorState] to display a value that is being incremented or
 * decremented with a rolling side button, rotating bezel or a slider e.g. a volume control.
 *
 * @param fraction Value of the indicator in the range 0..1 where 1 represents the maximum value.
 *   E.g. If displaying a volume value from 0..11 then the [fraction] will be volume/11.
 * @VisibleForTesting
 */
internal class FractionPositionIndicatorState(private val fraction: () -> Float) :
    PositionIndicatorState {
    override val positionFraction = 1f // Position indicator always starts at the bottom|end

    override fun sizeFraction(scrollableContainerSizePx: Float) = fraction()

    override fun equals(other: Any?) =
        (other as? FractionPositionIndicatorState)?.fraction?.invoke() == fraction()

    override fun hashCode(): Int = fraction().hashCode()

    override fun visibility(scrollableContainerSizePx: Float) = PositionIndicatorVisibility.Show
}

/**
 * An implementation of [PositionIndicatorState] to display the amount and position of a component
 * implementing the [ScrollState] class such as a [Column] implementing [Modifier.verticalScroll].
 *
 * @param scrollState the [ScrollState] to adapt
 * @VisibleForTesting
 */
internal class ScrollStateAdapter(private val scrollState: ScrollState) : PositionIndicatorState {
    override val positionFraction: Float
        get() {
            return if (scrollState.maxValue == 0) {
                0f
            } else {
                scrollState.value.toFloat() / scrollState.maxValue
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float) =
        if (scrollableContainerSizePx + scrollState.maxValue == 0.0f) {
            1.0f
        } else {
            scrollableContainerSizePx / (scrollableContainerSizePx + scrollState.maxValue)
        }

    override fun visibility(scrollableContainerSizePx: Float) =
        if (scrollState.maxValue == 0) {
            PositionIndicatorVisibility.Hide
        } else if (scrollState.isScrollInProgress) PositionIndicatorVisibility.Show
        else PositionIndicatorVisibility.AutoHide

    override fun equals(other: Any?): Boolean {
        return (other as? ScrollStateAdapter)?.scrollState == scrollState
    }

    override fun hashCode(): Int {
        return scrollState.hashCode()
    }
}

/**
 * An implementation of [PositionIndicatorState] to display the amount and position of a
 * [ScalingLazyColumn] component via its [ScalingLazyListState].
 *
 * Note that size and position calculations ignore spacing between list items both for determining
 * the number and the number of visible items.
 *
 * @param state the [ScalingLazyListState] to adapt.
 * @VisibleForTesting
 */
internal class ScalingLazyColumnStateAdapter(private val state: ScalingLazyListState) :
    PositionIndicatorState {

    override val positionFraction: Float
        get() {
            val layoutInfo: ScalingLazyListLayoutInfo = state.layoutInfo
            return if (layoutInfo.visibleItemsInfo.isEmpty()) {
                0.0f
            } else {
                val decimalFirstItemIndex = decimalFirstItemIndex(layoutInfo)
                val decimalLastItemIndex = decimalLastItemIndex(layoutInfo)
                val decimalLastItemIndexDistanceFromEnd =
                    layoutInfo.totalItemsCount - decimalLastItemIndex

                if (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd == 0.0f) {
                    0.0f
                } else {
                    decimalFirstItemIndex /
                        (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd)
                }
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float): Float {
        val layoutInfo: ScalingLazyListLayoutInfo = state.layoutInfo
        return if (layoutInfo.totalItemsCount == 0) {
            1.0f
        } else {
            val decimalFirstItemIndex = decimalFirstItemIndex(layoutInfo)
            val decimalLastItemIndex = decimalLastItemIndex(layoutInfo)

            (decimalLastItemIndex - decimalFirstItemIndex) / layoutInfo.totalItemsCount.toFloat()
        }
    }

    override fun visibility(scrollableContainerSizePx: Float): PositionIndicatorVisibility {
        val layoutInfo: ScalingLazyListLayoutInfo = state.layoutInfo
        val canScroll = layoutInfo.visibleItemsInfo.isNotEmpty() && canScrollBackwardsOrForwards()
        return if (canScroll) {
            if (state.isScrollInProgress) PositionIndicatorVisibility.Show
            else PositionIndicatorVisibility.AutoHide
        } else {
            PositionIndicatorVisibility.Hide
        }
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? ScalingLazyColumnStateAdapter)?.state == state
    }

    /**
     * Provide a float value that represents the index of the last visible list item in a scaling
     * lazy column. The value should be in the range from [n,n+1] for a given index n, where n is
     * the index of the last visible item and a value of n represents that only the very start|top
     * of the item is visible, and n+1 means that whole of the item is visible in the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    private fun decimalLastItemIndex(layoutInfo: ScalingLazyListLayoutInfo): Float {
        if (layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val lastItem = layoutInfo.visibleItemsInfo.last()
        // This is the offset of the last item w.r.t. the ScalingLazyColumn coordinate system where
        // 0 in the center of the visible viewport and +/-(state.viewportHeightPx / 2f) are the
        // start and end of the viewport.
        //
        // Note that [ScalingLazyListAnchorType] determines how the list items are anchored to the
        // center of the viewport, it does not change viewport coordinates. As a result this
        // calculation needs to take the anchorType into account to calculate the correct end
        // of list item offset.
        val lastItemEndOffset = lastItem.startOffset(layoutInfo.anchorType) + lastItem.size
        val viewportEndOffset = layoutInfo.viewportSize.height / 2f
        // Coerce item size to at least 1 to avoid divide by zero for zero height items
        val lastItemVisibleFraction =
            (1f - ((lastItemEndOffset - viewportEndOffset) / lastItem.size.coerceAtLeast(1)))
                .coerceAtMost(1f)

        return lastItem.index.toFloat() + lastItemVisibleFraction
    }

    /**
     * Provide a float value that represents the index of first visible list item in a scaling lazy
     * column. The value should be in the range from [n,n+1] for a given index n, where n is the
     * index of the first visible item and a value of n represents that all of the item is visible
     * in the viewport and a value of n+1 means that only the very end|bottom of the list item is
     * visible at the start|top of the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    private fun decimalFirstItemIndex(layoutInfo: ScalingLazyListLayoutInfo): Float {
        if (layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val firstItem = layoutInfo.visibleItemsInfo.first()
        val firstItemStartOffset = firstItem.startOffset(layoutInfo.anchorType)
        val viewportStartOffset = -(layoutInfo.viewportSize.height / 2f)
        // Coerce item size to at least 1 to avoid divide by zero for zero height items
        val firstItemInvisibleFraction =
            ((viewportStartOffset - firstItemStartOffset) / firstItem.size.coerceAtLeast(1))
                .coerceAtLeast(0f)

        return firstItem.index.toFloat() + firstItemInvisibleFraction
    }

    private fun canScrollBackwardsOrForwards(): Boolean =
        state.canScrollBackward || state.canScrollForward
}

/**
 * An implementation of [PositionIndicatorState] to display the amount and position of a
 * [ScalingLazyColumn] component via its [ScalingLazyListState].
 *
 * Note that size and position calculations ignore spacing between list items both for determining
 * the number and the number of visible items.
 *
 * @param state the [ScalingLazyListState] to adapt.
 */
@Deprecated("Use [ScalingLazyColumnStateAdapter] instead")
internal class MaterialScalingLazyColumnStateAdapter(
    @Suppress("DEPRECATION") private val state: androidx.wear.compose.material.ScalingLazyListState
) : PositionIndicatorState {

    override val positionFraction: Float
        get() {
            val layoutInfo = state.layoutInfo

            return if (layoutInfo.visibleItemsInfo.isEmpty()) {
                0.0f
            } else {
                val decimalFirstItemIndex = decimalFirstItemIndex(layoutInfo)
                val decimalLastItemIndex = decimalLastItemIndex(layoutInfo)
                val decimalLastItemIndexDistanceFromEnd =
                    layoutInfo.totalItemsCount - decimalLastItemIndex

                if (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd == 0.0f) {
                    0.0f
                } else {
                    decimalFirstItemIndex /
                        (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd)
                }
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float): Float {
        val layoutInfo = state.layoutInfo
        return if (layoutInfo.totalItemsCount == 0) {
            1.0f
        } else {
            val decimalFirstItemIndex = decimalFirstItemIndex(layoutInfo)
            val decimalLastItemIndex = decimalLastItemIndex(layoutInfo)

            (decimalLastItemIndex - decimalFirstItemIndex) / layoutInfo.totalItemsCount.toFloat()
        }
    }

    override fun visibility(scrollableContainerSizePx: Float): PositionIndicatorVisibility {
        val layoutInfo = state.layoutInfo
        val canScroll = layoutInfo.visibleItemsInfo.isNotEmpty() && canScrollBackwardsOrForwards()
        return if (canScroll) {
            if (state.isScrollInProgress) PositionIndicatorVisibility.Show
            else PositionIndicatorVisibility.AutoHide
        } else {
            PositionIndicatorVisibility.Hide
        }
    }

    private fun canScrollBackwardsOrForwards(): Boolean =
        state.canScrollBackward || state.canScrollForward

    override fun hashCode(): Int {
        return state.hashCode()
    }

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean {
        return (other as? MaterialScalingLazyColumnStateAdapter)?.state == state
    }

    /**
     * Provide a float value that represents the index of the last visible list item in a scaling
     * lazy column. The value should be in the range from [n,n+1] for a given index n, where n is
     * the index of the last visible item and a value of n represents that only the very start|top
     * of the item is visible, and n+1 means that whole of the item is visible in the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    @Suppress("DEPRECATION")
    private fun decimalLastItemIndex(
        layoutInfo: androidx.wear.compose.material.ScalingLazyListLayoutInfo
    ): Float {
        if (layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val lastItem = layoutInfo.visibleItemsInfo.last()
        // This is the offset of the last item w.r.t. the ScalingLazyColumn coordinate system where
        // 0 in the center of the visible viewport and +/-(state.viewportHeightPx / 2f) are the
        // start and end of the viewport.
        //
        // Note that [ScalingLazyListAnchorType] determines how the list items are anchored to the
        // center of the viewport, it does not change viewport coordinates. As a result this
        // calculation needs to take the anchorType into account to calculate the correct end
        // of list item offset.
        val lastItemEndOffset = lastItem.startOffset(state.anchorType.value!!) + lastItem.size
        val viewportEndOffset = state.viewportHeightPx.value!! / 2f
        // Coerce item size to at least 1 to avoid divide by zero for zero height items
        val lastItemVisibleFraction =
            (1f - ((lastItemEndOffset - viewportEndOffset) / lastItem.size.coerceAtLeast(1)))
                .coerceAtMost(1f)

        return lastItem.index.toFloat() + lastItemVisibleFraction
    }

    /**
     * Provide a float value that represents the index of first visible list item in a scaling lazy
     * column. The value should be in the range from [n,n+1] for a given index n, where n is the
     * index of the first visible item and a value of n represents that all of the item is visible
     * in the viewport and a value of n+1 means that only the very end|bottom of the list item is
     * visible at the start|top of the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    @Suppress("DEPRECATION")
    private fun decimalFirstItemIndex(
        layoutInfo: androidx.wear.compose.material.ScalingLazyListLayoutInfo
    ): Float {
        if (layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val firstItem = layoutInfo.visibleItemsInfo.first()
        val firstItemStartOffset = firstItem.startOffset(state.anchorType.value!!)
        val viewportStartOffset = -(state.viewportHeightPx.value!! / 2f)
        // Coerce item size to at least 1 to avoid divide by zero for zero height items
        val firstItemInvisibleFraction =
            ((viewportStartOffset - firstItemStartOffset) / firstItem.size.coerceAtLeast(1))
                .coerceAtLeast(0f)

        return firstItem.index.toFloat() + firstItemInvisibleFraction
    }
}

/**
 * An implementation of [PositionIndicatorState] to display the amount and position of a
 * [LazyColumn] component via its [LazyListState].
 *
 * @param state the [LazyListState] to adapt.
 * @VisibleForTesting
 */
internal class LazyColumnStateAdapter(private val state: LazyListState) : PositionIndicatorState {
    override val positionFraction: Float
        get() {
            return if (state.layoutInfo.visibleItemsInfo.isEmpty()) {
                0.0f
            } else {
                val decimalFirstItemIndex = decimalFirstItemIndex()
                val decimalLastItemIndex = decimalLastItemIndex()

                val decimalLastItemIndexDistanceFromEnd =
                    state.layoutInfo.totalItemsCount - decimalLastItemIndex

                if (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd == 0.0f) {
                    0.0f
                } else {
                    decimalFirstItemIndex /
                        (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd)
                }
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float) =
        if (state.layoutInfo.totalItemsCount == 0) {
            1.0f
        } else {
            val decimalFirstItemIndex = decimalFirstItemIndex()
            val decimalLastItemIndex = decimalLastItemIndex()

            (decimalLastItemIndex - decimalFirstItemIndex) /
                state.layoutInfo.totalItemsCount.toFloat()
        }

    override fun visibility(scrollableContainerSizePx: Float): PositionIndicatorVisibility {
        return if (sizeFraction(scrollableContainerSizePx) < 0.999f) {
            if (state.isScrollInProgress) PositionIndicatorVisibility.Show
            else PositionIndicatorVisibility.AutoHide
        } else {
            PositionIndicatorVisibility.Hide
        }
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? LazyColumnStateAdapter)?.state == state
    }

    private fun decimalLastItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        // Coerce item sizes to at least 1 to avoid divide by zero for zero height items
        val lastItemVisibleSize =
            (state.layoutInfo.viewportEndOffset - lastItem.offset)
                .coerceAtMost(lastItem.size)
                .coerceAtLeast(1)
        return lastItem.index.toFloat() +
            lastItemVisibleSize.toFloat() / lastItem.size.coerceAtLeast(1).toFloat()
    }

    private fun decimalFirstItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val firstItem = state.layoutInfo.visibleItemsInfo.first()
        val firstItemOffset = firstItem.offset - state.layoutInfo.viewportStartOffset
        // Coerce item size to at least 1 to avoid divide by zero for zero height items
        return firstItem.index.toFloat() -
            firstItemOffset.coerceAtMost(0).toFloat() / firstItem.size.coerceAtLeast(1).toFloat()
    }
}

// TODO(ssancho): implement min/max thumb size (1/10 & 9/10)
private fun ContentDrawScope.drawCurvedIndicator(
    color: Color,
    background: Color,
    paddingHorizontalPx: Float,
    indicatorOnTheRight: Boolean,
    sweepDegrees: Float,
    indicatorWidthPx: Float,
    indicatorStart: Float,
    indicatorSize: Float,
    highlightAlpha: Float
) {
    val diameter = max(size.width, size.height)
    val arcSize =
        Size(
            diameter - 2 * paddingHorizontalPx - indicatorWidthPx,
            diameter - 2 * paddingHorizontalPx - indicatorWidthPx
        )
    val arcTopLeft =
        Offset(
            size.width - diameter + paddingHorizontalPx + indicatorWidthPx / 2f,
            (size.height - diameter) / 2f + paddingHorizontalPx + indicatorWidthPx / 2f,
        )
    val startAngleOffset = if (indicatorOnTheRight) 0f else 180f
    drawArc(
        background,
        startAngle = startAngleOffset - sweepDegrees / 2,
        sweepDegrees,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = indicatorWidthPx, cap = StrokeCap.Round)
    )
    drawArc(
        lerp(color, Color.White, highlightAlpha),
        startAngle = startAngleOffset + sweepDegrees * (-0.5f + indicatorStart),
        sweepAngle = sweepDegrees * indicatorSize,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = indicatorWidthPx, cap = StrokeCap.Round)
    )
}

private fun ContentDrawScope.drawStraightIndicator(
    color: Color,
    background: Color,
    paddingHorizontalPx: Float,
    indicatorOnTheRight: Boolean,
    indicatorWidthPx: Float,
    indicatorHeightPx: Float,
    indicatorStart: Float,
    indicatorSize: Float,
    highlightAlpha: Float
) {
    val x =
        if (indicatorOnTheRight) {
            size.width - paddingHorizontalPx - indicatorWidthPx / 2
        } else {
            paddingHorizontalPx + indicatorWidthPx / 2
        }
    val lineTop = Offset(x, (size.height - indicatorHeightPx) / 2f)
    val lineBottom = lineTop + Offset(0f, indicatorHeightPx)
    drawLine(
        color = background,
        lineTop,
        lineBottom,
        strokeWidth = indicatorWidthPx,
        cap = StrokeCap.Round
    )
    drawLine(
        lerp(color, Color.White, highlightAlpha),
        lerp(lineTop, lineBottom, indicatorStart),
        lerp(lineTop, lineBottom, indicatorStart + indicatorSize),
        strokeWidth = indicatorWidthPx,
        cap = StrokeCap.Round
    )
}

internal fun Float.toDegrees() = this * 180f / PI.toFloat()

private fun sqr(x: Float) = x * x

/** Find the start offset of the list item w.r.t. the */
internal fun ScalingLazyListItemInfo.startOffset(anchorType: ScalingLazyListAnchorType) =
    offset -
        if (anchorType == ScalingLazyListAnchorType.ItemCenter) {
            (size / 2f)
        } else {
            0f
        }
