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

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
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

internal data class SharedBoundsNodeElement(val sharedElementState: SharedElementEntry) :
    ModifierNodeElement<SharedBoundsNode>() {
    override fun create(): SharedBoundsNode = SharedBoundsNode(sharedElementState)

    override fun update(node: SharedBoundsNode) {
        node.sharedElementEntry = sharedElementState
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
internal class SharedBoundsNode(state: SharedElementEntry) :
    ApproachLayoutModifierNode,
    Modifier.Node(),
    DrawModifierNode,
    ModifierLocalModifierNode,
    ObserverModifierNode,
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

    override fun calculateAlternativeTargetBounds(targetBoundsBeforeDisposed: Rect): Rect? {
        return sharedElementEntry.calculateTargetBounds(targetBoundsBeforeDisposed)
    }

    private val approachCoordinates: LayoutCoordinates
        get() = requireLayoutCoordinates()

    private var isPlaced: Boolean = false

    private val rootCoords: LayoutCoordinates
        get() = sharedElement.scope.root

    var sharedElementEntry: SharedElementEntry = state
        internal set(value) {
            if (value != field) {
                // State changed!
                field.isAttached = false
                field = value
                value.isAttached = isAttached
                if (isAttached) {
                    setup()
                }
            }
        }

    private fun requireLookaheadLayoutCoordinates(): LayoutCoordinates =
        with(sharedElementEntry.sharedElement.scope) {
            requireLayoutCoordinates().toLookaheadCoordinates()
        }

    private val boundsAnimation: BoundsAnimation
        get() = sharedElementEntry.boundsAnimation

    private var layer: GraphicsLayer? = state.layer
        set(value) {
            if (value == null) {
                field?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
            } else {
                sharedElementEntry.layer = value
            }
            field = value
        }

    private val sharedElement: SharedElement
        get() = sharedElementEntry.sharedElement

    override val providedValues =
        modifierLocalMapOf(ModifierLocalSharedElementInternalState to state)

    private fun setup() {
        provide(ModifierLocalSharedElementInternalState, sharedElementEntry)
        sharedElementEntry.parentState = ModifierLocalSharedElementInternalState.current
        layer = requireGraphicsContext().createGraphicsLayer()
        isPlaced = false
        sharedElementEntry.boundsProvider = this
    }

    override fun onAttach() {
        super.onAttach()
        observeReads(sharedElement.observingVisibilityChange)
        setup()
        sharedElementEntry.isAttached = true
    }

    override fun onDetach() {
        super.onDetach()
        layer = null
        sharedElementEntry.parentState = null
        sharedElementEntry.boundsProvider = null
        sharedElementEntry.isAttached = false
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
            sharedElement.onLookaheadPlaced(this, sharedElementEntry)
        }
    }

    // Match outlives transition. i.e. user didn't remove the not-visible shared element from
    // the tree. In this case, the not visible shared element follows the visible shared
    // element layout.
    private fun Placeable.PlacementScope.approachPlaceMatchBeyondTransition(
        placeable: Placeable,
        currentBounds: Rect,
    ) {
        if (!boundsAnimation.target) {
            // Match is found, but is not visible: Derive measured size & position
            // from the target bounds.
            val bounds = currentBounds
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
    }

    /**
     * Places *matched* shared element during transition. In this placement, we will be placing
     * based on the bounds transform of shared elements. Animations are also initialized in this
     * placement.
     */
    internal fun Placeable.PlacementScope.approachPlaceMatchInTransition(
        placeable: Placeable,
        targetData: TargetData,
        currentBounds: Rect,
    ) {
        val coordinates = coordinates
        if (coordinates == null) {
            // Shallow placement. Skip this placement and defer to the real placement.
            placeable.place(0, 0)
            return
        }

        val activeMatchRemoved = !sharedElement.state.activeMatchFound
        val positionInScope = rootCoords.localPositionOf(coordinates, Offset.Zero)
        // Start animation if needed
        // Once the animation starts, we will only change target bounds when the target
        // structural offset changes. When MFR (e.g. scrolling) changes, we will track the
        // current MFR, and apply the total offset incurred since the start of the animation
        // (i.e. currentMfr - initialMfr) directly to the animated value.
        if (activeMatchRemoved) {
            boundsAnimation.animate(
                currentBounds,
                targetData.targetBounds,
                BoundsTransform { _, _ -> spring(visibilityThreshold = Rect.VisibilityThreshold) },
            )
        } else {
            boundsAnimation.animate(currentBounds, targetData.targetBounds)
        }

        val animatedBounds = boundsAnimation.value
        val topLeft: Offset
        val animatedTopLeft =
            animatedBounds?.let { targetData.calculateOffsetFromDirectManipulation(it) }

        if (boundsAnimation.target || activeMatchRemoved) {
            // The visible shared element defines the current bounds, either through animation
            // or when the animation is finished through its own position.

            topLeft = animatedTopLeft ?: positionInScope
            val bounds =
                if (animatedTopLeft == null) {
                    Rect(positionInScope, coordinates.size.toSize())
                } else {
                    Rect(animatedTopLeft, animatedBounds.size)
                }

            sharedElement.state.updateBounds(bounds)
            if (SharedTransitionDebug) {
                println(
                    "SharedTransition, animated bounds: $bounds," +
                        " target: ${targetData.targetBounds}," +
                        " scope size: ${sharedElement.scope.lookaheadRoot.size}," +
                        " ${sharedElement.state}"
                )
            }
        } else {
            topLeft = animatedTopLeft ?: currentBounds.topLeft
        }

        val (x, y) = positionInScope.let { topLeft - it }
        placeable.place(x.fastRoundToInt(), y.fastRoundToInt())
    }

    private fun MeasureScope.approachPlace(placeable: Placeable): MeasureResult {
        val (w, h) =
            if (sharedElement.state.matchIsOrHasBeenConfigured) {
                // found match && actively animating
                sharedElementEntry.placeHolderSize.calculateSize(
                    requireLookaheadLayoutCoordinates().size,
                    IntSize(placeable.width, placeable.height),
                )
            } else {
                IntSize(placeable.width, placeable.height)
            }
        return layout(w, h) {
            isPlaced = true

            val matchState = sharedElement.state
            if (!sharedElementEntry.isEnabled) {
                // Early return if the state isn't enabled.
                placeable.place(0, 0)
            } else if (matchState.matchIsOrHasBeenConfigured) {
                val targetData =
                    requireNotNull(matchState.targetData) {
                        "Match State is configured, but target data is null. State = $matchState"
                    }
                val currentBounds =
                    requireNotNull(matchState.currentBounds) {
                        "Match State is configured, but current bounds is null. State = $matchState"
                    }
                if (sharedElement.scope.isTransitionActive) {
                    approachPlaceMatchInTransition(placeable, targetData, currentBounds)
                } else {
                    // Match outlives transition. i.e. user didn't remove the not-visible shared
                    // element from
                    // the tree. In this case, the not visible shared element follows the visible
                    // shared
                    // element layout.
                    approachPlaceMatchBeyondTransition(placeable, currentBounds)
                }
            } else {
                // Not matched yet, or active match not configured yet.
                placeable.place(0, 0)
            }
        }
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        return sharedElementEntry.isEnabled &&
            sharedElement.foundMatch &&
            sharedElement.scope.isTransitionActive
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Approach pass. Animation may not have started, or if the animation isn't
        // running, we'll measure with current bounds.
        val resolvedConstraints =
            // When a match is found, all matches will be measured using the constraints
            // created by the target bounds, **even when there is no active transition**.
            (boundsAnimation.value ?: sharedElement.tryInitializingCurrentBounds())?.let {
                val (width, height) = it.size.roundToIntSize()
                require(width != Constraints.Infinity && height != Constraints.Infinity) {
                    "Error: Infinite width/height is invalid. " +
                        "animated bounds: ${boundsAnimation.value}," +
                        " current bounds: ${sharedElement.state.currentBounds}"
                }
                Constraints.fixed(width.coerceAtLeast(0), height.coerceAtLeast(0))
            } ?: constraints
        if (SharedTransitionDebug) {
            println(
                "SharedTransition, approach measure constraints: $resolvedConstraints," +
                    " key = ${sharedElement.key}, state: ${sharedElement.state}"
            )
        }
        val placeable = measurable.measure(resolvedConstraints)
        return approachPlace(placeable)
    }

    override fun ContentDrawScope.draw() {
        val matchState = sharedElement.state
        val bounds = matchState.currentBounds
        if (SharedTransitionDebug) {
            println(
                "SharedTransition, ContentDrawScope.draw() invoked. Bounds size: ${bounds?.size}" +
                    " for key = ${sharedElement.key}"
            )
        }
        // Update clipPath
        sharedElementEntry.clipPathInOverlay =
            if (sharedElementEntry.shouldRenderInOverlay && bounds != null) {
                sharedElementEntry.overlayClip.getClipPath(
                    sharedElementEntry.userState,
                    bounds,
                    layoutDirection,
                    requireDensity(),
                )
            } else {
                null
            }
        val layer =
            requireNotNull(sharedElementEntry.layer) {
                "Error: Layer is null when accessed for shared bounds/element : ${sharedElement.key}," +
                    "target: ${sharedElementEntry.boundsAnimation.target}, is attached: $isAttached"
            }

        layer.record {
            if (SharedTransitionDebug) {
                println(
                    "SharedTransition, record layer at size: ${bounds?.size} for" +
                        " key = ${sharedElement.key}"
                )
            }

            this@draw.drawContent()
            if (
                VisualDebugging &&
                    sharedElement.boundsTransformIsActive &&
                    sharedElementEntry.isEnabled
            ) {
                // TODO: also draw border of the clip path
                drawRect(Color.Green, style = Stroke(3f))
            }
        }
        if (sharedElementEntry.shouldRenderInPlace) {
            if (SharedTransitionDebug) {
                println("SharedTransition, drawing in place. key = ${sharedElement.key}")
            }
            drawLayer(layer)
        }
    }

    override fun onObservedReadsChanged() {
        sharedElement.updateMatch()
        observeReads(sharedElement.observingVisibilityChange)
    }
}

internal val ModifierLocalSharedElementInternalState = modifierLocalOf<SharedElementEntry?> { null }
