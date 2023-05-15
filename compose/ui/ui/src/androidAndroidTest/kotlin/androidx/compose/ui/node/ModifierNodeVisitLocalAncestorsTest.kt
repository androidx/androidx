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
class ModifierNodeVisitLocalAncestorsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noParents() {
        // Arrange.
        val node = object : Modifier.Node() {}
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(node))
        }

        // Act.
        rule.runOnIdle {
            node.visitLocalAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors).isEmpty()
    }

    @Test
    fun doesNotVisitOtherLayoutNodes() {
        // Arrange.
        val (node, parent) = List(2) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(Modifier.elementOf(parent)) {
                Box(Modifier.elementOf(node))
            }
        }

        // Act.
        rule.runOnIdle {
            node.visitLocalAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors).isEmpty()
    }

    @Test
    fun multipleAncestors() {
        // Arrange.
        val (node, ancestor1, ancestor2) = List(3) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(ancestor2)
                    .elementOf(ancestor1)
                    .elementOf(node)
            )
        }

        // Act.
        rule.runOnIdle {
            node.visitLocalAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors)
            .containsExactly(ancestor1, ancestor2)
            .inOrder()
    }

    @Test
    fun doesNotVisitChild() {
        // Arrange.
        val (node, child, parent) = List(3) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
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
            node.visitLocalAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors).containsExactly(parent)
    }

    @Test
    fun nonContiguousAncestors() {
        // Arrange.
        val (node, ancestor1, ancestor2) = List(3) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(ancestor2)
                    .otherModifier()
                    .elementOf(ancestor1)
                    .otherModifier()
                    .elementOf(node)
            )
        }

        // Act.
        rule.runOnIdle {
            node.visitLocalAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors)
            .containsExactly(ancestor1, ancestor2)
            .inOrder()
    }

    @Ignore("b/278765590")
    @Test
    fun skipsUnattachedAncestors() {
        // Arrange.
        val (node, ancestor1, ancestor2, ancestor3) = List(4) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                Modifier
                    .elementOf(ancestor3)
                    .elementOf(ancestor2)
                    .elementOf(ancestor1)
                    .elementOf(node)
            )
        }
        rule.runOnIdle {
            ancestor1.detach()
            ancestor3.detach()
        }

        // Act.
        rule.runOnIdle {
            node.visitLocalAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors).containsExactly(ancestor2)
    }

    private fun Modifier.otherModifier(): Modifier = this.then(Modifier)
}
