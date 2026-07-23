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

@file:OptIn(ExperimentalAnimationApi::class, ExperimentalDeferredTransitionApi::class)

package androidx.compose.animation

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.createDeferredAnimation
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain

private val NeutralSlideOffset: (IntSize) -> IntOffset = { IntOffset.Zero }
private val NeutralChangeSize: (IntSize) -> IntSize = { it }

@RequiresOptIn(message = "This is an experimental animation API.")
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalAnimationApi

/**
 * [EnterTransition] defines how an [AnimatedVisibility] Composable appears on screen as it becomes
 * visible. The 4 categories of EnterTransitions available are:
 * 1. fade: [fadeIn]
 * 2. scale: [scaleIn]
 * 3. slide: [slideIn], [slideInHorizontally], [slideInVertically]
 * 4. expand: [expandIn], [expandHorizontally], [expandVertically]
 *
 * [EnterTransition.None] can be used when no enter transition is desired. Different
 * [EnterTransition]s can be combined using plus operator, for example:
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
public sealed class EnterTransition {
    /**
     * The underlying transition configuration containing the specs for fade, slide, scale, expand,
     * and veil animations.
     */
    public abstract val config: EnterExitTransitionConfig

    /**
     * Combines different enter transitions. The order of the [EnterTransition]s being combined does
     * not matter, as these [EnterTransition]s will start simultaneously. The order of applying
     * transforms from these enter transitions (if defined) is: veil first, then alpha and scale,
     * shrink or expand, then slide.
     *
     * @sample androidx.compose.animation.samples.FullyLoadedTransition
     * @param enter another [EnterTransition] to be combined
     */
    @Stable
    public operator fun plus(enter: EnterTransition): EnterTransition {
        if (this == None) return enter
        if (enter == None) return this
        return EnterTransitionImpl(
            EnterExitTransitionConfig(
                fade = enter.config.fade ?: config.fade,
                slide = enter.config.slide ?: config.slide,
                changeSize = enter.config.changeSize ?: config.changeSize,
                scale = enter.config.scale ?: config.scale,
                veil = enter.config.veil ?: config.veil,
                // `enter` after plus operator to prioritize its values on the map
                effectsMap = config.effectsMap + enter.config.effectsMap,
            )
        )
    }

    override fun toString(): String =
        if (this == None) {
            "EnterTransition.None"
        } else {
            config.run {
                "EnterTransition: " +
                    "Fade - " +
                    fade?.toString() +
                    ", Slide - " +
                    slide?.toString() +
                    ", Shrink - " +
                    changeSize?.toString() +
                    ", Scale - " +
                    scale?.toString() +
                    ", Veil - " +
                    veil?.toString()
            }
        }

    override fun equals(other: Any?): Boolean {
        return other is EnterTransition && other.config == config
    }

    override fun hashCode(): Int = config.hashCode()

    public companion object {
        /**
         * This can be used when no enter transition is desired. It can be useful in cases where
         * there are other forms of enter animation defined indirectly for an [AnimatedVisibility].
         * e.g.The children of the [AnimatedVisibility] have all defined their own
         * [EnterTransition], or when the parent is fading in, etc.
         *
         * @see [ExitTransition.None]
         */
        public val None: EnterTransition = EnterTransitionImpl(EnterExitTransitionConfig())
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
 * [ExitTransition.None] can be used when no exit transition is desired. Different [ExitTransition]s
 * can be combined using plus operator, for example:
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 *
 * __Note__: [fadeOut] and [slideOut] do not affect the size of the [AnimatedVisibility] composable.
 * In contrast, [shrinkOut] (and [shrinkHorizontally], [shrinkVertically]) will shrink the clip
 * bounds to reveal less and less of the content. This will automatically animate other layouts to
 * fill in the space, very much like [animateContentSize].
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
public sealed class ExitTransition {
    /**
     * The underlying transition configuration containing the specs for fade, slide, scale, shrink,
     * and veil animations.
     */
    public abstract val config: EnterExitTransitionConfig

    /**
     * Combines different exit transitions. The order of the [ExitTransition]s being combined does
     * not matter, as these [ExitTransition]s will start simultaneously. The order of applying
     * transforms from these exit transitions (if defined) is: veil first, then alpha and scale,
     * shrink or expand, then slide.
     *
     * @sample androidx.compose.animation.samples.FullyLoadedTransition
     * @param exit another [ExitTransition] to be combined.
     */
    @Stable
    public operator fun plus(exit: ExitTransition): ExitTransition {
        if (this == None) return exit
        if (exit == None) return this
        return ExitTransitionImpl(
            EnterExitTransitionConfig(
                fade = exit.config.fade ?: config.fade,
                slide = exit.config.slide ?: config.slide,
                changeSize = exit.config.changeSize ?: config.changeSize,
                scale = exit.config.scale ?: config.scale,
                veil = exit.config.veil ?: config.veil,
                hold = exit.config.hold || config.hold,
                // `exit` after plus operator to prioritize its values on the map
                effectsMap = config.effectsMap + exit.config.effectsMap,
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is ExitTransition && other.config == config
    }

    override fun toString(): String =
        when (this) {
            None -> "ExitTransition.None"
            KeepUntilTransitionsFinished -> "ExitTransition.KeepUntilTransitionsFinished"
            else ->
                config.run {
                    "ExitTransition:  " +
                        "Fade - " +
                        fade?.toString() +
                        ",  Slide - " +
                        slide?.toString() +
                        ",  Shrink - " +
                        changeSize?.toString() +
                        ",  Scale - " +
                        scale?.toString() +
                        ",  Veil - " +
                        veil?.toString() +
                        ",  KeepUntilTransitionsFinished - " +
                        hold
                }
        }

    override fun hashCode(): Int = config.hashCode()

    public companion object {
        /**
         * This can be used when no built-in [ExitTransition] (i.e. fade/slide, etc) is desired for
         * the [AnimatedVisibility], but rather the children are defining their own exit animation
         * using the [Transition] scope.
         *
         * __Note:__ If [None] is used, and nothing is animating in the Transition<EnterExitState>
         * scope that [AnimatedVisibility] provided, the content will be removed from
         * [AnimatedVisibility] right away.
         *
         * @sample androidx.compose.animation.samples.AVScopeAnimateEnterExit
         */
        public val None: ExitTransition = ExitTransitionImpl(EnterExitTransitionConfig())

        /**
         * Keep this type of exit transition internal and only expose it in AnimatedContent, as
         * holding only makes sense when there's enter and exit at the same time. In other words,
         * when dealing with one set of content entering OR exiting, such as AnimatedVisibility,
         * holding would not be meaningful.
         */
        internal val KeepUntilTransitionsFinished: ExitTransition =
            ExitTransitionImpl(EnterExitTransitionConfig(hold = true))
    }
}

internal sealed class TransitionEffect {
    internal abstract val key: TransitionEffectKey<*>
}

internal interface TransitionEffectKey<E : TransitionEffect>

internal data class ContentScaleTransitionEffect(
    val contentScale: ContentScale,
    val alignment: Alignment,
) : TransitionEffect() {
    companion object Key : TransitionEffectKey<ContentScaleTransitionEffect>

    override val key: TransitionEffectKey<*>
        get() = Key
}

internal infix fun EnterTransition.withEffect(effect: TransitionEffect): EnterTransition =
    EnterTransitionImpl(EnterExitTransitionConfig(effectsMap = mapOf(effect.key to effect)))

internal infix fun ExitTransition.withEffect(effect: TransitionEffect): ExitTransition =
    ExitTransitionImpl(EnterExitTransitionConfig(effectsMap = mapOf(effect.key to effect)))

/**
 * This fades in the content of the transition, from the specified starting alpha (i.e.
 * [initialAlpha]) to 1f, using the supplied [animationSpec]. [initialAlpha] defaults to 0f, and
 * [spring] is used by default.
 *
 * @sample androidx.compose.animation.samples.FadeTransition
 * @param animationSpec the [FiniteAnimationSpec] for this animation, [spring] by default
 * @param initialAlpha the starting alpha of the enter transition, 0f by default
 */
@Stable
public fun fadeIn(
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    initialAlpha: Float = 0f,
): EnterTransition {
    return EnterTransitionImpl(
        EnterExitTransitionConfig(fade = FadeConfig(initialAlpha, animationSpec))
    )
}

/**
 * This fades out the content of the transition, from full opacity to the specified target alpha
 * (i.e. [targetAlpha]), using the supplied [animationSpec]. By default, the content will be faded
 * out to fully transparent (i.e. [targetAlpha] defaults to 0), and [animationSpec] uses [spring] by
 * default.
 *
 * @sample androidx.compose.animation.samples.FadeTransition
 * @param animationSpec the [FiniteAnimationSpec] for this animation, [spring] by default
 * @param targetAlpha the target alpha of the exit transition, 0f by default
 */
@Stable
public fun fadeOut(
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    targetAlpha: Float = 0f,
): ExitTransition {
    return ExitTransitionImpl(
        EnterExitTransitionConfig(fade = FadeConfig(targetAlpha, animationSpec))
    )
}

/**
 * This slides in the content of the transition, from a starting offset defined in [initialOffset]
 * to `IntOffset(0, 0)`. The direction of the slide can be controlled by configuring the
 * [initialOffset]. A positive x value means sliding from right to left, whereas a negative x value
 * will slide the content to the right. Similarly positive and negative y values correspond to
 * sliding up and down, respectively.
 *
 * If the sliding is only desired horizontally or vertically, instead of along both axis, consider
 * using [slideInHorizontally] or [slideInVertically].
 *
 * [initialOffset] is a lambda that takes the full size of the content and returns an offset. This
 * allows the offset to be defined proportional to the full size, or as an absolute value.
 *
 * @sample androidx.compose.animation.samples.SlideInOutSample
 * @param animationSpec the animation used for the slide-in, [spring] by default.
 * @param initialOffset a lambda that takes the full size of the content and returns the initial
 *   offset for the slide-in
 */
@Stable
public fun slideIn(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
    initialOffset: (fullSize: IntSize) -> IntOffset,
): EnterTransition {
    return EnterTransitionImpl(
        EnterExitTransitionConfig(slide = SlideConfig(initialOffset, animationSpec))
    )
}

/**
 * This slides out the content of the transition, from an offset of `IntOffset(0, 0)` to the target
 * offset defined in [targetOffset]. The direction of the slide can be controlled by configuring the
 * [targetOffset]. A positive x value means sliding from left to right, whereas a negative x value
 * would slide the content from right to left. Similarly, positive and negative y values correspond
 * to sliding down and up, respectively.
 *
 * If the sliding is only desired horizontally or vertically, instead of along both axis, consider
 * using [slideOutHorizontally] or [slideOutVertically].
 *
 * [targetOffset] is a lambda that takes the full size of the content and returns an offset. This
 * allows the offset to be defined proportional to the full size, or as an absolute value.
 *
 * @sample androidx.compose.animation.samples.SlideInOutSample
 * @param animationSpec the animation used for the slide-out, [spring] by default.
 * @param targetOffset a lambda that takes the full size of the content and returns the target
 *   offset for the slide-out
 */
@Stable
public fun slideOut(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
    targetOffset: (fullSize: IntSize) -> IntOffset,
): ExitTransition {
    return ExitTransitionImpl(
        EnterExitTransitionConfig(slide = SlideConfig(targetOffset, animationSpec))
    )
}

/**
 * This scales the content as it appears, from an initial scale (defined in [initialScale]) to 1f.
 * [transformOrigin] defines the pivot point in terms of fraction of the overall size.
 * [TransformOrigin.Center] by default. [scaleIn] can be used in combination with any other type of
 * [EnterTransition] using the plus operator (e.g. `scaleIn() + slideInHorizontally()`)
 *
 * Note: Scale is applied __before__ slide. This means when using [slideIn]/[slideOut] with
 * [scaleIn]/[scaleOut], the amount of scaling needs to be taken into account when sliding.
 *
 * The scaling will change the visual of the content, but will __not__ affect the layout size.
 * [scaleIn] can be combined with [expandIn]/[expandHorizontally]/[expandVertically] to coordinate
 * layout size change while scaling. For example:
 *
 * @sample androidx.compose.animation.samples.ScaledEnterExit
 * @param animationSpec the animation used for the scale-out, [spring] by default.
 * @param initialScale the initial scale for the enter transition, 0 by default.
 * @param transformOrigin the pivot point in terms of fraction of the overall size. By default it's
 *   [TransformOrigin.Center].
 */
@Stable
public fun scaleIn(
    animationSpec: FiniteAnimationSpec<Float> = DefaultScaleSpring,
    initialScale: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
): EnterTransition {
    return EnterTransitionImpl(
        EnterExitTransitionConfig(scale = ScaleConfig(initialScale, transformOrigin, animationSpec))
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
 *
 * @sample androidx.compose.animation.samples.ScaledEnterExit
 * @param animationSpec the animation used for the slide-out, [spring] by default.
 * @param targetScale the target scale for the exit transition, 0 by default.
 * @param transformOrigin the pivot point in terms of fraction of the overall size. By default it's
 *   [TransformOrigin.Center].
 */
@Stable
public fun scaleOut(
    animationSpec: FiniteAnimationSpec<Float> = DefaultScaleSpring,
    targetScale: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
): ExitTransition {
    return ExitTransitionImpl(
        EnterExitTransitionConfig(scale = ScaleConfig(targetScale, transformOrigin, animationSpec))
    )
}

/**
 * This animates an unveiling scrim over the content as it enters.
 *
 * @sample androidx.compose.animation.samples.AnimatedContentVeil
 * @sample androidx.compose.animation.samples.AnimatedVisibilityVeil
 * @param animationSpec the animation used for the scrim, [spring] by default.
 * @param initialColor the starting color of the scrim.
 * @param matchParentSize whether the scrim should match the parent size. When [matchParentSize] is
 *   true, the veil is applied independently from all other transforms and matches the parent size.
 *   When [matchParentSize] is false, the veil is applied first and thus is affected by other
 *   transforms. Note: The veil may be clipped if a clip modifier is used on the same layout as the
 *   EnterTransition, even when [matchParentSize] is true.
 */
@Stable
public fun unveilIn(
    animationSpec: FiniteAnimationSpec<Color> = spring(stiffness = Spring.StiffnessMediumLow),
    initialColor: Color = Color.Black.copy(alpha = 0.5f),
    matchParentSize: Boolean = false,
): EnterTransition {
    return EnterTransitionImpl(
        EnterExitTransitionConfig(
            veil =
                VeilConfig(
                    initialColor,
                    initialColor.copy(alpha = 0f),
                    animationSpec,
                    matchParentSize,
                )
        )
    )
}

/**
 * This animates a veiling scrim over the content as it exits.
 *
 * @sample androidx.compose.animation.samples.AnimatedContentVeil
 * @sample androidx.compose.animation.samples.AnimatedVisibilityVeil
 * @param animationSpec the animation used for the scrim, [spring] by default.
 * @param targetColor the target color of the scrim.
 * @param matchParentSize whether the scrim should match the parent size. When [matchParentSize] is
 *   true, the veil is applied independently from all other transforms and matches the parent size.
 *   When [matchParentSize] is false, the veil is applied first and thus is affected by other
 *   transforms. Note: The veil may be clipped if a clip modifier is used on the same layout as the
 *   ExitTransition, even when [matchParentSize] is true.
 */
@Stable
public fun veilOut(
    animationSpec: FiniteAnimationSpec<Color> = spring(stiffness = Spring.StiffnessMediumLow),
    targetColor: Color = Color.Black.copy(alpha = 0.5f),
    matchParentSize: Boolean = false,
): ExitTransition {
    return ExitTransitionImpl(
        EnterExitTransitionConfig(
            veil =
                VeilConfig(
                    targetColor.copy(alpha = 0f),
                    targetColor,
                    animationSpec,
                    matchParentSize,
                )
        )
    )
}

/**
 * This expands the clip bounds of the appearing content from the size returned from [initialSize]
 * to the full size. [expandFrom] controls which part of the content gets revealed first. By
 * default, the clip bounds animates from `IntSize(0, 0)` to full size, starting from revealing the
 * bottom right corner (or bottom left corner in RTL layouts) of the content, to fully revealing the
 * entire content as the size expands.
 *
 * __Note__: [expandIn] animates the bounds of the content. This bounds change will also result in
 * the animation of other layouts that are dependent on this size.
 *
 * [initialSize] is a lambda that takes the full size of the content and returns an initial size of
 * the bounds of the content. This allows not only absolute size, but also an initial size that is
 * proportional to the content size.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By default,
 * clip is set to true, which only shows content in the animated bounds.
 *
 * For expanding only horizontally or vertically, consider [expandHorizontally], [expandVertically].
 *
 * @sample androidx.compose.animation.samples.ExpandInShrinkOutSample
 * @param animationSpec the animation used for the expanding animation, [spring] by default.
 * @param expandFrom the starting point of the expanding bounds, [Alignment.BottomEnd] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param initialSize the start size of the expanding bounds, returning `IntSize(0, 0)` by default.
 */
@Stable
public fun expandIn(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold,
        ),
    expandFrom: Alignment = Alignment.BottomEnd,
    clip: Boolean = true,
    initialSize: (fullSize: IntSize) -> IntSize = { IntSize(0, 0) },
): EnterTransition {
    return EnterTransitionImpl(
        EnterExitTransitionConfig(
            changeSize = ChangeSizeConfig(expandFrom, initialSize, animationSpec, clip)
        )
    )
}

/**
 * This shrinks the clip bounds of the disappearing content from the full size to the size returned
 * from [targetSize]. [shrinkTowards] controls the direction of the bounds shrink animation. By
 * default, the clip bounds animates from full size to `IntSize(0, 0)`, shrinking towards the the
 * bottom right corner (or bottom left corner in RTL layouts) of the content.
 *
 * __Note__: [shrinkOut] animates the bounds of the content. This bounds change will also result in
 * the animation of other layouts that are dependent on this size.
 *
 * [targetSize] is a lambda that takes the full size of the content and returns a target size of the
 * bounds of the content. This allows not only absolute size, but also a target size that is
 * proportional to the content size.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By default,
 * clip is set to true, which only shows content in the animated bounds.
 *
 * For shrinking only horizontally or vertically, consider [shrinkHorizontally], [shrinkVertically].
 *
 * @sample androidx.compose.animation.samples.ExpandInShrinkOutSample
 * @param animationSpec the animation used for the shrinking animation, [spring] by default.
 * @param shrinkTowards the ending point of the shrinking bounds, [Alignment.BottomEnd] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param targetSize returns the end size of the shrinking bounds, `IntSize(0, 0)` by default.
 */
@Stable
public fun shrinkOut(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold,
        ),
    shrinkTowards: Alignment = Alignment.BottomEnd,
    clip: Boolean = true,
    targetSize: (fullSize: IntSize) -> IntSize = { IntSize(0, 0) },
): ExitTransition {
    return ExitTransitionImpl(
        EnterExitTransitionConfig(
            changeSize = ChangeSizeConfig(shrinkTowards, targetSize, animationSpec, clip)
        )
    )
}

/**
 * This expands the clip bounds of the appearing content horizontally, from the width returned from
 * [initialWidth] to the full width. [expandFrom] controls which part of the content gets revealed
 * first. By default, the clip bounds animates from 0 to full width, starting from the end of the
 * content, and expand to fully revealing the whole content.
 *
 * __Note__: [expandHorizontally] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [initialWidth] is a lambda that takes the full width of the content and returns an initial width
 * of the bounds of the content. This allows not only an absolute width, but also an initial width
 * that is proportional to the content width.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By default,
 * clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.HorizontalTransitionSample
 * @param animationSpec the animation used for the expanding animation, [spring] by default.
 * @param expandFrom the starting point of the expanding bounds, [Alignment.End] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param initialWidth the start width of the expanding bounds, returning 0 by default.
 */
@Stable
public fun expandHorizontally(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold,
        ),
    expandFrom: Alignment.Horizontal = Alignment.End,
    clip: Boolean = true,
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
 * [clip] defines whether the content outside of the animated bounds should be clipped. By default,
 * clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.ExpandShrinkVerticallySample
 * @param animationSpec the animation used for the expanding animation, [spring] by default.
 * @param expandFrom the starting point of the expanding bounds, [Alignment.Bottom] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param initialHeight the start height of the expanding bounds, returning 0 by default.
 */
@Stable
public fun expandVertically(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold,
        ),
    expandFrom: Alignment.Vertical = Alignment.Bottom,
    clip: Boolean = true,
    initialHeight: (fullHeight: Int) -> Int = { 0 },
): EnterTransition {
    return expandIn(animationSpec, expandFrom.toAlignment(), clip) {
        IntSize(it.width, initialHeight(it.height))
    }
}

/**
 * This shrinks the clip bounds of the disappearing content horizontally, from the full width to the
 * width returned from [targetWidth]. [shrinkTowards] controls the direction of the bounds shrink
 * animation. By default, the clip bounds animates from full width to 0, shrinking towards the end
 * of the content.
 *
 * __Note__: [shrinkHorizontally] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [targetWidth] is a lambda that takes the full width of the content and returns a target width of
 * the content. This allows not only absolute width, but also a target width that is proportional to
 * the content width.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By default,
 * clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.HorizontalTransitionSample
 * @param animationSpec the animation used for the shrinking animation, [spring] by default.
 * @param shrinkTowards the ending point of the shrinking bounds, [Alignment.End] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param targetWidth returns the end width of the shrinking bounds, 0 by default.
 */
@Stable
public fun shrinkHorizontally(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold,
        ),
    shrinkTowards: Alignment.Horizontal = Alignment.End,
    clip: Boolean = true,
    targetWidth: (fullWidth: Int) -> Int = { 0 },
): ExitTransition {
    // TODO: Support different animation types
    return shrinkOut(animationSpec, shrinkTowards.toAlignment(), clip) {
        IntSize(targetWidth(it.width), it.height)
    }
}

/**
 * This shrinks the clip bounds of the disappearing content vertically, from the full height to the
 * height returned from [targetHeight]. [shrinkTowards] controls the direction of the bounds shrink
 * animation. By default, the clip bounds animates from full height to 0, shrinking towards the
 * bottom of the content.
 *
 * __Note__: [shrinkVertically] animates the bounds of the content. This bounds change will also
 * result in the animation of other layouts that are dependent on this size.
 *
 * [targetHeight] is a lambda that takes the full height of the content and returns a target height
 * of the content. This allows not only absolute height, but also a target height that is
 * proportional to the content height.
 *
 * [clip] defines whether the content outside of the animated bounds should be clipped. By default,
 * clip is set to true, which only shows content in the animated bounds.
 *
 * @sample androidx.compose.animation.samples.ExpandShrinkVerticallySample
 * @param animationSpec the animation used for the shrinking animation, [spring] by default.
 * @param shrinkTowards the ending point of the shrinking bounds, [Alignment.Bottom] by default.
 * @param clip whether the content outside of the animated bounds should be clipped, true by default
 * @param targetHeight returns the end height of the shrinking bounds, 0 by default.
 */
@Stable
public fun shrinkVertically(
    animationSpec: FiniteAnimationSpec<IntSize> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize.VisibilityThreshold,
        ),
    shrinkTowards: Alignment.Vertical = Alignment.Bottom,
    clip: Boolean = true,
    targetHeight: (fullHeight: Int) -> Int = { 0 },
): ExitTransition {
    // TODO: Support different animation types
    return shrinkOut(animationSpec, shrinkTowards.toAlignment(), clip) {
        IntSize(it.width, targetHeight(it.height))
    }
}

