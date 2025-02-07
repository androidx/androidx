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

import androidx.compose.animation.core.Easing
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.foundation.lazy.inverseLerp

/**
 * A modifier that enables Material3 Motion transformations for content within a
 * [TransformingLazyColumn] item. It also draws the background behind the content using Material3
 * Motion transformations.
 *
 * This modifier calculates and applies transformations to the content based on the
 * [TransformingLazyColumnItemScrollProgress] of the item inside the [TransformingLazyColumn]. It
 * adjusts the height, position, applies scaling and morphing effects as the item scrolls.
 *
 * When [ReduceMotion] is enabled, this modifier will not apply any transformations.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnReducedMotionSample
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnScalingMorphingEffectSample
 * @param scope The [TransformingLazyColumnItemScope] provides access to the item's index and key.
 * @param backgroundColor Color of the background.
 * @param shape Shape of the background.
 */
@Composable
public fun Modifier.scrollTransform(
    scope: TransformingLazyColumnItemScope,
    backgroundColor: Color,
    shape: Shape = RectangleShape
): Modifier =
    if (LocalReduceMotion.current) this
    else
        with(scope) {
            var minMorphingHeight by remember(scope) { mutableStateOf<Float?>(null) }
            val spec =
                remember(scope) {
                    TransformingLazyColumnScrollTransformBehavior { minMorphingHeight }
                }
            val painter =
                remember(scope, backgroundColor, shape) {
                    ScalingMorphingBackgroundPainter(
                        spec,
                        shape,
                        border = null,
                        backgroundPainter = ColorPainter(backgroundColor)
                    ) {
                        scrollProgress
                    }
                }
            this@scrollTransform then
                TargetMorphingHeightConsumerModifierElement { minMorphingHeight = it?.toFloat() }
                    .paint(painter)
                    .transformedHeight { height, scrollProgress ->
                        with(spec) {
                            scrollProgress.placementHeight(height.toFloat()).fastRoundToInt()
                        }
                    }
                    .graphicsLayer { contentTransformation(spec) { scrollProgress } }
        }

/**
 * A modifier that enables Material3 Motion transformations for content within a
 * [TransformingLazyColumn] item.
 *
 * This modifier calculates and applies transformations to the content and background based on the
 * [TransformingLazyColumnItemScrollProgress] of the item inside the
 * [TransformingLazyColumnItemScope]. It adjusts the height, position, applies scaling and morphing
 * effects as the item scrolls.
 *
 * When [ReduceMotion] is enabled, this modifier will not apply any transformations.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnReducedMotionSample
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnScrollingSample
 * @param scope The [TransformingLazyColumnItemScope] provides access to the item's index and key.
 * @param shape [Shape] of the background.
 * @param painter [Painter] to use for the background.
 * @param border Border to draw around the background, or null if no border is needed.
 */
@Composable
public fun Modifier.scrollTransform(
    scope: TransformingLazyColumnItemScope,
    shape: Shape,
    painter: Painter,
    border: BorderStroke? = null
): Modifier =
    if (LocalReduceMotion.current) this
    else
        with(scope) {
            var minMorphingHeight by remember(scope) { mutableStateOf<Float?>(null) }
            val spec =
                remember(scope) {
                    TransformingLazyColumnScrollTransformBehavior { minMorphingHeight }
                }
            val morphingPainter =
                remember(scope, painter, shape, border) {
                    ScalingMorphingBackgroundPainter(spec, shape, border, painter) {
                        scrollProgress
                    }
                }
            this@scrollTransform then
                TargetMorphingHeightConsumerModifierElement { minMorphingHeight = it?.toFloat() }
                    .paint(morphingPainter)
                    .transformedHeight { height, scrollProgress ->
                        with(spec) {
                            scrollProgress.placementHeight(height.toFloat()).fastRoundToInt()
                        }
                    }
                    .graphicsLayer { contentTransformation(spec) { scrollProgress } }
                    .clip(shape)
        }

