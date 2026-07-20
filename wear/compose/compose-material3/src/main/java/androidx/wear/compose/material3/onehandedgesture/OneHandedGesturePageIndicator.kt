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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.max
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.PageIndicatorDefaults
import androidx.wear.compose.material3.R
import androidx.wear.compose.material3.VerticalPageIndicator
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
 * A horizontal page indicator that can temporarily display a gesture indicator to demonstrate how
 * to navigate between pages using one-handed gestures.
 *
 * This component functions as a standard page indicator, using dots or bars to represent the
 * [pagerState]. It also observes the [OneHandedGesturePageIndicatorState] to manage the visual
 * transition into a gesture indicator. When [OneHandedGesturePageIndicatorState.showIndicator] is
 * called, the indicator temporarily replaces its standard visual state with a gesture animation
 * sequence.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.pager.HorizontalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureHorizontalPagerSample
 * @param gestureConfiguration the specification for the one-handed gesture
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
 *
 * See also the
 * [UI Design Guides for One-Handed Gestures](https://developer.android.com/design/ui/wear/guides/patterns/gestures)
 */
@Composable
public fun OneHandedGestureHorizontalPageIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGesturePageIndicatorState,
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
            state = indicatorState,
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
 * [pagerState]. It also observes the [OneHandedGesturePageIndicatorState] to manage the visual
 * transition into a gesture indicator. When [OneHandedGesturePageIndicatorState.showIndicator] is
 * called, the indicator temporarily replaces its standard visual state with a gesture animation
 * sequence.
 *
 * Sample demonstrating a gesture indicator applied to a
 * [androidx.wear.compose.foundation.pager.VerticalPager]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureVerticalPagerSample
 * @param gestureConfiguration the specification for the one-handed gesture
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
 *
 * See also the
 * [UI Design Guides for One-Handed Gestures](https://developer.android.com/design/ui/wear/guides/patterns/gestures)
 */
@Composable
public fun OneHandedGestureVerticalPageIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    indicatorState: OneHandedGesturePageIndicatorState,
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
            state = indicatorState,
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

/**
 * State object for [OneHandedGestureHorizontalPageIndicator] and
 * [OneHandedGestureVerticalPageIndicator], used to coordinate visual feedback between a
 * [oneHandedGesture] modifier and a gesture indicators for [HorizontalPageIndicator] and
 * [VerticalPageIndicator].
 *
 * Developers should call [OneHandedGesturePageIndicatorState.showIndicator] within the
 * `onGestureAvailable` callback provided by [oneHandedGesture] modifier to signal that an
 * indication event has occurred. The associated indicator provides an animation block to this state
 * to initiate its animation.
 *
 * **Note:** It is not recommended to show multiple gesture indicators for the same
 * [OneHandedGestureConfiguration] and an error will be thrown if gesture indicators of more than
 * one type are associated with the same [OneHandedGestureConfiguration].
 */
@Stable
public class OneHandedGesturePageIndicatorState @RememberInComposition constructor() {
    /**
     * Initiate showing the gesture indicator associated with this
     * `OneHandedGesturePageIndicatorState`
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
            // Animate indicator visibility in
            launch { avdAnimationScale.animateTo(1f, EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT) }
            delay(INDICATOR_ANIMATION_START_DELAY_MILLIS.milliseconds)

            // Play indicator animation
            avdActive = true // Start the AVD

            // Wait for AVD duration
            delay(
                (gestureIndicator.duration.inWholeMilliseconds +
                        POST_INDICATOR_ANIMATION_DELAY_MILLIS)
                    .milliseconds
            )

            // Animate indicator visibility out
            val finalScaleAnimationJob = launch {
                avdAnimationScale.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT)
            }

            finalScaleAnimationJob.join()

            // Confirm that the indicator was shown, which will update the
            // tally for frequency checking.
            gestureManager.notifyIndicatorShown(gestureConfiguration)
        } finally {
            avdActive = false

            withContext(NonCancellable) { avdAnimationScale.snapTo(0f) }
        }
    }

    internal lateinit var gestureManager: GestureManager
    internal lateinit var gestureConfiguration: OneHandedGestureConfiguration
    private val mutex = Mutex()

    internal var avdActive by mutableStateOf(false)
    internal val avdAnimationScale = Animatable(0f)
}

@Composable
private fun GesturePageIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    state: OneHandedGesturePageIndicatorState,
    backgroundRotation: Float = 0f,
    backgroundScale: Float = 1f,
    transform: TransformOrigin,
    avdAlignment: Alignment,
    gestureIndicatorTint: Color,
    gestureIndicatorBackgroundColor: Color,
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

    val density = LocalDensity.current
    val avdPainter =
        rememberAnimatedVectorPainter(animatedImageVector = avd, atEnd = state.avdActive)
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
                    scaleX = state.avdAnimationScale.value
                    scaleY = state.avdAnimationScale.value
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
}
