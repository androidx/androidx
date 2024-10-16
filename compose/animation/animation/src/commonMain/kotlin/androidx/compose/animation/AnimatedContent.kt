/*
 * Copyright 2021 The Android Open Source Project
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
@file:OptIn(InternalAnimationApi::class)

package androidx.compose.animation

import androidx.collection.mutableScatterMapOf
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Start
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.createDeferredAnimation
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMaxOfOrNull

/**
 * [AnimatedContent] is a container that automatically animates its content when [targetState]
 * changes. Its [content] for different target states is defined in a mapping between a target state
 * and a composable function.
 *
 * **IMPORTANT**: The targetState parameter for the [content] lambda should *always* be taken into
 * account in deciding what composable function to return as the content for that state. This is
 * critical to ensure a successful lookup of all the incoming and outgoing content during content
 * transform.
 *
 * When [targetState] changes, content for both new and previous targetState will be looked up
 * through the [content] lambda. They will go through a [ContentTransform] so that the new target
 * content can be animated in while the initial content animates out. Meanwhile the container will
 * animate its size as needed to accommodate the new content, unless [SizeTransform] is set to
 * `null`. Once the [ContentTransform] is finished, the outgoing content will be disposed.
 *
 * If [targetState] is expected to mutate frequently and not all mutations should be treated as
 * target state change, consider defining a mapping between [targetState] and a key in [contentKey].
 * As a result, transitions will be triggered when the resulting key changes. In other words, there
 * will be no animation when switching between [targetState]s that share the same key. By default,
 * the key will be the same as the targetState object.
 *
 * By default, the [ContentTransform] will be a delayed [fadeIn] of the target content and a delayed
 * [scaleIn] [togetherWith] a [fadeOut] of the initial content, using a [SizeTransform] to animate
 * any size change of the content. This behavior can be customized using [transitionSpec]. If
 * desired, different [ContentTransform]s can be defined for different pairs of initial content and
 * target content.
 *
 * [AnimatedContent] displays only the content for [targetState] when not animating. However, during
 * the transient content transform, there will be more than one set of content present in the
 * [AnimatedContent] container. It may be sometimes desired to define the positional relationship
 * among the different content and the overlap. This can be achieved by defining [contentAlignment]
 * and [zOrder][ContentTransform.targetContentZIndex]. By default, [contentAlignment] aligns all
 * content to [Alignment.TopStart], and the `zIndex` for all the content is 0f. __Note__: The target
 * content will always be placed last, therefore it will be on top of all the other content unless
 * zIndex is specified.
 *
 * Different content in [AnimatedContent] will have access to their own [AnimatedContentScope]. This
 * allows content to define more local enter/exit transitions via
 * [AnimatedContentScope.animateEnterExit] and [AnimatedContentScope.transition]. These custom
 * enter/exit animations will be triggered as the content enters/leaves the container.
 *
 * [label] is an optional parameter to differentiate from other animations in Android Studio.
 *
 * @sample androidx.compose.animation.samples.SimpleAnimatedContentSample
 *
 * Below is an example of customizing [transitionSpec] to imply a spatial relationship between the
 * content for different states:
 *
 * @sample androidx.compose.animation.samples.AnimateIncrementDecrementSample
 * @see ContentTransform
 * @see AnimatedContentScope
 */
@Composable
public fun <S> AnimatedContent(
    targetState: S,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
            .togetherWith(fadeOut(animationSpec = tween(90)))
    },
    contentAlignment: Alignment = Alignment.TopStart,
    label: String = "AnimatedContent",
    contentKey: (targetState: S) -> Any? = { it },
    content: @Composable() AnimatedContentScope.(targetState: S) -> Unit
) {
    val transition = updateTransition(targetState = targetState, label = label)
    transition.AnimatedContent(
        modifier,
        transitionSpec,
        contentAlignment,
        contentKey,
        content = content
    )
}

