/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.compose.animation.scene.content

import android.annotation.SuppressLint
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.compose.animation.scene.Ancestor
import com.android.compose.animation.scene.AnimatedState
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.Element
import com.android.compose.animation.scene.ElementContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.ElementScope
import com.android.compose.animation.scene.ElementStateScope
import com.android.compose.animation.scene.ElementWithValues
import com.android.compose.animation.scene.InternalContentScope
import com.android.compose.animation.scene.MovableElement
import com.android.compose.animation.scene.MovableElementContentScope
import com.android.compose.animation.scene.MovableElementKey
import com.android.compose.animation.scene.NestedSceneTransitionLayoutState
import com.android.compose.animation.scene.Scale
import com.android.compose.animation.scene.SceneTransitionLayoutForTesting
import com.android.compose.animation.scene.SceneTransitionLayoutImpl
import com.android.compose.animation.scene.SceneTransitionLayoutScope
import com.android.compose.animation.scene.SceneTransitionLayoutState
import com.android.compose.animation.scene.SharedValueType
import com.android.compose.animation.scene.UserAction
import com.android.compose.animation.scene.UserActionResult
import com.android.compose.animation.scene.ValueKey
import com.android.compose.animation.scene.animateSharedValueAsState
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.animation.scene.effect.GestureEffect
import com.android.compose.animation.scene.element
import com.android.compose.animation.scene.elementAlpha
import com.android.compose.animation.scene.elementState
import com.android.compose.animation.scene.getAllNestedTransitionStates
import com.android.compose.animation.scene.getScale
import com.android.compose.animation.scene.modifiers.noResizeDuringTransitions
import com.android.compose.gesture.NestedScrollControlState
import com.android.compose.gesture.NestedScrollableBound
import com.android.compose.gesture.nestedScrollController
import com.android.compose.modifiers.thenIf
import com.android.compose.ui.graphics.ContainerNode
import com.android.compose.ui.graphics.ContainerState
import kotlin.math.pow

/** A content defined in a [SceneTransitionLayout], i.e. a scene or an overlay. */
@Stable
internal sealed class Content(
    open val key: ContentKey,
    val layoutImpl: SceneTransitionLayoutImpl,
    content: @Composable InternalContentScope.() -> Unit,
    actions: Map<UserAction.Resolved, UserActionResult>,
    zIndex: Float,
    globalZIndex: Long,
    effectFactory: OverscrollFactory,
    val alwaysCompose: Boolean,
) {
    private val nestedScrollControlState = NestedScrollControlState()
    internal val scope = ContentScopeImpl(layoutImpl, content = this, nestedScrollControlState)
    val containerState = ContainerState()

    // Important: All fields in this class should be backed by State given that contents are updated
    // directly during composition, outside of a SideEffect, or are observed during composition,
    // layout or drawing.
    var content by mutableStateOf(content)
    var targetSize by mutableStateOf(Element.SizeUnspecified)
    var userActions by mutableStateOf(actions)
    var zIndex by mutableFloatStateOf(zIndex)

    /**
     * The globalZIndex is a zIndex that indicates the z order of each content across any nested
     * STLs. This is done by dividing the number range of a Long into chunks of three digits. As
     * Long.MAX_VALUE is a bit larger than 1e18 we start the first level at 1e15 to give at least
     * 1000 contents space. The first level of nesting depth will occupy the 3 highest digits and
     * with each level we continue into the next three. Therefore the parent z order will have
     * priority and their children have room to order themselves within the "less significant bits".
     *
     * As an example, imagine the following tree of nested scenes:
     * ```
     *      /     \
     *    A01     A02  -- nestingDepth 0
     *  /    \     |
     * B01   B02  C01  -- nestingDepth 1
     *        |
     *       D01       -- nestingDepth 2
     * ```
     *
     * The zIndex values would be:
     * ```
     * A01:        1e15 (1_000_000_000_000_000)
     * A02:        2e15 (2_000_000_000_000_000)
     * B01:    1.001e15 (1_001_000_000_000_000)
     * B02:    1.002e15 (1_002_000_000_000_000)
     * C01:    2.001e15 (2_001_000_000_000_000)
     * D01: 1.002001e15 (1_002_001_000_000_000)
     * ```
     *
     * Therefore the order of zIndexes will correctly be: A01, B01, B02, D01, A02, C01, which
     * corresponds to a Pre-order traversal of the tree.
     *
     * Since composition of the tree does not happen all at once we can't do a Pre-order traversal
     * right away without allocating resources to build and manage the tree structure through all
     * updates. Using this method we have stable zIndexes at time of composition of each content
     * independently with the only drawback that contents per each STL are limited to 999 and
     * nesting depth is limited to 6 (18 / 3).
     */
    var globalZIndex by mutableLongStateOf(globalZIndex)

    companion object {
        fun calculateGlobalZIndex(
            parentGlobalZIndex: Long,
            localZIndex: Int,
            nestingDepth: Int,
        ): Long {
            require(nestingDepth in 0..5) { "NestingDepth of STLs can be at most 5." }
            require(localZIndex in 1..999) { "A scene can have at most 999 contents." }
            val offsetForDepth = 10.0.pow((5 - nestingDepth) * 3).toLong()
            return parentGlobalZIndex + offsetForDepth * localZIndex
        }
    }

    private var lastFactory by mutableStateOf(effectFactory)
    var verticalEffects by mutableStateOf(ContentEffects(effectFactory))
        private set

    var horizontalEffects by mutableStateOf(ContentEffects(effectFactory))
        private set

    @SuppressLint("NotConstructor")
    @Composable
    fun Content(modifier: Modifier = Modifier, isInvisible: Boolean = false) {
        // If this content has a custom factory, provide it to the content so that the factory is
        // automatically used when calling rememberOverscrollEffect().
        val isElevationPossible =
            layoutImpl.state.isElevationPossible(content = key, element = null)

        val content =
            @Composable {
                Box(
                    modifier.then(ContentElement(this, isElevationPossible)).thenIf(
                        layoutImpl.implicitTestTags
                    ) {
                        Modifier.testTag(key.testTag)
                    }
                ) {
                    CompositionLocalProvider(LocalOverscrollFactory provides lastFactory) {
                        scope.content()
                    }
                }
            }

        if (alwaysCompose) {
            AlwaysComposedContent(isInvisible, content)
        } else {
            content()
        }
    }

    @Composable
    private fun AlwaysComposedContent(isInvisible: Boolean, content: @Composable () -> Unit) {
        val maxState = if (isInvisible) Lifecycle.State.CREATED else Lifecycle.State.RESUMED
        val parentLifecycle = LocalLifecycleOwner.current.lifecycle
        val lifecycleOwner =
            remember(parentLifecycle) { RestrictedLifecycleOwner(parentLifecycle, maxState) }
        DisposableEffect(lifecycleOwner) { onDispose { lifecycleOwner.destroy() } }

        if (maxState != lifecycleOwner.maxLifecycleState) {
            SideEffect { lifecycleOwner.maxLifecycleState = maxState }
        }

        CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner, content)
    }

    fun areNestedSwipesAllowed(): Boolean = nestedScrollControlState.isOuterScrollAllowed

    fun maybeUpdateEffects(effectFactory: OverscrollFactory) {
        if (effectFactory != lastFactory) {
            lastFactory = effectFactory
            verticalEffects = ContentEffects(effectFactory)
            horizontalEffects = ContentEffects(effectFactory)
        }
    }
}

