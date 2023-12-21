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
class ModifierNodeVisitSubtreeIfTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noChildren() {
        // Arrange.
        val node = object : Modifier.Node() {}
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node))
        }

        // Act.
        rule.runOnIdle {
            node.visitSubtreeIf(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).isEmpty()
    }

    @Test
    fun stopsIfWeReturnFalse() {
        // Arrange.
        val (node1, node2, node3) = List(3) { object : Modifier.Node() {} }
        val (node4, node5, node6) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node1)) {
                Box(Modifier.elementOf(node2)) {
                    Box(Modifier.elementOf(node3))
                    Box(Modifier.elementOf(node4)) {
                        Box(Modifier.elementOf(node6))
                    }
                    Box(Modifier.elementOf(node5))
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
        assertThat(visitedChildren)
            .containsExactly(node2, node3, node4, node5)
            .inOrder()
    }

    @Test
    fun continuesIfWeReturnTrue() {
        // Arrange.
        val (node1, node2, node3) = List(3) { object : Modifier.Node() {} }
        val (node4, node5, node6) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node1)) {
                Box(Modifier.elementOf(node2)) {
                    Box(Modifier.elementOf(node3))
                    Box(Modifier.elementOf(node4)) {
                        Box(Modifier.elementOf(node6))
                    }
                    Box(Modifier.elementOf(node5))
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
            .containsExactly(node2, node3, node4, node6, node5)
            .inOrder()
    }

    @Test
    fun visitsItemsAcrossLayoutNodes() {
        // Arrange.
        val (node1, node2, node3, node4, node5) = List(5) { object : Modifier.Node() {} }
        val (node6, node7, node8, node9, node10) = List(5) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(node1)
                    .elementOf(node2)
            ) {
                Box(
                    Modifier
                        .elementOf(node3)
                        .elementOf(node4)
                ) {
                    Box(
                        Modifier
                            .elementOf(node7)
                            .elementOf(node8)
                    )
                }
                Box(
                    Modifier
                        .elementOf(node5)
                        .elementOf(node6)
                ) {
                    Box(
                        Modifier
                            .elementOf(node9)
                            .elementOf(node10)
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

    @Ignore("b/278765590")
    @Test
    fun skipsUnattachedItems() {
        // Arrange.
        val (parent, node, node1, node2, node3) = List(5) { object : Modifier.Node() {} }
        val (node4, node5, node6, node7, node8) = List(5) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(parent)
                    .elementOf(node)
            ) {
                Box(
                    Modifier
                        .elementOf(node1)
                        .elementOf(node2)
                ) {
                    Box(Modifier.elementOf(node3)) {
                        Box(Modifier.elementOf(node4))
                    }
                    Box(Modifier.elementOf(node5))
                }
                Box(Modifier.elementOf(node6)) {
                    Box(
                        Modifier
                            .elementOf(node7)
                            .elementOf(node8)
                    )
                }
            }
        }
        rule.runOnIdle {
            node2.detach()
            node6.detach()
        }

        // Act.
        rule.runOnIdle {
            node.visitSubtreeIf(Nodes.Any) {
                visitedChildren.add(it)
                true
            }
        }

        // Assert.
        assertThat(visitedChildren)
            .containsExactly(node1, node3, node4, node5)
            .inOrder()
    }
}
