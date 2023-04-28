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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
class ModifierNodeVisitAncestorsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noAncestors() {
        // Arrange.
        val node = object : Modifier.Node() {}
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node))
        }

        // Act.
        rule.runOnIdle {
            node.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors.trimRootModifierNodes()).isEmpty()
    }

    @Test
    fun localAncestors() {
        // Arrange.
        val (node, localParent1, localParent2) = List(3) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(localParent2)
                    .elementOf(localParent1)
                    .elementOf(node)
            )
        }

        // Act.
        rule.runOnIdle {
            node.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors.trimRootModifierNodes())
            .containsExactly(localParent1, localParent2)
            .inOrder()
    }

    @Test
    fun nonContiguousLocalAncestors() {
        // Arrange.
        val (node, localParent1, localParent2) = List(3) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(localParent2)
                    .otherModifier()
                    .elementOf(localParent1)
                    .otherModifier()
                    .elementOf(node)
            )
        }

        // Act.
        rule.runOnIdle {
            node.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors.trimRootModifierNodes())
            .containsExactly(localParent1, localParent2)
            .inOrder()
    }

    @Test
    fun ancestorsInOtherLayoutNodes() {
        // Arrange.
        val (node, localParent) = List(2) { object : Modifier.Node() {} }
        val (ancestor1, ancestor2, ancestor3) = List(3) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(ancestor3)) {
                Box(
                    Modifier
                        .elementOf(ancestor2)
                        .elementOf(ancestor1)
                ) {
                    Box {
                        Box(
                            Modifier
                                .elementOf(localParent)
                                .elementOf(node)
                        )
                    }
                }
            }
        }

        // Act.
        rule.runOnIdle {
            node.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors.trimRootModifierNodes())
            .containsExactly(localParent, ancestor1, ancestor2, ancestor3)
            .inOrder()
    }

    @Ignore("b/278765590")
    @Test
    fun unattachedAncestorsAreSkipped() {
        // Arrange.
        val node = object : Modifier.Node() {}
        val (localParent1, localParent2, localParent3) = List(3) { object : Modifier.Node() {} }
        val (ancestor1, ancestor2, ancestor3, ancestor4) = List(4) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(ancestor4)) {
                Box(Modifier.elementOf(ancestor3)) {
                    Box(
                        Modifier
                            .elementOf(ancestor2)
                            .elementOf(ancestor1)
                    ) {
                        Box {
                            Box(
                                Modifier
                                    .elementOf(localParent3)
                                    .elementOf(localParent2)
                                    .elementOf(localParent1)
                                    .elementOf(node)
                            )
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            ancestor2.detach()
            ancestor3.detach()
            localParent1.detach()
            localParent3.detach()
        }

        // Act.
        rule.runOnIdle {
            node.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors.trimRootModifierNodes())
            .containsExactly(localParent2, ancestor1, ancestor4)
            .inOrder()
    }

    @Test
    fun localAncestorsAreAvailableDuringOnDetach() {
        // Arrange.
        val (localParent1, localParent2) = List(2) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        val detachableNode = DetachableNode { node ->
            node.visitAncestors(Nodes.Any) { visitedAncestors.add(it) }
        }
        var removeNode by mutableStateOf(false)
        rule.setContent {
            Box(
                modifier = Modifier
                    .elementOf(localParent2)
                    .elementOf(localParent1)
                    .then(if (removeNode) Modifier else Modifier.elementOf(detachableNode))
            )
        }

        // Act.
        rule.runOnIdle { removeNode = true }

        // Assert.
        rule.runOnIdle {
            assertThat(visitedAncestors)
                .containsAtLeastElementsIn(arrayOf(localParent1, localParent2))
                .inOrder()
        }
    }

    @Test
    fun ancestorsAcrossMultipleLayoutNodesAreAvailableDuringOnDetach() {
        // Arrange.
        val (ancestor1, ancestor2, ancestor3, ancestor4) = List(4) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        val detachableNode = DetachableNode { node ->
            node.visitAncestors(Nodes.Any) { visitedAncestors.add(it) }
        }
        var removeNode by mutableStateOf(false)
        rule.setContent {
            Box(Modifier.elementOf(ancestor4)) {
                Box(
                    Modifier
                        .elementOf(ancestor3)
                        .elementOf(ancestor2)
                ) {
                    Box(
                        Modifier
                            .elementOf(ancestor1)
                            .then(
                                if (removeNode) Modifier else Modifier.elementOf(detachableNode)
                            )
                    )
                }
            }
        }

        // Act.
        rule.runOnIdle { removeNode = true }

        // Assert.
        rule.runOnIdle {
            assertThat(visitedAncestors)
                .containsAtLeastElementsIn(arrayOf(ancestor1, ancestor2, ancestor3, ancestor4))
                .inOrder()
        }
    }

    private class DetachableNode(val onDetach: (DetachableNode) -> Unit) : Modifier.Node() {
        override fun onDetach() {
            onDetach.invoke(this)
            super.onDetach()
        }
    }

    private fun Modifier.otherModifier(): Modifier = this.then(Modifier)
}
