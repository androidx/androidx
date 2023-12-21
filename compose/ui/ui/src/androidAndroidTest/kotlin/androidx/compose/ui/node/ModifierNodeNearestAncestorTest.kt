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
class ModifierNodeNearestAncestorTest {
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
        val result = rule.runOnIdle {
            node.nearestAncestor(Nodes.GlobalPositionAware)
        }

        // Assert.
        assertThat(result).isNull()
    }

    @Test
    fun nearestAncestorInDifferentLayoutNode_nonContiguousParentLayoutNode() {
        // Arrange.
        val (ancestor, node) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(Modifier.elementOf(ancestor)) {
                Box {
                    Box(Modifier.elementOf(node))
                }
            }
        }

        // Act.
        val result = rule.runOnIdle {
            node.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(result).isEqualTo(ancestor)
    }

    @Test
    fun nearestAncestorWithinCurrentLayoutNode_immediateParent() {
        // Arrange.
        val (ancestor, node) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                Modifier
                    .elementOf(ancestor)
                    .elementOf(node)
            )
        }

        // Act.
        val result = rule.runOnIdle {
            node.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(result).isEqualTo(ancestor)
    }

    @Test
    fun nearestAncestorWithinCurrentLayoutNode_nonContiguousAncestor() {
        // Arrange.
        val (ancestor, node) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                Modifier.elementOf(ancestor)
                    .otherModifier()
                    .elementOf(node)
            )
        }

        // Act.
        val result = rule.runOnIdle {
            node.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(result).isEqualTo(ancestor)
    }

    @Test
    fun nearestAncestorInDifferentLayoutNode_immediateParentLayoutNode() {
        // Arrange.
        val (ancestor, node) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(Modifier.elementOf(ancestor)) {
                Box(Modifier.elementOf(node))
            }
        }

        // Act.
        val result = rule.runOnIdle {
            node.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(result).isEqualTo(ancestor)
    }

    @Ignore("b/278765590")
    @Test
    fun unattachedLocalAncestorIsSkipped() {
        // Arrange.
        val (ancestor1, ancestor2, node) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                Modifier
                    .elementOf(ancestor2)
                    .elementOf(ancestor1)
                    .elementOf(node)
            )
        }
        rule.runOnIdle {
            ancestor1.detach()
        }

        // Act.
        val result = rule.runOnIdle {
            node.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(result).isEqualTo(ancestor2)
    }

    @Ignore("b/278765590")
    @Test
    fun unattachedLocalAncestor_returnsAncestorInParentNode() {
        // Arrange.
        val (ancestor, localAncestor, node) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(Modifier.elementOf(ancestor)) {
                Box {
                    Box(
                        Modifier
                            .elementOf(localAncestor)
                            .elementOf(node)
                    )
                }
            }
        }
        rule.runOnIdle {
            localAncestor.detach()
        }

        // Act.
        val result = rule.runOnIdle {
            node.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(result).isEqualTo(ancestor)
    }

    @Ignore("b/278765590")
    @Test
    fun unattachedAncestorInParentNodeIsSkipped() {
        // Arrange.
        val (ancestor1, ancestor2, node) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                Modifier
                    .elementOf(ancestor2)
                    .elementOf(ancestor1)
            ) {
                Box(Modifier.elementOf(node))
            }
        }
        rule.runOnIdle {
            ancestor1.detach()
        }

        // Act.
        val result = rule.runOnIdle {
            node.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(result).isEqualTo(ancestor2)
    }

    private fun Modifier.otherModifier(): Modifier = this.then(Modifier)
}
