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

package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Placeable.PlacementScope.Companion.place
import androidx.compose.ui.layout.Placeable.PlacementScope.Companion.placeWithLayer
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.NodeMeasuringIntrinsics
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.visitAncestors
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

/**
 * This establishes an internal IntermediateLayoutModifierNode. This node implicitly creates
 * a [LookaheadScope], unless there is already a [LookaheadScope] in its ancestor. This allows
 * lookahead to function "locally" without an explicit [LookaheadScope] defined.
 *
 * [coroutineScope] is a CoroutineScope that we provide to the IntermediateMeasureBlock for
 * the intermediate changes to launch from. It is scoped to the lifecycle of the
 * Modifier.intermediateLayout.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class IntermediateLayoutModifierNode(
    internal var measureBlock: IntermediateMeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
    ) -> MeasureResult
) : LayoutModifierNode, Modifier.Node() {

    // This is the union scope of LookaheadScope, CoroutineScope and MeasureScope that will be
    // used as the receiver for user-provided measure block.
    private val intermediateMeasureScope = IntermediateMeasureScopeImpl()

    // If there's no lookahead scope in the ancestor, this is the lookahead scope that
    // we'll provide to the intermediateLayout modifier
    private val localLookaheadScope: LookaheadScopeImpl = LookaheadScopeImpl {
        coordinator!!
    }

    /**
     * Closest LookaheadScope in the ancestor. Defaults to local lookahead scope, and
     * gets modified if there was already a parent scope.
     */
    private var closestLookaheadScope: LookaheadScope = localLookaheadScope

    // TODO: This needs to be wired up with a user provided lambda to explicitly tell us when the
    // intermediate changes are finished. The functionality to support this has been implemented,
    // but the API change to get this lambda from devs has to be deferred until Modifier.Node
    // delegate design is finished.
    var isIntermediateChangeActive: Boolean = true

    // Caches the lookahead constraints in order to snap to lookahead constraints in main pass
    // when the intermediate changes are finished.
    private var lookaheadConstraints: Constraints? = null

    // Measurable & Placeable that serves as a middle layer between intermediateLayout logic and
    // child measurable/placeable. This allows the middle layer to overwrite any changes in
    // intermediateLayout when [IntermediateMeasureBlock#isIntermediateChangeActive] = false.
    // This ensures a convergence between main pass and lookahead pass when there's no
    // intermediate changes.
    private var intermediateMeasurable: IntermediateMeasurablePlaceable? = null

    override fun onAttach() {
        val layoutNode = coordinator!!.layoutNode

        val coordinates = coordinator!!.lookaheadDelegate?.lookaheadLayoutCoordinates
        require(coordinates != null)

        val closestLookaheadRoot = layoutNode.lookaheadRoot
        closestLookaheadScope = if (closestLookaheadRoot?.isVirtualLookaheadRoot == true) {
            // The closest explicit scope in the tree will be the closest scope, as all
            // descendant intermediateLayoutModifiers will be using that as their LookaheadScope
            LookaheadScopeImpl {
                closestLookaheadRoot.parent!!.innerCoordinator.coordinates
            }
        } else {
            // If no explicit scope is ever defined, then fallback to implicitly created scopes
            var ancestorNode: IntermediateLayoutModifierNode? = null
            visitAncestors(Nodes.IntermediateMeasure) {
                // Find the closest ancestor, and return
                ancestorNode = it
                return@visitAncestors
            }
            ancestorNode?.localLookaheadScope ?: localLookaheadScope
        }
    }

    /**
     * This gets call in the lookahead pass. Since intermediateLayout is designed to only make
     * transient changes that don't affect lookahead, we simply pass through for the lookahead
     * pass.
     */
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult = measurable.measure(constraints).run {
        layout(width, height) {
            place(0, 0)
        }
    }

    /**
     * This gets called in the main pass to allow intermediate measurements & placements gradually
     * converging to the lookahead results.
     */
    fun MeasureScope.intermediateMeasure(
        measurable: Measurable,
        constraints: Constraints,
        lookaheadSize: IntSize,
        lookaheadConstraints: Constraints,
    ): MeasureResult {
        intermediateMeasureScope.lookaheadSize = lookaheadSize
        this@IntermediateLayoutModifierNode.lookaheadConstraints = lookaheadConstraints

        return (intermediateMeasurable ?: IntermediateMeasurablePlaceable(measurable)).apply {
            intermediateMeasurable = this
            wrappedMeasurable = measurable
        }.let { wrappedMeasurable ->
            intermediateMeasureScope.measureBlock(wrappedMeasurable, constraints)
        }
    }

    /**
     * The function used to calculate minIntrinsicWidth for intermediate changes.
     */
    internal fun IntrinsicMeasureScope.minIntermediateIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = NodeMeasuringIntrinsics.minWidth(
        { intrinsicMeasurable, constraints ->
            intermediateMeasureScope.measureBlock(intrinsicMeasurable, constraints)
        },
        this,
        measurable,
        height
    )

    /**
     * The function used to calculate minIntrinsicHeight for intermediate changes.
     */
    internal fun IntrinsicMeasureScope.minIntermediateIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = NodeMeasuringIntrinsics.minHeight(
        { intrinsicMeasurable, constraints ->
            intermediateMeasureScope.measureBlock(intrinsicMeasurable, constraints)
        },
        this,
        measurable,
        width
    )

    /**
     * The function used to calculate maxIntrinsicWidth for intermediate changes.
     */
    internal fun IntrinsicMeasureScope.maxIntermediateIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = NodeMeasuringIntrinsics.maxWidth(
        { intrinsicMeasurable, constraints ->
            intermediateMeasureScope.measureBlock(intrinsicMeasurable, constraints)
        },
        this,
        measurable,
        height
    )

    /**
     * The function used to calculate maxIntrinsicHeight for intermediate changes.
     */
    internal fun IntrinsicMeasureScope.maxIntermediateIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = NodeMeasuringIntrinsics.maxHeight(
        { intrinsicMeasurable, constraints ->
            intermediateMeasureScope.measureBlock(intrinsicMeasurable, constraints)
        },
        this,
        measurable,
        width
    )

    /**
     * This class serves as a layer between measure and layout logic defined in the [measureBlock]
     * and the child measurable (i.e. the next LayoutModifierNodeCoordinator). This class allows
     * us to prevent any change in the [measureBlock] from impacting the child when there is _no_
     * active changes in the given CoroutineScope.
     */
    private inner class IntermediateMeasurablePlaceable(
        var wrappedMeasurable: Measurable
    ) : Measurable, Placeable() {
        var wrappedPlaceable: Placeable? = null
        override fun measure(constraints: Constraints): Placeable {
            wrappedPlaceable = if (isIntermediateChangeActive) {
                wrappedMeasurable.measure(constraints).also {
                    measurementConstraints = constraints
                    measuredSize = IntSize(it.width, it.height)
                }
            } else {
                // If the intermediate change isn't active, we'll measure with
                // lookahead constraints and return lookahead size.
                wrappedMeasurable.measure(lookaheadConstraints!!).also {
                    measurementConstraints = lookaheadConstraints!!
                    // isIntermediateChangeActive could change from false to true between
                    // measurement & returning measure results. Use case: animateContentSize
                    measuredSize = if (isIntermediateChangeActive) {
                        IntSize(it.width, it.height)
                    } else {
                        intermediateMeasureScope.lookaheadSize
                    }
                }
            }
            return this
        }

        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            val offset =
                if (isIntermediateChangeActive) position else IntOffset.Zero
            layerBlock?.let {
                wrappedPlaceable?.placeWithLayer(
                    offset,
                    zIndex,
                    it
                )
            } ?: wrappedPlaceable?.place(offset, zIndex)
        }

        override val parentData: Any?
            get() = wrappedMeasurable.parentData

        override fun get(alignmentLine: AlignmentLine): Int =
            wrappedPlaceable!!.get(alignmentLine)

        override fun minIntrinsicWidth(height: Int): Int =
            wrappedMeasurable.minIntrinsicWidth(height)

        override fun maxIntrinsicWidth(height: Int): Int =
            wrappedMeasurable.maxIntrinsicWidth(height)

        override fun minIntrinsicHeight(width: Int): Int =
            wrappedMeasurable.minIntrinsicHeight(width)

        override fun maxIntrinsicHeight(width: Int): Int =
            wrappedMeasurable.maxIntrinsicHeight(width)
    }

    @ExperimentalComposeUiApi
    private inner class IntermediateMeasureScopeImpl : IntermediateMeasureScope,
        CoroutineScope {
        override var lookaheadSize: IntSize = IntSize.Zero

        override fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates =
            with(closestLookaheadScope) { this@toLookaheadCoordinates.toLookaheadCoordinates() }

        override val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates
            get() = with(closestLookaheadScope) {
                this@lookaheadScopeCoordinates.lookaheadScopeCoordinates
            }

        @Suppress("DEPRECATION")
        @Deprecated(
            "onPlaced in LookaheadLayoutScope has been deprecated. It's replaced" +
                " with reading LookaheadLayoutCoordinates directly during placement in" +
                "IntermediateMeasureScope"
        )
        override fun Modifier.onPlaced(
            onPlaced: (
                lookaheadScopeCoordinates: LookaheadLayoutCoordinates,
                layoutCoordinates: LookaheadLayoutCoordinates
            ) -> Unit
        ): Modifier = with(closestLookaheadScope) {
            this@onPlaced.onPlaced(onPlaced)
        }

        override fun layout(
            width: Int,
            height: Int,
            alignmentLines: Map<AlignmentLine, Int>,
            placementBlock: Placeable.PlacementScope.() -> Unit
        ) = object : MeasureResult {
            override val width = width
            override val height = height
            override val alignmentLines = alignmentLines
            override fun placeChildren() {
                Placeable.PlacementScope.executeWithRtlMirroringValues(
                    width,
                    layoutDirection,
                    this@IntermediateLayoutModifierNode.coordinator,
                    placementBlock
                )
            }
        }

        override val layoutDirection: LayoutDirection
            get() = coordinator!!.layoutDirection
        override val density: Float
            get() = coordinator!!.density
        override val fontScale: Float
            get() = coordinator!!.fontScale
        override val coroutineContext: CoroutineContext
            get() = coroutineScope.coroutineContext
    }
}