/**
 * This slides in the content horizontally, from a starting offset defined in [initialOffsetX] to
 * `0` **pixels**. The direction of the slide can be controlled by configuring the [initialOffsetX].
 * A positive value means sliding from right to left, whereas a negative value would slide the
 * content from left to right.
 *
 * [initialOffsetX] is a lambda that takes the full width of the content and returns an offset. This
 * allows the starting offset to be defined proportional to the full size, or as an absolute value.
 * It defaults to return half of negative width, which would offset the content to the left by half
 * of its width, and slide towards the right.
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 * @param animationSpec the animation used for the slide-in, [spring] by default.
 * @param initialOffsetX a lambda that takes the full width of the content in pixels and returns the
 *   initial offset for the slide-in, by default it returns `-fullWidth/2`
 */
@Stable
public fun slideInHorizontally(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
    initialOffsetX: (fullWidth: Int) -> Int = { -it / 2 },
): EnterTransition =
    slideIn(
        initialOffset = { IntOffset(initialOffsetX(it.width), 0) },
        animationSpec = animationSpec,
    )

/**
 * This slides in the content vertically, from a starting offset defined in [initialOffsetY] to `0`
 * in **pixels**. The direction of the slide can be controlled by configuring the [initialOffsetY].
 * A positive initial offset means sliding up, whereas a negative value would slide the content
 * down.
 *
 * [initialOffsetY] is a lambda that takes the full Height of the content and returns an offset.
 * This allows the starting offset to be defined proportional to the full height, or as an absolute
 * value. It defaults to return half of negative height, which would offset the content up by half
 * of its Height, and slide down.
 *
 * @sample androidx.compose.animation.samples.FullyLoadedTransition
 * @param animationSpec the animation used for the slide-in, [spring] by default.
 * @param initialOffsetY a lambda that takes the full Height of the content and returns the initial
 *   offset for the slide-in, by default it returns `-fullHeight/2`
 */
