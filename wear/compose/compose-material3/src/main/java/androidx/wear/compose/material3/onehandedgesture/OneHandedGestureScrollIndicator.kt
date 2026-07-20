/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IndicatorImpl
import androidx.wear.compose.material3.IndicatorState
import androidx.wear.compose.material3.OffsetOverscrollEffect
import androidx.wear.compose.material3.R
import androidx.wear.compose.material3.ScalingLazyColumnStateAdapter
import androidx.wear.compose.material3.ScrollIndicatorColors
import androidx.wear.compose.material3.ScrollIndicatorDefaults
import androidx.wear.compose.material3.TransformingLazyColumnStateAdapter
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A scroll indicator that transitions to indicate that a scroll gesture is available to the user.
 *
 * This component functions as a standard scroll indicator, reflecting the scroll position of a
 * [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]. It also observes the
 * [OneHandedGestureScrollIndicatorState] to manage the visual transition into a gesture indicator.
 * When [OneHandedGestureScrollIndicatorState.showIndicator] is called, the indicator temporarily
 * replaces its standard visual state with a gesture animation sequence.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample
 * @param gestureConfiguration the specification for the one-handed gesture
 * @param indicatorState The state object used to synchronize the indicator visibility.
 * @param scrollState The state object of the
 *   [androidx.wear.compose.foundation.lazy.TransformingLazyColumn] this indicator is coupled with.
 * @param modifier The [Modifier] to be applied to the scroll indicator.
 * @param scrollIndicatorColors [ScrollIndicatorColors] that will be used to resolve the indicator
 *   and track colors for this [androidx.wear.compose.material3.ScrollIndicator].
 * @param gestureIndicatorTint The color which will be used for a tint of the gesture animation
 *   icon.
 * @param gestureIndicatorBackgroundColor The color which will be used for a background behind the
 *   gesture animation.
 * @param reverseDirection Reverses direction of ScrollIndicator if true.
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes to the scroll size and position. To disable this animation
 *   [androidx.compose.animation.core.snap] AnimationSpec should be passed instead.
 *
 * See also the
 * [UI Design Guides for One-Handed Gestures](https://developer.android.com/design/ui/wear/guides/patterns/gestures)
 */
@Composable
public fun OneHandedGestureScrollIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGestureScrollIndicatorState,
    scrollState: TransformingLazyColumnState,
    modifier: Modifier = Modifier,
    scrollIndicatorColors: ScrollIndicatorColors = ScrollIndicatorDefaults.colors(),
    gestureIndicatorTint: Color = OneHandedGestureDefaults.scrollIndicatorTint,
    gestureIndicatorBackgroundColor: Color =
        OneHandedGestureDefaults.scrollIndicatorBackgroundColor,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec,
) {
    val overscrollEffect = rememberOverscrollEffect()?.let { it as? OffsetOverscrollEffect }
    val reduceMotionEnabled = LocalReduceMotion.current
    val scrollIndicatorState = remember {
        TransformingLazyColumnStateAdapter(
            state = scrollState,
            overscrollEffect = overscrollEffect,
            reduceMotionEnabled = reduceMotionEnabled,
        )
    }

    GestureScrollIndicator(
        gestureConfiguration,
        indicatorState,
        scrollState,
        scrollIndicatorState,
        modifier,
        gestureIndicatorTint,
        gestureIndicatorBackgroundColor,
        reverseDirection,
        positionAnimationSpec,
        scrollIndicatorColors,
    )
}

/**
 * A scroll indicator that transitions to indicate that a scroll gesture is available to the user.
 *
 * This component functions as a standard scroll indicator, reflecting the scroll position of a
 * [androidx.wear.compose.foundation.lazy.ScalingLazyColumn]. It also observes the
 * [OneHandedGestureScrollIndicatorState] to manage the visual transition into a gesture indicator.
 * When [OneHandedGestureScrollIndicatorState.showIndicator] is called, the indicator temporarily
 * replaces its standard visual state with a gesture animation sequence.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.lazy.ScalingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureScalingLazyColumnSample
 * @param gestureConfiguration the specification for the one-handed gesture
 * @param indicatorState The state object used to synchronize the indicator visibility.
 * @param scrollState The state object of the
 *   [androidx.wear.compose.foundation.lazy.ScalingLazyColumn] this indicator is coupled with.
 * @param modifier The [Modifier] to be applied to the scroll indicator.
 * @param scrollIndicatorColors [ScrollIndicatorColors] that will be used to resolve the indicator
 *   and track colors for this [androidx.wear.compose.material3.ScrollIndicator].
 * @param gestureIndicatorTint The color which will be used for a tint of the gesture animation
 *   icon.
 * @param gestureIndicatorBackgroundColor The color which will be used for a background behind the
 *   gesture animation.
 * @param reverseDirection Reverses direction of ScrollIndicator if true.
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes to the scroll size and position. To disable this animation
 *   [androidx.compose.animation.core.snap] AnimationSpec should be passed instead.
 *
 * See also the
 * [UI Design Guides for One-Handed Gestures](https://developer.android.com/design/ui/wear/guides/patterns/gestures)
 */
