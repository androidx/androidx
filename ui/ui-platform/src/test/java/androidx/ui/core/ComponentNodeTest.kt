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
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
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
import kotlin.IllegalArgumentException

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
        node.resize(10.ipx, 10.ipx)
        verify(owner, times(1)).onSizeChange(node)
    }

    @Test
    fun moveTo() {
        val (node, _, _) = createSimpleLayout()
        val owner = mock(Owner::class.java)
        node.attach(owner)
        verify(owner, times(0)).onPositionChange(node)
        node.moveTo(10.ipx, 10.ipx)
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
    fun testGlobalToLocal() {
        val node0 = LayoutNode()
        node0.attach(mockOwner())
        val node1 = LayoutNode()
        node0.emitInsertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.moveTo(x0, y0)
        node1.moveTo(x1, y1)

        val globalPosition = PxPosition(250.px, 300.px)

        val expectedX = globalPosition.x - x0.toPx() - x1.toPx()
        val expectedY = globalPosition.y - y0.toPx() - y1.toPx()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.globalToLocal(globalPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testLocalToGlobal() {
        val node0 = LayoutNode()
        node0.attach(mockOwner())
        val node1 = LayoutNode()
        node0.emitInsertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.moveTo(x0, y0)
        node1.moveTo(x1, y1)

        val localPosition = PxPosition(5.px, 15.px)

        val expectedX = localPosition.x + x0.toPx() + x1.toPx()
        val expectedY = localPosition.y + y0.toPx() + y1.toPx()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.localToGlobal(localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testLocalToGlobalUsesOwnerPosition() {
        val node = LayoutNode()
        node.attach(mockOwner(PxPosition(20.px, 20.px)))
        node.moveTo(100.ipx, 10.ipx)

        val result = node.localToGlobal(PxPosition.Origin)

        assertEquals(PxPosition(120.px, 30.px), result)
    }

    @Test
    fun testChildToLocal() {
        val node0 = LayoutNode()
        val node1 = LayoutNode()
        node0.emitInsertAt(0, node1)

        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.moveTo(100.ipx, 10.ipx)
        node1.moveTo(x1, y1)

        val localPosition = PxPosition(5.px, 15.px)

        val expectedX = localPosition.x + x1.toPx()
        val expectedY = localPosition.y + y1.toPx()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node0.childToLocal(node1, localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testChildToLocalFailedWhenNotAncestor() {
        val node0 = LayoutNode()
        val node1 = LayoutNode()
        val node2 = LayoutNode()
        node0.emitInsertAt(0, node1)
        node1.emitInsertAt(0, node2)

        thrown.expect(IllegalStateException::class.java)

        node2.childToLocal(node1, PxPosition(5.px, 15.px))
    }

    @Test
    fun testChildToLocalFailedWhenNotAncestorNoParent() {
        val node0 = LayoutNode()
        val node1 = LayoutNode()

        thrown.expect(IllegalStateException::class.java)

        node1.childToLocal(node0, PxPosition(5.px, 15.px))
    }

    @Test
    fun testChildToLocalTheSameNode() {
        val node = LayoutNode()
        val position = PxPosition(5.px, 15.px)

        val result = node.childToLocal(node, position)

        assertEquals(position, result)
    }

    @Test
    fun testPositionRelativeToRoot() {
        val parent = LayoutNode()
        val child = LayoutNode()
        parent.emitInsertAt(0, child)
        parent.moveTo(-100.ipx, 10.ipx)
        child.moveTo(50.ipx, 80.ipx)

        val actual = child.positionRelativeToRoot()

        assertEquals(PxPosition(-50.px, 90.px), actual)
    }

    @Test
    fun testPositionRelativeToRootIsNotAffectedByOwnerPosition() {
        val parent = LayoutNode()
        parent.attach(mockOwner(PxPosition(20.px, 20.px)))
        val child = LayoutNode()
        parent.emitInsertAt(0, child)
        child.moveTo(50.ipx, 80.ipx)

        val actual = child.positionRelativeToRoot()

        assertEquals(PxPosition(50.px, 80.px), actual)
    }

    @Test
    fun testPositionRelativeToAncestorWithParent() {
        val parent = LayoutNode()
        val child = LayoutNode()
        parent.emitInsertAt(0, child)
        parent.moveTo(-100.ipx, 10.ipx)
        child.moveTo(50.ipx, 80.ipx)

        val actual = child.positionRelativeToAncestor(parent)

        assertEquals(PxPosition(50.px, 80.px), actual)
    }

    @Test
    fun testPositionRelativeToAncestorWithGrandParent() {
        val grandParent = LayoutNode()
        val parent = LayoutNode()
        val child = LayoutNode()
        grandParent.emitInsertAt(0, parent)
        parent.emitInsertAt(0, child)
        grandParent.moveTo(-7.ipx, 17.ipx)
        parent.moveTo(23.ipx, -13.ipx)
        child.moveTo(-3.ipx, 11.ipx)

        val actual = child.positionRelativeToAncestor(grandParent)

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
    fun testRemoveTooMany() {
        val pointerInputNode = PointerInputNode()
        pointerInputNode.emitInsertAt(0, DrawNode())
        thrown.expect(IndexOutOfBoundsException::class.java)
        pointerInputNode.emitRemoveAt(0, 2)
    }

    // ComponentNode should error when there aren't enough items
    @Test
    fun testRemoveTooMany2() {
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

    private fun createSimpleLayout(): Triple<LayoutNode, ComponentNode, ComponentNode> {
        val layoutNode = LayoutNode()
        val child1 = LayoutNode()
        val child2 = LayoutNode()
        layoutNode.emitInsertAt(0, child1)
        layoutNode.emitInsertAt(1, child2)
        return Triple(layoutNode, child1, child2)
    }

    private fun mockOwner(position: PxPosition = PxPosition.Origin): Owner =
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
}
