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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ModifierNodeVisitSelfAndChildrenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noChildren() {
        // Arrange.
        val node = object : Modifier.Node() {}
        val visitedNodes = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node))
        }

        // Act.
        rule.runOnIdle {
            node.visitSelfAndChildren(Nodes.Any) {
                visitedNodes.add(it)
            }
        }

        // Assert.
        assertThat(visitedNodes).containsExactly(node)
    }

    @Test
    fun oneChild() {
        // Arrange.
        val (node, child) = List(2) { object : Modifier.Node() {} }
        val visitedNodes = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(node)
                    .elementOf(child)
            )
        }

        // Act.
        rule.runOnIdle {
            node.visitSelfAndChildren(Nodes.Any) {
                visitedNodes.add(it)
            }
        }

        // Assert.
        assertThat(visitedNodes)
            .containsExactly(node, child)
            .inOrder()
    }

    @Test
    fun multipleNodesInModifierChain() {
        // Arrange.
        val (node, child1, child2) = List(3) { object : Modifier.Node() {} }
        val visitedNodes = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(node)
                    .elementOf(child1)
                    .elementOf(child2)
            )
        }

        // Act.
        rule.runOnIdle {
            node.visitSelfAndChildren(Nodes.Any) {
                visitedNodes.add(it)
            }
        }

        // Assert.
        assertThat(visitedNodes)
            .containsExactly(node, child1)
            .inOrder()
    }

    @Test
    fun nonContiguousChild() {
        // Arrange.
        val (node, child) = List(2) { object : Modifier.Node() {} }
        val visitedNodes = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(node)
                    .otherModifier()
                    .elementOf(child)
            )
        }

        // Act.
        rule.runOnIdle {
            node.visitSelfAndChildren(Nodes.Any) {
                visitedNodes.add(it)
            }
        }

        // Assert.
        assertThat(visitedNodes)
            .containsExactly(node, child)
            .inOrder()
    }

    @Test
    fun childInDifferentLayoutNode() {
        // Arrange.
        val (node, child) = List(2) { object : Modifier.Node() {} }
        val visitedNodes = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box(Modifier.elementOf(child))
            }
        }

        // Act.
        rule.runOnIdle {
            node.visitSelfAndChildren(Nodes.Any) {
                visitedNodes.add(it)
            }
        }

        // Assert.
        assertThat(visitedNodes)
            .containsExactly(node, child)
            .inOrder()
    }

    @Test
    fun childrenInDifferentLayoutNode() {
        // Arrange.
        val (node, child1, child2) = List(3) { object : Modifier.Node() {} }
        val visitedNodes = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box(Modifier.elementOf(child1))
                Box(Modifier.elementOf(child2))
            }
        }

        // Act.
        rule.runOnIdle {
            node.visitSelfAndChildren(Nodes.Any) {
                visitedNodes.add(it)
            }
        }

        // Assert.
        assertThat(visitedNodes)
            .containsExactly(node, child1, child2)
            .inOrder()
    }

    @Test
    fun childInDifferentLayoutNodeNonContiguous() {
        // Arrange.
        val (node, child) = List(2) { object : Modifier.Node() {} }
        val visitedNodes = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box(Modifier)
                Box(Modifier.otherModifier()) {
                    Box(Modifier.elementOf(child))
                }
            }
        }

        // Act.
        rule.runOnIdle {
            node.visitSelfAndChildren(Nodes.Any) {
                visitedNodes.add(it)
            }
        }

        // Assert.
        assertThat(visitedNodes)
            .containsExactly(node, child)
            .inOrder()
    }

    @Test
    fun childrenInDifferentLayoutNodeNonContiguous() {
        // Arrange.
        val (node, child1, child2, child3) = List(4) { object : Modifier.Node() {} }
        val visitedNodes = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box {
                    Box(Modifier.elementOf(child1))
                }
                Box(Modifier.otherModifier()) {
                    Box(Modifier.elementOf(child2))
                }
                Box(Modifier.elementOf(child3))
            }
        }

        // Act.
        rule.runOnIdle {
            node.visitSelfAndChildren(Nodes.Any) {
                visitedNodes.add(it)
            }
        }

        // Assert.
        assertThat(visitedNodes)
            .containsExactly(node, child1, child2, child3)
            .inOrder()
    }

    @Ignore("b/278765590")
    @Test
    fun skipsUnattachedItems() {
        val (node, child1, child2, child3) = List(4) { object : Modifier.Node() {} }
        val (child4, child5, child6, child7) = List(4) { object : Modifier.Node() {} }
        val visitedNodes = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(node)
                    .elementOf(child1)
            ) {
                Box {
                    Box(
                        Modifier
                            .elementOf(child2)
                            .elementOf(child3)
                    )
                }
                Box(Modifier.otherModifier()) {
                    Box(Modifier.elementOf(child4))
                }
                Box(
                    Modifier
                        .elementOf(child5)
                        .elementOf(child6)
                        .elementOf(child7)
                )
            }
        }
        rule.runOnIdle {
            child1.detach()
            child2.detach()
            child6.detach()
        }

        // Act.
        rule.runOnIdle {
            node.visitSelfAndChildren(Nodes.Any) {
                visitedNodes.add(it)
            }
        }

        // Assert.
        assertThat(visitedNodes)
            .containsExactly(node, child3, child4, child5, child7)
            .inOrder()
    }

    private fun Modifier.otherModifier(): Modifier = this.then(Modifier)
}