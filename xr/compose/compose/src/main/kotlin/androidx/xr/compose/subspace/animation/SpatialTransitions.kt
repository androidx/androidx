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

package androidx.xr.compose.subspace.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.xr.compose.unit.IntVolumeOffset
import androidx.xr.compose.unit.IntVolumeSize

/** Public transition spec APIs for use with [AnimatedSpatialVisibility]. */
public object SpatialTransitions {
    /**
     * This fades in the content of the transition, from the specified starting alpha (i.e.
     * [initialAlpha]) to 1f, using the supplied [animationSpec]. [initialAlpha] defaults to 0f, and
     * [spring] is used by default.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialFade
     * @param animationSpec the [FiniteAnimationSpec] for this animation, [spring] by default
     * @param initialAlpha the starting alpha of the enter transition, 0f by default
     */
    @Stable
    public fun fadeIn(
        animationSpec: FiniteAnimationSpec<Float> =
            SpatialTransitionDefaults.DefaultAlphaAnimationSpec,
        initialAlpha: Float = 0f,
    ): SpatialEnterTransition {
        return SpatialEnterTransition.Impl(
            SpatialTransitionData(fade = Fade(initialAlpha, animationSpec))
        )
    }

    /**
     * This fades out the content of the transition, from full opacity to the specified target alpha
     * (i.e. [targetAlpha]), using the supplied [animationSpec]. By default, the content will be
     * faded out to fully transparent (i.e. [targetAlpha] defaults to 0), and [animationSpec] uses
     * [spring] by default.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialFade
     * @param animationSpec the [FiniteAnimationSpec] for this animation, [spring] by default
     * @param targetAlpha the target alpha of the exit transition, 0f by default
     */
    @Stable
    public fun fadeOut(
        animationSpec: FiniteAnimationSpec<Float> =
            SpatialTransitionDefaults.DefaultAlphaAnimationSpec,
        targetAlpha: Float = 0f,
    ): SpatialExitTransition {
        return SpatialExitTransition.Impl(
            SpatialTransitionData(fade = Fade(targetAlpha, animationSpec))
        )
    }

    /**
     * This slides in the content of the transition, from a starting offset defined in
     * [initialOffset] to `IntVolumeOffset(0, 0, 0)`. The direction of the slide can be controlled
     * by configuring the [initialOffset]. A positive x value means sliding from right to left,
     * whereas a negative x value will slide the content to the right. Similarly positive and
     * negative y values correspond to sliding up and down, respectively, and positive and negative
     * z values correspond to sliding closer and further, respectively.
     *
     * If the sliding is only desired along one axis, consider using [slideInHorizontally],
     * [slideInVertically], or [slideInDepth].
     *
     * [initialOffset] is a lambda that takes the full size of the content and returns an offset.
     * This allows the offset to be defined proportional to the full size, or as an absolute value.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialSlide
     * @param animationSpec the animation used for the slide-in, [spring] by default.
     * @param initialOffset a lambda that takes the full size of the content and returns the initial
     *   offset for the slide-in
     */
    @Stable
    public fun slideIn(
        animationSpec: FiniteAnimationSpec<IntVolumeOffset> =
            SpatialTransitionDefaults.DefaultSlideAnimationSpec,
        initialOffset: Density.(fullSize: IntVolumeSize) -> IntVolumeOffset,
    ): SpatialEnterTransition {
        return SpatialEnterTransition.Impl(
            SpatialTransitionData(slide = Slide(initialOffset, animationSpec))
        )
    }

    /**
     * This slides in the content horizontally, from a starting offset defined in [initialOffsetX]
     * to `0` **pixels**. The direction of the slide can be controlled by configuring the
     * [initialOffsetX]. A positive value means sliding from right to left, whereas a negative value
     * would slide the content from left to right.
     *
     * [initialOffsetX] is a lambda that takes the full width of the content and returns an offset.
     * This allows the starting offset to be defined proportional to the full size, or as an
     * absolute value. It defaults to return half of negative width, which would offset the content
     * to the left by half of its width, and slide towards the right.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialSlide
     * @param animationSpec the animation used for the slide-in, [spring] by default.
     * @param initialOffsetX a lambda that takes the full width of the content in pixels and returns
     *   the initial offset for the slide-in, by default it returns `-fullWidth/2`
     */
    @Stable
    public fun slideInHorizontally(
        animationSpec: FiniteAnimationSpec<IntVolumeOffset> =
            SpatialTransitionDefaults.DefaultSlideAnimationSpec,
        initialOffsetX: Density.(fullWidth: Int) -> Int = { -it / 2 },
    ): SpatialEnterTransition {
        return slideIn(
            initialOffset = { IntVolumeOffset(x = initialOffsetX(it.width), y = 0, z = 0) },
            animationSpec = animationSpec,
        )
    }

