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
class ModifierNodeVisitChildrenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noChildren() {
        // Arrange.
        val testNode = object : Modifier.Node() {}
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(testNode))
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
    fun localChildren() {
        // Arrange.
        val (node, child1, child2) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
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
            node.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(child1)
    }

    @Test
    fun nonContiguousLocalChild() {
        // Arrange.
        val (node, child1, child2) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(node)
                    .otherModifier()
                    .elementOf(child1)
                    .otherModifier()
                    .elementOf(child2)
            )
        }

        // Act.
        rule.runOnIdle {
            node.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(child1)
    }

    @Test
    fun visitChildrenInOtherLayoutNodes() {
        // Arrange.
        val (node, child1, child2, child3) = List(4) { object : Modifier.Node() {} }
        val (grandchild1, grandchild2) = List(2) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box(Modifier.elementOf(child1)) {
                    Box(Modifier.elementOf(grandchild1))
                }
                Box(Modifier.elementOf(child2)) {
                    Box(Modifier.elementOf(grandchild2))
                }
                Box(Modifier.elementOf(child3))
            }
        }

        // Act.
        rule.runOnIdle {
            node.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren)
            .containsExactly(child1, child2, child3)
            .inOrder()
    }

    @Ignore("b/278765590")
    @Test
    fun skipsUnattachedLocalChild() {
        // Arrange.
        val (node, child1, child2) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(node)
                    .elementOf(child1)
                    .elementOf(child2)
            )
        }
        rule.runOnIdle {
            child1.detach()
        }

        // Act.
        rule.runOnIdle {
            node.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren)
            .containsExactly(child2)
            .inOrder()
    }

    @Ignore("b/278765590")
    @Test
    fun skipsUnattachedChild() {
        // Arrange.
        val (node, child1, child2, child3) = List(4) { object : Modifier.Node() {} }
        val (child4, child5, child6) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box(Modifier.elementOf(child1))
                Box(Modifier.elementOf(child2))
                Box(Modifier.elementOf(child3)) {
                    Box(Modifier.elementOf(child5))
                    Box(Modifier.elementOf(child6))
                }
                Box(Modifier.elementOf(child4))
            }
        }
        rule.runOnIdle {
            child1.detach()
            child3.detach()
        }

        // Act.
        rule.runOnIdle {
            node.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren)
            .containsExactly(child2, child4)
            .inOrder()
    }

    private fun Modifier.otherModifier(): Modifier = this.then(Modifier)
}