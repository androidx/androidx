/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.LayoutNode.Companion.NotPlacedPlaceOrder
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach

/**
 * This class works as a layout delegate for [LayoutNode]. It delegates all the measure/layout
 * requests to its [measurePassDelegate] and [lookaheadPassDelegate] depending on whether the
 * request is specific to lookahead.
 */
internal class LayoutNodeLayoutDelegate(
    private val layoutNode: LayoutNode,
) {
    val outerCoordinator: NodeCoordinator
        get() = layoutNode.nodes.outerCoordinator

    val lastConstraints: Constraints?
        get() = measurePassDelegate.lastConstraints

    val lastLookaheadConstraints: Constraints?
        get() = lookaheadPassDelegate?.lastConstraints

    internal val height: Int
        get() = measurePassDelegate.height

    internal val width: Int
        get() = measurePassDelegate.width

    /**
     * This gets set to true via [MeasurePassDelegate.markDetachedFromParentLookaheadPass] and
     * automatically gets unset in `measure` when the measure call comes from parent with
     * layoutState being LookaheadMeasuring or LookaheadLayingOut.
     */
    internal var detachedFromParentLookaheadPass: Boolean = false
        private set

    /**
     * The layout state the node is currently in.
     *
     * The mutation of [layoutState] is confined to [LayoutNodeLayoutDelegate], and is therefore
     * read-only outside this class. This makes the state machine easier to reason about.
     */
    internal var layoutState = LayoutState.Idle
        private set

    /**
     * Tracks whether another measure pass is needed for the LayoutNodeLayoutDelegate. Mutation to
     * [measurePending] is confined to LayoutNodeLayoutDelegate. It can only be set true from
     * outside of this class via [markMeasurePending]. It is cleared (i.e. set false) during the
     * measure pass (i.e. in [performMeasure]).
     */
    internal var measurePending: Boolean = false
        private set

    /**
     * Tracks whether another layout pass is needed for the LayoutNodeLayoutDelegate. Mutation to
     * [layoutPending] is confined to this class. It can only be set true from outside of this class
     * via [markLayoutPending]. It is cleared (i.e. set false) during the layout pass (i.e. in
     * [MeasurePassDelegate.layoutChildren]).
     */
    internal var layoutPending: Boolean = false
        private set

    /**
     * Tracks whether another layout pass is needed for the LayoutNodeLayoutDelegate for the
     * purposes of calculating alignment lines. After calculating alignment lines, if the
     * [Placeable.PlacementScope.coordinates] have been accessed, there is no need to rerun layout
     * for further alignment lines checks, but [layoutPending] will indicate that the normal
     * placement still needs to be run.
     */
    private var layoutPendingForAlignment = false

    /**
     * Tracks whether another lookahead measure pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [lookaheadMeasurePending] is confined to LayoutNodeLayoutDelegate. It can only be
     * set true from outside of this class via [markLookaheadMeasurePending]. It is cleared (i.e.
     * set false) during the lookahead measure pass (i.e. in [performLookaheadMeasure]).
     */
    internal var lookaheadMeasurePending: Boolean = false
        private set

    /**
     * Tracks whether another lookahead layout pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [lookaheadLayoutPending] is confined to this class. It can only be set true from
     * outside of this class via [markLookaheadLayoutPending]. It is cleared (i.e. set false) during
     * the layout pass (i.e. in [LookaheadPassDelegate.layoutChildren]).
     */
    internal var lookaheadLayoutPending: Boolean = false
        private set

    /**
     * Tracks whether another lookahead layout pass is needed for the LayoutNodeLayoutDelegate for
     * the purposes of calculating alignment lines. After calculating alignment lines, if the
     * [Placeable.PlacementScope.coordinates] have been accessed, there is no need to rerun layout
     * for further alignment lines checks, but [lookaheadLayoutPending] will indicate that the
     * normal placement still needs to be run.
     */
    private var lookaheadLayoutPendingForAlignment = false

    /**
     * The counter on a parent node which is used by its children to understand the order in which
     * they were placed in the lookahead pass.
     */
    private var nextChildLookaheadPlaceOrder: Int = 0

    /**
     * The counter on a parent node which is used by its children to understand the order in which
     * they were placed in the main pass.
     */
    private var nextChildPlaceOrder: Int = 0

    /** Marks the layoutNode dirty for another layout pass. */
    internal fun markLayoutPending() {
        layoutPending = true
        layoutPendingForAlignment = true
    }

    /** Marks the layoutNode dirty for another measure pass. */
    internal fun markMeasurePending() {
        measurePending = true
    }

    /** Marks the layoutNode dirty for another lookahead layout pass. */
    internal fun markLookaheadLayoutPending() {
        lookaheadLayoutPending = true
        lookaheadLayoutPendingForAlignment = true
    }

    /** Marks the layoutNode dirty for another lookahead measure pass. */
    internal fun markLookaheadMeasurePending() {
        lookaheadMeasurePending = true
    }

    internal val alignmentLinesOwner: AlignmentLinesOwner
        get() = measurePassDelegate

    internal val lookaheadAlignmentLinesOwner: AlignmentLinesOwner?
        get() = lookaheadPassDelegate

    /**
     * This is used to track when the [Placeable.PlacementScope.coordinates] have been accessed
     * while placement is run. When the coordinates are accessed during an alignment line query, it
     * indicates that the placement is not final and must be run again so that the correct
     * positioning is done. If the coordinates are not accessed during an alignment lines query (and
     * it isn't just a [LookaheadCapablePlaceable.isShallowPlacing]), then the placement can be
     * considered final and doesn't have to be run again.
     *
     * Also, if coordinates are accessed during placement, then a change in parent coordinates
     * requires placement to be run again.
     */
    var coordinatesAccessedDuringPlacement = false
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = value
                if (value && !coordinatesAccessedDuringModifierPlacement) {
                    // if first out of both flags changes to true increment
                    childrenAccessingCoordinatesDuringPlacement++
                } else if (!value && !coordinatesAccessedDuringModifierPlacement) {
                    // if both flags changes to false decrement
                    childrenAccessingCoordinatesDuringPlacement--
                }
            }
        }

    /**
     * Similar to [coordinatesAccessedDuringPlacement], but tracks the coordinates read happening
     * during the modifier layout blocks run.
     */
    var coordinatesAccessedDuringModifierPlacement = false
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = value
                if (value && !coordinatesAccessedDuringPlacement) {
                    // if first out of both flags changes to true increment
                    childrenAccessingCoordinatesDuringPlacement++
                } else if (!value && !coordinatesAccessedDuringPlacement) {
                    // if both flags changes to false decrement
                    childrenAccessingCoordinatesDuringPlacement--
                }
            }
        }

    /**
     * The number of children with [coordinatesAccessedDuringPlacement] or have descendants with
     * [coordinatesAccessedDuringPlacement]. This also includes this, if
     * [coordinatesAccessedDuringPlacement] is `true`.
     */
    var childrenAccessingCoordinatesDuringPlacement = 0
        set(value) {
            val oldValue = field
            field = value
            if ((oldValue == 0) != (value == 0)) {
                // A child is either newly listening for coordinates or stopped listening
                val parentLayoutDelegate = layoutNode.parent?.layoutDelegate
                if (parentLayoutDelegate != null) {
                    if (value == 0) {
                        parentLayoutDelegate.childrenAccessingCoordinatesDuringPlacement--
                    } else {
                        parentLayoutDelegate.childrenAccessingCoordinatesDuringPlacement++
                    }
                }
            }
        }

    /** Equivalent flag of [coordinatesAccessedDuringPlacement] but for [lookaheadPassDelegate]. */
    var lookaheadCoordinatesAccessedDuringPlacement = false
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = value
                if (value && !lookaheadCoordinatesAccessedDuringModifierPlacement) {
                    // if first out of both flags changes to true increment
                    childrenAccessingLookaheadCoordinatesDuringPlacement++
                } else if (!value && !lookaheadCoordinatesAccessedDuringModifierPlacement) {
                    // if both flags changes to false decrement
                    childrenAccessingLookaheadCoordinatesDuringPlacement--
                }
            }
        }

    /**
     * Equivalent flag of [coordinatesAccessedDuringModifierPlacement] but for
     * [lookaheadPassDelegate].
     */
    var lookaheadCoordinatesAccessedDuringModifierPlacement = false
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = value
                if (value && !lookaheadCoordinatesAccessedDuringPlacement) {
                    // if first out of both flags changes to true increment
                    childrenAccessingLookaheadCoordinatesDuringPlacement++
                } else if (!value && !lookaheadCoordinatesAccessedDuringPlacement) {
                    // if both flags changes to false decrement
                    childrenAccessingLookaheadCoordinatesDuringPlacement--
                }
            }
        }

    /**
     * Equivalent flag of [childrenAccessingCoordinatesDuringPlacement] but for
     * [lookaheadPassDelegate].
     *
     * Naturally, this flag should only be affected by the lookahead coordinates access flags.
     */
    var childrenAccessingLookaheadCoordinatesDuringPlacement = 0
        set(value) {
            val oldValue = field
            field = value
            if ((oldValue == 0) != (value == 0)) {
                // A child is either newly listening for coordinates or stopped listening
                val parentLayoutDelegate = layoutNode.parent?.layoutDelegate
                if (parentLayoutDelegate != null) {
                    if (value == 0) {
                        parentLayoutDelegate.childrenAccessingLookaheadCoordinatesDuringPlacement--
                    } else {
                        parentLayoutDelegate.childrenAccessingLookaheadCoordinatesDuringPlacement++
                    }
                }
            }
        }

    /**
     * measurePassDelegate manages the measure/layout and alignmentLine related queries for the
     * actual measure/layout pass.
     */
    internal val measurePassDelegate = MeasurePassDelegate()

    /**
     * lookaheadPassDelegate manages the measure/layout and alignmentLine related queries for the
     * lookahead pass.
     */
    internal var lookaheadPassDelegate: LookaheadPassDelegate? = null
        private set

    // Used by performMeasureBlock so that we don't have to allocate a lambda on every call
    private var performMeasureConstraints = Constraints()

    private val performMeasureBlock: () -> Unit = {
        outerCoordinator.measure(performMeasureConstraints)
    }

    fun onCoordinatesUsed() {
        val state = layoutNode.layoutState
        if (state == LayoutState.LayingOut || state == LayoutState.LookaheadLayingOut) {
            if (measurePassDelegate.layingOutChildren) {
                coordinatesAccessedDuringPlacement = true
            } else {
                coordinatesAccessedDuringModifierPlacement = true
            }
        }
        if (state == LayoutState.LookaheadLayingOut) {
            if (lookaheadPassDelegate?.layingOutChildren == true) {
                lookaheadCoordinatesAccessedDuringPlacement = true
            } else {
                lookaheadCoordinatesAccessedDuringModifierPlacement = true
            }
        }
    }

    /**
     * [MeasurePassDelegate] manages the measure/layout and alignmentLine related queries for the
     * actual measure/layout pass.
     */
    inner class MeasurePassDelegate :
        Measurable, Placeable(), AlignmentLinesOwner, MotionReferencePlacementDelegate {
        /**
         * Is true during [replace] invocation. Helps to differentiate between the cases when our
         * parent is measuring us during the measure block, and when we are remeasured individually
         * because of some change. This could be useful to know if we need to record the placing
         * order.
         */
        private var relayoutWithoutParentInProgress: Boolean = false

        /**
         * The value [placeOrder] had during the previous parent `layoutChildren`. Helps us to
         * understand if the order did change.
         */
        internal var previousPlaceOrder: Int = NotPlacedPlaceOrder
            private set

        /**
         * The order in which this node was placed by its parent during the previous
         * `layoutChildren`. Before the placement the order is set to [NotPlacedPlaceOrder] to all
         * the children. Then every placed node assigns this variable to parent's
         * LayoutNodeLayoutDelegate's nextChildPlaceOrder and increments this counter. Not placed
         * items will still have [NotPlacedPlaceOrder] set.
         */
        internal var placeOrder: Int = NotPlacedPlaceOrder
            private set

        private var measuredOnce = false
        private var placedOnce = false
        val lastConstraints: Constraints?
            get() =
                if (measuredOnce) {
                    measurementConstraints
                } else {
                    null
                }

        internal var measuredByParent: LayoutNode.UsageByParent = LayoutNode.UsageByParent.NotUsed
        internal var duringAlignmentLinesQuery = false

        private var lastPosition: IntOffset = IntOffset.Zero
        private var lastLayerBlock: (GraphicsLayerScope.() -> Unit)? = null
        private var lastExplicitLayer: GraphicsLayer? = null
        private var lastZIndex: Float = 0f

        private var parentDataDirty: Boolean = true
        override var parentData: Any? = null
            private set

        /**
         * Whether or not this [LayoutNode] and all of its parents have been placed in the
         * hierarchy.
         */
        override var isPlaced: Boolean = false
            internal set

        var isPlacedByParent: Boolean = false
            internal set

        override val innerCoordinator: NodeCoordinator
            get() = layoutNode.innerCoordinator

        override val alignmentLines: AlignmentLines = LayoutNodeAlignmentLines(this)

        private val _childDelegates = MutableVector<MeasurePassDelegate>()

        internal var childDelegatesDirty: Boolean = true
        internal val childDelegates: List<MeasurePassDelegate>
            get() {
                // Update the children list first so we know whether the cached list is
                // reusable.
                layoutNode.updateChildrenIfDirty()

                if (!childDelegatesDirty) return _childDelegates.asMutableList()
                layoutNode.updateChildMeasurables(_childDelegates) {
                    it.layoutDelegate.measurePassDelegate
                }
                childDelegatesDirty = false
                return _childDelegates.asMutableList()
            }

        internal fun markDetachedFromParentLookaheadPass() {
            detachedFromParentLookaheadPass = true
        }

        var layingOutChildren = false
            private set

        private val layoutChildrenBlock: () -> Unit = {
            clearPlaceOrder()
            forEachChildAlignmentLinesOwner { it.alignmentLines.usedDuringParentLayout = false }
            innerCoordinator.measureResult.placeChildren()

            checkChildrenPlaceOrderForUpdates()
            forEachChildAlignmentLinesOwner {
                it.alignmentLines.previousUsedDuringParentLayout =
                    it.alignmentLines.usedDuringParentLayout
            }
        }

        override fun layoutChildren() {
            layingOutChildren = true
            alignmentLines.recalculateQueryOwner()

            if (layoutPending) {
                onBeforeLayoutChildren()
            }
            // as a result of the previous operation we can figure out a child has been resized
            // and we need to be remeasured, not relaid out
            if (
                layoutPendingForAlignment ||
                    (!duringAlignmentLinesQuery &&
                        !innerCoordinator.isPlacingForAlignment &&
                        layoutPending)
            ) {
                layoutPending = false
                val oldLayoutState = layoutState
                layoutState = LayoutState.LayingOut
                coordinatesAccessedDuringPlacement = false
                with(layoutNode) {
                    val owner = requireOwner()
                    owner.snapshotObserver.observeLayoutSnapshotReads(
                        this,
                        affectsLookahead = false,
                        block = layoutChildrenBlock
                    )
                }
                layoutState = oldLayoutState

                if (innerCoordinator.isPlacingForAlignment && coordinatesAccessedDuringPlacement) {
                    requestLayout()
                }
                layoutPendingForAlignment = false
            }

            if (alignmentLines.usedDuringParentLayout) {
                alignmentLines.previousUsedDuringParentLayout = true
            }
            if (alignmentLines.dirty && alignmentLines.required) alignmentLines.recalculate()

            layingOutChildren = false
        }

        private fun checkChildrenPlaceOrderForUpdates() {
            with(layoutNode) {
                forEachChild { child ->
                    // we set `placeOrder` to NotPlacedPlaceOrder for all the children, then
                    // during the placeChildren() invocation the real order will be assigned for
                    // all the placed children.
                    if (child.measurePassDelegate.previousPlaceOrder != child.placeOrder) {
                        onZSortedChildrenInvalidated()
                        invalidateLayer()
                        if (child.placeOrder == NotPlacedPlaceOrder) {
                            child.measurePassDelegate.markSubtreeAsNotPlaced()
                        }
                    }
                }
            }
        }

        private fun markSubtreeAsNotPlaced() {
            if (isPlaced) {
                isPlaced = false
                layoutNode.forEachCoordinatorIncludingInner {
                    // nodes are not placed with a layer anymore, so the layers should be released
                    it.releaseLayer()
                }
                forEachChildDelegate { it.markSubtreeAsNotPlaced() }
            }
        }

        private fun markNodeAndSubtreeAsPlaced() {
            val wasPlaced = isPlaced
            isPlaced = true
            with(layoutNode) {
                if (!wasPlaced) {
                    innerCoordinator.onPlaced()

                    // if the node was not placed previous remeasure request could have been ignored
                    if (measurePending) {
                        requestRemeasure(forceRequest = true)
                    } else if (lookaheadMeasurePending) {
                        requestLookaheadRemeasure(forceRequest = true)
                    }
                }
                // invalidate all the nodes layers that were invalidated while the node was not
                // placed
                forEachCoordinatorIncludingInner {
                    if (it.lastLayerDrawingWasSkipped) {
                        it.invalidateLayer()
                    }
                }
                forEachChild {
                    // this child was placed during the previous parent's layoutChildren(). this
                    // means that before the parent became not placed this child was placed. we need
                    // to restore that
                    if (it.placeOrder != NotPlacedPlaceOrder) {
                        it.measurePassDelegate.markNodeAndSubtreeAsPlaced()
                        rescheduleRemeasureOrRelayout(it)
                    }
                }
            }
        }

        internal var zIndex: Float = 0f
            private set

        private var onNodePlacedCalled = false

        // Used by placeOuterBlock to avoid allocating the lambda on every call
        private var placeOuterCoordinatorLayerBlock: (GraphicsLayerScope.() -> Unit)? = null
        private var placeOuterCoordinatorLayer: GraphicsLayer? = null
        private var placeOuterCoordinatorPosition = IntOffset.Zero
        private var placeOuterCoordinatorZIndex = 0f

        private val placeOuterCoordinatorBlock: () -> Unit = {
            val scope =
                outerCoordinator.wrappedBy?.placementScope
                    ?: layoutNode.requireOwner().placementScope
            with(scope) {
                val layerBlock = placeOuterCoordinatorLayerBlock
                val layer = placeOuterCoordinatorLayer
                if (layer != null) {
                    outerCoordinator.placeWithLayer(
                        placeOuterCoordinatorPosition,
                        layer,
                        placeOuterCoordinatorZIndex
                    )
                } else if (layerBlock == null) {
                    outerCoordinator.place(
                        placeOuterCoordinatorPosition,
                        placeOuterCoordinatorZIndex
                    )
                } else {
                    outerCoordinator.placeWithLayer(
                        placeOuterCoordinatorPosition,
                        placeOuterCoordinatorZIndex,
                        layerBlock
                    )
                }
            }
        }

        /** Invoked when the parent placed the node. It will trigger the layout. */
        internal fun onNodePlaced() {
            onNodePlacedCalled = true
            val parent = layoutNode.parent

            var newZIndex = innerCoordinator.zIndex
            layoutNode.forEachCoordinator { newZIndex += it.zIndex }
            if (newZIndex != zIndex) {
                zIndex = newZIndex
                parent?.onZSortedChildrenInvalidated()
                parent?.invalidateLayer()
            }

            if (!isPlaced) {
                // when the visibility of a child has been changed we need to invalidate
                // parents inner layer - the layer in which this child will be drawn
                parent?.invalidateLayer()
                markNodeAndSubtreeAsPlaced()
                if (relayoutWithoutParentInProgress) {
                    // this node wasn't placed previously and the parent thinks this node is not
                    // visible, so we need to relayout the parent to get the `placeOrder`.
                    parent?.requestRelayout()
                }
            } else {
                // Call onPlaced callback on each placement, even if it was already placed,
                // but without subtree invalidation.
                layoutNode.innerCoordinator.onPlaced()
            }

            if (parent != null) {
                if (
                    !relayoutWithoutParentInProgress && parent.layoutState == LayoutState.LayingOut
                ) {
                    // the parent is currently placing its children
                    checkPrecondition(placeOrder == NotPlacedPlaceOrder) {
                        "Place was called on a node which was placed already"
                    }
                    placeOrder = parent.layoutDelegate.nextChildPlaceOrder
                    parent.layoutDelegate.nextChildPlaceOrder++
                }
                // if relayoutWithoutParentInProgress is true we were asked to be relaid out without
                // affecting the parent. this means our placeOrder didn't change since the last time
                // parent placed us.
            } else {
                // parent is null for the root node
                placeOrder = 0
            }

            layoutChildren()
        }

        private fun clearPlaceOrder() {
            // reset the place order counter which will be used by the children
            nextChildPlaceOrder = 0
            forEachChildDelegate { child ->
                // and reset the place order for all the children before placing them
                child.previousPlaceOrder = child.placeOrder
                child.placeOrder = NotPlacedPlaceOrder
                child.isPlacedByParent = false
                // before rerunning the user's layout block reset previous measuredByParent
                // for children which we measured in the layout block during the last run.
                if (child.measuredByParent == LayoutNode.UsageByParent.InLayoutBlock) {
                    child.measuredByParent = LayoutNode.UsageByParent.NotUsed
                }
            }
        }

        private inline fun forEachChildDelegate(block: (MeasurePassDelegate) -> Unit) {
            layoutNode.forEachChild { block(it.measurePassDelegate) }
        }

        /** The function to be executed when the parent layout measures its children. */
        override fun measure(constraints: Constraints): Placeable {
            if (layoutNode.intrinsicsUsageByParent == LayoutNode.UsageByParent.NotUsed) {
                // This LayoutNode may have asked children for intrinsics. If so, we should
                // clear the intrinsics usage for everything that was requested previously.
                layoutNode.clearSubtreeIntrinsicsUsage()
            }
            // If we are at the lookahead root of the tree, do both the lookahead measure and
            // regular measure. Otherwise, we'll be consistent with parent's lookahead measure
            // and regular measure stages. This avoids producing exponential amount of
            // lookahead when LookaheadLayouts are nested.
            if (layoutNode.isOutMostLookaheadRoot()) {
                lookaheadPassDelegate!!.run {
                    measuredByParent = LayoutNode.UsageByParent.NotUsed
                    measure(constraints)
                }
            }
            trackMeasurementByParent(layoutNode)
            remeasure(constraints)
            return this
        }

        /** Return true if the measured size has been changed */
        fun remeasure(constraints: Constraints): Boolean {
            requirePrecondition(!layoutNode.isDeactivated) {
                "measure is called on a deactivated node"
            }
            val owner = layoutNode.requireOwner()
            val parent = layoutNode.parent
            @Suppress("Deprecation")
            layoutNode.canMultiMeasure =
                layoutNode.canMultiMeasure || (parent != null && parent.canMultiMeasure)
            if (layoutNode.measurePending || measurementConstraints != constraints) {
                alignmentLines.usedByModifierMeasurement = false
                forEachChildAlignmentLinesOwner {
                    it.alignmentLines.usedDuringParentMeasurement = false
                }
                measuredOnce = true
                val outerPreviousMeasuredSize = outerCoordinator.size
                measurementConstraints = constraints
                performMeasure(constraints)
                val sizeChanged =
                    outerCoordinator.size != outerPreviousMeasuredSize ||
                        outerCoordinator.width != width ||
                        outerCoordinator.height != height
                // We are using the coerced coordinator size here to avoid double offset in layout
                // coop.
                measuredSize = IntSize(outerCoordinator.width, outerCoordinator.height)
                return sizeChanged
            } else {
                // this node doesn't require being remeasured. however in order to make sure we have
                // the final size we need to also make sure the whole subtree is remeasured as it
                // can
                // trigger extra remeasure request on our node. we do it now in order to report the
                // final measured size to our parent without doing extra pass later.
                owner.forceMeasureTheSubtree(layoutNode)

                // Restore the intrinsics usage for the sub-tree
                layoutNode.resetSubtreeIntrinsicsUsage()
            }
            return false
        }

        private fun trackMeasurementByParent(node: LayoutNode) {
            val parent = node.parent
            if (parent != null) {
                checkPrecondition(
                    measuredByParent == LayoutNode.UsageByParent.NotUsed ||
                        @Suppress("DEPRECATION") node.canMultiMeasure
                ) {
                    MeasuredTwiceErrorMessage
                }
                measuredByParent =
                    when (parent.layoutState) {
                        LayoutState.Measuring -> LayoutNode.UsageByParent.InMeasureBlock
                        LayoutState.LayingOut -> LayoutNode.UsageByParent.InLayoutBlock
                        else ->
                            throw IllegalStateException(
                                "Measurable could be only measured from the parent's measure or layout" +
                                    " block. Parents state is ${parent.layoutState}"
                            )
                    }
            } else {
                // when we measure the root it is like the virtual parent is currently laying out
                measuredByParent = LayoutNode.UsageByParent.NotUsed
            }
        }

        // We are setting our measuredSize to match the coerced outerCoordinator size, to prevent
        // double offseting for layout cooperation. However, this means that here we need
        // to override these getters to make the measured values correct in Measured.
        // TODO(popam): clean this up
        override val measuredWidth: Int
            get() = outerCoordinator.measuredWidth

        override val measuredHeight: Int
            get() = outerCoordinator.measuredHeight

        override fun get(alignmentLine: AlignmentLine): Int {
            if (layoutNode.parent?.layoutState == LayoutState.Measuring) {
                alignmentLines.usedDuringParentMeasurement = true
            } else if (layoutNode.parent?.layoutState == LayoutState.LayingOut) {
                alignmentLines.usedDuringParentLayout = true
            }
            duringAlignmentLinesQuery = true
            val result = outerCoordinator[alignmentLine]
            duringAlignmentLinesQuery = false
            return result
        }

        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            placeSelf(position, zIndex, layerBlock, null)
        }

        override fun placeAt(position: IntOffset, zIndex: Float, layer: GraphicsLayer) {
            placeSelf(position, zIndex, null, layer)
        }

        /**
         * Flag to indicate when we need to propagate coordinates updates that are not related to a
         * position change.
         *
         * @see isPlacedUnderMotionFrameOfReference
         */
        private var needsCoordinatesUpdate = false

        override var isPlacedUnderMotionFrameOfReference: Boolean = false
            set(new) {
                // Delegated to outerCoordinator
                val old = outerCoordinator.isPlacedUnderMotionFrameOfReference
                if (new != old) {
                    outerCoordinator.isPlacedUnderMotionFrameOfReference = old
                    // Affects coordinates measurements
                    this.needsCoordinatesUpdate = true
                }
                field = new
            }

        private fun placeSelf(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
            layer: GraphicsLayer?
        ) {
            isPlacedByParent = true
            if (position != lastPosition || needsCoordinatesUpdate) {
                if (
                    coordinatesAccessedDuringModifierPlacement ||
                        coordinatesAccessedDuringPlacement ||
                        needsCoordinatesUpdate
                ) {
                    layoutPending = true
                    needsCoordinatesUpdate = false
                }
                notifyChildrenUsingCoordinatesWhilePlacing()
            }
            // This can actually be called as soon as LookaheadMeasure is done, but devs may expect
            // certain placement results (e.g. LayoutCoordinates) to be valid when lookahead
            // placement
            // takes place. If that's not the case, it will make sense to move this right after
            // lookahead measure, before place.
            if (layoutNode.isOutMostLookaheadRoot()) {
                // Lookahead placement first
                val scope =
                    outerCoordinator.wrappedBy?.placementScope
                        ?: layoutNode.requireOwner().placementScope
                with(scope) {
                    lookaheadPassDelegate!!.let {
                        // Since this is the root of the lookahead delegate tree, no parent will
                        // reset the place order, therefore we have to do it manually.
                        layoutNode.parent?.run { layoutDelegate.nextChildLookaheadPlaceOrder = 0 }
                        it.placeOrder = NotPlacedPlaceOrder
                        it.place(position.x, position.y)
                    }
                }
            }

            checkPrecondition(lookaheadPassDelegate?.placedOnce != false) {
                "Error: Placement happened before lookahead."
            }

            // Post-lookahead (if any) placement
            placeOuterCoordinator(position, zIndex, layerBlock, layer)
        }

        private fun placeOuterCoordinator(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
            layer: GraphicsLayer?
        ) {
            requirePrecondition(!layoutNode.isDeactivated) {
                "place is called on a deactivated node"
            }
            layoutState = LayoutState.LayingOut

            val firstPlacement = !placedOnce
            lastPosition = position
            lastZIndex = zIndex
            lastLayerBlock = layerBlock
            lastExplicitLayer = layer
            placedOnce = true
            onNodePlacedCalled = false

            val owner = layoutNode.requireOwner()
            owner.rectManager.onLayoutPositionChanged(layoutNode, position, firstPlacement)
            if (!layoutPending && isPlaced) {
                outerCoordinator.placeSelfApparentToRealOffset(position, zIndex, layerBlock, layer)
                onNodePlaced()
            } else {
                alignmentLines.usedByModifierLayout = false
                coordinatesAccessedDuringModifierPlacement = false
                placeOuterCoordinatorLayerBlock = layerBlock
                placeOuterCoordinatorPosition = position
                placeOuterCoordinatorZIndex = zIndex
                placeOuterCoordinatorLayer = layer
                owner.snapshotObserver.observeLayoutModifierSnapshotReads(
                    layoutNode,
                    affectsLookahead = false,
                    block = placeOuterCoordinatorBlock
                )
            }

            layoutState = LayoutState.Idle
        }

        /**
         * Calls [placeOuterCoordinator] with the same position used during the last
         * [placeOuterCoordinator] call. [placeOuterCoordinator] only does the placement for
         * post-lookahead pass.
         */
        fun replace() {
            try {
                relayoutWithoutParentInProgress = true
                checkPrecondition(placedOnce) { "replace called on unplaced item" }
                val wasPlacedBefore = isPlaced
                placeOuterCoordinator(lastPosition, lastZIndex, lastLayerBlock, lastExplicitLayer)
                if (wasPlacedBefore && !onNodePlacedCalled) {
                    // parent should be notified that this node is not placed anymore so the
                    // children `placeOrder`s are updated.
                    layoutNode.parent?.requestRelayout()
                }
            } finally {
                relayoutWithoutParentInProgress = false
            }
        }

        override fun minIntrinsicWidth(height: Int): Int {
            onIntrinsicsQueried()
            return outerCoordinator.minIntrinsicWidth(height)
        }

        override fun maxIntrinsicWidth(height: Int): Int {
            onIntrinsicsQueried()
            return outerCoordinator.maxIntrinsicWidth(height)
        }

        override fun minIntrinsicHeight(width: Int): Int {
            onIntrinsicsQueried()
            return outerCoordinator.minIntrinsicHeight(width)
        }

        override fun maxIntrinsicHeight(width: Int): Int {
            onIntrinsicsQueried()
            return outerCoordinator.maxIntrinsicHeight(width)
        }

        private fun onIntrinsicsQueried() {
            // How intrinsics work when specific / custom intrinsics are not provided to the custom
            // layout is we essentially run the measure block of a child with not-final constraints
            // and fake measurables. It is possible that some measure blocks are not pure and have
            // side effects, like save some state calculated during the measurement.
            // In order to make it possible we always have to rerun the measure block with the real
            // final constraints after the intrinsics run. Sometimes it will cause unnecessary
            // remeasurements, but it makes sure such component states are using the correct final
            // constraints/sizes.
            layoutNode.requestRemeasure()

            // Mark the intrinsics size has been used by the parent if it hasn't already been
            // marked.
            val parent = layoutNode.parent
            if (
                parent != null &&
                    layoutNode.intrinsicsUsageByParent == LayoutNode.UsageByParent.NotUsed
            ) {
                layoutNode.intrinsicsUsageByParent =
                    when (parent.layoutState) {
                        LayoutState.Measuring -> LayoutNode.UsageByParent.InMeasureBlock
                        LayoutState.LayingOut -> LayoutNode.UsageByParent.InLayoutBlock
                        // Called from parent's intrinsic measurement
                        else -> parent.intrinsicsUsageByParent
                    }
            }
        }

        fun invalidateParentData() {
            parentDataDirty = true
        }

        fun updateParentData(): Boolean {
            if (parentData == null && outerCoordinator.parentData == null) return false
            if (!parentDataDirty) return false
            parentDataDirty = false
            parentData = outerCoordinator.parentData
            return true
        }

        override fun calculateAlignmentLines(): Map<out AlignmentLine, Int> {
            if (!duringAlignmentLinesQuery) {
                // Mark alignments used by modifier
                if (layoutState == LayoutState.Measuring) {
                    alignmentLines.usedByModifierMeasurement = true
                    // We quickly transition to layoutPending as we need the alignment lines now.
                    // Later we will see that we also laid out as part of measurement and will skip
                    // layout.
                    if (alignmentLines.dirty) markLayoutPending()
                } else {
                    // Note this can also happen for onGloballyPositioned queries.
                    alignmentLines.usedByModifierLayout = true
                }
            }
            innerCoordinator.isPlacingForAlignment = true
            layoutChildren()
            innerCoordinator.isPlacingForAlignment = false
            return alignmentLines.getLastCalculation()
        }

        override val parentAlignmentLinesOwner: AlignmentLinesOwner?
            get() = layoutNode.parent?.layoutDelegate?.alignmentLinesOwner

        override fun forEachChildAlignmentLinesOwner(block: (AlignmentLinesOwner) -> Unit) {
            layoutNode.forEachChild { block(it.layoutDelegate.alignmentLinesOwner) }
        }

        override fun requestLayout() {
            layoutNode.requestRelayout()
        }

        override fun requestMeasure() {
            layoutNode.requestRemeasure()
        }

        /**
         * This is called any time a placement has done that changes the position during the layout
         * pass. If any child is looking at their own coordinates to know how to place children, it
         * will be invalided.
         *
         * Note that this is called for every changed position. While not many layouts look at their
         * coordinates, if there is one, it will cause all position changes from an ancestor to call
         * down the hierarchy. If this becomes expensive (e.g. many parents change their position on
         * the same frame), it might be worth using a flag so that this call becomes cheap after the
         * first one.
         */
        fun notifyChildrenUsingCoordinatesWhilePlacing() {
            if (childrenAccessingCoordinatesDuringPlacement > 0) {
                layoutNode.forEachChild { child ->
                    val childLayoutDelegate = child.layoutDelegate
                    val accessed =
                        childLayoutDelegate.coordinatesAccessedDuringPlacement ||
                            childLayoutDelegate.coordinatesAccessedDuringModifierPlacement
                    if (accessed && !childLayoutDelegate.layoutPending) {
                        child.requestRelayout()
                    }
                    childLayoutDelegate.measurePassDelegate
                        .notifyChildrenUsingCoordinatesWhilePlacing()
                }
            }
        }

        /**
         * The callback to be executed before running layoutChildren.
         *
         * There are possible cases when we run layoutChildren() on the parent node, but some of its
         * children are not yet measured even if they are supposed to be measured in the measure
         * block of our parent.
         *
         * Example: val child = Layout(...) Layout(child) { measurable, constraints -> val placeable
         * = measurable.first().measure(constraints) layout(placeable.width, placeable.height) {
         * placeable.place(0, 0) } } And now some set of changes scheduled remeasure for child and
         * relayout for parent.
         *
         * During the [MeasureAndLayoutDelegate.measureAndLayout] we will start with the parent as
         * it has lower depth. Inside the layout block we will call placeable.width which is
         * currently dirty as the child was scheduled to remeasure. This callback will ensure it
         * never happens and pre-remeasure everything required for this layoutChildren().
         */
        private fun onBeforeLayoutChildren() {
            layoutNode.forEachChild {
                if (
                    it.measurePending &&
                        it.measuredByParent == LayoutNode.UsageByParent.InMeasureBlock
                ) {
                    if (it.remeasure()) {
                        layoutNode.requestRemeasure()
                    }
                }
            }
        }

        /**
         * If this was used in an intrinsics measurement, find the parent that used it and
         * invalidate either the measure block or layout block.
         */
        fun invalidateIntrinsicsParent(forceRequest: Boolean) {
            val parent = layoutNode.parent
            val intrinsicsUsageByParent = layoutNode.intrinsicsUsageByParent
            if (parent != null && intrinsicsUsageByParent != LayoutNode.UsageByParent.NotUsed) {
                // find measuring parent
                var intrinsicsUsingParent: LayoutNode = parent
                while (intrinsicsUsingParent.intrinsicsUsageByParent == intrinsicsUsageByParent) {
                    intrinsicsUsingParent = intrinsicsUsingParent.parent ?: break
                }
                when (intrinsicsUsageByParent) {
                    LayoutNode.UsageByParent.InMeasureBlock ->
                        intrinsicsUsingParent.requestRemeasure(forceRequest)
                    LayoutNode.UsageByParent.InLayoutBlock ->
                        intrinsicsUsingParent.requestRelayout(forceRequest)
                    else -> error("Intrinsics isn't used by the parent")
                }
            }
        }

        fun onNodeDetached() {
            placeOrder = NotPlacedPlaceOrder
            previousPlaceOrder = NotPlacedPlaceOrder
            isPlaced = false
        }

        /**
         * Measure the [MeasurePassDelegate] using the lookahead constraints.
         *
         * Note: [measure] will only be invoked if we are in the right block. That means if
         * lookahead measurement was done in the measurement block, this function needs to be
         * invoked in measurement block. Otherwise, no-op.
         */
        fun measureBasedOnLookahead() {
            val lookaheadDelegate = lookaheadPassDelegate
            val parent =
                checkPreconditionNotNull(layoutNode.parent) { "layoutNode parent is not set" }
            checkPreconditionNotNull(lookaheadDelegate) { "invalid lookaheadDelegate" }
            if (
                lookaheadDelegate.measuredByParent == LayoutNode.UsageByParent.InMeasureBlock &&
                    parent.layoutState == LayoutState.Measuring
            ) {
                measure(lookaheadDelegate.lastConstraints!!)
            } else if (
                lookaheadDelegate.measuredByParent == LayoutNode.UsageByParent.InLayoutBlock &&
                    parent.layoutState == LayoutState.LayingOut
            ) {
                measure(lookaheadDelegate.lastConstraints!!)
            }
        }

        /**
         * Places the [MeasurePassDelegate] at the same position with the same zIndex and layerBlock
         * as lookahead.
         */
        fun placeBasedOnLookahead() {
            val lookaheadDelegate =
                checkPreconditionNotNull(lookaheadPassDelegate) { "invalid lookaheadDelegate" }
            placeSelf(
                lookaheadDelegate.lastPosition,
                lookaheadDelegate.lastZIndex,
                lookaheadDelegate.lastLayerBlock,
                lookaheadDelegate.lastExplicitLayer
            )
        }
    }

    /**
     * [LookaheadPassDelegate] manages the measure/layout and alignmentLine related queries for the
     * lookahead pass.
     */
    inner class LookaheadPassDelegate :
        Placeable(), Measurable, AlignmentLinesOwner, MotionReferencePlacementDelegate {

        /**
         * Is true during [replace] invocation. Helps to differentiate between the cases when our
         * parent is measuring us during the measure block, and when we are remeasured individually
         * because of some change. This could be useful to know if we need to record the placing
         * order.
         */
        private var relayoutWithoutParentInProgress: Boolean = false

        /**
         * The value [placeOrder] had during the previous parent `layoutChildren`. Helps us to
         * understand if the order did change.
         */
        private var previousPlaceOrder: Int = NotPlacedPlaceOrder
            private set

        /**
         * The order in which this node was placed by its parent during the previous
         * `layoutChildren`. Before the placement the order is set to [NotPlacedPlaceOrder] to all
         * the children. Then every placed node assigns this variable to parent's
         * LayoutNodeLayoutDelegate's [nextChildLookaheadPlaceOrder] and increments this counter.
         * Not placed items will still have [NotPlacedPlaceOrder] set.
         */
        internal var placeOrder: Int = NotPlacedPlaceOrder

        internal var measuredByParent = LayoutNode.UsageByParent.NotUsed
        internal val measurePassDelegate: MeasurePassDelegate
            get() = this@LayoutNodeLayoutDelegate.measurePassDelegate

        internal var duringAlignmentLinesQuery: Boolean = false
        internal var placedOnce: Boolean = false
        private var measuredOnce: Boolean = false
        val lastConstraints: Constraints?
            get() = lookaheadConstraints

        private var lookaheadConstraints: Constraints? = null
        internal var lastPosition: IntOffset = IntOffset.Zero
            private set

        internal var lastZIndex: Float = 0f
            private set

        internal var lastLayerBlock: (GraphicsLayerScope.() -> Unit)? = null
            private set

        internal var lastExplicitLayer: GraphicsLayer? = null
            private set

        override var isPlaced: Boolean = false

        override val innerCoordinator: NodeCoordinator
            get() = layoutNode.innerCoordinator

        override val alignmentLines: AlignmentLines = LookaheadAlignmentLines(this)

        private val _childDelegates = MutableVector<LookaheadPassDelegate>()

        internal var childDelegatesDirty: Boolean = true

        /** [Measurable]s provided to layout during lookahead pass. */
        internal val childDelegates: List<LookaheadPassDelegate>
            get() {
                layoutNode.children.let {
                    // Invoke children to get children updated before checking dirty
                    if (!childDelegatesDirty) return _childDelegates.asMutableList()
                }
                layoutNode.updateChildMeasurables(_childDelegates) {
                    it.layoutDelegate.lookaheadPassDelegate!!
                }
                childDelegatesDirty = false
                return _childDelegates.asMutableList()
            }

        var layingOutChildren = false
            private set

        private inline fun forEachChildDelegate(block: (LookaheadPassDelegate) -> Unit) =
            layoutNode.forEachChild { block(it.layoutDelegate.lookaheadPassDelegate!!) }

        override fun layoutChildren() {
            layingOutChildren = true
            alignmentLines.recalculateQueryOwner()

            if (lookaheadLayoutPending) {
                onBeforeLayoutChildren()
            }
            val lookaheadDelegate = innerCoordinator.lookaheadDelegate!!
            // as a result of the previous operation we can figure out a child has been resized
            // and we need to be remeasured, not relaid out
            if (
                lookaheadLayoutPendingForAlignment ||
                    (!duringAlignmentLinesQuery &&
                        !lookaheadDelegate.isPlacingForAlignment &&
                        lookaheadLayoutPending)
            ) {
                lookaheadLayoutPending = false
                val oldLayoutState = layoutState
                layoutState = LayoutState.LookaheadLayingOut
                val owner = layoutNode.requireOwner()
                lookaheadCoordinatesAccessedDuringPlacement = false
                owner.snapshotObserver.observeLayoutSnapshotReads(layoutNode) {
                    clearPlaceOrder()
                    forEachChildAlignmentLinesOwner { child ->
                        child.alignmentLines.usedDuringParentLayout = false
                    }
                    innerCoordinator.lookaheadDelegate?.isPlacingForAlignment?.let { forAlignment ->
                        layoutNode.children.fastForEach {
                            it.outerCoordinator.lookaheadDelegate?.isPlacingForAlignment =
                                forAlignment
                        }
                    }
                    lookaheadDelegate.measureResult.placeChildren()
                    innerCoordinator.lookaheadDelegate?.isPlacingForAlignment?.let { _ ->
                        layoutNode.children.fastForEach {
                            it.outerCoordinator.lookaheadDelegate?.isPlacingForAlignment = false
                        }
                    }
                    checkChildrenPlaceOrderForUpdates()
                    forEachChildAlignmentLinesOwner { child ->
                        child.alignmentLines.previousUsedDuringParentLayout =
                            child.alignmentLines.usedDuringParentLayout
                    }
                }
                layoutState = oldLayoutState
                if (
                    lookaheadCoordinatesAccessedDuringPlacement &&
                        lookaheadDelegate.isPlacingForAlignment
                ) {
                    requestLayout()
                }
                lookaheadLayoutPendingForAlignment = false
            }
            if (alignmentLines.usedDuringParentLayout) {
                alignmentLines.previousUsedDuringParentLayout = true
            }
            if (alignmentLines.dirty && alignmentLines.required) alignmentLines.recalculate()

            layingOutChildren = false
        }

        private fun checkChildrenPlaceOrderForUpdates() {
            forEachChildDelegate { child ->
                // we set `placeOrder` to NotPlacedPlaceOrder for all the children, then
                // during the placeChildren() invocation the real order will be assigned for
                // all the placed children.
                if (child.previousPlaceOrder != child.placeOrder) {
                    if (child.placeOrder == NotPlacedPlaceOrder) {
                        child.markSubtreeAsNotPlaced()
                    }
                }
            }
        }

        private fun markSubtreeAsNotPlaced() {
            if (isPlaced) {
                isPlaced = false
                forEachChildDelegate { it.markSubtreeAsNotPlaced() }
            }
        }

        override fun calculateAlignmentLines(): Map<out AlignmentLine, Int> {
            if (!duringAlignmentLinesQuery) {
                if (layoutState == LayoutState.LookaheadMeasuring) {
                    // Mark alignments used by modifier
                    alignmentLines.usedByModifierMeasurement = true
                    // We quickly transition to layoutPending as we need the alignment lines now.
                    // Later we will see that we also laid out as part of measurement and will skip
                    // layout.
                    if (alignmentLines.dirty) markLookaheadLayoutPending()
                } else {
                    // Note this can also happen for onGloballyPositioned queries.
                    alignmentLines.usedByModifierLayout = true
                }
            }
            innerCoordinator.lookaheadDelegate?.isPlacingForAlignment = true
            layoutChildren()
            innerCoordinator.lookaheadDelegate?.isPlacingForAlignment = false
            return alignmentLines.getLastCalculation()
        }

        override val parentAlignmentLinesOwner: AlignmentLinesOwner?
            get() = layoutNode.parent?.layoutDelegate?.lookaheadAlignmentLinesOwner

        override fun forEachChildAlignmentLinesOwner(block: (AlignmentLinesOwner) -> Unit) {
            layoutNode.forEachChild { block(it.layoutDelegate.lookaheadAlignmentLinesOwner!!) }
        }

        override fun requestLayout() {
            layoutNode.requestLookaheadRelayout()
        }

        override fun requestMeasure() {
            layoutNode.requestLookaheadRemeasure()
        }

        /**
         * This is called any time a placement has done that changes the position during the
         * lookahead layout pass. If any child is looking at their own coordinates to know how to
         * place children, it will be invalided.
         *
         * Note that this is called for every changed position. While not many layouts look at their
         * coordinates, if there is one, it will cause all position changes from an ancestor to call
         * down the hierarchy. If this becomes expensive (e.g. many parents change their position on
         * the same frame), it might be worth using a flag so that this call becomes cheap after the
         * first one.
         */
        fun notifyChildrenUsingLookaheadCoordinatesWhilePlacing() {
            if (childrenAccessingLookaheadCoordinatesDuringPlacement > 0) {
                layoutNode.forEachChild { child ->
                    val childLayoutDelegate = child.layoutDelegate
                    val accessed =
                        childLayoutDelegate.lookaheadCoordinatesAccessedDuringPlacement ||
                            childLayoutDelegate.lookaheadCoordinatesAccessedDuringModifierPlacement
                    if (accessed && !childLayoutDelegate.lookaheadLayoutPending) {
                        child.requestLookaheadRelayout()
                    }
                    childLayoutDelegate.lookaheadPassDelegate
                        ?.notifyChildrenUsingLookaheadCoordinatesWhilePlacing()
                }
            }
        }

        override fun measure(constraints: Constraints): Placeable {
            if (
                layoutNode.parent?.layoutState == LayoutState.LookaheadMeasuring ||
                    layoutNode.parent?.layoutState == LayoutState.LookaheadLayingOut
            ) {
                detachedFromParentLookaheadPass = false
            }
            trackLookaheadMeasurementByParent(layoutNode)
            if (layoutNode.intrinsicsUsageByParent == LayoutNode.UsageByParent.NotUsed) {
                // This LayoutNode may have asked children for intrinsics. If so, we should
                // clear the intrinsics usage for everything that was requested previously.
                layoutNode.clearSubtreeIntrinsicsUsage()
            }
            // Since this a measure request coming from the parent. We'd be starting lookahead
            // only if the current layoutNode is the top-level lookahead root.
            // This is an optimization to avoid redundant Snapshot.enter when creating new snapshots
            // for lookahead, in order to reduce the size of the call stack.
            remeasure(constraints)
            return this
        }

        // Track lookahead measurement
        private fun trackLookaheadMeasurementByParent(node: LayoutNode) {
            // when we measure the root it is like the virtual parent is currently laying out
            val parent = node.parent
            if (parent != null) {
                checkPrecondition(
                    measuredByParent == LayoutNode.UsageByParent.NotUsed ||
                        @Suppress("DEPRECATION") node.canMultiMeasure
                ) {
                    MeasuredTwiceErrorMessage
                }
                measuredByParent =
                    when (parent.layoutState) {
                        LayoutState.LookaheadMeasuring,
                        LayoutState.Measuring -> LayoutNode.UsageByParent.InMeasureBlock
                        LayoutState.LayingOut,
                        LayoutState.LookaheadLayingOut -> LayoutNode.UsageByParent.InLayoutBlock
                        else ->
                            throw IllegalStateException(
                                "Measurable could be only measured from the parent's measure or layout" +
                                    " block. Parents state is ${parent.layoutState}"
                            )
                    }
            } else {
                measuredByParent = LayoutNode.UsageByParent.NotUsed
            }
        }

        private var parentDataDirty: Boolean = true
        override var parentData: Any? = measurePassDelegate.parentData
            private set

        // Lookahead remeasurement with the given constraints.
        fun remeasure(constraints: Constraints): Boolean {
            requirePrecondition(!layoutNode.isDeactivated) {
                "measure is called on a deactivated node"
            }
            val parent = layoutNode.parent
            @Suppress("Deprecation")
            layoutNode.canMultiMeasure =
                layoutNode.canMultiMeasure || (parent != null && parent.canMultiMeasure)
            if (layoutNode.lookaheadMeasurePending || lookaheadConstraints != constraints) {
                lookaheadConstraints = constraints
                measurementConstraints = constraints
                alignmentLines.usedByModifierMeasurement = false
                forEachChildAlignmentLinesOwner {
                    it.alignmentLines.usedDuringParentMeasurement = false
                }
                // Copy out the previous size before performing lookahead measure. If never
                // measured, set the last size to negative instead of Zero in anticipation for zero
                // being a valid lookahead size.
                val lastLookaheadSize =
                    if (measuredOnce) measuredSize else IntSize(Int.MIN_VALUE, Int.MIN_VALUE)
                measuredOnce = true
                val lookaheadDelegate = outerCoordinator.lookaheadDelegate
                checkPrecondition(lookaheadDelegate != null) {
                    "Lookahead result from lookaheadRemeasure cannot be null"
                }

                performLookaheadMeasure(constraints)
                measuredSize = IntSize(lookaheadDelegate.width, lookaheadDelegate.height)
                val sizeChanged =
                    lastLookaheadSize.width != lookaheadDelegate.width ||
                        lastLookaheadSize.height != lookaheadDelegate.height
                return sizeChanged
            } else {
                // this node doesn't require being remeasured. however in order to make sure we have
                // the final size we need to also make sure the whole subtree is remeasured as it
                // can trigger extra remeasure request on our node. we do it now in order to report
                // the final measured size to our parent without doing extra pass later.
                layoutNode.owner?.forceMeasureTheSubtree(layoutNode, affectsLookahead = true)

                // Restore the intrinsics usage for the sub-tree
                layoutNode.resetSubtreeIntrinsicsUsage()
            }
            return false
        }

        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            placeSelf(position, zIndex, layerBlock, null)
        }

        override fun placeAt(position: IntOffset, zIndex: Float, layer: GraphicsLayer) {
            placeSelf(position, zIndex, null, layer)
        }

        override var isPlacedUnderMotionFrameOfReference: Boolean = false
            set(new) {
                // Delegated to outerCoordinator
                val old = outerCoordinator.lookaheadDelegate?.isPlacedUnderMotionFrameOfReference
                if (new != old) {
                    outerCoordinator.lookaheadDelegate?.isPlacedUnderMotionFrameOfReference = new
                }
                field = new
            }

        private fun placeSelf(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
            layer: GraphicsLayer?
        ) {
            requirePrecondition(!layoutNode.isDeactivated) {
                "place is called on a deactivated node"
            }
            layoutState = LayoutState.LookaheadLayingOut
            placedOnce = true
            onNodePlacedCalled = false
            if (position != lastPosition) {
                if (
                    lookaheadCoordinatesAccessedDuringModifierPlacement ||
                        lookaheadCoordinatesAccessedDuringPlacement
                ) {
                    lookaheadLayoutPending = true
                }
                notifyChildrenUsingLookaheadCoordinatesWhilePlacing()
            }
            val owner = layoutNode.requireOwner()

            if (!lookaheadLayoutPending && isPlaced) {
                outerCoordinator.lookaheadDelegate!!.placeSelfApparentToRealOffset(position)
                onNodePlaced()
            } else {
                lookaheadCoordinatesAccessedDuringModifierPlacement = false
                alignmentLines.usedByModifierLayout = false
                owner.snapshotObserver.observeLayoutModifierSnapshotReads(layoutNode) {
                    val scope =
                        if (layoutNode.isOutMostLookaheadRoot()) {
                            outerCoordinator.wrappedBy?.placementScope
                        } else {
                            outerCoordinator.wrappedBy?.lookaheadDelegate?.placementScope
                        } ?: owner.placementScope
                    with(scope) { outerCoordinator.lookaheadDelegate!!.place(position) }
                }
            }
            lastPosition = position
            lastZIndex = zIndex
            lastLayerBlock = layerBlock
            lastExplicitLayer = layer
            layoutState = LayoutState.Idle
        }

        // We are setting our measuredSize to match the coerced outerCoordinator size, to prevent
        // double offseting for layout cooperation. However, this means that here we need
        // to override these getters to make the measured values correct in Measured.
        // TODO(popam): clean this up
        override val measuredWidth: Int
            get() = outerCoordinator.lookaheadDelegate!!.measuredWidth

        override val measuredHeight: Int
            get() = outerCoordinator.lookaheadDelegate!!.measuredHeight

        override fun get(alignmentLine: AlignmentLine): Int {
            if (layoutNode.parent?.layoutState == LayoutState.LookaheadMeasuring) {
                alignmentLines.usedDuringParentMeasurement = true
            } else if (layoutNode.parent?.layoutState == LayoutState.LookaheadLayingOut) {
                alignmentLines.usedDuringParentLayout = true
            }
            duringAlignmentLinesQuery = true
            val result = outerCoordinator.lookaheadDelegate!![alignmentLine]
            duringAlignmentLinesQuery = false
            return result
        }

        override fun minIntrinsicWidth(height: Int): Int {
            onIntrinsicsQueried()
            return outerCoordinator.lookaheadDelegate!!.minIntrinsicWidth(height)
        }

        override fun maxIntrinsicWidth(height: Int): Int {
            onIntrinsicsQueried()
            return outerCoordinator.lookaheadDelegate!!.maxIntrinsicWidth(height)
        }

        override fun minIntrinsicHeight(width: Int): Int {
            onIntrinsicsQueried()
            return outerCoordinator.lookaheadDelegate!!.minIntrinsicHeight(width)
        }

        override fun maxIntrinsicHeight(width: Int): Int {
            onIntrinsicsQueried()
            return outerCoordinator.lookaheadDelegate!!.maxIntrinsicHeight(width)
        }

        private fun onIntrinsicsQueried() {
            // How intrinsics work when specific / custom intrinsics are not provided to the custom
            // layout is we essentially run the measure block of a child with not-final constraints
            // and fake measurables. It is possible that some measure blocks are not pure and have
            // side effects, like save some state calculated during the measurement.
            // In order to make it possible we always have to rerun the measure block with the real
            // final constraints after the intrinsics run. Sometimes it will cause unnecessary
            // remeasurements, but it makes sure such component states are using the correct final
            // constraints/sizes.
            layoutNode.requestLookaheadRemeasure()

            // Mark the intrinsics size has been used by the parent if it hasn't already been
            // marked.
            val parent = layoutNode.parent
            if (
                parent != null &&
                    layoutNode.intrinsicsUsageByParent == LayoutNode.UsageByParent.NotUsed
            ) {
                layoutNode.intrinsicsUsageByParent =
                    when (parent.layoutState) {
                        LayoutState.Measuring -> LayoutNode.UsageByParent.InMeasureBlock
                        LayoutState.LayingOut -> LayoutNode.UsageByParent.InLayoutBlock
                        // Called from parent's intrinsic measurement
                        else -> parent.intrinsicsUsageByParent
                    }
            }
        }

        /**
         * If this was used in an intrinsics measurement, find the parent that used it and
         * invalidate either the measure block or layout block.
         */
        fun invalidateIntrinsicsParent(forceRequest: Boolean) {
            val parent = layoutNode.parent
            val intrinsicsUsageByParent = layoutNode.intrinsicsUsageByParent
            if (parent != null && intrinsicsUsageByParent != LayoutNode.UsageByParent.NotUsed) {
                // find measuring parent
                var intrinsicsUsingParent: LayoutNode = parent
                while (intrinsicsUsingParent.intrinsicsUsageByParent == intrinsicsUsageByParent) {
                    intrinsicsUsingParent = intrinsicsUsingParent.parent ?: break
                }
                when (intrinsicsUsageByParent) {
                    LayoutNode.UsageByParent.InMeasureBlock ->
                        if (intrinsicsUsingParent.lookaheadRoot != null) {
                            intrinsicsUsingParent.requestLookaheadRemeasure(forceRequest)
                        } else {
                            intrinsicsUsingParent.requestRemeasure(forceRequest)
                        }
                    LayoutNode.UsageByParent.InLayoutBlock ->
                        if (intrinsicsUsingParent.lookaheadRoot != null) {
                            intrinsicsUsingParent.requestLookaheadRelayout(forceRequest)
                        } else {
                            intrinsicsUsingParent.requestRelayout(forceRequest)
                        }
                    else -> error("Intrinsics isn't used by the parent")
                }
            }
        }

        fun invalidateParentData() {
            parentDataDirty = true
        }

        fun updateParentData(): Boolean {
            if (parentData == null && outerCoordinator.lookaheadDelegate!!.parentData == null) {
                return false
            }
            if (!parentDataDirty) return false
            parentDataDirty = false
            parentData = outerCoordinator.lookaheadDelegate!!.parentData
            return true
        }

        private var onNodePlacedCalled = false

        internal fun onNodePlaced() {
            onNodePlacedCalled = true
            val parent = layoutNode.parent
            if (!isPlaced) {
                markNodeAndSubtreeAsPlaced()
                if (relayoutWithoutParentInProgress) {
                    // this node wasn't placed previously and the parent thinks this node is not
                    // visible, so we need to relayout the parent to get the `placeOrder`.
                    parent?.requestLookaheadRelayout()
                }
            }
            if (parent != null) {
                if (
                    !relayoutWithoutParentInProgress &&
                        (parent.layoutState == LayoutState.LayingOut ||
                            parent.layoutState == LayoutState.LookaheadLayingOut)
                ) {
                    // the parent is currently placing its children
                    checkPrecondition(placeOrder == NotPlacedPlaceOrder) {
                        "Place was called on a node which was placed already"
                    }
                    placeOrder = parent.layoutDelegate.nextChildLookaheadPlaceOrder
                    parent.layoutDelegate.nextChildLookaheadPlaceOrder++
                }
                // if relayoutWithoutParentInProgress is true we were asked to be relaid out without
                // affecting the parent. this means our placeOrder didn't change since the last time
                // parent placed us.
            } else {
                // parent is null for the root node
                placeOrder = 0
            }
            layoutChildren()
        }

        private fun clearPlaceOrder() {
            // reset the place order counter which will be used by the children
            this@LayoutNodeLayoutDelegate.nextChildLookaheadPlaceOrder = 0
            forEachChildDelegate { child ->
                // and reset the place order for all the children before placing them
                child.previousPlaceOrder = child.placeOrder
                child.placeOrder = NotPlacedPlaceOrder
                // before rerunning the user's layout block reset previous measuredByParent
                // for children which we measured in the layout block during the last run.
                if (child.measuredByParent == LayoutNode.UsageByParent.InLayoutBlock) {
                    child.measuredByParent = LayoutNode.UsageByParent.NotUsed
                }
            }
        }

        private fun markNodeAndSubtreeAsPlaced() {
            val wasPlaced = isPlaced
            isPlaced = true
            if (!wasPlaced) {
                if (lookaheadMeasurePending) {
                    // if the node was not placed previous remeasure request could have been ignored
                    layoutNode.requestLookaheadRemeasure(forceRequest = true)
                }
            }

            layoutNode.forEachChild {
                // this child was placed during the previous parent's layoutChildren(). this means
                // that
                // before the parent became not placed this child was placed. we need to restore
                // that
                val childDelegate =
                    requireNotNull(it.lookaheadPassDelegate) {
                        "Error: Child node's lookahead pass delegate cannot be null " +
                            "when in a lookahead scope."
                    }
                if (childDelegate.placeOrder != NotPlacedPlaceOrder) {
                    childDelegate.markNodeAndSubtreeAsPlaced()
                    it.rescheduleRemeasureOrRelayout(it)
                }
            }
        }

        /**
         * The callback to be executed before running layoutChildren.
         *
         * There are possible cases when we run layoutChildren() on the parent node, but some of its
         * children are not yet measured even if they are supposed to be measured in the measure
         * block of our parent.
         *
         * Example: val child = Layout(...) Layout(child) { measurable, constraints -> val placeable
         * = measurable.first().measure(constraints) layout(placeable.width, placeable.height) {
         * placeable.place(0, 0) } } And now some set of changes scheduled remeasure for child and
         * relayout for parent.
         *
         * During the [MeasureAndLayoutDelegate.measureAndLayout] we will start with the parent as
         * it has lower depth. Inside the layout block we will call placeable.width which is
         * currently dirty as the child was scheduled to remeasure. This callback will ensure it
         * never happens and pre-remeasure everything required for this layoutChildren().
         */
        private fun onBeforeLayoutChildren() {
            layoutNode.forEachChild {
                if (
                    it.lookaheadMeasurePending &&
                        it.measuredByParentInLookahead == LayoutNode.UsageByParent.InMeasureBlock
                ) {
                    if (
                        it.layoutDelegate.lookaheadPassDelegate!!.remeasure(
                            it.layoutDelegate.lastLookaheadConstraints!!
                        )
                    ) {
                        layoutNode.requestLookaheadRemeasure()
                    }
                }
            }
        }

        fun replace() {
            try {
                relayoutWithoutParentInProgress = true
                checkPrecondition(placedOnce) { "replace() called on item that was not placed" }

                onNodePlacedCalled = false
                val wasPlacedBefore = isPlaced
                placeSelf(lastPosition, 0f, lastLayerBlock, lastExplicitLayer)
                if (wasPlacedBefore && !onNodePlacedCalled) {
                    // parent should be notified that this node is not placed anymore so the
                    // children `placeOrder`s are updated.
                    layoutNode.parent?.requestLookaheadRelayout()
                }
            } finally {
                relayoutWithoutParentInProgress = false
            }
        }

        fun onNodeDetached() {
            placeOrder = NotPlacedPlaceOrder
            previousPlaceOrder = NotPlacedPlaceOrder
            isPlaced = false
        }
    }

    /**
     * Performs measure with the given constraints and perform necessary state mutations before and
     * after the measurement.
     */
    private fun performMeasure(constraints: Constraints) {
        checkPrecondition(layoutState == LayoutState.Idle) {
            "layout state is not idle before measure starts"
        }
        layoutState = LayoutState.Measuring
        measurePending = false
        performMeasureConstraints = constraints
        layoutNode
            .requireOwner()
            .snapshotObserver
            .observeMeasureSnapshotReads(layoutNode, affectsLookahead = false, performMeasureBlock)
        // The resulting layout state might be Ready. This can happen when the layout node's
        // own modifier is querying an alignment line during measurement, therefore we
        // need to also layout the layout node.
        if (layoutState == LayoutState.Measuring) {
            markLayoutPending()
            layoutState = LayoutState.Idle
        }
    }

    private fun performLookaheadMeasure(constraints: Constraints) {
        layoutState = LayoutState.LookaheadMeasuring
        lookaheadMeasurePending = false
        layoutNode.requireOwner().snapshotObserver.observeMeasureSnapshotReads(layoutNode) {
            outerCoordinator.lookaheadDelegate!!.measure(constraints)
        }
        markLookaheadLayoutPending()
        if (layoutNode.isOutMostLookaheadRoot()) {
            // If layoutNode is the root of the lookahead, measure is redirected to lookahead
            // measure, and layout pass will begin lookahead placement, measure & layout.
            markLayoutPending()
        } else {
            // If layoutNode is not the root of the lookahead, measure needs to follow the
            // lookahead measure.
            markMeasurePending()
        }
        layoutState = LayoutState.Idle
    }

    internal fun ensureLookaheadDelegateCreated() {
        if (lookaheadPassDelegate == null) {
            lookaheadPassDelegate = LookaheadPassDelegate()
        }
    }

    fun updateParentData() {
        if (measurePassDelegate.updateParentData()) {
            layoutNode.parent?.requestRemeasure()
        }
        if (lookaheadPassDelegate?.updateParentData() == true) {
            if (layoutNode.isOutMostLookaheadRoot()) {
                layoutNode.parent?.requestRemeasure()
            } else {
                layoutNode.parent?.requestLookaheadRemeasure()
            }
        }
    }

    fun invalidateParentData() {
        measurePassDelegate.invalidateParentData()
        lookaheadPassDelegate?.invalidateParentData()
    }

    fun resetAlignmentLines() {
        measurePassDelegate.alignmentLines.reset()
        lookaheadPassDelegate?.alignmentLines?.reset()
    }

    fun markChildrenDirty() {
        measurePassDelegate.childDelegatesDirty = true
        lookaheadPassDelegate?.let { it.childDelegatesDirty = true }
    }
}

