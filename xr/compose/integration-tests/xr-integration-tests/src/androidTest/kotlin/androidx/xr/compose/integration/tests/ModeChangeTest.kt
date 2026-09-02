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

package androidx.xr.compose.integration.tests

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.platform.requestFullSpace
import androidx.xr.compose.platform.requestHomeSpace
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterPosition
import androidx.xr.compose.spatial.OrbiterPosition.EdgeAlignment
import androidx.xr.compose.spatial.SpatialElevation
import androidx.xr.compose.spatial.SpatialElevationLevel
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.unit.DpVolumeOffset
import androidx.xr.testutils.XrDeviceTest
import kotlinx.coroutines.launch
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrument tests for the Space Transition CUJ test case in Compose XR.
 *
 * Covers:
 * - FullSpace 3-panel layout validation (Left, Middle, Right panels)
 * - HomeSpace single-panel layout validation
 * - Dynamic space switching between FullSpace and HomeSpace via real XR platform APIs
 *   (LocalSpatialCapabilities, activity.requestHomeSpace, activity.requestFullSpace)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class ModeChangeTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Validates that the HomeSpace single-panel layout renders the space content. */
    @Test
    fun homeSpaceLayout_rendersMainPanelAndControls() {
        composeTestRule.setContent { SpaceChangeAppContent() }

        composeTestRule.onNodeWithText("HomeSpace").assertExists()
        composeTestRule.onNodeWithText("Transition to FullSpace").assertIsDisplayed()
    }

    /**
     * Validates that the FullSpace 3-panel layout renders all panels and controls after transition.
     */
    @Test
    fun fullSpaceLayout_rendersThreePanelsAndControls() {
        Assume.assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            SpatialConfiguration.hasXrSpatialFeature(composeTestRule.activity),
        )

        composeTestRule.setContent { SpaceChangeAppContent() }

        // Transition from initial HomeSpace into FullSpace via requestFullSpace()
        composeTestRule.onNodeWithText("Transition to FullSpace").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("FullSpace")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Left Panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Right Panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("FullSpace").assertExists()
        composeTestRule.onNodeWithText("Transition to HomeSpace").assertIsDisplayed()
    }

    /** Validates dynamic switching between HomeSpace and FullSpace. */
    @Test
    fun dynamicSpaceSwitch_togglesLayout() {
        Assume.assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            SpatialConfiguration.hasXrSpatialFeature(composeTestRule.activity),
        )

        composeTestRule.setContent { SpaceChangeAppContent() }

        // 1. Verify initial HomeSpace state
        composeTestRule.onNodeWithText("HomeSpace").assertExists()
        composeTestRule.onNodeWithText("Transition to FullSpace").assertIsDisplayed()

        // 2. Transition HomeSpace -> FullSpace via requestFullSpace()
        composeTestRule.onNodeWithText("Transition to FullSpace").performClick()

        // 3. Verify FullSpace state controls are active (wait for async OS space transition)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("FullSpace")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Left Panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Right Panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("FullSpace").assertExists()
        composeTestRule.onNodeWithText("Transition to HomeSpace").assertIsDisplayed()

        // 4. Transition FullSpace -> HomeSpace via requestHomeSpace()
        composeTestRule.onNodeWithText("Transition to HomeSpace").performClick()

        // 5. Verify returned to HomeSpace state controls (wait for async OS space transition)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("HomeSpace")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("HomeSpace").assertExists()
        composeTestRule.onNodeWithText("Transition to FullSpace").assertIsDisplayed()
    }

    @Composable
    private fun SpaceChangeAppContent() {
        val activity = LocalActivity.current as? ComponentActivity
        val coroutineScope = rememberCoroutineScope()
        val isSpatialUiEnabled = LocalSpatialCapabilities.current.isSpatialUiEnabled

        if (isSpatialUiEnabled) {
            Subspace {
                SpatialRow {
                    SpatialPanel(modifier = SubspaceModifier.width(180.dp).height(120.dp)) {
                        Text("Left Panel")
                    }
                    SpatialPanel(modifier = SubspaceModifier.width(360.dp).height(240.dp)) {
                        SpacePanelContent(
                            orbiterText = "FullSpace",
                            buttonText = "Transition to HomeSpace",
                            onButtonClick = {
                                coroutineScope.launch { activity?.requestHomeSpace() }
                            },
                        )
                    }
                    SpatialPanel(modifier = SubspaceModifier.width(180.dp).height(120.dp)) {
                        Text("Right Panel")
                    }
                }
            }
        } else {
            SpacePanelContent(
                orbiterText = "HomeSpace",
                buttonText = "Transition to FullSpace",
                onButtonClick = { coroutineScope.launch { activity?.requestFullSpace() } },
            )
        }
    }

    @Composable
    private fun SpacePanelContent(
        orbiterText: String,
        buttonText: String,
        onButtonClick: () -> Unit,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Orbiter(
                    position =
                        OrbiterPosition.TopCenter(
                            EdgeAlignment.Outside,
                            offset = DpVolumeOffset(y = 5.dp),
                        )
                ) {
                    Text(text = orbiterText, fontSize = 20.sp, color = Color.Black)
                }

                SpatialElevation(elevation = SpatialElevationLevel.Level5) {
                    Button(onClick = onButtonClick, shape = RoundedCornerShape(10.dp)) {
                        Text(text = buttonText, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
