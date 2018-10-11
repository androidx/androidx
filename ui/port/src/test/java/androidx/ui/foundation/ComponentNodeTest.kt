/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.foundation

import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.LayoutNode.Companion.measure
import androidx.ui.rendering.box.BoxConstraints
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

@RunWith(JUnit4::class)
class ComponentNodeTest {
    @get:Rule
    val thrown = ExpectedException.none()!!

    // Ensure that attach and detach work properly
    @Test
    fun componentNodeAttachDetach() {
        val node = LayoutNode { _, _ -> Size(0.0, 0.0) }
        assertFalse(node.isAttached)
        assertNull(node.owner)

        val owner = mock(Owner::class.java)
        node.attach(owner)
        assertTrue(node.isAttached)
        assertEquals(owner, node.owner)

        verify(owner, times(1)).onAttach(node)

        node.detach()
        assertFalse(node.isAttached)
        assertNull(node.owner)
        verify(owner, times(1)).onDetach(node)
    }

    // Ensure that LayoutNode's children are ordered properly through add, remove, move
    @Test
    fun layoutNodeChildrenOrder() {
        val (node, child1, child2) = createSimpleLayout()
        assertEquals(2, node.size)
        assertEquals(child1, node[0])
        assertEquals(child2, node[1])
        assertEquals(0, child1.size)
        assertEquals(0, child2.size)

        node.remove(index = 0, count = 1)
        assertEquals(1, node.size)
        assertEquals(child2, node[0])

        node.add(index = 0, child = child1)
        assertEquals(2, node.size)
        assertEquals(child1, node[0])
        assertEquals(child2, node[1])

        node.remove(index = 0, count = 2)
        assertEquals(0, node.size)

        val child3 = DrawNode {}
        val child4 = DrawNode {}

        node.add(0, child1)
        node.add(1, child2)
        node.add(2, child3)
        node.add(3, child4)

        assertEquals(4, node.size)
        assertEquals(child1, node[0])
        assertEquals(child2, node[1])
        assertEquals(child3, node[2])
        assertEquals(child4, node[3])

        node.move(from = 3, count = 1, to = 0)
        assertEquals(4, node.size)
        assertEquals(child4, node[0])
        assertEquals(child1, node[1])
        assertEquals(child2, node[2])
        assertEquals(child3, node[3])

        node.move(from = 0, count = 2, to = 2)
        assertEquals(4, node.size)
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
        assertTrue(node.isAttached)
        assertTrue(child1.isAttached)
        assertTrue(child2.isAttached)
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

        assertFalse(node.isAttached)
        assertFalse(child1.isAttached)
        assertFalse(child2.isAttached)
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

        node.remove(0, 1)
        assertTrue(node.isAttached)
        assertFalse(child1.isAttached)
        assertTrue(child2.isAttached)
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

        node.remove(0, 1)
        reset(owner)

        node.add(1, child1)
        assertTrue(node.isAttached)
        assertTrue(child1.isAttached)
        assertTrue(child2.isAttached)
        assertEquals(owner, node.owner)
        assertEquals(owner, child1.owner)
        assertEquals(owner, child2.owner)

        verify(owner, times(0)).onAttach(node)
        verify(owner, times(1)).onAttach(child1)
        verify(owner, times(0)).onAttach(child2)
    }

    @Test
    fun drawNodeChildSizes() {
        val node = DrawNode {}
        assertEquals(0, node.size)
    }

    @Test
    fun drawNodeGet() {
        thrown.expect(IllegalArgumentException::class.java)
        val node = DrawNode {}
        node[0]
    }

    @Test
    fun drawNodeAdd() {
        thrown.expect(UnsupportedOperationException::class.java)
        val node = DrawNode {}
        node.add(0, DrawNode {})
    }

    @Test
    fun drawNodeMove() {
        thrown.expect(UnsupportedOperationException::class.java)
        val node = DrawNode {}
        node.move(from = 0, to = 0, count = 0)
    }