/**
 * Returns if the we are at the lookahead root of the tree, by checking if the parent is has a
 * lookahead root.
 */
internal fun LayoutNode.isOutMostLookaheadRoot(): Boolean =
    lookaheadRoot != null &&
        (parent?.lookaheadRoot == null || layoutDelegate.detachedFromParentLookaheadPass)

private inline fun <T : Measurable> LayoutNode.updateChildMeasurables(
    destination: MutableVector<T>,
    transform: (LayoutNode) -> T
) {
    forEachChildIndexed { i, layoutNode ->
        if (destination.size <= i) {
            destination.add(transform(layoutNode))
        } else {
            destination[i] = transform(layoutNode)
        }
    }
    destination.removeRange(children.size, destination.size)
}

private const val MeasuredTwiceErrorMessage: String =
    "measure() may not be called multiple times on the same Measurable. If you want to " +
        "get the content size of the Measurable before calculating the final constraints, " +
        "please use methods like minIntrinsicWidth()/maxIntrinsicWidth() and " +
        "minIntrinsicHeight()/maxIntrinsicHeight()"

/**
 * AlignmentLinesOwner defines APIs that are needed to respond to alignment line changes, and to
 * query alignment line related info.
 *
 * [LayoutNodeLayoutDelegate.LookaheadPassDelegate] and
 * [LayoutNodeLayoutDelegate.MeasurePassDelegate] both implement this interface, and they
 * encapsulate the difference in alignment lines handling for lookahead pass vs. actual
 * measure/layout pass.
 */
