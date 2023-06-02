/*
 * Copyright 2020 The Android Open Source Project
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

@file:OptIn(InternalAnimationApi::class, ExperimentalAnimationApi::class)

package androidx.compose.animation

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.createDeferredAnimation
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain

@RequiresOptIn(message = "This is an experimental animation API.")
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAnimationApi

/**
 * [EnterTransition] defines how an [AnimatedVisibility] Composable appears on screen as it
 * becomes visible. The 4 categories of EnterTransitions available are:
 * 1. fade: [fadeIn]
 * 2. scale: [scaleIn]
 * 3. slide: [slideIn], [slideInHorizontally], [slideInVertically]
 * 4. expand: [expandIn], [expandHorizontally], [expandVertically]
 *
 * [EnterTransition.None] can be used when no enter transition is desired.
 * Different [EnterTransition]s can be combined using plus operator,  for example:
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 *
 * __Note__: [fadeIn], [scaleIn] and [slideIn] do not affect the size of the [AnimatedVisibility]
 * composable. In contrast, [expandIn] will grow the clip bounds to reveal the whole content. This
 * will automatically animate other layouts out of the way, very much like [animateContentSize].
 *
 * @see fadeIn
 * @see scaleIn
 * @see slideIn
 * @see slideInHorizontally
 * @see slideInVertically
 * @see expandIn
 * @see expandHorizontally
 * @see expandVertically
 * @see AnimatedVisibility
 */
@Immutable
sealed class EnterTransition {
    internal abstract val data: TransitionData

    /**
     * Combines different enter transitions. The order of the [EnterTransition]s being combined
     * does not matter, as these [EnterTransition]s will start simultaneously. The order of
     * applying transforms from these enter transitions (if defined) is: alpha and scale first,
     * shrink or expand, then slide.
     *
     * @sample androidx.compose.animation.samples.FullyLoadedTransition
     *
     * @param enter another [EnterTransition] to be combined
     */
    @Stable
    operator fun plus(enter: EnterTransition): EnterTransition {
        return EnterTransitionImpl(
            TransitionData(
                fade = data.fade ?: enter.data.fade,
                slide = data.slide ?: enter.data.slide,
                changeSize = data.changeSize ?: enter.data.changeSize,
                scale = data.scale ?: enter.data.scale
            )
        )
    }

    override fun toString(): String =
        if (this == None) {
            "EnterTransition.None"
        } else {
            data.run {
                "EnterTransition: \n" + "Fade - " + fade?.toString() + ",\nSlide - " +
                    slide?.toString() + ",\nShrink - " + changeSize?.toString() +
                    ",\nScale - " + scale?.toString()
            }
        }

    override fun equals(other: Any?): Boolean {
        return other is EnterTransition && other.data == data
    }

    override fun hashCode(): Int = data.hashCode()

    companion object {
        /**
         * This can be used when no enter transition is desired. It can be useful in cases where
         * there are other forms of enter animation defined indirectly for an
         * [AnimatedVisibility]. e.g.The children of the [AnimatedVisibility] have all defined
         * their own [EnterTransition], or when the parent is fading in, etc.
         *
         * @see [ExitTransition.None]
         */
        val None: EnterTransition = EnterTransitionImpl(TransitionData())
    }
}

/**
 * [ExitTransition] defines how an [AnimatedVisibility] Composable disappears on screen as it
 * becomes not visible. The 4 categories of [ExitTransition] available are:
 * 1. fade: [fadeOut]
 * 2. scale: [scaleOut]
 * 3. slide: [slideOut], [slideOutHorizontally], [slideOutVertically]
 * 4. shrink: [shrinkOut], [shrinkHorizontally], [shrinkVertically]
 *
 * [ExitTransition.None] can be used when no exit transition is desired.
 * Different [ExitTransition]s can be combined using plus operator, for example:
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 *
 * __Note__: [fadeOut] and [slideOut] do not affect the size of the [AnimatedVisibility]
 * composable. In contrast, [shrinkOut] (and [shrinkHorizontally], [shrinkVertically]) will shrink
 * the clip bounds to reveal less and less of the content.  This will automatically animate other
 * layouts to fill in the space, very much like [animateContentSize].
 *
 * @see fadeOut
 * @see scaleOut
 * @see slideOut
 * @see slideOutHorizontally
 * @see slideOutVertically
 * @see shrinkOut
 * @see shrinkHorizontally
 * @see shrinkVertically
 * @see AnimatedVisibility
 */
@Immutable
sealed class ExitTransition {
    internal abstract val data: TransitionData

