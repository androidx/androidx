/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.FloatRange
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.trace
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.foundation.lazy.inverseLerp
import kotlin.math.ceil

/**
 * Version of [TransformationSpec] that supports variable screen sizes.
 *
 * Use this API to define custom transformations for the items as they scroll across the screen.
 *
 * @sample androidx.wear.compose.material3.samples.ResponsiveTransformationSpecButtonSample
 */
public sealed interface ResponsiveTransformationSpec : TransformationSpec {
    public companion object {
        /**
         * Default [TransformationSpec] for small screen size.
         *
         * This spec should be used in [rememberTransformationSpec] together with the specs for
         * alternative screen sizes, so that in-between screen sizes can be supported using
         * interpolation.
         *
         * @param screenSize The size of the small screen.
         * @param minElementHeightFraction The minimum element height, as a ratio of the viewport
         *   height, to use for determining the transition area height within the range
         *   [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content
         *   item, this defines the start and end points for transitioning the item. Item heights
         *   lower than [minElementHeightFraction] will be treated as if [minElementHeightFraction].
         *   Must be smaller than or equal to [maxElementHeightFraction].
         * @param maxElementHeightFraction The maximum element height, as a ratio of the viewport
         *   height, to use for determining the transition area height within the range
         *   [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content
         *   item, this defines the start and end points for transitioning the item. Item heights
         *   higher than [maxElementHeightFraction] will be treated as if
         *   [maxElementHeightFraction]. Must be greater than or equal to
         *   [minElementHeightFraction].
         * @param minTransitionAreaHeightFraction The lower bound of the range of heights for the
         *   transition area, i.e. how tall the transition area is for items of height
         *   [minElementHeightFraction] or shorter. Taller items will have taller transition areas,
         *   up to [maxTransitionAreaHeightFraction]. This is defined as a fraction (value between
         *   0f..1f) of the viewport height. Must be less than or equal to
         *   [maxTransitionAreaHeightFraction]. Note that the transition area is the same for all
         *   variables, but each variable can define a transformation zone inside it in which the
         *   transformations will actually occur. See [TransformationVariableSpec].
         * @param maxTransitionAreaHeightFraction The upper bound of the range of heights for the
         *   transition area, i.e. how tall the transition area is for items of height
         *   [maxElementHeightFraction] or taller. Shorter items will have shorter transition areas,
         *   down to [minTransitionAreaHeightFraction]. This is defined as a fraction (value between
         *   0f..1f) of the viewport height. Must be greater than or equal to
         *   [minTransitionAreaHeightFraction]. Note that the transition area is the same for all
         *   variables, but each variable can define a transformation zone inside it in which the
         *   transformations will actually occur. See [TransformationVariableSpec].
         * @param easing An interpolator to use to determine how to apply transformations as the
         *   item transitions across the transformation zones.
         * @param containerAlpha Configuration for how the container (background) of the item will
         *   fade in/out.
         * @param contentAlpha Configuration for how the content of the item will fade in/out.
         * @param scale Configuration for scaling the whole item (container and content).
         */
        public fun smallScreen(
            screenSize: Dp = ResponsiveTransformationSpecDefaults.SmallScreenSize,
            @FloatRange(from = 0.0, to = 1.0) minElementHeightFraction: Float = 0.2f,
            @FloatRange(from = 0.0, to = 1.0) maxElementHeightFraction: Float = 0.6f,
            @FloatRange(from = 0.0, to = 1.0) minTransitionAreaHeightFraction: Float = 0.35f,
            @FloatRange(from = 0.0, to = 1.0) maxTransitionAreaHeightFraction: Float = 0.55f,
            easing: Easing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f),
            containerAlpha: TransformationVariableSpec = TransformationVariableSpec(0.5f),
            contentAlpha: TransformationVariableSpec = TransformationVariableSpec(0.5f),
            scale: TransformationVariableSpec = TransformationVariableSpec(0.7f),
        ): ResponsiveTransformationSpec =
            ResponsiveTransformationSpecImpl(
                screenSize = screenSize,
                minElementHeightFraction = minElementHeightFraction,
                maxElementHeightFraction = maxElementHeightFraction,
                minTransitionAreaHeightFraction = minTransitionAreaHeightFraction,
                maxTransitionAreaHeightFraction = maxTransitionAreaHeightFraction,
                easing = easing,
                containerAlpha = containerAlpha,
                contentAlpha = contentAlpha,
                scale = scale,
            )

