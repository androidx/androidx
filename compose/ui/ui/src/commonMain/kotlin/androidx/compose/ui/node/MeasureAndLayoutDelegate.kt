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

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.node.LayoutNode.LayoutState.Idle
import androidx.compose.ui.node.LayoutNode.LayoutState.LayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.LookaheadLayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.LookaheadMeasuring
import androidx.compose.ui.node.LayoutNode.LayoutState.Measuring
import androidx.compose.ui.node.LayoutNode.UsageByParent.InLayoutBlock
import androidx.compose.ui.node.LayoutNode.UsageByParent.InMeasureBlock
import androidx.compose.ui.unit.Constraints

/**
 * Keeps track of [LayoutNode]s which needs to be remeasured or relaid out.
 *
 * Use [requestRemeasure] to schedule remeasuring or [requestRelayout] to schedule relayout.
 *
 * Use [measureAndLayout] to perform scheduled actions and [dispatchOnPositionedCallbacks] to
 * dispatch [OnGloballyPositionedModifier] callbacks for the nodes affected by the previous
 * [measureAndLayout] execution.
 */
internal class MeasureAndLayoutDelegate(private val root: LayoutNode) {
    /** LayoutNodes that need measure or layout. */
    private val relayoutNodes = DepthSortedSetsForDifferentPasses(Owner.enableExtraAssertions)

    /** Whether any LayoutNode needs measure or layout. */
    val hasPendingMeasureOrLayout
        get() = relayoutNodes.isNotEmpty()

    /** Whether any on positioned callbacks need to be dispatched */
    val hasPendingOnPositionedCallbacks
        get() = onPositionedDispatcher.isNotEmpty()

    /** Flag to indicate that we're currently measuring. */
    internal var duringMeasureLayout = false
    /**
     * True when we are currently executing a full measure/layout pass, which mean we will iterate
     * through all the nodes in [relayoutNodes].
     */
    private var duringFullMeasureLayoutPass = false

    /** Dispatches on positioned callbacks. */
    private val onPositionedDispatcher = OnPositionedDispatcher()

    /** List of listeners that must be called after layout has completed. */
    private val onLayoutCompletedListeners = mutableVectorOf<Owner.OnLayoutCompletedListener>()

    /**
     * The current measure iteration. The value is incremented during the [measureAndLayout]
     * execution. Some [measureAndLayout] executions will increment it more than once.
     */
    var measureIteration: Long = 1L
        get() {
            requirePrecondition(duringMeasureLayout) {
                "measureIteration should be only used during the measure/layout pass"
            }
            return field
        }
        private set

    /**
     * Stores the list of [LayoutNode]s scheduled to be remeasured in the next measure/layout pass.
     * We were unable to mark them as needsRemeasure=true previously as this request happened during
     * the previous measure/layout pass and they were already measured as part of it. See
     * [requestRemeasure] for more details.
     */
    private val postponedMeasureRequests = mutableVectorOf<PostponedRequest>()

    private var rootConstraints: Constraints? = null

    /** @param constraints The constraints to measure the root [LayoutNode] with */
    fun updateRootConstraints(constraints: Constraints) {
        if (rootConstraints != constraints) {
            requirePrecondition(!duringMeasureLayout) {
                "updateRootConstraints called while measuring"
            }
            rootConstraints = constraints
            if (root.lookaheadRoot != null) {
                root.markLookaheadMeasurePending()
            }
            root.markMeasurePending()
            relayoutNodes.add(root, root.lookaheadRoot != null)
        }
    }

    private val consistencyChecker: LayoutTreeConsistencyChecker? =
        if (Owner.enableExtraAssertions) {
            LayoutTreeConsistencyChecker(
                root,
                relayoutNodes,
                postponedMeasureRequests.asMutableList(),
            )
        } else {
            null
        }