    /**
     * This slides in the content vertically, from a starting offset defined in [initialOffsetY] to
     * `0` **pixels**. The direction of the slide can be controlled by configuring the
     * [initialOffsetY]. A positive value means sliding from top to bottom, whereas a negative value
     * would slide the content from bottom to top.
     *
     * [initialOffsetY] is a lambda that takes the full height of the content and returns an offset.
     * This allows the starting offset to be defined proportional to the full size, or as an
     * absolute value. It defaults to return half of negative height, which would offset the content
     * down by half of its height, and slide upwards.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialSlide
     * @param animationSpec the animation used for the slide-in, [spring] by default.
     * @param initialOffsetY a lambda that takes the full height of the content in pixels and
     *   returns the initial offset for the slide-in, by default it returns `-fullHeight/2`
     */
    @Stable
    public fun slideInVertically(
        animationSpec: FiniteAnimationSpec<IntVolumeOffset> =
            SpatialTransitionDefaults.DefaultSlideAnimationSpec,
        initialOffsetY: Density.(fullHeight: Int) -> Int = { -it / 2 },
    ): SpatialEnterTransition {
        return slideIn(
            initialOffset = { IntVolumeOffset(x = 0, y = initialOffsetY(it.height), z = 0) },
            animationSpec = animationSpec,
        )
    }

    /**
     * This slides in the content depthwise, from a starting offset defined in [initialOffsetZ] to
     * `0` **pixels**. The direction of the slide can be controlled by configuring the
     * [initialOffsetZ]. A positive value means sliding from close to far, whereas a negative value
     * would slide the content from far to close.
     *
     * [initialOffsetZ] is a lambda that takes the full depth of the content and returns an offset.
     * This allows the starting offset to be defined proportional to the full size, or as an
     * absolute value.
     *
     * Unlike [slideInVertically] and [slideInHorizontally], this defaults to sliding in from `20dp`
     * **away** from the neutral depth point. This is because many commonly-animated Spatial
     * elements, such as `SpatialPanel`, report a depth of 0.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialSlide
     * @param animationSpec the animation used for the slide-in, [spring] by default.
     * @param initialOffsetZ a lambda that takes the full height of the content in pixels and
     *   returns the initial offset for the slide-in, by default it returns `-20.dp.toPx()`
     */
    @Stable
    public fun slideInDepth(
        animationSpec: FiniteAnimationSpec<IntVolumeOffset> =
            SpatialTransitionDefaults.DefaultSlideAnimationSpec,
        initialOffsetZ: Density.(fullDepth: Int) -> Int = { -20.dp.roundToPx() },
    ): SpatialEnterTransition {
        return slideIn(
            initialOffset = { IntVolumeOffset(x = 0, y = 0, z = initialOffsetZ(it.depth)) },
            animationSpec = animationSpec,
        )
    }

    /**
     * This slides out the content of the transition, from an offset of `IntVolumeOffset(0, 0, 0)`
     * to the target offset defined in [targetOffset]. The direction of the slide can be controlled
     * by configuring the [targetOffset]. A positive x value means sliding from left to right,
     * whereas a negative x value would slide the content from right to left. Similarly, positive
     * and negative y values correspond to sliding down and up, respectively, and positive and
     * negative z values correspond to sliding closer and further, respectively.
     *
     * If the sliding is only desired along one axis, consider using [slideOutHorizontally],
     * [slideOutVertically], or [slideOutDepth].
     *
     * [targetOffset] is a lambda that takes the full size of the content and returns an offset.
     * This allows the offset to be defined proportional to the full size, or as an absolute value.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialSlide
     * @param animationSpec the animation used for the slide-out, [spring] by default.
     * @param targetOffset a lambda that takes the full size of the content and returns the target
     *   offset for the slide-out
     */
    @Stable
    public fun slideOut(
        animationSpec: FiniteAnimationSpec<IntVolumeOffset> =
            SpatialTransitionDefaults.DefaultSlideAnimationSpec,
        targetOffset: Density.(fullSize: IntVolumeSize) -> IntVolumeOffset,
    ): SpatialExitTransition {
        return SpatialExitTransition.Impl(
            SpatialTransitionData(slide = Slide(targetOffset, animationSpec))
        )
    }

