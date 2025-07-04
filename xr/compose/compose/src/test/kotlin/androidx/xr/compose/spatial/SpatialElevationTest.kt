/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.spatial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.scenecore.scene
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialElevationTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatialElevation_mainContent_isComposed() {
        composeTestRule.setContent {
            TestSetup {
                SpatialElevation {
                    Box(modifier = Modifier.size(100.dp).testTag("MainContent")) {
                        Text("Main Content")
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("MainContent").assertExists()
    }

    @Test
    fun spatialElevation_popup_doesNotThrowError() {
        composeTestRule.setContent { TestSetup { SpatialElevation { Popup { Text("Popup") } } } }

        composeTestRule.onNodeWithText("Popup").assertExists()
    }

    @Test
    fun spatialElevation_xrNotSupported_doesNotThrowError() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) { SpatialElevation { Popup { Text("Popup") } } }
        }

        composeTestRule.onNodeWithText("Popup").assertExists()
    }

    @Test
    fun spatialElevation_homeSpaceMode_doesNotElevate() {
        composeTestRule.setContent {
            TestSetup {
                Parent { SpatialElevation { Text("Main Content") } }
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onParent().onChild().assertTextContains("Main Content")
    }

    @Test
    fun spatialElevation_fullSpaceMode_doesElevate() {
        composeTestRule.setContent {
            TestSetup {
                LocalSession.current?.scene?.requestFullSpaceMode()
                Parent { SpatialElevation { Text("Main Content") } }
            }
        }

        composeTestRule.onParent().onChild().assertDoesNotExist()
        composeTestRule.onNodeWithText("Main Content").assertExists()
    }
}