    /**
     * Combines different exit transitions. The order of the [ExitTransition]s being combined
     * does not matter, as these [ExitTransition]s will start simultaneously. The order of
     * applying transforms from these exit transitions (if defined) is: alpha and scale first,
     * shrink or expand, then slide.
     *
     * @sample androidx.compose.animation.samples.FullyLoadedTransition
     *
     * @param exit another [ExitTransition] to be combined.
     */
    @Stable
    operator fun plus(exit: ExitTransition): ExitTransition {
        return ExitTransitionImpl(
            TransitionData(
                fade = data.fade ?: exit.data.fade,
                slide = data.slide ?: exit.data.slide,
                changeSize = data.changeSize ?: exit.data.changeSize,
                scale = data.scale ?: exit.data.scale
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is ExitTransition && other.data == data
    }

    override fun toString(): String =
        if (this == None) {
            "ExitTransition.None"
        } else {
            data.run {
                "ExitTransition: \n" + "Fade - " + fade?.toString() + ",\nSlide - " +
                    slide?.toString() + ",\nShrink - " + changeSize?.toString() +
                    ",\nScale - " + scale?.toString()
            }
        }

    override fun hashCode(): Int = data.hashCode()

    companion object {
        /**
         * This can be used when no built-in [ExitTransition] (i.e. fade/slide, etc) is desired for
         * the [AnimatedVisibility], but rather the children are defining their own exit
         * animation using the [Transition] scope.
         *
         * __Note:__ If [None] is used, and nothing is animating in the Transition<EnterExitState>
         * scope that [AnimatedVisibility] provided, the content will be removed from
         * [AnimatedVisibility] right away.
         *
         * @sample androidx.compose.animation.samples.AVScopeAnimateEnterExit
         */
        val None: ExitTransition = ExitTransitionImpl(TransitionData())
    }
}

/**
 * This fades in the content of the transition, from the specified starting alpha (i.e.
 * [initialAlpha]) to 1f, using the supplied [animationSpec]. [initialAlpha] defaults to 0f,
 * and [spring] is used by default.
 *
 * @sample androidx.compose.animation.samples.FadeTransition
 *
 * @param animationSpec the [FiniteAnimationSpec] for this animation, [spring] by default
 * @param initialAlpha the starting alpha of the enter transition, 0f by default
 */
@Stable
fun fadeIn(
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    initialAlpha: Float = 0f
): EnterTransition {
    return EnterTransitionImpl(TransitionData(fade = Fade(initialAlpha, animationSpec)))
}

/**
 * This fades out the content of the transition, from full opacity to the specified target alpha
 * (i.e. [targetAlpha]), using the supplied [animationSpec]. By default, the content will be faded
 * out to fully transparent (i.e. [targetAlpha] defaults to 0), and [animationSpec] uses
 * [spring] by default.
 *
 * @sample androidx.compose.animation.samples.FadeTransition
 *
 * @param animationSpec the [FiniteAnimationSpec] for this animation, [spring] by default
 * @param targetAlpha the target alpha of the exit transition, 0f by default
 */
@Stable
fun fadeOut(
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    targetAlpha: Float = 0f,
): ExitTransition {
    return ExitTransitionImpl(TransitionData(fade = Fade(targetAlpha, animationSpec)))
}

/**
 * This slides in the content of the transition, from a starting offset defined in [initialOffset]
 * to `IntOffset(0, 0)`. The direction of the slide can be controlled by configuring the
 * [initialOffset]. A positive x value means sliding from right to left, whereas a negative x
 * value will slide the content to the right. Similarly positive and negative y values
 * correspond to sliding up and down, respectively.
 *
 * If the sliding is only desired horizontally or vertically, instead of along both axis, consider
 * using [slideInHorizontally] or [slideInVertically].
 *
 * [initialOffset] is a lambda that takes the full size of the content and returns an offset.
 * This allows the offset to be defined proportional to the full size, or as an absolute value.
 *
 * @sample androidx.compose.animation.samples.SlideInOutSample
 *
 * @param animationSpec the animation used for the slide-in, [spring] by default.
 * @param initialOffset a lambda that takes the full size of the content and returns the initial
 *                        offset for the slide-in
 */
@Stable
fun slideIn(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
    initialOffset: (fullSize: IntSize) -> IntOffset,
): EnterTransition {
    return EnterTransitionImpl(TransitionData(slide = Slide(initialOffset, animationSpec)))
}

/**
 * This slides out the content of the transition, from an offset of `IntOffset(0, 0)` to the
 * target offset defined in [targetOffset]. The direction of the slide can be controlled by
 * configuring the [targetOffset]. A positive x value means sliding from left to right, whereas a
 * negative x value would slide the content from right to left. Similarly,  positive and negative y
 * values correspond to sliding down and up, respectively.
 *
 * If the sliding is only desired horizontally or vertically, instead of along both axis, consider
 * using [slideOutHorizontally] or [slideOutVertically].
 *
 * [targetOffset] is a lambda that takes the full size of the content and returns an offset.
 * This allows the offset to be defined proportional to the full size, or as an absolute value.
 *
 * @sample androidx.compose.animation.samples.SlideInOutSample
 *
 * @param animationSpec the animation used for the slide-out, [spring] by default.
 * @param targetOffset a lambda that takes the full size of the content and returns the target
 *                     offset for the slide-out
 */
@Stable
fun slideOut(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
    targetOffset: (fullSize: IntSize) -> IntOffset,
): ExitTransition {
    return ExitTransitionImpl(TransitionData(slide = Slide(targetOffset, animationSpec)))
}

/**
 * This scales the content as it appears, from an initial scale (defined in [initialScale]) to 1f.
 * [transformOrigin] defines the pivot point in terms of fraction of the overall size.
 * [TransformOrigin.Center] by default. [scaleIn] can be used in combination with any other type
 * of [EnterTransition] using the plus operator (e.g. `scaleIn() + slideInHorizontally()`)
 *
 * Note: Scale is applied __before__ slide. This means when using [slideIn]/[slideOut] with
 * [scaleIn]/[scaleOut], the amount of scaling needs to be taken into account when sliding.
 *
 * The scaling will change the visual of the content, but will __not__ affect the layout size.
 * [scaleIn] can be combined with [expandIn]/[expandHorizontally]/[expandVertically] to coordinate
 * layout size change while scaling. For example:
 * @sample androidx.compose.animation.samples.ScaledEnterExit
 *
 * @param animationSpec the animation used for the scale-out, [spring] by default.
 * @param initialScale the initial scale for the enter transition, 0 by default.
 * @param transformOrigin the pivot point in terms of fraction of the overall size. By default it's
 *                        [TransformOrigin.Center].
 */
@Stable
fun scaleIn(
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    initialScale: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
): EnterTransition {
    return EnterTransitionImpl(
        TransitionData(scale = Scale(initialScale, transformOrigin, animationSpec))
    )
}

/**
 * This scales the content of the exit transition, from 1f to the target scale defined in
 * [targetScale]. [transformOrigin] defines the pivot point in terms of fraction of the overall
 * size. By default it's [TransformOrigin.Center]. [scaleOut] can be used in combination with any
 * other type of [ExitTransition] using the plus operator (e.g. `scaleOut() + fadeOut()`)
 *
 * Note: Scale is applied __before__ slide. This means when using [slideIn]/[slideOut] with
 * [scaleIn]/[scaleOut], the amount of scaling needs to be taken into account when sliding.
 *
 * The scaling will change the visual of the content, but will __not__ affect the layout size.
 * [scaleOut] can be combined with [shrinkOut]/[shrinkHorizontally]/[shrinkVertically] for
 * coordinated layout size change animation. For example:
 * @sample androidx.compose.animation.samples.ScaledEnterExit
 *
 * @param animationSpec the animation used for the slide-out, [spring] by default.
 * @param targetScale the target scale for the exit transition, 0 by default.
 * @param transformOrigin the pivot point in terms of fraction of the overall size. By default it's
 *                        [TransformOrigin.Center].
 */
@Stable
fun scaleOut(
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    targetScale: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center
): ExitTransition {
    return ExitTransitionImpl(
        TransitionData(scale = Scale(targetScale, transformOrigin, animationSpec))
    )
}

/**
 * This expands the clip bounds of the appearing content from the size returned from [initialSize]
 * to the full size. [expandFrom] controls which part of the content gets revealed first. By
 * default, the clip bounds animates from `IntSize(0, 0)` to full size, starting from revealing the
 * bottom right corner (or bottom left corner in RTL layouts) of the content, to fully revealing
 * the entire content as the size expands.
 *
 * __Note__: [expandIn] animates the bounds of the content. This bounds change will also result
 * in the animation of other layouts that are dependent on this size.
 *
 * [initialSize] is a lambda that takes the full size of the content and returns an initial size of
 * the bounds of the content. This allows not only absolute size, but also an initial size that
 * is proportional to the content size.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * For expanding only horizontally or vertically, consider [expandHorizontally], [expandVertically].
 *
 * @sample androidx.compose.animation.samples.ExpandInShrinkOutSample
 *
 * @param animationSpec the animation used for the expanding animation, [spring] by default.
 * @param expandFrom the starting point of the expanding bounds, [Alignment.BottomEnd] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param initialSize the start size of the expanding bounds, returning `IntSize(0, 0)` by default.
 */
@Stable
fun expandIn(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold
        ),
    expandFrom: Alignment = Alignment.BottomEnd,
    clip: Boolean = true,
    initialSize: (fullSize: IntSize) -> IntSize = { IntSize(0, 0) },
): EnterTransition {
    return EnterTransitionImpl(
        TransitionData(
            changeSize = ChangeSize(expandFrom, initialSize, animationSpec, clip)
        )
    )
}

/**
 * This shrinks the clip bounds of the disappearing content from the full size to the size returned
 * from [targetSize]. [shrinkTowards] controls the direction of the bounds shrink animation. By
 * default, the clip bounds animates from  full size to `IntSize(0, 0)`, shrinking towards the
 * the bottom right corner (or bottom left corner in RTL layouts) of the content.
 *
 * __Note__: [shrinkOut] animates the bounds of the content. This bounds change will also result
 * in the animation of other layouts that are dependent on this size.
 *
 * [targetSize] is a lambda that takes the full size of the content and returns a target size of
 * the bounds of the content. This allows not only absolute size, but also a target size that
 * is proportional to the content size.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * For shrinking only horizontally or vertically, consider [shrinkHorizontally], [shrinkVertically].
 *
 * @sample androidx.compose.animation.samples.ExpandInShrinkOutSample
 *
 * @param animationSpec the animation used for the shrinking animation, [spring] by default.
 * @param shrinkTowards the ending point of the shrinking bounds, [Alignment.BottomEnd] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param targetSize returns the end size of the shrinking bounds, `IntSize(0, 0)` by default.
 */
@Stable
fun shrinkOut(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold
        ),
    shrinkTowards: Alignment = Alignment.BottomEnd,
    clip: Boolean = true,
    targetSize: (fullSize: IntSize) -> IntSize = { IntSize(0, 0) },
): ExitTransition {
    return ExitTransitionImpl(
        TransitionData(
            changeSize = ChangeSize(shrinkTowards, targetSize, animationSpec, clip)
        )
    )
}

