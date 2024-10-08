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

package androidx.wear.compose.material3.lazy

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress

/**
 * The set of parameters implementing motion transformation behavior for Material3.
 *
 * New morphing effect allows to change the shape and the size of the visual element and its extent
 * depends on the [TransformingLazyColumnItemScrollProgress].
 *
 * @property morphingMinHeight The minimum height each element morphs to before the whole element
 *   scales down. Providing null value means no morphing effect will be applied.
 */
internal class LazyColumnScrollTransformBehavior(private val morphingMinHeight: () -> Float?) {
    // Scaling

    /** Scale factor applied to the item at the top edge of the LazyColumn. */
    private val topEdgeScalingFactor = 0.6f

    /** Scale factor applied to the item at the bottom edge of the LazyColumn. */
    private val bottomEdgeScaleFactor = 0.3f

    /** Easing applied to the scale factor at the top part of the LazyColumn. */
    private val topScaleEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** Easing applied to the scale factor at the bottom part of the LazyColumn. */
    private val bottomScaleEasing = CubicBezierEasing(0f, 0f, 0.6f, 1f)

    // Alpha

    /** Alpha applied to the content of the item at the top edge of the LazyColumn. */
    private val topEdgeContentAlpha = 0.33f

    /** Alpha applied to the content of the item at the bottom edge of the LazyColumn. */
    private val bottomEdgeContentAlpha = 0f

    /** Alpha easing applied to the content of the item at the bottom edge of the LazyColumn. */
    private val bottomContentAlphaEasing = CubicBezierEasing(0.6f, 0f, 0.4f, 1f)

    /** Alpha applied to the background of the item at the top edge of the LazyColumn. */
    private val topEdgeBackgroundAlpha = 0.3f

    /** Alpha applied to the background of the item at the bottom edge of the LazyColumn. */
    private val bottomEdgeBackgroundAlpha = 0.15f

    // Morphing

    /** Multiplier used to drift the item's bottom edge around sticky bottom line. */
    private val driftFactor = 0.5f

    /**
     * Line to which the item's bottom edge will stick, as a percentage of the screen height, while
     * the rest of the content is morphing.
     */
    private val stickyBottomFlippedOffsetPercentage = 0.09f

    /** Final value of the width morphing as percentage of the width. */
    private val morphWidthTargetPercentage = 0.87f

    private val widthMorphEasing: CubicBezierEasing
        get() = bottomScaleEasing

    /** Height of an item before scaling is applied. */
    fun TransformingLazyColumnItemScrollProgress.morphedHeight(contentHeight: Float): Float =
        morphingMinHeight()?.let {
            val driftingBottomFraction =
                stickyBottomFlippedOffsetPercentage + (flippedBottomOffsetFraction * driftFactor)
            if (flippedBottomOffsetFraction < driftingBottomFraction) {
                val newHeight =
                    contentHeight * (flippedTopOffsetFraction - driftingBottomFraction) /
                        (flippedTopOffsetFraction - flippedBottomOffsetFraction)
                return maxOf(it, newHeight)
            } else {
                return@let contentHeight
            }
        } ?: contentHeight

    /** Height of an item after all effects are applied. */
    fun TransformingLazyColumnItemScrollProgress.placementHeight(contentHeight: Float): Float =
        morphedHeight(contentHeight) * scale

    private val TransformingLazyColumnItemScrollProgress.flippedTopOffsetFraction: Float
        get() = 1f - topOffsetFraction

    private val TransformingLazyColumnItemScrollProgress.flippedBottomOffsetFraction: Float
        get() = 1f - bottomOffsetFraction

    val TransformingLazyColumnItemScrollProgress.scale: Float
        get() =
            when {
                flippedTopOffsetFraction < 0.5f ->
                    lerp(
                        bottomEdgeScaleFactor,
                        1f,
                        bottomScaleEasing.transform(
                            (0f..0.5f).progression(flippedTopOffsetFraction)
                        )
                    )
                flippedBottomOffsetFraction > 0.5f ->
                    lerp(
                        1f,
                        topEdgeScalingFactor,
                        topScaleEasing.transform(
                            (0.5f..1f).progression(flippedBottomOffsetFraction)
                        )
                    )
                else -> 1f
            }

    val TransformingLazyColumnItemScrollProgress.backgroundXOffsetFraction: Float
        get() =
            when {
                flippedTopOffsetFraction < 0.3f ->
                    lerp(
                        morphWidthTargetPercentage,
                        1f,
                        widthMorphEasing.transform((0f..0.3f).progression(flippedTopOffsetFraction))
                    )
                else -> 0f
            }

    val TransformingLazyColumnItemScrollProgress.contentXOffsetFraction: Float
        get() = if (backgroundXOffsetFraction > 0f) 1f - backgroundXOffsetFraction else 0f

    val TransformingLazyColumnItemScrollProgress.contentAlpha: Float
        get() =
            when {
                flippedTopOffsetFraction < 0.03f -> 0f
                flippedTopOffsetFraction < 0.15f ->
                    lerp(
                        bottomEdgeContentAlpha,
                        1f,
                        bottomContentAlphaEasing.transform(
                            (0.03f..0.15f).progression(flippedTopOffsetFraction)
                        )
                    )
                flippedBottomOffsetFraction > 0.7f ->
                    lerp(
                        1f,
                        topEdgeContentAlpha,
                        (0.7f..1f).progression(flippedBottomOffsetFraction)
                    )
                else -> 1f
            }

    val TransformingLazyColumnItemScrollProgress.backgroundAlpha: Float
        get() =
            when {
                flippedTopOffsetFraction < 0.3f ->
                    lerp(
                        bottomEdgeBackgroundAlpha,
                        1f,
                        (0f..0.3f).progression(flippedTopOffsetFraction)
                    )
                flippedBottomOffsetFraction > 0.6f ->
                    lerp(
                        1f,
                        topEdgeBackgroundAlpha,
                        (0.6f..1f).progression(flippedBottomOffsetFraction)
                    )
                else -> 1f
            }

    private fun ClosedRange<Float>.progression(value: Float) =
        ((value - start) / (endInclusive - start)).coerceIn(0f..1f)
}
