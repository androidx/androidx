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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection

// TODO b/316559454 to make it public
/** Scrolling related information to transform a layout into a "Scrollable Container" */
internal fun Modifier.scrollingContainer(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean,
    reverseScrolling: Boolean,
    flingBehavior: FlingBehavior?,
    interactionSource: MutableInteractionSource?,
    overscrollEffect: OverscrollEffect?,
    bringIntoViewSpec: BringIntoViewSpec? = null
): Modifier {
    return this.then(
            ScrollingContainerElement(
                state = state,
                orientation = orientation,
                enabled = enabled,
                reverseScrolling = reverseScrolling,
                flingBehavior = flingBehavior,
                interactionSource = interactionSource,
                bringIntoViewSpec = bringIntoViewSpec,
                overscrollEffect = overscrollEffect
            )
        )
        .then(if (overscrollEffect == null) Modifier else Modifier.overscroll(overscrollEffect))
}

/**
 * Applies clipping and wraps [androidx.compose.foundation.gestures.scrollable] and automatically
 * calculates reverseDirection using [ScrollableDefaults.reverseDirection] based on the provided
 * [orientation] and [reverseScrolling] parameters, and the resolved [LayoutDirection].
 */
private class ScrollingContainerElement(
    private val state: ScrollableState,
    private val orientation: Orientation,
    private val enabled: Boolean,
    private val reverseScrolling: Boolean,
    private val flingBehavior: FlingBehavior?,
    private val interactionSource: MutableInteractionSource?,
    private val bringIntoViewSpec: BringIntoViewSpec?,
    private val overscrollEffect: OverscrollEffect?
) : ModifierNodeElement<ScrollingContainerNode>() {
    override fun create(): ScrollingContainerNode {
        return ScrollingContainerNode(
            state = state,
            orientation = orientation,
            enabled = enabled,
            reverseScrolling = reverseScrolling,
            flingBehavior = flingBehavior,
            interactionSource = interactionSource,
            bringIntoViewSpec = bringIntoViewSpec,
            overscrollEffect = overscrollEffect
        )
    }

    override fun update(node: ScrollingContainerNode) {
        node.update(
            state = state,
            orientation = orientation,
            overscrollEffect = overscrollEffect,
            enabled = enabled,
            reverseScrolling = reverseScrolling,
            flingBehavior = flingBehavior,
            interactionSource = interactionSource,
            bringIntoViewSpec = bringIntoViewSpec
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollingContainer"
        properties["state"] = state
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseScrolling"] = reverseScrolling
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
        properties["bringIntoViewSpec"] = bringIntoViewSpec
        properties["overscrollEffect"] = overscrollEffect
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ScrollingContainerElement

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (enabled != other.enabled) return false
        if (reverseScrolling != other.reverseScrolling) return false
        if (flingBehavior != other.flingBehavior) return false
        if (interactionSource != other.interactionSource) return false
        if (bringIntoViewSpec != other.bringIntoViewSpec) return false
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
        result = 31 * result + (overscrollEffect?.hashCode() ?: 0)
        return result
    }
}

private class ScrollingContainerNode(
    private var state: ScrollableState,
    private var orientation: Orientation,
    private var enabled: Boolean,
    private var reverseScrolling: Boolean,
    private var flingBehavior: FlingBehavior?,
    private var interactionSource: MutableInteractionSource?,
    private var bringIntoViewSpec: BringIntoViewSpec?,
    private var overscrollEffect: OverscrollEffect?
) : DelegatingNode(), LayoutModifierNode {
    override val shouldAutoInvalidate = false
    private var scrollableNode: ScrollableNode? = null
    private var shouldReverseDirection = false

    // Needs to be mutated to properly update the underlying layer, which relies on instance
    // equality
    private var layerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape =
            if (orientation == Orientation.Vertical) VerticalScrollableClipShape
            else HorizontalScrollableClipShape
    }

    override fun onAttach() {
        shouldReverseDirection = shouldReverseDirection()
        if (scrollableNode == null) {
            scrollableNode =
                delegate(
                    ScrollableNode(
                        state,
                        overscrollEffect,
                        flingBehavior,
                        orientation,
                        enabled,
                        shouldReverseDirection,
                        interactionSource,
                        bringIntoViewSpec
                    )
                )
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        // Note: this is functionally the same as Modifier.clip, but inlined to reduce nodes.
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    override fun onLayoutDirectionChange() {
        val reverseDirection = shouldReverseDirection()
        if (shouldReverseDirection != reverseDirection) {
            shouldReverseDirection = reverseDirection
            update(
                state,
                orientation,
                overscrollEffect,
                enabled,
                reverseScrolling,
                flingBehavior,
                interactionSource,
                bringIntoViewSpec
            )
        }
    }

    fun update(
        state: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        reverseScrolling: Boolean,
        flingBehavior: FlingBehavior?,
        interactionSource: MutableInteractionSource?,
        bringIntoViewSpec: BringIntoViewSpec?
    ) {
        this.state = state
        if (this.orientation != orientation) {
            this.orientation = orientation
            this.layerBlock = {
                clip = true
                shape =
                    if (orientation == Orientation.Vertical) VerticalScrollableClipShape
                    else HorizontalScrollableClipShape
            }
            invalidatePlacement()
        }
        this.overscrollEffect = overscrollEffect
        this.enabled = enabled
        this.reverseScrolling = reverseScrolling
        this.flingBehavior = flingBehavior
        this.interactionSource = interactionSource
        this.bringIntoViewSpec = bringIntoViewSpec
        this.shouldReverseDirection = shouldReverseDirection()

        scrollableNode?.update(
            state,
            orientation,
            overscrollEffect,
            enabled,
            shouldReverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec
        )
    }

    fun shouldReverseDirection(): Boolean {
        var layoutDirection = LayoutDirection.Ltr
        if (isAttached) {
            layoutDirection = requireLayoutDirection()
        }
        return ScrollableDefaults.reverseDirection(layoutDirection, orientation, reverseScrolling)
    }
}
