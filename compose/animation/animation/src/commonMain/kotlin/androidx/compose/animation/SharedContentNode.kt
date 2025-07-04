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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toSize
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
internal class SharedBoundsNode(state: SharedElementInternalState) :
    ApproachLayoutModifierNode,
    Modifier.Node(),
    DrawModifierNode,
    ModifierLocalModifierNode,
    BoundsProvider {

    override val lastBoundsInSharedTransitionScope: Rect?
        get() {
            // If the node was detached, or detached and re-attached between the query and
            // last placement, the last position is no longer attainable. Early return.
            if (!isAttached || !isPlaced) return null
            // TODO: Use the local bounding box and convert the size back to local size to
            // animate constraints when we build support for matrix transform in lookahead
            // coordinates, hence shared elements.
            return Rect(
                rootCoords.localPositionOf(approachCoordinates),
                approachCoordinates.size.toSize(),
            )
        }

    private val approachCoordinates: LayoutCoordinates
        get() = requireLayoutCoordinates()

    private var isPlaced: Boolean = false

    private val rootCoords: LayoutCoordinates
        get() = sharedElement.scope.root

    var state: SharedElementInternalState = state
        internal set(value) {
            if (value != field) {
                // State changed!
                field = value
                if (isAttached) {
                    setup()
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

    private fun setup() {
        provide(ModifierLocalSharedElementInternalState, state)
        state.parentState = ModifierLocalSharedElementInternalState.current
        layer = requireGraphicsContext().createGraphicsLayer()
        isPlaced = false
        state.boundsProvider = this
    }

    override fun onAttach() {
        super.onAttach()
        setup()
    }

    override fun onDetach() {
        super.onDetach()
        layer = null
        state.parentState = null
        state.boundsProvider = null
        isPlaced = false
    }

    override fun onReset() {
        super.onReset()
        // Reset layer
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = requireGraphicsContext().createGraphicsLayer()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Lookahead pass: Record lookahead size and lookahead coordinates
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
            // Update the lookahead result after child placement, so that child has an
            // opportunity to use its placement to influence the bounds animation.
            sharedElement.onLookaheadPlaced(this, state)
        }
    }

    // Match outlives transition. i.e. user didn't remove the not-visible shared element from
    // the tree. In this case, the not visible shared element follows the visible shared
    // element layout.
    private fun Placeable.PlacementScope.approachPlaceMatchBeyondTransition(placeable: Placeable) {
        if (!boundsAnimation.target) {
            // Match is found, but is not visible: Derive measured size & position
            // from the target bounds.
            val bounds = sharedElement.currentBoundsWhenMatched
            if (bounds != null) {
                // If current bounds is null in this case, it means the target has never
                // been placed.
                val (x, y) =
                    coordinates?.let {
                        val positionInScope = rootCoords.localPositionOf(it, Offset.Zero)
                        (bounds.topLeft - positionInScope).round()
                    } ?: IntOffset.Zero

                placeable.place(x, y)
            } else {
                placeable.place(0, 0)
            }
        } else {
            if (boundsAnimation.target || !sharedElement.foundMatch) {
                placeable.place(0, 0)
            }
        }
    }

    /**
     * Places *matched* shared element during transition. In this placement, we will be placing
     * based on the bounds transform of shared elements. Animations are also initialized in this
     * placement.
     */
    private fun Placeable.PlacementScope.approachPlaceMatchInTransition(placeable: Placeable) {
        val coordinates = coordinates
        if (coordinates == null) {
            // Shallow placement. Skip this placement and defer to the real placement.
            placeable.place(0, 0)
            return
        }

        val positionInScope = rootCoords.localPositionOf(coordinates, Offset.Zero)
        // Start animation if needed
        if (sharedElement.targetData != null) {
            val bounds =
                sharedElement.currentBoundsWhenMatched
                    ?: positionInScope.let {
                        Rect(it, Size(placeable.width.toFloat(), placeable.height.toFloat()))
                    }
            // Once the animation starts, we will only change target bounds when the target
            // structural offset changes. When MFR (e.g. scrolling) changes, we will track the
            // current MFR, and apply the total offset incurred since the start of the animation
            // (i.e. currentMfr - initialMfr) directly to the animated value.
            boundsAnimation.animate(bounds, sharedElement.targetData!!.targetBounds)
        }

        val animatedBounds = boundsAnimation.value
        val topLeft: Offset
        val animatedTopLeft =
            animatedBounds?.let {
                sharedElement.targetData!!.calculateOffsetFromDirectManipulation(it)
            }

        if (boundsAnimation.target) {
            // The visible shared element defines the current bounds, either through animation
            // or when the animation is finished through its own position.

            topLeft = animatedTopLeft ?: positionInScope
            val bounds =
                if (animatedTopLeft == null) {
                    Rect(positionInScope, coordinates.size.toSize())
                } else {
                    Rect(animatedTopLeft, animatedBounds.size)
                }
            sharedElement.currentBoundsWhenMatched = bounds
        } else {
            topLeft = animatedTopLeft ?: sharedElement.currentBoundsWhenMatched!!.topLeft
        }

        val (x, y) = positionInScope.let { topLeft - it }
        placeable.place(x.fastRoundToInt(), y.fastRoundToInt())
    }

    private fun MeasureScope.approachPlace(placeable: Placeable): MeasureResult {
        isPlaced = true
        if (!sharedElement.foundMatch) {
            // No modification to placement if no match is found.
            sharedElement.currentBoundsWhenMatched = null
            return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }

        // Match outlives transition. i.e. user didn't remove the not-visible shared element from
        // the tree. In this case, the not visible shared element follows the visible shared
        // element layout.
        if (!sharedElement.scope.isTransitionActive) {
            return layout(placeable.width, placeable.height) {
                approachPlaceMatchBeyondTransition(placeable)
            }
        } else { // found match && actively animating
            val (w, h) =
                state.placeHolderSize.calculateSize(
                    requireLookaheadLayoutCoordinates().size,
                    IntSize(placeable.width, placeable.height),
                )
            return layout(w, h) { approachPlaceMatchInTransition(placeable) }
        }
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        return sharedElement.foundMatch && state.sharedElement.scope.isTransitionActive
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Approach pass. Animation may not have started, or if the animation isn't
        // running, we'll measure with current bounds.
        val resolvedConstraints =
            if (!sharedElement.foundMatch) {
                constraints
            } else {
                // When a match is found, all matches will be measured using the constraints
                // created by the target bounds, **even when there is no active transition**.
                (boundsAnimation.value ?: sharedElement.tryInitializingCurrentBounds())?.let {
                    val (width, height) = it.size.roundToIntSize()
                    require(width != Constraints.Infinity && height != Constraints.Infinity) {
                        "Error: Infinite width/height is invalid. " +
                            "animated bounds: ${boundsAnimation.value}," +
                            " current bounds: ${sharedElement.currentBoundsWhenMatched}"
                    }
                    Constraints.fixed(width.coerceAtLeast(0), height.coerceAtLeast(0))
                } ?: constraints
            }
        val placeable = measurable.measure(resolvedConstraints)
        return approachPlace(placeable)
    }

    override fun ContentDrawScope.draw() {
        // Update clipPath
        state.clipPathInOverlay =
            if (sharedElement.foundMatch && sharedElement.currentBoundsWhenMatched != null) {
                state.overlayClip.getClipPath(
                    state.userState,
                    sharedElement.currentBoundsWhenMatched!!,
                    layoutDirection,
                    requireDensity(),
                )
            } else {
                null
            }
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
