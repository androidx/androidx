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
import androidx.ui.focus.FocusDetailedState
import androidx.ui.focus.FocusDetailedState.Inactive
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color.Companion.Red
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnUiThread
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
        runOnUiThread {
            // Arrange.
            val focusModifier = createFocusModifier(FocusDetailedState.Inactive).also {
                composeTestRule.setContent { Box(modifier = it) }
            }

            // Act.
            val rootFocusNode = focusModifier.focusNode.findParentFocusNode()!!
                .findParentFocusNode()

            // Assert.
            assertThat(rootFocusNode).isNull()
        }
    }

    @Test
    fun returnsImmediateParentFromModifierChain() {
        runOnUiThread {
            // Arrange.
            // focusNode1--focusNode2--focusNode3--focusNode4--focusNode5
            val modifier1 = createFocusModifier(Inactive)
            val modifier2 = createFocusModifier(Inactive)
            val modifier3 = createFocusModifier(Inactive)
            val modifier4 = createFocusModifier(Inactive)
            val modifier5 = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = modifier1 + modifier2 + modifier3 + modifier4 + modifier5) {}
            }

            // Act.
            val parent = modifier3.focusNode.findParentFocusNode()

            // Assert.
            assertThat(parent).isEqualTo(modifier2.focusNode)
        }
    }

    @Test
    fun returnsImmediateParentFromModifierChain_ignoresNonFocusModifiers() {
        runOnUiThread {
            // Arrange.
            // focusNode1--focusNode2--nonFocusNode--focusNode3
            val modifier1 = createFocusModifier(Inactive)
            val modifier2 = createFocusModifier(Inactive)
            val modifier3 = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = modifier1 + modifier2 + Modifier.drawBackground(Red) + modifier3)
            }

            // Act.
            val parent = modifier3.focusNode.findParentFocusNode()

            // Assert.
            assertThat(parent).isEqualTo(modifier2.focusNode)
        }
    }

    @Test
    fun returnsLastFocusParentFromParentLayoutNode() {
        runOnUiThread {
            // Arrange.
            // parentLayoutNode--parentFocusNode1--parentFocusNode2
            //       |
            // layoutNode--focusNode
            val parentFocusModifier1 = createFocusModifier(Inactive)
            val parentFocusModifier2 = createFocusModifier(Inactive)
            val focusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = parentFocusModifier1 + parentFocusModifier2) {
                    Box(modifier = focusModifier)
                }
            }

            // Act.
            val parent = focusModifier.focusNode.findParentFocusNode()

            // Assert.
            assertThat(parent).isEqualTo(parentFocusModifier2.focusNode)
        }
    }

    @Test
    fun returnsImmediateParent() {
        runOnUiThread {
            // Arrange.
            // grandparentLayoutNode--grandparentFocusNode
            //       |
            // parentLayoutNode--parentFocusNode
            //       |
            // layoutNode--focusNode
            val grandparentFocusModifier = createFocusModifier(Inactive)
            val parentFocusModifier = createFocusModifier(Inactive)
            val focusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = grandparentFocusModifier) {
                    Box(modifier = parentFocusModifier) {
                        Box(modifier = focusModifier)
                    }
                }
            }

            // Act.
            val parent = focusModifier.focusNode.findParentFocusNode()

            // Assert.
            assertThat(parent).isEqualTo(parentFocusModifier.focusNode)
        }
    }

    @Test
    fun ignoresIntermediateLayoutNodesThatDontHaveFocusNodes() {
        runOnUiThread {
            // Arrange.
            // grandparentLayoutNode--grandparentFocusNode
            //       |
            // parentLayoutNode
            //       |
            // layoutNode--focusNode
            val grandparentFocusModifier = createFocusModifier(Inactive)
            val focusModifier = createFocusModifier(Inactive)
            composeTestRule.setContent {
                Box(modifier = grandparentFocusModifier) {
                    Box {
                        Box(modifier = focusModifier)
                    }
                }
            }

            // Act.
            val parent = focusModifier.focusNode.findParentFocusNode()

            // Assert.
            assertThat(parent).isEqualTo(grandparentFocusModifier.focusNode)
        }
    }
}