/**
 * A modifier that enables Material3 Motion transformations for content within a
 * [TransformingLazyColumn] item.
 *
 * This modifier calculates and applies transformations to the content and background based on the
 * [TransformingLazyColumnItemScrollProgress] of the item inside the
 * [TransformingLazyColumnItemScope]. It adjusts the height, position, applies scaling and morphing
 * effects as the item scrolls.
 *
 * Note that in most cases is recommended to use one of the other overrides to explicitly provide
 * [Shape] and background [Color] (or [Painter]) so the modifier can do the background drawing and
 * apply specific effects to background and content, as in the Material spec.
 *
 * When [ReduceMotion] is enabled, this modifier will not apply any transformations.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnReducedMotionSample
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnScrollingSample
 * @param scope The [TransformingLazyColumnItemScope] provides access to the item's index and key.
 */
@Composable
public fun Modifier.scrollTransform(
    scope: TransformingLazyColumnItemScope,
): Modifier =
    if (LocalReduceMotion.current) this
    else
        with(scope) {
            var minMorphingHeight by remember(scope) { mutableStateOf<Float?>(null) }
            val spec =
                remember(scope) {
                    TransformingLazyColumnScrollTransformBehavior { minMorphingHeight }
                }

            this@scrollTransform then
                TargetMorphingHeightConsumerModifierElement { minMorphingHeight = it?.toFloat() }
                    .transformedHeight { height, scrollProgress ->
                        with(spec) {
                            scrollProgress.placementHeight(height.toFloat()).fastRoundToInt()
                        }
                    }
                    .graphicsLayer { contentTransformation(spec) { scrollProgress } }
        }

/**
 * A modifier that enables Material3 Motion transformations for content within a
 * [TransformingLazyColumn] item.
 *
 * This modifier calculates and applies transformations to the content and background based on the
 * [TransformingLazyColumnItemScrollProgress] of the item inside the
 * [TransformingLazyColumnItemScope]. It adjusts the height, position, applies scaling and morphing
 * effects as the item scrolls.
 *
 * When [ReduceMotion] is enabled, this modifier will not apply any transformations.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnReducedMotionSample
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnScalingMorphingEffectSample
 * @param scope The [TransformingLazyColumnItemScope] provides access to the item's index and key.
 * @param spec [TransformationSpec] to specify all needed parameters for the transformations.
 */
@Composable
internal fun Modifier.scrollTransform(
    scope: TransformingLazyColumnItemScope,
    spec: TransformationSpec,
): Modifier =
    if (LocalReduceMotion.current) this
    else
        with(scope) {
            val minMorphingHeight = remember(scope) { mutableStateOf<Float?>(null) }
            val transformation = remember { mutableStateOf<TransformationState?>(null) }
            val updatedSpec by rememberUpdatedState(spec)

            this@scrollTransform then
                TargetMorphingHeightConsumerModifierElement {
                        minMorphingHeight.value = it?.toFloat()
                    }
                    .transformedHeight { height, scrollProgress ->
                        // TODO: may be better to create once and update. Still need to ensure
                        // readers
                        // are reading a state so they register to updates.
                        transformationState(
                                spec = updatedSpec,
                                itemHeight = height.toFloat(),
                                minMorphingHeight = minMorphingHeight.value,
                                scrollProgress = scrollProgress
                            )
                            .also { transformation.value = it }
                            .placementHeight
                            .fastRoundToInt()
                    }
                    .graphicsLayer { contentTransformation(transformation.value) }
        }