    @Test
    fun drawNodeRemove() {
        thrown.expect(IllegalArgumentException::class.java)
        val node = DrawNode {}
        node.remove(index = 0, count = 0)
    }

    @Test
    fun singleChildAdd() {
        val node = GestureNode()
        val owner = mock(Owner::class.java)
        node.attach(owner)
        verify(owner, times(1)).onAttach(node)

        val child = DrawNode {}
        node.add(0, child)
        verify(owner, times(1)).onAttach(child)
        assertEquals(1, node.size)
        assertEquals(node, child.parent)
        assertTrue(child.isAttached)
    }

    @Test
    fun singleChildSizes() {
        val node = GestureNode()
        assertEquals(0, node.size)
        node.add(0, GestureNode())
        assertEquals(1, node.size)
    }

    @Test
    fun singleChildeGet() {
        val node = GestureNode()
        val child = GestureNode()
        node.add(0, child)
        assertEquals(child, node[0])
    }

    @Test
    fun singleChildMove() {
        thrown.expect(UnsupportedOperationException::class.java)
        val node = GestureNode()
        node.add(0, GestureNode())
        node.move(from = 0, to = 0, count = 0)
    }

    @Test
    fun singleChildRemove() {
        val node = GestureNode()
        val owner = mock(Owner::class.java)
        node.attach(owner)
        val child = DrawNode {}
        node.add(0, child)
        node.remove(index = 0, count = 1)
        verify(owner, times(1)).onDetach(child)
        assertEquals(0, node.size)
        assertEquals(null, child.parent)
        assertFalse(child.isAttached)
    }

    // Ensure that depth is as expected
    @Test
    fun depth() {
        val root = LayoutNode { _, _ -> Size(0.0, 0.0) }
        val (child, grand1, grand2) = createSimpleLayout()
        root.add(0, child)

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
        val layoutNode = LayoutNode { _, _ -> Size(0.0, 0.0) }
        val childLayoutNode = LayoutNode { _, _ -> Size(0.0, 0.0) }
        layoutNode.add(0, childLayoutNode)

        val owner = mock(Owner::class.java)
        layoutNode.attach(owner)

        assertNull(layoutNode.parentLayoutNode)
        assertEquals(layoutNode, childLayoutNode.parentLayoutNode)
        assertEquals(1, layoutNode.layoutChildren.size)
        assertEquals(childLayoutNode, layoutNode.layoutChildren[childLayoutNode])
        assertEquals(0, childLayoutNode.layoutChildren.size)

        layoutNode.remove(index = 0, count = 1)
        assertNull(childLayoutNode.parentLayoutNode)
        assertEquals(0, layoutNode.layoutChildren.size)
    }

    // layoutNode hierarchy should be set properly when a LayoutNode is a grandchild of a LayoutNode
    @Test
    fun indirectLayoutNodeHierarchy() {
        val layoutNode = LayoutNode { _, _ -> Size(0.0, 0.0) }
        val intermediate = GestureNode()
        val childLayoutNode = LayoutNode { _, _ -> Size(0.0, 0.0) }
        layoutNode.add(0, intermediate)
        intermediate.add(0, childLayoutNode)

        val owner = mock(Owner::class.java)
        layoutNode.attach(owner)

        assertNull(layoutNode.parentLayoutNode)
        assertEquals(layoutNode, childLayoutNode.parentLayoutNode)
        assertEquals(1, layoutNode.layoutChildren.size)
        assertEquals(childLayoutNode, layoutNode.layoutChildren[intermediate])
        assertEquals(0, childLayoutNode.layoutChildren.size)

        intermediate.remove(index = 0, count = 1)
        assertNull(childLayoutNode.parentLayoutNode)
        assertEquals(0, layoutNode.layoutChildren.size)
    }