internal interface AlignmentLinesOwner : Measurable {
    /** Whether the AlignmentLinesOwner has been placed. */
    val isPlaced: Boolean

    /** InnerNodeCoordinator of the LayoutNode that the AlignmentLinesOwner operates on. */
    val innerCoordinator: NodeCoordinator

    /**
     * Alignment lines for either lookahead pass or post-lookahead pass, depending on the
     * AlignmentLineOwner.
     */
    val alignmentLines: AlignmentLines

    /**
     * The implementation for laying out children. Different types of AlignmentLinesOwner will
     * layout children for either the lookahead pass, or the layout pass post-lookahead.
     */
    fun layoutChildren()

    /** Recalculate the alignment lines if dirty, and layout children as needed. */
    fun calculateAlignmentLines(): Map<out AlignmentLine, Int>

    /**
     * Parent [AlignmentLinesOwner]. This will be the AlignmentLinesOwner for the same pass but for
     * the parent [LayoutNode].
     */
    val parentAlignmentLinesOwner: AlignmentLinesOwner?

    /**
     * This allows iterating all the AlignmentOwners for the same pass for each of the child
     * LayoutNodes
     */
    fun forEachChildAlignmentLinesOwner(block: (AlignmentLinesOwner) -> Unit)

    /**
     * Depending on which pass the [AlignmentLinesOwner] is created for, this could mean
     * requestLookaheadLayout() for the lookahead pass, or requestLayout() for post- lookahead pass.
     */
    fun requestLayout()

    /**
     * Depending on which pass the [AlignmentLinesOwner] is created for, this could mean
     * requestLookaheadMeasure() for the lookahead pass, or requestMeasure() for post- lookahead
     * pass.
     */
    fun requestMeasure()
}

/**
 * Interface for layout delegates, so that they can set the
 * [LookaheadCapablePlaceable.isPlacedUnderMotionFrameOfReference] to the proper placeable.
 */
internal interface MotionReferencePlacementDelegate {

    /**
     * Called when a layout is about to be placed.
     *
     * The corresponding [LookaheadCapablePlaceable] should have their
     * [LookaheadCapablePlaceable.isPlacedUnderMotionFrameOfReference] flag updated to the given
     * value.
     *
     * The placeable should be tagged such that its corresponding coordinates reflect the flag in
     * [androidx.compose.ui.layout.LayoutCoordinates.introducesMotionFrameOfReference]. Note that
     * when it's placed on the current frame of reference, it means it doesn't introduce a new frame
     * of reference.
     *
     * This also means that coordinates consumers (onPlaced readers) are expected to be updated.
     */
    var isPlacedUnderMotionFrameOfReference: Boolean
}
