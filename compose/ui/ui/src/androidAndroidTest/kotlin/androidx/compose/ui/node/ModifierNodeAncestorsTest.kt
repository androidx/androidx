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
import androidx.compose.ui.focus.FocusTargetNode
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
class ModifierNodeAncestorsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noAncestors() {
        // Arrange.
        val node = object : Modifier.Node() {}
        rule.setContent {
            Box(Modifier.elementOf(node))
        }

        // Act.
        val ancestors = rule.runOnIdle {
            node.ancestors(Nodes.Any)
        }

        // Assert.
        assertThat(ancestors?.trimRootModifierNodes()).isEmpty()
    }

    @Test
    fun noMatchingAncestors() {
        // Arrange.
        val node = object : Modifier.Node() {}
        rule.setContent {
            Box(Modifier.elementOf(node))
        }

        // Act.
        val ancestors = rule.runOnIdle {
            node.ancestors(Nodes.GlobalPositionAware)
        }

        // Assert.
        assertThat(ancestors).isNull()
    }

    @Test
    fun returnsLocalAncestors() {
        // Arrange.
        val (node, localAncestor1, localAncestor2) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                modifier = Modifier
                    .elementOf(localAncestor2)
                    .elementOf(localAncestor1)
                    .elementOf(node)
            )
        }

        // Act.
        val result = rule.runOnIdle {
            node.ancestors(Nodes.Any)
        }

        // Assert.
        assertThat(result?.trimRootModifierNodes())
            .containsExactly(localAncestor1, localAncestor2)
            .inOrder()
    }

    @Test
    fun returnsAncestors() {
        // Arrange.
        val (node, ancestor1, ancestor2, other) = List(4) { object : Modifier.Node() {} }
        val (localAncestor1, localAncestor2) = List(2) { object : Modifier.Node() {} }

        rule.setContent {
            Box(
                modifier = Modifier
                    .elementOf(ancestor2)
                    .elementOf(ancestor1)
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .elementOf(localAncestor2)
                            .elementOf(localAncestor1)
                            .elementOf(node)
                    )
                }
            }
            Box(Modifier.elementOf(other))
        }

        // Act.
        val result = rule.runOnIdle {
            node.ancestors(Nodes.Any)
        }

        // Assert.
        assertThat(result?.trimRootModifierNodes())
            .containsExactly(localAncestor1, localAncestor2, ancestor1, ancestor2)
            .inOrder()
    }

    @Ignore("b/278765590")
    @Test
    fun doesNotReturnUnattachedAncestors() {
        // Arrange.
        val (ancestor4, ancestor2, ancestor3, ancestor1) = List(4) { object : Modifier.Node() {} }
        val (node, localAncestor1) = List(3) { object : Modifier.Node() {} }
        val (localAncestor2, localAncestor3) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                modifier = Modifier
                    .elementOf(ancestor4)
                    .elementOf(ancestor3)
                    .elementOf(ancestor2)
            ) {
                Box(Modifier.elementOf(ancestor1)) {
                    Box(
                        modifier = Modifier
                            .elementOf(localAncestor3)
                            .elementOf(localAncestor2)
                            .elementOf(localAncestor1)
                            .elementOf(node)
                    )
                }
            }
        }
        rule.runOnIdle {
            ancestor3.detach()
            ancestor4.detach()
            localAncestor1.detach()
            localAncestor3.detach()
        }

        // Act.
        val result = rule.runOnIdle {
            node.ancestors(Nodes.Any)
        }

        // Assert.
        assertThat(result?.trimRootModifierNodes())
            .containsExactly(localAncestor2, ancestor1, ancestor2)
            .inOrder()
    }

    @Test
    fun findAncestorsOfType() {
        // Arrange.
        val (ancestor1, ancestor2, ancestor3, ancestor4) = List(4) { FocusTargetNode() }
        val (node, other1, other2, other3) = List(4) { FocusTargetNode() }
        rule.setContent {
            Box(
                Modifier
                    .elementOf(ancestor4)
                    .elementOf(ancestor3)
            ) {
                Box {
                    Box(Modifier.elementOf(other1))
                    Box(
                        Modifier
                            .elementOf(ancestor2)
                            .elementOf(ancestor1)
                    ) {
                        Box(
                            Modifier
                                .elementOf(node)
                                .elementOf(other3)
                        )
                    }
                    Box(Modifier.elementOf(other2))
                }
            }
        }

        // Act.
        val ancestors = rule.runOnIdle {
            node.ancestors(Nodes.FocusTarget)
        }

        // Assert.
        // This test returns all ancestors, even the root focus node. so we drop that one.
        assertThat(ancestors?.dropLast(1))
            .containsExactly(ancestor1, ancestor2, ancestor3, ancestor4)
            .inOrder()
    }
}
