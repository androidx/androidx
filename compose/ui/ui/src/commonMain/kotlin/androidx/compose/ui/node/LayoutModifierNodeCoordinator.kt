/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScopeImpl
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset

internal class LayoutModifierNodeCoordinator(
    layoutNode: LayoutNode,
    measureNode: LayoutModifierNode,
) : NodeCoordinator(layoutNode) {

    var layoutModifierNode: LayoutModifierNode = measureNode
        internal set(value) {
            if (value != field) {
                // Opt for a cheaper type check (via bit operation) before casting, as we anticipate
                // the node to not be ApproachLayoutModifierNode in most cases.
                if (value.node.isKind(Nodes.ApproachMeasure)) {
                    value as ApproachLayoutModifierNode
                    approachMeasureScope =
                        approachMeasureScope?.also { it.approachNode = value }
                            ?: ApproachMeasureScopeImpl(this, value)
                } else {
                    approachMeasureScope = null
                }
            }
            field = value
        }

    override val tail: Modifier.Node
        get() = layoutModifierNode.node

    val wrappedNonNull: NodeCoordinator
        get() = wrapped!!

    internal var lookaheadConstraints: Constraints? = null

    override var lookaheadDelegate: LookaheadDelegate? =
        if (layoutNode.lookaheadRoot != null) LookaheadDelegateForLayoutModifierNode() else null

    /**
     * Lazily initialized IntermediateMeasureScope. This is only initialized when the current
     * modifier is an ApproachLayoutModifierNode.
     */
    private var approachMeasureScope: ApproachMeasureScopeImpl? =
        // Opt for a cheaper type check (via bit operation) before casting, as we anticipate
        // the node to not be ApproachLayoutModifierNode in most cases.
        if (measureNode.node.isKind(Nodes.ApproachMeasure)) {
            ApproachMeasureScopeImpl(this, measureNode as ApproachLayoutModifierNode)
        } else null

    /**
     * LookaheadDelegate impl for when the modifier is any [LayoutModifier] except
     * IntermediateLayoutModifier. This impl will invoke [LayoutModifier.measure] for the lookahead
     * measurement.
     */
    private inner class LookaheadDelegateForLayoutModifierNode :
        LookaheadDelegate(this@LayoutModifierNodeCoordinator) {
        // LookaheadMeasure
        override fun measure(constraints: Constraints): Placeable =
            performingMeasure(constraints) {
                this@LayoutModifierNodeCoordinator.lookaheadConstraints = constraints
                with(this@LayoutModifierNodeCoordinator.layoutModifierNode) {
                    measure(
                        // This allows `measure` calls in the modifier to be redirected to
                        // calling lookaheadMeasure in wrapped.
                        this@LayoutModifierNodeCoordinator.wrappedNonNull.lookaheadDelegate!!,
                        constraints,
                    )
                }
            }

        override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
            return calculateAlignmentAndPlaceChildAsNeeded(alignmentLine).also {
                cachedAlignmentLinesMap[alignmentLine] = it
            }
        }

        override fun minIntrinsicWidth(height: Int): Int =
            with(this@LayoutModifierNodeCoordinator.layoutModifierNode) {
                minIntrinsicWidth(
                    this@LayoutModifierNodeCoordinator.wrappedNonNull.lookaheadDelegate!!,
                    height,
                )
            }

        override fun maxIntrinsicWidth(height: Int): Int =
            with(this@LayoutModifierNodeCoordinator.layoutModifierNode) {
                maxIntrinsicWidth(
                    this@LayoutModifierNodeCoordinator.wrappedNonNull.lookaheadDelegate!!,
                    height,
                )
            }

        override fun minIntrinsicHeight(width: Int): Int =
            with(this@LayoutModifierNodeCoordinator.layoutModifierNode) {
                minIntrinsicHeight(
                    this@LayoutModifierNodeCoordinator.wrappedNonNull.lookaheadDelegate!!,
                    width,
                )
            }

        override fun maxIntrinsicHeight(width: Int): Int =
            with(this@LayoutModifierNodeCoordinator.layoutModifierNode) {
                maxIntrinsicHeight(
                    this@LayoutModifierNodeCoordinator.wrappedNonNull.lookaheadDelegate!!,
                    width,
                )
            }
    }

    override fun ensureLookaheadDelegateCreated() {
        if (lookaheadDelegate == null) {
            lookaheadDelegate = LookaheadDelegateForLayoutModifierNode()
        }
    }

    override fun measure(constraints: Constraints): Placeable {
        @Suppress("NAME_SHADOWING")
        val constraints =
            if (forceMeasureWithLookaheadConstraints) {
                requireNotNull(lookaheadConstraints) {
                    "Lookahead constraints cannot be null in approach pass."
                }
            } else {
                constraints
            }
        performingMeasure(constraints) {
            measureResult =
                approachMeasureScope?.let { scope ->
                    // approachMeasureScope is created/updated when layoutModifierNode is set. An
                    // ApproachLayoutModifierNode will lead to a non-null approachMeasureScope.
                    with(scope.approachNode) {
                        scope.approachMeasureRequired =
                            isMeasurementApproachInProgress(scope.lookaheadSize) ||
                                constraints != lookaheadConstraints
                        if (!scope.approachMeasureRequired) {
                            // In the future we'll skip the invocation of this measure block when
                            // no approach is needed. For now, we'll ignore the constraints change
                            // in the measure block when it's declared approach complete.
                            wrappedNonNull.forceMeasureWithLookaheadConstraints = true
                        }
                        val result = scope.approachMeasure(wrappedNonNull, constraints)
                        wrappedNonNull.forceMeasureWithLookaheadConstraints = false
                        val reachedLookaheadSize =
                            result.width == lookaheadDelegate!!.width &&
                                result.height == lookaheadDelegate!!.height
                        if (
                            !scope.approachMeasureRequired &&
                                wrappedNonNull.size == wrappedNonNull.lookaheadDelegate?.size &&
                                !reachedLookaheadSize
                        ) {
                            object : MeasureResult by result {
                                override val width = lookaheadDelegate!!.width
                                override val height = lookaheadDelegate!!.height
                            }
                        } else {
                            result
                        }
                    }
                } ?: with(layoutModifierNode) { measure(wrappedNonNull, constraints) }
            this@LayoutModifierNodeCoordinator
        }
        onMeasured()
        return this
    }

    override fun minIntrinsicWidth(height: Int): Int =
        approachMeasureScope?.run {
            with(approachNode) {
                minApproachIntrinsicWidth(this@LayoutModifierNodeCoordinator.wrappedNonNull, height)
            }
        } ?: with(layoutModifierNode) { minIntrinsicWidth(wrappedNonNull, height) }

    override fun maxIntrinsicWidth(height: Int): Int =
        approachMeasureScope?.run {
            with(approachNode) {
                maxApproachIntrinsicWidth(this@LayoutModifierNodeCoordinator.wrappedNonNull, height)
            }
        } ?: with(layoutModifierNode) { maxIntrinsicWidth(wrappedNonNull, height) }

    override fun minIntrinsicHeight(width: Int): Int =
        approachMeasureScope?.run {
            with(approachNode) {
                minApproachIntrinsicHeight(this@LayoutModifierNodeCoordinator.wrappedNonNull, width)
            }
        } ?: with(layoutModifierNode) { minIntrinsicHeight(wrappedNonNull, width) }

    override fun maxIntrinsicHeight(width: Int): Int =
        approachMeasureScope?.run {
            with(approachNode) {
                maxApproachIntrinsicHeight(this@LayoutModifierNodeCoordinator.wrappedNonNull, width)
            }
        } ?: with(layoutModifierNode) { maxIntrinsicHeight(wrappedNonNull, width) }

    override fun placeAt(position: IntOffset, zIndex: Float, layer: GraphicsLayer) {
        super.placeAt(position, zIndex, layer)
        onAfterPlaceAt()
    }

    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        super.placeAt(position, zIndex, layerBlock)
        onAfterPlaceAt()
    }

    private fun onAfterPlaceAt() {
        // The coordinator only runs their placement block to obtain our position, which allows them
        // to calculate the offset of an alignment line we have already provided a position for.
        // No need to place our wrapped as well (we might have actually done this already in
        // get(line), to obtain the position of the alignment line the coordinator currently needs
        // our position in order ot know how to offset the value we provided).
        if (isShallowPlacing) return
        onPlaced()
        approachMeasureScope?.let {
            with(it.approachNode) {
                val approachComplete =
                    with(placementScope) {
                        !isPlacementApproachInProgress(
                            lookaheadDelegate!!.lookaheadLayoutCoordinates
                        ) &&
                            !it.approachMeasureRequired &&
                            size == lookaheadDelegate?.size &&
                            wrappedNonNull.size == wrappedNonNull.lookaheadDelegate?.size
                    }
                wrappedNonNull.forcePlaceWithLookaheadOffset = approachComplete
            }
        }
        measureResult.placeChildren()
        wrappedNonNull.forcePlaceWithLookaheadOffset = false
    }

    override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
        return lookaheadDelegate?.getCachedAlignmentLine(alignmentLine)
            ?: calculateAlignmentAndPlaceChildAsNeeded(alignmentLine)
    }

    override fun performDraw(canvas: Canvas, graphicsLayer: GraphicsLayer?) {
        wrappedNonNull.draw(canvas, graphicsLayer)
        if (layoutNode.requireOwner().showLayoutBounds) {
            val wrapped = wrapped
            if (wrapped != null && (size != wrapped.size || wrapped.position != IntOffset.Zero)) {
                drawBorder(canvas, modifierBoundsPaint)
            }
        }
    }

    internal companion object {
        val modifierBoundsPaint =
            Paint().also { paint ->
                paint.color = Color.Blue
                paint.strokeWidth = 1f
                paint.style = PaintingStyle.Stroke
            }
    }
}

private fun LookaheadCapablePlaceable.calculateAlignmentAndPlaceChildAsNeeded(
    alignmentLine: AlignmentLine
): Int {
    val child = child
    checkPrecondition(child != null) {
        "Child of $this cannot be null when calculating alignment line"
    }
    if (measureResult.alignmentLines.containsKey(alignmentLine)) {
        return measureResult.alignmentLines[alignmentLine] ?: AlignmentLine.Unspecified
    }
    val positionInWrapped = child[alignmentLine]
    if (positionInWrapped == AlignmentLine.Unspecified) {
        return AlignmentLine.Unspecified
    }
    // Place our wrapped to obtain their position inside ourselves.
    child.isShallowPlacing = true
    isPlacingForAlignment = true
    replace()
    child.isShallowPlacing = false
    isPlacingForAlignment = false
    return if (alignmentLine is HorizontalAlignmentLine) {
        positionInWrapped + child.position.y
    } else {
        positionInWrapped + child.position.x
    }
}