@Composable
public fun OneHandedGestureScrollIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGestureScrollIndicatorState,
    scrollState: ScalingLazyListState,
    modifier: Modifier = Modifier,
    scrollIndicatorColors: ScrollIndicatorColors = ScrollIndicatorDefaults.colors(),
    gestureIndicatorTint: Color = OneHandedGestureDefaults.scrollIndicatorTint,
    gestureIndicatorBackgroundColor: Color =
        OneHandedGestureDefaults.scrollIndicatorBackgroundColor,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec,
) {
    val overscrollEffect = rememberOverscrollEffect()?.let { it as? OffsetOverscrollEffect }
    val reduceMotionEnabled = LocalReduceMotion.current
    val scrollIndicatorState = remember {
        ScalingLazyColumnStateAdapter(
            state = scrollState,
            overscrollEffect = overscrollEffect,
            reduceMotionEnabled = reduceMotionEnabled,
        )
    }

    GestureScrollIndicator(
        gestureConfiguration,
        indicatorState,
        scrollState,
        scrollIndicatorState,
        modifier,
        gestureIndicatorTint,
        gestureIndicatorBackgroundColor,
        reverseDirection,
        positionAnimationSpec,
        scrollIndicatorColors,
    )
}

/**
 * State object for [OneHandedGestureScrollIndicator] used to coordinate visual feedback between a
 * [oneHandedGesture] modifier and gesture indicators for ScrollIndicator.
 *
 * Developers should call [showIndicator] within the `onGestureAvailable` callback provided by
 * [oneHandedGesture] modifier to signal that an indication event has occurred. The associated
 * indicator provides an animation block to this state to initiate its animation.
 *
 * **Note:** It is not recommended to show multiple gesture indicators for the same
 * [OneHandedGestureConfiguration] and an error will be thrown if gesture indicators of more than
 * one type are associated with the same [OneHandedGestureConfiguration].
 */
@Stable
public class OneHandedGestureScrollIndicatorState @RememberInComposition constructor() {
    /**
     * Initiate showing the `OneHandedGestureScrollIndicator` associated with this
     * `OneHandedGestureScrollIndicatorState`
     */
    public suspend fun showIndicator() {
        mutex.withLock {
            // As long as this indicator has registered with the gesture manager, we can perform the
            // animation
            val gestureIndicator =
                gestureManager.getRegisteredGestureIndicator(gestureConfiguration)
            gestureIndicator?.let { coroutineScope { performAnimation(it) } }
        }
    }

    internal suspend fun CoroutineScope.performAnimation(
        gestureIndicator: RegisteredIndicator
    ): Unit {
        try {
            // Ensure scrollbar is shown while the gesture indicator animation is on
            launch { scrollableState.animateScrollBy(0.1f) }

            // Animate indicator visibility in
            launch { avdAnimationScale.animateTo(1f, EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT) }
            launch { colorProgress.animateTo(1f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT) }
            delay(INDICATOR_ANIMATION_START_DELAY_MILLIS.milliseconds)

            // Play indicator animation
            avdActive = true // Start the AVD
            val jiggleAmount = 0.5f

            // delay before kicking off the downward scrollbar jiggle
            delay(SCROLLBAR_DOWNWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS.milliseconds)

            // kick off the downward scrollbar jiggle
            launch {
                jiggleFractionAnimatable.animateTo(
                    jiggleAmount,
                    animationSpec = EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT,
                ) {
                    scrollIndicatorState.jiggleAmount = value
                }
            }

            // delay before kicking off the upward scrollbar jiggle
            delay(SCROLLBAR_UPWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS.milliseconds)
            launch {
                jiggleFractionAnimatable.animateTo(
                    0f,
                    animationSpec = EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT,
                ) {
                    scrollIndicatorState.jiggleAmount = value
                }
            }

            delay(
                max(
                        0,
                        (gestureIndicator.duration.inWholeMilliseconds +
                            POST_INDICATOR_ANIMATION_DELAY_MILLIS -
                            SCROLLBAR_DOWNWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS -
                            SCROLLBAR_UPWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS),
                    )
                    .milliseconds
            ) // Wait for AVD duration

            // Animate indicator visibility out
            val finalScaleAnimationJob = launch {
                avdAnimationScale.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT)
            }

            val indicatorColorResetJob = launch {
                colorProgress.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT)
            }

            finalScaleAnimationJob.join()
            indicatorColorResetJob.join()