        /**
         * Default [TransformationSpec] for large screen size.
         *
         * This spec should be used in [rememberTransformationSpec] together with the specs for
         * alternative screen sizes, so that in-between screen sizes can be supported using
         * interpolation.
         *
         * @param screenSize The screen size of the large watch.
         * @param minElementHeightFraction The minimum element height, as a ratio of the viewport
         *   height, to use for determining the transition area height within the range
         *   [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content
         *   item, this defines the start and end points for transitioning the item. Item heights
         *   lower than [minElementHeightFraction] will be treated as if [minElementHeightFraction].
         *   Must be smaller than or equal to [maxElementHeightFraction].
         * @param maxElementHeightFraction The maximum element height, as a ratio of the viewport
         *   height, to use for determining the transition area height within the range
         *   [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content
         *   item, this defines the start and end points for transitioning the item. Item heights
         *   higher than [maxElementHeightFraction] will be treated as if
         *   [maxElementHeightFraction]. Must be greater than or equal to
         *   [minElementHeightFraction].
         * @param minTransitionAreaHeightFraction The lower bound of the range of heights for the
         *   transition area, i.e. how tall the transition area is for items of height
         *   [minElementHeightFraction] or shorter. Taller items will have taller transition areas,
         *   up to [maxTransitionAreaHeightFraction]. This is defined as a fraction (value between
         *   0f..1f) of the viewport height. Must be less than or equal to
         *   [maxTransitionAreaHeightFraction]. Note that the transition area is the same for all
         *   variables, but each variable can define a transformation zone inside it in which the
         *   transformations will actually occur. See [TransformationVariableSpec].
         * @param maxTransitionAreaHeightFraction The upper bound of the range of heights for the
         *   transition area, i.e. how tall the transition area is for items of height
         *   [maxElementHeightFraction] or taller. Shorter items will have shorter transition areas,
         *   down to [minTransitionAreaHeightFraction]. This is defined as a fraction (value between
         *   0f..1f) of the viewport height. Must be greater than or equal to
         *   [minTransitionAreaHeightFraction]. Note that the transition area is the same for all
         *   variables, but each variable can define a transformation zone inside it in which the
         *   transformations will actually occur. See [TransformationVariableSpec].
         * @param easing An interpolator to use to determine how to apply transformations as the
         *   item transitions across the transformation zones.
         * @param containerAlpha Configuration for how the container (background) of the item will
         *   fade in/out.
         * @param contentAlpha Configuration for how the content of the item will fade in/out.
         * @param scale Configuration for scaling the whole item (container and content).
         */
        public fun largeScreen(
            screenSize: Dp = ResponsiveTransformationSpecDefaults.LargeScreenSize,
            @FloatRange(from = 0.0, to = 1.0) minElementHeightFraction: Float = 0.15f,
            @FloatRange(from = 0.0, to = 1.0) maxElementHeightFraction: Float = 0.45f,
            @FloatRange(from = 0.0, to = 1.0) minTransitionAreaHeightFraction: Float = 0.4f,
            @FloatRange(from = 0.0, to = 1.0) maxTransitionAreaHeightFraction: Float = 0.6f,
            easing: Easing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f),
            containerAlpha: TransformationVariableSpec = TransformationVariableSpec(0.5f),
            contentAlpha: TransformationVariableSpec = TransformationVariableSpec(0.5f),
            scale: TransformationVariableSpec = TransformationVariableSpec(0.6f),
        ): ResponsiveTransformationSpec =
            ResponsiveTransformationSpecImpl(
                screenSize = screenSize,
                minElementHeightFraction = minElementHeightFraction,
                maxElementHeightFraction = maxElementHeightFraction,
                minTransitionAreaHeightFraction = minTransitionAreaHeightFraction,
                maxTransitionAreaHeightFraction = maxTransitionAreaHeightFraction,
                easing = easing,
                containerAlpha = containerAlpha,
                contentAlpha = contentAlpha,
                scale = scale,
            )

        internal val NoOpTransformationSpec: TransformationSpec =
            object : TransformationSpec {
                override fun getTransformedHeight(
                    measuredHeight: Int,
                    scrollProgress: TransformingLazyColumnItemScrollProgress,
                ): Int = measuredHeight

                override fun GraphicsLayerScope.applyContentTransformation(
                    scrollProgress: TransformingLazyColumnItemScrollProgress
                ) {}

                override fun GraphicsLayerScope.applyContainerTransformation(
                    scrollProgress: TransformingLazyColumnItemScrollProgress
                ) {}

                override fun TransformedContainerPainterScope.createTransformedContainerPainter(
                    painter: Painter,
                    shape: Shape,
                    border: BorderStroke?,
                ): Painter = painter
            }
    }
}

