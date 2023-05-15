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
class ModifierNodeVisitLocalDescendantsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noChildren() {
        // Arrange.
        val testNode = object : Modifier.Node() {}
        val visitedDescendants = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(testNode))
        }

        // Act.
        rule.runOnIdle {
            testNode.visitLocalDescendants(Nodes.Any) {
                visitedDescendants.add(it)
            }
        }

        // Assert.
        assertThat(visitedDescendants).isEmpty()
    }

    @Test
    fun doesNotVisitOtherLayoutNodes() {
        // Arrange.
        val (node, child) = List(3) { object : Modifier.Node() {} }
        val visitedDescendants = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box(Modifier.elementOf(child))
            }
        }

        // Act.
        rule.runOnIdle {
            node.visitLocalDescendants(Nodes.Any) {
                visitedDescendants.add(it)
            }
        }

        // Assert.
        assertThat(visitedDescendants).isEmpty()
    }

    @Test
    fun multipleDescendants() {
        // Arrange.
        val (node, child1, child2) = List(3) { object : Modifier.Node() {} }
        val visitedDescendants = mutableListOf<Modifier.Node>()
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
            node.visitLocalDescendants(Nodes.Any) {
                visitedDescendants.add(it)
            }
        }

        // Assert.
        assertThat(visitedDescendants)
            .containsExactly(child1, child2)
            .inOrder()
    }

    @Test
    fun doesNotVisitParent() {
        // Arrange.
        val (node, child, parent) = List(3) { object : Modifier.Node() {} }
        val visitedDescendants = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(parent)
                    .elementOf(node)
                    .elementOf(child)
            )
        }

        // Act.
        rule.runOnIdle {
            node.visitLocalDescendants(Nodes.Any) {
                visitedDescendants.add(it)
            }
        }

        // Assert.
        assertThat(visitedDescendants).containsExactly(child)
    }

    @Test
    fun nonContiguousChildren() {
        // Arrange.
        val (node, child1, child2) = List(3) { object : Modifier.Node() {} }
        val visitedDescendants = mutableListOf<Modifier.Node>()
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
            node.visitLocalDescendants(Nodes.Any) {
                visitedDescendants.add(it)
            }
        }

        // Assert.
        assertThat(visitedDescendants)
            .containsExactly(child1, child2)
            .inOrder()
    }

    @Ignore("b/278765590")
    @Test
    fun skipsUnattachedChild() {
        // Arrange.
        val (node, child1, child2, child3, child4) = List(5) { object : Modifier.Node() {} }
        val visitedDescendants = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(node)
                    .elementOf(child1)
                    .elementOf(child2)
                    .elementOf(child3)
                    .elementOf(child4)
            )
        }
        rule.runOnIdle {
            child1.detach()
            child3.detach()
        }

        // Act.
        rule.runOnIdle {
            node.visitLocalDescendants(Nodes.Any) {
                visitedDescendants.add(it)
            }
        }

        // Assert.
        assertThat(visitedDescendants)
            .containsExactly(child2)
            .inOrder()
    }

    private fun Modifier.otherModifier(): Modifier = this.then(Modifier)
}
