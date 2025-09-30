/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableNode
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection

/**
 * Configure a component to act as a scrollable area. A scrollable area clips its content to its
 * bounds, renders overscroll, and handles scroll gestures such that the content, not the viewport,
 * moves with the user's gestures.
 *
 * This modifier is a building block for creating custom scrollable containers, and serves as a
 * higher-level abstraction over [androidx.compose.foundation.gestures.scrollable]. For simpler use
 * cases, prefer higher-level components that are built with `scrollableArea`, such as
 * [verticalScroll] and [androidx.compose.foundation.lazy.LazyColumn]. For example, [verticalScroll]
 * offsets the content in the viewport out of the box to have scrollable container behavior.
 *
 * The main difference between this modifier and `scrollable` is how it handles scroll direction.
 * For a scrollable area, a user's "scroll up" gesture should move the content *up*, which means the
 * scroll position of the layout in the viewport is actually moving *down*. `scrollableArea`
 * performs this inversion for you, accounting for [orientation], [LayoutDirection], and the
 * [reverseScrolling] flag by using [ScrollableDefaults.reverseDirection].
 *
 * In contrast, the lower-level `[androidx.compose.foundation.gestures.scrollable]` modifier reports
 * raw scroll deltas. By default, these deltas are not inverted, meaning that moving a pointer up on
 * the screen reports a negative delta. This is useful for use cases that are not directly related
 * to moving content.
 *
 * This `scrollableArea` overload uses overscroll provided through [LocalOverscrollFactory] by
 * default. See the other overload to manually provide an [OverscrollEffect] instance, or disable
 * overscroll.
 *
 * @sample androidx.compose.foundation.samples.ScrollableAreaSample
 * @param state The [ScrollableState] of the component.
 * @param orientation The [Orientation] of scrolling.
 * @param enabled Whether scrolling is enabled.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param reverseScrolling Reverse the direction of scrolling. When `false` (the default), the
 *   content is anchored to the top (for vertical scrolling) or start (for horizontal scrolling). A
 *   scroll position of 0 means the first item is placed at the beginning of the viewport. When
 *   `true`, the content is anchored to the bottom or end. A scroll position of 0 means the last
 *   item is placed at the end of the viewport. This is useful for use cases like a chat feed where
 *   new items appear at the bottom and the list grows upwards. When `reverseScrolling` is `true`,
 *   the layout of the content inside the container should also be reversed.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this scrollable area. Note that if `null` is provided, interactions
 *   will still happen internally.
 * @param bringIntoViewSpec The configuration that this scrollable area should use to perform
 *   scrolling when scroll requests are received from the focus system. If null is provided the
 *   system will use the behavior provided by
 *   [androidx.compose.foundation.gestures.LocalBringIntoViewSpec] which by default has a platform
 *   dependent implementation.
 */
fun Modifier.scrollableArea(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseScrolling: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    bringIntoViewSpec: BringIntoViewSpec? = null,
): Modifier {
    return clipScrollableContainer(orientation)
        .then(
            ScrollableAreaElement(
                state = state,
                orientation = orientation,
                enabled = enabled,
                reverseScrolling = reverseScrolling,
                flingBehavior = flingBehavior,
                interactionSource = interactionSource,
                bringIntoViewSpec = bringIntoViewSpec,
                useLocalOverscrollFactory = true,
                overscrollEffect = null,
            )
        )
}

/**
 * Configure a component to act as a scrollable area. A scrollable area clips its content to its
 * bounds, renders overscroll, and handles scroll gestures such that the content, not the viewport,
 * moves with the user's gestures.
 *
 * This modifier is a building block for creating custom scrollable containers, and serves as a
 * higher-level abstraction over [androidx.compose.foundation.gestures.scrollable]. For simpler use
 * cases, prefer higher-level components that are built with `scrollableArea`, such as
 * [verticalScroll] and [androidx.compose.foundation.lazy.LazyColumn]. For example, [verticalScroll]
 * offsets the content in the viewport out of the box to have scrollable container behavior.
 *
 * The main difference between this modifier and `scrollable` is how it handles scroll direction.
 * For a scrollable area, a user's "scroll up" gesture should move the content *up*, which means the
 * scroll position of the layout in the viewport is actually moving *down*. `scrollableArea`
 * performs this inversion for you, accounting for [orientation], [LayoutDirection], and the
 * [reverseScrolling] flag by using [ScrollableDefaults.reverseDirection].
 *
 * In contrast, the lower-level `[androidx.compose.foundation.gestures.scrollable]` modifier reports
 * raw scroll deltas. By default, these deltas are not inverted, meaning that moving a pointer up on
 * the screen reports a negative delta. This is useful for use cases that are not directly related
 * to moving content.
 *
 * This overload allows providing [OverscrollEffect] that will be rendered within the scrollable
 * area. See the other overload of `scrollableArea` in order to use a default [OverscrollEffect]
 * provided by [LocalOverscrollFactory].
 *
 * @sample androidx.compose.foundation.samples.ScrollableAreaSample
 * @param state The [ScrollableState] of the component.
 * @param orientation The [Orientation] of scrolling.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   scrollable area. Note that the [OverscrollEffect.node] will be applied internally as well - you
 *   do not need to use Modifier.overscroll separately.
 * @param enabled Whether scrolling is enabled.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param reverseScrolling Reverse the direction of scrolling. When `false` (the default), the
 *   content is anchored to the top (for vertical scrolling) or start (for horizontal scrolling). A
 *   scroll position of 0 means the first item is placed at the beginning of the viewport. When
 *   `true`, the content is anchored to the bottom or end. A scroll position of 0 means the last
 *   item is placed at the end of the viewport. This is useful for use cases like a chat feed where
 *   new items appear at the bottom and the list grows upwards. When `reverseScrolling` is `true`,
 *   the layout of the content inside the container should also be reversed.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this scrollable area. Note that if `null` is provided, interactions
 *   will still happen internally.
 * @param bringIntoViewSpec The configuration that this scrollable area should use to perform
 *   scrolling when scroll requests are received from the focus system. If null is provided the
 *   system will use the behavior provided by
 *   [androidx.compose.foundation.gestures.LocalBringIntoViewSpec] which by default has a platform
 *   dependent implementation.
 */