/**
 * [ContentTransform] defines how the target content (i.e. content associated with target state)
 * enters [AnimatedContent] and how the initial content disappears.
 *
 * [targetContentEnter] defines the enter transition for the content associated with the new target
 * state. It can be a combination of [fadeIn], [slideIn]/[slideInHorizontally]
 * /[slideInVertically]/[AnimatedContentTransitionScope.slideIntoContainer], and expand. Similarly,
 * [initialContentExit] supports a combination of [ExitTransition] for animating out the initial
 * content (i.e. outgoing content). If the initial content and target content are of different size,
 * the [sizeTransform] will be triggered unless it's explicitly set to `null`.
 * [AnimatedContentTransitionScope.slideIntoContainer] and
 * [AnimatedContentTransitionScope.slideOutOfContainer] can provide container-size-aware sliding in
 * from the edge of the container, or sliding out to the edge of the container.
 *
 * [ContentTransform] supports the zIndex definition when the content enters the [AnimatedContent]
 * container via [targetContentZIndex]. By default, all content has a `0f` zIndex. Among content
 * with the same zIndex, the incoming target content will be on top, as it will be placed last.
 * However, this may not always be desired. zIndex can be specified to change that order. The
 * content with higher zIndex guarantee to be placed on top of content with lower zIndex.
 *
 * [sizeTransform] manages the expanding and shrinking of the container if there is any size change
 * as new content enters the [AnimatedContent] and old content leaves. Unlike [AnimatedVisibility],
 * for [AnimatedContent] it is generally more predictable to manage the size of the container using
 * [SizeTransform] than influencing the size using [expandIn]/[expandHorizontally]/[shrinkOut], etc
 * for each content. By default, [spring] will be used to animate any size change, and
 * [AnimatedContent] will be clipped to the animated size. Both can be customized by supplying a
 * different [SizeTransform]. If no size animation is desired, [sizeTransform] can be set to `null`.
 *
 * @sample androidx.compose.animation.samples.AnimatedContentTransitionSpecSample
 * @see SizeTransform
 * @see EnterTransition
 * @see ExitTransition
 * @see AnimatedContent
 */
public class ContentTransform(
    public val targetContentEnter: EnterTransition,
    public val initialContentExit: ExitTransition,
    targetContentZIndex: Float = 0f,
    sizeTransform: SizeTransform? = SizeTransform()
) {
    /**
     * This describes the zIndex of the new target content as it enters the container. It defaults
     * to 0f. Content with higher zIndex will be drawn over lower `zIndex`ed content. Among content
     * with the same index, the target content will be placed on top.
     */
    public var targetContentZIndex: Float by mutableFloatStateOf(targetContentZIndex)

    /**
     * [sizeTransform] manages the expanding and shrinking of the container if there is any size
     * change as new content enters the [AnimatedContent] and old content leaves. By default,
     * [spring] will be used to animate any size change, and [AnimatedContent] will be clipped to
     * the animated size. Both can be customized by supplying a different [SizeTransform]. If no
     * size animation is desired, [sizeTransform] can be set to `null`.
     */
    public var sizeTransform: SizeTransform? = sizeTransform
        internal set
}

/**
 * This creates a [SizeTransform] with the provided [clip] and [sizeAnimationSpec]. By default,
 * [clip] will be true. This means during the size animation, the content will be clipped to the
 * animated size. [sizeAnimationSpec] defaults to return a [spring] animation.
 *
 * @sample androidx.compose.animation.samples.AnimatedContentTransitionSpecSample
 */
public fun SizeTransform(
    clip: Boolean = true,
    sizeAnimationSpec: (initialSize: IntSize, targetSize: IntSize) -> FiniteAnimationSpec<IntSize> =
        { _, _ ->
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold
            )
        }
): SizeTransform = SizeTransformImpl(clip, sizeAnimationSpec)

/**
 * [SizeTransform] defines how to transform from one size to another when the size of the content
 * changes. When [clip] is true, the content will be clipped to the animation size.
 * [createAnimationSpec] specifies the animation spec for the size animation based on the initial
 * and target size.
 *
 * @sample androidx.compose.animation.samples.AnimatedContentTransitionSpecSample
 */
public interface SizeTransform {
    /** Whether the content should be clipped using the animated size. */
    public val clip: Boolean

    /**
     * This allows [FiniteAnimationSpec] to be defined based on the [initialSize] before the size
     * animation and the [targetSize] of the animation.
     */
    public fun createAnimationSpec(
        initialSize: IntSize,
        targetSize: IntSize
    ): FiniteAnimationSpec<IntSize>
}

/** Private implementation of SizeTransform interface. */
private class SizeTransformImpl(
    override val clip: Boolean = true,
    val sizeAnimationSpec:
        (initialSize: IntSize, targetSize: IntSize) -> FiniteAnimationSpec<IntSize>
) : SizeTransform {
    override fun createAnimationSpec(
        initialSize: IntSize,
        targetSize: IntSize
    ): FiniteAnimationSpec<IntSize> = sizeAnimationSpec(initialSize, targetSize)
}

/**
 * This creates a [ContentTransform] using the provided [EnterTransition] and [exit], where the
 * enter and exit transition will be running simultaneously. For example:
 *
 * @sample androidx.compose.animation.samples.AnimatedContentTransitionSpecSample
 */
