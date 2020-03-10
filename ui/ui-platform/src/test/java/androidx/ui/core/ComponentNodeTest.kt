/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.test.filters.SmallTest
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.core.pointerinput.resize
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.toPx
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@SmallTest
@RunWith(JUnit4::class)
class ComponentNodeTest {
    @get:Rule
    val thrown = ExpectedException.none()!!

    // Ensure that attach and detach work properly
    @Test
    fun componentNodeAttachDetach() {
        val node = LayoutNode()
        assertNull(node.owner)

        val owner = mock(Owner::class.java)
        node.attach(owner)
        assertEquals(owner, node.owner)
        assertTrue(node.isAttached())

        verify(owner, times(1)).onAttach(node)

        node.detach()
        assertNull(node.owner)
        assertFalse(node.isAttached())
        verify(owner, times(1)).onDetach(node)
    }

    // Ensure that LayoutNode's children are ordered properly through add, remove, move
    @Test
    fun layoutNodeChildrenOrder() {
        val (node, child1, child2) = createSimpleLayout()
        assertEquals(2, node.count)
        assertEquals(child1, node[0])
        assertEquals(child2, node[1])
        assertEquals(0, child1.count)
        assertEquals(0, child2.count)

        node.emitRemoveAt(index = 0, count = 1)
        assertEquals(1, node.count)
        assertEquals(child2, node[0])

        node.emitInsertAt(index = 0, instance = child1)
        assertEquals(2, node.count)
        assertEquals(child1, node[0])
        assertEquals(child2, node[1])

        node.emitRemoveAt(index = 0, count = 2)
        assertEquals(0, node.count)

        val child3 = DrawNode()
        val child4 = DrawNode()

        node.emitInsertAt(0, child1)
        node.emitInsertAt(1, child2)
        node.emitInsertAt(2, child3)
        node.emitInsertAt(3, child4)

        assertEquals(4, node.count)
        assertEquals(child1, node[0])
        assertEquals(child2, node[1])
        assertEquals(child3, node[2])
        assertEquals(child4, node[3])

        node.emitMove(from = 3, count = 1, to = 0)
        assertEquals(4, node.count)
        assertEquals(child4, node[0])
        assertEquals(child1, node[1])
        assertEquals(child2, node[2])
        assertEquals(child3, node[3])

        node.emitMove(from = 0, count = 2, to = 3)
        assertEquals(4, node.count)
        assertEquals(child2, node[0])
        assertEquals(child3, node[1])
        assertEquals(child4, node[2])
        assertEquals(child1, node[3])
    }

    // Ensure that attach of a LayoutNode connects all children
    @Test
    fun layoutNodeAttach() {
        val (node, child1, child2) = createSimpleLayout()

        val owner = mock(Owner::class.java)
        node.attach(owner)
        assertEquals(owner, node.owner)
        assertEquals(owner, child1.owner)
        assertEquals(owner, child2.owner)

        verify(owner, times(1)).onAttach(node)
        verify(owner, times(1)).onAttach(child1)
        verify(owner, times(1)).onAttach(child2)
    }

    // Ensure that detach of a LayoutNode detaches all children
    @Test
    fun layoutNodeDetach() {
        val (node, child1, child2) = createSimpleLayout()
        val owner = mock(Owner::class.java)
        node.attach(owner)
        reset(owner)
        node.detach()

        assertEquals(node, child1.parent)
        assertEquals(node, child2.parent)
        assertNull(node.owner)
        assertNull(child1.owner)
        assertNull(child2.owner)

        verify(owner, times(1)).onDetach(node)
        verify(owner, times(1)).onDetach(child1)
        verify(owner, times(1)).onDetach(child2)
    }

    // Ensure that dropping a child also detaches it
    @Test
    fun layoutNodeDropDetaches() {
        val (node, child1, child2) = createSimpleLayout()
        val owner = mock(Owner::class.java)
        node.attach(owner)

        node.emitRemoveAt(0, 1)
        assertEquals(owner, node.owner)
        assertNull(child1.owner)
        assertEquals(owner, child2.owner)

        verify(owner, times(0)).onDetach(node)
        verify(owner, times(1)).onDetach(child1)
        verify(owner, times(0)).onDetach(child2)
    }

    // Ensure that adopting a child also attaches it
    @Test
    fun layoutNodeAdoptAttaches() {
        val (node, child1, child2) = createSimpleLayout()
        val owner = mock(Owner::class.java)
        node.attach(owner)

        node.emitRemoveAt(0, 1)
        reset(owner)

        node.emitInsertAt(1, child1)
        assertEquals(owner, node.owner)
        assertEquals(owner, child1.owner)
        assertEquals(owner, child2.owner)

        verify(owner, times(0)).onAttach(node)
        verify(owner, times(1)).onAttach(child1)
        verify(owner, times(0)).onAttach(child2)
    }

    @Test
    fun drawNodeChildcounts() {
        val node = DrawNode()
        assertEquals(0, node.count)
    }

    @Test
    fun drawNodeAdd() {
        val node = DrawNode()
        val child = DrawNode()
        node.emitInsertAt(0, child)
        assertEquals(1, node.count)
        assertEquals(child, node[0])
    }

    @Test
    fun childAdd() {
        val node = PointerInputNode()
        val owner = mock(Owner::class.java)
        node.attach(owner)
        verify(owner, times(1)).onAttach(node)

        val child = DrawNode()
        node.emitInsertAt(0, child)
        verify(owner, times(1)).onAttach(child)
        assertEquals(1, node.count)
        assertEquals(node, child.parent)
        assertEquals(owner, child.owner)
    }

    @Test
    fun childCount() {
        val node = PointerInputNode()
        assertEquals(0, node.count)
        node.emitInsertAt(0, PointerInputNode())
        assertEquals(1, node.count)
    }

    @Test
    fun childGet() {
        val node = PointerInputNode()
        val child = PointerInputNode()
        node.emitInsertAt(0, child)
        assertEquals(child, node[0])
    }

    @Test
    fun noMove() {
        val (layout, child1, child2) = createSimpleLayout()
        layout.emitMove(0, 0, 1)
        assertEquals(child1, layout[0])
        assertEquals(child2, layout[1])
    }