/**
 * This expands the clip bounds of the appearing content horizontally, from the width returned from
 * [initialWidth] to the full width. [expandFrom] controls which part of the content gets revealed
 * first. By default, the clip bounds animates from 0 to full width, starting from the end
 * of the content, and expand to fully revealing the whole content.
 *
 * __Note__: [expandHorizontally] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [initialWidth] is a lambda that takes the full width of the content and returns an initial width
 * of the bounds of the content. This allows not only an absolute width, but also an initial width
 * that is proportional to the content width.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.HorizontalTransitionSample
 *
 * @param animationSpec the animation used for the expanding animation, [spring] by default.
 * @param expandFrom the starting point of the expanding bounds, [Alignment.End] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param initialWidth the start width of the expanding bounds, returning 0 by default.
 */
@Stable
fun expandHorizontally(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold
        ),
    expandFrom: Alignment.Horizontal = Alignment.End,
    clip: Boolean = true,
    @Suppress("PrimitiveInLambda")
    initialWidth: (fullWidth: Int) -> Int = { 0 },
): EnterTransition {
    return expandIn(animationSpec, expandFrom.toAlignment(), clip = clip) {
        IntSize(initialWidth(it.width), it.height)
    }
}

/**
 * This expands the clip bounds of the appearing content vertically, from the height returned from
 * [initialHeight] to the full height. [expandFrom] controls which part of the content gets revealed
 * first. By default, the clip bounds animates from 0 to full height, revealing the bottom edge
 * first, followed by the rest of the content.
 *
 * __Note__: [expandVertically] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [initialHeight] is a lambda that takes the full height of the content and returns an initial
 * height of the bounds of the content. This allows not only an absolute height, but also an initial
 * height that is proportional to the content height.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.ExpandShrinkVerticallySample
 *
 * @param animationSpec the animation used for the expanding animation, [spring] by default.
 * @param expandFrom the starting point of the expanding bounds, [Alignment.Bottom] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param initialHeight the start height of the expanding bounds, returning 0 by default.
 */