public infix fun EnterTransition.togetherWith(exit: ExitTransition): ContentTransform =
    ContentTransform(this, exit)

@ExperimentalAnimationApi
@Deprecated(
    "Infix fun EnterTransition.with(ExitTransition) has been renamed to" + " togetherWith",
    ReplaceWith("togetherWith(exit)")
)
public infix fun EnterTransition.with(exit: ExitTransition): ContentTransform =
    ContentTransform(this, exit)

/**
 * [AnimatedContentTransitionScope] provides functions that are convenient and only applicable in
 * the context of [AnimatedContent], such as [slideIntoContainer] and [slideOutOfContainer].
 */
public sealed interface AnimatedContentTransitionScope<S> : Transition.Segment<S> {
    /**
     * Customizes the [SizeTransform] of a given [ContentTransform]. For example:
     *
     * @sample androidx.compose.animation.samples.AnimatedContentTransitionSpecSample
     */
    public infix fun ContentTransform.using(sizeTransform: SizeTransform?): ContentTransform

    /**
     * [SlideDirection] defines the direction of the slide in/out for [slideIntoContainer] and
     * [slideOutOfContainer]. The supported directions are: [Left], [Right], [Up] and [Down].
     */
    @Immutable
    @kotlin.jvm.JvmInline
    public value class SlideDirection internal constructor(private val value: Int) {
        public companion object {
            public val Left: SlideDirection = SlideDirection(0)
            public val Right: SlideDirection = SlideDirection(1)
            public val Up: SlideDirection = SlideDirection(2)
            public val Down: SlideDirection = SlideDirection(3)
            public val Start: SlideDirection = SlideDirection(4)
            public val End: SlideDirection = SlideDirection(5)
        }

        override fun toString(): String {
            return when (this) {
                Left -> "Left"
                Right -> "Right"
                Up -> "Up"
                Down -> "Down"
                Start -> "Start"
                End -> "End"
                else -> "Invalid"
            }
        }
    }

    /**
     * This defines a horizontal/vertical slide-in that is specific to [AnimatedContent] from the
     * edge of the container. The offset amount is dynamically calculated based on the current size
     * of the [AnimatedContent] and its content alignment. This offset (may be positive or negative
     * based on the direction of the slide) is then passed to [initialOffset]. By default,
     * [initialOffset] will be using the offset calculated from the system to slide the content in.
     * [slideIntoContainer] is a convenient alternative to [slideInHorizontally] and
     * [slideInVertically] when the incoming and outgoing content differ in size. Otherwise, it
     * would be equivalent to [slideInHorizontally] and [slideInVertically] with an offset of the
     * full width/height.
     *
     * [towards] specifies the slide direction. Content can be slided into the container towards
     * [SlideDirection.Left], [SlideDirection.Right], [SlideDirection.Up] and [SlideDirection.Down].
     *
     * [animationSpec] defines the animation that will be used to animate the slide-in.
     *
     * @sample androidx.compose.animation.samples.SlideIntoContainerSample
     * @see AnimatedContent
     * @see slideInHorizontally
     * @see slideInVertically
     */
    public fun slideIntoContainer(
        towards: SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset> =
            spring(visibilityThreshold = IntOffset.VisibilityThreshold),
        initialOffset: (offsetForFullSlide: Int) -> Int = { it }
    ): EnterTransition

    /**
     * This defines a horizontal/vertical exit transition to completely slide out of the
     * [AnimatedContent] container. The offset amount is dynamically calculated based on the current
     * size of the [AnimatedContent] and the new target size. This offset gets passed to
     * [targetOffset] lambda. By default, [targetOffset] uses this offset as is, but it can be
     * customized to slide a distance based on the offset. [slideOutOfContainer] is a convenient
     * alternative to [slideOutHorizontally] and [slideOutVertically] when the incoming and outgoing
     * content differ in size. Otherwise, it would be equivalent to [slideOutHorizontally] and
     * [slideOutVertically] with an offset of the full width/height.
     *
     * [towards] specifies the slide direction. Content can be slided out of the container towards
     * [SlideDirection.Left], [SlideDirection.Right], [SlideDirection.Up] and [SlideDirection.Down].
     *
     * [animationSpec] defines the animation that will be used to animate the slide-out.
     *
     * @sample androidx.compose.animation.samples.SlideIntoContainerSample
     * @see AnimatedContent
     * @see slideOutHorizontally
     * @see slideOutVertically
     */
    public fun slideOutOfContainer(
        towards: SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset> =
            spring(visibilityThreshold = IntOffset.VisibilityThreshold),
        targetOffset: (offsetForFullSlide: Int) -> Int = { it }
    ): ExitTransition

