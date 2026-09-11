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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
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
 * A wrapper that replaces the [content] to indicate to the user that a gesture action is available.
 *
 * This component manages the visual transition between the [content] and a gesture indicator. It
 * observes the [OneHandedGestureClickIndicatorState] to manage its visual transition. When
 * [OneHandedGestureClickIndicatorState.showIndicator] is called, the component replaces its
 * [content] with a gesture animation.
 *
 * Sample demonstrating a gesture indicator applied to a [androidx.wear.compose.material3.Button]:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureButtonSample
 *
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_OneHandedGestureButtonSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * @param gestureConfiguration the specification for the one-handed gesture
 * @param state The state object used to synchronize the indicator visibility.
 * @param modifier The [Modifier] to be applied to the [OneHandedGestureClickIndicator] layout.
 * @param gestureIndicatorSize The size constraints for the gesture indicator icon.
 * @param gestureIndicatorTint The color which will be used for a tint of the gesture animation
 * @param content The original component content (e.g., Text or Icon) to be displayed when no
 *   indicator is active.
 *
 * See also the
 * [UI Design Guides for One-Handed Gestures](https://developer.android.com/design/ui/wear/guides/patterns/gestures)
 */
@Composable
public fun OneHandedGestureClickIndicator(
    gestureConfiguration: OneHandedGestureConfiguration,
    state: OneHandedGestureClickIndicatorState,
    modifier: Modifier = Modifier,
    gestureIndicatorSize: OneHandedGestureIndicatorSize = OneHandedGestureDefaults.indicatorSize,
    gestureIndicatorTint: Color = OneHandedGestureDefaults.indicatorTint,
    content: @Composable () -> Unit,
) {
    val gestureManager = LocalOneHandedGestureManager.current
    val avd = gestureConfiguration.action.animatedImageVector()
    val duration = avd.totalDuration.milliseconds

    // Gesture manager needs to know whether this indicator draws outside the boundary of its UI
    // element as that affects the frequency with which it is shown.
    gestureManager.registerGestureIndicator(gestureConfiguration, isFloating = false, duration)
    state.gestureConfiguration = gestureConfiguration
    state.gestureManager = gestureManager

    Layout(
        content = {
            Box(modifier = Modifier.layoutId("icon"), contentAlignment = Alignment.Center) {
                val painter =
                    rememberAnimatedVectorPainter(
                        animatedImageVector = avd,
                        atEnd = state.avdActive,
                    )

                GestureIndicatorImage(
                    painter = painter,
                    size = DpSize(gestureIndicatorSize.size, gestureIndicatorSize.size),
                    tint = gestureIndicatorTint,
                    scaleX = { state.avdAnimationScale.value },
                    scaleY = { state.avdAnimationScale.value },
                )
            }
            Box(
                modifier =
                    Modifier.layoutId("content").graphicsLayer { alpha = state.contentAlpha.value },
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
}

/**
 * A state object used to coordinate visual feedback between a [oneHandedGesture] modifier and the
 * [OneHandedGestureClickIndicator].
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
public class OneHandedGestureClickIndicatorState @RememberInComposition constructor() {

    /**
     * Initiate showing the `OneHandedGestureClickIndicator` associated with this
     * `OneHandedGestureClickIndicatorState`
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
            launch { contentAlpha.animateTo(0f, EXPRESSIVE_DEFAULT_EFFECTS_SPRING_FLOAT) }
            launch { avdAnimationScale.animateTo(1f, EXPRESSIVE_DEFAULT_SPATIAL_SPRING_FLOAT) }
            delay(INDICATOR_ANIMATION_START_DELAY_MILLIS.milliseconds)

            // Play indicator animation
            avdActive = true // Start the AVD
            delay(gestureIndicator.duration)
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

            // Confirm that the indicator was shown, which will update the
            // tally for frequency checking.
            gestureManager.notifyIndicatorShown(gestureConfiguration)
        } finally {
            avdActive = false

            withContext(NonCancellable) {
                contentAlpha.snapTo(1f)
                avdAnimationScale.snapTo(0f)
            }
        }
    }

    internal lateinit var gestureManager: OneHandedGestureManager
    internal lateinit var gestureConfiguration: OneHandedGestureConfiguration
    private val mutex = Mutex()

    internal val contentAlpha = Animatable(1f)
    internal var avdActive: Boolean by mutableStateOf(false)
    internal val avdAnimationScale = Animatable(0f)
}