@Stable
fun expandVertically(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold
        ),
    expandFrom: Alignment.Vertical = Alignment.Bottom,
    clip: Boolean = true,
    @Suppress("PrimitiveInLambda")
    initialHeight: (fullHeight: Int) -> Int = { 0 },
): EnterTransition {
    return expandIn(animationSpec, expandFrom.toAlignment(), clip) {
        IntSize(it.width, initialHeight(it.height))
    }
}

/**
 * This shrinks the clip bounds of the disappearing content horizontally, from the full width to
 * the width returned from [targetWidth]. [shrinkTowards] controls the direction of the bounds
 * shrink animation. By default, the clip bounds animates from full width to 0, shrinking towards
 * the end of the content.
 *
 * __Note__: [shrinkHorizontally] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [targetWidth] is a lambda that takes the full width of the content and returns a target width of
 * the content. This allows not only absolute width, but also a target width that is proportional
 * to the content width.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.HorizontalTransitionSample
 *
 * @param animationSpec the animation used for the shrinking animation, [spring] by default.
 * @param shrinkTowards the ending point of the shrinking bounds, [Alignment.End] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param targetWidth returns the end width of the shrinking bounds, 0 by default.
 */
@Stable
fun shrinkHorizontally(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold
        ),
    shrinkTowards: Alignment.Horizontal = Alignment.End,
    clip: Boolean = true,
    @Suppress("PrimitiveInLambda")
    targetWidth: (fullWidth: Int) -> Int = { 0 }
): ExitTransition {
    // TODO: Support different animation types
    return shrinkOut(animationSpec, shrinkTowards.toAlignment(), clip) {
        IntSize(targetWidth(it.width), it.height)
    }
}

/**
 * This shrinks the clip bounds of the disappearing content vertically, from the full height to
 * the height returned from [targetHeight]. [shrinkTowards] controls the direction of the bounds
 * shrink animation. By default, the clip bounds animates from full height to 0, shrinking towards
 * the bottom of the content.
 *
 * __Note__: [shrinkVertically] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [targetHeight] is a lambda that takes the full height of the content and returns a target height
 * of the content. This allows not only absolute height, but also a target height that is
 * proportional to the content height.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By
 * default, clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.ExpandShrinkVerticallySample
 *
 * @param animationSpec the animation used for the shrinking animation, [spring] by default.
 * @param shrinkTowards the ending point of the shrinking bounds, [Alignment.Bottom] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param targetHeight returns the end height of the shrinking bounds, 0 by default.
 */