    /**
     * [KeepUntilTransitionsFinished] defers the disposal of the exiting content till both enter and
     * exit transitions have finished. It can be combined with other [ExitTransition]s using
     * [+][ExitTransition.plus].
     *
     * **Important**: [KeepUntilTransitionsFinished] works the best when the
     * [zIndex][ContentTransform.targetContentZIndex] for the incoming and outgoing content are
     * specified. Otherwise, if the content gets interrupted from entering and switching to exiting
     * using [KeepUntilTransitionsFinished], the holding pattern may render exiting content on top
     * of the entering content, unless the z-order is specified.
     *
     * @sample androidx.compose.animation.samples.SlideIntoContainerSample
     */
    public val ExitTransition.Companion.KeepUntilTransitionsFinished: ExitTransition
        get() = KeepUntilTransitionsFinished

    /** This returns the [Alignment] specified on [AnimatedContent]. */
    public val contentAlignment: Alignment
}

internal class AnimatedContentTransitionScopeImpl<S>
internal constructor(
    internal val transition: Transition<S>,
    override var contentAlignment: Alignment,
    internal var layoutDirection: LayoutDirection
) : AnimatedContentTransitionScope<S> {
    /** Initial state of a Transition Segment. This is the state that transition starts from. */
    override val initialState: S
        @Suppress("UnknownNullness") get() = transition.segment.initialState

    /** Target state of a Transition Segment. This is the state that transition will end on. */
    override val targetState: S
        @Suppress("UnknownNullness") get() = transition.segment.targetState

    /**
     * Customizes the [SizeTransform] of a given [ContentTransform]. For example:
     *
     * @sample androidx.compose.animation.samples.AnimatedContentTransitionSpecSample
     */
    override infix fun ContentTransform.using(sizeTransform: SizeTransform?) =
        this.apply { this.sizeTransform = sizeTransform }

    /**
     * This defines a horizontal/vertical slide-in that is specific to [AnimatedContent] from the
     * edge of the container. The offset amount is dynamically calculated based on the current size
     * of the [AnimatedContent] and its content alignment. This offset (may be positive or negative
     * based on the direction of the slide) is then passed to [initialOffset]. By default,
     * [initialOffset] will be using the offset calculated from the system to slide the content in.
     * [slideIntoContainer] is a convenient alternative to [slideInHorizontally] and
     * [slideInVertically] when the incoming and outgoing content differ in size. Otherwise, it
     * would be equivalent to [slideInHorizontally] and [slideInVertically] with an offset of the
     * full width/height.
     *
     * [towards] specifies the slide direction. Content can be slided into the container towards
     * [AnimatedContentTransitionScope.SlideDirection.Left],
     * [AnimatedContentTransitionScope.SlideDirection.Right],
     * [AnimatedContentTransitionScope.SlideDirection.Up] and
     * [AnimatedContentTransitionScope.SlideDirection.Down].
     *
     * [animationSpec] defines the animation that will be used to animate the slide-in.
     *
     * @sample androidx.compose.animation.samples.SlideIntoContainerSample
     * @see AnimatedContent
     * @see slideInHorizontally
     * @see slideInVertically
     */
    override fun slideIntoContainer(
        towards: AnimatedContentTransitionScope.SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset>,
        initialOffset: (offsetForFullSlide: Int) -> Int
    ): EnterTransition =
        when {
            towards.isLeft ->
                slideInHorizontally(animationSpec) {
                    initialOffset.invoke(
                        currentSize.width - calculateOffset(IntSize(it, it), currentSize).x
                    )
                }
            towards.isRight ->
                slideInHorizontally(animationSpec) {
                    initialOffset.invoke(-calculateOffset(IntSize(it, it), currentSize).x - it)
                }
            towards == Up ->
                slideInVertically(animationSpec) {
                    initialOffset.invoke(
                        currentSize.height - calculateOffset(IntSize(it, it), currentSize).y
                    )
                }
            towards == Down ->
                slideInVertically(animationSpec) {
                    initialOffset.invoke(-calculateOffset(IntSize(it, it), currentSize).y - it)
                }
            else -> EnterTransition.None
        }

    private val AnimatedContentTransitionScope.SlideDirection.isLeft: Boolean
        get() {
            return this == Left ||
                this == Start && layoutDirection == LayoutDirection.Ltr ||
                this == End && layoutDirection == LayoutDirection.Rtl
        }

    private val AnimatedContentTransitionScope.SlideDirection.isRight: Boolean
        get() {
            return this == Right ||
                this == Start && layoutDirection == LayoutDirection.Rtl ||
                this == End && layoutDirection == LayoutDirection.Ltr
        }

    private fun calculateOffset(fullSize: IntSize, currentSize: IntSize): IntOffset {
        return contentAlignment.align(fullSize, currentSize, LayoutDirection.Ltr)
    }

    /**
     * This defines a horizontal/vertical exit transition to completely slide out of the
     * [AnimatedContent] container. The offset amount is dynamically calculated based on the current
     * size of the [AnimatedContent] and the new target size. This offset gets passed to
     * [targetOffset] lambda. By default, [targetOffset] uses this offset as is, but it can be
     * customized to slide a distance based on the offset. [slideOutOfContainer] is a convenient
     * alternative to [slideOutHorizontally] and [slideOutVertically] when the incoming and outgoing
     * content differ in size. Otherwise, it would be equivalent to [slideOutHorizontally] and
     * [slideOutVertically] with an offset of the full width/height.
     *
     * [towards] specifies the slide direction. Content can be slided out of the container towards
     * [AnimatedContentTransitionScope.SlideDirection.Left],
     * [AnimatedContentTransitionScope.SlideDirection.Right],
     * [AnimatedContentTransitionScope.SlideDirection.Up] and
     * [AnimatedContentTransitionScope.SlideDirection.Down].
     *
     * [animationSpec] defines the animation that will be used to animate the slide-out.
     *
     * @sample androidx.compose.animation.samples.SlideIntoContainerSample
     * @see AnimatedContent
     * @see slideOutHorizontally
     * @see slideOutVertically
     */
    override fun slideOutOfContainer(
        towards: AnimatedContentTransitionScope.SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset>,
        targetOffset: (offsetForFullSlide: Int) -> Int
    ): ExitTransition {
        return when {
            // Note: targetSize could be 0 for empty composables
            towards.isLeft ->
                slideOutHorizontally(animationSpec) {
                    val targetSize = targetSizeMap[transition.targetState]?.value ?: IntSize.Zero
                    targetOffset.invoke(-calculateOffset(IntSize(it, it), targetSize).x - it)
                }
            towards.isRight ->
                slideOutHorizontally(animationSpec) {
                    val targetSize = targetSizeMap[transition.targetState]?.value ?: IntSize.Zero
                    targetOffset.invoke(
                        -calculateOffset(IntSize(it, it), targetSize).x + targetSize.width
                    )
                }
            towards == Up ->
                slideOutVertically(animationSpec) {
                    val targetSize = targetSizeMap[transition.targetState]?.value ?: IntSize.Zero
                    targetOffset.invoke(-calculateOffset(IntSize(it, it), targetSize).y - it)
                }
            towards == Down ->
                slideOutVertically(animationSpec) {
                    val targetSize = targetSizeMap[transition.targetState]?.value ?: IntSize.Zero
                    targetOffset.invoke(
                        -calculateOffset(IntSize(it, it), targetSize).y + targetSize.height
                    )
                }
            else -> ExitTransition.None
        }
    }

    internal var measuredSize: IntSize by mutableStateOf(IntSize.Zero)
    internal val targetSizeMap = mutableScatterMapOf<S, State<IntSize>>()
    internal var animatedSize: State<IntSize>? = null

    // Current size of the container. If there's any size animation, the current size will be
    // read from the animation value, otherwise we'll use the current
    private val currentSize: IntSize
        get() = animatedSize?.value ?: measuredSize

    @Suppress("ComposableModifierFactory", "ModifierFactoryExtensionFunction")
    @Composable
    internal fun createSizeAnimationModifier(contentTransform: ContentTransform): Modifier {
        var shouldAnimateSize by remember(this) { mutableStateOf(false) }
        val sizeTransform = rememberUpdatedState(contentTransform.sizeTransform)
        if (transition.currentState == transition.targetState) {
            shouldAnimateSize = false
        } else {
            // TODO: CurrentSize is only relevant to enter/exit transition, not so much for sizeAnim
            if (sizeTransform.value != null) {
                shouldAnimateSize = true
            }
        }
        val sizeAnimation: Transition<S>.DeferredAnimation<IntSize, AnimationVector2D>?
        return if (shouldAnimateSize) {
                sizeAnimation = transition.createDeferredAnimation(IntSize.VectorConverter)
                remember(sizeAnimation) {
                    (if (sizeTransform.value?.clip == false) Modifier else Modifier.clipToBounds())
                }
            } else {
                sizeAnimation = null
                animatedSize = null
                Modifier
            }
            .then(
                // Keep the SizeModifier in the chain and switch between active animating and
                // passive
                // observing based on sizeAnimation's value
                SizeModifierElement(sizeAnimation, sizeTransform, this)
            )
    }

    // This helps track the target measurable without affecting the placement order. Target
    // measurable needs to be measured first but placed last.
    internal class ChildData(isTarget: Boolean) : ParentDataModifier {
        // isTarget is read during measure. It is necessary to make this a MutableState
        // such that when the target changes, measure is triggered
        var isTarget by mutableStateOf(isTarget)

        override fun Density.modifyParentData(parentData: Any?): Any {
            return this@ChildData
        }
    }

    private class SizeModifierElement<S>(
        val sizeAnimation: Transition<S>.DeferredAnimation<IntSize, AnimationVector2D>?,
        val sizeTransform: State<SizeTransform?>,
        val scope: AnimatedContentTransitionScopeImpl<S>
    ) : ModifierNodeElement<SizeModifierNode<S>>() {
        override fun create(): SizeModifierNode<S> {
            return SizeModifierNode(sizeAnimation, sizeTransform, scope)
        }

        override fun hashCode(): Int {
            return (31 * scope.hashCode() + sizeAnimation.hashCode()) * 31 +
                sizeTransform.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is SizeModifierElement<*> &&
                other.sizeAnimation == sizeAnimation &&
                other.sizeTransform == sizeTransform
        }

        override fun update(node: SizeModifierNode<S>) {
            node.sizeAnimation = sizeAnimation
            node.sizeTransform = sizeTransform
            node.scope = scope
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "sizeTransform"
            properties["sizeAnimation"] = sizeAnimation
            properties["sizeTransform"] = sizeTransform
            properties["scope"] = scope
        }
    }

    private class SizeModifierNode<S>(
        var sizeAnimation: Transition<S>.DeferredAnimation<IntSize, AnimationVector2D>?,
        var sizeTransform: State<SizeTransform?>,
        var scope: AnimatedContentTransitionScopeImpl<S>,
    ) : LayoutModifierNodeWithPassThroughIntrinsics() {
        // This is used to track the on-going size change so that when the target state changes,
        // we always start from the last seen size to the new target size to ensure continuity.
        private var lastSize: IntSize = UnspecifiedSize

        private fun lastContinuousSizeOrDefault(default: IntSize) =
            if (lastSize == UnspecifiedSize) default else lastSize

        override fun onReset() {
            super.onReset()
            lastSize = UnspecifiedSize
        }

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            val measuredSize: IntSize
            if (isLookingAhead) {
                measuredSize = IntSize(placeable.width, placeable.height)
            } else if (sizeAnimation == null) {
                // Observing mode
                measuredSize = IntSize(placeable.width, placeable.height)
                lastSize = IntSize(placeable.width, placeable.height)
            } else {
                val currentSize = IntSize(placeable.width, placeable.height)
                val size =
                    sizeAnimation!!.animate(
                        transitionSpec = {
                            val initial =
                                if (initialState == scope.initialState) {
                                    lastContinuousSizeOrDefault(currentSize)
                                } else {
                                    scope.targetSizeMap[initialState]?.value ?: IntSize.Zero
                                }
                            val target = scope.targetSizeMap[targetState]?.value ?: IntSize.Zero
                            sizeTransform.value?.createAnimationSpec(initial, target)
                                ?: spring(stiffness = Spring.StiffnessMediumLow)
                        }
                    ) {
                        // Animate from the approach size to the lookahead size.
                        if (it == scope.initialState) {
                            lastContinuousSizeOrDefault(currentSize)
                        } else {
                            scope.targetSizeMap[it]?.value ?: IntSize.Zero
                        }
                    }
                scope.animatedSize = size
                measuredSize = size.value
                lastSize = size.value
            }
            return layout(measuredSize.width, measuredSize.height) {
                val offset =
                    scope.contentAlignment.align(
                        IntSize(placeable.width, placeable.height),
                        measuredSize,
                        LayoutDirection.Ltr
                    )
                placeable.place(offset)
            }
        }
    }
}

