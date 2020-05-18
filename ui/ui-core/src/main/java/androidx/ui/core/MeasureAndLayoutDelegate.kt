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

package androidx.ui.core

import androidx.ui.unit.IntPx
import androidx.ui.util.fastForEach
import androidx.ui.util.trace

/**
 * Keeps track of [LayoutNode]s which needs to be remeasured or relaid out.
 *
 * Use [requestRemeasure] to schedule remeasuring or [requestRelayout] to schedule relayout.
 *
 * Use [measureAndLayout] to perform scheduled actions and [dispatchOnPositionedCallbacks] to
 * dispatch [OnPositionedModifier] callbacks for the nodes affected by the previous
 * [measureAndLayout] execution.
 *
 * @param root Root [LayoutNode]
 */
internal class MeasureAndLayoutDelegate(private val root: LayoutNode) {

    /**
     * LayoutNodes that need measure or layout
     */
    private val relayoutNodes = DepthSortedSet(Owner.enableExtraAssertions)

    /**
     * Flag to indicate that we're currently measuring.
     */
    private var duringMeasureLayout = false

    /**
     *
     */
    private val onPositionedDispatcher = OnPositionedDispatcher()

    /**
     * The current measure iteration. The value is incremented during the [measureAndLayout]
     * execution. Some [measureAndLayout] executions will increment it more than once.
     */
    var measureIteration: Long = 1L
        get() {
            require(duringMeasureLayout) {
                "measureIteration should be only used during the measure/layout pass"
            }
            return field
        }
        private set

    /**
     * Stores the list of [LayoutNode]s scheduled to be remeasured in the next measure/layout pass.
     * We were unable to mark them as needsRemeasure=true previously as this request happened
     * during the previous measure/layout pass and they were already measured as part of it.
     * See [requestRemeasure] for more details.
     */
    private val postponedMeasureRequests = mutableListOf<LayoutNode>()

    /**
     * The constraints used to measure the root [LayoutNode].
     */
    var rootConstraints = Constraints.fixed(width = IntPx.Zero, height = IntPx.Zero)
        set(value) {
            if (value != field) {
                field = value
                requestRemeasure(root)
            }
        }

    private val consistencyChecker: LayoutTreeConsistencyChecker? =
        if (Owner.enableExtraAssertions) {
            LayoutTreeConsistencyChecker(
                root,
                { duringMeasureLayout },
                relayoutNodes,
                postponedMeasureRequests
            )
        } else {
            null
        }

    /**
     * Requests remeasure for this [layoutNode] and nodes affected by its measure result.
     *
     * @return returns true if the [measureAndLayout] execution should be scheduled as a result
     * of the request.
     */
    fun requestRemeasure(layoutNode: LayoutNode): Boolean {
        return trace("AndroidOwner:onRequestMeasure") {
            layoutNode.requireOwner()
            if (layoutNode.isMeasuring) {
                // we're already measuring it, let's swallow. example when it happens: we compose
                // DataNode inside WithConstraints, this calls onRequestMeasure on DataNode's
                // parent, but this parent is WithConstraints which is currently measuring.
                return false
            }
            if (layoutNode.needsRemeasure) {
                // requestMeasure has already been called for this node
                return false
            }
            if (layoutNode.isLayingOut) {
                // requestMeasure is currently laying out and it is incorrect to request remeasure
                // now, let's postpone it.
                layoutNode.markRemeasureRequested()
                postponedMeasureRequests.add(layoutNode)
                consistencyChecker?.assertConsistent()
                return false
            }

            // find root of layout request:
            var layout = layoutNode
            while (layout.affectsParentSize && layout.parent != null) {
                val parent = layout.parent!!
                if (parent.isMeasuring || parent.isLayingOut) {
                    if (!layout.needsRemeasure) {
                        layout.markRemeasureRequested()
                        // parent is currently measuring and we set needsRemeasure to true so if
                        // the parent didn't yet try to measure the node it will remeasure it.
                        // if the parent didn't plan to measure during this pass then needsRemeasure
                        // stay 'true' and we will manually call 'onRequestMeasure' for all
                        // the not-measured nodes in 'postponedMeasureRequests'.
                        postponedMeasureRequests.add(layout)
                    }
                    consistencyChecker?.assertConsistent()
                    return false
                } else {
                    layout.markRemeasureRequested()
                    if (parent.needsRemeasure) {
                        // don't need to do anything else since the parent is already scheduled
                        // for a remeasuring
                        consistencyChecker?.assertConsistent()
                        return false
                    }
                    layout = parent
                }
            }
            layout.markRemeasureRequested()

            requestRelayout(layout.parent ?: layout)
        }
    }