    @Test
    fun childRemove() {
        val node = PointerInputNode()
        val owner = mock(Owner::class.java)
        node.attach(owner)
        val child = DrawNode()
        node.emitInsertAt(0, child)
        node.emitRemoveAt(index = 0, count = 1)
        verify(owner, times(1)).onDetach(child)
        assertEquals(0, node.count)
        assertEquals(null, child.parent)
        assertNull(child.owner)
    }

    // Ensure that depth is as expected
    @Test
    fun depth() {
        val root = LayoutNode()
        val (child, grand1, grand2) = createSimpleLayout()
        root.emitInsertAt(0, child)

        val owner = mock(Owner::class.java)
        root.attach(owner)

        assertEquals(0, root.depth)
        assertEquals(1, child.depth)
        assertEquals(2, grand1.depth)
        assertEquals(2, grand2.depth)
    }

    // layoutNode hierarchy should be set properly when a LayoutNode is a child of a LayoutNode
    @Test
    fun directLayoutNodeHierarchy() {
        val layoutNode = LayoutNode()
        val childLayoutNode = LayoutNode()
        layoutNode.emitInsertAt(0, childLayoutNode)

        assertNull(layoutNode.parentLayoutNode)
        assertEquals(layoutNode, childLayoutNode.parentLayoutNode)
        val layoutNodeChildren = findLayoutNodeChildren(layoutNode)
        assertEquals(1, layoutNodeChildren.size)
        assertEquals(childLayoutNode, layoutNodeChildren[0])

        layoutNode.emitRemoveAt(index = 0, count = 1)
        assertNull(childLayoutNode.parentLayoutNode)
    }

    // layoutNode hierarchy should be set properly when a GestureNode is a child of a LayoutNode
    @Test
    fun directLayoutAndGestureNodesHierarchy() {
        val layoutNode = LayoutNode()
        val singleChildNode = PointerInputNode()
        layoutNode.emitInsertAt(0, singleChildNode)

        assertNull(layoutNode.parentLayoutNode)
        assertEquals(layoutNode, singleChildNode.parentLayoutNode)
        val layoutNodeChildren = findLayoutNodeChildren(layoutNode)
        assertEquals(0, layoutNodeChildren.size)

        val childLayoutNodes = findLayoutNodeChildren(singleChildNode)
        assertEquals(0, childLayoutNodes.size)

        layoutNode.emitRemoveAt(index = 0, count = 1)
        assertNull(singleChildNode.parentLayoutNode)
    }

    // layoutNode hierarchy should be set properly when a LayoutNode is a grandchild of a LayoutNode
    @Test
    fun indirectLayoutNodeHierarchy() {
        val layoutNode = LayoutNode()
        val intermediate = PointerInputNode()
        val childLayoutNode = LayoutNode()
        layoutNode.emitInsertAt(0, intermediate)
        assertEquals(layoutNode, intermediate.parentLayoutNode)

        intermediate.emitInsertAt(0, childLayoutNode)

        assertNull(layoutNode.parentLayoutNode)
        assertEquals(layoutNode, childLayoutNode.parentLayoutNode)

        val layoutNodeChildren = findLayoutNodeChildren(layoutNode)
        assertEquals(1, layoutNodeChildren.size)
        assertEquals(childLayoutNode, layoutNodeChildren[0])

        val intermediateLayoutNodeChildren = findLayoutNodeChildren(intermediate)
        assertEquals(1, intermediateLayoutNodeChildren.size)
        assertEquals(childLayoutNode, intermediateLayoutNodeChildren[0])

        intermediate.emitRemoveAt(index = 0, count = 1)
        assertNull(childLayoutNode.parentLayoutNode)

        val intermediateLayoutNodeChildren2 = findLayoutNodeChildren(intermediate)
        assertEquals(0, intermediateLayoutNodeChildren2.size)
    }

    // Test visitChildren() for LayoutNode and a SingleChildNode
    @Test
    fun visitChildren() {
        val (node1, node2, node3) = createSimpleLayout()
        val node4 = PointerInputNode()
        node3.emitInsertAt(0, node4)
        val nodes = mutableListOf<ComponentNode>()
        node1.visitChildren { nodes.add(it) }
        assertEquals(2, nodes.size)
        assertEquals(node2, nodes[0])
        assertEquals(node3, nodes[1])
        node2.visitChildren { nodes.add(it) }
        assertEquals(2, nodes.size)
        node3.visitChildren { nodes.add(it) }
        assertEquals(3, nodes.size)
        assertEquals(node4, nodes[2])
    }

    @Test
    fun countChange() {
        val (node, _, _) = createSimpleLayout()
        val owner = mock(Owner::class.java)
        node.attach(owner)
        verify(owner, times(0)).onSizeChange(node)
        node.handleLayoutResult(object : MeasureScope.LayoutResult {
            override val width: IntPx = 10.ipx
            override val height: IntPx = 10.ipx
            override val alignmentLines: Map<AlignmentLine, IntPx> = emptyMap()
            override fun placeChildren(layoutDirection: LayoutDirection) {}
        })
        verify(owner, times(1)).onSizeChange(node)
    }

    @Test
    fun place() {
        val (node, _, _) = createSimpleLayout()
        val owner = mock(Owner::class.java)
        node.attach(owner)
        verify(owner, times(0)).onPositionChange(node)
        node.place(10.ipx, 10.ipx)
        verify(owner, times(1)).onPositionChange(node)
    }

    @Test
    fun testLayoutNodeAdd() {
        val (layout, child1, child2) = createSimpleLayout()
        val inserted = DrawNode()
        layout.emitInsertAt(0, inserted)
        val children = mutableListOf<ComponentNode>()
        layout.visitChildren { children.add(it) }
        assertEquals(3, children.size)
        assertEquals(inserted, children[0])
        assertEquals(child1, children[1])
        assertEquals(child2, children[2])
    }

