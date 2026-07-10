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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IndicatorImpl
import androidx.wear.compose.material3.IndicatorState
import androidx.wear.compose.material3.OffsetOverscrollEffect
import androidx.wear.compose.material3.PageIndicatorDefaults
import androidx.wear.compose.material3.R
import androidx.wear.compose.material3.ScalingLazyColumnStateAdapter
import androidx.wear.compose.material3.ScrollIndicatorColors
import androidx.wear.compose.material3.ScrollIndicatorDefaults
import androidx.wear.compose.material3.TransformingLazyColumnStateAdapter
import androidx.wear.compose.material3.VerticalPageIndicator
import androidx.wear.compose.material3.internal.LocalWristOrientation
import androidx.wear.compose.material3.internal.isLeftWrist
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A wrapper that replaces the [content] to indicate to the user that a gesture action is available.
 *
 * This component manages the visual transition between the [content] and a gesture indicator. It
 * observes the [OneHandedGestureIndicatorState] to manage its visual transition. When
 * [indicatorState] signals that the gesture defined by [gestureConfiguration] should be displayed,
 * the component replaces its [content] with a gesture animation. Once the animation completes, the
 * indicator resets [OneHandedGestureIndicatorState.isIndicatorActive] back to false, restoring the
 * original [content].
 *
 * Sample demonstrating a gesture indicator applied to a [androidx.wear.compose.material3.Button]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureButtonSample
 * @param gestureConfiguration The configuration of the gesture associated with this indicator.
 * @param indicatorState The state object used to synchronize the indicator visibility.
 * @param modifier The [Modifier] to be applied to the [OneHandedGestureIndicator] layout.
 * @param gestureIndicatorSize The size constraints for the gesture indicator icon.
 * @param gestureIndicatorTint The color which will be used for a tint of the gesture animation
 * @param content The original component content (e.g., Text or Icon) to be displayed when no
 *   indicator is active.
 */
@Composable
public fun OneHandedGestureIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGestureIndicatorState,
    modifier: Modifier = Modifier,
    gestureIndicatorSize: GestureIndicatorSize = OneHandedGestureDefaults.indicatorSize,
    gestureIndicatorTint: Color = OneHandedGestureDefaults.indicatorTint,
    content: @Composable () -> Unit,
) {
    val contentAlpha = remember { Animatable(1f) }
    var avdActive by remember { mutableStateOf(false) }
    val avdAnimationScale = remember { Animatable(0f) }
    val avd = gestureConfiguration.action.rememberAnimatedImageVector()

    Layout(
        content = {
            Box(modifier = Modifier.layoutId("icon"), contentAlignment = Alignment.Center) {
                val painter =
                    rememberAnimatedVectorPainter(animatedImageVector = avd, atEnd = avdActive)
                GestureIndicatorImage(
                    painter = painter,
                    size = DpSize(gestureIndicatorSize.size, gestureIndicatorSize.size),
                    tint = gestureIndicatorTint,
                    scaleX = { avdAnimationScale.value },
                    scaleY = { avdAnimationScale.value },
                )
            }
            Box(
                modifier =
                    Modifier.layoutId("content").graphicsLayer { alpha = contentAlpha.value },
                content = { content() },
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val iconMeasurable = measurables.fastFirstOrNull { it.layoutId == "icon" }
        val contentMeasurable = measurables.fastFirst { it.layoutId == "content" }

        val contentPlaceable = contentMeasurable.measure(constraints)
        val iconPlaceable = iconMeasurable?.measure(constraints)

        val width = contentPlaceable.width
        val height = contentPlaceable.height
        layout(width, height) {
            contentPlaceable.placeRelative(0, 0)

            iconPlaceable?.let {
                // Center the icon within the calculated layout width/height
                val xOffset = (width - iconPlaceable.width) / 2
                val yOffset = (height - iconPlaceable.height) / 2
                iconPlaceable.placeRelative(xOffset, yOffset)
            }
        }
    }

    GestureIndicateEventObserver(gestureConfiguration, indicatorState, false) {
        try {
            // Animate indicator visibility in
            launch { contentAlpha.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT) }
            launch { avdAnimationScale.animateTo(1f, EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT) }
            delay(INDICATOR_ANIMATION_START_DELAY_MILLIS.milliseconds)

            // Play indicator animation
            avdActive = true // Start the AVD
            delay(avd.totalDuration.milliseconds) // Wait for AVD duration
            delay(POST_INDICATOR_ANIMATION_DELAY_MILLIS.milliseconds)

            // Animate indicator visibility out
            val finalScaleAnimationJob = launch {
                avdAnimationScale.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT)
            }
            val finalButtonAnimationJob = launch {
                contentAlpha.animateTo(1f, EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT)
            }

            finalScaleAnimationJob.join()
            finalButtonAnimationJob.join()
        } finally {
            avdActive = false

            withContext(NonCancellable) {
                contentAlpha.snapTo(1f)
                avdAnimationScale.snapTo(0f)
            }
        }
    }
}

