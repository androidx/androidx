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

package androidx.ui.test

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.compose.foundation.selection.ToggleableState
import androidx.compose.foundation.semantics.inMutuallyExclusiveGroup
import androidx.compose.foundation.semantics.selected
import androidx.compose.foundation.semantics.toggleableState
import androidx.compose.foundation.layout.Column
import androidx.ui.semantics.SemanticsPropertyReceiver
import androidx.ui.semantics.hidden
import androidx.ui.semantics.testTag
import org.junit.Rule
import org.junit.Test

class AssertsTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun assertIsNotHidden_forVisibleElement_isOk() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test" }
        }

        onNodeWithTag("test")
            .assertIsNotHidden()
    }

    @Test(expected = AssertionError::class)
    fun assertIsNotHidden_forHiddenElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; hidden() }
        }

        onNodeWithTag("test")
            .assertIsNotHidden()
    }

    @Test
    fun assertIsHidden_forHiddenElement_isOk() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; hidden() }
        }

        onNodeWithTag("test")
            .assertIsHidden()
    }

    @Test(expected = AssertionError::class)
    fun assertIsHidden_forNotHiddenElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test" }
        }

        onNodeWithTag("test")
            .assertIsHidden()
    }

    @Test
    fun assertIsOn_forCheckedElement_isOk() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; toggleableState = ToggleableState.On }
        }

        onNodeWithTag("test")
            .assertIsOn()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOn_forUncheckedElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; toggleableState = ToggleableState.Off }
        }

        onNodeWithTag("test")
            .assertIsOn()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOn_forNotToggleableElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test" }
        }

        onNodeWithTag("test")
            .assertIsOn()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOff_forCheckedElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; toggleableState = ToggleableState.On }
        }

        onNodeWithTag("test")
            .assertIsOff()
    }

    @Test
    fun assertIsOff_forUncheckedElement_isOk() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; toggleableState = ToggleableState.Off }
        }

        onNodeWithTag("test")
            .assertIsOff()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOff_forNotToggleableElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; }
        }

        onNodeWithTag("test")
            .assertIsOff()
    }

    @Test(expected = AssertionError::class)
    fun assertIsSelected_forNotSelectedElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; selected = false }
        }

        onNodeWithTag("test")
            .assertIsSelected()
    }

    @Test
    fun assertIsSelected_forSelectedElement_isOk() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; selected = true }
        }

        onNodeWithTag("test")
            .assertIsSelected()
    }

    @Test(expected = AssertionError::class)
    fun assertIsSelected_forNotSelectableElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; }
        }

        onNodeWithTag("test")
            .assertIsSelected()
    }

    @Test(expected = AssertionError::class)
    fun assertIsUnselected_forSelectedElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; selected = true }
        }

        onNodeWithTag("test")
            .assertIsUnselected()
    }

    @Test
    fun assertIsUnselected_forUnselectedElement_isOk() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; selected = false }
        }

        onNodeWithTag("test")
            .assertIsUnselected()
    }

    @Test(expected = AssertionError::class)
    fun assertIsUnselected_forNotSelectableElement_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; }
        }

        onNodeWithTag("test")
            .assertIsUnselected()
    }
    @Test(expected = AssertionError::class)
    fun assertItemInExclusiveGroup_forItemNotInGroup_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; inMutuallyExclusiveGroup = false }
        }

        onNodeWithTag("test")
            .assertIsInMutuallyExclusiveGroup()
    }

    @Test(expected = AssertionError::class)
    fun assertItemInExclusiveGroup_forItemWithoutProperty_throwsError() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test" }
        }

        onNodeWithTag("test")
            .assertIsInMutuallyExclusiveGroup()
    }

    @Test
    fun assertItemInExclusiveGroup_forItemInGroup_isOk() {
        composeTestRule.setContent {
            BoundaryNode { testTag = "test"; inMutuallyExclusiveGroup = true }
        }

        onNodeWithTag("test")
            .assertIsInMutuallyExclusiveGroup()
    }

    @Composable
    fun BoundaryNode(props: (SemanticsPropertyReceiver.() -> Unit)) {
        Column(Modifier.semantics(properties = props)) {}
    }
}