    @Test
    fun testLayoutNodeRemove() {
        val (layout, child1, _) = createSimpleLayout()
        val child3 = DrawNode()
        val child4 = DrawNode()
        layout.emitInsertAt(2, child3)
        layout.emitInsertAt(3, child4)
        layout.emitRemoveAt(index = 1, count = 2)

        val children = mutableListOf<ComponentNode>()
        layout.visitChildren { children.add(it) }
        assertEquals(2, children.size)
        assertEquals(child1, children[0])
        assertEquals(child4, children[1])
    }

    @Test
    fun testMoveChildren() {
        val (layout, child1, child2) = createSimpleLayout()
        val child3 = DrawNode()
        val child4 = DrawNode()
        layout.emitInsertAt(2, child3)
        layout.emitInsertAt(3, child4)

        layout.emitMove(from = 2, to = 1, count = 2)

        val children = mutableListOf<ComponentNode>()
        layout.visitChildren { children.add(it) }
        assertEquals(4, children.size)
        assertEquals(child1, children[0])
        assertEquals(child3, children[1])
        assertEquals(child4, children[2])
        assertEquals(child2, children[3])

        layout.emitMove(from = 1, to = 3, count = 2)

        children.clear()
        layout.visitChildren { children.add(it) }
        assertEquals(4, children.size)
        assertEquals(child1, children[0])
        assertEquals(child2, children[1])
        assertEquals(child3, children[2])
        assertEquals(child4, children[3])
    }

    @Test
    fun testInvalidate() {
        val node = DrawNode()
        node.invalidate()
        assertTrue(node.needsPaint)

        val owner = mock(Owner::class.java)
        node.attach(owner)
        verify(owner, times(1)).onInvalidate(node)

        node.needsPaint = false
        reset(owner)
        node.invalidate()
        verify(owner, times(1)).onInvalidate(node)

        reset(owner)
        node.invalidate()
        verify(owner, times(0)).onInvalidate(node)
    }