/**
 * A scroll indicator that transitions to indicate that a scroll gesture is available to the user.
 *
 * This component functions as a standard scroll indicator, reflecting the scroll position of a
 * [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]. It also observes the
 * [OneHandedGestureIndicatorState] to manage the visual transition into a gesture indicator. When
 * the [indicatorState] signals that the gesture defined by [gestureConfiguration] should be
 * displayed, the indicator temporarily replaces its standard visual state with a gesture animation
 * sequence. Once the animation completes, the indicator resets
 * [OneHandedGestureIndicatorState.isIndicatorActive] back to false, and the component returns to
 * its standard scroll indicator behavior.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample
 * @param gestureConfiguration The configuration of the gesture associated with this indicator.
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
 */
@Composable
public fun OneHandedGestureScrollIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGestureIndicatorState,
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
        scrollIndicatorColors,
        gestureIndicatorTint,
        gestureIndicatorBackgroundColor,
        reverseDirection,
        positionAnimationSpec,
    )
}

/**
 * A scroll indicator that transitions to indicate that a scroll gesture is available to the user.
 *
 * This component functions as a standard scroll indicator, reflecting the scroll position of a
 * [androidx.wear.compose.foundation.lazy.ScalingLazyColumn]. It also observes the
 * [OneHandedGestureIndicatorState] to manage the visual transition into a gesture indicator. When
 * the [indicatorState] signals that the gesture defined by [gestureConfiguration] should be
 * displayed, the indicator temporarily replaces its standard visual state with a gesture animation
 * sequence. Once the animation completes, the indicator resets
 * [OneHandedGestureIndicatorState.isIndicatorActive] back to false, and the component returns to
 * its standard scroll indicator behavior.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.lazy.ScalingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureScalingLazyColumnSample
 * @param gestureConfiguration The configuration of the gesture associated with this indicator.
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
 */
@Composable
public fun OneHandedGestureScrollIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGestureIndicatorState,
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
        scrollIndicatorColors,
        gestureIndicatorTint,
        gestureIndicatorBackgroundColor,
        reverseDirection,
        positionAnimationSpec,
    )
}

/**
 * A horizontal page indicator that can temporarily display a gesture indicator to demonstrate how
 * to navigate between pages using one-handed gestures.
 *
 * This component functions as a standard page indicator, using dots or bars to represent the
 * [pagerState]. It observes the [OneHandedGestureIndicatorState] to manage its visual transition
 * into a gesture indicator. When [indicatorState] signals that the gesture defined by
 * [gestureConfiguration] should be displayed, the indicator replaces its standard visual state with
 * a gesture animation. Once the animation completes, the indicator resets
 * [OneHandedGestureIndicatorState.isIndicatorActive] back to false, returning the component to its
 * standard page indicator behavior.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.pager.HorizontalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureHorizontalPagerSample
 * @param gestureConfiguration The configuration of the gesture associated with this indicator.
 * @param indicatorState The state object used to synchronize the indicator visibility.
 * @param pagerState The state of the [androidx.wear.compose.foundation.pager.HorizontalPager] that
 *   this indicator represents.
 * @param modifier Modifier to be applied to the [HorizontalPageIndicator]
 * @param selectedColor The color which will be used for a selected indicator item.
 * @param unselectedColor The color which will be used for an unselected indicator item.
 * @param backgroundColor The color which will be used for an indicator background.
 * @param gestureIndicatorTint The color which will be used for a tint of the gesture animation
 *   icon.
 * @param gestureIndicatorBackgroundColor The color which will be used for a background behind the
 *   gesture animation.
 */
