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
class ModifierNodeChildTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noChildren() {
        // Arrange.
        val node = object : Modifier.Node() {}
        rule.setContent {
            Box(Modifier.elementOf(node))
        }

        // Act.
        val result = rule.runOnIdle {
            node.child
        }

        // Assert.
        assertThat(result.toString()).isEqualTo("<tail>")
    }

    @Test
    fun localChild() {
        // Arrange.
        val (node, localChild1, localChild2) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                Modifier.elementOf(node)
                    .elementOf(localChild1)
                    .elementOf(localChild2)
            )
        }

        // Act.
        val result = rule.runOnIdle {
            node.child
        }

        // Assert.
        assertThat(result).isEqualTo(localChild1)
    }

    @Test
    fun nonContiguousChild() {
        // Arrange.
        val (node, localChild) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                Modifier.elementOf(node)
                    .otherModifier()
                    .elementOf(localChild)
            )
        }

        // Act.
        val result = rule.runOnIdle {
            node.child
        }

        // Assert.
        assertThat(result).isEqualTo(localChild)
    }

    @Test
    fun doesNotReturnChildInDifferentLayoutNode() {
        // Arrange.
        val (node, child1, child2) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box(
                    Modifier
                        .elementOf(child1)
                        .elementOf(child2)
                )
            }
        }

        // Act.
        val result = rule.runOnIdle {
            node.child
        }

        // Assert.
        assertThat(result.toString()).isEqualTo("<tail>")
    }

    fun differentLayoutNode_nonContiguousChild() {
        // Arrange.
        val (node, child) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(Modifier.elementOf(node)) {
                Box {
                    Box(Modifier.otherModifier()) {
                        Box(Modifier.elementOf(child))
                    }
                }
            }
        }

        // Act.
        val result = rule.runOnIdle {
            node.child
        }

        // Assert.
        assertThat(result).isEqualTo(child)
    }

    @Ignore("b/278765590")
    @Test
    fun withinCurrentLayoutNode_skipsUnAttachedChild() {
        // Arrange.
        val (node, child1, child2) = List(3) { object : Modifier.Node() {} }
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
        val result = rule.runOnIdle {
            node.child
        }

        // Assert.
        assertThat(result).isEqualTo(child2)
    }

    private fun Modifier.otherModifier(): Modifier = this.then(Modifier)
}