/** Contains the default values used by [ResponsiveTransformationSpec] */
public object ResponsiveTransformationSpecDefaults {
    /** The default spec configuration point for the small screen size. */
    public val SmallScreenSize: Dp = 192.dp

    /** The default spec configuration point for the large screen size. */
    public val LargeScreenSize: Dp = 240.dp

    /** The default list of specs */
    internal val Specs: List<ResponsiveTransformationSpecImpl> =
        listOf(
                ResponsiveTransformationSpec.smallScreen(),
                ResponsiveTransformationSpec.largeScreen(),
            )
            .map { it as ResponsiveTransformationSpecImpl }
}

/**
 * Computes and remembers the appropriate [TransformationSpec] for the current screen size, given
 * one or more [ResponsiveTransformationSpec]s for different screen sizes.
 *
 * It would return special NoOp version of [TransformationSpec] when ReducedMotion is on.
 *
 * Example usage for [ResponsiveTransformationSpec], the recommended [TransformationSpec] for
 * large-screen aware Wear apps:
 *
 * @sample androidx.wear.compose.material3.samples.ResponsiveTransformationSpecButtonSample
 * @param specs The [ResponsiveTransformationSpec]s that should be used for different screen sizes.
 */
@Composable
public fun rememberTransformationSpec(
    vararg specs: ResponsiveTransformationSpec
): TransformationSpec {
    val screenSize = LocalConfiguration.current.screenHeightDp.dp
    val localReduceMotion = LocalReduceMotion.current
    return remember(specs, screenSize, localReduceMotion) {
        if (localReduceMotion) {
            ResponsiveTransformationSpec.NoOpTransformationSpec
        } else {
            val transformationSpecs =
                if (specs.isEmpty()) {
                    ResponsiveTransformationSpecDefaults.Specs
                } else {
                    specs.map { it as ResponsiveTransformationSpecImpl }
                }

            responsiveTransformationSpec(screenSize, transformationSpecs)
        }
    }
}

/**
 * Computes and remembers the appropriate [TransformationSpec] for the current screen size.
 *
 * It would return special NoOp version of [TransformationSpec] when ReducedMotion is on.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnNotificationsSample
 */
@Composable
public fun rememberTransformationSpec(): TransformationSpec {
    val screenSize = LocalConfiguration.current.screenHeightDp.dp
    val localReduceMotion = LocalReduceMotion.current
    return remember(screenSize, localReduceMotion) {
        if (localReduceMotion) {
            ResponsiveTransformationSpec.NoOpTransformationSpec
        } else {
            responsiveTransformationSpec(screenSize, ResponsiveTransformationSpecDefaults.Specs)
        }
    }
}

