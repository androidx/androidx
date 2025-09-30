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
import androidx.compose.ui.util.fastForEach

/**
 * [LookaheadPassDelegate] manages the measure/layout and alignmentLine related queries for the
 * lookahead pass.
 */
internal class LookaheadPassDelegate(
    private val layoutNodeLayoutDelegate: LayoutNodeLayoutDelegate
) : Placeable(), Measurable, AlignmentLinesOwner, MotionReferencePlacementDelegate {

    private enum class PlacedState {
        IsPlacedInLookahead,
        IsPlacedInApproach,
        IsNotPlaced,
    }

    /**
     * Tracks whether another lookahead measure pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [measurePending] is confined to LookaheadPassDelegate. It can only be set true
     * from outside of this class via [markMeasurePending]. It is cleared (i.e. set false) during
     * the lookahead measure pass (i.e. in [performMeasure]).
     */
    // TODO: Make LookaheadPassDelegate#measure/layoutPending as the source of truth rather than
    // the other way around (i.e. relying on [LayoutNodeLayoutDelegate]) once b/384579646 is fixed.
    private var measurePending: Boolean
        private set(value) {
            layoutNodeLayoutDelegate.lookaheadMeasurePending = value
        }
        get() = layoutNodeLayoutDelegate.lookaheadMeasurePending

    /**
     * Tracks whether another lookahead layout pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [layoutPending] is confined to this class. It can only be set true from outside
     * of this class via [markLayoutPending]. It is cleared (i.e. set false) during the layout pass
     * (i.e. in [LookaheadPassDelegate.layoutChildren]).
     */
    private var layoutPending: Boolean
        private set(value) {
            layoutNodeLayoutDelegate.lookaheadLayoutPending = value
        }
        get() = layoutNodeLayoutDelegate.lookaheadLayoutPending

    /**
     * Tracks whether another lookahead layout pass is needed for the LayoutNodeLayoutDelegate for
     * the purposes of calculating alignment lines. After calculating alignment lines, if the
     * [Placeable.PlacementScope.coordinates] have been accessed, there is no need to rerun layout
     * for further alignment lines checks, but [layoutPending] will indicate that the normal
     * placement still needs to be run.
     */
    private var layoutPendingForAlignment: Boolean
        private set(value) {
            layoutNodeLayoutDelegate.lookaheadLayoutPendingForAlignment = value
        }
        get() = layoutNodeLayoutDelegate.lookaheadLayoutPendingForAlignment

    private val layoutNode: LayoutNode
        get() = layoutNodeLayoutDelegate.layoutNode

    internal fun markLayoutPending() {
        layoutPending = true
        layoutPendingForAlignment = true
    }

    /** Marks the layoutNode dirty for another lookahead measure pass. */
    internal fun markMeasurePending() {
        measurePending = true
    }

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
    private var previousPlaceOrder: Int = NotPlacedPlaceOrder

    /**
     * The order in which this node was placed by its parent during the previous `layoutChildren`.
     * Before the placement the order is set to [NotPlacedPlaceOrder] to all the children. Then
     * every placed node assigns this variable to parent's LayoutNodeLayoutDelegate's
     * nextChildLookaheadPlaceOrder and increments this counter. Not placed items will still have
     * [NotPlacedPlaceOrder] set.
     */
    override var placeOrder: Int = NotPlacedPlaceOrder
        internal set

    internal var measuredByParent = LayoutNode.UsageByParent.NotUsed
    internal val measurePassDelegate: MeasurePassDelegate
        get() = layoutNodeLayoutDelegate.measurePassDelegate

    private val outerCoordinator
        get() = layoutNodeLayoutDelegate.outerCoordinator

    private var layoutState
        set(value) {
            layoutNodeLayoutDelegate.layoutState = value
        }
        get() = layoutNodeLayoutDelegate.layoutState

    private var duringAlignmentLinesQuery: Boolean = false
    internal var placedOnce: Boolean = false
    private var measuredOnce: Boolean = false
    val lastConstraints: Constraints?
        get() = lookaheadConstraints

    private var lookaheadConstraints: Constraints? = null
    private var lastPosition: IntOffset = IntOffset.Zero

    private var lastZIndex: Float = 0f

    private var lastLayerBlock: (GraphicsLayerScope.() -> Unit)? = null

    private var lastExplicitLayer: GraphicsLayer? = null

    internal val isPlaced: Boolean
        get() = _placedState != PlacedState.IsNotPlaced

    private var _placedState: PlacedState = PlacedState.IsNotPlaced

    override val innerCoordinator: NodeCoordinator
        get() = layoutNode.innerCoordinator

    override val alignmentLines: AlignmentLines = LookaheadAlignmentLines(this)

    private val _childDelegates = MutableVector<LookaheadPassDelegate>()

    /**
     * This property indicates whether the lookahead pass delegate needs to be placed in the
     * approach pass. In the vast majority cases LookaheadPassDelegate is placed in the lookahead
     * pass. The two scenarios that leads to it being placed in approach are:
     * 1) The layout node is a lookahead root. The lookahead pass therefore has to be initiated by
     *    the node itself (rather than participating in parent's lookahead pass).
     * 2) When the parent skips the child in its lookahead placement. This can happen when the
     *    parent's placement logic intentionally skips placing the child during lookahead after
     *    measuring it.
     */
    val needsToBePlacedInApproach: Boolean
        get() =
            if (layoutNode.isOutMostLookaheadRoot) {
                true
            } else {
                if (
                    _placedState == PlacedState.IsNotPlaced &&
                        !layoutNodeLayoutDelegate.detachedFromParentLookaheadPass
                ) {
                    // Never placed in lookahead. Since this node is not completely detached
                    // from
                    // parent's lookahead pass (i.e. properly measured during parent's
                    // lookahead),
                    // mark only placement detached from lookahead.
                    layoutNodeLayoutDelegate.detachedFromParentLookaheadPlacement = true
                }
                detachedFromParentLookaheadPlacement
            }

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

    private val layoutChildrenBlock = {
        clearPlaceOrder()
        forEachChildAlignmentLinesOwner { child ->
            child.alignmentLines.usedDuringParentLayout = false
        }
        innerCoordinator.lookaheadDelegate?.isPlacingForAlignment?.let { forAlignment ->
            layoutNode.children.fastForEach {
                it.outerCoordinator.lookaheadDelegate?.isPlacingForAlignment = forAlignment
            }
        }
        innerCoordinator.lookaheadDelegate!!.measureResult.placeChildren()
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

    override fun layoutChildren() {
        layingOutChildren = true
        alignmentLines.recalculateQueryOwner()

        if (layoutPending) {
            onBeforeLayoutChildren()
        }
        val lookaheadDelegate = innerCoordinator.lookaheadDelegate!!
        // as a result of the previous operation we can figure out a child has been resized
        // and we need to be remeasured, not relaid out
        if (
            layoutPendingForAlignment ||
                (!duringAlignmentLinesQuery &&
                    !lookaheadDelegate.isPlacingForAlignment &&
                    layoutPending)
        ) {
            layoutPending = false
            val oldLayoutState = layoutState
            layoutState = LayoutState.LookaheadLayingOut
            layoutNodeLayoutDelegate.lookaheadCoordinatesAccessedDuringPlacement = false

            val observer = layoutNode.requireOwner().snapshotObserver
            observer.observeLayoutSnapshotReadsAffectingLookahead(layoutNode, layoutChildrenBlock)
            layoutState = oldLayoutState
            if (
                layoutNodeLayoutDelegate.lookaheadCoordinatesAccessedDuringPlacement &&
                    lookaheadDelegate.isPlacingForAlignment
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

    private val detachedFromParentLookaheadPlacement
        get() = layoutNodeLayoutDelegate.detachedFromParentLookaheadPlacement

    private fun checkChildrenPlaceOrderForUpdates() {
        forEachChildDelegate { child ->
            // we set `placeOrder` to NotPlacedPlaceOrder for all the children, then
            // during the placeChildren() invocation the real order will be assigned for
            // all the placed children.
            if (child.previousPlaceOrder != child.placeOrder) {
                if (child.placeOrder == NotPlacedPlaceOrder) {
                    child.markNodeAndSubtreeAsNotPlaced(inLookahead = true)
                }
            }
        }
    }

    /**
     * Marks the subtree as not placed. When hitting a node that is detached from parent's lookahead
     * placement, both the node and subtree is skipped in the recursion. Their placedState will be
     * updated during approach.
     *
     * Note: This is not perfectly symmetric to markNodeAndSubtreeAsPlaced, because to consider a
     * node placed, we only need one confirmation of placing the node. In contrast, to say a node is
     * not placed, we need both passes to not place it.
     */
    internal fun markNodeAndSubtreeAsNotPlaced(inLookahead: Boolean) {
        if (
            (inLookahead && detachedFromParentLookaheadPlacement) ||
                (!inLookahead && !detachedFromParentLookaheadPlacement)
        ) {
            // Not in the right pass. No-op
            return
        }

        _placedState = PlacedState.IsNotPlaced

        // If the node is detached from parent's lookahead placement, it means it gets placed
        // during approach. We therefore don't take placement signals from parent in
        // lookahead pass, but defer that until approach pass.
        // Note: when we propagate this call downward in the tree, we mark `inLookahead` as
        // true, because from the children's perspective this call does indeed come from
        // their parent's lookahead, even though parent's lookahead is triggered by its
        // parent's approach.
        forEachChildDelegate { it.markNodeAndSubtreeAsNotPlaced(true) }
    }

    override fun calculateAlignmentLines(): Map<AlignmentLine, Int> {
        if (!duringAlignmentLinesQuery) {
            if (layoutState == LayoutState.LookaheadMeasuring) {
                // Mark alignments used by modifier
                alignmentLines.usedByModifierMeasurement = true
                // We quickly transition to layoutPending as we need the alignment lines now.
                // Later we will see that we also laid out as part of measurement and will skip
                // layout.
                if (alignmentLines.dirty) layoutNodeLayoutDelegate.markLookaheadLayoutPending()
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
     * This is called any time a placement has done that changes the position during the lookahead
     * layout pass. If any child is looking at their own coordinates to know how to place children,
     * it will be invalided.
     *
     * Note that this is called for every changed position. While not many layouts look at their
     * coordinates, if there is one, it will cause all position changes from an ancestor to call
     * down the hierarchy. If this becomes expensive (e.g. many parents change their position on the
     * same frame), it might be worth using a flag so that this call becomes cheap after the first
     * one.
     */
    fun notifyChildrenUsingLookaheadCoordinatesWhilePlacing() {
        if (layoutNodeLayoutDelegate.childrenAccessingLookaheadCoordinatesDuringPlacement > 0) {
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
            layoutNodeLayoutDelegate.detachedFromParentLookaheadPass = false
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

    // Used by performMeasureBlock so that we don't have to allocate a lambda on every call
    private var performMeasureConstraints = Constraints()

    internal val performMeasureBlock: () -> Unit = {
        outerCoordinator.lookaheadDelegate!!.measure(performMeasureConstraints)
    }

    internal fun performMeasure(constraints: Constraints) {
        layoutState = LayoutState.LookaheadMeasuring
        measurePending = false
        performMeasureConstraints = constraints
        val observer = layoutNode.requireOwner().snapshotObserver
        observer.observeMeasureSnapshotReadsAffectingLookahead(layoutNode, performMeasureBlock)
        markLayoutPending()
        if (layoutNode.isOutMostLookaheadRoot) {
            // If layoutNode is the root of the lookahead, measure is redirected to lookahead
            // measure, and layout pass will begin lookahead placement, measure & layout.
            measurePassDelegate.markLayoutPending()
        } else {
            // If layoutNode is not the root of the lookahead, measure needs to follow the
            // lookahead measure.
            measurePassDelegate.markMeasurePending()
        }
        layoutState = LayoutState.Idle
    }

    // Lookahead remeasurement with the given constraints.
    fun remeasure(constraints: Constraints): Boolean {
        withComposeStackTrace(layoutNode) {
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

                layoutNodeLayoutDelegate.performLookaheadMeasure(constraints)
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

    override var isPlacedUnderMotionFrameOfReference: Boolean = false

    override fun updatePlacedUnderMotionFrameOfReference(newMFR: Boolean) {
        // Delegated to outerCoordinator
        val old = outerCoordinator.lookaheadDelegate?.isPlacedUnderMotionFrameOfReference
        if (newMFR != old) {
            outerCoordinator.lookaheadDelegate?.isPlacedUnderMotionFrameOfReference = newMFR
        }
        isPlacedUnderMotionFrameOfReference = newMFR
    }

    private val layoutModifierBlock = {
        val expectsLookaheadPlacementFromParent =
            !layoutNode.isOutMostLookaheadRoot &&
                !layoutNodeLayoutDelegate.detachedFromParentLookaheadPlacement

        val scope =
            if (expectsLookaheadPlacementFromParent) {
                outerCoordinator.wrappedBy?.lookaheadDelegate?.placementScope
            } else {
                // Uses the approach pass placement scope intentionally here when
                // the
                // lookahead placement is detached from parent. This way we will
                // be able to pick up the correct `withMotionFrameOfReference` flag
                // from the placement scope.
                outerCoordinator.wrappedBy?.placementScope
            } ?: layoutNode.requireOwner().placementScope
        with(scope) { outerCoordinator.lookaheadDelegate!!.place(lastPosition) }
    }

    private fun placeSelf(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        layer: GraphicsLayer?,
    ) {
        withComposeStackTrace(layoutNode) {
            if (layoutNode.parent?.layoutState == LayoutState.LookaheadLayingOut) {
                // This placement call comes from parent
                layoutNodeLayoutDelegate.detachedFromParentLookaheadPlacement = false
            }
            requirePrecondition(!layoutNode.isDeactivated) {
                "place is called on a deactivated node"
            }
            layoutState = LayoutState.LookaheadLayingOut
            placedOnce = true
            onNodePlacedCalled = false
            if (position != lastPosition) {
                if (
                    layoutNodeLayoutDelegate.lookaheadCoordinatesAccessedDuringModifierPlacement ||
                        layoutNodeLayoutDelegate.lookaheadCoordinatesAccessedDuringPlacement
                ) {
                    layoutPending = true
                }
                notifyChildrenUsingLookaheadCoordinatesWhilePlacing()
            }
            val owner = layoutNode.requireOwner()

            lastPosition = position
            if (!layoutPending && isPlaced) {
                outerCoordinator.lookaheadDelegate!!.placeSelfApparentToRealOffset(position)
                onNodePlaced()
            } else {
                layoutNodeLayoutDelegate.lookaheadCoordinatesAccessedDuringModifierPlacement = false
                alignmentLines.usedByModifierLayout = false
                owner.snapshotObserver.observeLayoutModifierSnapshotReadsAffectingLookahead(
                    layoutNode,
                    layoutModifierBlock,
                )
            }
            lastZIndex = zIndex
            lastLayerBlock = layerBlock
            lastExplicitLayer = layer
            layoutState = LayoutState.Idle
        }
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
        if (
            (_placedState != PlacedState.IsPlacedInLookahead &&
                !detachedFromParentLookaheadPlacement) ||
                (_placedState != PlacedState.IsPlacedInApproach &&
                    detachedFromParentLookaheadPlacement)
        ) {
            // Needs to update _placedState
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
        layoutNodeLayoutDelegate.nextChildLookaheadPlaceOrder = 0
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

    // This gets called after the node has been placed. The call comes from:
    // 1) InnerCoordinator, after all the modifiers in the chain has been placed.
    // or 2) placeSelf, if the modifier chain doesn't need to be replaced.
    // Note: This method only marks subtree nodes that have a `placeOrder` i.e. nodes that
    // are placed in lookahead pass.
    private fun markNodeAndSubtreeAsPlaced() {
        val prevPlacedState = _placedState
        // Update the _placedState based on whether the placement is a part of approach or
        // lookahead.
        if (detachedFromParentLookaheadPlacement) {
            _placedState = PlacedState.IsPlacedInApproach
        } else {
            _placedState = PlacedState.IsPlacedInLookahead
        }
        if (prevPlacedState != PlacedState.IsPlacedInLookahead) {
            if (layoutNodeLayoutDelegate.lookaheadMeasurePending) {
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
        _placedState = PlacedState.IsNotPlaced
    }

    fun onAttachedToNullParent() {
        // it is a root node and attached root nodes are always placed (as there is no parent
        // to place them explicitly)
        _placedState = PlacedState.IsPlacedInLookahead
    }
}