@Stable
fun shrinkVertically(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold
        ),
    shrinkTowards: Alignment.Vertical = Alignment.Bottom,
    clip: Boolean = true,
    @Suppress("PrimitiveInLambda")
    targetHeight: (fullHeight: Int) -> Int = { 0 },
): ExitTransition {
    // TODO: Support different animation types
    return shrinkOut(animationSpec, shrinkTowards.toAlignment(), clip) {
        IntSize(it.width, targetHeight(it.height))
    }
}

/**
 * This slides in the content horizontally, from a starting offset defined in [initialOffsetX] to
 * `0` **pixels**. The direction of the slide can be controlled by configuring the
 * [initialOffsetX]. A positive value means sliding from right to left, whereas a negative
 * value would slide the content from left to right.
 *
 * [initialOffsetX] is a lambda that takes the full width of the content and returns an
 * offset. This allows the starting offset to be defined proportional to the full size, or as an
 * absolute value. It defaults to return half of negative width, which would offset the content
 * to the left by half of its width, and slide towards the right.
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 *
 * @param animationSpec the animation used for the slide-in, [spring] by default.
 * @param initialOffsetX a lambda that takes the full width of the content in pixels and returns the
 *                             initial offset for the slide-in, by default it returns `-fullWidth/2`
 */
@Stable
fun slideInHorizontally(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
    @Suppress("PrimitiveInLambda")
    initialOffsetX: (fullWidth: Int) -> Int = { -it / 2 },
): EnterTransition =
    slideIn(
        initialOffset = { IntOffset(initialOffsetX(it.width), 0) },
        animationSpec = animationSpec
    )

/**
 * This slides in the content vertically, from a starting offset defined in [initialOffsetY] to `0`
 * in **pixels**. The direction of the slide can be controlled by configuring the
 * [initialOffsetY]. A positive initial offset means sliding up, whereas a negative value would
 * slide the content down.
 *
 * [initialOffsetY] is a lambda that takes the full Height of the content and returns an
 * offset. This allows the starting offset to be defined proportional to the full height, or as an
 * absolute value. It defaults to return half of negative height, which would offset the content
 * up by half of its Height, and slide down.
 *
 * @sample androidx.compose.animation.samples.FullyLoadedTransition
 *
 * @param animationSpec the animation used for the slide-in, [spring] by default.
 * @param initialOffsetY a lambda that takes the full Height of the content and returns the
 *                           initial offset for the slide-in, by default it returns `-fullHeight/2`
 */
@Stable
fun slideInVertically(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
    @Suppress("PrimitiveInLambda")
    initialOffsetY: (fullHeight: Int) -> Int = { -it / 2 },
): EnterTransition =
    slideIn(
        initialOffset = { IntOffset(0, initialOffsetY(it.height)) },
        animationSpec = animationSpec
    )

/**
 * This slides out the content horizontally, from 0 to a target offset defined in [targetOffsetX]
 * in **pixels**. The direction of the slide can be controlled by configuring the
 * [targetOffsetX]. A positive value means sliding to the right, whereas a negative
 * value would slide the content towards the left.
 *
 * [targetOffsetX] is a lambda that takes the full width of the content and returns an
 * offset. This allows the target offset to be defined proportional to the full size, or as an
 * absolute value. It defaults to return half of negative width, which would slide the content to
 * the left by half of its width.
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 *
 * @param animationSpec the animation used for the slide-out, [spring] by default.
 * @param targetOffsetX a lambda that takes the full width of the content and returns the
 *                             initial offset for the slide-in, by default it returns `fullWidth/2`
 */
@Stable
fun slideOutHorizontally(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
    @Suppress("PrimitiveInLambda")
    targetOffsetX: (fullWidth: Int) -> Int = { -it / 2 },
): ExitTransition =
    slideOut(
        targetOffset = { IntOffset(targetOffsetX(it.width), 0) },
        animationSpec = animationSpec
    )

/**
 * This slides out the content vertically, from 0 to a target offset defined in [targetOffsetY]
 * in **pixels**. The direction of the slide-out can be controlled by configuring the
 * [targetOffsetY]. A positive target offset means sliding down, whereas a negative value would
 * slide the content up.
 *
 * [targetOffsetY] is a lambda that takes the full Height of the content and returns an
 * offset. This allows the target offset to be defined proportional to the full height, or as an
 * absolute value. It defaults to return half of the negative height, which would slide the content
 * up by half of its Height.
 *
 * @param animationSpec the animation used for the slide-out, [spring] by default.
 * @param targetOffsetY a lambda that takes the full Height of the content and returns the
 *                         target offset for the slide-out, by default it returns `fullHeight/2`
 */
@Stable
fun slideOutVertically(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
    @Suppress("PrimitiveInLambda")
    targetOffsetY: (fullHeight: Int) -> Int = { -it / 2 },
): ExitTransition =
    slideOut(
        targetOffset = { IntOffset(0, targetOffsetY(it.height)) },
        animationSpec = animationSpec
    )

/*********************** Below are internal classes and methods ******************/
@Immutable
internal data class Fade(val alpha: Float, val animationSpec: FiniteAnimationSpec<Float>)

@Immutable
internal data class Slide(
    val slideOffset: (fullSize: IntSize) -> IntOffset,
    val animationSpec: FiniteAnimationSpec<IntOffset>
)