@Stable
public fun slideInVertically(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
    initialOffsetY: (fullHeight: Int) -> Int = { -it / 2 },
): EnterTransition =
    slideIn(
        initialOffset = { IntOffset(0, initialOffsetY(it.height)) },
        animationSpec = animationSpec,
    )

/**
 * This slides out the content horizontally, from 0 to a target offset defined in [targetOffsetX] in
 * **pixels**. The direction of the slide can be controlled by configuring the [targetOffsetX]. A
 * positive value means sliding to the right, whereas a negative value would slide the content
 * towards the left.
 *
 * [targetOffsetX] is a lambda that takes the full width of the content and returns an offset. This
 * allows the target offset to be defined proportional to the full size, or as an absolute value. It
 * defaults to return half of negative width, which would slide the content to the left by half of
 * its width.
 *
 * @sample androidx.compose.animation.samples.SlideTransition
 * @param animationSpec the animation used for the slide-out, [spring] by default.
 * @param targetOffsetX a lambda that takes the full width of the content and returns the initial
 *   offset for the slide-in, by default it returns `fullWidth/2`
 */
@Stable
public fun slideOutHorizontally(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
    targetOffsetX: (fullWidth: Int) -> Int = { -it / 2 },
): ExitTransition =
    slideOut(
        targetOffset = { IntOffset(targetOffsetX(it.width), 0) },
        animationSpec = animationSpec,
    )

