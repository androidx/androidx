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
class FindFocusableChildrenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun returnsFirstFocusNodeInModifierChain() {
        lateinit var focusModifier1: FocusModifier
        lateinit var focusModifier2: FocusModifier
        lateinit var focusModifier3: FocusModifier
        // Arrange.
        // layoutNode--focusNode1--focusNode2--focusNode3
        composeTestRule.setContent {
            focusModifier1 = FocusModifier()
            focusModifier2 = FocusModifier()
            focusModifier3 = FocusModifier()
            Box(modifier = focusModifier1 + focusModifier2 + focusModifier3)
        }

        // Act.
        val focusableChildren = runOnIdleCompose {
            focusModifier1.focusNode.focusableChildren()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(focusableChildren).containsExactly(focusModifier2.focusNode)
        }
    }

    @Test
    fun skipsNonFocusNodesAndReturnsFirstFocusNodeInModifierChain() {
        lateinit var focusModifier1: FocusModifier
        lateinit var focusModifier2: FocusModifier
        // Arrange.
        // layoutNode--focusNode1--nonFocusNode--focusNode2
        composeTestRule.setContent {
            focusModifier1 = FocusModifier()
            focusModifier2 = FocusModifier()
            Box(modifier = focusModifier1 + Modifier.drawBackground(Red) + focusModifier2)
        }

        // Act.
        val focusableChildren = runOnIdleCompose {
            focusModifier1.focusNode.focusableChildren()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(focusableChildren).containsExactly(focusModifier2.focusNode)
        }
    }

    @Test
    fun returnsFirstFocusChildOfEachChildLayoutNode() {
        // Arrange.
        // parentLayoutNode--parentFocusNode
        //       |___________________________________
        //       |                                   |
        // childLayoutNode1--focusNode1          childLayoutNode2--focusNode2--focusNode3
        lateinit var parentFocusModifier: FocusModifier
        lateinit var focusModifier1: FocusModifier
        lateinit var focusModifier2: FocusModifier
        lateinit var focusModifier3: FocusModifier
        composeTestRule.setContent {
            parentFocusModifier = FocusModifier()
            focusModifier1 = FocusModifier()
            focusModifier2 = FocusModifier()
            focusModifier3 = FocusModifier()
            Box(modifier = parentFocusModifier) {
                Box(modifier = focusModifier1)
                Box(modifier = focusModifier2 + focusModifier3)
            }
        }

        // Act.
        val focusableChildren = runOnIdleCompose {
            parentFocusModifier.focusNode.focusableChildren()
        }

        // Assert.
        runOnIdleCompose {
            assertThat(focusableChildren).containsExactly(
                focusModifier1.focusNode, focusModifier2.focusNode
            )
        }
    }
}
