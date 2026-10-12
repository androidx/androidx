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

package androidx.compose.material3

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.MutableContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.internal.PredictiveBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Provides transforms adhering to
 * [Material 3 motion specifications](https://m3.material.io/styles/motion/transitions/transition-patterns).
 */
public object MaterialTransitions {

    /**
     * The recommended scrim color for navigation transitions.
     *
     * Uses [ColorScheme.scrim] as the base color and adjusts its opacity based on the [luminance]
     * of [ColorScheme.surface].
     */
    public val scrimColor: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            return MaterialTheme.colorScheme.scrim.copy(
                alpha = if (isDark) ScrimAlphaDark else ScrimAlphaLight
            )
        }

    /**
     * Predefined horizontal shared axis transforms adhering to Material 3 motion specifications.
     *
     * These animations are consistent with the default system cross-activity navigation animations.
     * See
     * [Material 3 transition patterns](https://m3.material.io/styles/motion/transitions/transition-patterns).
     */
    public object HorizontalSharedAxis {

        /**
         * Creates a [ContentTransform] for a forward horizontal shared axis transition.
         *
         * @sample androidx.compose.material3.samples.HorizontalSharedAxisTransitionSample
         */
        public fun forwardTransform(): ContentTransform {
            return ((slideInHorizontally { (it * SlideFraction).toInt() } +
                fadeIn(
                    animationSpec =
                        tween(
                            durationMillis = FadeDurationMillis,
                            delayMillis = FadeDelayMillis,
                            easing = LinearEasing,
                        )
                )) togetherWith
                (slideOutHorizontally { (-it * SlideFraction).toInt() } +
                    fadeOut(
                        animationSpec =
                            tween(
                                durationMillis = FadeDurationMillis,
                                delayMillis = FadeDelayMillis,
                                easing = LinearEasing,
                            )
                    )))
        }

        /**
         * Creates a [ContentTransform] for the commit phase of a horizontal shared axis backward
         * transition.
         *
         * @sample androidx.compose.material3.samples.HorizontalSharedAxisTransitionSample
         */
        public fun backwardTransform(): ContentTransform {
            return ((slideInHorizontally { (-it * SlideFraction).toInt() }) togetherWith
                    (slideOutHorizontally { (it * SlideFraction).toInt() } +
                        scaleOut(targetScale = 1f) +
                        fadeOut(
                            animationSpec =
                                tween(durationMillis = FadeDurationMillis, easing = LinearEasing)
                        )))
                .apply { targetContentZIndex = BackwardTargetContentZIndex }
        }

        /**
         * Creates a [MutableContentTransform] configured for the gesture phase of a horizontal
         * shared axis predictive back transition.
         *
         * @sample androidx.compose.material3.samples.HorizontalSharedAxisTransitionSample
         * @param backEvent provider of the ongoing [NavigationEvent].
         * @param density the [Density] used to convert display bounds margins to pixels.
         * @param scrimColor color of the scrim overlay applied to the entering content. Use
         *   [MaterialTransitions.scrimColor] for the recommended scrim color that adapts to the
         *   theme.
         */
        public fun predictiveBackTransform(
            backEvent: () -> NavigationEvent,
            density: Density,
            scrimColor: Color,
        ): MutableContentTransform {
            val displayBoundsMarginPx = with(density) { DisplayBoundsMargin.toPx() }
            val initialTouchY = backEvent().touchY

            return MutableContentTransform(targetVeilMatchParentSize = true) {
                initialContentTransform { fullSize ->
                    val event = backEvent()
                    val rawYDelta = event.touchY - initialTouchY
                    val easedProgress = PredictiveBack.transform(event.progress)
                    val currentScale = 1f - (easedProgress * ScaleProgressMultiplier)
                    val pivotFractionX =
                        when (event.swipeEdge) {
                            NavigationEvent.EDGE_RIGHT -> PivotFractionXRightEdge
                            else -> PivotFractionXDefault
                        }

                    alpha = 1f
                    scale = currentScale
                    transformOrigin = TransformOrigin(pivotFractionX, PivotFractionY)
                    offset =
                        calculateSlideOffset(
                            fullSize = fullSize,
                            currentScale = currentScale,
                            rawYDelta = rawYDelta,
                            displayBoundsMargin = displayBoundsMarginPx,
                        )
                }
                targetContentTransform { fullSize ->
                    val event = backEvent()
                    val rawYDelta = event.touchY - initialTouchY
                    val easedProgress = PredictiveBack.transform(event.progress)
                    val baseScale = 1f - (easedProgress * ScaleProgressMultiplier)

                    alpha = 1f
                    veil = scrimColor
                    scale = baseScale - TargetScaleDelta
                    val verticalOffset =
                        calculateSlideOffset(
                            fullSize = fullSize,
                            currentScale = baseScale,
                            rawYDelta = rawYDelta,
                            displayBoundsMargin = displayBoundsMarginPx,
                        )
                    offset =
                        IntOffset(
                            x = (-SlideFraction * fullSize.width).toInt(),
                            y = verticalOffset.y,
                        )
                }
            }
        }

        private const val SlideFraction = 0.2f
        private const val TargetScaleDelta = 0.05f
        private const val BackwardTargetContentZIndex = -10f
        private const val PivotFractionXRightEdge = 0.5f
        private const val PivotFractionXDefault = 0.9f
        private const val PivotFractionY = 0.5f
        private const val FadeDurationMillis = 90
        private const val FadeDelayMillis = 50
    }

    /**
     * Provides fade through transforms adhering to Material 3 motion specifications.
     *
     * Use these animations for transitions between destinations without a strong spatial or
     * directional relationship, such as switching between tabs in a bottom navigation bar. See
     * [Material 3 transition patterns](https://m3.material.io/styles/motion/transitions/transition-patterns).
     */
    public object FadeThrough {

        /**
         * Creates a [ContentTransform] for a fade through transition.
         *
         * @sample androidx.compose.material3.samples.FadeThroughTransitionSample
         */
        public fun transform(): ContentTransform {
            return (fadeIn(
                animationSpec =
                    tween(
                        durationMillis = FadeInDurationMillis,
                        delayMillis = FadeOutDurationMillis,
                        easing = LinearEasing,
                    )
            ) + scaleIn(initialScale = InitialScale)) togetherWith
                (fadeOut(
                    animationSpec =
                        tween(durationMillis = FadeOutDurationMillis, easing = LinearEasing)
                ) + scaleOut(targetScale = TargetScale))
        }

        /**
         * Creates a [MutableContentTransform] configured for the gesture phase of a fade through
         * predictive back transition.
         *
         * @sample androidx.compose.material3.samples.FadeThroughTransitionSample
         * @param backEvent provider of the ongoing [NavigationEvent]
         * @param density the [Density] used to convert display bounds margins to pixels
         */
        public fun predictiveBackTransform(
            backEvent: () -> NavigationEvent,
            density: Density,
        ): MutableContentTransform {
            val displayBoundsMarginPx = with(density) { DisplayBoundsMargin.toPx() }
            val initialTouchY = backEvent().touchY

            return MutableContentTransform {
                initialContentTransform { fullSize ->
                    val event = backEvent()
                    val rawYDelta = event.touchY - initialTouchY
                    val easedProgress = PredictiveBack.transform(event.progress)
                    val currentScale = 1f - (easedProgress * ScaleProgressMultiplier)

                    alpha = 1f
                    scale = currentScale
                    transformOrigin = TransformOrigin.Center
                    offset =
                        calculateSlideOffset(
                            fullSize = fullSize,
                            currentScale = currentScale,
                            rawYDelta = rawYDelta,
                            displayBoundsMargin = displayBoundsMarginPx,
                        )
                }
                targetContentTransform {
                    alpha = 0f
                    scale = InitialScale
                    transformOrigin = TransformOrigin.Center
                }
            }
        }

        private const val FadeOutDurationMillis = 90
        private const val FadeInDurationMillis = 210
        private const val InitialScale = 0.8f
        private const val TargetScale = 1.05f
    }
}