/**
 * This slides out the content vertically, from 0 to a target offset defined in [targetOffsetY] in
 * **pixels**. The direction of the slide-out can be controlled by configuring the [targetOffsetY].
 * A positive target offset means sliding down, whereas a negative value would slide the content up.
 *
 * [targetOffsetY] is a lambda that takes the full Height of the content and returns an offset. This
 * allows the target offset to be defined proportional to the full height, or as an absolute value.
 * It defaults to return half of the negative height, which would slide the content up by half of
 * its Height.
 *
 * @param animationSpec the animation used for the slide-out, [spring] by default.
 * @param targetOffsetY a lambda that takes the full Height of the content and returns the target
 *   offset for the slide-out, by default it returns `fullHeight/2`
 */
@Stable
public fun slideOutVertically(
    animationSpec: FiniteAnimationSpec<IntOffset> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
    targetOffsetY: (fullHeight: Int) -> Int = { -it / 2 },
): ExitTransition =
    slideOut(
        targetOffset = { IntOffset(0, targetOffsetY(it.height)) },
        animationSpec = animationSpec,
    )

/**
 * Configuration parameters for the fade effect of an [EnterTransition] or [ExitTransition].
 *
 * @property alpha The initial value for EnterTransition, or the target value for ExitTransition.
 * @property animationSpec The [FiniteAnimationSpec] used for the fade animation.
 * @sample androidx.compose.animation.samples.EnterExitTransitionConfigSample
 */
@Immutable
public class FadeConfig
internal constructor(
    public val alpha: Float,
    public val animationSpec: FiniteAnimationSpec<Float>,
) {
    internal fun copy(
        alpha: Float = this.alpha,
        animationSpec: FiniteAnimationSpec<Float> = this.animationSpec,
    ): FadeConfig = FadeConfig(alpha, animationSpec)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FadeConfig) return false
        return alpha == other.alpha && animationSpec == other.animationSpec
    }

    override fun hashCode(): Int {
        var result = alpha.hashCode()
        result = 31 * result + animationSpec.hashCode()
        return result
    }

    override fun toString(): String = "FadeConfig(alpha=$alpha, animationSpec=$animationSpec)"
}

/**
 * Configuration parameters for the slide effect of an [EnterTransition] or [ExitTransition].
 *
 * @property slideOffset Lambda that calculates the slide offset vector based on the container size.
 * @property animationSpec The [FiniteAnimationSpec] used for the slide animation.
 * @sample androidx.compose.animation.samples.EnterExitTransitionConfigSample
 */
@Immutable
public class SlideConfig
internal constructor(
    public val slideOffset: (fullSize: IntSize) -> IntOffset,
    public val animationSpec: FiniteAnimationSpec<IntOffset>,
) {
    internal fun copy(
        slideOffset: (fullSize: IntSize) -> IntOffset = this.slideOffset,
        animationSpec: FiniteAnimationSpec<IntOffset> = this.animationSpec,
    ): SlideConfig = SlideConfig(slideOffset, animationSpec)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SlideConfig) return false
        return slideOffset === other.slideOffset && animationSpec == other.animationSpec
    }

    override fun hashCode(): Int {
        var result = slideOffset.hashCode()
        result = 31 * result + animationSpec.hashCode()
        return result
    }

    override fun toString(): String =
        "SlideConfig(slideOffset=$slideOffset, animationSpec=$animationSpec)"
}

/**
 * Configuration parameters for the size change (expand/shrink) effect of an [EnterTransition] or
 * [ExitTransition].
 *
 * @property alignment The [Alignment] used to align the content inside the changing boundary.
 * @property size Lambda that calculates the initial size for EnterTransition, or target size for
 *   ExitTransition based on the full container size.
 * @property animationSpec The [FiniteAnimationSpec] used for the size animation.
 * @property clip If true, the content will be clipped to the animated size boundary.
 * @sample androidx.compose.animation.samples.EnterExitTransitionConfigSample
 */
@Immutable
public class ChangeSizeConfig
internal constructor(
    public val alignment: Alignment,
    public val size: (fullSize: IntSize) -> IntSize = { IntSize(0, 0) },
    public val animationSpec: FiniteAnimationSpec<IntSize>,
    @get:Suppress("GetterSetterNames") public val clip: Boolean = true,
) {
    internal fun copy(
        alignment: Alignment = this.alignment,
        size: (fullSize: IntSize) -> IntSize = this.size,
        animationSpec: FiniteAnimationSpec<IntSize> = this.animationSpec,
        clip: Boolean = this.clip,
    ): ChangeSizeConfig = ChangeSizeConfig(alignment, size, animationSpec, clip)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChangeSizeConfig) return false
        return alignment == other.alignment &&
            size === other.size &&
            animationSpec == other.animationSpec &&
            clip == other.clip
    }

    override fun hashCode(): Int {
        var result = alignment.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + animationSpec.hashCode()
        result = 31 * result + clip.hashCode()
        return result
    }

    override fun toString(): String =
        "ChangeSizeConfig(alignment=$alignment, size=$size, animationSpec=$animationSpec, clip=$clip)"
}