@Immutable
internal data class ChangeSize(
    val alignment: Alignment,
    val size: (fullSize: IntSize) -> IntSize = { IntSize(0, 0) },
    val animationSpec: FiniteAnimationSpec<IntSize>,
    val clip: Boolean = true
)

@Immutable
internal data class Scale(
    val scale: Float,
    val transformOrigin: TransformOrigin,
    val animationSpec: FiniteAnimationSpec<Float>
)

@Immutable
private class EnterTransitionImpl(override val data: TransitionData) : EnterTransition()

@Immutable
private class ExitTransitionImpl(override val data: TransitionData) : ExitTransition()

private fun Alignment.Horizontal.toAlignment() =
    when (this) {
        Alignment.Start -> Alignment.CenterStart
        Alignment.End -> Alignment.CenterEnd
        else -> Alignment.Center
    }

private fun Alignment.Vertical.toAlignment() =
    when (this) {
        Alignment.Top -> Alignment.TopCenter
        Alignment.Bottom -> Alignment.BottomCenter
        else -> Alignment.Center
    }

@Immutable
internal data class TransitionData(
    val fade: Fade? = null,
    val slide: Slide? = null,
    val changeSize: ChangeSize? = null,
    val scale: Scale? = null
)

@OptIn(ExperimentalAnimationApi::class, InternalAnimationApi::class)
@Suppress("ModifierFactoryExtensionFunction", "ComposableModifierFactory")
@Composable
internal fun Transition<EnterExitState>.createModifier(
    enter: EnterTransition,
    exit: ExitTransition,
    label: String
): Modifier {

    var shouldAnimateSlide by remember(this) { mutableStateOf(false) }
    var shouldAnimateSizeChange by remember(this) { mutableStateOf(false) }
    // Animate if the enter or exit transition for the type is defined. Once the shouldAnimateFoo
    // is set, it'll stay true until the transition is complete.  This would ensure the removal of
    // any of type animation in the enter/exit amid a transition doesn't result in a
    // jump. Reset shouldAnimateFoo to false when the transition is finished.
    val isTransitioning = currentState != targetState || isSeeking
    shouldAnimateSlide = isTransitioning &&
        (shouldAnimateSlide || enter.data.slide != null || exit.data.slide != null)
    shouldAnimateSizeChange = isTransitioning &&
        (shouldAnimateSizeChange || enter.data.changeSize != null || exit.data.changeSize != null)

    val slideAnimation = if (shouldAnimateSlide) {
        createDeferredAnimation(IntOffset.VectorConverter, remember { "$label slide" })
    } else {
        null
    }
    val sizeAnimation = if (shouldAnimateSizeChange) {
        createDeferredAnimation(IntSize.VectorConverter, remember { "$label shrink/expand" })
    } else null

    val offsetAnimation = if (shouldAnimateSizeChange) {
        createDeferredAnimation(
            IntOffset.VectorConverter,
            remember { "$label InterruptionHandlingOffset" }
        )
    } else null

    val disableClip = (enter.data.changeSize?.clip == false ||
        exit.data.changeSize?.clip == false) || !shouldAnimateSizeChange

    val graphicsLayerBlock = createGraphicsLayerBlock(enter, exit, label)

    return (if (disableClip) Modifier else Modifier.clipToBounds())
        .then(
            EnterExitTransitionElement(
                this, sizeAnimation, offsetAnimation, slideAnimation,
                enter, exit, graphicsLayerBlock
            )
        )
}