private val UnspecifiedSize: IntSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)

/**
 * Receiver scope for content lambda for AnimatedContent. In this scope,
 * [transition][AnimatedVisibilityScope.transition] can be used to observe the state of the
 * transition, or to add more enter/exit transition for the content.
 */
public sealed interface AnimatedContentScope : AnimatedVisibilityScope

private class AnimatedContentScopeImpl
internal constructor(animatedVisibilityScope: AnimatedVisibilityScope) :
    AnimatedContentScope, AnimatedVisibilityScope by animatedVisibilityScope

/**
 * [AnimatedContent] is a container that automatically animates its content when
 * [Transition.targetState] changes. Its [content] for different target states is defined in a
 * mapping between a target state and a composable function.
 *
 * **IMPORTANT**: The targetState parameter for the [content] lambda should *always* be taken into
 * account in deciding what composable function to return as the content for that state. This is
 * critical to ensure a successful lookup of all the incoming and outgoing content during content
 * transform.
 *
 * When [Transition.targetState] changes, content for both new and previous targetState will be
 * looked up through the [content] lambda. They will go through a [ContentTransform] so that the new
 * target content can be animated in while the initial content animates out. Meanwhile the container
 * will animate its size as needed to accommodate the new content, unless [SizeTransform] is set to
 * `null`. Once the [ContentTransform] is finished, the outgoing content will be disposed.
 *
 * If [Transition.targetState] is expected to mutate frequently and not all mutations should be
 * treated as target state change, consider defining a mapping between [Transition.targetState] and
 * a key in [contentKey]. As a result, transitions will be triggered when the resulting key changes.
 * In other words, there will be no animation when switching between [Transition.targetState]s that
 * share the same key. By default, the key will be the same as the targetState object.
 *
 * By default, the [ContentTransform] will be a delayed [fadeIn] of the target content and a delayed
 * [scaleIn] [togetherWith] a [fadeOut] of the initial content, using a [SizeTransform] to animate
 * any size change of the content. This behavior can be customized using [transitionSpec]. If
 * desired, different [ContentTransform]s can be defined for different pairs of initial content and
 * target content.
 *
 * [AnimatedContent] displays only the content for [Transition.targetState] when not animating.
 * However, during the transient content transform, there will be more than one sets of content
 * present in the [AnimatedContent] container. It may be sometimes desired to define the positional
 * relationship among different content and the style of overlap. This can be achieved by defining
 * [contentAlignment] and [zOrder][ContentTransform.targetContentZIndex]. By default,
 * [contentAlignment] aligns all content to [Alignment.TopStart], and the `zIndex` for all the
 * content is 0f. __Note__: The target content will always be placed last, therefore it will be on
 * top of all the other content unless zIndex is specified.
 *
 * Different content in [AnimatedContent] will have access to their own [AnimatedContentScope]. This
 * allows content to define more local enter/exit transitions via
 * [AnimatedContentScope.animateEnterExit] and [AnimatedContentScope.transition]. These custom
 * enter/exit animations will be triggered as the content enters/leaves the container.
 *
 * @sample androidx.compose.animation.samples.TransitionExtensionAnimatedContentSample
 * @see ContentTransform
 * @see AnimatedContentScope
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
public fun <S> Transition<S>.AnimatedContent(
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
            .togetherWith(fadeOut(animationSpec = tween(90)))
    },
    contentAlignment: Alignment = Alignment.TopStart,
    contentKey: (targetState: S) -> Any? = { it },
    content: @Composable() AnimatedContentScope.(targetState: S) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val rootScope =
        remember(this) {
            AnimatedContentTransitionScopeImpl(this, contentAlignment, layoutDirection)
        }
    // TODO: remove screen as soon as they are animated out
    val currentlyVisible = remember(this) { mutableStateListOf(currentState) }
    val contentMap = remember(this) { mutableScatterMapOf<S, @Composable() () -> Unit>() }
    // This is needed for tooling because it could change currentState directly,
    // as opposed to changing target only. When that happens we need to clear all the
    // visible content and only display the content for the new current state and target state.
    if (!currentlyVisible.contains(currentState)) {
        currentlyVisible.clear()
        currentlyVisible.add(currentState)
    }
    if (currentState == targetState) {
        if (currentlyVisible.size != 1 || currentlyVisible[0] != currentState) {
            currentlyVisible.clear()
            currentlyVisible.add(currentState)
        }
        if (contentMap.size != 1 || contentMap.containsKey(currentState)) {
            contentMap.clear()
        }
        // TODO: Do we want to support changing contentAlignment amid animation?
        rootScope.contentAlignment = contentAlignment
        rootScope.layoutDirection = layoutDirection
    }
    // Currently visible list always keeps the targetState at the end of the list, unless it's
    // already in the list in the case of interruption. This makes the composable associated with
    // the targetState get placed last, so the target composable will be displayed on top of
    // content associated with other states, unless zIndex is specified.
    if (currentState != targetState && !currentlyVisible.contains(targetState)) {
        // Replace the target with the same key if any
        val id = currentlyVisible.indexOfFirst { contentKey(it) == contentKey(targetState) }
        if (id == -1) {
            currentlyVisible.add(targetState)
        } else {
            currentlyVisible[id] = targetState
        }
    }
    if (!contentMap.containsKey(targetState) || !contentMap.containsKey(currentState)) {
        contentMap.clear()
        currentlyVisible.fastForEach { stateForContent ->
            contentMap[stateForContent] = {
                val specOnEnter = remember { transitionSpec(rootScope) }
                // NOTE: enter and exit for this AnimatedVisibility will be using different spec,
                // naturally.
                val exit =
                    remember(segment.targetState == stateForContent) {
                        if (segment.targetState == stateForContent) {
                            ExitTransition.None
                        } else {
                            rootScope.transitionSpec().initialContentExit
                        }
                    }
                val childData = remember {
                    AnimatedContentTransitionScopeImpl.ChildData(stateForContent == targetState)
                }
                // TODO: Will need a custom impl of this to: 1) get the signal for when
                // the animation is finished, 2) get the target size properly
                AnimatedEnterExitImpl(
                    this,
                    { it == stateForContent },
                    enter = specOnEnter.targetContentEnter,
                    exit = exit,
                    modifier =
                        Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0, zIndex = specOnEnter.targetContentZIndex)
                                }
                            }
                            .then(childData.apply { isTarget = stateForContent == targetState }),
                    shouldDisposeBlock = { currentState, targetState ->
                        currentState == EnterExitState.PostExit &&
                            targetState == EnterExitState.PostExit &&
                            !exit.data.hold
                    }
                ) {
                    // TODO: Should Transition.AnimatedVisibility have an end listener?
                    DisposableEffect(this) {
                        onDispose {
                            currentlyVisible.remove(stateForContent)
                            rootScope.targetSizeMap.remove(stateForContent)
                        }
                    }
                    rootScope.targetSizeMap[stateForContent] =
                        (this as AnimatedVisibilityScopeImpl).targetSize
                    with(remember { AnimatedContentScopeImpl(this) }) { content(stateForContent) }
                }
            }
        }
    }
    val contentTransform = remember(rootScope, segment) { transitionSpec(rootScope) }
    val sizeModifier = rootScope.createSizeAnimationModifier(contentTransform)
    Layout(
        modifier = modifier.then(sizeModifier),
        content = {
            currentlyVisible.fastForEach { key(contentKey(it)) { contentMap[it]?.invoke() } }
        },
        measurePolicy = remember { AnimatedContentMeasurePolicy(rootScope) }
    )
}

private class AnimatedContentMeasurePolicy(val rootScope: AnimatedContentTransitionScopeImpl<*>) :
    MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        var targetSize = IntSize.Zero
        // Measure the target composable first (but place it on top unless zIndex is specified)
        measurables.fastForEachIndexed { index, measurable ->
            if (
                (measurable.parentData as? AnimatedContentTransitionScopeImpl.ChildData)
                    ?.isTarget == true
            ) {
                placeables[index] =
                    measurable.measure(constraints).also {
                        targetSize = IntSize(it.width, it.height)
                    }
            }
        }
        // Measure the non-target composables after target, since these have no impact on
        // container size in the size animation.
        measurables.fastForEachIndexed { index, measurable ->
            if (placeables[index] == null) {
                placeables[index] = measurable.measure(constraints)
            }
        }
        val maxWidth: Int =
            if (isLookingAhead) {
                targetSize.width
            } else {
                placeables.maxByOrNull { it?.width ?: 0 }?.width ?: 0
            }
        val maxHeight =
            if (isLookingAhead) {
                targetSize.height
            } else {
                placeables.maxByOrNull { it?.height ?: 0 }?.height ?: 0
            }
        if (!isLookingAhead) {
            // update currently measured size only during approach
            rootScope.measuredSize = IntSize(maxWidth, maxHeight)
        }

        // Position the children.
        return layout(maxWidth, maxHeight) {
            placeables.forEach { placeable ->
                placeable?.let {
                    val offset =
                        rootScope.contentAlignment.align(
                            IntSize(it.width, it.height),
                            IntSize(maxWidth, maxHeight),
                            LayoutDirection.Ltr
                        )
                    it.place(offset.x, offset.y)
                }
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ) = measurables.fastMaxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ) = measurables.fastMaxOfOrNull { it.minIntrinsicHeight(width) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ) = measurables.fastMaxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ) = measurables.fastMaxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0
}
