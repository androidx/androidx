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
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class LayoutNodeTest {
    @get:Rule
    val thrown = ExpectedException.none()!!

    // Ensure that attach and detach work properly
    @Test
    fun layoutNodeAttachDetach() {
        val node = LayoutNode()
        assertNull(node.owner)

        val owner = mockOwner()
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
        assertEquals(2, node.children.size)
        assertEquals(child1, node.children[0])
        assertEquals(child2, node.children[1])
        assertEquals(0, child1.children.size)
        assertEquals(0, child2.children.size)

        node.removeAt(index = 0, count = 1)
        assertEquals(1, node.children.size)
        assertEquals(child2, node.children[0])

        node.insertAt(index = 0, instance = child1)
        assertEquals(2, node.children.size)
        assertEquals(child1, node.children[0])
        assertEquals(child2, node.children[1])

        node.removeAt(index = 0, count = 2)
        assertEquals(0, node.children.size)

        val child3 = LayoutNode()
        val child4 = LayoutNode()

        node.insertAt(0, child1)
        node.insertAt(1, child2)
        node.insertAt(2, child3)
        node.insertAt(3, child4)

        assertEquals(4, node.children.size)
        assertEquals(child1, node.children[0])
        assertEquals(child2, node.children[1])
        assertEquals(child3, node.children[2])
        assertEquals(child4, node.children[3])

        node.move(from = 3, count = 1, to = 0)
        assertEquals(4, node.children.size)
        assertEquals(child4, node.children[0])
        assertEquals(child1, node.children[1])
        assertEquals(child2, node.children[2])
        assertEquals(child3, node.children[3])

        node.move(from = 0, count = 2, to = 3)
        assertEquals(4, node.children.size)
        assertEquals(child2, node.children[0])
        assertEquals(child3, node.children[1])
        assertEquals(child4, node.children[2])
        assertEquals(child1, node.children[3])
    }

    // Ensure that attach of a LayoutNode connects all children
    @Test
    fun layoutNodeAttach() {
        val (node, child1, child2) = createSimpleLayout()

        val owner = mockOwner()
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
        val owner = mockOwner()
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
        val owner = mockOwner()
        node.attach(owner)

        node.removeAt(0, 1)
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
        val owner = mockOwner()
        node.attach(owner)

        node.removeAt(0, 1)

        node.insertAt(1, child1)
        assertEquals(owner, node.owner)
        assertEquals(owner, child1.owner)
        assertEquals(owner, child2.owner)

        verify(owner, times(1)).onAttach(node)
        verify(owner, times(2)).onAttach(child1)
        verify(owner, times(1)).onAttach(child2)
    }

    @Test
    fun childAdd() {
        val node = LayoutNode()
        val owner = mockOwner()
        node.attach(owner)
        verify(owner, times(1)).onAttach(node)

        val child = LayoutNode()
        node.insertAt(0, child)
        verify(owner, times(1)).onAttach(child)
        assertEquals(1, node.children.size)
        assertEquals(node, child.parent)
        assertEquals(owner, child.owner)
    }

    @Test
    fun childCount() {
        val node = LayoutNode()
        assertEquals(0, node.children.size)
        node.insertAt(0, LayoutNode())
        assertEquals(1, node.children.size)
    }

    @Test
    fun childGet() {
        val node = LayoutNode()
        val child = LayoutNode()
        node.insertAt(0, child)
        assertEquals(child, node.children[0])
    }

    @Test
    fun noMove() {
        val (layout, child1, child2) = createSimpleLayout()
        layout.move(0, 0, 1)
        assertEquals(child1, layout.children[0])
        assertEquals(child2, layout.children[1])
    }

    @Test
    fun childRemove() {
        val node = LayoutNode()
        val owner = mockOwner()
        node.attach(owner)
        val child = LayoutNode()
        node.insertAt(0, child)
        node.removeAt(index = 0, count = 1)
        verify(owner, times(1)).onDetach(child)
        assertEquals(0, node.children.size)
        assertEquals(null, child.parent)
        assertNull(child.owner)
    }

    // Ensure that depth is as expected
    @Test
    fun depth() {
        val root = LayoutNode()
        val (child, grand1, grand2) = createSimpleLayout()
        root.insertAt(0, child)

        val owner = mockOwner()
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
        layoutNode.insertAt(0, childLayoutNode)

        assertNull(layoutNode.parent)
        assertEquals(layoutNode, childLayoutNode.parent)
        val layoutNodeChildren = layoutNode.children
        assertEquals(1, layoutNodeChildren.size)
        assertEquals(childLayoutNode, layoutNodeChildren[0])

        layoutNode.removeAt(index = 0, count = 1)
        assertNull(childLayoutNode.parent)
    }

    @Test
    fun testLayoutNodeAdd() {
        val (layout, child1, child2) = createSimpleLayout()
        val inserted = LayoutNode()
        layout.insertAt(0, inserted)
        val children = layout.children
        assertEquals(3, children.size)
        assertEquals(inserted, children[0])
        assertEquals(child1, children[1])
        assertEquals(child2, children[2])
    }

    @Test
    fun testLayoutNodeRemove() {
        val (layout, child1, _) = createSimpleLayout()
        val child3 = LayoutNode()
        val child4 = LayoutNode()
        layout.insertAt(2, child3)
        layout.insertAt(3, child4)
        layout.removeAt(index = 1, count = 2)

        val children = layout.children
        assertEquals(2, children.size)
        assertEquals(child1, children[0])
        assertEquals(child4, children[1])
    }

    @Test
    fun testMoveChildren() {
        val (layout, child1, child2) = createSimpleLayout()
        val child3 = LayoutNode()
        val child4 = LayoutNode()
        layout.insertAt(2, child3)
        layout.insertAt(3, child4)

        layout.move(from = 2, to = 1, count = 2)

        val children = layout.children
        assertEquals(4, children.size)
        assertEquals(child1, children[0])
        assertEquals(child3, children[1])
        assertEquals(child4, children[2])
        assertEquals(child2, children[3])

        layout.move(from = 1, to = 3, count = 2)

        assertEquals(4, children.size)
        assertEquals(child1, children[0])
        assertEquals(child2, children[1])
        assertEquals(child3, children[2])
        assertEquals(child4, children[3])
    }

    @Test
    fun testPxGlobalToLocal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(mockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(x0, y0)
        node1.place(x1, y1)

        val globalPosition = PxPosition(250f, 300f)

        val expectedX = globalPosition.x - x0.value.toFloat() - x1.value.toFloat()
        val expectedY = globalPosition.y - y0.value.toFloat() - y1.value.toFloat()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.coordinates.globalToLocal(globalPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testIntPxGlobalToLocal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(mockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(x0, y0)
        node1.place(x1, y1)

        val globalPosition = PxPosition(250f, 300f)

        val expectedX = globalPosition.x - x0.value.toFloat() - x1.value.toFloat()
        val expectedY = globalPosition.y - y0.value.toFloat() - y1.value.toFloat()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.coordinates.globalToLocal(globalPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testPxLocalToGlobal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(mockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(x0, y0)
        node1.place(x1, y1)

        val localPosition = PxPosition(5f, 15f)

        val expectedX = localPosition.x + x0.value.toFloat() + x1.value.toFloat()
        val expectedY = localPosition.y + y0.value.toFloat() + y1.value.toFloat()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.coordinates.localToGlobal(localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testIntPxLocalToGlobal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(mockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x0 = 100.ipx
        val y0 = 10.ipx
        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(x0, y0)
        node1.place(x1, y1)

        val localPosition = PxPosition(5.ipx, 15.ipx)

        val expectedX = localPosition.x + x0.value.toFloat() + x1.value.toFloat()
        val expectedY = localPosition.y + y0.value.toFloat() + y1.value.toFloat()
        val expectedPosition = PxPosition(expectedX, expectedY)

        val result = node1.coordinates.localToGlobal(localPosition)

        assertEquals(expectedPosition, result)
    }

    @Test
    fun testPxLocalToGlobalUsesOwnerPosition() {
        val node = ZeroSizedLayoutNode()
        node.attach(mockOwner(IntPxPosition(20.ipx, 20.ipx)))
        node.place(100.ipx, 10.ipx)

        val result = node.coordinates.localToGlobal(PxPosition.Origin)

        assertEquals(PxPosition(120f, 30f), result)
    }

    @Test
    fun testIntPxLocalToGlobalUsesOwnerPosition() {
        val node = ZeroSizedLayoutNode()
        node.attach(mockOwner(IntPxPosition(20.ipx, 20.ipx)))
        node.place(100.ipx, 10.ipx)

        val result = node.coordinates.localToGlobal(PxPosition.Origin)

        assertEquals(PxPosition(120.ipx, 30.ipx), result)
    }

    @Test
    fun testChildToLocal() {
        val node0 = ZeroSizedLayoutNode()
        node0.attach(mockOwner())
        val node1 = ZeroSizedLayoutNode()
        node0.insertAt(0, node1)

        val x1 = 50.ipx
        val y1 = 80.ipx
        node0.place(100.ipx, 10.ipx)
        node1.place(x1, y1)

        val localPosition = PxPosition(5f, 15f)

        val expectedX = localPosition.x + x1.value.toFloat()
        val expectedY = localPosition.y + y1.value.toFloat()
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
        node0.insertAt(0, node1)
        node1.insertAt(0, node2)

        thrown.expect(IllegalStateException::class.java)

        node2.coordinates.childToLocal(node1.coordinates, PxPosition(5f, 15f))
    }

    @Test
    fun testChildToLocalFailedWhenNotAncestorNoParent() {
        val owner = mockOwner()
        val node0 = LayoutNode()
        node0.attach(owner)
        val node1 = LayoutNode()
        node1.attach(owner)

        thrown.expect(IllegalStateException::class.java)

        node1.coordinates.childToLocal(node0.coordinates, PxPosition(5f, 15f))
    }

    @Test
    fun testChildToLocalTheSameNode() {
        val node = LayoutNode()
        node.attach(mockOwner())
        val position = PxPosition(5f, 15f)

        val result = node.coordinates.childToLocal(node.coordinates, position)

        assertEquals(position, result)
    }

    @Test
    fun testPositionRelativeToRoot() {
        val parent = ZeroSizedLayoutNode()
        parent.attach(mockOwner())
        val child = ZeroSizedLayoutNode()
        parent.insertAt(0, child)
        parent.place(-100.ipx, 10.ipx)
        child.place(50.ipx, 80.ipx)

        val actual = child.coordinates.positionInRoot

        assertEquals(PxPosition(-50.ipx, 90.ipx), actual)
    }

    @Test
    fun testPositionRelativeToRootIsNotAffectedByOwnerPosition() {
        val parent = LayoutNode()
        parent.attach(mockOwner(IntPxPosition(20.ipx, 20.ipx)))
        val child = ZeroSizedLayoutNode()
        parent.insertAt(0, child)
        child.place(50.ipx, 80.ipx)

        val actual = child.coordinates.positionInRoot

        assertEquals(PxPosition(50.ipx, 80.ipx), actual)
    }

    @Test
    fun testPositionRelativeToAncestorWithParent() {
        val parent = ZeroSizedLayoutNode()
        parent.attach(mockOwner())
        val child = ZeroSizedLayoutNode()
        parent.insertAt(0, child)
        parent.place(-100.ipx, 10.ipx)
        child.place(50.ipx, 80.ipx)

        val actual = parent.coordinates.childToLocal(child.coordinates, PxPosition.Origin)

        assertEquals(PxPosition(50f, 80f), actual)
    }

    @Test
    fun testPositionRelativeToAncestorWithGrandParent() {
        val grandParent = ZeroSizedLayoutNode()
        grandParent.attach(mockOwner())
        val parent = ZeroSizedLayoutNode()
        val child = ZeroSizedLayoutNode()
        grandParent.insertAt(0, parent)
        parent.insertAt(0, child)
        grandParent.place(-7.ipx, 17.ipx)
        parent.place(23.ipx, -13.ipx)
        child.place(-3.ipx, 11.ipx)

        val actual = grandParent.coordinates.childToLocal(child.coordinates, PxPosition.Origin)

        assertEquals(PxPosition(20f, -2f), actual)
    }

    // LayoutNode shouldn't allow adding beyond the count
    @Test
    fun testAddBeyondCurrent() {
        val node = LayoutNode()
        thrown.expect(IndexOutOfBoundsException::class.java)
        node.insertAt(1, LayoutNode())
    }

    // LayoutNode shouldn't allow adding below 0
    @Test
    fun testAddBelowZero() {
        val node = LayoutNode()
        thrown.expect(IndexOutOfBoundsException::class.java)
        node.insertAt(-1, LayoutNode())
    }

    // LayoutNode should error when removing at index < 0
    @Test
    fun testRemoveNegativeIndex() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        thrown.expect(IndexOutOfBoundsException::class.java)
        node.removeAt(-1, 1)
    }

    // LayoutNode should error when removing at index > count
    @Test
    fun testRemoveBeyondIndex() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        thrown.expect(IndexOutOfBoundsException::class.java)
        node.removeAt(1, 1)
    }

    // LayoutNode should error when removing at count < 0
    @Test
    fun testRemoveNegativeCount() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        thrown.expect(IllegalArgumentException::class.java)
        node.removeAt(0, -1)
    }

    // LayoutNode should error when removing at count > entry count
    @Test
    fun testRemoveWithIndexBeyondSize() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        thrown.expect(IndexOutOfBoundsException::class.java)
        node.removeAt(0, 2)
    }

    // LayoutNode should error when there aren't enough items
    @Test
    fun testRemoveWithIndexEqualToSize() {
        val node = LayoutNode()
        thrown.expect(IndexOutOfBoundsException::class.java)
        node.removeAt(0, 1)
    }

    // LayoutNode should allow removing two items
    @Test
    fun testRemoveTwoItems() {
        val node = LayoutNode()
        node.insertAt(0, LayoutNode())
        node.insertAt(0, LayoutNode())
        node.removeAt(0, 2)
        assertEquals(0, node.children.size)
    }

    // The layout coordinates of a LayoutNode should be attached when
    // the layout node is attached.
    @Test
    fun coordinatesAttachedWhenLayoutNodeAttached() {
        val layoutNode = LayoutNode()
        val drawModifier = Modifier.drawBehind { }
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

    // The LayoutNodeWrapper should be detached when it has been replaced.
    @Test
    fun layoutNodeWrapperAttachedWhenLayoutNodeAttached() {
        val layoutNode = LayoutNode()
        val drawModifier = Modifier.drawBehind { }

        layoutNode.modifier = drawModifier
        val oldLayoutNodeWrapper = layoutNode.layoutNodeWrapper
        assertFalse(oldLayoutNodeWrapper.isAttached)

        layoutNode.attach(mockOwner())
        assertTrue(oldLayoutNodeWrapper.isAttached)

        layoutNode.modifier = Modifier.drawBehind { }
        val newLayoutNodeWrapper = layoutNode.layoutNodeWrapper
        assertTrue(newLayoutNodeWrapper.isAttached)
        assertFalse(oldLayoutNodeWrapper.isAttached)
    }

    @Test
    fun layoutNodeWrapperParentCoordinates() {
        val layoutNode = LayoutNode()
        val layoutNode2 = LayoutNode()
        val drawModifier = Modifier.drawBehind { }
        layoutNode.modifier = drawModifier
        layoutNode2.insertAt(0, layoutNode)
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
                PointerInputModifierImpl(pointerInputFilter)
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
                PointerInputModifierImpl(pointerInputFilter)
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
                PointerInputModifierImpl(
                    childPointerInputFilter
                )
            )
        val middleLayoutNode: LayoutNode =
            LayoutNode(
                100, 100, 400, 400,
                PointerInputModifierImpl(
                    middlePointerInputFilter
                )
            ).apply {
                insertAt(0, childLayoutNode)
            }
        val parentLayoutNode: LayoutNode =
            LayoutNode(
                0, 0, 500, 500,
                PointerInputModifierImpl(
                    parentPointerInputFilter
                )
            ).apply {
                insertAt(0, middleLayoutNode)
                attach(mockOwner())
            }

        val offset = when (numberOfChildrenHit) {
            3 -> PxPosition(250f, 250f)
            2 -> PxPosition(150f, 150f)
            1 -> PxPosition(50f, 50f)
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
                PointerInputModifierImpl(
                    childPointerInputFilter1
                )
            )

        val childLayoutNode2 =
            LayoutNode(
                50, 50, 100, 100,
                PointerInputModifierImpl(
                    childPointerInputFilter2
                )
            )

        val parentLayoutNode = LayoutNode(0, 0, 100, 100).apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            attach(mockOwner())
        }

        val offset1 = PxPosition(25f, 25f)
        val offset2 = PxPosition(75f, 75f)

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
                PointerInputModifierImpl(
                    childPointerInputFilter1
                )
            )

        val childLayoutNode2 =
            LayoutNode(
                50, 50, 150, 150,
                PointerInputModifierImpl(
                    childPointerInputFilter2
                )
            )

        val childLayoutNode3 =
            LayoutNode(
                100, 100, 200, 200,
                PointerInputModifierImpl(
                    childPointerInputFilter3
                )
            )

        val parentLayoutNode = LayoutNode(0, 0, 200, 200).apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            insertAt(2, childLayoutNode3)
            attach(mockOwner())
        }

        val offset1 = PxPosition(25f, 25f)
        val offset2 = PxPosition(75f, 75f)
        val offset3 = PxPosition(125f, 125f)

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
            PointerInputModifierImpl(
                childPointerInputFilter1
            )
        )
        val childLayoutNode2 = LayoutNode(
            25, 50, 75, 100,
            PointerInputModifierImpl(
                childPointerInputFilter2
            )
        )

        val parentLayoutNode = LayoutNode(0, 0, 150, 150).apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            attach(mockOwner())
        }

        val offset1 = PxPosition(50f, 25f)
        val offset2 = PxPosition(50f, 75f)
        val offset3 = PxPosition(50f, 125f)

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
            PointerInputModifierImpl(
                childPointerInputFilter1
            )
        )
        val childLayoutNode2 = LayoutNode(
            50, 25, 100, 75,
            PointerInputModifierImpl(
                childPointerInputFilter2
            )
        )

        val parentLayoutNode = LayoutNode(0, 0, 150, 150).apply {
            insertAt(0, childLayoutNode1)
            insertAt(1, childLayoutNode2)
            attach(mockOwner())
        }

        val offset1 = PxPosition(25f, 50f)
        val offset2 = PxPosition(75f, 50f)
        val offset3 = PxPosition(125f, 50f)

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
            PointerInputModifierImpl(
                pointerInputFilter1
            )
        )
        val layoutNode2 = LayoutNode(
            2, -1, 4, 1,
            PointerInputModifierImpl(
                pointerInputFilter2
            )
        )
        val layoutNode3 = LayoutNode(
            -1, 2, 1, 4,
            PointerInputModifierImpl(
                pointerInputFilter3
            )
        )
        val layoutNode4 = LayoutNode(
            2, 2, 4, 4,
            PointerInputModifierImpl(
                pointerInputFilter4
            )
        )

        val parentLayoutNode = LayoutNode(1, 1, 4, 4).apply {
            insertAt(0, layoutNode1)
            insertAt(1, layoutNode2)
            insertAt(2, layoutNode3)
            insertAt(3, layoutNode4)
            attach(mockOwner())
        }

        val offsetThatHits1 = PxPosition(1f, 1f)
        val offsetThatHits2 = PxPosition(3f, 1f)
        val offsetThatHits3 = PxPosition(1f, 3f)
        val offsetThatHits4 = PxPosition(3f, 3f)

        val offsetsThatMiss =
            listOf(
                PxPosition(1f, 0f),
                PxPosition(3f, 0f),
                PxPosition(0f, 1f),
                PxPosition(4f, 1f),
                PxPosition(0f, 3f),
                PxPosition(4f, 3f),
                PxPosition(1f, 4f),
                PxPosition(3f, 4f)
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
            PointerInputModifierImpl(
                pointerInputFilter
            )
        ).apply {
            attach(mockOwner(IntPxPosition(1.ipx, 1.ipx)))
        }

        val offsetThatHits1 = PxPosition(2f, 2f)
        val offsetThatHits2 = PxPosition(2f, 1f)
        val offsetThatHits3 = PxPosition(1f, 2f)
        val offsetsThatMiss =
            listOf(
                PxPosition(0f, 0f),
                PxPosition(0f, 1f),
                PxPosition(1f, 0f)
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
            PointerInputModifierImpl(
                pointerInputFilter1
            ) + PointerInputModifierImpl(
                pointerInputFilter2
            ) + PointerInputModifierImpl(
                pointerInputFilter3
            )

        val layoutNode = LayoutNode(
            25, 50, 75, 100,
            modifier
        ).apply {
            attach(mockOwner())
        }

        val offset1 = PxPosition(50f, 75f)

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
                PointerInputModifierImpl(
                    pointerInputFilter
                )
            )
        val layoutNode2: LayoutNode = LayoutNode(2, 6, 500, 500).apply {
            insertAt(0, layoutNode1)
        }
        val layoutNode3: LayoutNode = LayoutNode(3, 7, 500, 500).apply {
            insertAt(0, layoutNode2)
        }
        val layoutNode4: LayoutNode = LayoutNode(4, 8, 500, 500).apply {
            insertAt(0, layoutNode3)
        }.apply {
            attach(mockOwner())
        }
        val offset1 = PxPosition(499f, 499f)

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
            PointerInputModifierImpl(
                pointerInputFilter1
            ) + PointerInputModifierImpl(
                pointerInputFilter2
            )
        )
        val layoutNode2: LayoutNode = LayoutNode(2, 7, 500, 500).apply {
            insertAt(0, layoutNode1)
        }
        val layoutNode3 =
            LayoutNode(
                3, 8, 500, 500,
                PointerInputModifierImpl(
                    pointerInputFilter3
                ) + PointerInputModifierImpl(
                    pointerInputFilter4
                )
            ).apply {
                insertAt(0, layoutNode2)
            }

        val layoutNode4: LayoutNode = LayoutNode(4, 9, 500, 500).apply {
            insertAt(0, layoutNode3)
        }
        val layoutNode5: LayoutNode = LayoutNode(5, 10, 500, 500).apply {
            insertAt(0, layoutNode4)
        }.apply {
            attach(mockOwner())
        }

        val offset1 = PxPosition(499f, 499f)

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
            PointerInputModifierImpl(
                pointerInputFilter1
            )
        )
        val layoutNode2 = LayoutNode(
            0, 0, 100, 100,
            PointerInputModifierImpl(
                pointerInputFilter2
            )
        )

        val parentLayoutNode = LayoutNode(0, 0, 100, 100).apply {
            insertAt(0, layoutNode1)
            insertAt(1, layoutNode2)
            attach(mockOwner())
        }

        val offset = PxPosition(50f, 50f)

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
            PointerInputModifierImpl(
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

    @Test
    fun hitTest_zIndexIsAccounted() {

        val pointerInputFilter1: PointerInputFilter = spy()
        val pointerInputFilter2: PointerInputFilter = spy()

        val parent = LayoutNode(
            0, 0, 2, 2
        ).apply {
            attach(mockOwner())
        }
        parent.insertAt(
            0, LayoutNode(
                0, 0, 2, 2,
                PointerInputModifierImpl(
                    pointerInputFilter1
                ).zIndex(1f)
            )
        )
        parent.insertAt(
            1, LayoutNode(
                0, 0, 2, 2,
                PointerInputModifierImpl(
                    pointerInputFilter2
                )
            )
        )

        val hit = mutableListOf<PointerInputFilter>()

        // Act.

        parent.hitTest(PxPosition(1f, 1f), hit)

        // Assert.

        assertThat(hit).isEqualTo(listOf(pointerInputFilter1))
    }

    @Test
    fun onRequestMeasureIsNotCalledOnDetachedNodes() {
        val root = LayoutNode()

        val node1 = LayoutNode()
        root.add(node1)
        val node2 = LayoutNode()
        node1.add(node2)

        val owner = mockOwner()
        root.attach(owner)
        reset(owner)

        // Dispose
        root.removeAt(0, 1)

        assertFalse(node1.isAttached())
        assertFalse(node2.isAttached())
        verify(owner, times(0)).onRequestMeasure(node1)
        verify(owner, times(0)).onRequestMeasure(node2)
    }

    @Test
    fun updatingModifierToTheEmptyOneClearsReferenceToThePreviousModifier() {
        val root = LayoutNode()
        root.attach(mock {
            on { createLayer(anyOrNull(), anyOrNull(), anyOrNull()) } doReturn mock()
        })

        root.modifier = Modifier.drawLayer()

        assertNotNull(root.innerLayoutNodeWrapper.findLayer())

        root.modifier = Modifier

        assertNull(root.innerLayoutNodeWrapper.findLayer())
    }

    private fun createSimpleLayout(): Triple<LayoutNode, LayoutNode, LayoutNode> {
        val layoutNode = ZeroSizedLayoutNode()
        val child1 = ZeroSizedLayoutNode()
        val child2 = ZeroSizedLayoutNode()
        layoutNode.insertAt(0, child1)
        layoutNode.insertAt(1, child2)
        return Triple(layoutNode, child1, child2)
    }

    private fun mockOwner(
        position: IntPxPosition = IntPxPosition.Origin,
        targetRoot: LayoutNode = LayoutNode()
    ): Owner =
        mock {
            on { calculatePosition() } doReturn position
            on { root } doReturn targetRoot
        }

    private fun LayoutNode(x: Int, y: Int, x2: Int, y2: Int, modifier: Modifier = Modifier) =
        LayoutNode().apply {
            this.modifier = modifier
            layoutDirection = LayoutDirection.Ltr
            resize(x2.ipx - x.ipx, y2.ipx - y.ipx)
            var wrapper: LayoutNodeWrapper? = layoutNodeWrapper
            while (wrapper != null) {
                wrapper.measureResult = innerLayoutNodeWrapper.measureResult
                wrapper = (wrapper as? LayoutNodeWrapper)?.wrapped
            }
            place(x.ipx, y.ipx)
        }

    private fun ZeroSizedLayoutNode() = LayoutNode(0, 0, 0, 0)

    private class PointerInputModifierImpl(override val pointerInputFilter: PointerInputFilter) :
        PointerInputModifier
}