@Composable
private fun Transition<EnterExitState>.createGraphicsLayerBlock(
    enter: EnterTransition,
    exit: ExitTransition,
    label: String
): GraphicsLayerScope.() -> Unit {

    var shouldAnimateAlpha by remember(this) { mutableStateOf(false) }
    var shouldAnimateScale by remember(this) { mutableStateOf(false) }

    val isTransitioning = currentState != targetState || isSeeking
    shouldAnimateAlpha = isTransitioning &&
        (shouldAnimateAlpha || enter.data.fade != null || exit.data.fade != null)
    shouldAnimateScale = isTransitioning &&
        (shouldAnimateScale || enter.data.scale != null || exit.data.scale != null)

    // Fade - it's important to put fade in the end. Otherwise fade will clip slide.
    // We'll animate if at any point during the transition fadeIn/fadeOut becomes non-null. This
    // would ensure the removal of fadeIn/Out amid a fade animation doesn't result in a jump.
    val alpha by if (shouldAnimateAlpha) {
        animateFloat(
            transitionSpec = {
                when {
                    EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
                        enter.data.fade?.animationSpec ?: DefaultAlphaAndScaleSpring

                    EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
                        exit.data.fade?.animationSpec ?: DefaultAlphaAndScaleSpring

                    else -> DefaultAlphaAndScaleSpring
                }
            },
            label = remember { "$label alpha" }
        ) {
            when (it) {
                EnterExitState.Visible -> 1f
                EnterExitState.PreEnter -> enter.data.fade?.alpha ?: 1f
                EnterExitState.PostExit -> exit.data.fade?.alpha ?: 1f
            }
        }
    } else {
        DefaultAlpha
    }

    return if (shouldAnimateScale) {
        val scale by animateFloat(
            transitionSpec = {
                when {
                    EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
                        enter.data.scale?.animationSpec ?: DefaultAlphaAndScaleSpring

                    EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
                        exit.data.scale?.animationSpec ?: DefaultAlphaAndScaleSpring

                    else -> DefaultAlphaAndScaleSpring
                }
            },
            label = remember { "$label scale" }
        ) {
            when (it) {
                EnterExitState.Visible -> 1f
                EnterExitState.PreEnter -> enter.data.scale?.scale ?: 1f
                EnterExitState.PostExit -> exit.data.scale?.scale ?: 1f
            }
        }
        val transformOriginWhenVisible =
            if (currentState == EnterExitState.PreEnter) {
                enter.data.scale?.transformOrigin ?: exit.data.scale?.transformOrigin
            } else {
                exit.data.scale?.transformOrigin ?: enter.data.scale?.transformOrigin
            }
        // Animate transform origin if there's any change. If scale is only defined for enter or
        // exit, use the same transform origin for both.
        val transformOrigin by animateValue(
            TransformOriginVectorConverter,
            label = "TransformOriginInterruptionHandling"
        ) {
            when (it) {
                EnterExitState.Visible -> transformOriginWhenVisible
                EnterExitState.PreEnter ->
                    enter.data.scale?.transformOrigin ?: exit.data.scale?.transformOrigin

                EnterExitState.PostExit ->
                    exit.data.scale?.transformOrigin ?: enter.data.scale?.transformOrigin
            } ?: TransformOrigin.Center
        }

        val block: GraphicsLayerScope.() -> Unit = {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
            this.transformOrigin = transformOrigin
        }
        block
    } else if (shouldAnimateAlpha) {
        { this.alpha = alpha }
    } else {
        {}
    }
}

private val TransformOriginVectorConverter =
    TwoWayConverter<TransformOrigin, AnimationVector2D>(
        convertToVector = { AnimationVector2D(it.pivotFractionX, it.pivotFractionY) },
        convertFromVector = { TransformOrigin(it.v1, it.v2) }
    )

private val DefaultAlpha = mutableFloatStateOf(1f)
private val DefaultAlphaAndScaleSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)

private val DefaultOffsetAnimationSpec = spring(
    stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold
)