private data class ContentElement(
    private val content: Content,
    private val isElevationPossible: Boolean,
) : ModifierNodeElement<ContentNode>() {
    override fun create(): ContentNode = ContentNode(content, isElevationPossible)

    override fun update(node: ContentNode) {
        node.update(content, isElevationPossible)
    }
}

private class ContentNode(private var content: Content, private var isElevationPossible: Boolean) :
    DelegatingNode(), ApproachLayoutModifierNode {
    private var containerDelegate = containerDelegate(isElevationPossible)

    private fun containerDelegate(isElevationPossible: Boolean): ContainerNode? {
        return if (isElevationPossible) delegate(ContainerNode(content.containerState)) else null
    }

    override fun onDetach() {
        this.content.targetSize = Element.SizeUnspecified
    }

    fun update(content: Content, isElevationPossible: Boolean) {
        if (content != this.content) {
            this.content.targetSize = Element.SizeUnspecified
            this.content = content
        }

        if (content != this.content || isElevationPossible != this.isElevationPossible) {
            this.isElevationPossible = isElevationPossible

            containerDelegate?.let { undelegate(it) }
            containerDelegate = containerDelegate(isElevationPossible)
        }
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean = false

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        check(isLookingAhead)
        return measurable.measure(constraints).run {
            content.targetSize = IntSize(width, height)
            layout(width, height) { place(0, 0) }
        }
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        return measurable.measure(constraints).run { layout(width, height) { place(0, 0) } }
    }
}

internal class ContentEffects(factory: OverscrollFactory) {
    val overscrollEffect = factory.createOverscrollEffect()
    val gestureEffect = GestureEffect(overscrollEffect)
}