            // Confirm that the indicator was shown, which will update the
            // tally for frequency checking.
            gestureManager.notifyIndicatorShown(gestureConfiguration)
        } finally {
            scrollIndicatorState.jiggleAmount = 0f
            avdActive = false

            withContext(NonCancellable) {
                avdAnimationScale.snapTo(0f)

                colorProgress.snapTo(0f)

                jiggleFractionAnimatable.snapTo(0f)
            }
        }
    }

    internal lateinit var gestureManager: GestureManager
    internal lateinit var gestureConfiguration: OneHandedGestureConfiguration
    private val mutex = Mutex()

    internal lateinit var scrollableState: ScrollableState
    internal lateinit var scrollIndicatorState: IndicatorState
    internal var indicatorJiggleColor: Color = Color.Unspecified

    // Animatables
    internal var colorProgress = Animatable(0f)
    internal var avdActive by mutableStateOf(false)
    internal val avdAnimationScale = Animatable(0f)
    internal val jiggleFractionAnimatable = Animatable(0f)
}

@Composable
private fun GestureScrollIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    state: OneHandedGestureScrollIndicatorState,
    scrollableState: ScrollableState,
    scrollIndicatorState: IndicatorState,
    modifier: Modifier = Modifier,
    gestureIndicatorTint: Color,
    gestureIndicatorBackgroundColor: Color,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float>,
    scrollIndicatorColors: ScrollIndicatorColors,
) {
    val gestureManager = LocalGestureManager.current
    val avd = gestureConfiguration.action.animatedImageVector()
    val duration = avd.totalDuration.milliseconds

    // Gesture manager needs to know whether this indicator draws outside the boundary of its UI
    // element as that affects the frequency with which it is shown.
    // TODO - SHOULD BE isOverlay = TRUE
    gestureManager.registerGestureIndicator(gestureConfiguration, isFloating = false, duration)

    // Initialise internal State variables
    state.gestureConfiguration = gestureConfiguration
    state.gestureManager = gestureManager
    state.scrollableState = scrollableState
    state.scrollIndicatorState = scrollIndicatorState
    state.indicatorJiggleColor = gestureIndicatorBackgroundColor.copy(alpha = 0.8f)

    val isRtl = (LocalLayoutDirection.current == LayoutDirection.Rtl)
    val density = LocalDensity.current
    val backgroundPainter =
        painterResource(R.drawable.wear_one_handed_gesture_indicator_pointer_background)
    val backgroundSize =
        remember(density) {
            with(density) {
                DpSize(
                    backgroundPainter.intrinsicSize.width.toDp(),
                    backgroundPainter.intrinsicSize.height.toDp(),
                )
            }
        }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier.graphicsLayer {
                        scaleX = state.avdAnimationScale.value
                        scaleY = state.avdAnimationScale.value
                        transformOrigin =
                            TransformOrigin(
                                pivotFractionX = if (isRtl) 0f else 1f,
                                pivotFractionY = 0.5f,
                            )
                    }
                    .size(backgroundSize),
            contentAlignment = Alignment.CenterStart,
        ) {
            val painter =
                rememberAnimatedVectorPainter(animatedImageVector = avd, atEnd = state.avdActive)
            val avdSize =
                remember(painter, density) {
                    with(density) {
                        DpSize(
                            painter.intrinsicSize.width.toDp(),
                            painter.intrinsicSize.height.toDp(),
                        )
                    }
                }
            Icon(
                painter = backgroundPainter,
                contentDescription = null,
                tint = gestureIndicatorBackgroundColor,
                modifier = Modifier.graphicsLayer { scaleX = if (isRtl) -1f else 1f },
            )

            Box(
                modifier = Modifier.size(backgroundSize.height),
                contentAlignment = Alignment.Center,
            ) {
                GestureIndicatorImage(
                    painter = painter,
                    size = avdSize,
                    tint = gestureIndicatorTint,
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))

        val actualIndicatorColor =
            androidx.compose.ui.graphics.lerp(
                scrollIndicatorColors.indicatorColor,
                state.indicatorJiggleColor,
                state.colorProgress.value,
            )

        IndicatorImpl(
            state = scrollIndicatorState,
            indicatorHeight = ScrollIndicatorDefaults.indicatorHeight,
            indicatorWidth = ScrollIndicatorDefaults.indicatorWidth,
            paddingHorizontal = ScrollIndicatorDefaults.edgePadding,
            modifier = modifier,
            background = scrollIndicatorColors.trackColor,
            color = actualIndicatorColor,
            reverseDirection = reverseDirection,
            positionAnimationSpec = positionAnimationSpec,
        )
    }
}

private const val SCROLLBAR_DOWNWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS = 150L
private const val SCROLLBAR_UPWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS = 400L
