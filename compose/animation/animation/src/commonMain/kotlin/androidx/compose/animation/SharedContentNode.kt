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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.util.fastRoundToInt

internal data class SharedBoundsNodeElement(val sharedElementState: SharedElementInternalState) :
    ModifierNodeElement<SharedBoundsNode>() {
    override fun create(): SharedBoundsNode = SharedBoundsNode(sharedElementState)

    override fun update(node: SharedBoundsNode) {
        node.state = sharedElementState
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sharedBounds"
        properties["sharedElementState"] = sharedElementState
    }
}

/**
 * SharedContentNode is a Modifier.Node that dynamically acquire target bounds and animating the
 * layout bounds for Modifier.sharedElement and Modifier.sharedBounds.
 *
 * The target bounds are calculated during the lookahead pass based for the node that is becoming
 * visible. Once the target bounds are calculated, the bounds animation will happen during the
 * approach pass.
 */
internal class SharedBoundsNode(
    state: SharedElementInternalState,
) : ApproachLayoutModifierNode, Modifier.Node(), DrawModifierNode, ModifierLocalModifierNode {
    private val rootCoords: LayoutCoordinates
        get() = sharedElement.scope.root

    private val rootLookaheadCoords: LayoutCoordinates
        get() = sharedElement.scope.lookaheadRoot

    var state: SharedElementInternalState = state
        internal set(value) {
            if (value != field) {
                // State changed!
                field = value
                if (isAttached) {
                    provide(ModifierLocalSharedElementInternalState, value)
                    state.parentState = ModifierLocalSharedElementInternalState.current
                    state.layer = layer
                    state.lookaheadCoords = { requireLookaheadLayoutCoordinates() }
                }
            }
        }

    private fun requireLookaheadLayoutCoordinates(): LayoutCoordinates =
        with(state.sharedElement.scope) { requireLayoutCoordinates().toLookaheadCoordinates() }

    private val boundsAnimation: BoundsAnimation
        get() = state.boundsAnimation

    private var layer: GraphicsLayer? = state.layer
        set(value) {
            if (value == null) {
                field?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
            } else {
                state.layer = value
            }
            field = value
        }

    private val sharedElement: SharedElement
        get() = state.sharedElement

    override val providedValues =
        modifierLocalMapOf(ModifierLocalSharedElementInternalState to state)

    override fun onAttach() {
        super.onAttach()
        provide(ModifierLocalSharedElementInternalState, state)
        state.parentState = ModifierLocalSharedElementInternalState.current
        layer = requireGraphicsContext().createGraphicsLayer()
        state.lookaheadCoords = { requireLookaheadLayoutCoordinates() }
    }

    override fun onDetach() {
        super.onDetach()
        layer = null
        state.parentState = null
        state.lookaheadCoords = { null }
    }

    override fun onReset() {
        super.onReset()
        // Reset layer
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = requireGraphicsContext().createGraphicsLayer()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Lookahead pass: Record lookahead size and lookahead coordinates
        val placeable = measurable.measure(constraints)
        val lookaheadSize = Size(placeable.width.toFloat(), placeable.height.toFloat())
        return layout(placeable.width, placeable.height) {
            val topLeft =
                coordinates?.let {
                    rootLookaheadCoords.localPositionOf(it, Offset.Zero).also { topLeft ->
                        if (sharedElement.currentBounds == null) {
                            sharedElement.currentBounds = Rect(topLeft, lookaheadSize)
                        }
                    }
                }
            placeable.place(0, 0)
            // Update the lookahead result after child placement, so that child has an
            // opportunity to use its placement to influence the bounds animation.
            topLeft?.let { sharedElement.onLookaheadResult(state, lookaheadSize, it) }
        }
    }

    private fun MeasureScope.place(placeable: Placeable): MeasureResult {
        if (!sharedElement.foundMatch) {
            // No match
            return layout(placeable.width, placeable.height) {
                // Update currentBounds
                coordinates?.updateCurrentBounds()
                placeable.place(0, 0)
            }
        } else {
            val (w, h) =
                state.placeHolderSize.calculateSize(
                    requireLookaheadLayoutCoordinates().size,
                    IntSize(placeable.width, placeable.height)
                )
            return layout(w, h) {
                // Start animation if needed
                if (sharedElement.targetBounds != null) {
                    boundsAnimation.animate(
                        sharedElement.currentBounds!!,
                        sharedElement.targetBounds!!
                    )
                }
                val animatedBounds = boundsAnimation.value
                val positionInScope =
                    coordinates?.let { rootCoords.localPositionOf(it, Offset.Zero) }
                val topLeft: Offset

                // animation finished at visible
                if (animatedBounds != null) {
                    // Update CurrentBounds as needed
                    if (boundsAnimation.target) {
                        sharedElement.currentBounds = animatedBounds
                    }
                    topLeft = animatedBounds.topLeft
                } else {
                    if (boundsAnimation.target) {
                        coordinates?.updateCurrentBounds()
                    }
                    topLeft = sharedElement.currentBounds!!.topLeft
                }
                val (x, y) = positionInScope?.let { topLeft - it } ?: Offset.Zero
                placeable.place(x.fastRoundToInt(), y.fastRoundToInt())
            }
        }
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        return sharedElement.foundMatch && state.sharedElement.scope.isTransitionActive
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Approach pass. Animation may not have started, or if the animation isn't
        // running, we'll measure with current bounds.
        val resolvedConstraints =
            if (!sharedElement.foundMatch) {
                constraints
            } else {
                (boundsAnimation.value ?: sharedElement.currentBounds)?.let {
                    val (width, height) = it.size.roundToIntSize()
                    require(width != Constraints.Infinity && height != Constraints.Infinity) {
                        "Error: Infinite width/height is invalid. " +
                            "animated bounds: ${boundsAnimation.value}," +
                            " current bounds: ${sharedElement.currentBounds}"
                    }
                    Constraints.fixed(width.coerceAtLeast(0), height.coerceAtLeast(0))
                } ?: constraints
            }
        val placeable = measurable.measure(resolvedConstraints)
        return place(placeable)
    }

    private fun LayoutCoordinates.updateCurrentBounds() {
        sharedElement.currentBounds =
            Rect(
                rootCoords.localPositionOf(this, Offset.Zero),
                Size(this.size.width.toFloat(), this.size.height.toFloat())
            )
    }

    override fun ContentDrawScope.draw() {
        // Update clipPath
        state.clipPathInOverlay =
            state.overlayClip.getClipPath(
                state.userState,
                sharedElement.currentBounds!!,
                layoutDirection,
                requireDensity()
            )
        val layer =
            requireNotNull(state.layer) {
                "Error: Layer is null when accessed for shared bounds/element : ${sharedElement.key}," +
                    "target: ${state.boundsAnimation.target}, is attached: $isAttached"
            }

        layer.record {
            this@draw.drawContent()
            if (VisualDebugging && sharedElement.foundMatch) {
                // TODO: also draw border of the clip path
                drawRect(Color.Green, style = Stroke(3f))
            }
        }
        if (state.shouldRenderInPlace) {
            drawLayer(layer)
        }
    }
}

internal val ModifierLocalSharedElementInternalState =
    modifierLocalOf<SharedElementInternalState?> { null }