    /**
     * Requests lookahead remeasure for this [layoutNode] and nodes affected by its measure result
     *
     * Note: This should only be called on a [LayoutNode] in the subtree defined in a
     * LookaheadScope. The caller is responsible for checking with [LayoutNode.lookaheadRoot] is
     * valid (i.e. non-null) before calling this method.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result of the
     *   request.
     */
    fun requestLookaheadRemeasure(layoutNode: LayoutNode, forced: Boolean = false): Boolean {
        checkPrecondition(layoutNode.lookaheadRoot != null) {
            "Error: requestLookaheadRemeasure cannot be called on a node outside" +
                " LookaheadScope"
        }
        return when (layoutNode.layoutState) {
            LookaheadMeasuring -> {
                // requestLookaheadRemeasure has already been called for this node or
                // we're currently measuring it, let's swallow.
                false
            }
            Measuring,
            LookaheadLayingOut,
            LayingOut -> {
                // requestLookaheadRemeasure is currently laying out and it is incorrect to
                // request lookahead remeasure now, let's postpone it.
                postponedMeasureRequests.add(
                    PostponedRequest(node = layoutNode, isLookahead = true, isForced = forced)
                )
                consistencyChecker?.assertConsistent()
                false
            }
            Idle -> {
                if (layoutNode.lookaheadMeasurePending && !forced) {
                    false
                } else {
                    layoutNode.markLookaheadMeasurePending()
                    layoutNode.markMeasurePending()
                    // for the deactivated nodes we want to mark them as dirty, but not to trigger
                    // measureAndLayout() pass as they will be skipped.
                    if (layoutNode.isDeactivated) {
                        false
                    } else {
                        if (
                            (layoutNode.isPlacedInLookahead == true ||
                                layoutNode.canAffectParentInLookahead) &&
                                layoutNode.parent?.lookaheadMeasurePending != true
                        ) {
                            relayoutNodes.add(layoutNode, true)
                        } else if (
                            (layoutNode.isPlaced || layoutNode.canAffectParent) &&
                                layoutNode.parent?.measurePending != true
                        ) {
                            relayoutNodes.add(layoutNode, false)
                        }
                        !duringFullMeasureLayoutPass
                    }
                }
            }
        }
    }

    /**
     * Requests remeasure for this [layoutNode] and nodes affected by its measure result.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result of the
     *   request.
     */
    fun requestRemeasure(layoutNode: LayoutNode, forced: Boolean = false): Boolean =
        when (layoutNode.layoutState) {
            Measuring,
            LookaheadMeasuring -> {
                // requestMeasure has already been called for this node or
                // we're currently measuring it, let's swallow. example when it happens: we compose
                // DataNode inside BoxWithConstraints, this calls onRequestMeasure on DataNode's
                // parent, but this parent is BoxWithConstraints which is currently measuring.
                false
            }
            LookaheadLayingOut,
            LayingOut -> {
                // requestMeasure is currently laying out and it is incorrect to request remeasure
                // now, let's postpone it.
                postponedMeasureRequests.add(
                    PostponedRequest(node = layoutNode, isLookahead = false, isForced = forced)
                )
                consistencyChecker?.assertConsistent()
                false
            }
            Idle -> {
                if (layoutNode.measurePending && !forced) {
                    false
                } else {
                    layoutNode.markMeasurePending()
                    // for the deactivated nodes we want to mark them as dirty, but not to trigger
                    // measureAndLayout() pass as they will be skipped.
                    if (layoutNode.isDeactivated) {
                        false
                    } else {
                        if (layoutNode.isPlaced || layoutNode.canAffectParent) {
                            if (layoutNode.parent?.measurePending != true) {
                                relayoutNodes.add(layoutNode, false)
                            }
                            !duringFullMeasureLayoutPass
                        } else {
                            false // it can't affect parent
                        }
                    }
                }
            }
        }

