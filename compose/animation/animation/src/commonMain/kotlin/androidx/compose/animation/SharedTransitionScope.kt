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

package androidx.compose.animation

import androidx.collection.MutableScatterMap
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.animatedSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.createDeferredAnimation
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [SharedTransitionLayout] creates a layout and a [SharedTransitionScope] for the child layouts
 * in [content]. Any child (direct or indirect) of the [SharedTransitionLayout] can use the
 * receiver scope [SharedTransitionScope] to create shared element or shared bounds transitions.
 *
 * **Note**: [SharedTransitionLayout] creates a new Layout. For use cases where it's preferable
 * to not introduce a new layout between [content] and the parent layout, consider using
 * [SharedTransitionScope] instead.
 *
 * @param modifier Modifiers to be applied to the layout.
 * @param content The children composable to be laid out.
 */
@ExperimentalSharedTransitionApi
@Composable
fun SharedTransitionLayout(
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.() -> Unit
) {
    SharedTransitionScope {
        Box(it.then(modifier)) {
            content()
        }
    }
}

/**
 * [SharedTransitionScope] creates a [SharedTransitionScope] for the child layouts
 * in [content]. Any child (direct or indirect) of the [SharedTransitionLayout] can use the
 * receiver scope [SharedTransitionScope] to create shared element or shared bounds transitions.
 * [SharedTransitionScope] will not creates a new Layout.
 *
 * **IMPORTANT**: It is important to set the [Modifier] provided to the [content] on the first and
 * top-most child, as the [Modifier] both obtains the root coordinates and creates an overlay.
 * If the first child layout in [content] isn't the child with the highest zIndex, consider using
 * [SharedTransitionLayout] instead.
 *
 * @param content The children composable to be laid out.
 */
@ExperimentalSharedTransitionApi
@Composable
fun SharedTransitionScope(
    content: @Composable SharedTransitionScope.(Modifier) -> Unit
) {
    LookaheadScope {
        val coroutineScope = rememberCoroutineScope()
        val sharedScope = remember { SharedTransitionScope(this, coroutineScope) }
        sharedScope.content(
            Modifier
                .layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout(p.width, p.height) {
                        val coords = coordinates
                        if (coords != null) {
                            if (!isLookingAhead) {
                                sharedScope.root = coords
                            } else {
                                sharedScope.lookaheadRoot = coords
                            }
                        }
                        p.place(0, 0)
                    }
                }
                .drawWithContent {
                    drawContent()
                    sharedScope.drawInOverlay(this)
                }
        )
    }
}

/**
 * [BoundsTransform] defines the animation spec used to animate from initial bounds to the
 * target bounds.
 */
@ExperimentalSharedTransitionApi
fun interface BoundsTransform {
    /**
     * Returns a [FiniteAnimationSpec] for animating the bounds from [initialBounds] to
     * [targetBounds].
     */
    fun transform(initialBounds: Rect, targetBounds: Rect): FiniteAnimationSpec<Rect>
}

/**
 * [SharedTransitionScope] provides a coordinator space in which shared elements/ shared bounds
 * (when matched) will transform their bounds from one to another. Their position animation is
 * always relative to the origin defined by where [SharedTransitionScope] is in the tree.
 *
 * [SharedTransitionScope] also creates an overlay, in which all shared elements and shared bounds
 * are rendered by default, so that they are not subject to their parent's fading or clipping, and
 * can therefore transform the bounds without alpha jumps or being unintentionally clipped.
 *
 * It is also [SharedTransitionScope]'s responsibility to do the [SharedContentState] key match
 * for all the [sharedElement] or [sharedBounds] defined in this scope. Note: key match will not
 * work for [SharedContentState] created in different [SharedTransitionScope]s.
 *
 * [SharedTransitionScope] oversees all the animations in its scope. When any of the animations is
 * active, [isTransitionActive] will be true. Once a bounds transform starts, by default the
 * shared element or shared bounds will render the content in the overlay. The rendering will remain
 * in the overlay until all other animations in the [SharedTransitionScope] are finished (i.e.
 * when [isTransitionActive] == false).
 */
