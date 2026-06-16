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
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.MaterialTheme
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A wrapper that replaces the [content] to indicate to the user that a gesture action is available.
 *
 * This component listens to the provided [interactionSource] to handle the visual transition
 * between the standard [content] and a gesture indicator. When a relevant gesture interaction is
 * received, the [content] is swapped out for the visual indicator. Once the indicator animation
 * sequence completes, the component automatically restores the original [content].
 *
 * Sample demonstrating a gesture indicator applied to a [androidx.wear.compose.material3.Button]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureButtonSample
 * @param interactionSource The [InteractionSource] stream to observe for incoming gesture
 *   indications. This is used to determine when and which gesture indicator should be displayed.
 * @param modifier The [Modifier] to be applied to the [OneHandedGestureIndicator] layout.
 * @param gestureIndicatorSize The size constraints for the gesture indicator icon, defaulting to
 *   [GestureIndicatorSize.Medium].
 * @param gestureIndicatorTint The color which will be used for a tint of the gesture animation
 * @param content The original component content (e.g., Text or Icon) to be displayed when no
 *   indicator is active.
 */
@Composable
public fun OneHandedGestureIndicator(
    interactionSource: InteractionSource,
    modifier: Modifier = Modifier,
    gestureIndicatorSize: GestureIndicatorSize = GestureIndicatorSize.Medium,
    gestureIndicatorTint: Color = LocalContentColor.current,
    content: @Composable BoxScope.() -> Unit,
) {
    val contentAlpha = remember { Animatable(1f) }
    var activeInteraction by remember {
        mutableStateOf<OneHandedGestureInteraction.Indicate?>(null)
    }
    var avdActive by remember { mutableStateOf(false) }
    val avdAnimationScale = remember { Animatable(0f) }
    var avdDuration by remember { mutableIntStateOf(-1) }
    Layout(
        content = {
            Box(modifier = Modifier.layoutId("icon"), contentAlignment = Alignment.Center) {
                activeInteraction?.let { interaction ->
                    val avd = interaction.action.rememberAnimatedImageVector()
                    // Bridge UI-land back to the LaunchedEffect logic
                    LaunchedEffect(avd) { avdDuration = avd.totalDuration }

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
            }
            Box(
                modifier =
                    Modifier.layoutId("content").graphicsLayer { alpha = contentAlpha.value },
                content = content,
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

    GestureInteractionObserver(interactionSource, false) { interaction ->
        activeInteraction = interaction
        // wait for UI-land to load proper AVD and update avdDuration
        val duration =
            withTimeoutOrNull(1_000.milliseconds) { snapshotFlow { avdDuration }.first { it >= 0 } }
        if (duration == null) {
            activeInteraction = null
            return@GestureInteractionObserver
        }

        try {
            // Animate indicator visibility in
            launch { contentAlpha.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT) }
            launch { avdAnimationScale.animateTo(1f, EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT) }
            delay(INDICATOR_ANIMATION_START_DELAY_MILLIS.milliseconds)

            // Play indicator animation
            avdActive = true // Start the AVD
            delay(duration.milliseconds) // Wait for AVD duration
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
            avdDuration = -1
            avdActive = false
            activeInteraction = null

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
 * Under normal conditions, this component behaves like a standard scroll indicator, reflecting the
 * current scroll position of a [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]. It
 * listens to the provided [interactionSource] to handle the visual transition into a gesture
 * indicator. When a relevant gesture interaction is received, the indicator temporarily replaces
 * its standard visual state with a gesture animation sequence. Once the animation completes, it
 * automatically returns to its standard scroll indicator behavior.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.lazy.TransformingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample
 * @param interactionSource The [InteractionSource] stream to observe for incoming gesture
 *   indications. This is used to determine when the gesture animation sequence should be triggered.
 * @param state The state object of the
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
    interactionSource: InteractionSource,
    state: TransformingLazyColumnState,
    modifier: Modifier = Modifier,
    scrollIndicatorColors: ScrollIndicatorColors = ScrollIndicatorDefaults.colors(),
    gestureIndicatorTint: Color = MaterialTheme.colorScheme.onTertiary,
    gestureIndicatorBackgroundColor: Color = MaterialTheme.colorScheme.tertiary,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec,
) {
    val overscrollEffect = rememberOverscrollEffect()?.let { it as? OffsetOverscrollEffect }
    val reduceMotionEnabled = LocalReduceMotion.current
    val indicatorState = remember {
        TransformingLazyColumnStateAdapter(
            state = state,
            overscrollEffect = overscrollEffect,
            reduceMotionEnabled = reduceMotionEnabled,
        )
    }

    GestureScrollIndicator(
        interactionSource,
        state,
        indicatorState,
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
 * Under normal conditions, this component behaves like a standard scroll indicator, reflecting the
 * current scroll position of a [androidx.wear.compose.foundation.lazy.ScalingLazyColumn]. It
 * listens to the provided [interactionSource] to handle the visual transition into a gesture
 * indicator. When a relevant gesture interaction is received, the indicator temporarily replaces
 * its standard visual state with a gesture animation sequence. Once the animation completes, it
 * automatically returns to its standard scroll indicator behavior.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.lazy.ScalingLazyColumn]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureScalingLazyColumnSample
 * @param interactionSource The [InteractionSource] stream to observe for incoming gesture
 *   indications. This is used to determine when the gesture animation sequence should be triggered.
 * @param state The state object of the [androidx.wear.compose.foundation.lazy.ScalingLazyColumn]
 *   this indicator is coupled with.
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
    interactionSource: InteractionSource,
    state: ScalingLazyListState,
    modifier: Modifier = Modifier,
    scrollIndicatorColors: ScrollIndicatorColors = ScrollIndicatorDefaults.colors(),
    gestureIndicatorTint: Color = MaterialTheme.colorScheme.onTertiary,
    gestureIndicatorBackgroundColor: Color = MaterialTheme.colorScheme.tertiary,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec,
) {
    val overscrollEffect = rememberOverscrollEffect()?.let { it as? OffsetOverscrollEffect }
    val reduceMotionEnabled = LocalReduceMotion.current
    val indicatorState = remember {
        ScalingLazyColumnStateAdapter(
            state = state,
            overscrollEffect = overscrollEffect,
            reduceMotionEnabled = reduceMotionEnabled,
        )
    }

    GestureScrollIndicator(
        interactionSource,
        state,
        indicatorState,
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
 * In its idle state, this component functions as a standard page indicator, using dots or bars to
 * represent the [pagerState]. It listens to the provided [interactionSource] to handle the visual
 * transition into a gesture indicator. When a relevant gesture interaction is received, the
 * indicator temporarily replaces its standard visual state with a gesture animation sequence. Once
 * the animation completes, it automatically returns to its standard page indicator behavior.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.pager.HorizontalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureHorizontalPagerSample
 * @param interactionSource The [InteractionSource] stream to observe for incoming gesture
 *   indications. This is used to determine when the gesture animation sequence should be triggered.
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
    interactionSource: InteractionSource,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedColor: Color = PageIndicatorDefaults.selectedColor,
    unselectedColor: Color = PageIndicatorDefaults.unselectedColor,
    backgroundColor: Color = PageIndicatorDefaults.backgroundColor,
    gestureIndicatorTint: Color = MaterialTheme.colorScheme.onTertiary,
    gestureIndicatorBackgroundColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val transform = remember { TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GesturePageIndicator(
            interactionSource = interactionSource,
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
 * In its idle state, this component functions as a standard page indicator, using dots or bars to
 * represent the [pagerState]. It listens to the provided [interactionSource] to handle the visual
 * transition into a gesture indicator. When a relevant gesture interaction is received, the
 * indicator temporarily replaces its standard visual state with a gesture animation sequence. Once
 * the animation completes, it automatically returns to its standard page indicator behavior.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.pager.VerticalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureVerticalPagerSample
 * @param interactionSource The [InteractionSource] stream to observe for incoming gesture
 *   indications. This is used to determine when the gesture animation sequence should be triggered.
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
    interactionSource: InteractionSource,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedColor: Color = PageIndicatorDefaults.selectedColor,
    unselectedColor: Color = PageIndicatorDefaults.unselectedColor,
    backgroundColor: Color = PageIndicatorDefaults.backgroundColor,
    gestureIndicatorTint: Color = MaterialTheme.colorScheme.onTertiary,
    gestureIndicatorBackgroundColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val layoutDirection = LocalLayoutDirection.current

    val isRtl = layoutDirection == LayoutDirection.Rtl

    val transform =
        remember(isRtl) {
            TransformOrigin(pivotFractionX = if (isRtl) 0f else 1f, pivotFractionY = 0.5f)
        }

    Row(verticalAlignment = Alignment.CenterVertically) {
        GesturePageIndicator(
            interactionSource = interactionSource,
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
    interactionSource: InteractionSource,
    backgroundRotation: Float = 0f,
    backgroundScale: Float = 1f,
    transform: TransformOrigin,
    avdAlignment: Alignment,
    gestureIndicatorTint: Color,
    gestureIndicatorBackgroundColor: Color,
) {
    var avdActive by remember { mutableStateOf(false) }
    var activeInteraction by remember {
        mutableStateOf<OneHandedGestureInteraction.Indicate?>(null)
    }
    var avdDuration by remember { mutableIntStateOf(-1) }
    val avdAnimationScale = remember { Animatable(0f) }

    activeInteraction?.let { interaction ->
        val density = LocalDensity.current
        val avd = interaction.action.rememberAnimatedImageVector()
        val avdPainter = rememberAnimatedVectorPainter(animatedImageVector = avd, atEnd = avdActive)
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
        val avdSize =
            remember(density) {
                with(density) {
                    DpSize(
                        avdPainter.intrinsicSize.width.toDp(),
                        avdPainter.intrinsicSize.height.toDp(),
                    )
                }
            }
        val largestBackgroundSide = max(backgroundSize.width, backgroundSize.height)

        // Bridge UI-land back to the LaunchedEffect logic
        LaunchedEffect(avd) { avdDuration = avd.totalDuration }

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
                GestureIndicatorImage(
                    painter = avdPainter,
                    size = avdSize,
                    tint = gestureIndicatorTint,
                )
            }
        }
    }

    GestureInteractionObserver(interactionSource, true) { interaction ->
        activeInteraction = interaction
        // wait for UI-land to load proper AVD and update avdDuration
        val duration =
            withTimeoutOrNull(1_000.milliseconds) { snapshotFlow { avdDuration }.first { it >= 0 } }
        if (duration == null) {
            activeInteraction = null
            return@GestureInteractionObserver
        }
        try {
            // Animate indicator visibility in
            launch { avdAnimationScale.animateTo(1f, EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT) }
            delay(INDICATOR_ANIMATION_START_DELAY_MILLIS.milliseconds)

            // Play indicator animation
            avdActive = true // Start the AVD

            // Wait for AVD duration
            delay((duration + POST_INDICATOR_ANIMATION_DELAY_MILLIS).milliseconds)

            // Animate indicator visibility out
            val finalScaleAnimationJob = launch {
                avdAnimationScale.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT)
            }

            finalScaleAnimationJob.join()
        } finally {
            avdDuration = -1
            avdActive = false
            activeInteraction = null

            withContext(NonCancellable) { avdAnimationScale.snapTo(0f) }
        }
    }
}

@Composable
private fun GestureScrollIndicator(
    interactionSource: InteractionSource,
    scrollableState: ScrollableState,
    indicatorState: IndicatorState,
    modifier: Modifier = Modifier,
    scrollIndicatorColors: ScrollIndicatorColors = ScrollIndicatorDefaults.colors(),
    gestureIndicatorTint: Color = MaterialTheme.colorScheme.onTertiary,
    gestureIndicatorBackgroundColor: Color = MaterialTheme.colorScheme.tertiary,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec,
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

    var activeInteraction by remember {
        mutableStateOf<OneHandedGestureInteraction.Indicate?>(null)
    }
    val jiggleFractionAnimatable = remember { Animatable(0f) }
    val jiggleAmount = 0.5f
    val indicatorJiggleColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
    var avdDuration by remember { mutableIntStateOf(-1) }

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
            activeInteraction?.let { interaction ->
                val avd = interaction.action.rememberAnimatedImageVector()
                // Bridge UI-land back to the LaunchedEffect logic
                LaunchedEffect(avd) { avdDuration = avd.totalDuration }
                val painter =
                    rememberAnimatedVectorPainter(animatedImageVector = avd, atEnd = avdActive)
                val avdSize =
                    remember(density) {
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
        }
        Spacer(modifier = Modifier.width(6.dp))
        IndicatorImpl(
            indicatorState,
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

    GestureInteractionObserver(interactionSource, true) { interaction ->
        activeInteraction = interaction
        // wait for UI-land to load proper AVD and update avdDuration
        val duration =
            withTimeoutOrNull(1_000.milliseconds) { snapshotFlow { avdDuration }.first { it >= 0 } }
        if (duration == null) {
            activeInteraction = null
            return@GestureInteractionObserver
        }
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
                    indicatorState.jiggleAmount = value
                }
            }

            // delay before kicking off the upward scrollbar jiggle
            delay(SCROLLBAR_UPWARD_JIGGLE_ANIMATION_START_DELAY_MILLIS.milliseconds)
            launch {
                jiggleFractionAnimatable.animateTo(
                    0f,
                    animationSpec = EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT,
                ) {
                    indicatorState.jiggleAmount = value
                }
            }

            delay(
                max(
                        0,
                        (duration + POST_INDICATOR_ANIMATION_DELAY_MILLIS) -
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
            indicatorState.jiggleAmount = 0f
            avdDuration = -1
            avdActive = false
            activeInteraction = null

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
private fun GestureInteractionObserver(
    interactionSource: InteractionSource,
    shouldRestrictFrequency: Boolean,
    onIndicate: suspend CoroutineScope.(OneHandedGestureInteraction.Indicate) -> Unit,
) {
    // Use a Mutex to synchronize interaction processing. When the interactionSource changes or new
    // Interaction is emitted, the previous LaunchedEffect is canceled. However, because suspension
    // functions in a withContext(NonCancellable) block can yield execution, a race condition could
    // occur where the old cleanup and new initialization overlap.
    // The Mutex ensures that cleanup must fully complete before the next interaction begins.
    val gestureMutex = remember { Mutex() }
    val gestureManager = LocalGestureManager.current
    LaunchedEffect(interactionSource, gestureManager) {
        interactionSource.interactions
            .filterIsInstance<OneHandedGestureInteraction.Indicate>()
            .filter { interaction ->
                gestureManager.shouldShowGestureIndicator(
                    interaction.action,
                    interaction.key,
                    shouldRestrictFrequency,
                )
            }
            .collectLatest { interaction ->
                gestureMutex.withLock {
                    onIndicate(interaction)
                    gestureManager.notifyIndicatorShown(interaction.action, interaction.key)
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