    /**
     * Requests lookahead relayout for this [layoutNode] and nodes affected by its position.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result of the
     *   request.
     */
    fun requestLookaheadRelayout(layoutNode: LayoutNode, forced: Boolean = false): Boolean =
        when (layoutNode.layoutState) {
            LookaheadMeasuring,
            LookaheadLayingOut -> {
                // Don't need to do anything else since the parent is already scheduled
                // for a lookahead relayout (lookahead measure will trigger lookahead
                // relayout), or lookahead layout is in process right now
                consistencyChecker?.assertConsistent()
                false
            }
            Measuring,
            LayingOut,
            Idle -> {
                if (
                    (layoutNode.lookaheadMeasurePending || layoutNode.lookaheadLayoutPending) &&
                        !forced
                ) {
                    // Don't need to do anything else since the parent is already scheduled
                    // for a lookahead relayout (lookahead measure will trigger lookahead
                    // relayout)
                    consistencyChecker?.assertConsistent()
                    false
                } else {
                    // Mark both lookahead layout and layout as pending, as layout has a
                    // dependency on lookahead layout.
                    layoutNode.markLookaheadLayoutPending()
                    layoutNode.markLayoutPending()
                    // for the deactivated nodes we want to mark them as dirty, but not to trigger
                    // measureAndLayout() pass as they will be skipped.
                    if (layoutNode.isDeactivated) {
                        false
                    } else {
                        val parent = layoutNode.parent
                        if (
                            layoutNode.isPlacedInLookahead == true &&
                                parent?.lookaheadMeasurePending != true &&
                                parent?.lookaheadLayoutPending != true
                        ) {
                            relayoutNodes.add(layoutNode, true)
                        } else if (
                            layoutNode.isPlaced &&
                                parent?.layoutPending != true &&
                                parent?.measurePending != true
                        ) {
                            relayoutNodes.add(layoutNode, false)
                        }
                        !duringFullMeasureLayoutPass
                    }
                }
            }
        }

    /**
     * Requests relayout for this [layoutNode] and nodes affected by its position.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result of the
     *   request.
     */
    fun requestRelayout(layoutNode: LayoutNode, forced: Boolean = false): Boolean =
        when (layoutNode.layoutState) {
            Measuring,
            LookaheadMeasuring,
            LookaheadLayingOut,
            LayingOut -> {
                // don't need to do anything else since the parent is already scheduled
                // for a relayout (measure will trigger relayout), or is laying out right now
                consistencyChecker?.assertConsistent()
                false
            }
            Idle -> {
                if (
                    !forced &&
                        (layoutNode.isPlaced == layoutNode.isPlacedByParent) &&
                        (layoutNode.measurePending || layoutNode.layoutPending)
                ) {
                    // don't need to do anything else since the parent is already scheduled
                    // for a relayout (measure will trigger relayout), or is laying out right now
                    consistencyChecker?.assertConsistent()
                    false
                } else {
                    layoutNode.markLayoutPending()
                    // for the deactivated nodes we want to mark them as dirty, but not to trigger
                    // measureAndLayout() pass as they will be skipped.
                    if (layoutNode.isDeactivated) {
                        false
                    } else {
                        if (layoutNode.isPlacedByParent) {
                            val parent = layoutNode.parent
                            if (parent?.layoutPending != true && parent?.measurePending != true) {
                                relayoutNodes.add(layoutNode, false)
                            }
                            !duringFullMeasureLayoutPass
                        } else {
                            false // the node can't affect parent
                        }
                    }
                }
            }
        }

    /** Request that [layoutNode] and children should call their position change callbacks. */
    fun requestOnPositionedCallback(layoutNode: LayoutNode) {
        onPositionedDispatcher.onNodePositioned(layoutNode)
    }