@ExperimentalSharedTransitionApi
@Stable
class SharedTransitionScope internal constructor(
    lookaheadScope: LookaheadScope,
    val coroutineScope: CoroutineScope
) : LookaheadScope by lookaheadScope {

    /**
     * PlaceHolderSize defines the size of the space that was or will be occupied by the exiting
     * or entering [sharedElement]/[sharedBounds].
     */
    fun interface PlaceHolderSize {
        companion object {
            /**
             * [animatedSize] is a pre-defined [SharedTransitionScope.PlaceHolderSize] that lets the
             * parent layout of shared elements or shared bounds observe the animated size during an
             * active shared transition. Therefore the layout parent will most likely resize itself
             * and re-layout its children to adjust to the new animated size.
             *
             * @see [contentSize]
             * @see [SharedTransitionScope.PlaceHolderSize]
             */
            val animatedSize = PlaceHolderSize { _, animatedSize -> animatedSize }

            /**
             * [contentSize] is a pre-defined [SharedTransitionScope.PlaceHolderSize] that allows
             * the parent layout of shared elements or shared bounds to see the content size of the
             * shared content during an active shared transition. For outgoing content, this
             * [contentSize] is the initial size before the animation, whereas for incoming content
             * [contentSize] will return the lookahead/target size of the content. This is the
             * default value for shared elements and shared bounds. The effect is
             * that the parent layout does not resize during the shared element transition, hence
             * giving a sense of stability, rather than dynamic motion. If it's preferred to have
             * parent layout dynamically adjust its layout based on the shared element's animated
             * size, consider using [animatedSize].
             *
             * @see [contentSize]
             * @see [SharedTransitionScope.PlaceHolderSize]
             */
            val contentSize = PlaceHolderSize { contentSize, _ -> contentSize }
        }

        /**
         * Returns the size of the place holder based on [contentSize] and [animatedSize].
         * Note: [contentSize] for exiting content is the size before it starts exiting.
         * For entering content, [contentSize] is the lookahead size of the content (i.e. target
         * size of the shared transition).
         */
        fun calculateSize(contentSize: IntSize, animatedSize: IntSize): IntSize
    }

    /**
     * Indicates whether there is any ongoing transition between matched [sharedElement] or
     * [sharedBounds].
     */
    var isTransitionActive: Boolean by mutableStateOf(false)
        private set

    /**
     * [skipToLookaheadSize] enables a layout to measure its child with the lookahead constraints,
     * therefore laying out the child as if the transition has finished. This is particularly
     * helpful for layouts where re-flowing content based on animated constraints is undesirable,
     * such as texts.
     *
     * In the sample below, try remove the [skipToLookaheadSize] modifier and observe the
     * difference:
     * @sample androidx.compose.animation.samples.NestedSharedBoundsSample
     */
    fun Modifier.skipToLookaheadSize(): Modifier = this.then(SkipToLookaheadElement)

    /**
     * Renders the content in the [SharedTransitionScope]'s overlay, where shared content (i.e.
     * shared elements and shared bounds) is rendered by default. This is useful for rendering
     * content that is not shared on top of shared content to preserve a specific spatial
     * relationship.
     *
     * [renderInOverlay] dynamically controls whether the content should be rendered in the
     * [SharedTransitionScope]'s overlay. By default, it returns the same value as
     * [SharedTransitionScope.isTransitionActive]. This means the default behavior is to render
     * the child layout of this modifier in the overlay only when the transition is active.
     *
     * **IMPORTANT:** When elevating layouts into the overlay, the layout is no longer subjected
     * to 1) its parent's clipping, and 2) parent's layer transform (e.g. alpha, scale, etc).
     * Therefore, it is recommended to create an enter/exit animation (e.g. using
     * [AnimatedVisibilityScope.animateEnterExit]) for the child layout to avoid any abrupt visual
     * changes.
     *
     * [clipInOverlayDuringTransition] supports a custom clip path if clipping is desired. By default,
     * no clipping is applied. Manual management of clipping can often be avoided by putting layouts
     * with clipping as children of this modifier (i.e. to the right side of this modifier).
     *
     * @sample androidx.compose.animation.samples.SharedElementWithFABInOverlaySample
     */
    fun Modifier.renderInSharedTransitionScopeOverlay(
        renderInOverlay: () -> Boolean = defaultRenderInOverlay,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? =
            DefaultClipInOverlayDuringTransition
    ): Modifier =
        this.then(
            RenderInTransitionOverlayNodeElement(
                this@SharedTransitionScope,
                renderInOverlay,
                zIndexInOverlay,
                clipInOverlayDuringTransition
            )
        )

    /**
     * [OverlayClip] defines a specific clipping that should be applied to a [sharedBounds]
     * or [sharedElement] in the overlay.
     */
    interface OverlayClip {
        /**
         * Creates a clip path based using current animated [bounds] of the [sharedBounds] or
         * [sharedElement], their [state] (to query parent state's bounds if needed), and
         * [layoutDirection] and [density]. The topLeft of the [bounds] is the local position
         * of the sharedElement/sharedBounds in the [SharedTransitionScope].
         *
         * **Important**: The returned [Path] needs to be offset-ed as needed such that it is in
         * [SharedTransitionScope.lookaheadScopeCoordinates]'s coordinate space. For example,
         * if the path is created using [bounds], it needs to be offset-ed by [bounds].topLeft.
         *
         * It is recommended to modify the same [Path] object and return it here, instead of
         * creating new [Path]s.
         */
        fun getClipPath(
            state: SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density
        ): Path?
    }

    /**
     * [sharedElement] is a modifier that tags a layout with a [SharedContentState.key], such that
     * entering and exiting shared elements of the same key share the animated and continuously
     * changing bounds during the layout change. The bounds will be animated from the initial
     * bounds defined by the exiting shared element to the target bounds calculated based on the
     * incoming shared element. The animation for the bounds can be customized using
     * [boundsTransform].
     *
     * In contrast to [sharedBounds], [sharedElement] is designed for shared content that has the
     * exact match in terms of visual content and layout when the measure constraints are the same.
     * Such examples include image assets, icons,
     * [MovableContent][androidx.compose.runtime.MovableContent] etc.
     * Only the shared element that is becoming visible will be rendered during the
     * transition. The bounds for shared element are determined by the bounds of the shared element
     * becoming visible based on the target state of [animatedVisibilityScope].
     *
     * **Important**:
     * When a shared element finds its match and starts a transition, it will be rendered into
     * the overlay of the [SharedTransitionScope] in order to avoid being faded in/out along with
     * its parents or clipped by its parent as it transforms to the target size and position. This
     * also means that any clipping or fading for the shared elements will need to be applied
     * _explicitly_ as the child of [sharedElement] (i.e. after [sharedElement] modifier in the
     * modifier chain). For example:
     * `Modifier.sharedElement(...).clip(shape = RoundedCornerShape(20.dp)).animateEnterExit(...)`
     *
     * By default, the [sharedElement] is clipped by the [clipInOverlayDuringTransition] of its
     * parent [sharedBounds]. If the [sharedElement] has no parent [sharedBounds] or if the parent
     * [sharedBounds] has no clipping defined, it'll not be clipped. If additional clipping is
     * desired to ensure [sharedElement] doesn't move outside of a visual bounds,
     * [clipInOverlayDuringTransition] can be used to specify the clipping for when the shared element
     * is going through an active transition towards a new target bounds.
     *
     * While the shared elements are rendered in overlay during the transition, its
     * [zIndexInOverlay] can be specified to allow shared elements to render in a different order
     * than their placement/zOrder when not in the overlay. For example, the tile of a page is
     * typically placed and rendered before the content below. During the transition, it may be
     * desired to animate the title over on top of the other shared elements on that page to
     * indicate significance or a point of interest. [zIndexInOverlay] can be used to facilitate
     * such use cases. [zIndexInOverlay] is 0f by default.
     *
     * [renderInOverlayDuringTransition] is true by default. In some rare use cases, there may
     * be no clipping or layer transform (fade, scale, etc) in the application that prevents
     * shared elements from transitioning from one bounds to another without any clipping or
     * sudden alpha change. In such cases, [renderInOverlayDuringTransition] could be specified
     * to false.
     *
     * During a shared element transition, the space that was occupied by the exiting shared
     * element and the space that the entering shared element will take up are considered place
     * holders. Their sizes during the shared element transition can be configured through
     * [placeHolderSize]. By default, it will be the same as the content size of the respective
     * shared element. It can also be set to [animatedSize] or any other [PlaceHolderSize] to
     * report to their parent layout an animated size to create a visual effect where the parent
     * layout dynamically adjusts the layout to accommodate the animated size of the shared
     * elements.
     *
     * @sample androidx.compose.animation.samples.SharedElementInAnimatedContentSample
     *
     *  @see [sharedBounds]
     */
    @OptIn(ExperimentalAnimationApi::class)
    fun Modifier.sharedElement(
        state: SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
        boundsTransform: BoundsTransform = DefaultBoundsTransform,
        placeHolderSize: PlaceHolderSize = contentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = ParentClip
    ) = this.sharedBoundsImpl(
        state,
        parentTransition = animatedVisibilityScope.transition,
        visible = { it == EnterExitState.Visible },
        boundsTransform = boundsTransform,
        placeHolderSize = placeHolderSize,
        renderOnlyWhenVisible = true,
        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
        zIndexInOverlay = zIndexInOverlay,
        clipInOverlayDuringTransition = clipInOverlayDuringTransition
    )

    /**
     * [sharedBounds] is a modifier that tags a layout with a [SharedContentState.key], such that
     * entering and exiting shared bounds of the same key share the animated and continuously
     * changing bounds during the layout change. The bounds will be animated from the initial
     * bounds defined by the exiting shared bounds to the target bounds calculated based on the
     * incoming shared shared bounds. The animation for the bounds can be customized using
     * [boundsTransform].
     *
     * In contrast to [sharedElement], [sharedBounds] is designed for shared content that has the
     * visually different content. While the [sharedBounds] keeps the continuity of the bounds,
     * the incoming and outgoing content within the [sharedBounds] will enter and exit in an
     * enter/exit transition using [enter]/[exit]. They fade in/out by default.
     * The target bounds for [sharedBounds] are determined by the bounds of the [sharedBounds]
     * becoming visible based on the target state of [animatedVisibilityScope].
     *
     * **Important**:
     * When a shared bounds finds its match and starts a transition, it will be rendered into
     * the overlay of the [SharedTransitionScope] in order to avoid being faded in/out along with
     * its parents or clipped by its parent as it transforms to the target size and position. This
     * also means that any clipping or fading for the shared elements will need to be applied
     * _explicitly_ as the child of [sharedBounds] (i.e. after [sharedBounds] modifier in the
     * modifier chain). For example:
     * `Modifier.sharedBounds(...).clip(shape = RoundedCornerShape(20.dp))`
     *
     * By default, the [sharedBounds] is clipped by the [clipInOverlayDuringTransition] of its
     * parent [sharedBounds] in the layout tree. If the [sharedBounds] has no parent [sharedBounds]
     * or if the parent [sharedBounds] has no clipping defined, it'll not be clipped. If additional
     * clipping is desired to ensure child [sharedBounds] or child [sharedElement] don't move
     * outside of the this [sharedBounds]'s visual bounds in the overlay,
     * [clipInOverlayDuringTransition] can be used to specify the clipping.
     *
     * While the shared bounds are rendered in overlay during the transition, its
     * [zIndexInOverlay] can be specified to allow them to render in a different order
     * than their placement/zOrder when not in the overlay. For example, the tile of a page is
     * typically placed and rendered before the content below. During the transition, it may be
     * desired to animate the title over on top of the other shared elements on that page to
     * indicate significance or a point of interest. [zIndexInOverlay] can be used to facilitate
     * such use cases. [zIndexInOverlay] is 0f by default.
     *
     * [renderInOverlayDuringTransition] is true by default. In some rare use cases, there may
     * be no clipping or layer transform (fade, scale, etc) in the application that prevents
     * shared elements from transitioning from one bounds to another without any clipping or
     * sudden alpha change. In such cases, [renderInOverlayDuringTransition] could be specified
     * to false.
     *
     * During a shared bounds transition, the space that was occupied by the exiting shared
     * bounds and the space that the entering shared bounds will take up are considered place
     * holders. Their sizes during the shared element transition can be configured through
     * [placeHolderSize]. By default, it will be the same as the content size of the respective
     * shared bounds. It can also be set to [animatedSize] or any other [PlaceHolderSize] to
     * report to their parent layout an animated size to create a visual effect where the parent
     * layout dynamically adjusts the layout to accommodate the animated size of the shared
     * elements.
     *
     * @sample androidx.compose.animation.samples.NestedSharedBoundsSample
     * @see [sharedBounds]
     */
    @OptIn(ExperimentalAnimationApi::class)
    fun Modifier.sharedBounds(
        sharedContentState: SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
        enter: EnterTransition = fadeIn(),
        exit: ExitTransition = fadeOut(),
        boundsTransform: BoundsTransform = DefaultBoundsTransform,
        placeHolderSize: PlaceHolderSize = contentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = ParentClip
    ) =
        this
            .sharedBoundsImpl(
                sharedContentState,
                animatedVisibilityScope.transition,
                visible = { it == EnterExitState.Visible },
                boundsTransform,
                placeHolderSize = placeHolderSize,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                zIndexInOverlay = zIndexInOverlay,
                clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                renderOnlyWhenVisible = false
            )
            .composed {
                animatedVisibilityScope.transition.createModifier(
                    enter = enter,
                    exit = exit,
                    isEnabled = { sharedContentState.isMatchFound },
                    label = "enter/exit for ${sharedContentState.key}"
                )
            }

    /**
     * [sharedElementWithCallerManagedVisibility] is a modifier that tags a layout with a
     * [SharedContentState.key], such that
     * entering and exiting shared elements of the same key share the animated and continuously
     * changing bounds during the layout change. The bounds will be animated from the initial
     * bounds defined by the exiting shared element to the target bounds calculated based on the
     * incoming shared element. The animation for the bounds can be customized using
     * [boundsTransform].
     *
     * Compared to [sharedElement], [sharedElementWithCallerManagedVisibility] is designed
     * for shared element transitions where the shared element is not a part of the content that
     * is being animated out by [AnimatedVisibility]. Therefore, it is the caller's responsibility
     * to explicitly remove the exiting shared element (i.e. shared elements where
     * [visible] == false) from the tree as appropriate. Typically this is when the transition is
     * finished (i.e. [SharedTransitionScope.isTransitionActive] == false). The target bounds is
     * derived from the [sharedElementWithCallerManagedVisibility] with [visible] being true.
     *
     * In contrast to [sharedBounds], this modifier is intended for shared content that has the
     * exact match in terms of visual content and layout when the measure constraints are the same.
     * Such examples include image assets, icons,
     * [MovableContent][androidx.compose.runtime.MovableContent] etc.
     * Only the shared element that is becoming visible will be rendered during the
     * transition.
     *
     * **Important**:
     * When a shared element finds its match and starts a transition, it will be rendered into
     * the overlay of the [SharedTransitionScope] in order to avoid being faded in/out along with
     * its parents or clipped by its parent as it transforms to the target size and position. This
     * also means that any clipping or fading for the shared elements will need to be applied
     * _explicitly_ as the child of [sharedElementWithCallerManagedVisibility]
     * (i.e. after [sharedElementWithCallerManagedVisibility] modifier in the
     * modifier chain). For example:
     * ```
     * Modifier.sharedElementWithCallerManagedVisibility(...)
     *         .clip(shape = RoundedCornerShape(20.dp))
     * ```
     *
     * By default, the [sharedElementWithCallerManagedVisibility] is clipped by the
     * [clipInOverlayDuringTransition] of its
     * parent [sharedBounds]. If the [sharedElementWithCallerManagedVisibility] has no parent
     * [sharedBounds] or if the parent [sharedBounds] has no clipping defined, it'll not be
     * clipped. If additional clipping is desired to ensure
     * [sharedElementWithCallerManagedVisibility] doesn't move outside of a visual bounds,
     * [clipInOverlayDuringTransition] can be used to specify the clipping for when the shared
     * element is going through an active transition towards a new target bounds.
     *
     * While the shared elements are rendered in overlay during the transition, its
     * [zIndexInOverlay] can be specified to allow shared elements to render in a different order
     * than their placement/zOrder when not in the overlay. For example, the tile of a page is
     * typically placed and rendered before the content below. During the transition, it may be
     * desired to animate the title over on top of the other shared elements on that page to
     * indicate significance or a point of interest. [zIndexInOverlay] can be used to facilitate
     * such use cases. [zIndexInOverlay] is 0f by default.
     *
     * [renderInOverlayDuringTransition] is true by default. In some rare use cases, there may
     * be no clipping or layer transform (fade, scale, etc) in the application that prevents
     * shared elements from transitioning from one bounds to another without any clipping or
     * sudden alpha change. In such cases, [renderInOverlayDuringTransition] could be specified
     * to false.
     *
     * During a shared element transition, the space that was occupied by the exiting shared
     * element and the space that the entering shared element will take up are considered place
     * holders. Their sizes during the shared element transition can be configured through
     * [placeHolderSize]. By default, it will be the same as the content size of the respective
     * shared element. It can also be set to [animatedSize] or any other [PlaceHolderSize] to
     * report to their parent layout an animated size to create a visual effect where the parent
     * layout dynamically adjusts the layout to accommodate the animated size of the shared
     * elements.
     *
     * @sample androidx.compose.animation.samples.SharedElementWithMovableContentSample
     */
    fun Modifier.sharedElementWithCallerManagedVisibility(
        sharedContentState: SharedContentState,
        visible: Boolean,
        boundsTransform: BoundsTransform = DefaultBoundsTransform,
        placeHolderSize: PlaceHolderSize = contentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = ParentClip
    ) = this.sharedBoundsImpl<Unit>(
        sharedContentState,
        null,
        { visible },
        boundsTransform,
        placeHolderSize,
        renderOnlyWhenVisible = true,
        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
        zIndexInOverlay = zIndexInOverlay,
        clipInOverlayDuringTransition = clipInOverlayDuringTransition
    )

    /**
     * [sharedBoundsWithCallerManagedVisibility] is a modifier that tags a layout with a
     * [SharedContentState.key], such that
     * entering and exiting shared bounds of the same key share the animated and continuously
     * changing bounds during the layout change. The bounds will be animated from the initial
     * bounds defined by the exiting shared bounds to the target bounds calculated based on the
     * incoming shared bounds. The animation for the bounds can be customized using
     * [boundsTransform].
     *
     * Compared to [sharedBounds], [sharedBoundsWithCallerManagedVisibility] is designed
     * for shared bounds transitions where the shared bounds is not a part of the content that
     * is being animated out by [AnimatedVisibility]. Therefore, it is the caller's responsibility
     * to explicitly remove the exiting shared bounds (i.e. shared bounds where
     * [visible] == false) from the tree as appropriate. Typically this is when the transition is
     * finished (i.e. [SharedTransitionScope.isTransitionActive] == false). The target bounds is
     * derived from the [sharedBoundsWithCallerManagedVisibility] with [visible] being true.
     *
     * Similar to [sharedBounds], [sharedBoundsWithCallerManagedVisibility] is designed for
     * shared content that has the visually different content. It keeps the
     * continuity of the bounds. Unlike [sharedBounds], [sharedBoundsWithCallerManagedVisibility]
     * will not apply any enter transition or exit transition for the incoming and outgoing content
     * within the bounds. Such enter and exit animation will need to be added by the caller
     * of this API.
     *
     * **Important**:
     * When a shared bounds finds its match and starts a transition, it will be rendered into
     * the overlay of the [SharedTransitionScope] in order to avoid being faded in/out along with
     * its parents or clipped by its parent as it transforms to the target size and position. This
     * also means that any clipping or fading for the shared elements will need to be applied
     * _explicitly_ as the child of [sharedBoundsWithCallerManagedVisibility]
     * (i.e. after [sharedBoundsWithCallerManagedVisibility] modifier in the
     * modifier chain). For example:
     * ```
     * Modifier.sharedBoundsWithCallerManagedVisibility(...)
     *         .clip(shape = RoundedCornerShape(20.dp))
     * ```
     *
     * By default, the [sharedBoundsWithCallerManagedVisibility] is clipped by the
     * [clipInOverlayDuringTransition] of its
     * parent [sharedBounds]. If the [sharedBoundsWithCallerManagedVisibility] has no parent
     * [sharedBounds] or if the parent [sharedBounds] has no clipping defined, it'll not be
     * clipped. If additional clipping is desired to ensure
     * [sharedBoundsWithCallerManagedVisibility] doesn't move outside of a visual bounds,
     * [clipInOverlayDuringTransition] can be used to specify the clipping for when the shared
     * bounds is going through an active transition towards a new target bounds.
     *
     * While the shared bounds are rendered in overlay during the transition, its
     * [zIndexInOverlay] can be specified to allow shared bounds to render in a different order
     * than their placement/zOrder when not in the overlay. For example, the tile of a page is
     * typically placed and rendered before other layouts. During the transition, it may be
     * desired to animate the title over on top of the other shared elements on that page to
     * indicate significance or a point of interest. [zIndexInOverlay] can be used to facilitate
     * such use cases. [zIndexInOverlay] is 0f by default.
     *
     * [renderInOverlayDuringTransition] is true by default. In some rare use cases, there may
     * be no clipping or layer transform (fade, scale, etc) in the application that prevents
     * shared bounds from transitioning from one bounds to another without any clipping or
     * sudden alpha change. In such cases, [renderInOverlayDuringTransition] could be specified
     * to false.
     *
     * During a shared bounds transition, the space that was occupied by the exiting shared
     * bounds and the space that the entering shared bounds will take up are considered place
     * holders. Their sizes during the shared bounds transition can be configured through
     * [placeHolderSize]. By default, it will be the same as the content size of the respective
     * shared bounds. It can also be set to [animatedSize] or any other [PlaceHolderSize] to
     * report to their parent layout an animated size to create a visual effect where the parent
     * layout dynamically adjusts the layout to accommodate the animated size of the shared
     * bounds.
     *
     * // TODO: Evaluate whether this could become a public API
     */
    internal fun Modifier.sharedBoundsWithCallerManagedVisibility(
        sharedContentState: SharedContentState,
        visible: Boolean,
        boundsTransform: BoundsTransform = DefaultBoundsTransform,
        placeHolderSize: PlaceHolderSize = contentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = ParentClip
    ) = this.sharedBoundsImpl<Unit>(
        sharedContentState,
        null,
        { visible },
        boundsTransform,
        placeHolderSize,
        renderOnlyWhenVisible = false,
        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
        zIndexInOverlay = zIndexInOverlay,
        clipInOverlayDuringTransition = clipInOverlayDuringTransition
    )

    /**
     * Creates an [OverlayClip] based on a specific [clipShape].
     */
    fun OverlayClip(clipShape: Shape): OverlayClip = ShapeBasedClip(clipShape)

    /**
     * Creates and remembers a [SharedContentState] with a given [key].
     */
    @Composable
    fun rememberSharedContentState(key: Any): SharedContentState = remember(key) {
        SharedContentState(key)
    }

    /**
     * [SharedContentState] is designed to allow access of the properties of
     * [sharedBounds]/[sharedElement], such as whether a match of the same [key] has been found in
     * the [SharedTransitionScope], its [clipPathInOverlay] and [parentSharedContentState] if there
     * is a parent [sharedBounds] in the layout tree.
     */
    class SharedContentState internal constructor(val key: Any) {
        /**
         * Indicates whether a match of the same [key] has been found. [sharedElement]
         * or [sharedBounds] will not have any animation unless a match has been found.
         *
         * _Caveat_: [isMatchFound] is only set to true _after_ a new [sharedElement]/[sharedBounds]
         * of the same [key] has been composed. If the new [sharedBounds]/[sharedElement] is
         * declared in subcomposition (e.g. a LazyList) where the composition happens as a part of
         * the measure/layout pass, that's when [isMatchFound] will become true.
         */
        val isMatchFound: Boolean
            get() = nonNullInternalState.sharedElement.foundMatch

        /**
         * The resolved clip path in overlay based on the [OverlayClip] defined for the shared
         * content. [clipPathInOverlay] is set during Draw phase, before children are drawn. This
         * means it is safe to query [parentSharedContentState]'s [clipPathInOverlay] when
         * the shared content is drawn.
         */
        val clipPathInOverlay: Path?
            get() = nonNullInternalState.clipPathInOverlay

        /**
         * Returns the [SharedContentState] of a parent [sharedBounds], if any.
         */
        val parentSharedContentState: SharedContentState?
            get() = nonNullInternalState.parentState?.userState
        internal var internalState: SharedElementInternalState? by mutableStateOf(null)
        private val nonNullInternalState: SharedElementInternalState
            get() = requireNotNull(internalState) {
                "Error: SharedContentState has not been added to a sharedElement/sharedBounds" +
                    "modifier yet. Therefore the internal state has not bee initialized."
            }
    }

    /**********  Impl details below *****************/
    private val observeAnimatingBlock: () -> Unit = {
        sharedElements.any { _, element ->
            element.isAnimating()
        }
    }

    private val updateTransitionActiveness: (SharedTransitionScope) -> Unit = {
        updateTransitionActiveness()
    }

    private fun updateTransitionActiveness() {
        val isActive = sharedElements.any { _, element ->
            element.isAnimating()
        }
        if (isActive != isTransitionActive) {
            isTransitionActive = isActive
            if (!isActive) {
                sharedElements.forEach { _, element ->
                    element.onSharedTransitionFinished()
                }
            }
        }
        sharedElements.forEach { _, element ->
            element.updateMatch()
        }
        observer.observeReads(this, updateTransitionActiveness, observeAnimatingBlock)
    }

    private val observer = SnapshotStateObserver { it() }.also { it.start() }

    /**
     * sharedBoundsImpl is the implementation for creating animations for shared element
     * or shared bounds transition. [parentTransition] defines the parent Transition that
     * the shared element will add its animations to. When [parentTransition] is null,
     * [visible] will be cast to (Unit) -> Boolean, since we have no parent state to use
     * for the query.
     */
    @OptIn(ExperimentalTransitionApi::class)
    private fun <T> Modifier.sharedBoundsImpl(
        sharedContentState: SharedContentState,
        parentTransition: Transition<T>?,
        visible: (T) -> Boolean,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize = contentSize,
        renderOnlyWhenVisible: Boolean,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
    ) = composed {
        val key = sharedContentState.key
        val sharedElement = remember(key) { sharedElementsFor(key) }

        @Suppress("UNCHECKED_CAST")
        val boundsTransition = key(key, parentTransition) {
            if (parentTransition != null) {
                parentTransition.createChildTransition(key.toString()) { visible(it) }
            } else {
                val targetState =
                    (visible as (Unit) -> Boolean).invoke(Unit)
                val transitionState = remember {
                    MutableTransitionState(
                        initialState = if (sharedElement.currentBounds != null) {
                            // In the transition that we completely own, we could make the
                            // assumption that if a new shared element is added, it'll
                            // always animate from current bounds to target bounds. This ensures
                            // continuity of shared element bounds.
                            !targetState
                        } else {
                            targetState
                        }
                    )
                }.also { it.targetState = targetState }
                rememberTransition(transitionState)
            }
        }
        val animation = key(isTransitionActive) {
            boundsTransition.createDeferredAnimation(Rect.VectorConverter)
        }

        val boundsAnimation = remember(boundsTransition) {
            BoundsAnimation(
                this@SharedTransitionScope, boundsTransition, animation, boundsTransform
            )
        }.also {
            it.updateAnimation(animation, boundsTransform)
        }

        val sharedElementState = remember(key) {
            SharedElementInternalState(
                sharedElement,
                boundsAnimation,
                placeHolderSize,
                renderOnlyWhenVisible = renderOnlyWhenVisible,
                userState = sharedContentState,
                overlayClip = clipInOverlayDuringTransition,
                zIndex = zIndexInOverlay,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition
            )
        }.also {
            sharedContentState.internalState = it
            // Update the properties if any of them changes
            it.sharedElement = sharedElement
            it.renderOnlyWhenVisible = renderOnlyWhenVisible
            it.boundsAnimation = boundsAnimation
            it.placeHolderSize = placeHolderSize
            it.overlayClip = clipInOverlayDuringTransition
            it.zIndex = zIndexInOverlay
            it.renderInOverlayDuringTransition = renderInOverlayDuringTransition
            it.userState = sharedContentState
        }

        this then SharedBoundsNodeElement(sharedElementState)
    }

    internal lateinit var root: LayoutCoordinates
    internal lateinit var lookaheadRoot: LayoutCoordinates

    // TODO: Use MutableObjectList and impl sort
    private val renderers = mutableListOf<LayerRenderer>()

    private val sharedElements = MutableScatterMap<Any, SharedElement>()
    private val defaultRenderInOverlay: () -> Boolean = { isTransitionActive }

    private fun sharedElementsFor(key: Any): SharedElement {
        return sharedElements[key] ?: SharedElement(key, this).also {
            sharedElements[key] = it
        }
    }

    internal fun drawInOverlay(scope: ContentDrawScope) {
        // TODO: Sort while preserving the parent child order
        renderers.sortBy {
            if (it.zIndex == 0f && it is SharedElementInternalState && it.parentState == null) {
                -1f
            } else it.zIndex
        }
        renderers.fastForEach {
            it.drawInOverlay(drawScope = scope)
        }
    }

    internal fun onStateRemoved(sharedElementState: SharedElementInternalState) {
        with(sharedElementState.sharedElement) {
            removeState(sharedElementState)
            updateTransitionActiveness.invoke(this@SharedTransitionScope)
            observer.observeReads(scope, updateTransitionActiveness, observeAnimatingBlock)
            renderers.remove(sharedElementState)
            if (states.isEmpty()) {
                scope.coroutineScope.launch {
                    if (states.isEmpty()) {
                        scope.sharedElements.remove(key)
                    }
                }
            }
        }
    }

    internal fun onStateAdded(sharedElementState: SharedElementInternalState) {
        with(sharedElementState.sharedElement) {
            addState(sharedElementState)
            updateTransitionActiveness.invoke(this@SharedTransitionScope)
            observer.observeReads(scope, updateTransitionActiveness, observeAnimatingBlock)
            val id = renderers.indexOfFirst {
                (it as? SharedElementInternalState)?.sharedElement ==
                    sharedElementState.sharedElement
            }
            if (id == renderers.size - 1 || id == -1) {
                renderers.add(sharedElementState)
            } else {
                renderers.add(id + 1, sharedElementState)
            }
        }
    }

    internal fun onLayerRendererCreated(renderer: LayerRenderer) {
        renderers.add(renderer)
    }

    internal fun onLayerRendererRemoved(renderer: LayerRenderer) {
        renderers.remove(renderer)
    }

    private class ShapeBasedClip(
        val clipShape: Shape
    ) : OverlayClip {
        private val path = Path()

        override fun getClipPath(
            state: SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density
        ): Path {
            path.reset()
            path.addOutline(
                clipShape.createOutline(
                    bounds.size,
                    layoutDirection,
                    density
                )
            )
            path.translate(bounds.topLeft)
            return path
        }
    }
}

