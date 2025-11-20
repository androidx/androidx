/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.subspace.node

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.AndroidComposeSpatialElement
import androidx.xr.compose.testing.SubspaceTestingActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceDepthSortedSet]. */
@RunWith(AndroidJUnit4::class)
class SubspaceDepthSortedSetTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun subspaceDepthSortedSet_rootIsFirst_returnsTrueAlways() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, childNode)

        subspaceDepthSortedSet.add(childNode)
        subspaceDepthSortedSet.add(rootNode)

        assertThat(subspaceDepthSortedSet.first()).isEqualTo(rootNode)
    }

    @Test
    fun subspaceDepthSortedSet_addingAndRemovingChild_returnsTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, childNode)

        subspaceDepthSortedSet.add(childNode)

        assertThat(subspaceDepthSortedSet.contains(childNode)).isTrue()

        subspaceDepthSortedSet.remove(childNode)

        assertThat(subspaceDepthSortedSet.isEmpty()).isTrue()
    }

    @Test
    fun subspaceDepthSortedSet_clearSet_returnsAnEmptySet() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, childNode)

        subspaceDepthSortedSet.add(childNode)
        subspaceDepthSortedSet.add(rootNode)
        subspaceDepthSortedSet.clear()

        assertThat(subspaceDepthSortedSet.isEmpty()).isTrue()
    }

    @Test
    fun subspaceDepthSortedSet_addOrderDoesNotAffectSorting_returnsTrue() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, parentNode)
        parentNode.insertAt(0, childNode)

        subspaceDepthSortedSet.add(parentNode)
        subspaceDepthSortedSet.add(childNode)

        assertThat(subspaceDepthSortedSet.removeFirst()).isEqualTo(parentNode)
        assertThat(subspaceDepthSortedSet.removeFirst()).isEqualTo(childNode)

        subspaceDepthSortedSet.add(childNode)
        subspaceDepthSortedSet.add(parentNode)

        assertThat(subspaceDepthSortedSet.removeFirst()).isEqualTo(parentNode)
        assertThat(subspaceDepthSortedSet.removeFirst()).isEqualTo(childNode)
        assertThat(subspaceDepthSortedSet.isEmpty()).isTrue()
    }

    @Test
    fun drain_processesAllElementsInOrderAndEmptiesSet() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val set = SubspaceDepthSortedSet()

        // Create a hierarchy: root (depth 0) -> parent (depth 1) -> child (depth 2)
        rootNode.attach(owner)
        rootNode.insertAt(0, parentNode)
        parentNode.insertAt(0, childNode)

        // Add nodes in a non-sorted order to ensure drain respects the sorting
        set.add(childNode)
        set.add(rootNode)
        set.add(parentNode)

        assertThat(set.size).isEqualTo(3)

        val processedNodes = mutableListOf<SubspaceLayoutNode>()

        // Act: Drain the set, collecting each node as it's processed.
        set.drain { node -> processedNodes.add(node) }

        // Assert: The set should now be empty.
        assertThat(set.isEmpty()).isTrue()

        // Assert: The nodes should have been processed in depth-sorted order.
        assertThat(processedNodes).containsExactly(rootNode, parentNode, childNode).inOrder()
    }

    @Test
    fun removeFirst_removesAndReturnsSmallestElement() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, parentNode)
        parentNode.insertAt(0, childNode)

        // Add in a non-sorted order
        subspaceDepthSortedSet.add(childNode)
        subspaceDepthSortedSet.add(rootNode)
        subspaceDepthSortedSet.add(parentNode)

        assertThat(subspaceDepthSortedSet.size).isEqualTo(3)

        // pollFirst should return the root node (depth 0) and remove it.
        val first = subspaceDepthSortedSet.removeFirst()
        assertThat(first).isEqualTo(rootNode)
        assertThat(subspaceDepthSortedSet.size).isEqualTo(2)
        assertThat(subspaceDepthSortedSet.contains(rootNode)).isFalse()

        // The next pollFirst should return the parent node (depth 1).
        val second = subspaceDepthSortedSet.removeFirst()
        assertThat(second).isEqualTo(parentNode)
        assertThat(subspaceDepthSortedSet.size).isEqualTo(1)
        assertThat(subspaceDepthSortedSet.contains(parentNode)).isFalse()
    }

    @Test
    fun removeLast_removesAndReturnsLargestElement() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val subspaceDepthSortedSet = SubspaceDepthSortedSet()
        rootNode.attach(owner)
        rootNode.insertAt(0, parentNode)
        parentNode.insertAt(0, childNode)

        // Add in a non-sorted order
        subspaceDepthSortedSet.add(childNode)
        subspaceDepthSortedSet.add(rootNode)
        subspaceDepthSortedSet.add(parentNode)

        assertThat(subspaceDepthSortedSet.size).isEqualTo(3)

        // pollLast should return the child node (deepest) and remove it.
        val last = subspaceDepthSortedSet.removeLast()
        assertThat(last).isEqualTo(childNode)
        assertThat(subspaceDepthSortedSet.size).isEqualTo(2)
        assertThat(subspaceDepthSortedSet.contains(childNode)).isFalse()

        // The next pollLast should return the parent node.
        val secondLast = subspaceDepthSortedSet.removeLast()
        assertThat(secondLast).isEqualTo(parentNode)
        assertThat(subspaceDepthSortedSet.size).isEqualTo(1)
        assertThat(subspaceDepthSortedSet.contains(parentNode)).isFalse()
    }

    @Test
    fun add_withExtraAssertions_throwsIfDepthChanges() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val set = SubspaceDepthSortedSet(extraAssertions = true)
        rootNode.attach(owner)
        rootNode.insertAt(0, parentNode)

        // Add the node when its depth is 1
        set.add(parentNode)
        assertThat(parentNode.depth).isEqualTo(1)

        // Change the node's depth by moving it in the tree
        rootNode.removeAt(0, 1) // Detach from root
        rootNode.insertAt(0, childNode) // root -> child
        childNode.insertAt(0, parentNode) // root -> child -> parent
        assertThat(parentNode.depth).isEqualTo(2) // Depth is now 2

        // Act & Assert: Trying to add the same node again should fail the depth check
        val exception =
            assertThrows(IllegalStateException::class.java) {
                // This add will check the existing entry's depth against the new depth
                set.add(parentNode)
            }
        assertThat(exception).hasMessageThat().isEqualTo("invalid node depth")
    }

    @Test
    fun remove_withExtraAssertions_throwsIfDepthChanges() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()
        val set = SubspaceDepthSortedSet(extraAssertions = true)
        rootNode.attach(owner)
        rootNode.insertAt(0, parentNode)

        // Add the node when its depth is 1
        set.add(parentNode)
        assertThat(parentNode.depth).isEqualTo(1)

        // Change the node's depth by moving it in the tree
        rootNode.removeAt(0, 1) // Detach from root
        rootNode.insertAt(0, childNode) // root -> child
        childNode.insertAt(0, parentNode) // root -> child -> parent
        assertThat(parentNode.depth).isEqualTo(2) // Depth is now 2

        // Act & Assert: Removing the node whose depth has changed should fail the check
        val exception = assertThrows(IllegalStateException::class.java) { set.remove(parentNode) }
        assertThat(exception).hasMessageThat().isEqualTo("invalid node depth")
    }

    @Test
    fun remove_doesNotCauseFailureOnSubsequentContainsCheck() {
        val owner = AndroidComposeSpatialElement()
        val node = SubspaceLayoutNode()
        val set = SubspaceDepthSortedSet(extraAssertions = true)
        node.attach(owner)

        // 1. Add the node to the set.
        set.add(node)
        assertThat(set.contains(node)).isTrue()
        assertThat(set.size).isEqualTo(1)

        // 2. Remove the node.
        val wasRemoved = set.remove(node)
        assertThat(wasRemoved).isTrue()
        assertThat(set.size).isEqualTo(0)

        // 3. Verify that contains() now returns false and does not throw an exception.
        // This confirms the internal state was cleaned up correctly.
        val stillContains = set.contains(node)
        assertThat(stillContains).isFalse()
    }

    @Test
    fun remove_nodeNotInSet_doesNotFail() {
        val owner = AndroidComposeSpatialElement()
        val nodeInSet = SubspaceLayoutNode()
        val nodeNotInSet = SubspaceLayoutNode()
        val set = SubspaceDepthSortedSet(extraAssertions = true)
        nodeInSet.attach(owner)
        nodeNotInSet.attach(owner)

        // Add one node to the set
        set.add(nodeInSet)
        assertThat(set.size).isEqualTo(1)

        // Act: Try to remove a node that was never added
        val wasRemoved = set.remove(nodeNotInSet)

        // Assert: The operation should fail gracefully
        assertThat(wasRemoved).isFalse()
        assertThat(set.size).isEqualTo(1)
        assertThat(set.contains(nodeInSet)).isTrue()
    }

    @Test
    fun contains_nodeNotInSet_doesNotFail() {
        val owner = AndroidComposeSpatialElement()
        val nodeInSet = SubspaceLayoutNode()
        val nodeNotInSet = SubspaceLayoutNode()
        val set = SubspaceDepthSortedSet(extraAssertions = true)
        nodeInSet.attach(owner)
        nodeNotInSet.attach(owner)

        // Add one node to the set
        set.add(nodeInSet)

        // Act: Check for the presence of a node that was never added
        val isPresent = set.contains(nodeNotInSet)

        // Assert: The check should return false and not throw
        assertThat(isPresent).isFalse()
    }

    @Test
    fun iteratorRemove_withExtraAssertions_maintainsConsistency() {
        val owner = AndroidComposeSpatialElement()
        val node = SubspaceLayoutNode()
        val set = SubspaceDepthSortedSet(extraAssertions = true)
        node.attach(owner)
        set.add(node)
        assertThat(set.size).isEqualTo(1)

        // 1. Get an iterator for the set.
        val iterator = set.iterator()

        // 2. Use the iterator to remove the element.
        // In a correct implementation, this should trigger the overridden remove() logic.
        while (iterator.hasNext()) {
            if (iterator.next() == node) {
                iterator.remove()
            }
        }

        // 3. Assert that the set is now empty.
        assertThat(set.size).isEqualTo(0)
        assertThat(set.isEmpty()).isTrue()

        // 4. Assert that a subsequent check for the removed element works correctly.
        // This is the step that will FAIL with the current implementation, as it will
        // throw an IllegalStateException instead of returning false.
        assertThat(set.contains(node)).isFalse()
    }

    @Test
    fun clear_withExtraAssertions_maintainsConsistency() {
        val owner = AndroidComposeSpatialElement()
        val node = SubspaceLayoutNode()
        val set = SubspaceDepthSortedSet(extraAssertions = true)
        node.attach(owner)
        set.add(node)

        assertThat(set.size).isEqualTo(1)

        // 1. Clear the set. This currently only clears the backing set.
        set.clear()

        // 2. Assert that the set is now empty.
        assertThat(set.isEmpty()).isTrue()

        // 3. Assert that a subsequent check for the removed element works correctly.
        // This is the step that will FAIL with the current implementation, as it will
        // throw an IllegalStateException ("inconsistency in TreeSet") instead of
        // returning false.
        assertThat(set.contains(node)).isFalse()
    }

    @Test
    fun removeAll_returnsEmptySet() {
        val owner = AndroidComposeSpatialElement()
        val root = owner.root
        val parent = SubspaceLayoutNode()
        val child = SubspaceLayoutNode()
        val set = SubspaceDepthSortedSet(extraAssertions = true)

        root.insertAt(0, parent)
        parent.insertAt(0, child)
        set.add(root)
        set.add(parent)
        set.add(child)

        assertThat(set.size).isEqualTo(3)

        set.removeAll { true }

        assertThat(set.isEmpty()).isTrue()
    }
}
