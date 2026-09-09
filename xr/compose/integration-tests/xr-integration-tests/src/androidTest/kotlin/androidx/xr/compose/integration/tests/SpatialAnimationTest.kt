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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterPosition
import androidx.xr.compose.spatial.OrbiterPosition.EdgeAlignment
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.draw.alpha
import androidx.xr.compose.subspace.draw.scale
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.assertZPositionInRootIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.unit.DpVolumeOffset
import androidx.xr.testutils.XrDeviceTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrument tests for the Value-Based Animation Exploration CUJ in Compose XR.
 *
 * Covers:
 * - Alpha fade-in/fade-out animations using SubspaceModifier.alpha
 * - Scale growth/collapse animations using SubspaceModifier.scale
 * - Concurrent panel dimension (width, height) and Z-offset animations
 * - Animated Orbiter elevation depth offsets using DpVolumeOffset
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SpatialAnimationTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            SpatialConfiguration.hasXrSpatialFeature(context),
        )
    }

    /**
     * Validates that SubspaceModifier.alpha applies across alpha state changes (fade in and fade
     * out).
     */
    @Test
    fun spatialAnimation_alphaFadeInAndOut_animatesOpacity() {
        var targetAlpha by mutableFloatStateOf(0.0f)

        composeTestRule.setContent {
            val animatedAlpha by
                animateFloatAsState(targetValue = targetAlpha, animationSpec = tween(100))

            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp)
                            .height(150.dp)
                            .alpha(animatedAlpha)
                            .testTag("faded_panel")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Faded in Content", fontSize = 18.sp)
                        Text("Current Alpha: $animatedAlpha")
                    }
                }
            }
        }

        // Note: SubspaceModifier.alpha adjusts visual opacity on the underlying spatial entity
        // without detaching the panel from the semantics hierarchy. We verify that the panel
        // remains mounted and the animated state value transitions to completion.
        // Initial state at alpha = 0.0f (panel remains mounted in hierarchy)
        composeTestRule.onSubspaceNodeWithTag("faded_panel").assertExists()
        composeTestRule.onAllNodesWithText("Faded in Content").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Current Alpha: 0.0").onFirst().assertIsDisplayed()

        // Fade in: Mutate target alpha to 1.0f (full opacity)
        targetAlpha = 1.0f

        composeTestRule.onSubspaceNodeWithTag("faded_panel").assertExists()
        composeTestRule.onAllNodesWithText("Faded in Content").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Current Alpha: 1.0").onFirst().assertIsDisplayed()

        // Fade out: Mutate target alpha back to 0.0f (fully transparent)
        targetAlpha = 0.0f

        composeTestRule.onSubspaceNodeWithTag("faded_panel").assertExists()
        composeTestRule.onAllNodesWithText("Faded in Content").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Current Alpha: 0.0").onFirst().assertIsDisplayed()
    }

    /**
     * Validates scale-based growth and collapse of a side panel triggered by interactive buttons.
     */
    @Test
    fun spatialAnimation_scaleGrowth_expandsAndCollapsesSidePanel() {
        composeTestRule.setContent {
            var isExpanded by remember { mutableStateOf(false) }
            val sidePanelScale by
                animateFloatAsState(
                    targetValue = if (isExpanded) 1.0f else 0.5f,
                    animationSpec = tween(100),
                )

            Subspace {
                SpatialRow {
                    // Main Anchor Panel
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(300.dp).height(200.dp).testTag("main_panel")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Main Panel Content")
                            Button(onClick = { isExpanded = !isExpanded }) {
                                Text(if (isExpanded) "Collapse Side Panel" else "Expand Side Panel")
                            }
                        }
                    }

                    // Scaled Side Panel
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(300.dp)
                                .height(200.dp)
                                .scale(sidePanelScale)
                                .testTag("side_panel")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Side Panel Content", fontSize = 18.sp)
                            Text("Scale: $sidePanelScale")
                        }
                    }
                }
            }
        }

        // Initial state: side panel is scaled to collapsed size (0.5f)
        composeTestRule.onSubspaceNodeWithTag("side_panel").assertExists()
        composeTestRule.onAllNodesWithText("Main Panel Content").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Expand Side Panel").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Scale: 0.5").onFirst().assertIsDisplayed()

        // Click "Expand Side Panel" -> side panel grows to scale 1.0f
        composeTestRule.onAllNodesWithText("Expand Side Panel").onFirst().performClick()

        composeTestRule.onSubspaceNodeWithTag("side_panel").assertExists()
        composeTestRule.onAllNodesWithText("Scale: 1.0").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Collapse Side Panel").onFirst().assertIsDisplayed()

        // Click "Collapse Side Panel" -> side panel collapses back to scale 0.5f
        composeTestRule.onAllNodesWithText("Collapse Side Panel").onFirst().performClick()

        composeTestRule.onSubspaceNodeWithTag("side_panel").assertExists()
        composeTestRule.onAllNodesWithText("Scale: 0.5").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Expand Side Panel").onFirst().assertIsDisplayed()
    }

    /** Validates concurrent animation of width, height, and depth offset on a SpatialPanel. */
    @Test
    fun spatialAnimation_concurrentDimensionAndOffsetAnimation_updatesDimensionsAndOffset() {
        composeTestRule.setContent {
            var isExpanded by remember { mutableStateOf(false) }
            val width by
                animateDpAsState(
                    targetValue = if (isExpanded) 400.dp else 200.dp,
                    animationSpec = tween(50),
                )
            val height by
                animateDpAsState(
                    targetValue = if (isExpanded) 300.dp else 150.dp,
                    animationSpec = tween(50),
                )
            val zOffset by
                animateDpAsState(
                    targetValue = if (isExpanded) (-50).dp else 0.dp,
                    animationSpec = tween(50),
                )

            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(width)
                            .height(height)
                            .offset(z = zOffset)
                            .testTag("animated_panel")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Dimensions: ${width.value}x${height.value}")
                        Button(onClick = { isExpanded = !isExpanded }) {
                            Text(if (isExpanded) "Collapse Panel" else "Expand Panel")
                        }
                    }
                }
            }
        }

        // Assert initial collapsed dimensions and depth offset
        composeTestRule
            .onSubspaceNodeWithTag("animated_panel")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(150.dp)
            .assertZPositionInRootIsEqualTo(0.dp)
        composeTestRule.onAllNodesWithText("Expand Panel").onFirst().assertIsDisplayed()

        // Click "Expand Panel" -> expand to 400x300.dp and -50.dp Z offset
        composeTestRule.onAllNodesWithText("Expand Panel").onFirst().performClick()

        composeTestRule
            .onSubspaceNodeWithTag("animated_panel")
            .assertWidthIsEqualTo(400.dp)
            .assertHeightIsEqualTo(300.dp)
            .assertZPositionInRootIsEqualTo((-50).dp)
        composeTestRule.onAllNodesWithText("Collapse Panel").onFirst().assertIsDisplayed()

        // Click "Collapse Panel" -> return to 200x150.dp and 0.dp Z offset
        composeTestRule.onAllNodesWithText("Collapse Panel").onFirst().performClick()

        composeTestRule
            .onSubspaceNodeWithTag("animated_panel")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(150.dp)
            .assertZPositionInRootIsEqualTo(0.dp)
        composeTestRule.onAllNodesWithText("Expand Panel").onFirst().assertIsDisplayed()
    }

    /** Validates value-based animation of Orbiter elevation depth offset. */
    @Test
    fun spatialAnimation_orbiterElevationAnimation_animatesElevationOffset() {
        composeTestRule.setContent {
            var isElevated by remember { mutableStateOf(false) }
            val elevation by
                animateDpAsState(
                    targetValue = if (isElevated) 80.dp else 10.dp,
                    animationSpec = tween(50),
                )

            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp).height(200.dp).testTag("orbiter_panel")
                ) {
                    Orbiter(
                        position =
                            OrbiterPosition.TopCenter(
                                EdgeAlignment.Outside,
                                offset = DpVolumeOffset(y = 5.dp, z = elevation),
                            )
                    ) {
                        Box(
                            modifier = Modifier.background(Color.DarkGray).padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Animated Orbiter", color = Color.White)
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Orbiter Elevation: ${elevation.value}")
                        Button(onClick = { isElevated = !isElevated }) {
                            Text(if (isElevated) "Lower Orbiter" else "Elevate Orbiter")
                        }
                    }
                }
            }
        }

        // Note: Orbiter is attached as an auxiliary spatial entity to the parent panel.
        // We verify that animating DpVolumeOffset elevation drives state transitions
        // to completion, and that the Orbiter content remains mounted and interactive.
        // Initial state at elevation = 10.dp
        composeTestRule.onAllNodesWithText("Animated Orbiter").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Elevate Orbiter").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Orbiter Elevation: 10.0").onFirst().assertIsDisplayed()

        // Elevate Orbiter -> animate to elevation = 80.dp
        composeTestRule.onAllNodesWithText("Elevate Orbiter").onFirst().performClick()

        composeTestRule.onAllNodesWithText("Animated Orbiter").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Lower Orbiter").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Orbiter Elevation: 80.0").onFirst().assertIsDisplayed()

        // Lower Orbiter -> animate back to elevation = 10.dp
        composeTestRule.onAllNodesWithText("Lower Orbiter").onFirst().performClick()

        composeTestRule.onAllNodesWithText("Animated Orbiter").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Elevate Orbiter").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Orbiter Elevation: 10.0").onFirst().assertIsDisplayed()
    }
}