@Composable
public fun OneHandedGestureHorizontalPageIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGestureIndicatorState,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedColor: Color = PageIndicatorDefaults.selectedColor,
    unselectedColor: Color = PageIndicatorDefaults.unselectedColor,
    backgroundColor: Color = PageIndicatorDefaults.backgroundColor,
    gestureIndicatorTint: Color = OneHandedGestureDefaults.pageIndicatorTint,
    gestureIndicatorBackgroundColor: Color = OneHandedGestureDefaults.pageIndicatorBackgroundColor,
) {
    val transform = remember { TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GesturePageIndicator(
            gestureConfiguration = gestureConfiguration,
            indicatorState = indicatorState,
            backgroundRotation = 90f,
            transform = transform,
            avdAlignment = Alignment.TopCenter,
            gestureIndicatorTint = gestureIndicatorTint,
            gestureIndicatorBackgroundColor = gestureIndicatorBackgroundColor,
        )
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalPageIndicator(
            pagerState,
            modifier = modifier,
            selectedColor = selectedColor,
            unselectedColor = unselectedColor,
            backgroundColor = backgroundColor,
        )
    }
}

/**
 * A vertical page indicator that can temporarily display a gesture indicator to demonstrate how to
 * navigate between pages using one-handed gestures.
 *
 * This component functions as a standard page indicator, using dots or bars to represent the
 * [pagerState]. It observes the [OneHandedGestureIndicatorState] to manage its visual transition
 * into a gesture indicator. When [indicatorState] signals that the gesture defined by
 * [gestureConfiguration] should be displayed, the indicator replaces its standard visual state with
 * a gesture animation. Once the animation completes, the indicator resets
 * [OneHandedGestureIndicatorState.isIndicatorActive] back to false, returning the component to its
 * standard page indicator behavior.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.pager.VerticalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureVerticalPagerSample
 * @param gestureConfiguration The configuration of the gesture associated with this indicator.
 * @param indicatorState The state object used to synchronize the indicator visibility.
 * @param pagerState The state of the [androidx.wear.compose.foundation.pager.VerticalPager] that
 *   this indicator represents.
 * @param modifier Modifier to be applied to the [VerticalPageIndicator]
 * @param selectedColor The color which will be used for a selected indicator item.
 * @param unselectedColor The color which will be used for an unselected indicator item.
 * @param backgroundColor The color which will be used for an indicator background.
 * @param gestureIndicatorTint The color which will be used for a tint of the gesture animation
 *   icon.
 * @param gestureIndicatorBackgroundColor The color which will be used for a background behind the
 *   gesture animation.
 */
@Composable
public fun OneHandedGestureVerticalPageIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGestureIndicatorState,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedColor: Color = PageIndicatorDefaults.selectedColor,
    unselectedColor: Color = PageIndicatorDefaults.unselectedColor,
    backgroundColor: Color = PageIndicatorDefaults.backgroundColor,
    gestureIndicatorTint: Color = OneHandedGestureDefaults.pageIndicatorTint,
    gestureIndicatorBackgroundColor: Color = OneHandedGestureDefaults.pageIndicatorBackgroundColor,
) {
    val layoutDirection = LocalLayoutDirection.current

    val isRtl = layoutDirection == LayoutDirection.Rtl

    val transform =
        remember(isRtl) {
            TransformOrigin(pivotFractionX = if (isRtl) 0f else 1f, pivotFractionY = 0.5f)
        }

    Row(verticalAlignment = Alignment.CenterVertically) {
        GesturePageIndicator(
            gestureConfiguration = gestureConfiguration,
            indicatorState = indicatorState,
            backgroundScale = if (isRtl) -1f else 1f,
            transform = transform,
            avdAlignment = Alignment.CenterStart,
            gestureIndicatorTint = gestureIndicatorTint,
            gestureIndicatorBackgroundColor = gestureIndicatorBackgroundColor,
        )
        Spacer(modifier = Modifier.width(6.dp))
        VerticalPageIndicator(
            pagerState,
            modifier = modifier,
            selectedColor = selectedColor,
            unselectedColor = unselectedColor,
            backgroundColor = backgroundColor,
        )
    }
}

