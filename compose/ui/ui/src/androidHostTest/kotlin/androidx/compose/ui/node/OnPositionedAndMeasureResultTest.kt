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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Ruler
import androidx.compose.ui.layout.RulerScope
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OnPositionedAndMeasureResultTest {

    /**
     * Checks that `MeasureScope.layout(...)` populates `width`, `height`, `alignmentLines`, and
     * `rulers` on `MeasureResult`, and `placeChildren()` runs with the scope's density/fontScale.
     */
    @Test
    fun measureScope_layout_populatesPropertiesAndInvokesPlacementBlock() {
        val scope =
            object : MeasureScope {
                override val density: Float = 2f
                override val fontScale: Float = 1.5f
                override val layoutDirection: LayoutDirection = LayoutDirection.Rtl
            }
        val alignmentLines = mapOf<AlignmentLine, Int>(FirstBaseline to 12)
        val rulersBlock: RulerScope.() -> Unit = {}
        var placed = false
        var capturedDensity = -1f
        var capturedFontScale = -1f

        val result =
            scope.layout(40, 50, alignmentLines, rulersBlock) {
                placed = true
                capturedDensity = density
                capturedFontScale = fontScale
            }

        assertThat(result.width).isEqualTo(40)
        assertThat(result.height).isEqualTo(50)
        assertThat(result.alignmentLines).isEqualTo(alignmentLines)
        assertThat(result.rulers).isEqualTo(rulersBlock)

        result.placeChildren()
        assertThat(placed).isTrue()
        assertThat(capturedDensity).isEqualTo(2f)
        assertThat(capturedFontScale).isEqualTo(1.5f)
    }

    /**
     * Checks that the lazy-ruler `MeasureScope.layout(...)` overload populates `isRulerProvided`
     * and `rulerProvider` on `MeasureResult` and invokes the placement block.
     */
    @Test
    fun measureScope_layoutWithRulerProvider_populatesPropertiesAndPlaces() {
        val scope =
            object : MeasureScope {
                override val density: Float = 1f
                override val fontScale: Float = 1f
                override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
            }
        val customRuler = VerticalRuler()
        val isProvided: (Ruler) -> Boolean = { it === customRuler }
        val provider: RulerScope.(Ruler) -> Unit = { ruler ->
            if (ruler === customRuler) ruler.provides(10f)
        }
        var placed = false

        val result =
            scope.layout(
                width = 30,
                height = 60,
                alignmentLines = mapOf(FirstBaseline to 8),
                isRulerProvided = isProvided,
                rulerProvider = provider,
            ) {
                placed = true
            }

        assertThat(result.width).isEqualTo(30)
        assertThat(result.height).isEqualTo(60)
        assertThat(result.alignmentLines).containsEntry(FirstBaseline, 8)
        assertThat(result.isRulerProvided?.invoke(customRuler)).isTrue()
        assertThat(result.rulerProvider).isEqualTo(provider)
        result.placeChildren()
        assertThat(placed).isTrue()
    }

    /**
     * Checks that calling `MeasureScope.layout(...)` with a negative width or height throws
     * `IllegalStateException`.
     */
    @Test
    fun measureScope_layout_invalidSizeThrows() {
        val scope =
            object : MeasureScope {
                override val density: Float = 1f
                override val fontScale: Float = 1f
                override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
            }

        assertFailsWith<IllegalStateException> { scope.layout(-1, 10) {} }
        assertFailsWith<IllegalStateException> { scope.layout(10, -1) {} }
    }

    /**
     * Checks that calling `layout(...)` directly on a `NodeCoordinator` uses that coordinator's own
     * `placementScope` when `placeChildren()` runs.
     */
    @Test
    fun nodeCoordinator_measureScope_placeChildrenUsesCoordinatorPlacementScope() {
        val node = LayoutNode(0, 0, 100, 80)
        var capturedScope: Placeable.PlacementScope? = null
        val result =
            node.outerCoordinator.layout(25, 35, mapOf(FirstBaseline to 5)) {
                capturedScope = this
            }

        assertThat(result.width).isEqualTo(25)
        assertThat(result.height).isEqualTo(35)
        assertThat(result.alignmentLines).containsEntry(FirstBaseline, 5)
        result.placeChildren()
        assertThat(capturedScope).isSameInstanceAs(node.outerCoordinator.placementScope)
    }

    private fun placedNode(
        width: Int = 100,
        height: Int = 100,
        modifier: Modifier = Modifier,
        attachOwner: Boolean = true,
        children: List<LayoutNode> = emptyList(),
    ): LayoutNode {
        val node =
            LayoutNode().apply {
                this.modifier = modifier
                measurePolicy =
                    object : LayoutNode.NoIntrinsicsMeasurePolicy("not supported") {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints,
                        ): MeasureResult {
                            val placeables = measurables.map { it.measure(constraints) }
                            return layout(width, height) { placeables.forEach { it.place(0, 0) } }
                        }
                    }
                children.forEachIndexed { index, child -> insertAt(index, child) }
            }
        if (attachOwner) {
            val delegate = MeasureAndLayoutDelegate(node)
            val delegateOwner =
                object : MockOwner() {
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
                }
            node.attach(delegateOwner)
            delegate.updateRootConstraints(Constraints.fixed(width, height))
            delegate.measureAndLayout()
        }
        return node
    }

    /**
     * Checks that queuing a child and root into `OnPositionedDispatcher` dispatches both callbacks
     * and leaves `isNotEmpty()` as `false`.
     */
    @Test
    fun onPositionedDispatcher_dispatchesParentBeforeChild_evenWhenChildQueuedFirst() {
        val events = mutableListOf<String>()
        val child =
            placedNode(
                50,
                50,
                Modifier.onGloballyPositioned { events += "child" },
                attachOwner = false,
            )
        val root =
            placedNode(
                100,
                100,
                Modifier.onGloballyPositioned { events += "root" },
                children = listOf(child),
            )

        val dispatcher = OnPositionedDispatcher()
        dispatcher.onNodePositioned(child)
        dispatcher.onNodePositioned(root)
        assertThat(dispatcher.isNotEmpty()).isTrue()

        dispatcher.dispatch()

        assertThat(dispatcher.isNotEmpty()).isFalse()
        assertThat(events).containsExactly("root", "child")
    }

    /**
     * Checks that queuing multiple equal-depth sibling nodes into `OnPositionedDispatcher`
     * dispatches each sibling's callback once without dropping or duplicating any.
     */
    @Test
    fun onPositionedDispatcher_dispatchesEqualDepthSiblings_withoutDuplicates() {
        val events = mutableListOf<String>()
        val siblingA =
            placedNode(20, 20, Modifier.onGloballyPositioned { events += "A" }, attachOwner = false)
        val siblingB =
            placedNode(20, 20, Modifier.onGloballyPositioned { events += "B" }, attachOwner = false)
        val siblingC =
            placedNode(20, 20, Modifier.onGloballyPositioned { events += "C" }, attachOwner = false)
        val root =
            placedNode(
                100,
                100,
                children = listOf(siblingA, siblingB, siblingC),
            )
        assertThat(root.children).hasSize(3)

        val dispatcher = OnPositionedDispatcher()
        dispatcher.onNodePositioned(siblingA)
        dispatcher.onNodePositioned(siblingB)
        dispatcher.onNodePositioned(siblingC)
        dispatcher.dispatch()

        assertThat(events).containsExactly("A", "B", "C")
    }

    /**
     * Checks that `onRootNodePositioned(root)` clears previously queued individual nodes and
     * dispatches the `root` subtree.
     */
    @Test
    fun onPositionedDispatcher_onRootNodePositioned_clearsPriorQueuedNodes() {
        val events = mutableListOf<String>()
        val child =
            placedNode(
                50,
                50,
                Modifier.onGloballyPositioned { events += "child" },
                attachOwner = false,
            )
        val root =
            placedNode(
                100,
                100,
                Modifier.onGloballyPositioned { events += "root" },
                children = listOf(child),
            )
        val unrelated =
            placedNode(
                10,
                10,
                Modifier.onGloballyPositioned { events += "unrelated" },
            )

        val dispatcher = OnPositionedDispatcher()
        dispatcher.onNodePositioned(unrelated)
        dispatcher.onRootNodePositioned(root)
        dispatcher.dispatch()

        assertThat(events).containsExactly("root", "child").inOrder()
    }

    /**
     * Checks that `OnPositionedDispatcher.remove(node)` before `dispatch()` prevents that node's
     * callback from firing while still dispatching remaining queued nodes.
     */
    @Test
    fun onPositionedDispatcher_remove_preventsDispatch() {
        val events = mutableListOf<String>()
        val nodeA = placedNode(20, 20, Modifier.onGloballyPositioned { events += "A" })
        val nodeB = placedNode(20, 20, Modifier.onGloballyPositioned { events += "B" })

        val dispatcher = OnPositionedDispatcher()
        dispatcher.onNodePositioned(nodeA)
        dispatcher.onNodePositioned(nodeB)
        dispatcher.remove(nodeA)
        dispatcher.dispatch()

        assertThat(events).containsExactly("B")
    }

    /**
     * Checks that `OnPositionedDispatcher` ignores nodes with no `onGloballyPositioned` observers
     * so `isNotEmpty()` remains `false`.
     */
    @Test
    fun onPositionedDispatcher_ignoresNodesWithoutObservers() {
        val nodeWithoutObserver = placedNode(20, 20)

        val dispatcher = OnPositionedDispatcher()
        dispatcher.onNodePositioned(nodeWithoutObserver)
        assertThat(dispatcher.isNotEmpty()).isFalse()

        dispatcher.onRootNodePositioned(nodeWithoutObserver)
        assertThat(dispatcher.isNotEmpty()).isFalse()
    }

    /**
     * Checks that `OnPositionedDispatcher` handles more than `MinArraySize` (16) queued nodes and
     * supports re-entrant `onNodePositioned` calls during `dispatch()` for the next pass.
     */
    @Test
    fun onPositionedDispatcher_reentrantAddAndMoreThanMinArraySize_succeeds() {
        val events = mutableListOf<Int>()
        val dispatcher = OnPositionedDispatcher()
        val extraNode = placedNode(10, 10, Modifier.onGloballyPositioned { events += 999 })

        val nodes =
            List(24) { index ->
                placedNode(
                    10,
                    10,
                    Modifier.onGloballyPositioned {
                        events += index
                        if (index == 0) {
                            dispatcher.onNodePositioned(extraNode)
                        }
                    },
                )
            }

        nodes.forEach { dispatcher.onNodePositioned(it) }
        dispatcher.dispatch()

        assertThat(events).hasSize(24)
        assertThat(dispatcher.isNotEmpty()).isTrue()

        dispatcher.dispatch()
        assertThat(events).contains(999)
        assertThat(dispatcher.isNotEmpty()).isFalse()
    }

    /**
     * Checks that `toParentPosition`, `fromParentPosition`, and `transformFrom` on
     * `NodeCoordinator` agree and round-trip accurately with non-zero child offsets.
     */
    @Test
    fun nodeCoordinator_transformAndPositionConversions_roundTripWithNonZeroOffsets() {
        lateinit var child: LayoutNode
        val root =
            LayoutNode(0, 0, 200, 200) {
                child = childNode(15, 25, 85, 105)
            }

        val localPoint = Offset(10f, 20f)
        val inRoot = child.outerCoordinator.toParentPosition(localPoint)
        assertThat(inRoot).isEqualTo(Offset(25f, 45f))
        assertThat(child.outerCoordinator.fromParentPosition(inRoot)).isEqualTo(localPoint)

        val toAncestor = Matrix()
        root.outerCoordinator.transformFrom(child.outerCoordinator, toAncestor)
        assertThat(toAncestor.map(localPoint)).isEqualTo(Offset(25f, 45f))

        val fromAncestor = Matrix()
        child.outerCoordinator.transformFrom(root.outerCoordinator, fromAncestor)
        assertThat(fromAncestor.map(Offset(25f, 45f))).isEqualTo(localPoint)
    }

    /**
     * Checks pointer hit-testing across nested nodes with `touchBoundsExpansion` and
     * `interceptOutOfBoundsChildEvents` inside, near, and outside bounds.
     */
    @Test
    fun nodeCoordinator_hitTest_skipsAndHitsNearAndSpeculativeBounds() {
        val parentFilter =
            FakePointerInputModifierNode(
                touchBoundsExpansion = TouchBoundsExpansion(5, 5, 5, 5),
                interceptOutOfBoundsChildEvents = false,
            )
        val childFilter =
            FakePointerInputModifierNode(
                touchBoundsExpansion = TouchBoundsExpansion(2, 2, 2, 2),
                interceptOutOfBoundsChildEvents = false,
            )
        val minTargetFilter = FakePointerInputModifierNode()
        val root =
            LayoutNode(0, 0, 100, 100) {
                childNode(10, 10, 30, 30, parentFilter.toModifier()) {
                    // Extends to (38, 38) in root coordinates, outside parent's expanded (35, 35)
                    childNode(15, 15, 28, 28, childFilter.toModifier())
                }
                childNode(
                    60,
                    60,
                    70,
                    70,
                    minTargetFilter.toModifier(),
                    minimumTouchTargetSize = DpSize(48.dp, 48.dp),
                )
            }

        // Inside child bounds (both parent and child are hit, outer-to-inner order)
        assertThat(root.hitTest(Offset(26f, 26f), PointerType.Touch))
            .containsExactly(parentFilter, childFilter)
            .inOrder()

        // Inside parent expanded stylus bounds, outside child
        assertThat(root.hitTest(Offset(7f, 10f), PointerType.Stylus)).containsExactly(parentFilter)

        // Speculative hit: outside parent expanded bounds (35, 35), inside child bounds (38, 38)
        assertThat(root.hitTest(Offset(36f, 36f), PointerType.Touch)).containsExactly(childFilter)

        // Hit near via minimumTouchTargetSize (outside 60..70, inside 48dp touch target)
        assertThat(root.hitTest(Offset(55f, 65f), PointerType.Touch))
            .containsExactly(minTargetFilter)

        // Outside all bounds
        assertThat(root.hitTest(Offset(95f, 95f), PointerType.Touch)).isEmpty()
    }

    /**
     * Checks that `hitTestSemantics` skips a `SemanticsModifierNode` with no semantics properties
     * to reach a child `SemanticsModifierNode` that defines semantics.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun nodeCoordinator_hitTestSemantics_skipsNonImportantNodeToReachImportantChild() {
        val nonImportant =
            object : Modifier.Node(), SemanticsModifierNode {
                override fun SemanticsPropertyReceiver.applySemantics() {}
            }
        val important =
            object : Modifier.Node(), SemanticsModifierNode {
                override fun SemanticsPropertyReceiver.applySemantics() {
                    contentDescription = "target"
                }
            }
        fun SemanticsModifierNode.asModifier(): Modifier =
            object : ModifierNodeElement<Modifier.Node>() {
                override fun create(): Modifier.Node = this@asModifier.node

                override fun update(node: Modifier.Node) {}

                override fun equals(other: Any?): Boolean = other === this

                override fun hashCode(): Int = System.identityHashCode(this)
            }

        lateinit var childNodeRef: LayoutNode
        val root =
            LayoutNode(0, 0, 100, 100) {
                childNodeRef =
                    childNode(0, 0, 100, 100, nonImportant.asModifier()) {
                        childNode(10, 10, 50, 50, important.asModifier())
                    }
            }

        val hitResult = HitTestResult()
        root.hitTestSemantics(Offset(25f, 25f), hitResult)
        assertThat(hitResult).containsExactly(important)
        assertThat(childNodeRef.outerCoordinator.coordinates.size.width).isEqualTo(100)
    }
}