    @Test
    fun testPxGlobalToLocal() {
        val node0 = LayoutNode()
        node0.attach(mockOwner())
        val node1 = LayoutNode()
        node0.emitInsertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(x0, y0)
        node1.place(x1, y1)

        val globalPosition = PxPosition(250.px, 300.px)

        val expectedX = globalPosition.x - x0.toPx() - x1.toPx()
        val expectedY = globalPosition.y - y0.toPx() - y1.toPx()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.coordinates.globalToLocal(globalPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testIntPxGlobalToLocal() {
        val node0 = LayoutNode()
        node0.attach(mockOwner())
        val node1 = LayoutNode()
        node0.emitInsertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(x0, y0)
        node1.place(x1, y1)

        val globalPosition = PxPosition(250.ipx, 300.ipx)

        val expectedX = globalPosition.x - x0 - x1
        val expectedY = globalPosition.y - y0 - y1
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.coordinates.globalToLocal(globalPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testPxLocalToGlobal() {
        val node0 = LayoutNode()
        node0.attach(mockOwner())
        val node1 = LayoutNode()
        node0.emitInsertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(x0, y0)
        node1.place(x1, y1)

        val localPosition = PxPosition(5.px, 15.px)

        val expectedX = localPosition.x + x0.toPx() + x1.toPx()
        val expectedY = localPosition.y + y0.toPx() + y1.toPx()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.coordinates.localToGlobal(localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testIntPxLocalToGlobal() {
        val node0 = LayoutNode()
        node0.attach(mockOwner())
        val node1 = LayoutNode()
        node0.emitInsertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(x0, y0)
        node1.place(x1, y1)

        val localPosition = PxPosition(5.ipx, 15.ipx)

        val expectedX = localPosition.x + x0 + x1
        val expectedY = localPosition.y + y0 + y1
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.coordinates.localToGlobal(localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testPxLocalToGlobalUsesOwnerPosition() {
        val node = LayoutNode()
        node.attach(mockOwner(IntPxPosition(20.ipx, 20.ipx)))
        node.place(100.ipx, 10.ipx)

        val result = node.coordinates.localToGlobal(PxPosition.Origin)

        assertEquals(PxPosition(120.px, 30.px), result)
    }

    @Test
    fun testIntPxLocalToGlobalUsesOwnerPosition() {
        val node = LayoutNode()
        node.attach(mockOwner(IntPxPosition(20.ipx, 20.ipx)))
        node.place(100.ipx, 10.ipx)

        val result = node.coordinates.localToGlobal(PxPosition.Origin)

        assertEquals(PxPosition(120.ipx, 30.ipx), result)
    }

    @Test
    fun testChildToLocal() {
        val node0 = LayoutNode()
        node0.attach(mockOwner())
        val node1 = LayoutNode()
        node0.emitInsertAt(0, node1)

        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(100.ipx, 10.ipx)
        node1.place(x1, y1)

        val localPosition = PxPosition(5.px, 15.px)

        val expectedX = localPosition.x + x1.toPx()
        val expectedY = localPosition.y + y1.toPx()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node0.coordinates.childToLocal(node1.coordinates, localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testChildToLocalFailedWhenNotAncestor() {
        val node0 = LayoutNode()
        node0.attach(mockOwner())
        val node1 = LayoutNode()
        val node2 = LayoutNode()
        node0.emitInsertAt(0, node1)
        node1.emitInsertAt(0, node2)

        thrown.expect(IllegalStateException::class.java)

        node2.coordinates.childToLocal(node1.coordinates, PxPosition(5.px, 15.px))
    }

    @Test
    fun testChildToLocalFailedWhenNotAncestorNoParent() {
        val owner = mockOwner()
        val node0 = LayoutNode()
        node0.attach(owner)
        val node1 = LayoutNode()
        node1.attach(owner)

        thrown.expect(IllegalStateException::class.java)

        node1.coordinates.childToLocal(node0.coordinates, PxPosition(5.px, 15.px))
    }

    @Test
    fun testChildToLocalTheSameNode() {
        val node = LayoutNode()
        node.attach(mockOwner())
        val position = PxPosition(5.px, 15.px)

        val result = node.coordinates.childToLocal(node.coordinates, position)

        assertEquals(position, result)
    }

    @Test
    fun testPositionRelativeToRoot() {
        val parent = LayoutNode()
        parent.attach(mockOwner())
        val child = LayoutNode()
        parent.emitInsertAt(0, child)
        parent.place(-100.ipx, 10.ipx)
        child.place(50.ipx, 80.ipx)

        val actual = child.coordinates.positionInRoot

        assertEquals(PxPosition(-50.ipx, 90.ipx), actual)
    }

    @Test
    fun testPositionRelativeToRootIsNotAffectedByOwnerPosition() {
        val parent = LayoutNode()
        parent.attach(mockOwner(IntPxPosition(20.ipx, 20.ipx)))
        val child = LayoutNode()
        parent.emitInsertAt(0, child)
        child.place(50.ipx, 80.ipx)

        val actual = child.coordinates.positionInRoot

        assertEquals(PxPosition(50.ipx, 80.ipx), actual)
    }

    @Test
    fun testPositionRelativeToAncestorWithParent() {
        val parent = LayoutNode()
        parent.attach(mockOwner())
        val child = LayoutNode()
        parent.emitInsertAt(0, child)
        parent.place(-100.ipx, 10.ipx)
        child.place(50.ipx, 80.ipx)

        val actual = parent.coordinates.childToLocal(child.coordinates, PxPosition.Origin)

        assertEquals(PxPosition(50.px, 80.px), actual)
    }

    @Test
    fun testPositionRelativeToAncestorWithGrandParent() {
        val grandParent = LayoutNode()
        grandParent.attach(mockOwner())
        val parent = LayoutNode()
        val child = LayoutNode()
        grandParent.emitInsertAt(0, parent)
        parent.emitInsertAt(0, child)
        grandParent.place(-7.ipx, 17.ipx)
        parent.place(23.ipx, -13.ipx)
        child.place(-3.ipx, 11.ipx)

        val actual = grandParent.coordinates.childToLocal(child.coordinates, PxPosition.Origin)

        assertEquals(PxPosition(20.px, -2.px), actual)
    }

    // ComponentNode shouldn't allow adding beyond the count
    @Test
    fun testAddBeyondCurrent() {
        val pointerInputNode = PointerInputNode()
        thrown.expect(IndexOutOfBoundsException::class.java)
        pointerInputNode.emitInsertAt(1, DrawNode())
    }

    // ComponentNode shouldn't allow adding below 0
    @Test
    fun testAddBelowZero() {
        val pointerInputNode = PointerInputNode()
        thrown.expect(IndexOutOfBoundsException::class.java)
        pointerInputNode.emitInsertAt(-1, DrawNode())
    }

    // ComponentNode should error when removing at index < 0
    @Test
    fun testRemoveNegativeIndex() {
        val pointerInputNode = PointerInputNode()
        pointerInputNode.emitInsertAt(0, DrawNode())
        thrown.expect(IndexOutOfBoundsException::class.java)
        pointerInputNode.emitRemoveAt(-1, 1)
    }

    // ComponentNode should error when removing at index > count
    @Test
    fun testRemoveBeyondIndex() {
        val pointerInputNode = PointerInputNode()
        pointerInputNode.emitInsertAt(0, DrawNode())
        thrown.expect(IndexOutOfBoundsException::class.java)
        pointerInputNode.emitRemoveAt(1, 1)
    }

    // ComponentNode should error when removing at count < 0
    @Test
    fun testRemoveNegativeCount() {
        val pointerInputNode = PointerInputNode()
        pointerInputNode.emitInsertAt(0, DrawNode())
        thrown.expect(IllegalArgumentException::class.java)
        pointerInputNode.emitRemoveAt(0, -1)
    }

    // ComponentNode should error when removing at count > entry count
    @Test
    fun testReplaceoMany() {
        val pointerInputNode = PointerInputNode()
        pointerInputNode.emitInsertAt(0, DrawNode())
        thrown.expect(IndexOutOfBoundsException::class.java)
        pointerInputNode.emitRemoveAt(0, 2)
    }

    // ComponentNode should error when there aren't enough items
    @Test
    fun testReplaceoMany2() {
        val pointerInputNode = PointerInputNode()
        thrown.expect(IndexOutOfBoundsException::class.java)
        pointerInputNode.emitRemoveAt(0, 1)
    }

    // ComponentNode should allow removing two items
    @Test
    fun testRemoveTwoItems() {
        val pointerInputNode = PointerInputNode()
        pointerInputNode.emitInsertAt(0, DrawNode())
        pointerInputNode.emitInsertAt(0, DrawNode())
        pointerInputNode.emitRemoveAt(0, 2)
        assertEquals(0, pointerInputNode.count)
    }

    // The layout coordinates of a LayoutNode should be attached when
    // the layout node is attached.
    @Test
    fun coordinatesAttachedWhenLayoutNodeAttached() {
        val layoutNode = LayoutNode()
        val drawModifier = draw { _, _ -> }
        layoutNode.modifier = drawModifier
        assertFalse(layoutNode.coordinates.isAttached)
        assertFalse(layoutNode.coordinates.isAttached)
        layoutNode.attach(mockOwner())
        assertTrue(layoutNode.coordinates.isAttached)
        assertTrue(layoutNode.coordinates.isAttached)
        layoutNode.detach()
        assertFalse(layoutNode.coordinates.isAttached)
        assertFalse(layoutNode.coordinates.isAttached)
    }

    // The LayoutNodeWrapper should be detached when it has been replaced
    @Test
    fun layoutNodeWrapperAttachedWhenLayoutNodeAttached() {
        val layoutNode = LayoutNode()
        val drawModifier = draw { _, _ -> }
        layoutNode.modifier = drawModifier
        val layoutNodeWrapper = layoutNode.layoutNodeWrapper
        assertFalse(layoutNodeWrapper.isAttached)
        layoutNode.attach(mockOwner())
        assertTrue(layoutNodeWrapper.isAttached)
        layoutNode.modifier = draw { _, _ -> }
        assertFalse(layoutNodeWrapper.isAttached)
        assertTrue(layoutNode.coordinates.isAttached)
        assertTrue(layoutNode.coordinates.isAttached)
    }

    @Test
    fun layoutNodeWrapperParentCoordinates() {
        val layoutNode = LayoutNode()
        val layoutNode2 = LayoutNode()
        val drawModifier = draw { _, _ -> }
        layoutNode.modifier = drawModifier
        layoutNode2.emitInsertAt(0, layoutNode)
        layoutNode2.attach(mockOwner())

        assertEquals(
            layoutNode2.innerLayoutNodeWrapper,
            layoutNode.innerLayoutNodeWrapper.parentCoordinates
        )
        assertEquals(
            layoutNode2.innerLayoutNodeWrapper,
            layoutNode.layoutNodeWrapper.parentCoordinates
        )
    }

    @Test
    fun hitTest_pointerInBounds_pointerInputFilterHit() {
        val pointerInputFilter: PointerInputFilter = spy()
        val layoutNode =
            LayoutNode(
                0, 0, 1, 1,
                PointerInputModifier(pointerInputFilter)
            ).apply {
                attach(mockOwner())
            }
        val hit = mutableListOf<PointerInputFilter>()

        layoutNode.hitTest(PxPosition(0.ipx, 0.ipx), hit)

        assertThat(hit).isEqualTo(listOf(pointerInputFilter))
    }

    @Test
    fun hitTest_pointerOutOfBounds_nothingHit() {
        val pointerInputFilter: PointerInputFilter = spy()
        val layoutNode =
            LayoutNode(
                0, 0, 1, 1,
                PointerInputModifier(pointerInputFilter)
            ).apply {
                attach(mockOwner())
            }
        val hit = mutableListOf<PointerInputFilter>()

        layoutNode.hitTest(PxPosition(-1.ipx, -1.ipx), hit)
        layoutNode.hitTest(PxPosition(0.ipx, -1.ipx), hit)
        layoutNode.hitTest(PxPosition(1.ipx, -1.ipx), hit)

        layoutNode.hitTest(PxPosition(-1.ipx, 0.ipx), hit)
        // 0, 0 would hit
        layoutNode.hitTest(PxPosition(1.ipx, 0.ipx), hit)

        layoutNode.hitTest(PxPosition(-1.ipx, 1.ipx), hit)
        layoutNode.hitTest(PxPosition(0.ipx, 1.ipx), hit)
        layoutNode.hitTest(PxPosition(1.ipx, 1.ipx), hit)

        assertThat(hit).isEmpty()
    }

    @Test
    fun hitTest_nestedOffsetNodesHits3_allHitInCorrectOrder() {
        hitTest_nestedOffsetNodes_allHitInCorrectOrder(3)
    }

    @Test
    fun hitTest_nestedOffsetNodesHits2_allHitInCorrectOrder() {
        hitTest_nestedOffsetNodes_allHitInCorrectOrder(2)
    }

    @Test
    fun hitTest_nestedOffsetNodesHits1_allHitInCorrectOrder() {
        hitTest_nestedOffsetNodes_allHitInCorrectOrder(1)
    }

    private fun hitTest_nestedOffsetNodes_allHitInCorrectOrder(numberOfChildrenHit: Int) {
        // Arrange

        val childPointerInputFilter: PointerInputFilter = spy()
        val middlePointerInputFilter: PointerInputFilter = spy()
        val parentPointerInputFilter: PointerInputFilter = spy()

        val childLayoutNode =
            LayoutNode(
                100, 100, 200, 200,
                PointerInputModifier(
                    childPointerInputFilter
                )
            )
        val middleLayoutNode: LayoutNode =
            LayoutNode(
                100, 100, 400, 400,
                PointerInputModifier(
                    middlePointerInputFilter
                )
            ).apply {
                emitInsertAt(0, childLayoutNode)
            }
        val parentLayoutNode: LayoutNode =
            LayoutNode(
                0, 0, 500, 500,
                PointerInputModifier(
                    parentPointerInputFilter
                )
            ).apply {
                emitInsertAt(0, middleLayoutNode)
                attach(mockOwner())
            }

        val offset = when (numberOfChildrenHit) {
            3 -> PxPosition(250.px, 250.px)
            2 -> PxPosition(150.px, 150.px)
            1 -> PxPosition(50.px, 50.px)
            else -> throw IllegalStateException()
        }

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        parentLayoutNode.hitTest(offset, hit)

        // Assert.

        when (numberOfChildrenHit) {
            3 -> assertThat(hit)
                .isEqualTo(
                    listOf(
                        parentPointerInputFilter,
                        middlePointerInputFilter,
                        childPointerInputFilter
                    )
                )
            2 -> assertThat(hit)
                .isEqualTo(
                    listOf(
                        parentPointerInputFilter,
                        middlePointerInputFilter
                    )
                )
            1 -> assertThat(hit)
                .isEqualTo(
                    listOf(
                        parentPointerInputFilter
                    )
                )
            else -> throw IllegalStateException()
        }
    }

    /**
     * This test creates a layout of this shape:
     *
     *  -------------
     *  |     |     |
     *  |  t  |     |
     *  |     |     |
     *  |-----|     |
     *  |           |
     *  |     |-----|
     *  |     |     |
     *  |     |  t  |
     *  |     |     |
     *  -------------
     *
     * Where there is one child in the top right and one in the bottom left, and 2 pointers where
     * one in the top left and one in the bottom right.
     */
    @Test
    fun hitTest_2PointersOver2DifferentPointerInputModifiers_resultIsCorrect() {

        // Arrange

        val childPointerInputFilter1: PointerInputFilter = spy()
        val childPointerInputFilter2: PointerInputFilter = spy()

        val childLayoutNode1 =
            LayoutNode(
                0, 0, 50, 50,
                PointerInputModifier(
                    childPointerInputFilter1
                )
            )

        val childLayoutNode2 =
            LayoutNode(
                50, 50, 100, 100,
                PointerInputModifier(
                    childPointerInputFilter2
                )
            )

        val parentLayoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, childLayoutNode1)
            emitInsertAt(1, childLayoutNode2)
            attach(mockOwner())
        }

        val offset1 = PxPosition(25.px, 25.px)
        val offset2 = PxPosition(75.px, 75.px)

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()

        // Act

        parentLayoutNode.hitTest(offset1, hit1)
        parentLayoutNode.hitTest(offset2, hit2)

        // Assert

        assertThat(hit1).isEqualTo(listOf(childPointerInputFilter1))
        assertThat(hit2).isEqualTo(listOf(childPointerInputFilter2))
    }

    /**
     * This test creates a layout of this shape:
     *
     *  ---------------
     *  | t      |    |
     *  |        |    |
     *  |  |-------|  |
     *  |  | t     |  |
     *  |  |       |  |
     *  |  |       |  |
     *  |--|  |-------|
     *  |  |  | t     |
     *  |  |  |       |
     *  |  |  |       |
     *  |  |--|       |
     *  |     |       |
     *  ---------------
     *
     * There are 3 staggered children and 3 pointers, the first is on child 1, the second is on
     * child 2 in a space that overlaps child 1, and the third is in a space in child 3 that
     * overlaps child 2.
     */
    @Test
    fun hitTest_3DownOnOverlappingPointerInputModifiers_resultIsCorrect() {

        val childPointerInputFilter1: PointerInputFilter = spy()
        val childPointerInputFilter2: PointerInputFilter = spy()
        val childPointerInputFilter3: PointerInputFilter = spy()

        val childLayoutNode1 =
            LayoutNode(
                0, 0, 100, 100,
                PointerInputModifier(
                    childPointerInputFilter1
                )
            )

        val childLayoutNode2 =
            LayoutNode(
                50, 50, 150, 150,
                PointerInputModifier(
                    childPointerInputFilter2
                )
            )

        val childLayoutNode3 =
            LayoutNode(
                100, 100, 200, 200,
                PointerInputModifier(
                    childPointerInputFilter3
                )
            )

        val parentLayoutNode = LayoutNode(0, 0, 200, 200).apply {
            emitInsertAt(0, childLayoutNode1)
            emitInsertAt(1, childLayoutNode2)
            emitInsertAt(2, childLayoutNode3)
            attach(mockOwner())
        }

        val offset1 = PxPosition(25.px, 25.px)
        val offset2 = PxPosition(75.px, 75.px)
        val offset3 = PxPosition(125.px, 125.px)

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()
        val hit3 = mutableListOf<PointerInputFilter>()

        parentLayoutNode.hitTest(offset1, hit1)
        parentLayoutNode.hitTest(offset2, hit2)
        parentLayoutNode.hitTest(offset3, hit3)

        assertThat(hit1).isEqualTo(listOf(childPointerInputFilter1))
        assertThat(hit2).isEqualTo(listOf(childPointerInputFilter2))
        assertThat(hit3).isEqualTo(listOf(childPointerInputFilter3))
    }

    /**
     * This test creates a layout of this shape:
     *
     *  ---------------
     *  |             |
     *  |      t      |
     *  |             |
     *  |  |-------|  |
     *  |  |       |  |
     *  |  |   t   |  |
     *  |  |       |  |
     *  |  |-------|  |
     *  |             |
     *  |      t      |
     *  |             |
     *  ---------------
     *
     * There are 2 children with one over the other and 3 pointers: the first is on background
     * child, the second is on the foreground child, and the third is again on the background child.
     */
    @Test
    fun hitTest_3DownOnFloatingPointerInputModifierV_resultIsCorrect() {

        val childPointerInputFilter1: PointerInputFilter = spy()
        val childPointerInputFilter2: PointerInputFilter = spy()

        val childLayoutNode1 = LayoutNode(
            0, 0, 100, 150,
            PointerInputModifier(
                childPointerInputFilter1
            )
        )
        val childLayoutNode2 = LayoutNode(
            25, 50, 75, 100,
            PointerInputModifier(
                childPointerInputFilter2
            )
        )

        val parentLayoutNode = LayoutNode(0, 0, 150, 150).apply {
            emitInsertAt(0, childLayoutNode1)
            emitInsertAt(1, childLayoutNode2)
            attach(mockOwner())
        }

        val offset1 = PxPosition(50.px, 25.px)
        val offset2 = PxPosition(50.px, 75.px)
        val offset3 = PxPosition(50.px, 125.px)

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()
        val hit3 = mutableListOf<PointerInputFilter>()

        // Act

        parentLayoutNode.hitTest(offset1, hit1)
        parentLayoutNode.hitTest(offset2, hit2)
        parentLayoutNode.hitTest(offset3, hit3)

        // Assert

        assertThat(hit1).isEqualTo(listOf(childPointerInputFilter1))
        assertThat(hit2).isEqualTo(listOf(childPointerInputFilter2))
        assertThat(hit3).isEqualTo(listOf(childPointerInputFilter1))
    }

    /**
     * This test creates a layout of this shape:
     *
     *  -----------------
     *  |               |
     *  |   |-------|   |
     *  |   |       |   |
     *  | t |   t   | t |
     *  |   |       |   |
     *  |   |-------|   |
     *  |               |
     *  -----------------
     *
     * There are 2 children with one over the other and 3 pointers: the first is on background
     * child, the second is on the foreground child, and the third is again on the background child.
     */
    @Test
    fun hitTest_3DownOnFloatingPointerInputModifierH_resultIsCorrect() {

        val childPointerInputFilter1: PointerInputFilter = spy()
        val childPointerInputFilter2: PointerInputFilter = spy()

        val childLayoutNode1 = LayoutNode(
            0, 0, 150, 100,
            PointerInputModifier(
                childPointerInputFilter1
            )
        )
        val childLayoutNode2 = LayoutNode(
            50, 25, 100, 75,
            PointerInputModifier(
                childPointerInputFilter2
            )
        )

        val parentLayoutNode = LayoutNode(0, 0, 150, 150).apply {
            emitInsertAt(0, childLayoutNode1)
            emitInsertAt(1, childLayoutNode2)
            attach(mockOwner())
        }

        val offset1 = PxPosition(25.px, 50.px)
        val offset2 = PxPosition(75.px, 50.px)
        val offset3 = PxPosition(125.px, 50.px)

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()
        val hit3 = mutableListOf<PointerInputFilter>()

        // Act

        parentLayoutNode.hitTest(offset1, hit1)
        parentLayoutNode.hitTest(offset2, hit2)
        parentLayoutNode.hitTest(offset3, hit3)

        // Assert

        assertThat(hit1).isEqualTo(listOf(childPointerInputFilter1))
        assertThat(hit2).isEqualTo(listOf(childPointerInputFilter2))
        assertThat(hit3).isEqualTo(listOf(childPointerInputFilter1))
    }

    /**
     * This test creates a layout of this shape:
     *     0   1   2   3   4
     *   .........   .........
     * 0 .     t .   . t     .
     *   .   |---|---|---|   .
     * 1 . t | t |   | t | t .
     *   ....|---|   |---|....
     * 2     |           |
     *   ....|---|   |---|....
     * 3 . t | t |   | t | t .
     *   .   |---|---|---|   .
     * 4 .     t .   . t     .
     *   .........   .........
     *
     * 4 LayoutNodes with PointerInputModifiers that are clipped by their parent LayoutNode. 4
     * pointers are just inside the parent LayoutNode and inside the child LayoutNodes. 8
     * pointers touch just outside the parent LayoutNode but inside the child LayoutNodes.
     *
     * Because LayoutNodes clip the bounds where children LayoutNodes can be hit, all 8 should miss,
     * but the other 4 touches are inside both, so hit.
     */
    @Test
    fun hitTest_4DownInClippedAreaOfLnsWithPims_resultIsCorrect() {

        // Arrange

        val pointerInputFilter1: PointerInputFilter = spy()
        val pointerInputFilter2: PointerInputFilter = spy()
        val pointerInputFilter3: PointerInputFilter = spy()
        val pointerInputFilter4: PointerInputFilter = spy()

        val layoutNode1 = LayoutNode(
            -1, -1, 1, 1,
            PointerInputModifier(
                pointerInputFilter1
            )
        )
        val layoutNode2 = LayoutNode(
            2, -1, 4, 1,
            PointerInputModifier(
                pointerInputFilter2
            )
        )
        val layoutNode3 = LayoutNode(
            -1, 2, 1, 4,
            PointerInputModifier(
                pointerInputFilter3
            )
        )
        val layoutNode4 = LayoutNode(
            2, 2, 4, 4,
            PointerInputModifier(
                pointerInputFilter4
            )
        )

        val parentLayoutNode = LayoutNode(1, 1, 4, 4).apply {
            emitInsertAt(0, layoutNode1)
            emitInsertAt(1, layoutNode2)
            emitInsertAt(2, layoutNode3)
            emitInsertAt(3, layoutNode4)
            attach(mockOwner())
        }

        val offsetThatHits1 = PxPosition(1.px, 1.px)
        val offsetThatHits2 = PxPosition(3.px, 1.px)
        val offsetThatHits3 = PxPosition(1.px, 3.px)
        val offsetThatHits4 = PxPosition(3.px, 3.px)

        val offsetsThatMiss =
            listOf(
                PxPosition(1.px, 0.px),
                PxPosition(3.px, 0.px),
                PxPosition(0.px, 1.px),
                PxPosition(4.px, 1.px),
                PxPosition(0.px, 3.px),
                PxPosition(4.px, 3.px),
                PxPosition(1.px, 4.px),
                PxPosition(3.px, 4.px)
            )

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()
        val hit3 = mutableListOf<PointerInputFilter>()
        val hit4 = mutableListOf<PointerInputFilter>()

        val miss = mutableListOf<PointerInputFilter>()

        // Act.

        parentLayoutNode.hitTest(offsetThatHits1, hit1)
        parentLayoutNode.hitTest(offsetThatHits2, hit2)
        parentLayoutNode.hitTest(offsetThatHits3, hit3)
        parentLayoutNode.hitTest(offsetThatHits4, hit4)

        offsetsThatMiss.forEach {
            parentLayoutNode.hitTest(it, miss)
        }

        // Assert.

        assertThat(hit1).isEqualTo(listOf(pointerInputFilter1))
        assertThat(hit2).isEqualTo(listOf(pointerInputFilter2))
        assertThat(hit3).isEqualTo(listOf(pointerInputFilter3))
        assertThat(hit4).isEqualTo(listOf(pointerInputFilter4))
        assertThat(miss).isEmpty()
    }

    /**
     * This test creates a layout of this shape:
     *
     *   |---|
     *   |tt |
     *   |t  |
     *   |---|t
     *       tt
     *
     *   But where the additional offset suggest something more like this shape.
     *
     *   tt
     *   t|---|
     *    |  t|
     *    | tt|
     *    |---|
     *
     *   Without the additional offset, it would be expected that only the top left 3 pointers would
     *   hit, but with the additional offset, only the bottom right 3 hit.
     */
    @Test
    fun hitTest_ownerIsOffset_onlyCorrectPointersHit() {

        // Arrange

        val pointerInputFilter: PointerInputFilter = spy()

        val layoutNode = LayoutNode(
            0, 0, 2, 2,
            PointerInputModifier(
                pointerInputFilter
            )
        ).apply {
            attach(mockOwner(IntPxPosition(1.ipx, 1.ipx)))
        }

        val offsetThatHits1 = PxPosition(2.px, 2.px)
        val offsetThatHits2 = PxPosition(2.px, 1.px)
        val offsetThatHits3 = PxPosition(1.px, 2.px)
        val offsetsThatMiss =
            listOf(
                PxPosition(0.px, 0.px),
                PxPosition(0.px, 1.px),
                PxPosition(1.px, 0.px)
            )

        val hit1 = mutableListOf<PointerInputFilter>()
        val hit2 = mutableListOf<PointerInputFilter>()
        val hit3 = mutableListOf<PointerInputFilter>()

        val miss = mutableListOf<PointerInputFilter>()

        // Act.

        layoutNode.hitTest(offsetThatHits1, hit1)
        layoutNode.hitTest(offsetThatHits2, hit2)
        layoutNode.hitTest(offsetThatHits3, hit3)

        offsetsThatMiss.forEach {
            layoutNode.hitTest(it, miss)
        }

        // Assert.

        assertThat(hit1).isEqualTo(listOf(pointerInputFilter))
        assertThat(hit2).isEqualTo(listOf(pointerInputFilter))
        assertThat(hit3).isEqualTo(listOf(pointerInputFilter))
        assertThat(miss).isEmpty()
    }

    @Test
    fun hitTest_pointerOn3NestedPointerInputModifiers_allPimsHitInCorrectOrder() {

        // Arrange.

        val pointerInputFilter1: PointerInputFilter = spy()
        val pointerInputFilter2: PointerInputFilter = spy()
        val pointerInputFilter3: PointerInputFilter = spy()

        val modifier =
            PointerInputModifier(
                pointerInputFilter1
            ) + PointerInputModifier(
                pointerInputFilter2
            ) + PointerInputModifier(
                pointerInputFilter3
            )

        val layoutNode = LayoutNode(
            25, 50, 75, 100,
            modifier
        ).apply {
            attach(mockOwner())
        }

        val offset1 = PxPosition(50.px, 75.px)

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        layoutNode.hitTest(offset1, hit)

        // Assert.

        assertThat(hit).isEqualTo(
            listOf(
                pointerInputFilter1,
                pointerInputFilter2,
                pointerInputFilter3
            )
        )
    }

    @Test
    fun hitTest_pointerOnDeeplyNestedPointerInputModifier_pimIsHit() {

        // Arrange.

        val pointerInputFilter: PointerInputFilter = spy()

        val layoutNode1 =
            LayoutNode(
                1, 5, 500, 500,
                PointerInputModifier(
                    pointerInputFilter
                )
            )
        val layoutNode2: LayoutNode = LayoutNode(2, 6, 500, 500).apply {
            emitInsertAt(0, layoutNode1)
        }
        val layoutNode3: LayoutNode = LayoutNode(3, 7, 500, 500).apply {
            emitInsertAt(0, layoutNode2)
        }
        val layoutNode4: LayoutNode = LayoutNode(4, 8, 500, 500).apply {
            emitInsertAt(0, layoutNode3)
        }.apply {
            attach(mockOwner())
        }
        val offset1 = PxPosition(499.px, 499.px)

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        layoutNode4.hitTest(offset1, hit)

        // Assert.

        assertThat(hit).isEqualTo(listOf(pointerInputFilter))
    }

    @Test
    fun hitTest_pointerOnComplexPointerAndLayoutNodePath_pimsHitInCorrectOrder() {

        // Arrange.

        val pointerInputFilter1: PointerInputFilter = spy()
        val pointerInputFilter2: PointerInputFilter = spy()
        val pointerInputFilter3: PointerInputFilter = spy()
        val pointerInputFilter4: PointerInputFilter = spy()

        val layoutNode1 = LayoutNode(
            1, 6, 500, 500,
            PointerInputModifier(
                pointerInputFilter1
            ) + PointerInputModifier(
                pointerInputFilter2
            )
        )
        val layoutNode2: LayoutNode = LayoutNode(2, 7, 500, 500).apply {
            emitInsertAt(0, layoutNode1)
        }
        val layoutNode3 =
            LayoutNode(
                3, 8, 500, 500,
                PointerInputModifier(
                    pointerInputFilter3
                ) + PointerInputModifier(
                    pointerInputFilter4
                )
            ).apply {
                emitInsertAt(0, layoutNode2)
            }

        val layoutNode4: LayoutNode = LayoutNode(4, 9, 500, 500).apply {
            emitInsertAt(0, layoutNode3)
        }
        val layoutNode5: LayoutNode = LayoutNode(5, 10, 500, 500).apply {
            emitInsertAt(0, layoutNode4)
        }.apply {
            attach(mockOwner())
        }

        val offset1 = PxPosition(499.px, 499.px)

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        layoutNode5.hitTest(offset1, hit)

        // Assert.

        assertThat(hit).isEqualTo(
            listOf(
                pointerInputFilter3,
                pointerInputFilter4,
                pointerInputFilter1,
                pointerInputFilter2
            )
        )
    }

    @Test
    fun hitTest_pointerOnFullyOverlappingPointerInputModifiers_onlyTopPimIsHit() {

        val pointerInputFilter1: PointerInputFilter = spy()
        val pointerInputFilter2: PointerInputFilter = spy()

        val layoutNode1 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                pointerInputFilter1
            )
        )
        val layoutNode2 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifier(
                pointerInputFilter2
            )
        )

        val parentLayoutNode = LayoutNode(0, 0, 100, 100).apply {
            emitInsertAt(0, layoutNode1)
            emitInsertAt(1, layoutNode2)
            attach(mockOwner())
        }

        val offset = PxPosition(50.px, 50.px)

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        parentLayoutNode.hitTest(offset, hit)

        // Assert.

        assertThat(hit).isEqualTo(listOf(pointerInputFilter2))
    }

    @Test
    fun hitTest_pointerOnPointerInputModifierInLayoutNodeWithNoSize_nothingHit() {

        val pointerInputFilter: PointerInputFilter = spy()

        val layoutNode = LayoutNode(
            0, 0, 0, 0,
            PointerInputModifier(
                pointerInputFilter
            )
        ).apply {
            attach(mockOwner())
        }

        val offset = PxPosition.Origin

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        layoutNode.hitTest(offset, hit)

        // Assert.

        assertThat(hit).isEmpty()
    }

    private fun createSimpleLayout(): Triple<LayoutNode, ComponentNode, ComponentNode> {
        val layoutNode = LayoutNode()
        val child1 = LayoutNode()
        val child2 = LayoutNode()
        layoutNode.emitInsertAt(0, child1)
        layoutNode.emitInsertAt(1, child2)
        return Triple(layoutNode, child1, child2)
    }

    private fun mockOwner(position: IntPxPosition = IntPxPosition.Origin): Owner =
        mock {
            on { calculatePosition() } doReturn position
        }

    private fun findLayoutNodeChildren(node: ComponentNode): List<LayoutNode> {
        val layoutNodes = mutableListOf<LayoutNode>()
        node.visitLayoutChildren { child ->
            layoutNodes += child
        }
        return layoutNodes
    }

    private fun LayoutNode(x: Int, y: Int, x2: Int, y2: Int, modifier: Modifier = Modifier.None) =
        LayoutNode().apply {
            this.modifier = modifier
            resize(x2.ipx - x.ipx, y2.ipx - y.ipx)
            place(x.ipx, y.ipx)
        }
}