internal class ContentScopeImpl(
    private val layoutImpl: SceneTransitionLayoutImpl,
    private val content: Content,
    private val nestedScrollControlState: NestedScrollControlState,
) : InternalContentScope, ElementStateScope by layoutImpl.elementStateScope {
    override val contentKey: ContentKey
        get() = content.key

    override val layoutState: SceneTransitionLayoutState =
        if (layoutImpl.ancestors.isEmpty()) {
            layoutImpl.state
        } else {
            NestedSceneTransitionLayoutState(
                ancestors = layoutImpl.ancestors.fastMap { it.layoutImpl.state },
                delegate = layoutImpl.state,
            )
        }

    override val lookaheadScope: LookaheadScope
        get() = layoutImpl.lookaheadScope

    override val verticalOverscrollEffect: OverscrollEffect
        get() = content.verticalEffects.overscrollEffect

    override val horizontalOverscrollEffect: OverscrollEffect
        get() = content.horizontalEffects.overscrollEffect

    override fun Modifier.element(key: ElementKey): Modifier {
        return element(layoutImpl, content, key)
    }

    @Composable
    override fun Element(
        key: ElementKey,
        modifier: Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Element(layoutImpl, this@ContentScopeImpl.content, key, modifier, content)
    }

    @Composable
    override fun ElementWithValues(
        key: ElementKey,
        modifier: Modifier,
        content: @Composable (ElementScope<ElementContentScope>.() -> Unit),
    ) {
        ElementWithValues(layoutImpl, this@ContentScopeImpl.content, key, modifier, content)
    }

    @Composable
    override fun MovableElement(
        key: MovableElementKey,
        modifier: Modifier,
        content: @Composable (ElementScope<MovableElementContentScope>.() -> Unit),
    ) {
        MovableElement(layoutImpl, this@ContentScopeImpl.content, key, modifier, content)
    }

    @Composable
    override fun <T> animateContentValueAsState(
        value: T,
        key: ValueKey,
        type: SharedValueType<T, *>,
        canOverflow: Boolean,
    ): AnimatedState<T> {
        return animateSharedValueAsState(
            layoutImpl = layoutImpl,
            content = content.key,
            element = null,
            key = key,
            value = value,
            type = type,
            canOverflow = canOverflow,
        )
    }

    override fun Modifier.noResizeDuringTransitions(): Modifier {
        return noResizeDuringTransitions(layoutState = layoutImpl.state)
    }

    override fun Modifier.disableSwipesWhenScrolling(bounds: NestedScrollableBound): Modifier {
        return nestedScrollController(nestedScrollControlState, bounds)
    }

    @Composable
    override fun NestedSceneTransitionLayout(
        state: SceneTransitionLayoutState,
        modifier: Modifier,
        builder: SceneTransitionLayoutScope<ContentScope>.() -> Unit,
    ) {
        NestedSceneTransitionLayoutForTesting(state, modifier, null, builder)
    }

    @Composable
    override fun NestedSceneTransitionLayoutForTesting(
        state: SceneTransitionLayoutState,
        modifier: Modifier,
        onLayoutImpl: ((SceneTransitionLayoutImpl) -> Unit)?,
        builder: SceneTransitionLayoutScope<InternalContentScope>.() -> Unit,
    ) {
        val ancestors =
            remember(layoutImpl, contentKey, layoutImpl.ancestors) {
                layoutImpl.ancestors + Ancestor(layoutImpl, contentKey)
            }
        SceneTransitionLayoutForTesting(
            state,
            modifier,
            onLayoutImpl = onLayoutImpl,
            builder = builder,
            sharedElementMap = layoutImpl.elements,
            ancestors = ancestors,
            lookaheadScope = layoutImpl.lookaheadScope,
            implicitTestTags = layoutImpl.implicitTestTags,
        )
    }

    override fun isAlwaysComposedContentVisible(): Boolean {
        return isAlwaysComposedContentVisible(layoutImpl.state, contentKey) &&
            layoutImpl.ancestors.all { ancestor ->
                isAlwaysComposedContentVisible(ancestor.layoutImpl.state, ancestor.inContent)
            }
    }

    private fun isAlwaysComposedContentVisible(
        layoutState: SceneTransitionLayoutState,
        content: ContentKey,
    ): Boolean {
        return layoutState.currentScene == content ||
            layoutState.currentOverlays.contains(content) ||
            layoutState.currentTransitions.fastAny {
                it.fromContent == content || it.toContent == content
            }
    }

    override fun ElementKey.currentAlpha(): Float? {
        val element = layoutImpl.elements[this] ?: return null
        val stateInContent = element.stateByContent[contentKey] ?: return null
        val elementState =
            elementState(layoutImpl, element, getAllNestedTransitionStates(layoutImpl))
                ?: return null
        val transition = elementState as? TransitionState.Transition
        return elementAlpha(layoutImpl, element, transition, stateInContent)
    }

    override fun ElementKey.currentScale(): Scale? {
        val element = layoutImpl.elements[this] ?: return null
        val stateInContent = element.stateByContent[contentKey] ?: return null
        val elementState =
            elementState(layoutImpl, element, getAllNestedTransitionStates(layoutImpl))
                ?: return null
        val transition = elementState as? TransitionState.Transition
        return getScale(layoutImpl, element, transition, stateInContent)
    }
}

/** A [LifecycleOwner] that follows its [parentLifecycle] but is capped at [maxLifecycleState]. */
private class RestrictedLifecycleOwner(
    val parentLifecycle: Lifecycle,
    maxLifecycleState: Lifecycle.State,
) : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this)

    var maxLifecycleState = maxLifecycleState
        set(value) {
            field = value
            updateState()
        }

    private val observer = LifecycleEventObserver { _, _ -> updateState() }

    init {
        updateState()
        parentLifecycle.addObserver(observer)
    }

    private fun updateState() {
        lifecycle.currentState = minOf(this.maxLifecycleState, parentLifecycle.currentState)
    }

    fun destroy() {
        parentLifecycle.removeObserver(observer)
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }
}
