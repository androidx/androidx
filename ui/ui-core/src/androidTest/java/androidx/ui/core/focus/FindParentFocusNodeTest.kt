/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.focus

import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color.Companion.Red
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class FindParentFocusNodeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noParentReturnsNull() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            focusModifier = FocusModifier()
            Box(modifier = focusModifier)
        }

        // Act.
        val rootFocusNode = runOnIdleCompose {
            focusModifier.focusNode.findParentFocusNode()!!.findParentFocusNode()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(rootFocusNode).isNull()
        }
    }

    @Test
    fun returnsImmediateParentFromModifierChain() {
        // Arrange.
        // focusNode1--focusNode2--focusNode3--focusNode4--focusNode5
        lateinit var modifier1: FocusModifier
        lateinit var modifier2: FocusModifier
        lateinit var modifier3: FocusModifier
        lateinit var modifier4: FocusModifier
        lateinit var modifier5: FocusModifier
        composeTestRule.setFocusableContent {
            modifier1 = FocusModifier()
            modifier2 = FocusModifier()
            modifier3 = FocusModifier()
            modifier4 = FocusModifier()
            modifier5 = FocusModifier()
            Box(modifier = modifier1 + modifier2 + modifier3 + modifier4 + modifier5) {}
        }

        // Act.
        val parent = runOnIdleCompose {
            modifier3.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(parent).isEqualTo(modifier2.focusNode)
        }
    }

    @Test
    fun returnsImmediateParentFromModifierChain_ignoresNonFocusModifiers() {
        // Arrange.
        // focusNode1--focusNode2--nonFocusNode--focusNode3
        lateinit var modifier1: FocusModifier
        lateinit var modifier2: FocusModifier
        lateinit var modifier3: FocusModifier
        composeTestRule.setFocusableContent {
            modifier1 = FocusModifier()
            modifier2 = FocusModifier()
            modifier3 = FocusModifier()
            Box(modifier = modifier1 + modifier2 + Modifier.drawBackground(Red) + modifier3)
        }

        // Act.
        val parent = runOnIdleCompose {
            modifier3.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(parent).isEqualTo(modifier2.focusNode)
        }
    }

    @Test
    fun returnsLastFocusParentFromParentLayoutNode() {
        // Arrange.
        // parentLayoutNode--parentFocusNode1--parentFocusNode2
        //       |
        // layoutNode--focusNode
        lateinit var parentFocusModifier1: FocusModifier
        lateinit var parentFocusModifier2: FocusModifier
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            parentFocusModifier1 = FocusModifier()
            parentFocusModifier2 = FocusModifier()
            focusModifier = FocusModifier()
            Box(modifier = parentFocusModifier1 + parentFocusModifier2) {
                Box(modifier = focusModifier)
            }
        }

        // Act.
        val parent = runOnIdleCompose {
            focusModifier.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(parent).isEqualTo(parentFocusModifier2.focusNode)
        }
    }

    @Test
    fun returnsImmediateParent() {
        // Arrange.
        // grandparentLayoutNode--grandparentFocusNode
        //       |
        // parentLayoutNode--parentFocusNode
        //       |
        // layoutNode--focusNode
        lateinit var grandparentFocusModifier: FocusModifier
        lateinit var parentFocusModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            grandparentFocusModifier = FocusModifier()
            parentFocusModifier = FocusModifier()
            focusModifier = FocusModifier()
            Box(modifier = grandparentFocusModifier) {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = focusModifier)
                }
            }
        }

        // Act.
        val parent = runOnIdleCompose {
            focusModifier.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(parent).isEqualTo(parentFocusModifier.focusNode)
        }
    }

    @Test
    fun ignoresIntermediateLayoutNodesThatDontHaveFocusNodes() {
        // Arrange.
        // grandparentLayoutNode--grandparentFocusNode
        //       |
        // parentLayoutNode
        //       |
        // layoutNode--focusNode
        lateinit var grandparentFocusModifier: FocusModifier
        lateinit var focusModifier: FocusModifier
        composeTestRule.setFocusableContent {
            grandparentFocusModifier = FocusModifier()
            focusModifier = FocusModifier()
            Box(modifier = grandparentFocusModifier) {
                Box {
                    Box(modifier = focusModifier)
                }
            }
        }

        // Act.
        val parent = runOnIdleCompose {
            focusModifier.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(parent).isEqualTo(grandparentFocusModifier.focusNode)
        }
    }
}