    /**
     * This slides out the content horizontally, from 0 to a target offset defined in
     * [targetOffsetX] in **pixels**. The direction of the slide can be controlled by configuring
     * the [targetOffsetX]. A positive value means sliding to the right, whereas a negative value
     * would slide the content towards the left.
     *
     * [targetOffsetX] is a lambda that takes the full width of the content and returns an offset.
     * This allows the target offset to be defined proportional to the full size, or as an absolute
     * value. It defaults to return half of negative width, which would slide the content to the
     * left by half of its width.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialSlide
     * @param animationSpec the animation used for the slide-out, [spring] by default.
     * @param targetOffsetX a lambda that takes the full width of the content and returns the
     *   initial offset for the slide-in, by default it returns `fullWidth/2`
     */
    @Stable
    public fun slideOutHorizontally(
        animationSpec: FiniteAnimationSpec<IntVolumeOffset> =
            SpatialTransitionDefaults.DefaultSlideAnimationSpec,
        targetOffsetX: Density.(fullWidth: Int) -> Int = { -it / 2 },
    ): SpatialExitTransition {
        return slideOut(
            targetOffset = { IntVolumeOffset(x = targetOffsetX(it.width), y = 0, z = 0) },
            animationSpec = animationSpec,
        )
    }

    /**
     * This slides out the content vertically, from 0 to a target offset defined in [targetOffsetY]
     * in **pixels**. The direction of the slide-out can be controlled by configuring the
     * [targetOffsetY]. A positive target offset means sliding down, whereas a negative value would
     * slide the content up.
     *
     * [targetOffsetY] is a lambda that takes the full Height of the content and returns an offset.
     * This allows the target offset to be defined proportional to the full height, or as an
     * absolute value. It defaults to return half of the negative height, which would slide the
     * content up by half of its Height.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialSlide
     * @param animationSpec the animation used for the slide-out, [spring] by default.
     * @param targetOffsetY a lambda that takes the full Height of the content and returns the
     *   target offset for the slide-out, by default it returns `fullHeight/2`
     */
    @Stable
    public fun slideOutVertically(
        animationSpec: FiniteAnimationSpec<IntVolumeOffset> =
            SpatialTransitionDefaults.DefaultSlideAnimationSpec,
        targetOffsetY: Density.(fullHeight: Int) -> Int = { -it / 2 },
    ): SpatialExitTransition {
        return slideOut(
            targetOffset = { IntVolumeOffset(x = 0, y = targetOffsetY(it.height), z = 0) },
            animationSpec = animationSpec,
        )
    }

    /**
     * This slides out the content depthwise, from 0 to a target offset defined in [targetOffsetZ]
     * in **pixels**. The direction of the slide-out can be controlled by configuring the
     * [targetOffsetZ]. A positive value means sliding from close to far, whereas a negative value
     * would slide the content from far to close.
     *
     * [targetOffsetZ] is a lambda that takes the full depth of the content and returns an offset.
     * This allows the target offset to be defined proportional to the full depth, or as an absolute
     * value.
     *
     * Unlike [slideInVertically] and [slideInHorizontally], this defaults to sliding in from `20dp`
     * **away** from the neutral depth point. This is because many commonly-animated Spatial
     * elements, such as `SpatialPanel`, report a depth of 0.
     *
     * @sample androidx.xr.compose.samples.animation.SpatialSlide
     * @param animationSpec the animation used for the slide-out, [spring] by default.
     * @param targetOffsetZ a lambda that takes the full depth of the content and returns the target
     *   offset for the slide-out, by default it returns `-20.dp.toPx()`
     */
    @Stable
    public fun slideOutDepth(
        animationSpec: FiniteAnimationSpec<IntVolumeOffset> =
            SpatialTransitionDefaults.DefaultSlideAnimationSpec,
        targetOffsetZ: Density.(fullDepth: Int) -> Int = { -20.dp.roundToPx() },
    ): SpatialExitTransition {
        return slideOut(
            targetOffset = { IntVolumeOffset(x = 0, y = 0, z = targetOffsetZ(it.depth)) },
            animationSpec = animationSpec,
        )
    }
}

/** Defaults for use with [SpatialTransitions]. */
public object SpatialTransitionDefaults {
    /** The default [SpatialEnterTransition] used by [SpatialTransitions]. */
    public val DefaultEnter: SpatialEnterTransition =
        SpatialTransitions.fadeIn() + SpatialTransitions.slideInDepth()

    /** The default [SpatialExitTransition] used by [SpatialTransitions]. */
    public val DefaultExit: SpatialExitTransition =
        SpatialTransitions.fadeOut() + SpatialTransitions.slideOutDepth()

    internal val DefaultAlphaAnimationSpec: FiniteAnimationSpec<Float> =
        spring(stiffness = Spring.StiffnessLow, dampingRatio = DEFAULT_SPATIAL_SPRING_DAMPING)

    internal val DefaultSlideAnimationSpec: FiniteAnimationSpec<IntVolumeOffset> =
        spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = DEFAULT_SPATIAL_SPRING_DAMPING,
            visibilityThreshold = IntVolumeOffset(1, 1, 1),
        )

    /** Spatial Animations use a different damping value than 2D animations. */
    private const val DEFAULT_SPATIAL_SPRING_DAMPING = 0.8f
}