    /**
     * Requests relayout for this [layoutNode] and nodes affected by its position.
     *
     * @return returns true if the [measureAndLayout] execution should be scheduled as a result
     * of the request.
     */
    fun requestRelayout(layoutNode: LayoutNode): Boolean {
        if (layoutNode.needsRelayout || (layoutNode.needsRemeasure && layoutNode !== root) ||
            layoutNode.isLayingOut || layoutNode.isMeasuring
        ) {
            // don't need to do anything else since the parent is already scheduled
            // for a relayout (measure will trigger relayout), or is laying out right now
            consistencyChecker?.assertConsistent()
            return false
        }
        layoutNode.requireOwner()
        var nodeToRelayout = layoutNode

        // mark alignments as dirty first
        if (!layoutNode.alignmentLinesRequired) {
            // Mark parents alignment lines as dirty, for cases when we needed alignment lines
            // at some point, but currently they are not queried anymore. If they are actively
            // queried, they will be made dirty below in this method.
            var layout: LayoutNode? = layoutNode
            while (layout != null && !layout.dirtyAlignmentLines) {
                layout.dirtyAlignmentLines = true
                layout = layout.parent
            }
        } else {
            var layout = layoutNode
            while (layout != layoutNode.alignmentLinesQueryOwner &&
                // and relayout or remeasure(includes relayout) is not scheduled already
                !(layout.needsRelayout || layout.needsRemeasure || layoutNode.isLayingOut ||
                        layoutNode.isMeasuring)
            ) {
                layout.markRelayoutRequested()
                layout.dirtyAlignmentLines = true
                val parent = layout.parent
                if (parent == null) break
                layout = parent
            }
            layout.dirtyAlignmentLines = true
            nodeToRelayout = layout
        }

        nodeToRelayout.markRelayoutRequested()
        relayoutNodes.add(nodeToRelayout)
        consistencyChecker?.assertConsistent()
        return !duringMeasureLayout
    }

    private fun LayoutNode.markRemeasureRequested() {
        needsRemeasure = true
        if (needsRelayout) {
            // cancel needsRelayout as remeasure includes relayout
            needsRelayout = false
        }
    }

    private fun LayoutNode.markRelayoutRequested() {
        // remeasure includes relayout so we are ok
        if (!needsRemeasure) {
            needsRelayout = true
        }
    }

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out
     */
    fun measureAndLayout() {
        trace("AndroidOwner:measureAndLayout") {
            if (relayoutNodes.isNotEmpty()) {
                duringMeasureLayout = true
                relayoutNodes.popEach { layoutNode ->
                    measureIteration++
                    if (layoutNode === root) {
                        // it is the root node - the only top node from relayoutNodes
                        // which needs to be remeasured.
                        layoutNode.measure(rootConstraints, layoutNode.layoutDirection)
                    }
                    require(!layoutNode.needsRemeasure) {
                        "$layoutNode shouldn't require remeasure. relayoutNodes " +
                                "consists of the top nodes of the affected subtrees"
                    }
                    if (layoutNode.needsRelayout) {
                        layoutNode.layout()
                        onPositionedDispatcher.onNodePositioned(layoutNode)
                        consistencyChecker?.assertConsistent()
                    }
                    // execute postponed `onRequestMeasure`
                    if (postponedMeasureRequests.isNotEmpty()) {
                        postponedMeasureRequests.fastForEach {
                            // if it was detached or already measured by the parent then skip it
                            if (it.isAttached() && it.needsRemeasure) {
                                it.needsRemeasure = false
                                requestRemeasure(it)
                            }
                        }
                        postponedMeasureRequests.clear()
                    }
                }
                duringMeasureLayout = false
                consistencyChecker?.assertConsistent()
            }
        }
    }

    /**
     * Dispatch [OnPositionedModifier] callbacks for the nodes affected by the previous
     * [measureAndLayout] execution.
     */
    fun dispatchOnPositionedCallbacks() {
        onPositionedDispatcher.dispatch()
    }

    /**
     * Removes [node] from the list of LayoutNodes being scheduled for the remeasure/relayout as
     * it was detached.
     */
    fun onNodeDetached(node: LayoutNode) {
        relayoutNodes.remove(node)
    }
}
