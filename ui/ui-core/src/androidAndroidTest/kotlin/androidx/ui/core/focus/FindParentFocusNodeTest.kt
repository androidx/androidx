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

import androidx.compose.foundation.Box
import androidx.compose.foundation.background
import androidx.test.filters.SmallTest
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdle
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(b/161299807): Migrate this test to use the new focus API.
@Suppress("DEPRECATION")
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
        val rootFocusNode = runOnIdle {
            focusModifier.focusNode.findParentFocusNode()!!.findParentFocusNode()
        }

        // Assert.
        runOnIdle {
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
            Box(modifier1.then(modifier2).then(modifier3).then(modifier4).then(modifier5)) {}
        }

        // Act.
        val parent = runOnIdle {
            modifier3.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdle {
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
            Box(
                modifier = modifier1
                    .then(modifier2)
                    .background(color = Red)
                    .then(modifier3)
            )
        }

        // Act.
        val parent = runOnIdle {
            modifier3.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdle {
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
            Box(modifier = parentFocusModifier1.then(parentFocusModifier2)) {
                Box(modifier = focusModifier)
            }
        }

        // Act.
        val parent = runOnIdle {
            focusModifier.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdle {
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
        val parent = runOnIdle {
            focusModifier.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdle {
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
        val parent = runOnIdle {
            focusModifier.focusNode.findParentFocusNode()
        }

        // Assert.
        runOnIdle {
            assertThat(parent).isEqualTo(grandparentFocusModifier.focusNode)
        }
    }
}