    // Test visitChildren() for LayoutNode and a SingleChildNode
    @Test
    fun visitChildren() {
        val (node1, node2, node3) = createSimpleLayout()
        val node4 = GestureNode()
        node3.add(0, node4)
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
    fun requestLayout() {
        val (node, _, _) = createSimpleLayout()
        val owner = mock(Owner::class.java)
        node.attach(owner)
        verify(owner, times(1)).onRequestLayout(node)
        LayoutNode.measure(node, BoxConstraints.tightFor(0.0, 0.0), false)
        reset(owner)

        node.dirtyLayout()
        verify(owner, times(1)).onRequestLayout(node)

        reset(owner)

        // Shouldn't do anything the second time since it already needs a layout
        node.dirtyLayout()
        verify(owner, times(0)).onRequestLayout(node)
    }

    @Test
    fun testLayout() {
        val (parent, child) = createNestedLayout()
        val owner = mock(Owner::class.java)
        parent.attach(owner)
        val constraints = BoxConstraints.tightFor(width = 20.0, height = 25.0)
        LayoutNode.measure(parent, constraints, true)
        assertEquals(constraints, parent.constraints)
        val childConstraints = BoxConstraints.tightFor(width = 10.0, height = 15.0)
        assertEquals(childConstraints, child.constraints)
        assertTrue(parent.parentUsesSize)
        assertFalse(child.parentUsesSize)

        assertEquals(20, parent.width)
        assertEquals(25, parent.height)
        assertEquals(10, child.width)
        assertEquals(15, child.height)
        assertEquals(5, child.x)
        assertEquals(4, child.y)
        assertEquals(0, parent.x)
        assertEquals(0, parent.y)

        verify(owner, times(1)).onSizeChange(child)
        verify(owner, times(1)).onSizeChange(parent)
        verify(owner, times(1)).onPositionChange(child)
    }

    @Test
    fun testLayoutNodeAdd() {
        val (layout, child1, child2) = createSimpleLayout()
        val inserted = DrawNode {}
        layout.add(0, inserted)
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
        val child3 = DrawNode {}
        val child4 = DrawNode {}
        layout.add(2, child3)
        layout.add(3, child4)
        layout.remove(index = 1, count = 2)

        val children = mutableListOf<ComponentNode>()
        layout.visitChildren { children.add(it) }
        assertEquals(2, children.size)
        assertEquals(child1, children[0])
        assertEquals(child4, children[1])
    }

    @Test
    fun testMoveChildren() {
        val (layout, child1, child2) = createSimpleLayout()
        val child3 = DrawNode {}
        val child4 = DrawNode {}
        layout.add(2, child3)
        layout.add(3, child4)

        layout.move(from = 2, to = 1, count = 2)

        val children = mutableListOf<ComponentNode>()
        layout.visitChildren { children.add(it) }
        assertEquals(4, children.size)
        assertEquals(child1, children[0])
        assertEquals(child3, children[1])
        assertEquals(child4, children[2])
        assertEquals(child2, children[3])

        layout.move(from = 1, to = 2, count = 2)

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
        val node = DrawNode {}
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

    private fun createSimpleLayout(): Triple<LayoutNode, ComponentNode, ComponentNode> {
        val layoutNode = LayoutNode { _, _ -> Size(0.0, 0.0) }
        val child1 = LayoutNode { _, _ -> Size(0.0, 0.0) }
        val child2 = LayoutNode { _, _ -> Size(0.0, 0.0) }
        layoutNode.add(0, child1)
        layoutNode.add(1, child2)
        return Triple(layoutNode, child1, child2)
    }

    private fun createNestedLayout(): Pair<LayoutNode, LayoutNode> {
        val parent = LayoutNode { c, _ ->
            val child = layoutChildren.values.first()!!
            c as BoxConstraints
            val constraints = BoxConstraints.tightFor(c.minWidth - 10.0, c.minHeight - 10.0)
            measure(child, constraints, false)
            position(child, 5, 4)
            Size(c.minWidth, c.minHeight)
        }
        val child = LayoutNode { c, _ ->
            c as BoxConstraints
            Size(c.minWidth, c.minHeight)
        }
        parent.add(0, child)
        return Pair(parent, child)
    }
}