/**
 * Configuration parameters for the scale effect of an [EnterTransition] or [ExitTransition].
 *
 * @property scale The initial scale value for EnterTransition, or the target scale value for
 *   ExitTransition.
 * @property transformOrigin The pivot point as a [TransformOrigin] for the scale transformation.
 * @property animationSpec The [FiniteAnimationSpec] used for the scale animation.
 * @sample androidx.compose.animation.samples.EnterExitTransitionConfigSample
 */
@Immutable
public class ScaleConfig
internal constructor(
    public val scale: Float,
    public val transformOrigin: TransformOrigin,
    public val animationSpec: FiniteAnimationSpec<Float>,
) {
    internal fun copy(
        scale: Float = this.scale,
        transformOrigin: TransformOrigin = this.transformOrigin,
        animationSpec: FiniteAnimationSpec<Float> = this.animationSpec,
    ): ScaleConfig = ScaleConfig(scale, transformOrigin, animationSpec)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScaleConfig) return false
        return scale == other.scale &&
            transformOrigin == other.transformOrigin &&
            animationSpec == other.animationSpec
    }

    override fun hashCode(): Int {
        var result = scale.hashCode()
        result = 31 * result + transformOrigin.hashCode()
        result = 31 * result + animationSpec.hashCode()
        return result
    }

    override fun toString(): String =
        "ScaleConfig(scale=$scale, transformOrigin=$transformOrigin, animationSpec=$animationSpec)"
}

/**
 * Configuration parameters for the veil effect (color overlay transition) of an [EnterTransition]
 * or [ExitTransition].
 *
 * @property initialColor The initial color of the veil overlay.
 * @property targetColor The target color of the veil overlay.
 * @property animationSpec The [FiniteAnimationSpec] used for the veil animation.
 * @property matchParentSize If true, the veil will match the parent size.
 * @sample androidx.compose.animation.samples.EnterExitTransitionConfigSample
 */
@Immutable
public class VeilConfig
internal constructor(
    public val initialColor: Color,
    public val targetColor: Color,
    public val animationSpec: FiniteAnimationSpec<Color>,
    @get:Suppress("GetterSetterNames") public val matchParentSize: Boolean,
) {
    internal fun copy(
        initialColor: Color = this.initialColor,
        targetColor: Color = this.targetColor,
        animationSpec: FiniteAnimationSpec<Color> = this.animationSpec,
        matchParentSize: Boolean = this.matchParentSize,
    ): VeilConfig = VeilConfig(initialColor, targetColor, animationSpec, matchParentSize)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VeilConfig) return false
        return initialColor == other.initialColor &&
            targetColor == other.targetColor &&
            animationSpec == other.animationSpec &&
            matchParentSize == other.matchParentSize
    }

    override fun hashCode(): Int {
        var result = initialColor.hashCode()
        result = 31 * result + targetColor.hashCode()
        result = 31 * result + animationSpec.hashCode()
        result = 31 * result + matchParentSize.hashCode()
        return result
    }

    override fun toString(): String =
        "VeilConfig(initialColor=$initialColor, targetColor=$targetColor, " +
            "animationSpec=$animationSpec, matchParentSize=$matchParentSize)"
}

/**
 * Configurations for all transitions within an [EnterTransition] or [ExitTransition].
 *
 * This class exposes the internal parameters for all transition effects that have been combined
 * into the transition. If an effect is not present in the transition, its corresponding
 * configuration property will be `null`.
 *
 * @property fade The fade effect configuration, or `null` if fade is not defined.
 * @property slide The slide effect configuration, or `null` if slide is not defined.
 * @property changeSize The size change effect configuration, or `null` if size change is not
 *   defined.
 * @property scale The scale effect configuration, or `null` if scale is not defined.
 * @property veil The veil effect configuration, or `null` if veil is not defined.
 * @sample androidx.compose.animation.samples.EnterExitTransitionConfigSample
 */
@Immutable
public class EnterExitTransitionConfig
internal constructor(
    public val fade: FadeConfig? = null,
    public val slide: SlideConfig? = null,
    public val changeSize: ChangeSizeConfig? = null,
    public val scale: ScaleConfig? = null,
    @get:Suppress("GetterSetterNames") public val veil: VeilConfig? = null,
    internal val hold: Boolean = false,
    internal val effectsMap: Map<TransitionEffectKey<*>, TransitionEffect> = emptyMap(),
) {
    internal fun copy(
        fade: FadeConfig? = this.fade,
        slide: SlideConfig? = this.slide,
        changeSize: ChangeSizeConfig? = this.changeSize,
        scale: ScaleConfig? = this.scale,
        veil: VeilConfig? = this.veil,
        hold: Boolean = this.hold,
        effectsMap: Map<TransitionEffectKey<*>, TransitionEffect> = this.effectsMap,
    ): EnterExitTransitionConfig =
        EnterExitTransitionConfig(fade, slide, changeSize, scale, veil, hold, effectsMap)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnterExitTransitionConfig) return false
        return fade == other.fade &&
            slide == other.slide &&
            changeSize == other.changeSize &&
            scale == other.scale &&
            veil == other.veil &&
            hold == other.hold &&
            effectsMap == other.effectsMap
    }

    override fun hashCode(): Int {
        var result = fade?.hashCode() ?: 0
        result = 31 * result + (slide?.hashCode() ?: 0)
        result = 31 * result + (changeSize?.hashCode() ?: 0)
        result = 31 * result + (scale?.hashCode() ?: 0)
        result = 31 * result + (veil?.hashCode() ?: 0)
        result = 31 * result + hold.hashCode()
        result = 31 * result + effectsMap.hashCode()
        return result
    }

    override fun toString(): String =
        "EnterExitTransitionConfig(fade=$fade, slide=$slide, changeSize=$changeSize, scale=$scale, " +
            "veil=$veil, hold=$hold, effectsMap=$effectsMap)"
}

/** ********************* Below are internal classes and methods ***************** */
@Immutable
private class EnterTransitionImpl(override val config: EnterExitTransitionConfig) :
    EnterTransition()

@Immutable
private class ExitTransitionImpl(override val config: EnterExitTransitionConfig) : ExitTransition()

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

@Suppress("UNCHECKED_CAST")
internal operator fun <T : TransitionEffect> EnterTransition.get(key: TransitionEffectKey<T>): T? =
    config.effectsMap[key] as? T

@Suppress("UNCHECKED_CAST")
internal operator fun <T : TransitionEffect> ExitTransition.get(key: TransitionEffectKey<T>): T? =
    config.effectsMap[key] as? T

