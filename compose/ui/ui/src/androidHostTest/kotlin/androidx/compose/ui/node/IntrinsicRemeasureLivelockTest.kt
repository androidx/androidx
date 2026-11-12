/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.spatial.ExecuteDelayed
import androidx.compose.ui.spatial.RectManager
import androidx.compose.ui.unit.Constraints
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for a measure/layout livelock: a measure block that queries its child's intrinsic
 * size on every measure, combined with a child whose own measure block queries ITS children's
 * intrinsics (the shape produced by e.g. `Row(Modifier.height(IntrinsicSize.Max))`), makes
 * [MeasureAndLayoutDelegate.measureAndLayout] never drain its dirty set:
 *
 * onIntrinsicsQueried -> requestRemeasure -> invalidateIntrinsicsParent -> requestRemeasure
 *
 * re-marks the intrinsics-querying ancestor on every drain iteration, forever.
 *
 * Composable-level equivalent (livelocks a real app, verified on desktop):
 * ```
 * Box(Modifier.layout { m, c ->
 *     val p = m.measure(c)
 *     val iw = m.maxIntrinsicWidth(c.maxHeight) // per-measure intrinsic query
 *     layout(maxOf(p.width, iw), p.height) { p.place(0, 0) }
 * }) {
 *     Row(Modifier.height(IntrinsicSize.Max)) { Text("left"); Text("right") }
 * }
 * ```
 */
@OptIn(InternalComposeUiApi::class)
class IntrinsicRemeasureLivelockTest {

    private val maxAllowedPasses = 5