    /** @return true if the [LayoutNode] size has been changed. */
    private fun doLookaheadRemeasure(layoutNode: LayoutNode, constraints: Constraints?): Boolean {
        if (layoutNode.lookaheadRoot == null) return false
        val lookaheadSizeChanged =
            if (constraints != null) {
                layoutNode.lookaheadRemeasure(constraints)
            } else {
                layoutNode.lookaheadRemeasure()
            }

        val parent = layoutNode.parent
        if (lookaheadSizeChanged && parent != null) {
            if (parent.lookaheadRoot == null) {
                parent.requestRemeasure(invalidateIntrinsics = false)
            } else if (layoutNode.measuredByParentInLookahead == InMeasureBlock) {
                parent.requestLookaheadRemeasure(invalidateIntrinsics = false)
            } else if (layoutNode.measuredByParentInLookahead == InLayoutBlock) {
                parent.requestLookaheadRelayout()
            }
        }
        return lookaheadSizeChanged
    }

    private fun doRemeasure(layoutNode: LayoutNode, constraints: Constraints?): Boolean {
        val sizeChanged =
            if (constraints != null) {
                layoutNode.remeasure(constraints)
            } else {
                layoutNode.remeasure()
            }
        val parent = layoutNode.parent
        if (sizeChanged && parent != null) {
            if (layoutNode.measuredByParent == InMeasureBlock) {
                parent.requestRemeasure(invalidateIntrinsics = false)
            } else if (layoutNode.measuredByParent == InLayoutBlock) {
                parent.requestRelayout()
            }
        }
        return sizeChanged
    }

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out
     */
    fun measureAndLayout(onLayout: (() -> Unit)? = null): Boolean {
        var rootNodeResized = false
        performMeasureAndLayout(fullPass = true) {
            if (relayoutNodes.isNotEmpty()) {
                relayoutNodes.popEach { layoutNode, affectsLookahead ->
                    val sizeChanged = remeasureAndRelayoutIfNeeded(layoutNode, affectsLookahead)
                    if (layoutNode === root && sizeChanged) {
                        rootNodeResized = true
                    }
                }
                onLayout?.invoke()
            }
        }
        callOnLayoutCompletedListeners()
        return rootNodeResized
    }

    /**
     * Only does measurement from the root without doing any placement. This is intended to be
     * called to determine only how large the root is with minimal effort.
     */
    fun measureOnly() {
        if (relayoutNodes.isNotEmpty()) {
            performMeasureAndLayout(fullPass = false) {
                if (!relayoutNodes.isEmpty(affectsLookahead = true)) {
                    if (root.lookaheadRoot != null) {
                        // This call will walk the tree to look for lookaheadMeasurePending nodes
                        // and
                        // do a lookahead remeasure for those nodes only.
                        remeasureOnly(root, affectsLookahead = true)
                    } else {
                        // First do a lookahead remeasure pass for all the lookaheadMeasurePending
                        // nodes,
                        // followed by a remeasure pass for the rest of the tree.
                        remeasureLookaheadRootsInSubtree(root)
                    }
                }
                remeasureOnly(root, affectsLookahead = false)
            }
        }
    }

    private fun remeasureLookaheadRootsInSubtree(layoutNode: LayoutNode) {
        layoutNode.forEachChild {
            if (it.measureAffectsParent) {
                if (it.isOutMostLookaheadRoot()) {
                    // This call will walk the subtree to look for lookaheadMeasurePending nodes and
                    // do a recursive lookahead remeasure starting at the root.
                    remeasureOnly(it, affectsLookahead = true)
                } else {
                    // Only search downward when no lookahead root is found
                    remeasureLookaheadRootsInSubtree(it)
                }
            }
        }
    }

    fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
        if (layoutNode.isDeactivated) {
            // regular measureAndLayout() pass will skip deactivated nodes, so here we should
            // do nothing as well.
            return
        }
        requirePrecondition(layoutNode != root) { "measureAndLayout called on root" }
        performMeasureAndLayout(fullPass = false) {
            relayoutNodes.remove(layoutNode)
            // we don't check for the layoutState as even if the node doesn't need remeasure
            // it could be remeasured because the constraints changed.
            val lookaheadSizeChanged = doLookaheadRemeasure(layoutNode, constraints)
            if (
                (lookaheadSizeChanged || layoutNode.lookaheadLayoutPending) &&
                    layoutNode.isPlacedInLookahead == true
            ) {
                layoutNode.lookaheadReplace()
            }
            // Make sure the subtree starting from [layoutNode] are lookahead replaced. This is
            // needed because the child nodes that are skipped in `lookaheadReplace` above
            // due to not changing position may not be skipped by the `replace` call below. Hence
            // we can avoid having `replace` called for nodes that have not been lookahead placed.
            ensureSubtreeLookaheadReplaced(layoutNode)

            doRemeasure(layoutNode, constraints)
            if (layoutNode.layoutPending && layoutNode.isPlaced) {
                layoutNode.replace()
                onPositionedDispatcher.onNodePositioned(layoutNode)
            }

            drainPostponedMeasureRequests()
        }
        callOnLayoutCompletedListeners()
    }

    private fun ensureSubtreeLookaheadReplaced(layoutNode: LayoutNode) {
        layoutNode.forEachChild {
            if (it.isPlacedInLookahead == true && !it.isDeactivated) {
                if (relayoutNodes.contains(it, true)) {
                    // Only replace if invalidation pending
                    it.lookaheadReplace()
                }
                ensureSubtreeLookaheadReplaced(it)
            }
        }
    }

    private inline fun performMeasureAndLayout(fullPass: Boolean, block: () -> Unit) {
        requirePrecondition(root.isAttached) {
            "performMeasureAndLayout called with unattached root"
        }
        requirePrecondition(root.isPlaced) { "performMeasureAndLayout called with unplaced root" }
        requirePrecondition(!duringMeasureLayout) {
            "performMeasureAndLayout called during measure layout"
        }
        // we don't need to measure any children unless we have the correct root constraints
        if (rootConstraints != null) {
            duringMeasureLayout = true
            duringFullMeasureLayoutPass = fullPass
            try {
                block()
            } finally {
                duringMeasureLayout = false
                duringFullMeasureLayoutPass = false
            }
            consistencyChecker?.assertConsistent()
        }
    }

    fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        onLayoutCompletedListeners += listener
    }

    private fun callOnLayoutCompletedListeners() {
        onLayoutCompletedListeners.forEach { it.onLayoutComplete() }
        onLayoutCompletedListeners.clear()
    }

    /**
     * Does actual remeasure and relayout on the node if it is required. The [layoutNode] should be
     * already removed from [relayoutNodes] before running it.
     *
     * When [affectsLookahead] is false, we'll skip lookahead measure & layout, and only measure and
     * layout as needed. This is needed because we don't want [forceMeasureTheSubtree] that doesn't
     * affect lookahead to leak into lookahead and start doing lookahead measure/layout. That would
     * prevent some of the lookahead remeasure/relayout requests from being properly handled as the
     * starting node of [forceMeasureTheSubtree] would be in [LayoutNode.LayoutState.Measuring]
     * until it returns.
     *
     * Note, when [affectsLookahead] is true, we will only do lookahead measure and layout.
     *
     * @return true if the [LayoutNode] size has been changed.
     */
    private fun remeasureAndRelayoutIfNeeded(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean = true,
        relayoutNeeded: Boolean = true
    ): Boolean {
        var sizeChanged = false
        if (layoutNode.isDeactivated) {
            // we don't remeasure or relayout deactivated nodes.
            return false
        }
        if (
            layoutNode.isPlaced || // the root node doesn't have isPlacedByParent = true
                layoutNode.isPlacedByParent ||
                layoutNode.canAffectParent ||
                layoutNode.isPlacedInLookahead == true ||
                layoutNode.canAffectParentInLookahead ||
                layoutNode.alignmentLinesRequired
        ) {
            val constraints = if (layoutNode === root) rootConstraints!! else null
            if (affectsLookahead) {
                // Only do lookahead invalidation when affectsLookahead is true
                if (layoutNode.lookaheadMeasurePending) {
                    sizeChanged = doLookaheadRemeasure(layoutNode, constraints)
                }
                if (relayoutNeeded) {
                    if (
                        (sizeChanged || layoutNode.lookaheadLayoutPending) &&
                            layoutNode.isPlacedInLookahead == true
                    ) {
                        layoutNode.lookaheadReplace()
                    }
                }
            } else {
                if (layoutNode.measurePending) {
                    sizeChanged = doRemeasure(layoutNode, constraints)
                }
                if (relayoutNeeded) {
                    if (layoutNode.layoutPending) {
                        val isPlacedByPlacedParent =
                            layoutNode === root ||
                                (layoutNode.parent?.isPlaced == true && layoutNode.isPlacedByParent)
                        if (isPlacedByPlacedParent) {
                            if (layoutNode === root) {
                                layoutNode.place(0, 0)
                            } else {
                                layoutNode.replace()
                            }
                            onPositionedDispatcher.onNodePositioned(layoutNode)
                            // Since there has been an update to a coordinator somewhere in the
                            // modifier chain of this layout node, we might have onRectChanged
                            // callbacks that need to be notified of that change. As a result, even
                            // if the outer rect of this layout node hasn't changed, we want to
                            // invalidate the callbacks for them
                            layoutNode.requireOwner().rectManager.invalidateCallbacksFor(layoutNode)
                            consistencyChecker?.assertConsistent()
                        }
                    }
                }
            }
            drainPostponedMeasureRequests()
        }
        return sizeChanged
    }

    private fun drainPostponedMeasureRequests() {
        if (postponedMeasureRequests.isNotEmpty()) {
            postponedMeasureRequests.forEach { request ->
                if (request.node.isAttached) {
                    if (!request.isLookahead) {
                        request.node.requestRemeasure(
                            forceRequest = request.isForced,
                            invalidateIntrinsics = false
                        )
                    } else {
                        request.node.requestLookaheadRemeasure(
                            forceRequest = request.isForced,
                            invalidateIntrinsics = false
                        )
                    }
                }
            }
            postponedMeasureRequests.clear()
        }
    }

    /**
     * Remeasures [layoutNode] if it has [LayoutNode.measurePending] or
     * [LayoutNode.lookaheadMeasurePending].
     */
    private fun remeasureOnly(layoutNode: LayoutNode, affectsLookahead: Boolean) {
        if (layoutNode.isDeactivated) {
            return
        }
        val constraints = if (layoutNode === root) rootConstraints!! else null
        if (affectsLookahead) {
            doLookaheadRemeasure(layoutNode, constraints)
        } else {
            doRemeasure(layoutNode, constraints)
        }
    }

    /**
     * Makes sure the passed [layoutNode] and its subtree has the final sizes. The nodes which can
     * potentially affect the parent size will be remeasured.
     *
     * The node or some of the nodes in its subtree can still be kept unmeasured if they are not
     * placed and don't affect the parent size. See [requestRemeasure] for details.
     */
    fun forceMeasureTheSubtree(layoutNode: LayoutNode, affectsLookahead: Boolean) {
        // if there is nothing in `relayoutNodes` everything is remeasured.
        if (relayoutNodes.isEmpty(affectsLookahead)) {
            return
        }

        // assert that it is executed during the `measureAndLayout` pass.
        checkPrecondition(duringMeasureLayout) {
            "forceMeasureTheSubtree should be executed during the measureAndLayout pass"
        }

        // if this node is not yet measured this invocation shouldn't be needed.
        requirePrecondition(!layoutNode.measurePending(affectsLookahead)) {
            "node not yet measured"
        }

        forceMeasureTheSubtreeInternal(layoutNode, affectsLookahead)
    }

    private fun onlyRemeasureIfScheduled(node: LayoutNode, affectsLookahead: Boolean) {
        if (
            node.measurePending(affectsLookahead) && relayoutNodes.contains(node, affectsLookahead)
        ) {
            // we don't need to run relayout as part of this logic. so the node will
            // not be removed from `relayoutNodes` in order to be visited again during
            // the regular pass. it is important as the parent of this node can decide
            // to not place this child, so the child relayout should be skipped.
            remeasureAndRelayoutIfNeeded(node, affectsLookahead, relayoutNeeded = false)
        }
    }

    private fun forceMeasureTheSubtreeInternal(layoutNode: LayoutNode, affectsLookahead: Boolean) {
        layoutNode.forEachChild { child ->
            // only proceed if child's size can affect the parent size
            if (
                !affectsLookahead && child.measureAffectsParent ||
                    affectsLookahead && child.measureAffectsParentLookahead
            ) {
                // When LookaheadRoot's parent gets forceMeasureSubtree call, we need to check
                // both lookahead invalidation and non-lookahead invalidation, just like a measure()
                // call from LookaheadRoot's parent would start the two tracks - lookahead and post
                // lookahead measurements.
                if (child.isOutMostLookaheadRoot() && !affectsLookahead) {
                    // Force subtree measure hitting a lookahead root, pending lookahead measure.
                    // This could happen when the "applyChanges" cause nodes to be attached in
                    // lookahead subtree while the "applyChanges" is a part of the ancestor's
                    // subcomposition in the measure pass.
                    if (child.lookaheadMeasurePending && relayoutNodes.contains(child, true)) {
                        remeasureAndRelayoutIfNeeded(child, true, relayoutNeeded = false)
                    } else {
                        forceMeasureTheSubtree(child, true)
                    }
                }

                onlyRemeasureIfScheduled(child, affectsLookahead)

                // if the child is still in NeedsRemeasure state then this child remeasure wasn't
                // needed. it can happen for example when this child is not placed and can't affect
                // the parent size. we can skip the whole subtree.
                if (!child.measurePending(affectsLookahead)) {
                    // run recursively for the subtree.
                    forceMeasureTheSubtreeInternal(child, affectsLookahead)
                }
            }
        }

        // if the child was resized during the remeasurement it could request a remeasure on
        // the parent. we need to remeasure now as this function assumes the whole subtree is
        // fully measured as a result of the invocation.
        onlyRemeasureIfScheduled(layoutNode, affectsLookahead)
    }

    /**
     * Dispatch [OnPositionedModifier] callbacks for the nodes affected by the previous
     * [measureAndLayout] execution.
     *
     * @param forceDispatch true means the whole tree should dispatch the callback (for example when
     *   the global position of the Owner has been changed)
     */
    fun dispatchOnPositionedCallbacks(forceDispatch: Boolean = false) {
        if (forceDispatch) {
            onPositionedDispatcher.onRootNodePositioned(root)
        }
        onPositionedDispatcher.dispatch()
    }

    /**
     * Removes [node] from the list of LayoutNodes being scheduled for the remeasure/relayout as it
     * was detached.
     */
    fun onNodeDetached(node: LayoutNode) {
        relayoutNodes.remove(node)
        onPositionedDispatcher.remove(node)
    }

    private val LayoutNode.measureAffectsParent
        get() =
            (measuredByParent == InMeasureBlock ||
                layoutDelegate.alignmentLinesOwner.alignmentLines.required)

    private val LayoutNode.canAffectParent
        get() = measurePending && measureAffectsParent

    private val LayoutNode.canAffectParentInLookahead
        get() = lookaheadMeasurePending && measureAffectsParentLookahead

    private val LayoutNode.measureAffectsParentLookahead
        get() =
            (measuredByParentInLookahead == InMeasureBlock ||
                layoutDelegate.lookaheadAlignmentLinesOwner?.alignmentLines?.required == true)

    private fun LayoutNode.measurePending(affectsLookahead: Boolean) =
        if (affectsLookahead) lookaheadMeasurePending else measurePending

    class PostponedRequest(val node: LayoutNode, val isLookahead: Boolean, val isForced: Boolean)
}