@OptIn(ExperimentalAnimationApi::class)
@Suppress("ModifierFactoryExtensionFunction", "ComposableModifierFactory")
@Composable
internal fun Transition<EnterExitState>.createModifier(
    enter: EnterTransition,
    exit: ExitTransition,
    trackActiveEnterExit: Boolean = true,
    isEnabled: () -> Boolean = { true },
    sharedMutableTransformState: SharedMutableTransformState? = null,
    label: String,
): Modifier {
    val activeMutableState =
        if (trackActiveEnterExit || sharedMutableTransformState == null) {
            // When null, it indicates the caller has not provided an external state to track.
            // In this case, an empty `SharedMutableTransformState` is created internally
            // to satisfy non-null requirements, but no actual mutable data will be tracked.
            trackActiveMutableState(sharedMutableTransformState)
        } else {
            sharedMutableTransformState
        }
    val activeEnter =
        if (trackActiveEnterExit) {
            trackActiveEnter(enter = enter, activeMutableState = activeMutableState)
        } else {
            enter
        }
    val activeExit =
        if (trackActiveEnterExit) {
            trackActiveExit(exit = exit, activeMutableState = activeMutableState)
        } else {
            exit
        }

    val shouldAnimateVeil =
        activeEnter.config.veil != null ||
            activeExit.config.veil != null ||
            activeMutableState.veilRequiresAnimation
    val shouldAnimateSlide =
        activeEnter.config.slide != null ||
            activeExit.config.slide != null ||
            activeMutableState.slideRequiresAnimation
    val shouldAnimateSizeChange =
        activeEnter.config.changeSize != null || activeExit.config.changeSize != null

    val slideAnimation =
        if (shouldAnimateSlide) {
            createDeferredAnimation(IntOffset.VectorConverter, remember { "$label slide" })
        } else {
            null
        }
    val sizeAnimation =
        if (shouldAnimateSizeChange) {
            createDeferredAnimation(IntSize.VectorConverter, remember { "$label shrink/expand" })
        } else null

    val offsetAnimation =
        if (shouldAnimateSizeChange) {
            createDeferredAnimation(
                IntOffset.VectorConverter,
                remember { "$label InterruptionHandlingOffset" },
            )
        } else null

    val disableClip =
        (activeEnter.config.changeSize?.clip == false ||
            activeExit.config.changeSize?.clip == false) || !shouldAnimateSizeChange

    val colorSpace =
        activeEnter.config.veil?.initialColor?.colorSpace
            ?: activeEnter.config.veil?.targetColor?.colorSpace
            ?: activeExit.config.veil?.initialColor?.colorSpace
            ?: activeExit.config.veil?.targetColor?.colorSpace
            ?: ColorSpaces.Srgb
    val veilModifierElement =
        if (shouldAnimateVeil) {
            val veilAnimation =
                createDeferredAnimation(
                    Color.VectorConverter(colorSpace),
                    remember { "$label veil" },
                )
            VeilModifierElement(this, veilAnimation, activeEnter, activeExit, activeMutableState)
        } else {
            Modifier
        }
    val shouldVeilMatchParentSize =
        activeEnter.config.veil?.matchParentSize
            ?: activeExit.config.veil?.matchParentSize
            ?: activeMutableState.mutableData?.veilMatchParentSize
            ?: false

    val graphicsLayerBlock =
        createGraphicsLayerBlock(activeEnter, activeExit, activeMutableState, label)

    return (if (shouldVeilMatchParentSize) veilModifierElement else Modifier)
        .then(Modifier.graphicsLayer { clip = !disableClip && isEnabled() })
        .then(
            EnterExitTransitionElement(
                this,
                sizeAnimation,
                offsetAnimation,
                slideAnimation,
                activeEnter,
                activeExit,
                activeMutableState,
                isEnabled,
                graphicsLayerBlock,
            )
        )
        .then(if (!shouldVeilMatchParentSize) veilModifierElement else Modifier)
}

/**
 * Invokes [effect] whenever the transition settles (i.e., reaches its target state) or when it is
 * interrupted by a new target state without deferred phase (i.e. via `animateTo()`, not by
 * `defer()`).
 */
@Composable
internal fun <S> Transition<S>.DeferredTransitionCleanupEffect(effect: () -> Unit) {
    val isMutating = pendingTargetState != null

    if (currentState == targetState && !isMutating) {
        effect()
    }

    val wasMutating = remember { booleanArrayOf(isMutating) }
    val lastTarget = remember { arrayOfNulls<Any?>(1) }
    if (lastTarget[0] != targetState) {
        if (!isMutating && !wasMutating[0]) {
            effect()
        }
        lastTarget[0] = targetState
    }
    wasMutating[0] = isMutating
}

@Composable
internal fun Transition<EnterExitState>.trackActiveMutableState(
    sharedMutableTransformState: SharedMutableTransformState?
): SharedMutableTransformState {
    val shared = sharedMutableTransformState ?: remember(this) { SharedMutableTransformState() }
    val isMutating = pendingTargetState != null && shared.mutableData != null
    val isSettled = currentState == targetState
    shared.updateMutationState(isMutating, isSettled)

    LaunchedEffect(isMutating) {
        if (isMutating && !isSettled) {
            shared.startCatchUp()
        }
    }

    DeferredTransitionCleanupEffect { shared.clear() }
    return shared
}

@Composable
internal fun Transition<EnterExitState>.trackActiveEnter(
    enter: EnterTransition,
    activeMutableState: SharedMutableTransformState? = null,
): EnterTransition {
    // Active enter & active exit reference the enter and exit transition that is currently being
    // used. It is important to preserve the active enter/exit that was previously used before
    // changing target state, such that if the previous enter/exit is interrupted, we still hold
    // reference to the enter/exit that define those animations and therefore could recover.
    var activeEnter by remember(this) { mutableStateOf(enter) }
    if (currentState == targetState && currentState == EnterExitState.Visible) {
        if (isSeeking) {
            // When seeking, the timing is different and there's no need to handle interruptions.
            activeEnter = enter
        } else {
            activeEnter = EnterTransition.None
        }
    } else if (targetState != EnterExitState.PostExit) {
        // Generate a fallback enter transition to seamlessly handoff deferred animations.
        // This ensures properties modified during the deferred phase remain tracked even if
        // not specified in the enter transition spec, so that they don't snap if interrupted.
        // User-specified `enter` properties will automatically override these fallback values
        // when combined via the `+` operator below.
        val handoffEnter = activeMutableState?.getHandoffEnter() ?: EnterTransition.None
        activeEnter += handoffEnter + enter
    }
    return activeEnter
}

@Composable
internal fun Transition<EnterExitState>.trackActiveExit(
    exit: ExitTransition,
    activeMutableState: SharedMutableTransformState? = null,
): ExitTransition {
    // Active enter & active exit reference the enter and exit transition that is currently being
    // used. It is important to preserve the active enter/exit that was previously used before
    // changing target state, such that if the previous enter/exit is interrupted, we still hold
    // reference to the enter/exit that define those animations and therefore could recover.
    var activeExit by remember(this) { mutableStateOf(exit) }
    if (currentState == targetState && currentState == EnterExitState.Visible) {
        if (isSeeking) {
            // When seeking, the timing is different and there's no need to handle interruptions.
            activeExit = exit
        } else {
            activeExit = ExitTransition.None
        }
    } else if (targetState != EnterExitState.Visible) {
        // The exit transition accumulates when the content goes from exiting, to incoming,
        // to then again exiting. In this scenario, we first neutralize the previous exit animations
        // by animating them to their resting state (e.g. scale = 1f, alpha = 1f).
        // This ensures seamless animations without jump cuts and prevents old exit animations
        // from bleeding into the new exit transition (e.g. preventing a previous `scaleOut`
        // from mistakenly combining with a new `slideOut`).
        val neutralizedExit =
            if (activeMutableState?.isMutating == true) {
                // Manual transforms are applied on top of any potentially still running animations.
                // Therefore, we shouldn't neutralize in this case and continue the running
                // animation.
                activeExit
            } else {
                ExitTransitionImpl(
                    activeExit.config.copy(
                        fade = activeExit.config.fade?.copy(alpha = 1f),
                        scale = activeExit.config.scale?.copy(scale = 1f),
                        slide = activeExit.config.slide?.copy(slideOffset = NeutralSlideOffset),
                        changeSize = activeExit.config.changeSize?.copy(size = NeutralChangeSize),
                        veil =
                            activeExit.config.veil?.let { it.copy(targetColor = it.initialColor) },
                    )
                )
            }
        // Generate an exit transition to sustain deferred animations that were active at handoff.
        // User-specified `exit` properties will automatically override these sustained values
        // when combined via the `+` operator below.
        val handoffExit = activeMutableState?.getHandoffExit() ?: ExitTransition.None

        activeExit = neutralizedExit + handoffExit + exit
    }
    return activeExit
}

internal fun interface GraphicsLayerBlockForEnterExit {
    fun init(): GraphicsLayerScope.() -> Unit
}