@Composable
private fun GesturePageIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGestureIndicatorState,
    backgroundRotation: Float = 0f,
    backgroundScale: Float = 1f,
    transform: TransformOrigin,
    avdAlignment: Alignment,
    gestureIndicatorTint: Color,
    gestureIndicatorBackgroundColor: Color,
) {
    var avdActive by remember { mutableStateOf(false) }
    val avdAnimationScale = remember { Animatable(0f) }
    val density = LocalDensity.current
    val avd = gestureConfiguration.action.rememberAnimatedImageVector()
    val avdPainter = rememberAnimatedVectorPainter(animatedImageVector = avd, atEnd = avdActive)
    val backgroundPainter =
        painterResource(R.drawable.wear_one_handed_gesture_indicator_pointer_background)
    val backgroundSize =
        remember(backgroundPainter, density) {
            with(density) {
                DpSize(
                    backgroundPainter.intrinsicSize.width.toDp(),
                    backgroundPainter.intrinsicSize.height.toDp(),
                )
            }
        }
    val avdSize =
        remember(avdPainter, density) {
            with(density) {
                DpSize(
                    avdPainter.intrinsicSize.width.toDp(),
                    avdPainter.intrinsicSize.height.toDp(),
                )
            }
        }
    val largestBackgroundSide = max(backgroundSize.width, backgroundSize.height)

    Box(
        modifier =
            Modifier.graphicsLayer {
                    scaleX = avdAnimationScale.value
                    scaleY = avdAnimationScale.value
                    transformOrigin = transform
                }
                .size(largestBackgroundSide),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = backgroundPainter,
            contentDescription = null,
            tint = gestureIndicatorBackgroundColor,
            modifier =
                Modifier.graphicsLayer {
                    scaleX = backgroundScale
                    rotationZ = backgroundRotation
                },
        )
        Box(
            modifier = Modifier.size(backgroundSize.height).align(avdAlignment),
            contentAlignment = Alignment.Center,
        ) {
            GestureIndicatorImage(painter = avdPainter, size = avdSize, tint = gestureIndicatorTint)
        }
    }

    GestureIndicateEventObserver(gestureConfiguration, indicatorState, true) {
        try {
            // Animate indicator visibility in
            launch { avdAnimationScale.animateTo(1f, EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT) }
            delay(INDICATOR_ANIMATION_START_DELAY_MILLIS.milliseconds)

            // Play indicator animation
            avdActive = true // Start the AVD

            // Wait for AVD duration
            delay((avd.totalDuration + POST_INDICATOR_ANIMATION_DELAY_MILLIS).milliseconds)

            // Animate indicator visibility out
            val finalScaleAnimationJob = launch {
                avdAnimationScale.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT)
            }

            finalScaleAnimationJob.join()
        } finally {
            avdActive = false

            withContext(NonCancellable) { avdAnimationScale.snapTo(0f) }
        }
    }
}

@Composable
private fun GestureScrollIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    gestureIndicatorState: OneHandedGestureIndicatorState,
    scrollableState: ScrollableState,
    scrollIndicatorState: IndicatorState,
    modifier: Modifier = Modifier,
    scrollIndicatorColors: ScrollIndicatorColors,
    gestureIndicatorTint: Color,
    gestureIndicatorBackgroundColor: Color,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float>,
) {
    var avdActive by remember { mutableStateOf(false) }
    val avdAnimationScale = remember { Animatable(0f) }
    val isRtl = (LocalLayoutDirection.current == LayoutDirection.Rtl)
    val density = LocalDensity.current
    val indicatorDefaultColor = scrollIndicatorColors.indicatorColor
    val indicatorColor = remember { Animatable(indicatorDefaultColor) }
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

    val jiggleFractionAnimatable = remember { Animatable(0f) }
    val jiggleAmount = 0.5f
    val indicatorJiggleColor = gestureIndicatorBackgroundColor.copy(alpha = 0.8f)
    val avd = gestureConfiguration.action.rememberAnimatedImageVector()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier.graphicsLayer {
                        scaleX = avdAnimationScale.value
                        scaleY = avdAnimationScale.value
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
                rememberAnimatedVectorPainter(animatedImageVector = avd, atEnd = avdActive)
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
        IndicatorImpl(
            state = scrollIndicatorState,
            indicatorHeight = ScrollIndicatorDefaults.indicatorHeight,
            indicatorWidth = ScrollIndicatorDefaults.indicatorWidth,
            paddingHorizontal = ScrollIndicatorDefaults.edgePadding,
            modifier = modifier,
            background = scrollIndicatorColors.trackColor,
            color = indicatorColor.value,
            reverseDirection = reverseDirection,
            positionAnimationSpec = positionAnimationSpec,
        )
    }

    GestureIndicateEventObserver(gestureConfiguration, gestureIndicatorState, true) {
        try {
            // Ensure scrollbar is shown while the gesture indicator animation is on
            launch { scrollableState.animateScrollBy(0.1f) }

            // Animate indicator visibility in
            launch { avdAnimationScale.animateTo(1f, EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT) }
            launch {
                indicatorColor.animateTo(
                    indicatorJiggleColor,
                    EXPRESSIVE_DEFAULT_EFFECTS_SPRING_COLOR,
                )
            }
            delay(INDICATOR_ANIMATION_START_DELAY_MILLIS.milliseconds)

            // Play indicator animation
            avdActive = true // Start the AVD

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
                        (avd.totalDuration + POST_INDICATOR_ANIMATION_DELAY_MILLIS) -
                            SCROLLBAR_DOWNWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS -
                            SCROLLBAR_UPWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS,
                    )
                    .milliseconds
            ) // Wait for AVD duration

            // Animate indicator visibility out
            val finalScaleAnimationJob = launch {
                avdAnimationScale.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT)
            }

            val indicatorColorResetJob = launch {
                indicatorColor.animateTo(
                    indicatorDefaultColor,
                    EXPRESSIVE_DEFAULT_EFFECTS_SPRING_COLOR,
                )
            }

            finalScaleAnimationJob.join()
            indicatorColorResetJob.join()
        } finally {
            scrollIndicatorState.jiggleAmount = 0f
            avdActive = false

            withContext(NonCancellable) {
                avdAnimationScale.snapTo(0f)
                indicatorColor.snapTo(indicatorDefaultColor)
                jiggleFractionAnimatable.snapTo(0f)
            }
        }
    }
}