/**
 * A modifier that enables Material3 Motion transformations for content within a
 * [TransformingLazyColumn] item.
 *
 * This modifier calculates and applies transformations to the content and background based on the
 * [TransformingLazyColumnItemScrollProgress] of the item inside the
 * [TransformingLazyColumnItemScope]. It adjusts the height, position, applies scaling and morphing
 * effects as the item scrolls.
 *
 * When [ReduceMotion] is enabled, this modifier will not apply any transformations.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnReducedMotionSample
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnScalingMorphingEffectSample
 * @param scope The [TransformingLazyColumnItemScope] provides access to the item's index and key.
 * @param spec [TransformationSpec] to specify all needed parameters for the transformations.
 * @param shape [Shape] of the background.
 * @param painter [Painter] to use for the background.
 * @param border Border to draw around the background, or null if no border is needed.
 */
@Composable
internal fun Modifier.scrollTransform(
    scope: TransformingLazyColumnItemScope,
    spec: TransformationSpec,
    shape: Shape,
    painter: Painter,
    border: BorderStroke? = null
): Modifier =
    if (LocalReduceMotion.current) this
    else
        with(scope) {
            val minMorphingHeight = remember(scope) { mutableStateOf<Float?>(null) }
            val transformation = remember { mutableStateOf<TransformationState?>(null) }
            val morphingPainter =
                remember(scope, spec, painter, shape, border) {
                    BackgroundPainter(transformation, shape, border, painter)
                }
            val updatedSpec by rememberUpdatedState(spec)

            this@scrollTransform then
                TargetMorphingHeightConsumerModifierElement {
                        minMorphingHeight.value = it?.toFloat()
                    }
                    .paint(morphingPainter)
                    .transformedHeight { height, scrollProgress ->
                        // TODO: may be better to create once and update. Still need to ensure
                        // readers
                        // are reading a state so they register to updates.
                        transformationState(
                                spec = updatedSpec,
                                itemHeight = height.toFloat(),
                                minMorphingHeight = minMorphingHeight.value,
                                scrollProgress = scrollProgress
                            )
                            .also { transformation.value = it }
                            .placementHeight
                            .fastRoundToInt()
                    }
                    .graphicsLayer { contentTransformation(transformState = transformation.value) }
                    .clip(shape)
        }

/**
 * Class that represents where in the transition areas a given item is. This can be either in the
 * top transition area, the bottom transition area, or neither.
 */
@JvmInline
internal value class TransitionAreaProgress(private val encodedProgress: Float) {
    // encodedProgress is, going from top to bottom:
    // -1 to 0 for top transition
    // 0 in the center of the screen, no transition
    // 0 to 1 for bottom transition.

    /** Are we in the top transition area */
    val isInTopTransitionArea: Boolean
        get() = encodedProgress < 0

    /**
     * How far into the transition area we are. 0 = item is entering the screen, 1 = item is
     * exiting/outside the transition area.
     */
    val progress: Float
        get() = if (encodedProgress < 0) encodedProgress + 1 else 1 - encodedProgress

    /**
     * Compute the value the given variable will have, given the current progress on the
     * transformation zone and the easing to apply.
     */
    fun compute(variable: TransformVariableSpec, easing: Easing): Float {
        val edgeValue = if (isInTopTransitionArea) variable.topValue else variable.bottomValue
        val transformationZoneProgress =
            inverseLerp(
                variable.transformationZoneEnterFraction,
                variable.transformationZoneExitFraction,
                progress
            )
        return lerp(edgeValue, 1f, easing.transform(transformationZoneProgress))
    }

    companion object {
        /** We are not in a transition area */
        val None = TransitionAreaProgress(0f)

        /**
         * We are in the top transition area, progress is 0 for an item entering the screen, up to 1
         * for an item exiting this transition area.
         */
        fun Top(progress: Float) = TransitionAreaProgress((progress - 1f).coerceAtMost(0f))

        /**
         * We are in the botom transition area, progress is 0 for an item entering the screen, up to
         * 1 for an item exiting this transition area.
         */
        fun Bottom(progress: Float) = TransitionAreaProgress((1f - progress).coerceAtLeast(0f))
    }
}

