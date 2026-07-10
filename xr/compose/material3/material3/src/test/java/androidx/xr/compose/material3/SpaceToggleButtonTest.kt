/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.material3

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalSpatialCapabilities
import junit.framework.TestCase.assertTrue
import kotlin.test.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpaceToggleButtonTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private val testTag = "SpaceToggleButton"

    private lateinit var collapseDescription: String
    private lateinit var expandDescription: String

    @Before
    fun setUp() {
        collapseDescription =
            composeTestRule.activity.getString(
                R.string.xr_compose_material3_space_mode_switch_collapse
            )
        expandDescription =
            composeTestRule.activity.getString(
                R.string.xr_compose_material3_space_mode_switch_expand
            )
    }

    @Test
    fun spaceToggleButton_click_changesStateToExpand() {
        var isSpatialUiEnabled = false

        composeTestRule.setContent {
            isSpatialUiEnabled = LocalSpatialCapabilities.current.isSpatialUiEnabled
            SpaceToggleButton(modifier = Modifier.testTag(testTag))
        }

        assertTrue(isSpatialUiEnabled)
        composeTestRule.onNodeWithContentDescription(collapseDescription).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(collapseDescription).performClick()

        composeTestRule.onNodeWithContentDescription(expandDescription).assertIsDisplayed()
        assertFalse(isSpatialUiEnabled)
    }

    @Test
    fun spaceToggleButton_click_changesStateToCollapse() {
        var isSpatialUiEnabled = true

        composeTestRule.setContent {
            isSpatialUiEnabled = LocalSpatialCapabilities.current.isSpatialUiEnabled
            SpaceToggleButton(modifier = Modifier.testTag(testTag))
        }

        // set up HomeSpace
        assertTrue(isSpatialUiEnabled)
        composeTestRule.onNodeWithContentDescription(collapseDescription).performClick()
        composeTestRule.onNodeWithContentDescription(expandDescription).assertIsDisplayed()
        assertFalse(isSpatialUiEnabled)

        composeTestRule.onNodeWithContentDescription(expandDescription).performClick()
        composeTestRule.onNodeWithContentDescription(collapseDescription).assertIsDisplayed()
        assertTrue(isSpatialUiEnabled)
    }

    @Test
    fun spaceToggleButton_customContent_reflectsState() {

        composeTestRule.setContent {
            SpaceToggleButton { isFullSpace ->
                Text(text = if (isFullSpace) "Leave Full Space" else "Leave Home Space")
            }
        }

        composeTestRule.onNodeWithText("Leave Full Space").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leave Full Space").performClick()

        composeTestRule.onNodeWithText("Leave Home Space").assertIsDisplayed()
    }
}