@Composable
private fun GestureIndicatorImage(
    painter: Painter,
    size: DpSize,
    tint: Color,
    scaleX: () -> Float = { 1.0f },
    scaleY: () -> Float = { 1.0f },
) {
    // rememberAnimatedVectorPainter hardcodes autoMirror = true, which reacts to
    // LocalLayoutDirection. To gain manual control over mirroring, we force
    // LayoutDirection.Ltr and apply a horizontal scale based on the wrist orientation.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val wristOrientation = LocalWristOrientation.current
        Image(
            painter = painter,
            contentDescription = null,
            modifier =
                Modifier.size(size).graphicsLayer {
                    // Mirror the image only when worn on the right hand
                    this.scaleX = if (wristOrientation.isLeftWrist()) scaleX() else -scaleX()
                    this.scaleY = scaleY()
                },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

@Composable
private fun GestureAction.rememberAnimatedImageVector(): AnimatedImageVector {
    val resourceId =
        when (this) {
            GestureAction.Primary -> R.drawable.wear_one_handed_gesture_primary_indicator_animation
            else -> R.drawable.wear_one_handed_gesture_dismiss_indicator_animation
        }
    return AnimatedImageVector.animatedVectorResource(resourceId)
}

@Composable
private fun GestureIndicateEventObserver(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGestureIndicatorState,
    shouldRestrictFrequency: Boolean,
    onIndicate: suspend CoroutineScope.() -> Unit,
) {
    // Use a Mutex to synchronize event processing. When the state changes, the previous
    // LaunchedEffect is canceled. However, because suspension functions in onIndicate() have a
    // withContext(NonCancellable) block which can yield execution, a race condition could occur
    // where the old cleanup and new initialization overlap.
    // The Mutex ensures that cleanup must fully complete before the next event begins.
    val gestureMutex = remember { Mutex() }
    val gestureManager = LocalGestureManager.current
    if (indicatorState.isIndicatorActive) {
        LaunchedEffect(gestureManager, gestureConfiguration, shouldRestrictFrequency) {
            if (
                gestureManager.shouldShowGestureIndicator(
                    gestureConfiguration,
                    shouldRestrictFrequency,
                )
            ) {
                gestureMutex.withLock {
                    try {
                        onIndicate()
                        gestureManager.notifyIndicatorShown(gestureConfiguration)
                    } finally {
                        indicatorState.isIndicatorActive = false
                    }
                }
            }
        }
    }
}

private val EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT =
    spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 350f)

private val EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)

private val EXPRESSIVE_DEFAULT_EFFECTS_SPRING_COLOR =
    spring<Color>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)

@VisibleForTesting internal const val INDICATOR_ANIMATION_START_DELAY_MILLIS = 450L
private const val POST_INDICATOR_ANIMATION_DELAY_MILLIS = 200L
private const val SCROLLBAR_DOWNWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS = 150L
private const val SCROLLBAR_UPWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS = 400L