fun Modifier.scrollableArea(
    state: ScrollableState,
    orientation: Orientation,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    reverseScrolling: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    bringIntoViewSpec: BringIntoViewSpec? = null,
): Modifier {
    return clipScrollableContainer(orientation)
        .then(
            ScrollableAreaElement(
                state = state,
                orientation = orientation,
                enabled = enabled,
                reverseScrolling = reverseScrolling,
                flingBehavior = flingBehavior,
                interactionSource = interactionSource,
                bringIntoViewSpec = bringIntoViewSpec,
                useLocalOverscrollFactory = false,
                overscrollEffect = overscrollEffect,
            )
        )
}

/**
 * Applies clipping and wraps [androidx.compose.foundation.gestures.scrollable] and automatically
 * calculates reverseDirection using [ScrollableDefaults.reverseDirection] based on the provided
 * [orientation] and [reverseScrolling] parameters, and the resolved [LayoutDirection].
 */
private class ScrollableAreaElement(
    private val state: ScrollableState,
    private val orientation: Orientation,
    private val enabled: Boolean,
    private val reverseScrolling: Boolean,
    private val flingBehavior: FlingBehavior?,
    private val interactionSource: MutableInteractionSource?,
    private val bringIntoViewSpec: BringIntoViewSpec?,
    private val useLocalOverscrollFactory: Boolean,
    private val overscrollEffect: OverscrollEffect?,
) : ModifierNodeElement<ScrollableAreaNode>() {
    override fun create(): ScrollableAreaNode {
        return ScrollableAreaNode(
            state = state,
            orientation = orientation,
            enabled = enabled,
            reverseScrolling = reverseScrolling,
            flingBehavior = flingBehavior,
            interactionSource = interactionSource,
            bringIntoViewSpec = bringIntoViewSpec,
            useLocalOverscrollFactory = useLocalOverscrollFactory,
            userProvidedOverscrollEffect = overscrollEffect,
        )
    }

    override fun update(node: ScrollableAreaNode) {
        node.update(
            state = state,
            orientation = orientation,
            useLocalOverscrollFactory = useLocalOverscrollFactory,
            overscrollEffect = overscrollEffect,
            enabled = enabled,
            reverseScrolling = reverseScrolling,
            flingBehavior = flingBehavior,
            interactionSource = interactionSource,
            bringIntoViewSpec = bringIntoViewSpec,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollableArea"
        properties["state"] = state
        properties["orientation"] = orientation
        if (!useLocalOverscrollFactory) {
            properties["overscrollEffect"] = overscrollEffect
        }
        properties["enabled"] = enabled
        properties["reverseScrolling"] = reverseScrolling
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
        properties["bringIntoViewSpec"] = bringIntoViewSpec
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ScrollableAreaElement

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (enabled != other.enabled) return false
        if (reverseScrolling != other.reverseScrolling) return false
        if (flingBehavior != other.flingBehavior) return false
        if (interactionSource != other.interactionSource) return false
        if (bringIntoViewSpec != other.bringIntoViewSpec) return false
        if (useLocalOverscrollFactory != other.useLocalOverscrollFactory) return false
        if (overscrollEffect != other.overscrollEffect) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + reverseScrolling.hashCode()
        result = 31 * result + (flingBehavior?.hashCode() ?: 0)
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        result = 31 * result + (bringIntoViewSpec?.hashCode() ?: 0)
        result = 31 * result + useLocalOverscrollFactory.hashCode()
        result = 31 * result + (overscrollEffect?.hashCode() ?: 0)
        return result
    }
}

private class ScrollableAreaNode(
    private var state: ScrollableState,
    private var orientation: Orientation,
    private var enabled: Boolean,
    private var reverseScrolling: Boolean,
    private var flingBehavior: FlingBehavior?,
    private var interactionSource: MutableInteractionSource?,
    private var bringIntoViewSpec: BringIntoViewSpec?,
    private var useLocalOverscrollFactory: Boolean,
    private var userProvidedOverscrollEffect: OverscrollEffect?,
) : DelegatingNode(), CompositionLocalConsumerModifierNode, ObserverModifierNode {
    override val shouldAutoInvalidate = false
    private var scrollableNode: ScrollableNode? = null
    private var overscrollNode: DelegatableNode? = null
    private var localOverscrollFactory: OverscrollFactory? = null
    private var localOverscrollFactoryCreatedOverscrollEffect: OverscrollEffect? = null
    private var shouldReverseDirection = false

    fun getOverscrollEffect(): OverscrollEffect? =
        if (useLocalOverscrollFactory) {
            localOverscrollFactoryCreatedOverscrollEffect
        } else {
            userProvidedOverscrollEffect
        }

    override fun onAttach() {
        shouldReverseDirection = shouldReverseDirection()
        attachOverscrollNodeIfNeeded()
        if (scrollableNode == null) {
            scrollableNode =
                delegate(
                    ScrollableNode(
                        state,
                        getOverscrollEffect(),
                        flingBehavior,
                        orientation,
                        enabled,
                        shouldReverseDirection,
                        interactionSource,
                        bringIntoViewSpec,
                    )
                )
        }
    }

    override fun onDetach() {
        overscrollNode?.let { undelegate(it) }
    }

    override fun onLayoutDirectionChange() {
        val reverseDirection = shouldReverseDirection()
        if (shouldReverseDirection != reverseDirection) {
            shouldReverseDirection = reverseDirection
            update(
                state,
                orientation,
                useLocalOverscrollFactory,
                getOverscrollEffect(),
                enabled,
                reverseScrolling,
                flingBehavior,
                interactionSource,
                bringIntoViewSpec,
            )
        }
    }

    fun update(
        state: ScrollableState,
        orientation: Orientation,
        useLocalOverscrollFactory: Boolean,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        reverseScrolling: Boolean,
        flingBehavior: FlingBehavior?,
        interactionSource: MutableInteractionSource?,
        bringIntoViewSpec: BringIntoViewSpec?,
    ) {
        this.state = state
        this.orientation = orientation
        var useLocalOverscrollFactoryChanged = false
        if (this.useLocalOverscrollFactory != useLocalOverscrollFactory) {
            useLocalOverscrollFactoryChanged = true
            this.useLocalOverscrollFactory = useLocalOverscrollFactory
        }
        var overscrollEffectChanged = false
        if (this.userProvidedOverscrollEffect != overscrollEffect) {
            overscrollEffectChanged = true
            this.userProvidedOverscrollEffect = overscrollEffect
        }
        if (
            useLocalOverscrollFactoryChanged ||
                // If the overscroll effect changed but we are still using the local factory, this
                // should no-op
                overscrollEffectChanged && !useLocalOverscrollFactory
        ) {
            overscrollNode?.let { undelegate(it) }
            overscrollNode = null
            attachOverscrollNodeIfNeeded()
        }
        this.enabled = enabled
        this.reverseScrolling = reverseScrolling
        this.flingBehavior = flingBehavior
        this.interactionSource = interactionSource
        this.bringIntoViewSpec = bringIntoViewSpec
        this.shouldReverseDirection = shouldReverseDirection()

        scrollableNode?.update(
            state,
            orientation,
            getOverscrollEffect(),
            enabled,
            shouldReverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec,
        )
    }

    fun shouldReverseDirection(): Boolean {
        var layoutDirection = LayoutDirection.Ltr
        if (isAttached) {
            layoutDirection = requireLayoutDirection()
        }
        return ScrollableDefaults.reverseDirection(layoutDirection, orientation, reverseScrolling)
    }

    private fun attachOverscrollNodeIfNeeded() {
        if (overscrollNode == null) {
            // Overrides overscrollEffect if set
            if (useLocalOverscrollFactory) {
                observeReads {
                    localOverscrollFactory = currentValueOf(LocalOverscrollFactory)
                    localOverscrollFactoryCreatedOverscrollEffect =
                        localOverscrollFactory?.createOverscrollEffect()
                }
            }
            val effect = getOverscrollEffect()
            if (effect != null) {
                val node = effect.node
                if (!node.node.isAttached) {
                    overscrollNode = delegate(node)
                }
            }
        } else {
            // If we already have a node, re-delegate to it if needed. This will no-op if we are
            // already delegating to it.
            overscrollNode?.let {
                if (!it.node.isAttached) {
                    delegate(it)
                }
            }
        }
    }

    override fun onObservedReadsChanged() {
        val factory = currentValueOf(LocalOverscrollFactory)
        if (factory != localOverscrollFactory) {
            localOverscrollFactory = factory
            localOverscrollFactoryCreatedOverscrollEffect = null
            overscrollNode?.let { undelegate(it) }
            overscrollNode = null
            attachOverscrollNodeIfNeeded()
            scrollableNode?.update(
                state,
                orientation,
                getOverscrollEffect(),
                enabled,
                shouldReverseDirection,
                flingBehavior,
                interactionSource,
                bringIntoViewSpec,
            )
        }
    }
}