    /** Leaf with fixed size and intrinsics, standing in for Text. */
    private fun leafPolicy() =
        object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints,
            ): MeasureResult = layout(10, 10) {}

            override fun IntrinsicMeasureScope.minIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int,
            ): Int = 10

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int,
            ): Int = 10

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int,
            ): Int = 10

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int,
            ): Int = 10
        }

    /**
     * Queries its children's max intrinsic height during measure and forwards intrinsic queries to
     * its children - the node-level shape of `Row(Modifier.height(IntrinsicSize.Max))`.
     */
    private fun intrinsicHeightRowPolicy() =
        object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints,
            ): MeasureResult {
                val height = measurables.maxOf { it.maxIntrinsicHeight(constraints.maxWidth) }
                val placeables = measurables.map { it.measure(constraints) }
                val width = placeables.sumOf { it.width }
                return layout(width, height) {
                    var x = 0
                    placeables.forEach {
                        it.place(x, 0)
                        x += it.width
                    }
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int,
            ): Int = measurables.sumOf { it.maxIntrinsicWidth(height) }

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int,
            ): Int = measurables.maxOf { it.maxIntrinsicHeight(width) }
        }

    /**
     * Measures its child and (optionally) queries the child's max intrinsic width on every
     * measure - the node-level shape of the `Modifier.layout` block in the repro.
     */
    private fun itemPolicy(queryIntrinsics: Boolean, onMeasure: () -> Unit) =
        object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints,
            ): MeasureResult {
                onMeasure()
                val placeable = measurables.single().measure(constraints)
                val width =
                    if (queryIntrinsics) {
                        maxOf(
                            placeable.width,
                            measurables.single().maxIntrinsicWidth(constraints.maxHeight),
                        )
                    } else {
                        placeable.width
                    }
                return layout(width, placeable.height) { placeable.place(0, 0) }
            }
        }

    private fun createTestOwner(root: LayoutNode, delegate: MeasureAndLayoutDelegate): Owner {
        val mock = MockOwner(root = root)
        return object : Owner by mock {
            // No-op scheduler: the default one posts to the platform main dispatcher,
            // which is not available in a bare test JVM.
            override val rectManager =
                RectManager(
                    executeDelayed =
                        object : ExecuteDelayed {
                            override fun executeDelayed(delayMillis: Long, block: () -> Unit): Any =
                                this

                            override fun removeDelayedExecution(token: Any) {}
                        }
                )

            override fun onRequestMeasure(
                layoutNode: LayoutNode,
                affectsLookahead: Boolean,
                forceRequest: Boolean,
                scheduleMeasureAndLayout: Boolean,
            ) {
                if (affectsLookahead) {
                    delegate.requestLookaheadRemeasure(layoutNode, forceRequest)
                } else {
                    delegate.requestRemeasure(layoutNode, forceRequest)
                }
            }

            override fun onRequestRelayout(
                layoutNode: LayoutNode,
                affectsLookahead: Boolean,
                forceRequest: Boolean,
            ) {
                if (affectsLookahead) {
                    delegate.requestLookaheadRelayout(layoutNode, forceRequest)
                } else {
                    delegate.requestRelayout(layoutNode, forceRequest)
                }
            }

            override fun forceMeasureTheSubtree(layoutNode: LayoutNode, affectsLookahead: Boolean) {
                delegate.forceMeasureTheSubtree(layoutNode, affectsLookahead)
            }
        }
    }

    private fun measureCountAfterMeasureAndLayout(
        queryIntrinsics: Boolean,
        inLookahead: Boolean = false,
    ): Int {
        val root = LayoutNode()
        val delegate = MeasureAndLayoutDelegate(root)
        val owner = createTestOwner(root, delegate)

        var itemMeasures = 0
        val item =
            LayoutNode().apply {
                measurePolicy =
                    itemPolicy(queryIntrinsics) {
                        itemMeasures++
                        // Circuit breaker: a settling pass measures this node a handful of
                        // times; the livelock measures it forever.
                        check(itemMeasures <= maxAllowedPasses) {
                            "livelock: item measured $itemMeasures times within a single " +
                                "measureAndLayout() pass"
                        }
                    }
            }
        val row = LayoutNode().apply { measurePolicy = intrinsicHeightRowPolicy() }
        val left = LayoutNode().apply { measurePolicy = leafPolicy() }
        val right = LayoutNode().apply { measurePolicy = leafPolicy() }

        row.insertAt(0, left)
        row.insertAt(1, right)
        item.insertAt(0, row)

        if (inLookahead) {
            val lookaheadRoot = LayoutNode(isVirtual = true).apply { isVirtualLookaheadRoot = true }
            lookaheadRoot.insertAt(0, item)
            root.insertAt(0, lookaheadRoot)
        } else {
            root.insertAt(0, item)
        }

        root.measurePolicy =
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    val placeable = measurables.single().measure(constraints)
                    return layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, 0)
                    }
                }
            }
        root.attach(owner)

        delegate.updateRootConstraints(Constraints(maxWidth = 1000, maxHeight = 1000))
        delegate.requestRemeasure(root)
        delegate.measureAndLayout()
        return itemMeasures
    }

    @Test
    fun perMeasureIntrinsicQueryOverIntrinsicUsingContentSettles() {
        val measures = measureCountAfterMeasureAndLayout(queryIntrinsics = true)
        assertEquals(1, measures)
    }

    @Test
    fun controlWithoutIntrinsicQuerySettles() {
        val measures = measureCountAfterMeasureAndLayout(queryIntrinsics = false)
        assertEquals(1, measures)
    }

    @Test
    fun perMeasureIntrinsicQueryOverIntrinsicUsingContentSettles_inLookahead() {
        val measures = measureCountAfterMeasureAndLayout(queryIntrinsics = true, inLookahead = true)
        assertEquals(2, measures)
    }

    @Test
    fun controlWithoutIntrinsicQuerySettles_inLookahead() {
        val measures =
            measureCountAfterMeasureAndLayout(queryIntrinsics = false, inLookahead = true)
        assertEquals(2, measures)
    }

    /**
     * When intrinsics are queried in both the measure block and the layout block, modifying child
     * content must trigger a remeasure of the parent (InMeasureBlock precedence).
     */
    @Test
    fun intrinsicsUsedInBothMeasureAndLayout_childInvalidationRemeasuresParent() {
        val root = LayoutNode()
        val delegate = MeasureAndLayoutDelegate(root)
        val owner = createTestOwner(root, delegate)

        var parentMeasures = 0
        var parentLayouts = 0

        val child = LayoutNode().apply { measurePolicy = leafPolicy() }
        val parent =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            parentMeasures++
                            check(parentMeasures <= maxAllowedPasses) {
                                "livelock: parent measured $parentMeasures times within a single " +
                                    "measureAndLayout() pass"
                            }
                            // Query child's intrinsics during measure pass
                            measurables.single().minIntrinsicHeight(constraints.maxWidth)
                            val placeable = measurables.single().measure(constraints)
                            return layout(100, 100) {
                                parentLayouts++
                                // Query child's intrinsics during layout pass
                                measurables.single().minIntrinsicHeight(constraints.maxWidth)
                                placeable.place(0, 0)
                            }
                        }

                        override fun IntrinsicMeasureScope.minIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int,
                        ): Int = measurables.single().minIntrinsicHeight(width)
                    }
            }
        parent.insertAt(0, child)
        root.insertAt(0, parent)

        root.measurePolicy =
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    val placeable = measurables.single().measure(constraints)
                    return layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, 0)
                    }
                }
            }
        root.attach(owner)

        delegate.updateRootConstraints(Constraints(maxWidth = 1000, maxHeight = 1000))
        delegate.requestRemeasure(root)
        delegate.measureAndLayout()

        // InMeasureBlock must take precedence over InLayoutBlock
        assertEquals(LayoutNode.UsageByParent.InMeasureBlock, child.intrinsicsUsageByParent)

        parentMeasures = 0
        parentLayouts = 0

        // Invalidate child content affecting intrinsics
        child.requestRemeasure()
        delegate.measureAndLayout()

        // Modifying child content must trigger a remeasure of the parent, not just a relayout
        assertEquals(1, parentMeasures)
        assertEquals(1, parentLayouts)
    }

    @Test
    fun intrinsicsUsedInBothMeasureAndLayout_inLookahead_childInvalidationRemeasuresParent() {
        val root = LayoutNode()
        val delegate = MeasureAndLayoutDelegate(root)
        val owner = createTestOwner(root, delegate)

        var parentLookaheadMeasures = 0
        var parentLookaheadLayouts = 0
        var parentMeasuresInPass = 0

        val child = LayoutNode().apply { measurePolicy = leafPolicy() }
        val parent =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            parentMeasuresInPass++
                            check(parentMeasuresInPass <= maxAllowedPasses) {
                                "livelock: parent measured $parentMeasuresInPass times within a " +
                                    "single measureAndLayout() pass"
                            }
                            if (isLookingAhead) {
                                parentLookaheadMeasures++
                            }
                            // Query child's intrinsics during measure pass
                            measurables.single().minIntrinsicHeight(constraints.maxWidth)
                            val placeable = measurables.single().measure(constraints)
                            return layout(100, 100) {
                                if (isLookingAhead) {
                                    parentLookaheadLayouts++
                                }
                                // Query child's intrinsics during layout pass
                                measurables.single().minIntrinsicHeight(constraints.maxWidth)
                                placeable.place(0, 0)
                            }
                        }

                        override fun IntrinsicMeasureScope.minIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int,
                        ): Int = measurables.single().minIntrinsicHeight(width)
                    }
            }
        parent.insertAt(0, child)

        val lookaheadRoot = LayoutNode(isVirtual = true).apply { isVirtualLookaheadRoot = true }
        lookaheadRoot.insertAt(0, parent)
        root.insertAt(0, lookaheadRoot)

        root.measurePolicy =
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    val placeable = measurables.single().measure(constraints)
                    return layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, 0)
                    }
                }
            }
        root.attach(owner)

        delegate.updateRootConstraints(Constraints(maxWidth = 1000, maxHeight = 1000))
        delegate.requestRemeasure(root)
        delegate.measureAndLayout()

        // InMeasureBlock must take precedence over InLayoutBlock in lookahead pass
        assertEquals(LayoutNode.UsageByParent.InMeasureBlock, child.intrinsicsUsageByParent)

        parentLookaheadMeasures = 0
        parentLookaheadLayouts = 0
        parentMeasuresInPass = 0

        // Invalidate child content affecting intrinsics in lookahead
        child.requestLookaheadRemeasure()
        delegate.measureAndLayout()

        // Modifying child content must trigger a remeasure of the parent in lookahead
        assertEquals(1, parentLookaheadMeasures)
        assertEquals(1, parentLookaheadLayouts)
    }

    @Test
    fun changeIntrinsicsUsageFromMeasureToLayout_childInvalidationDoesNotRemeasureParent() {
        val root = LayoutNode()
        val delegate = MeasureAndLayoutDelegate(root)
        val owner = createTestOwner(root, delegate)

        var parentMeasures = 0
        var parentLayouts = 0
        var intrinsicsInMeasure = true

        val child = LayoutNode().apply { measurePolicy = leafPolicy() }
        val parent =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            parentMeasures++
                            if (intrinsicsInMeasure) {
                                measurables.single().minIntrinsicHeight(constraints.maxWidth)
                                val placeable = measurables.single().measure(constraints)
                                return layout(100, 100) {
                                    parentLayouts++
                                    placeable.place(0, 0)
                                }
                            } else {
                                return layout(100, 100) {
                                    parentLayouts++
                                    measurables.single().minIntrinsicHeight(constraints.maxWidth)
                                    val placeable = measurables.single().measure(constraints)
                                    placeable.place(0, 0)
                                }
                            }
                        }

                        override fun IntrinsicMeasureScope.minIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int,
                        ): Int = measurables.single().minIntrinsicHeight(width)
                    }
            }
        parent.insertAt(0, child)
        root.insertAt(0, parent)

        root.measurePolicy =
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    val placeable = measurables.single().measure(constraints)
                    return layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, 0)
                    }
                }
            }
        root.attach(owner)

        delegate.updateRootConstraints(Constraints(maxWidth = 1000, maxHeight = 1000))
        delegate.requestRemeasure(root)
        delegate.measureAndLayout()

        // Frame 1: parent measured child in measure pass with intrinsics
        assertEquals(LayoutNode.UsageByParent.InMeasureBlock, child.intrinsicsUsageByParent)

        // Frame 2: switch to querying intrinsics in layout pass
        intrinsicsInMeasure = false
        delegate.requestRemeasure(parent)
        delegate.measureAndLayout()

        // After switching to layout pass, child's intrinsicsUsageByParent must be InLayoutBlock!
        assertEquals(LayoutNode.UsageByParent.InLayoutBlock, child.intrinsicsUsageByParent)

        parentMeasures = 0
        parentLayouts = 0

        // Frame 3: child invalidation
        child.requestRemeasure()
        delegate.measureAndLayout()

        // Since intrinsics were only used in layout pass, parent must NOT be remeasured
        assertEquals(0, parentMeasures)
        assertEquals(1, parentLayouts)
    }

    @Test
    fun changeIntrinsicsUsageFromMeasureToLayout_nestedSubtree() {
        val root = LayoutNode()
        val delegate = MeasureAndLayoutDelegate(root)
        val owner = createTestOwner(root, delegate)

        var parentMeasures = 0
        var parentLayouts = 0
        var intrinsicsInMeasure = true

        val leaf = LayoutNode().apply { measurePolicy = leafPolicy() }
        val column =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            val placeable = measurables.single().measure(constraints)
                            return layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                    }
            }
        val box =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            val placeable = measurables.single().measure(constraints)
                            return layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                    }
            }
        val parent =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            parentMeasures++
                            if (intrinsicsInMeasure) {
                                val h =
                                    measurables.single().minIntrinsicHeight(constraints.maxWidth)
                                val placeable =
                                    measurables.single().measure(Constraints.fixedHeight(h))
                                return layout(placeable.width, placeable.height) {
                                    parentLayouts++
                                    placeable.place(0, 0)
                                }
                            } else {
                                return layout(100, 100) {
                                    parentLayouts++
                                    val h =
                                        measurables
                                            .single()
                                            .minIntrinsicHeight(constraints.maxWidth)
                                    val placeable =
                                        measurables.single().measure(Constraints.fixedHeight(h))
                                    placeable.place(0, 0)
                                }
                            }
                        }
                    }
            }

        column.insertAt(0, leaf)
        box.insertAt(0, column)
        parent.insertAt(0, box)
        root.insertAt(0, parent)

        root.measurePolicy =
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    val placeable = measurables.single().measure(constraints)
                    return layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, 0)
                    }
                }
            }
        root.attach(owner)

        delegate.updateRootConstraints(Constraints(maxWidth = 1000, maxHeight = 1000))
        delegate.requestRemeasure(root)
        delegate.measureAndLayout()

        // Frame 1: parent measured child in measure pass with intrinsics

        // Frame 2: switch to querying intrinsics in layout pass
        intrinsicsInMeasure = false
        delegate.requestRemeasure(parent)
        delegate.measureAndLayout()

        assertEquals(LayoutNode.UsageByParent.InLayoutBlock, box.intrinsicsUsageByParent)
        assertEquals(LayoutNode.UsageByParent.InLayoutBlock, column.intrinsicsUsageByParent)
        assertEquals(LayoutNode.UsageByParent.InLayoutBlock, leaf.intrinsicsUsageByParent)

        parentMeasures = 0
        parentLayouts = 0

        // Frame 3: leaf invalidation
        leaf.requestRemeasure()
        delegate.measureAndLayout()

        // Since intrinsics were only used in layout pass, parent must NOT be remeasured
        assertEquals(0, parentMeasures)
        assertEquals(1, parentLayouts)
    }

    /**
     * An ancestor that reads a node's intrinsics in its layout block marks that node, and the
     * subtree its intrinsics are computed from, as InLayoutBlock. If the node later reads its
     * child's intrinsics in its own measure block, the child must be upgraded to InMeasureBlock:
     * the node's active Measuring state takes precedence over the InLayoutBlock usage inherited
     * from the ancestor. Otherwise a change to the child's intrinsics would only relayout the
     * ancestor, and the node would never be remeasured to adapt its size.
     */
    @Test
    fun intrinsicQueryDuringMeasurePassNotOverriddenByAncestorLayoutUsage() {
        runAncestorLayoutUsageScenario(inLookahead = false)
    }

    @Test
    fun intrinsicQueryDuringMeasurePassNotOverriddenByAncestorLayoutUsage_inLookahead() {
        runAncestorLayoutUsageScenario(inLookahead = true)
    }

    private fun runAncestorLayoutUsageScenario(inLookahead: Boolean) {
        val root = LayoutNode()
        val delegate = MeasureAndLayoutDelegate(root)
        val owner = createTestOwner(root, delegate)

        var leafHeight = 10
        var ancestorQueriesMiddleInLayout = true
        var middleQueriesColumnInMeasure = false
        // Middle's passes checked by the assertions: lookahead passes when in lookahead,
        // otherwise main passes.
        var middleMeasures = 0
        var middleLayouts = 0
        // Every measure of middle within the current measureAndLayout() call.
        var middleMeasuresInPass = 0

        val leaf =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult = layout(10, leafHeight) {}

                        override fun IntrinsicMeasureScope.minIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int,
                        ): Int = leafHeight
                    }
            }
        // Intermediate node without custom intrinsics (uses DefaultIntrinsicMeasurable)
        val column =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            val placeable = measurables.single().measure(constraints)
                            return layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                    }
            }
        val middle =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            middleMeasuresInPass++
                            // Circuit breaker: fail fast instead of hanging on a livelock.
                            check(middleMeasuresInPass <= maxAllowedPasses) {
                                "livelock: middle measured $middleMeasuresInPass times within a " +
                                    "single measureAndLayout() pass"
                            }
                            val counted = !inLookahead || isLookingAhead
                            if (counted) middleMeasures++
                            val child = measurables.single()
                            val childConstraints =
                                if (middleQueriesColumnInMeasure) {
                                    Constraints.fixedHeight(
                                        child.minIntrinsicHeight(constraints.maxWidth)
                                    )
                                } else {
                                    constraints
                                }
                            val placeable = child.measure(childConstraints)
                            return layout(placeable.width, placeable.height) {
                                if (counted) middleLayouts++
                                placeable.place(0, 0)
                            }
                        }

                        override fun IntrinsicMeasureScope.minIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int,
                        ): Int = measurables.single().minIntrinsicHeight(width)
                    }
            }
        val ancestor =
            LayoutNode().apply {
                measurePolicy =
                    object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            val placeable = measurables.single().measure(constraints)
                            return layout(100, 100) {
                                if (ancestorQueriesMiddleInLayout) {
                                    measurables.single().minIntrinsicHeight(constraints.maxWidth)
                                }
                                placeable.place(0, 0)
                            }
                        }
                    }
            }

        column.insertAt(0, leaf)
        middle.insertAt(0, column)
        ancestor.insertAt(0, middle)
        if (inLookahead) {
            val lookaheadRoot = LayoutNode(isVirtual = true).apply { isVirtualLookaheadRoot = true }
            lookaheadRoot.insertAt(0, ancestor)
            root.insertAt(0, lookaheadRoot)
        } else {
            root.insertAt(0, ancestor)
        }

        root.measurePolicy =
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    val placeable = measurables.single().measure(constraints)
                    return layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, 0)
                    }
                }
            }
        root.attach(owner)

        fun runFrame() {
            middleMeasuresInPass = 0
            delegate.measureAndLayout()
        }

        // Frame 1: only the ancestor's layout block reads intrinsics, so middle and the subtree
        // middle's intrinsics are computed from (column, leaf) are all InLayoutBlock.
        delegate.updateRootConstraints(Constraints(maxWidth = 1000, maxHeight = 1000))
        delegate.requestRemeasure(root)
        runFrame()

        assertEquals(LayoutNode.UsageByParent.InLayoutBlock, middle.intrinsicsUsageByParent)
        assertEquals(LayoutNode.UsageByParent.InLayoutBlock, column.intrinsicsUsageByParent)
        assertEquals(LayoutNode.UsageByParent.InLayoutBlock, leaf.intrinsicsUsageByParent)

        // Frame 2: middle starts reading column's intrinsics in its own measure block while it
        // still carries InLayoutBlock from the ancestor.
        // - The ancestor stops reading middle's intrinsics: onIntrinsicsQueried() always schedules
        //   a remeasure of the queried node, which would remeasure middle in Frame 3 even with a
        //   stale usage.
        // - invalidateIntrinsics = false keeps middle's InLayoutBlock usage in place: a regular
        //   request would also relayout the ancestor, which can clear that usage before middle is
        //   remeasured.
        ancestorQueriesMiddleInLayout = false
        middleQueriesColumnInMeasure = true
        if (inLookahead) {
            middle.requestLookaheadRemeasure(invalidateIntrinsics = false)
        } else {
            middle.requestRemeasure(invalidateIntrinsics = false)
        }
        runFrame()

        // Middle was Measuring when it read column's intrinsics, so column and leaf must be
        // upgraded to InMeasureBlock instead of inheriting middle's InLayoutBlock usage.
        assertEquals(LayoutNode.UsageByParent.InLayoutBlock, middle.intrinsicsUsageByParent)
        assertEquals(LayoutNode.UsageByParent.InMeasureBlock, column.intrinsicsUsageByParent)
        assertEquals(LayoutNode.UsageByParent.InMeasureBlock, leaf.intrinsicsUsageByParent)

        middleMeasures = 0
        middleLayouts = 0

        // Frame 3: leaf's intrinsic height changes. Middle must be remeasured (not just relaid
        // out) to adapt its size to the new intrinsic height.
        leafHeight = 20
        if (inLookahead) {
            leaf.requestLookaheadRemeasure()
        } else {
            leaf.requestRemeasure()
        }
        runFrame()

        assertEquals(1, middleMeasures)
        assertEquals(1, middleLayouts)
        val middleHeight = if (inLookahead) middle.lookaheadPassDelegate!!.height else middle.height
        assertEquals(20, middleHeight)
    }
}
