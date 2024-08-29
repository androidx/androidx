/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ModifierNodeVisitSubtreeTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun noChildren() {
        // Arrange.
        val node = object : Modifier.Node() {}
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent { Box(Modifier.elementOf(node)) }

        // Act.
        rule.runOnIdle { node.visitSubtree(Nodes.Any) { visitedChildren.add(it) } }

        // Assert.
        assertThat(visitedChildren).isEmpty()
    }

    @Test
    fun localChildren() {
        // Arrange.
        val (parent, node, localChild1, localChild2) = List(4) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier.elementOf(parent)
                    .elementOf(node)
                    .elementOf(localChild1)
                    .elementOf(localChild2)
            )
        }

        // Act.
        rule.runOnIdle { node.visitSubtree(Nodes.Any) { visitedChildren.add(it) } }

        // Assert.
        assertThat(visitedChildren).containsExactly(localChild1, localChild2).inOrder()
    }

    @Test
    fun differentLayoutNodes() {
        // Arrange.
        val (node, child1, child2, child3, child4) = List(5) { object : Modifier.Node() {} }
        val (child5, child6, child7, child8) = List(4) { object : Modifier.Node() {} }

        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node).elementOf(child1).elementOf(child2)) {
                Box(Modifier.elementOf(child3).elementOf(child4)) {
                    Box(Modifier.elementOf(child5).elementOf(child6))
                }
                Box { Box(Modifier.elementOf(child7).elementOf(child8)) }
            }
        }

        // Act.
        rule.runOnIdle { node.visitSubtree(Nodes.Any) { visitedChildren.add(it) } }

        // Assert.
        assertThat(visitedChildren)
            .containsExactly(child1, child2, child3, child4, child5, child6, child7, child8)
            .inOrder()
    }

    @Test
    fun differentLayoutNodesInDrawOrder_zIndex() {
        // Arrange.
        abstract class TrackedNode : Modifier.Node()
        val (node, child1, child2, child3, child4) = List(5) { object : TrackedNode() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box(Modifier.elementOf(child1))
                Box(Modifier.elementOf(child2).zIndex(10f)) {
                    Box(Modifier.elementOf(child3).zIndex(-10f))
                }
                Box { Box(Modifier.elementOf(child4)) }
            }
        }

        // Act.
        rule.runOnIdle {
            node.visitSubtree(Nodes.Any, zOrder = true) {
                @Suppress("KotlinConstantConditions") if (it is TrackedNode) visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(child1, child4, child2, child3).inOrder()
    }

    @Test
    fun differentLayoutNodesInDrawOrder_subcompose() {
        // Arrange.
        val (node, child1, child2, child3, child4) = List(5) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            ReverseMeasureLayout(
                Modifier.elementOf(node),
                { Box(Modifier.elementOf(child1)) },
                { Box(Modifier.elementOf(child2)) { Box(Modifier.elementOf(child3)) } },
                { Box { Box(Modifier.elementOf(child4)) } }
            )
        }

        // Act.
        rule.runOnIdle { node.visitSubtree(Nodes.Any, zOrder = true) { visitedChildren.add(it) } }

        // Assert.
        assertThat(visitedChildren).containsExactly(child1, child2, child3, child4).inOrder()
    }

    @Ignore("b/278765590")
    @Test
    fun skipsUnattached() {
        // Arrange.
        val (node, localChild1, localChild2) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node).elementOf(localChild1).elementOf(localChild2))
        }
        rule.runOnIdle { localChild1.markAsDetached() }

        // Act.
        rule.runOnIdle { node.visitSubtree(Nodes.Any) { visitedChildren.add(it) } }

        // Assert.
        assertThat(visitedChildren).containsExactly(localChild2)
    }
}