private object SkipToLookaheadElement : ModifierNodeElement<SkipToLookaheadNode>() {
    override fun create(): SkipToLookaheadNode {
        return SkipToLookaheadNode()
    }

    override fun hashCode(): Int {
        return SkipToLookaheadElement::class.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is SkipToLookaheadElement
    }

    override fun update(node: SkipToLookaheadNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "skipToLookahead"
    }
}

private class SkipToLookaheadNode : LayoutModifierNode,
    Modifier.Node() {
    var lookaheadConstraints: Constraints? = null
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        if (isLookingAhead) {
            lookaheadConstraints = constraints
        }
        val p = measurable.measure(lookaheadConstraints!!)
        val (w, h) = constraints.constrain(IntSize(p.width, p.height))
        return layout(w, h) {
            p.place(0, 0)
        }
    }
}

internal interface LayerRenderer {
    val parentState: SharedElementInternalState?
    fun drawInOverlay(drawScope: DrawScope)
    val zIndex: Float
}

private val DefaultSpring = spring(
    stiffness = StiffnessMediumLow,
    visibilityThreshold = Rect.VisibilityThreshold
)

@ExperimentalSharedTransitionApi
private val ParentClip: SharedTransitionScope.OverlayClip =
    object : SharedTransitionScope.OverlayClip {
        override fun getClipPath(
            state: SharedTransitionScope.SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density
        ): Path? {
            return state.parentSharedContentState?.clipPathInOverlay
        }
    }

private val DefaultClipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? =
    { _, _ -> null }

@ExperimentalSharedTransitionApi
private val DefaultBoundsTransform = BoundsTransform { _, _ -> DefaultSpring }