/** Uses a TransformationSpec to compute a TransformationState. */
internal fun transformationState(
    spec: TransformationSpec,
    itemHeight: Float,
    minMorphingHeight: Float?,
    scrollProgress: TransformingLazyColumnItemScrollProgress
): TransformationState {
    val transformProgress = transformProgress(scrollProgress, spec)
    val scale = transformProgress.compute(spec.scale, spec.easing)
    val morphedHeight =
        minMorphingHeight?.let {
            morphedHeight(
                scrollProgress = scrollProgress,
                spec = spec,
                itemHeight = itemHeight,
                minMorphingHeight = it
            )
        } ?: itemHeight

    return TransformationState(
        scale = scale,
        containerAlpha = transformProgress.compute(spec.containerAlpha, spec.easing),
        contentAlpha = transformProgress.compute(spec.contentAlpha, spec.easing),
        morphWidth = transformProgress.compute(spec.containerWidth, spec.easing),
        morphedHeight = morphedHeight
    )
}

/** Uses a TransformationSpec to convert a scrollProgress into a transitionProgress. */
private fun transformProgress(
    scrollProgress: TransformingLazyColumnItemScrollProgress,
    spec: TransformationSpec
): TransitionAreaProgress =
    if (scrollProgress == TransformingLazyColumnItemScrollProgress.Unspecified) {
        TransitionAreaProgress.None
    } else {
        // Size of the item, relative to the screen
        val relativeItemHeight =
            scrollProgress.bottomOffsetFraction - scrollProgress.topOffsetFraction

        // Where is the size of the item in the minElementHeight .. maxElementHeight range
        val sizeRatio =
            inverseLerp(spec.minElementHeight, spec.maxElementHeight, relativeItemHeight)

        // Size of each transition area.
        val scalingLine =
            lerp(spec.minTransitionArea, spec.maxTransitionArea, sizeRatio)
                // Ensure the top & bottom transition areas don't overlap.
                .coerceAtMost((1f + relativeItemHeight) / 2f)

        // See if we are in the top/bottom transition area and return that value.
        if (scrollProgress.bottomOffsetFraction < 1f - scrollProgress.topOffsetFraction) {
            TransitionAreaProgress.Top(scrollProgress.bottomOffsetFraction / scalingLine)
        } else {
            TransitionAreaProgress.Bottom((1f - scrollProgress.topOffsetFraction) / scalingLine)
        }
    }

/** Compute new morphing height for the item based on its size and position on the screen. */
private fun morphedHeight(
    scrollProgress: TransformingLazyColumnItemScrollProgress,
    spec: TransformationSpec,
    itemHeight: Float,
    minMorphingHeight: Float
): Float {
    // Size of the item, relative to the screen
    val relativeItemHeight = scrollProgress.bottomOffsetFraction - scrollProgress.topOffsetFraction
    val screenSize = itemHeight / relativeItemHeight

    val growthStartTopOffsetFraction =
        spec.growthStartScreenFraction - minMorphingHeight / screenSize
    val growthEndTopOffsetFraction = spec.growthEndScreenFraction - relativeItemHeight
    // Fraction of how item has grown so far.
    val heightMorphProgress =
        // growthStartTopOffsetFraction > growthEndTopOffsetFraction since item has minimum size at
        // the bottom of the screen.
        inverseLerp(
                growthStartTopOffsetFraction,
                growthEndTopOffsetFraction,
                scrollProgress.topOffsetFraction
            )
            .coerceIn(0f, 1f)
    return lerp(minMorphingHeight, itemHeight, heightMorphProgress)
}

// TODO: Decide what we want to compute & store vs compute when needed.
internal data class TransformationState(
    val containerAlpha: Float,
    val contentAlpha: Float,
    val scale: Float,
    val morphWidth: Float,
    val morphedHeight: Float, // Height after morphing, before scaling
    val contentXOffsetFraction: Float = 0f, // TODO: Implement horizontal morphing
    val backgroundXOffsetFraction: Float = 1f // TODO: Implement horizontal morphing
) {
    internal val placementHeight: Float
        get() = morphedHeight * scale
}