@Composable
private fun Transition<EnterExitState>.createGraphicsLayerBlock(
    enter: EnterTransition,
    exit: ExitTransition,
    mutableTransformState: SharedMutableTransformState,
    label: String,
): GraphicsLayerBlockForEnterExit {

    val shouldAnimateAlpha =
        enter.config.fade != null ||
            exit.config.fade != null ||
            mutableTransformState.alphaRequiresAnimation
    val shouldAnimateScale =
        enter.config.scale != null ||
            exit.config.scale != null ||
            mutableTransformState.scaleRequiresAnimation

    // Fade - it's important to put fade in the end. Otherwise fade will clip slide.
    // We'll animate if at any point during the transition fadeIn/fadeOut becomes non-null. This
    // would ensure the removal of fadeIn/Out amid a fade animation doesn't result in a jump.
    val alphaAnimation =
        if (shouldAnimateAlpha) {
            createDeferredAnimation(
                typeConverter = Float.VectorConverter,
                label = remember { "$label alpha" },
            )
        } else null

    val scaleAnimation =
        if (shouldAnimateScale) {
            createDeferredAnimation(
                typeConverter = Float.VectorConverter,
                label = remember { "$label scale" },
            )
        } else null

    val transformOriginAnimation =
        if (shouldAnimateScale) {
            createDeferredAnimation(
                TransformOriginVectorConverter,
                label = "TransformOriginInterruptionHandling",
            )
        } else null

    return GraphicsLayerBlockForEnterExit {
        val alpha =
            alphaAnimation?.animate(
                transitionSpec = {
                    when {
                        EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
                            enter.config.fade?.animationSpec ?: DefaultAlphaSpring
                        EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
                            exit.config.fade?.animationSpec ?: DefaultAlphaSpring
                        else -> DefaultAlphaSpring
                    }
                },
                forcedInitialValue = mutableTransformState.alphaHandoffValue,
            ) {
                when (it) {
                    EnterExitState.Visible -> 1f
                    EnterExitState.PreEnter -> enter.config.fade?.alpha ?: 1f
                    EnterExitState.PostExit -> exit.config.fade?.alpha ?: 1f
                }
            }

        val scale =
            scaleAnimation?.animate(
                transitionSpec = {
                    when {
                        EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
                            enter.config.scale?.animationSpec ?: DefaultScaleSpring
                        EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
                            exit.config.scale?.animationSpec ?: DefaultScaleSpring
                        else -> DefaultScaleSpring
                    }
                },
                forcedInitialValue = mutableTransformState.scaleHandoffValue,
                forcedInitialVelocity = mutableTransformState.scaleHandoffVelocity,
            ) {
                when (it) {
                    EnterExitState.Visible -> 1f
                    EnterExitState.PreEnter -> enter.config.scale?.scale ?: 1f
                    EnterExitState.PostExit -> exit.config.scale?.scale ?: 1f
                }
            }
        val transformOriginWhenVisible =
            if (currentState == EnterExitState.PreEnter) {
                enter.config.scale?.transformOrigin ?: exit.config.scale?.transformOrigin
            } else {
                exit.config.scale?.transformOrigin ?: enter.config.scale?.transformOrigin
            }
        // Animate transform origin if there's any change. If scale is only defined for enter or
        // exit, use the same transform origin for both.
        val transformOrigin =
            transformOriginAnimation?.animate(
                transitionSpec = { spring() },
                forcedInitialValue = mutableTransformState.transformOriginHandoffValue,
            ) {
                when (it) {
                    EnterExitState.Visible -> transformOriginWhenVisible
                    EnterExitState.PreEnter ->
                        enter.config.scale?.transformOrigin ?: exit.config.scale?.transformOrigin
                    EnterExitState.PostExit ->
                        exit.config.scale?.transformOrigin ?: TransformOrigin.Center
                } ?: TransformOrigin.Center
            }

        val block: GraphicsLayerScope.() -> Unit = {
            this.alpha = mutableTransformState.combinedAlpha(transitionValue = alpha?.value ?: 1f)
            val combinedScale =
                mutableTransformState.combinedScale(transitionValue = scale?.value ?: 1f)
            this.scaleX = combinedScale
            this.scaleY = combinedScale
            this.transformOrigin =
                mutableTransformState.combinedTransformOrigin(
                    transitionValue = transformOrigin?.value ?: TransformOrigin.Center
                )
        }
        block
    }
}

internal val TransformOriginVectorConverter =
    TwoWayConverter<TransformOrigin, AnimationVector2D>(
        convertToVector = { AnimationVector2D(it.pivotFractionX, it.pivotFractionY) },
        convertFromVector = { TransformOrigin(it.v1, it.v2) },
    )

private val DefaultAlphaSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)

private val DefaultScaleSpring =
    spring<Float>(
        stiffness = Spring.StiffnessMediumLow,
        // 0.002f threshold (0.2%) prevents visual discontinuities/popping near the target scale
        // (e.g. ~1px cutoff on a 500px element) while ensuring timely animation completion.
        visibilityThreshold = 0.002f,
    )

private val DefaultColorAnimationSpec = spring<Color>(stiffness = Spring.StiffnessMediumLow)

private val DefaultOffsetAnimationSpec =
    spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

private class EnterExitTransitionModifierNode(
    var transition: Transition<EnterExitState>,
    var sizeAnimation: Transition<EnterExitState>.DeferredAnimation<IntSize, AnimationVector2D>?,
    var offsetAnimation:
        Transition<EnterExitState>.DeferredAnimation<IntOffset, AnimationVector2D>?,
    var slideAnimation: Transition<EnterExitState>.DeferredAnimation<IntOffset, AnimationVector2D>?,
    var enter: EnterTransition,
    var exit: ExitTransition,
    mutableTransformState: SharedMutableTransformState,
    var isEnabled: () -> Boolean,
    var graphicsLayerBlock: GraphicsLayerBlockForEnterExit,
) :
    LayoutModifierNodeWithPassThroughIntrinsics(),
    LayoutAwareModifierNode,
    ModifierLocalModifierNode {

    var mutableTransformState: SharedMutableTransformState = mutableTransformState
        set(value) {
            if (field != value) {
                field = value
                provide(ModifierLocalSharedMutableTransformState, value)
            }
        }

    override val providedValues =
        modifierLocalMapOf(ModifierLocalSharedMutableTransformState to mutableTransformState)

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.mutableTransformState.parentLayoutCoordinates = coordinates
    }

    private var lookaheadConstraintsAvailable = false
    private var lookaheadSize: IntSize = InvalidSize
    private var lookaheadConstraints: Constraints = Constraints()
        set(value) {
            lookaheadConstraintsAvailable = true
            field = value
        }

    var currentAlignment: Alignment? = null
    val alignment: Alignment?
        get() =
            with(transition.segment) {
                if (EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible) {
                    enter.config.changeSize?.alignment ?: exit.config.changeSize?.alignment
                } else {
                    exit.config.changeSize?.alignment ?: enter.config.changeSize?.alignment
                }
            }

    val sizeTransitionSpec: Transition.Segment<EnterExitState>.() -> FiniteAnimationSpec<IntSize> =
        {
            when {
                EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
                    enter.config.changeSize?.animationSpec
                EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
                    exit.config.changeSize?.animationSpec
                else -> DefaultSizeAnimationSpec
            } ?: DefaultSizeAnimationSpec
        }

    fun sizeByState(targetState: EnterExitState, fullSize: IntSize): IntSize =
        when (targetState) {
            EnterExitState.Visible -> fullSize
            EnterExitState.PreEnter -> enter.config.changeSize?.size?.invoke(fullSize) ?: fullSize
            EnterExitState.PostExit -> exit.config.changeSize?.size?.invoke(fullSize) ?: fullSize
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
            else ->
                when (targetState) {
                    EnterExitState.Visible -> IntOffset.Zero
                    EnterExitState.PreEnter -> IntOffset.Zero
                    EnterExitState.PostExit ->
                        exit.config.changeSize?.let {
                            val endSize = it.size(fullSize)
                            val targetOffset =
                                alignment!!.align(fullSize, endSize, LayoutDirection.Ltr)
                            val currentOffset =
                                currentAlignment!!.align(fullSize, endSize, LayoutDirection.Ltr)
                            targetOffset - currentOffset
                        } ?: IntOffset.Zero
                }
        }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
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
            return layout(measuredSize.width, measuredSize.height) { placeable.place(0, 0) }
        } else if (isEnabled()) {
            val layerBlock = graphicsLayerBlock.init()
            // Measure the content based on the current constraints passed down from parent.
            // AnimatedContent will measure outgoing children with a cached constraints to avoid
            // re-layout the outgoing content. At the animateEnterExit() level, it's not best not
            // to make assumptions, which is why we use constraints from parent.
            val placeable = measurable.measure(constraints)
            val measuredSize = IntSize(placeable.width, placeable.height)
            val target = if (lookaheadSize.isValid) lookaheadSize else measuredSize
            val animSize = sizeAnimation?.animate(sizeTransitionSpec) { sizeByState(it, target) }
            // Since we measure with lookahead constraints when available, the size needs to
            // be constrained by incoming constraints so that we know how to position content
            // in the constrained rect based on alignment.
            val currentSize = constraints.constrain(animSize?.value ?: measuredSize)
            val offsetDelta =
                offsetAnimation
                    ?.animate({ DefaultOffsetAnimationSpec }) { targetOffsetByState(it, target) }
                    ?.value ?: IntOffset.Zero

            val animSlideOffsetState =
                slideAnimation?.animate(
                    transitionSpec = slideSpec,
                    forcedInitialValue = mutableTransformState.slideHandoffValue,
                    forcedInitialVelocity = mutableTransformState.slideHandoffVelocity,
                ) {
                    slideTargetValueByState(it, target)
                }

            return layout(currentSize.width, currentSize.height) {
                val combinedSlideOffset =
                    mutableTransformState.combinedSlide(
                        transitionValue = animSlideOffsetState?.value ?: IntOffset.Zero,
                        fullSize = measuredSize,
                    )

                val offset =
                    (currentAlignment?.align(target, currentSize, LayoutDirection.Ltr)
                        ?: IntOffset.Zero) + combinedSlideOffset

                placeable.placeWithLayer(
                    offset.x + offsetDelta.x,
                    offset.y + offsetDelta.y,
                    0f,
                    layerBlock,
                )
            }
        } else {
            // If not enabled, skip all animations
            return measurable.measure(constraints).run { layout(width, height) { place(0, 0) } }
        }
    }

    val slideSpec: Transition.Segment<EnterExitState>.() -> FiniteAnimationSpec<IntOffset> = {
        when {
            EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible -> {
                enter.config.slide?.animationSpec ?: DefaultOffsetAnimationSpec
            }
            EnterExitState.Visible isTransitioningTo EnterExitState.PostExit -> {
                exit.config.slide?.animationSpec ?: DefaultOffsetAnimationSpec
            }
            else -> DefaultOffsetAnimationSpec
        }
    }

    fun slideTargetValueByState(targetState: EnterExitState, fullSize: IntSize): IntOffset {
        val preEnter = enter.config.slide?.slideOffset?.invoke(fullSize) ?: IntOffset.Zero
        val postExit = exit.config.slide?.slideOffset?.invoke(fullSize) ?: IntOffset.Zero
        return when (targetState) {
            EnterExitState.Visible -> IntOffset.Zero
            EnterExitState.PreEnter -> preEnter
            EnterExitState.PostExit -> postExit
        }
    }
}

