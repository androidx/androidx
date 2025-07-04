/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.window.PopupProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SpatialPopup]. */
@RunWith(AndroidJUnit4::class)
class SpatialPopupTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatialPopup_dismissOnBackPressTrue_invokesDismissRequest() {
        val showPopup = mutableStateOf(true)

        composeTestRule.setContent {
            TestSetup {
                if (showPopup.value) {
                    SpatialPopup(
                        onDismissRequest = { showPopup.value = false },
                        properties = PopupProperties(dismissOnBackPress = true),
                    ) {
                        Text("Spatial Popup")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Popup").assertExists()

        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()

        composeTestRule.onNodeWithText("Spatial Popup").assertDoesNotExist()
    }

    @Test
    fun spatialPopup_dismissOnBackPressFalse_doesNotInvokeDismissRequest() {
        val showPopup = mutableStateOf(true)

        composeTestRule.setContent {
            TestSetup {
                if (showPopup.value) {
                    SpatialPopup(
                        onDismissRequest = { showPopup.value = false },
                        properties = PopupProperties(dismissOnBackPress = false),
                    ) {
                        Text("Spatial Popup")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Spatial Popup").assertExists()

        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()

        composeTestRule.onNodeWithText("Spatial Popup").assertExists()
    }
}