/** This class contains all parameters needed to configure the transformations for a single item */
internal class ResponsiveTransformationSpecImpl(
    /** The screen size of the watch. */
    val screenSize: Dp,

    /**
     * The minimum element height, as a ratio of the viewport height, to use for determining the
     * transition area height within the range
     * [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content item,
     * this defines the start and end points for transitioning the item. Item heights lower than
     * [minElementHeightFraction] will be treated as if [minElementHeightFraction]. Must be smaller
     * than or equal to [maxElementHeightFraction].
     */
    val minElementHeightFraction: Float,

    /**
     * The maximum element height, as a ratio of the viewport height, to use for determining the
     * transition area height within the range
     * [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content item,
     * this defines the start and end points for transitioning the item. Item heights higher than
     * [maxElementHeightFraction] will be treated as if [maxElementHeightFraction]. Must be greater
     * than or equal to [minElementHeightFraction].
     */
    val maxElementHeightFraction: Float,

    /**
     * The lower bound of the range of heights for the transition area, i.e. how tall the transition
     * area is for items of height [minElementHeightFraction] or shorter. Taller items will have
     * taller transition areas, up to [maxTransitionAreaHeightFraction]. This is defined as a
     * fraction (value between 0f..1f) of the viewport height. Must be less than or equal to
     * [maxTransitionAreaHeightFraction].
     *
     * Note that the transition area is the same for all variables, but each variable can define a
     * transformation zone inside it in which the transformations will actually occur. See
     * [TransformationVariableSpec].
     */
    val minTransitionAreaHeightFraction: Float,

    /**
     * The upper bound of the range of heights for the transition area, i.e. how tall the transition
     * area is for items of height [maxElementHeightFraction] or taller. Shorter items will have
     * shorter transition areas, down to [minTransitionAreaHeightFraction]. This is defined as a
     * fraction (value between 0f..1f) of the viewport height. Must be greater than or equal to
     * [minTransitionAreaHeightFraction].
     *
     * Note that the transition area is the same for all variables, but each variable can define a
     * transformation zone inside it in which the transformations will actually occur. See
     * [TransformationVariableSpec].
     */
    val maxTransitionAreaHeightFraction: Float,

    /**
     * An interpolator to use to determine how to apply transformations as the item transitions
     * across the transformation zones.
     */
    val easing: Easing,

    /** Configuration for how the container (background) of the item will fade in/out. */
    val containerAlpha: TransformationVariableSpec,

    /** Configuration for how the content of the item will fade in/out. */
    val contentAlpha: TransformationVariableSpec,

    /** Configuration for scaling the whole item (container and content). */
    val scale: TransformationVariableSpec,
) : ResponsiveTransformationSpec {
    init {
        // The element height range must be non-empty.
        require(minElementHeightFraction < maxElementHeightFraction) {
            "minElementHeight must be smaller than maxElementHeight"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResponsiveTransformationSpecImpl

        if (minElementHeightFraction != other.minElementHeightFraction) return false
        if (maxElementHeightFraction != other.maxElementHeightFraction) return false
        if (minTransitionAreaHeightFraction != other.minTransitionAreaHeightFraction) return false
        if (maxTransitionAreaHeightFraction != other.maxTransitionAreaHeightFraction) return false
        if (easing != other.easing) return false
        if (containerAlpha != other.containerAlpha) return false
        if (contentAlpha != other.contentAlpha) return false
        if (scale != other.scale) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1

        result += result * minElementHeightFraction.hashCode()
        result += result * maxElementHeightFraction.hashCode()
        result += result * minTransitionAreaHeightFraction.hashCode()
        result += result * maxTransitionAreaHeightFraction.hashCode()
        result += result * easing.hashCode()
        result += result * containerAlpha.hashCode()
        result += result * contentAlpha.hashCode()
        result += result * scale.hashCode()

        return result
    }

    override fun getTransformedHeight(
        measuredHeight: Int,
        scrollProgress: TransformingLazyColumnItemScrollProgress,
    ): Int =
        trace("wear-compose:tlc:getTransformedHeight") {
            with(TransitionAreaProgress(scrollProgress)) {
                ceil(compute(scale, easing) * measuredHeight).fastRoundToInt()
            }
        }

    override fun GraphicsLayerScope.applyContentTransformation(
        scrollProgress: TransformingLazyColumnItemScrollProgress
    ) =
        trace("wear-compose:tlc:applyContentTransformation") {
            if (scrollProgress.isUnspecified) return
            with(TransitionAreaProgress(scrollProgress)) {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = compute(contentAlpha, easing)
            }
        }

    override fun GraphicsLayerScope.applyContainerTransformation(
        scrollProgress: TransformingLazyColumnItemScrollProgress
    ) =
        trace("wear-compose:tlc:applyContainerTransformation") {
            if (scrollProgress.isUnspecified) return
            with(TransformationState(TransitionAreaProgress(scrollProgress))) {
                compositingStrategy = CompositingStrategy.Offscreen
                translationY = -1f * size.height * (1f - scale) / 2f
                alpha = containerAlpha
                scaleX = scale
                scaleY = scale
            }
        }

    override fun TransformedContainerPainterScope.createTransformedContainerPainter(
        painter: Painter,
        shape: Shape,
        border: BorderStroke?,
    ): Painter =
        BackgroundPainter(
            height = { itemHeight },
            shape = shape,
            border = border,
            backgroundPainter = painter,
        )
}

private fun lerp(
    start: ResponsiveTransformationSpecImpl,
    stop: ResponsiveTransformationSpecImpl,
    progress: Float,
) =
    ResponsiveTransformationSpecImpl(
        screenSize = lerp(start.screenSize.value, stop.screenSize.value, progress).dp,
        minElementHeightFraction =
            lerp(start.minElementHeightFraction, stop.minElementHeightFraction, progress),
        maxElementHeightFraction =
            lerp(start.maxElementHeightFraction, stop.maxElementHeightFraction, progress),
        minTransitionAreaHeightFraction =
            lerp(
                start.minTransitionAreaHeightFraction,
                stop.minTransitionAreaHeightFraction,
                progress,
            ),
        maxTransitionAreaHeightFraction =
            lerp(
                start.maxTransitionAreaHeightFraction,
                stop.maxTransitionAreaHeightFraction,
                progress,
            ),
        easing = { fraction ->
            lerp(start.easing.transform(fraction), stop.easing.transform(fraction), progress)
        },
        containerAlpha = lerp(start.containerAlpha, stop.containerAlpha, progress),
        contentAlpha = lerp(start.contentAlpha, stop.contentAlpha, progress),
        scale = lerp(start.scale, stop.scale, progress),
    )

/** Uses a TransformationSpec to compute a TransformationState. */
internal fun ResponsiveTransformationSpecImpl.TransformationState(
    transitionAreaProgress: TransitionAreaProgress
): TransformationState =
    with(transitionAreaProgress) {
        TransformationState(
            scale = compute(scale, easing),
            containerAlpha = compute(containerAlpha, easing),
        )
    }

/** Uses a TransformationSpec to convert a scrollProgress into a transitionProgress. */
internal fun ResponsiveTransformationSpecImpl.TransitionAreaProgress(
    scrollProgress: TransformingLazyColumnItemScrollProgress
): TransitionAreaProgress =
    if (scrollProgress == TransformingLazyColumnItemScrollProgress.Unspecified) {
        TransitionAreaProgress.None
    } else {
        // Size of the item, relative to the screen
        val relativeItemHeight =
            scrollProgress.bottomOffsetFraction - scrollProgress.topOffsetFraction

        // Where is the size of the item in the minElementHeight .. maxElementHeight range
        val sizeRatio =
            inverseLerp(minElementHeightFraction, maxElementHeightFraction, relativeItemHeight)

        // Size of each transition area.
        val scalingLine =
            lerp(minTransitionAreaHeightFraction, maxTransitionAreaHeightFraction, sizeRatio)
                // Ensure the top & bottom transition areas don't overlap.
                .coerceAtMost((1f + relativeItemHeight) / 2f)

        // See if we are in the top/bottom transition area and return that value.
        if (scrollProgress.bottomOffsetFraction < 1f - scrollProgress.topOffsetFraction) {
            TransitionAreaProgress.Top(scrollProgress.bottomOffsetFraction / scalingLine)
        } else {
            TransitionAreaProgress.Bottom((1f - scrollProgress.topOffsetFraction) / scalingLine)
        }
    }

/**
 * Computes the appropriate [ResponsiveTransformationSpecImpl] for a given screen size, given one or
 * more [ResponsiveTransformationSpecImpl]s for different screen sizes.
 */
internal fun responsiveTransformationSpec(
    screenSize: Dp,
    specs: List<ResponsiveTransformationSpecImpl>,
): ResponsiveTransformationSpecImpl {
    require(specs.isNotEmpty()) { "Must provide at least one TransformationSpec" }

    val sortedSpecs = specs.sortedBy { it.screenSize }

    if (screenSize <= sortedSpecs.first().screenSize) return sortedSpecs.first()
    if (screenSize >= sortedSpecs.last().screenSize) return sortedSpecs.last()

    var ix = 1 // We checked and it's greater than the first element's screen size.
    while (ix < sortedSpecs.size && screenSize > sortedSpecs[ix].screenSize) ix++

    return lerp(
        sortedSpecs[ix - 1],
        sortedSpecs[ix],
        inverseLerp(
            sortedSpecs[ix - 1].screenSize.value,
            sortedSpecs[ix].screenSize.value,
            screenSize.value,
        ),
    )
}