private val DefaultSizeAnimationSpec =
    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold)

private class EnterExitTransitionElement(
    val transition: Transition<EnterExitState>,
    var sizeAnimation: Transition<EnterExitState>.DeferredAnimation<IntSize, AnimationVector2D>?,
    var offsetAnimation:
        Transition<EnterExitState>.DeferredAnimation<IntOffset, AnimationVector2D>?,
    var slideAnimation: Transition<EnterExitState>.DeferredAnimation<IntOffset, AnimationVector2D>?,
    var enter: EnterTransition,
    var exit: ExitTransition,
    var mutableTransformState: SharedMutableTransformState,
    var isEnabled: () -> Boolean,
    var graphicsLayerBlock: GraphicsLayerBlockForEnterExit,
) : ModifierNodeElement<EnterExitTransitionModifierNode>() {
    override fun create(): EnterExitTransitionModifierNode =
        EnterExitTransitionModifierNode(
            transition,
            sizeAnimation,
            offsetAnimation,
            slideAnimation,
            enter,
            exit,
            mutableTransformState,
            isEnabled,
            graphicsLayerBlock,
        )

    override fun update(node: EnterExitTransitionModifierNode) {
        node.transition = transition
        node.sizeAnimation = sizeAnimation
        node.offsetAnimation = offsetAnimation
        node.slideAnimation = slideAnimation
        node.enter = enter
        node.exit = exit
        node.mutableTransformState = mutableTransformState
        node.isEnabled = isEnabled
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
        properties["mutableTransformState"] = mutableTransformState
        properties["graphicsLayerBlock"] = graphicsLayerBlock
    }

    override fun hashCode(): Int {
        return ((((((transition.hashCode() * 31 + sizeAnimation.hashCode()) * 31 +
            offsetAnimation.hashCode()) * 31 + slideAnimation.hashCode()) * 31 + enter.hashCode()) *
            31 + exit.hashCode()) * 31 + isEnabled.hashCode()) * 31 +
            graphicsLayerBlock.hashCode() * 31 +
            mutableTransformState.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is EnterExitTransitionElement &&
            other.transition == transition &&
            other.sizeAnimation == sizeAnimation &&
            other.offsetAnimation == offsetAnimation &&
            other.slideAnimation == slideAnimation &&
            other.enter == enter &&
            other.exit == exit &&
            other.mutableTransformState == mutableTransformState &&
            other.isEnabled === isEnabled &&
            other.graphicsLayerBlock == graphicsLayerBlock
    }
}

private data class VeilModifierElement(
    val transition: Transition<EnterExitState>,
    val veilAnimation: Transition<EnterExitState>.DeferredAnimation<Color, AnimationVector4D>,
    val enter: EnterTransition,
    val exit: ExitTransition,
    val mutableTransformState: SharedMutableTransformState,
) : ModifierNodeElement<VeilModifierNode>() {
    override fun create(): VeilModifierNode =
        VeilModifierNode(transition, veilAnimation, enter, exit, mutableTransformState)

    override fun update(node: VeilModifierNode) {
        node.transition = transition
        node.veilAnimation = veilAnimation
        node.enter = enter
        node.exit = exit
        node.mutableTransformState = mutableTransformState
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "veil"
        properties["transition"] = transition
        properties["veilAnimation"] = veilAnimation
        properties["enter"] = enter
        properties["exit"] = exit
        properties["mutableTransformState"] = mutableTransformState
    }
}

private class VeilModifierNode(
    var transition: Transition<EnterExitState>,
    var veilAnimation: Transition<EnterExitState>.DeferredAnimation<Color, AnimationVector4D>,
    var enter: EnterTransition,
    var exit: ExitTransition,
    var mutableTransformState: SharedMutableTransformState,
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        drawContent()

        val veilColor =
            veilAnimation.animate(
                transitionSpec = {
                    when {
                        EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
                            enter.config.veil?.animationSpec ?: DefaultColorAnimationSpec
                        EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
                            exit.config.veil?.animationSpec ?: DefaultColorAnimationSpec
                        else -> DefaultColorAnimationSpec
                    }
                },
                forcedInitialValue = mutableTransformState.veilHandoffValue,
            ) {
                when (it) {
                    EnterExitState.Visible ->
                        enter.config.veil?.targetColor
                            ?: exit.config.veil?.initialColor
                            ?: Color.Transparent
                    EnterExitState.PreEnter -> enter.config.veil?.initialColor ?: Color.Transparent
                    EnterExitState.PostExit -> exit.config.veil?.targetColor ?: Color.Transparent
                }
            }

        val combinedVeilColor =
            mutableTransformState.combinedVeil(transitionValue = veilColor.value)

        if (combinedVeilColor.alpha != 0f) {
            val veil = enter.config.veil ?: exit.config.veil
            if (veil?.matchParentSize == true) {
                val layoutCoordinates = requireLayoutCoordinates()
                val parentSize =
                    layoutCoordinates.parentLayoutCoordinates?.size?.let {
                        Size(it.width.toFloat(), it.height.toFloat())
                    } ?: Size.Zero
                val offsetInParent = layoutCoordinates.positionInParent()

                drawRect(color = combinedVeilColor, size = parentSize, topLeft = -offsetInParent)
            } else {
                drawRect(combinedVeilColor)
            }
        }
    }
}