private class EnterExitTransitionModifierNode(
    var transition: Transition<EnterExitState>,
    var sizeAnimation: Transition<EnterExitState>.DeferredAnimation<IntSize, AnimationVector2D>?,
    var offsetAnimation:
    Transition<EnterExitState>.DeferredAnimation<IntOffset, AnimationVector2D>?,
    var slideAnimation: Transition<EnterExitState>.DeferredAnimation<IntOffset, AnimationVector2D>?,
    var enter: EnterTransition,
    var exit: ExitTransition,
    var graphicsLayerBlock: GraphicsLayerScope.() -> Unit
) : LayoutModifierNodeWithPassThroughIntrinsics() {

    private var lookaheadConstraintsAvailable = false
    private var lookaheadSize: IntSize = InvalidSize
    private var lookaheadConstraints: Constraints = Constraints()
        set(value) {
            lookaheadConstraintsAvailable = true
            field = value
        }
    var currentAlignment: Alignment? = null
    val alignment: Alignment?
        get() = with(transition.segment) {
            if (EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible) {
                enter.data.changeSize?.alignment ?: exit.data.changeSize?.alignment
            } else {
                exit.data.changeSize?.alignment ?: enter.data.changeSize?.alignment
            }
        }

    private fun targetConstraints(default: Constraints) =
        if (lookaheadConstraintsAvailable) lookaheadConstraints else default

    val sizeTransitionSpec: Transition.Segment<EnterExitState>.() -> FiniteAnimationSpec<IntSize> =
        {
            when {
                EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
                    enter.data.changeSize?.animationSpec

                EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
                    exit.data.changeSize?.animationSpec

                else -> DefaultSizeAnimationSpec
            } ?: DefaultSizeAnimationSpec
        }

    fun sizeByState(targetState: EnterExitState, fullSize: IntSize): IntSize = when (targetState) {
        EnterExitState.Visible -> fullSize
        EnterExitState.PreEnter -> enter.data.changeSize?.size?.invoke(fullSize) ?: fullSize
        EnterExitState.PostExit -> exit.data.changeSize?.size?.invoke(fullSize) ?: fullSize
    }

    override fun onAttach() {
        super.onAttach()
        lookaheadConstraintsAvailable = false
        lookaheadSize = InvalidSize
    }

    // This offset is only needed when the alignment value changes during the shrink/expand
    // animation. For example, if user specify an enter that expands from the left, and an exit
    // that shrinks towards the right, the asymmetric enter/exit will be brittle to interruption.
    // Hence the following offset animation to smooth over such interruption.
    fun targetOffsetByState(targetState: EnterExitState, fullSize: IntSize): IntOffset =
        when {
            currentAlignment == null -> IntOffset.Zero
            alignment == null -> IntOffset.Zero
            currentAlignment == alignment -> IntOffset.Zero
            else -> when (targetState) {
                EnterExitState.Visible -> IntOffset.Zero
                EnterExitState.PreEnter -> IntOffset.Zero
                EnterExitState.PostExit -> exit.data.changeSize?.let {
                    val endSize = it.size(fullSize)
                    val targetOffset = alignment!!.align(
                        fullSize,
                        endSize,
                        LayoutDirection.Ltr
                    )
                    val currentOffset = currentAlignment!!.align(
                        fullSize,
                        endSize,
                        LayoutDirection.Ltr
                    )
                    targetOffset - currentOffset
                } ?: IntOffset.Zero
            }
        }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        if (transition.currentState == transition.targetState) {
            currentAlignment = null
        } else if (currentAlignment == null) {
            currentAlignment = alignment ?: Alignment.TopStart
        }
        if (isLookingAhead) {
            val placeable = measurable.measure(constraints)
            val measuredSize = IntSize(placeable.width, placeable.height)
            lookaheadSize = measuredSize
            lookaheadConstraints = constraints
            val sizeToReport = if (transition.targetState == EnterExitState.Visible)
                measuredSize
            else
                IntSize.Zero
            return layout(sizeToReport.width, sizeToReport.height) {
                placeable.place(0, 0)
            }
        } else {
            val placeable = measurable.measure(targetConstraints(constraints))
            val measuredSize = IntSize(placeable.width, placeable.height)
            val target = if (lookaheadSize.isValid) lookaheadSize else measuredSize
            val animSize = sizeAnimation?.animate(sizeTransitionSpec) { sizeByState(it, target) }
            // Since we measure with lookahead constraints when available, the size needs to
            // be constrained by incoming constraints so that we know how to position content
            // in the constrained rect based on alignment.
            val currentSize = constraints.constrain(animSize?.value ?: measuredSize)
            val offsetDelta = offsetAnimation?.animate({ DefaultOffsetAnimationSpec }) {
                targetOffsetByState(it, target)
            }?.value ?: IntOffset.Zero
            val slideOffset = slideAnimation?.animate(slideSpec) {
                slideTargetValueByState(it, target)
            }?.value ?: IntOffset.Zero
            val offset = (currentAlignment?.align(target, currentSize, LayoutDirection.Ltr)
                ?: IntOffset.Zero) + slideOffset
            return layout(currentSize.width, currentSize.height) {
                placeable.placeWithLayer(
                    offset.x + offsetDelta.x, offset.y + offsetDelta.y, 0f, graphicsLayerBlock
                )
            }
        }
    }

    val slideSpec: Transition.Segment<EnterExitState>.() -> FiniteAnimationSpec<IntOffset> = {
        when {
            EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible -> {
                enter.data.slide?.animationSpec ?: DefaultOffsetAnimationSpec
            }

            EnterExitState.Visible isTransitioningTo EnterExitState.PostExit -> {
                exit.data.slide?.animationSpec ?: DefaultOffsetAnimationSpec
            }

            else -> DefaultOffsetAnimationSpec
        }
    }

    fun slideTargetValueByState(targetState: EnterExitState, fullSize: IntSize): IntOffset {
        val preEnter = enter.data.slide?.slideOffset?.invoke(fullSize) ?: IntOffset.Zero
        val postExit = exit.data.slide?.slideOffset?.invoke(fullSize) ?: IntOffset.Zero
        return when (targetState) {
            EnterExitState.Visible -> IntOffset.Zero
            EnterExitState.PreEnter -> preEnter
            EnterExitState.PostExit -> postExit
        }
    }
}

private val DefaultSizeAnimationSpec = spring(
    stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold
)

private data class EnterExitTransitionElement(
    val transition: Transition<EnterExitState>,
    var sizeAnimation: Transition<EnterExitState>.DeferredAnimation<IntSize, AnimationVector2D>?,
    var offsetAnimation:
    Transition<EnterExitState>.DeferredAnimation<IntOffset, AnimationVector2D>?,
    var slideAnimation: Transition<EnterExitState>.DeferredAnimation<IntOffset, AnimationVector2D>?,
    var enter: EnterTransition,
    var exit: ExitTransition,
    var graphicsLayerBlock: GraphicsLayerScope.() -> Unit
) : ModifierNodeElement<EnterExitTransitionModifierNode>() {
    override fun create(): EnterExitTransitionModifierNode =
        EnterExitTransitionModifierNode(
            transition, sizeAnimation, offsetAnimation, slideAnimation, enter, exit,
            graphicsLayerBlock
        )

    override fun update(node: EnterExitTransitionModifierNode) {
        node.transition = transition
        node.sizeAnimation = sizeAnimation
        node.offsetAnimation = offsetAnimation
        node.slideAnimation = slideAnimation
        node.enter = enter
        node.exit = exit
        node.graphicsLayerBlock = graphicsLayerBlock
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "enterExitTransition"
        properties["transition"] = transition
        properties["sizeAnimation"] = sizeAnimation
        properties["offsetAnimation"] = offsetAnimation
        properties["slideAnimation"] = slideAnimation
        properties["enter"] = enter
        properties["exit"] = exit
        properties["graphicsLayerBlock"] = graphicsLayerBlock
    }
}