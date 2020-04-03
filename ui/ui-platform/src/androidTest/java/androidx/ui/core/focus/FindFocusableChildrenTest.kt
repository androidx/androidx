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
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color.Companion.Red
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class FindFocusableChildrenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun returnsFirstFocusNodeInModifierChain() {
        composeTestRule.runOnUiThread {
            // Arrange.
            // layoutNode--focusNode1--focusNode2--focusNode3
            val focusModifier1 = createFocusModifier(FocusDetailedState.Inactive)
            val focusModifier2 = createFocusModifier(FocusDetailedState.Inactive)
            val focusModifier3 = createFocusModifier(FocusDetailedState.Inactive)
            composeTestRule.setContent {
                Box(modifier = focusModifier1 + focusModifier2 + focusModifier3)
            }

            // Act.
            val focusableChildren = focusModifier1.focusNode.focusableChildren()

            // Assert.
            Truth.assertThat(focusableChildren).containsExactly(focusModifier2.focusNode)
        }
    }

    @Test
    fun skipsNonFocusNodesAndReturnsFirstFocusNodeInModifierChain() {
        composeTestRule.runOnUiThread {
            // Arrange.
            // layoutNode--focusNode1--nonFocusNode--focusNode2
            val focusModifier1 = createFocusModifier(FocusDetailedState.Inactive)
            val focusModifier2 = createFocusModifier(FocusDetailedState.Inactive)
            composeTestRule.setContent {
                Box(modifier = focusModifier1 + Modifier.drawBackground(Red) + focusModifier2)
            }

            // Act.
            val focusableChildren = focusModifier1.focusNode.focusableChildren()

            // Assert.
            Truth.assertThat(focusableChildren).containsExactly(focusModifier2.focusNode)
        }
    }

    @Test
    fun returnsFirstFocusChildOfEachChildLayoutNode() {
        composeTestRule.runOnUiThread {
            // Arrange.
            // parentLayoutNode--parentFocusNode
            //       |___________________________________
            //       |                                   |
            // childLayoutNode1--focusNode1          childLayoutNode2--focusNode2--focusNode3
            val parentFocusModifier = createFocusModifier(FocusDetailedState.Inactive)
            val focusModifier1 = createFocusModifier(FocusDetailedState.Inactive)
            val focusModifier2 = createFocusModifier(FocusDetailedState.Inactive)
            val focusModifier3 = createFocusModifier(FocusDetailedState.Inactive)
            composeTestRule.setContent {
                Box(modifier = parentFocusModifier) {
                    Box(modifier = focusModifier1)
                    Box(modifier = focusModifier2 + focusModifier3)
                }
            }

            // Act.
            val focusableChildren = parentFocusModifier.focusNode.focusableChildren()

            // Assert.
            Truth.assertThat(focusableChildren)
                .containsExactly(focusModifier1.focusNode, focusModifier2.focusNode)
        }
    }
}
