/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusTargetNode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DelegatableNodeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun visitChildren_noChildren() {
        // Arrange.
        val testNode = object : Modifier.Node() {}
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(modifier = elementOf { testNode })
        }

        // Act.
        rule.runOnIdle {
            testNode.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).isEmpty()
    }

    @Test
    fun visitChildWithinCurrentLayoutNode_immediateChild() {
        // Arrange.
        val (node1, node2, node3) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .then(elementOf { node2 })
                    .then(elementOf { node3 })
            )
        }

        // Act.
        rule.runOnIdle {
            node1.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(node2)
    }

    @Test
    fun visitChildWithinCurrentLayoutNode_nonContiguousChild() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .otherModifier()
                    .then(elementOf { node2 })
            )
        }

        // Act.
        rule.runOnIdle {
            node1.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(node2)
    }

    @Test
    fun visitChildrenInOtherLayoutNodes() {
        // Arrange.
        val (node1, node2, node3, node4, node5) = List(5) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(modifier = elementOf { node1 }) {
                Box(modifier = elementOf { node2 }) {
                    Box(modifier = elementOf { node3 })
                }
                Box(modifier = elementOf { node4 }) {
                    Box(modifier = elementOf { node5 })
                }
            }
        }

        // Act.
        rule.runOnIdle {
            node1.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(node2, node4).inOrder()
    }

    @Test
    fun visitSubtreeIf_stopsIfWeReturnFalse() {
        // Arrange.
        val (node1, node2, node3) = List(3) { object : Modifier.Node() {} }
        val (node4, node5, node6) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(modifier = elementOf { node1 }) {
                Box(modifier = elementOf { node2 }) {
                    Box(modifier = elementOf { node3 })
                    Box(modifier = elementOf { node4 }) {
                        Box(modifier = elementOf { node6 })
                    }
                    Box(modifier = elementOf { node5 })
                }
            }
        }

        // Act.
        rule.runOnIdle {
            node1.visitSubtreeIf(Nodes.Any) {
                visitedChildren.add(it)
                // return false if we encounter node4
                it != node4
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(node2, node3, node4, node5).inOrder()
    }

    @Test
    fun visitSubtreeIf_continuesIfWeReturnTrue() {
        // Arrange.
        val (node1, node2, node3) = List(3) { object : Modifier.Node() {} }
        val (node4, node5, node6) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(modifier = elementOf { node1 }) {
                Box(modifier = elementOf { node2 }) {
                    Box(modifier = elementOf { node3 })
                    Box(modifier = elementOf { node4 }) {
                        Box(modifier = elementOf { node6 })
                    }
                    Box(modifier = elementOf { node5 })
                }
            }
        }

        // Act.
        rule.runOnIdle {
            node1.visitSubtreeIf(Nodes.Any) {
                visitedChildren.add(it)
                true
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(node2, node3, node4, node6, node5).inOrder()
    }

    @Test
    fun visitSubtree_visitsItemsInCurrentLayoutNode() {
        // Arrange.
        val (node1, node2, node3, node4, node5) = List(6) { object : Modifier.Node() {} }
        val (node6, node7, node8, node9, node10) = List(6) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .then(elementOf { node2 })
            ) {
                Box(
                    modifier = elementOf { node3 }
                        .then(elementOf { node4 })
                ) {
                    Box(
                        modifier = elementOf { node7 }
                            .then(elementOf { node8 })
                    )
                }
                Box(
                    modifier = elementOf { node5 }
                        .then(elementOf { node6 })
                ) {
                    Box(
                        modifier = elementOf { node9 }
                            .then(elementOf { node10 })
                    )
                }
            }
        }

        // Act.
        rule.runOnIdle {
            node1.visitSubtreeIf(Nodes.Any) {
                visitedChildren.add(it)
                true
            }
        }

        // Assert.
        assertThat(visitedChildren)
            .containsExactly(node2, node3, node4, node7, node8, node5, node6, node9, node10)
            .inOrder()
    }

    @Test
    fun visitAncestorWithinCurrentLayoutNode_immediateParent() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .then(elementOf { node2 })
            )
        }

        // Act.
        rule.runOnIdle {
            node2.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors.first()).isEqualTo(node1)
    }

    @Test
    fun visitAncestorWithinCurrentLayoutNode_nonContiguousAncestor() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .border(10.dp, Color.Red)
                    .then(elementOf { node2 })
            )
        }

        // Act.
        rule.runOnIdle {
            node2.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors).contains(node1)
    }

    @Test
    fun visitAncestorsInOtherLayoutNodes() {
        // Arrange.
        val (node1, node2, node3, node4, node5) = List(5) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(modifier = elementOf { node1 }) {
                Box(
                    modifier = Modifier
                        .then(elementOf { node2 })
                        .then(elementOf { node3 })
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .then(elementOf { node4 })
                                .then(elementOf { node5 })
                        )
                    }
                }
            }
        }

        // Act.
        rule.runOnIdle {
            node5.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors)
            .containsAtLeastElementsIn(arrayOf(node4, node3, node2, node1))
            .inOrder()
    }

    @Test
    fun nearestAncestorInDifferentLayoutNode_nonContiguousParentLayoutNode() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(modifier = elementOf { node1 }) {
                Box {
                    Box(modifier = elementOf { node2 })
                }
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            node2.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(parent).isEqualTo(node1)
    }

    @Test
    fun visitAncestors_sameLayoutNode_calledDuringOnDetach() {
        // Arrange.
        val (node1, node2) = List(5) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        val detachableNode = DetachableNode { node ->
            node.visitAncestors(Nodes.Any) { visitedAncestors.add(it) }
        }
        val removeNode = mutableStateOf(false)
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .then(elementOf { node2 })
                    .then(if (removeNode.value) Modifier else elementOf { detachableNode })
            )
        }

        // Act.
        rule.runOnIdle { removeNode.value = true }

        // Assert.
        rule.runOnIdle {
            assertThat(visitedAncestors)
                .containsAtLeastElementsIn(arrayOf(node2, node1))
                .inOrder()
        }
    }

    @Test
    fun visitAncestors_multipleLayoutNodes_calledDuringOnDetach() {
        // Arrange.
        val (node1, node2) = List(5) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        val detachableNode = DetachableNode { node ->
            node.visitAncestors(Nodes.Any) { visitedAncestors.add(it) }
        }
        val removeNode = mutableStateOf(false)
        rule.setContent {
            Box(elementOf { node1 }) {
                Box(elementOf { node2 }) {
                    Box(if (removeNode.value) Modifier else elementOf { detachableNode })
                }
            }
        }

        // Act.
        rule.runOnIdle { removeNode.value = true }

        // Assert.
        rule.runOnIdle {
            assertThat(visitedAncestors)
                .containsAtLeastElementsIn(arrayOf(node2, node1))
                .inOrder()
        }
    }

    @Test
    fun nearestAncestorWithinCurrentLayoutNode_immediateParent() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .then(elementOf { node2 })
            )
        }

        // Act.
        val parent = rule.runOnIdle {
            node2.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(parent).isEqualTo(node1)
    }

    @Test
    fun nearestAncestorWithinCurrentLayoutNode_nonContiguousAncestor() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .otherModifier()
                    .then(elementOf { node2 })
            )
        }

        // Act.
        val parent = rule.runOnIdle {
            node2.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(parent).isEqualTo(node1)
    }

    @Test
    fun nearestAncestorInDifferentLayoutNode_immediateParentLayoutNode() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(modifier = elementOf { node1 }) {
                Box(modifier = elementOf { node2 })
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            node2.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(parent).isEqualTo(node1)
    }

    @Test
    fun findAncestors() {
        // Arrange.
        val (node1, node2, node3, node4) = List(4) { FocusTargetNode() }
        val (node5, node6, node7, node8) = List(4) { FocusTargetNode() }
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .then(elementOf { node2 })
            ) {
                Box {
                    Box(modifier = elementOf { node3 })
                    Box(
                        modifier = elementOf { node4 }
                            .then(elementOf { node5 })
                    ) {
                        Box(
                            modifier = elementOf { node6 }
                                .then(elementOf { node7 })
                        )
                    }
                    Box(modifier = elementOf { node8 })
                }
            }
        }

        // Act.
        val ancestors = rule.runOnIdle {
            node6.ancestors(Nodes.FocusTarget)
        }

        // Assert.
        // This test returns all ancestors, even the root focus node. so we drop that one.
        assertThat(ancestors?.dropLast(1)).containsExactly(node5, node4, node2, node1).inOrder()
    }

    @Test
    fun firstChild_currentLayoutNode() {
        // Arrange.
        val (node1, node2, node3) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .then(elementOf { node2 })
                    .then(elementOf { node3 })
            )
        }

        // Act.
        val child = rule.runOnIdle {
            node1.child
        }

        // Assert.
        assertThat(child).isEqualTo(node2)
    }

    @Test
    fun firstChild_currentLayoutNode_nonContiguousChild() {
        // Arrange.
        val (node1, node2) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                modifier = elementOf { node1 }
                    .otherModifier()
                    .then(elementOf { node2 })
            )
        }

        // Act.
        val child = rule.runOnIdle {
            node1.child
        }

        // Assert.
        assertThat(child).isEqualTo(node2)
    }

    fun firstChild_differentLayoutNode_nonContiguousChild() {
        // Arrange.
        val (node1, node2) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(modifier = elementOf { node1 }) {
                Box {
                    Box(modifier = Modifier.otherModifier()) {
                        Box(modifier = elementOf { node2 })
                    }
                }
            }
        }

        // Act.
        val child = rule.runOnIdle {
            node1.child
        }

        // Assert.
        assertThat(child).isEqualTo(node2)
    }

    @Test
    fun delegatedNodeGetsCoordinator() {
        val node = object : DelegatingNode() {
            val inner = delegate(
                object : Modifier.Node() { }
            )
        }

        rule.setContent {
            Box(modifier = elementOf { node })
        }

        rule.runOnIdle {
            assertThat(node.isAttached).isTrue()
            assertThat(node.coordinator).isNotNull()
            assertThat(node.inner.isAttached).isTrue()
            assertThat(node.inner.coordinator).isNotNull()
            assertThat(node.inner.coordinator).isEqualTo(node.coordinator)
        }
    }

    private fun Modifier.otherModifier(): Modifier = this.then(Modifier)

    private inline fun <reified T : Modifier.Node> elementOf(noinline create: () -> T) =
        ElementOf(create)

    private data class ElementOf<T : Modifier.Node>(
        val factory: () -> T
    ) : ModifierNodeElement<T>() {
        override fun create(): T = factory()
        override fun update(node: T) {}
        override fun InspectorInfo.inspectableProperties() {
            name = "testNode"
        }
    }

    private class DetachableNode(val onDetach: (DetachableNode) -> Unit) : Modifier.Node() {
        override fun onDetach() {
            onDetach.invoke(this)
            super.onDetach()
        }
    }
}
