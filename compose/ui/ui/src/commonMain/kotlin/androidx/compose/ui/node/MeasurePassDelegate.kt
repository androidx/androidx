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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.LayoutNode.Companion.NotPlacedPlaceOrder
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * [MeasurePassDelegate] manages the measure/layout and alignmentLine related queries for the actual
 * measure/layout pass.
 */
internal class MeasurePassDelegate(private val layoutNodeLayoutDelegate: LayoutNodeLayoutDelegate) :
    Measurable, Placeable(), AlignmentLinesOwner, MotionReferencePlacementDelegate {
    /**
     * Is true during [replace] invocation. Helps to differentiate between the cases when our parent
     * is measuring us during the measure block, and when we are remeasured individually because of
     * some change. This could be useful to know if we need to record the placing order.
     */
    private var relayoutWithoutParentInProgress: Boolean = false

    /**
     * The value [placeOrder] had during the previous parent `layoutChildren`. Helps us to
     * understand if the order did change.
     */
    internal var previousPlaceOrder: Int = NotPlacedPlaceOrder
        private set

    /**
     * The order in which this node was placed by its parent during the previous `layoutChildren`.
     * Before the placement the order is set to [NotPlacedPlaceOrder] to all the children. Then
     * every placed node assigns this variable to parent's LayoutNodeLayoutDelegate's
     * nextChildPlaceOrder and increments this counter. Not placed items will still have
     * [NotPlacedPlaceOrder] set.
     */
    internal var placeOrder: Int = NotPlacedPlaceOrder
        private set

    private var measuredOnce = false
    var placedOnce = false
        private set

    val lastConstraints: Constraints?
        get() =
            if (measuredOnce) {
                measurementConstraints
            } else {
                null
            }

    val layoutNode: LayoutNode
        get() = layoutNodeLayoutDelegate.layoutNode

    internal var measuredByParent: LayoutNode.UsageByParent = LayoutNode.UsageByParent.NotUsed
    internal var duringAlignmentLinesQuery = false

    internal var lastPosition: IntOffset = IntOffset.Zero
        private set

    private var lastLayerBlock: (GraphicsLayerScope.() -> Unit)? = null
    private var lastExplicitLayer: GraphicsLayer? = null
    private var lastZIndex: Float = 0f

    private var parentDataDirty: Boolean = true
    override var parentData: Any? = null
        private set

    private val lookaheadPassDelegate: LookaheadPassDelegate?
        get() = layoutNodeLayoutDelegate.lookaheadPassDelegate

    /**
     * Whether or not this [LayoutNode] and all of its parents have been placed in the hierarchy.
     */
    override var isPlaced: Boolean = false
        internal set

    var isPlacedByParent: Boolean = false
        internal set

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

    var layoutState: LayoutState
        get() = layoutNodeLayoutDelegate.layoutState
        set(value) {
            layoutNodeLayoutDelegate.layoutState = value
        }

    val outerCoordinator: NodeCoordinator
        get() = layoutNodeLayoutDelegate.outerCoordinator

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
        layoutNodeLayoutDelegate.detachedFromParentLookaheadPass = true
    }

    var layingOutChildren = false
        private set

    // Used by performMeasureBlock so that we don't have to allocate a lambda on every call
    private var performMeasureConstraints = Constraints()

    internal val performMeasureBlock: () -> Unit = {
        outerCoordinator.measure(performMeasureConstraints)
    }

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
            layoutNodeLayoutDelegate.coordinatesAccessedDuringPlacement = false
            with(layoutNode) {
                val owner = requireOwner()
                owner.snapshotObserver.observeLayoutSnapshotReads(this, block = layoutChildrenBlock)
            }
            layoutState = oldLayoutState

            if (
                innerCoordinator.isPlacingForAlignment &&
                    layoutNodeLayoutDelegate.coordinatesAccessedDuringPlacement
            ) {
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
                    if (
                        child.placeOrder == androidx.compose.ui.node.LayoutNode.NotPlacedPlaceOrder
                    ) {
                        if (child.layoutDelegate.detachedFromParentLookaheadPlacement) {
                            // Child's lookahead placement is dependent on the approach
                            // placement
                            child.lookaheadPassDelegate!!.markNodeAndSubtreeAsNotPlaced(
                                inLookahead = false
                            )
                        }
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
                // TODO(b/309776096): Node can be detached without calling this, so we need to
                //  find a better place to more reliable call this.
                it.onUnplaced()

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
                if (it.placeOrder != androidx.compose.ui.node.LayoutNode.NotPlacedPlaceOrder) {
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
            outerCoordinator.wrappedBy?.placementScope ?: layoutNode.requireOwner().placementScope
        with(scope) {
            val layerBlock = placeOuterCoordinatorLayerBlock
            val layer = placeOuterCoordinatorLayer
            if (layer != null) {
                outerCoordinator.placeWithLayer(
                    placeOuterCoordinatorPosition,
                    layer,
                    placeOuterCoordinatorZIndex,
                )
            } else if (layerBlock == null) {
                outerCoordinator.place(placeOuterCoordinatorPosition, placeOuterCoordinatorZIndex)
            } else {
                outerCoordinator.placeWithLayer(
                    placeOuterCoordinatorPosition,
                    placeOuterCoordinatorZIndex,
                    layerBlock,
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
            if (!relayoutWithoutParentInProgress && parent.layoutState == LayoutState.LayingOut) {
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
        layoutNodeLayoutDelegate.nextChildPlaceOrder = 0
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

    /**
     * Performs measure with the given constraints and perform necessary state mutations before and
     * after the measurement.
     */
    // inlined as used only in one place to not add extra function call overhead
    @Suppress("NOTHING_TO_INLINE")
    internal inline fun performMeasure(constraints: Constraints) {
        checkPrecondition(layoutState == LayoutState.Idle) {
            "layout state is not idle before measure starts"
        }
        performMeasureConstraints = constraints
        layoutState = LayoutState.Measuring
        measurePending = false
        layoutNode
            .requireOwner()
            .snapshotObserver
            .observeMeasureSnapshotReads(layoutNode, performMeasureBlock)
        // The resulting layout state might be Ready. This can happen when the layout node's
        // own modifier is querying an alignment line during measurement, therefore we
        // need to also layout the layout node.
        if (layoutState == LayoutState.Measuring) {
            markLayoutPending()
            layoutState = LayoutState.Idle
        }
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
        if (layoutNode.isOutMostLookaheadRoot) {
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
        withComposeStackTrace(layoutNode) {
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
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
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

    override fun updatePlacedUnderMotionFrameOfReference(newMFR: Boolean) {
        // Delegated to outerCoordinator
        val old = outerCoordinator.isPlacedUnderMotionFrameOfReference
        if (newMFR != old) {
            outerCoordinator.isPlacedUnderMotionFrameOfReference = newMFR
            // Affects coordinates measurements
            this.needsCoordinatesUpdate = true
        }
        isPlacedUnderMotionFrameOfReference = newMFR
    }

    private fun placeSelf(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        layer: GraphicsLayer?,
    ) {
        withComposeStackTrace(layoutNode) {
            isPlacedByParent = true
            if (position != lastPosition || needsCoordinatesUpdate) {
                if (
                    layoutNodeLayoutDelegate.coordinatesAccessedDuringModifierPlacement ||
                        layoutNodeLayoutDelegate.coordinatesAccessedDuringPlacement ||
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
            if (lookaheadPassDelegate?.needsToBePlacedInApproach == true) {
                // Lookahead placement first
                val scope =
                    outerCoordinator.wrappedBy?.placementScope
                        ?: layoutNode.requireOwner().placementScope
                with(scope) {
                    lookaheadPassDelegate!!.let {
                        // Since this is the root of the lookahead delegate tree, no parent will
                        // reset the place order, therefore we have to do it manually.
                        layoutNode.parent?.run { layoutDelegate.nextChildLookaheadPlaceOrder = 0 }
                        it.placeOrder = androidx.compose.ui.node.LayoutNode.NotPlacedPlaceOrder
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
    }

    private fun placeOuterCoordinator(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        layer: GraphicsLayer?,
    ) {
        requirePrecondition(!layoutNode.isDeactivated) { "place is called on a deactivated node" }
        layoutState = LayoutState.LayingOut

        lastPosition = position
        lastZIndex = zIndex
        lastLayerBlock = layerBlock
        lastExplicitLayer = layer
        onNodePlacedCalled = false

        val owner = layoutNode.requireOwner()
        if (!layoutPending && isPlaced) {
            outerCoordinator.placeSelfApparentToRealOffset(position, zIndex, layerBlock, layer)
            onNodePlaced()
        } else {
            alignmentLines.usedByModifierLayout = false
            layoutNodeLayoutDelegate.coordinatesAccessedDuringModifierPlacement = false
            placeOuterCoordinatorLayerBlock = layerBlock
            placeOuterCoordinatorPosition = position
            placeOuterCoordinatorZIndex = zIndex
            placeOuterCoordinatorLayer = layer
            owner.snapshotObserver.observeLayoutModifierSnapshotReads(
                layoutNode,
                block = placeOuterCoordinatorBlock,
            )
        }

        layoutState = LayoutState.Idle
        placedOnce = true
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
        } catch (e: Throwable) {
            layoutNode.rethrowWithComposeStackTrace(e)
        } finally {
            relayoutWithoutParentInProgress = false
        }
    }

    override fun minIntrinsicWidth(height: Int): Int {
        // If there is an intrinsic size query coming from above the lookahead root, we will
        // direct the query down to the lookahead pass. Note, when a regular measure call
        // reaches a top-level lookahead root, the measure call is turned into lookahead
        // measure followed by approach measure. This is a similar, although not exactly the
        // same, mental model.
        if (layoutNode.isOutMostLookaheadRoot) {
            return lookaheadPassDelegate!!.minIntrinsicWidth(height)
        }
        onIntrinsicsQueried()
        return outerCoordinator.minIntrinsicWidth(height)
    }

    override fun maxIntrinsicWidth(height: Int): Int {
        // If there is an intrinsic size query coming from above the lookahead root, we will
        // direct the query down to the lookahead pass. Note, when a regular measure call
        // reaches a top-level lookahead root, the measure call is turned into lookahead
        // measure followed by approach measure. This is a similar, although not exactly the
        // same, mental model.
        if (layoutNode.isOutMostLookaheadRoot) {
            return lookaheadPassDelegate!!.maxIntrinsicWidth(height)
        }
        onIntrinsicsQueried()
        return outerCoordinator.maxIntrinsicWidth(height)
    }

    override fun minIntrinsicHeight(width: Int): Int {
        // If there is an intrinsic size query coming from above the lookahead root, we will
        // direct the query down to the lookahead pass. Note, when a regular measure call
        // reaches a top-level lookahead root, the measure call is turned into lookahead
        // measure followed by approach measure. This is a similar, although not exactly the
        // same, mental model.
        if (layoutNode.isOutMostLookaheadRoot) {
            return lookaheadPassDelegate!!.minIntrinsicHeight(width)
        }
        onIntrinsicsQueried()
        return outerCoordinator.minIntrinsicHeight(width)
    }

    override fun maxIntrinsicHeight(width: Int): Int {
        // If there is an intrinsic size query coming from above the lookahead root, we will
        // direct the query down to the lookahead pass. Note, when a regular measure call
        // reaches a top-level lookahead root, the measure call is turned into lookahead
        // measure followed by approach measure. This is a similar, although not exactly the
        // same, mental model.
        if (layoutNode.isOutMostLookaheadRoot) {
            return lookaheadPassDelegate!!.maxIntrinsicHeight(width)
        }
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
            parent != null && layoutNode.intrinsicsUsageByParent == LayoutNode.UsageByParent.NotUsed
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

    override fun calculateAlignmentLines(): Map<AlignmentLine, Int> {
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
     * pass. If any child is looking at their own coordinates to know how to place children, it will
     * be invalided.
     *
     * Note that this is called for every changed position. While not many layouts look at their
     * coordinates, if there is one, it will cause all position changes from an ancestor to call
     * down the hierarchy. If this becomes expensive (e.g. many parents change their position on the
     * same frame), it might be worth using a flag so that this call becomes cheap after the first
     * one.
     */
    fun notifyChildrenUsingCoordinatesWhilePlacing() {
        if (layoutNodeLayoutDelegate.childrenAccessingCoordinatesDuringPlacement > 0) {
            layoutNode.forEachChild { child ->
                val childLayoutDelegate = child.layoutDelegate
                val accessed =
                    childLayoutDelegate.coordinatesAccessedDuringPlacement ||
                        childLayoutDelegate.coordinatesAccessedDuringModifierPlacement
                if (accessed && !childLayoutDelegate.layoutPending) {
                    child.requestRelayout()
                }
                childLayoutDelegate.measurePassDelegate.notifyChildrenUsingCoordinatesWhilePlacing()
            }
        }
    }

    /**
     * The callback to be executed before running layoutChildren.
     *
     * There are possible cases when we run layoutChildren() on the parent node, but some of its
     * children are not yet measured even if they are supposed to be measured in the measure block
     * of our parent.
     *
     * Example: val child = Layout(...) Layout(child) { measurable, constraints -> val placeable =
     * measurable.first().measure(constraints) layout(placeable.width, placeable.height) {
     * placeable.place(0, 0) } } And now some set of changes scheduled remeasure for child and
     * relayout for parent.
     *
     * During the [MeasureAndLayoutDelegate.measureAndLayout] we will start with the parent as it
     * has lower depth. Inside the layout block we will call placeable.width which is currently
     * dirty as the child was scheduled to remeasure. This callback will ensure it never happens and
     * pre-remeasure everything required for this layoutChildren().
     */
    private fun onBeforeLayoutChildren() {
        layoutNode.forEachChild {
            if (
                it.measurePending && it.measuredByParent == LayoutNode.UsageByParent.InMeasureBlock
            ) {
                if (it.remeasure()) {
                    layoutNode.requestRemeasure()
                }
            }
        }
    }

    /**
     * If this was used in an intrinsics measurement, find the parent that used it and invalidate
     * either the measure block or layout block.
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

    fun markLayoutPending() {
        layoutPending = true
        layoutPendingForAlignment = true
    }

    /** Marks the layoutNode dirty for another measure pass. */
    internal fun markMeasurePending() {
        measurePending = true
    }
}