// -----------------------------------------------------------------------------
// Internal Implementation Details & Math
// -----------------------------------------------------------------------------

private const val ScrimAlphaLight = 0.2f
private const val ScrimAlphaDark = 0.8f
private const val ScaleProgressMultiplier = 0.15f
private val DisplayBoundsMargin = 8.dp

private fun calculateSlideOffset(
    fullSize: IntSize,
    currentScale: Float,
    rawYDelta: Float,
    displayBoundsMargin: Float,
): IntOffset {
    val verticalOffsetPx =
        calculateDampedVerticalOffset(
            rawYDelta = rawYDelta,
            containerHeight = fullSize.height.toFloat(),
            currentScale = currentScale,
            displayBoundsMargin = displayBoundsMargin,
        )
    return IntOffset(0, verticalOffsetPx.toInt())
}

internal fun calculateDampedVerticalOffset(
    rawYDelta: Float,
    containerHeight: Float,
    currentScale: Float,
    displayBoundsMargin: Float,
): Float {
    if (containerHeight <= 0f) return 0f
    val maxDeltaY =
        max(0f, (containerHeight - (containerHeight * currentScale)) / 2f - displayBoundsMargin)
    val deltaYRatio = min(1f, abs(rawYDelta) / (containerHeight / 2f))
    val interpolatedRatio = 1f - (1f - deltaYRatio) * (1f - deltaYRatio) // Quadratic decelerate
    val yDirection = if (rawYDelta < 0f) -1f else 1f
    return maxDeltaY * interpolatedRatio * yDirection
}
