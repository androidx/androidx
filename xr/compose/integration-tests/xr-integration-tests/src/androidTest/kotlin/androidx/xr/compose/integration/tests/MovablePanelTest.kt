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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.platform.requestFullSpace
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.MovePolicy
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrument tests for the Movable Modifier CUJ test case in Compose XR.
 *
 * Covers:
 * - Subspace layout placement, bounds, and 2D content rendering for movable panels
 * - Movable modifier configuration with default system move policy (SystemMovePolicy)
 * - Movable modifier with observer callbacks and custom move policy
 * - Real 3D spatial move raycast interaction and translation via SpatialInteractionHelper
 * - Multi-axis (X and Y) translation via spatial move affordance (DragBar) manipulation
 * - Verification that disabled movable panels do not expose move affordances
 * - Dynamic enabling/disabling of the movable modifier
 * - Custom move policy callbacks and dynamic layout offset updates
 * - Scale with distance (depth scaling) configuration
 * - Spatial layout hierarchy stability when containing movable panels (SpatialRow)
 * - Multi-entity independence and isolation for sibling movable panels
 * - 3D translation across all spatial axes (X, Y, and Z depth)
 * - Dynamic resizing while preserving movable modifier state and position
 * - SpatialMainPanel host window positioning and movable modifier support
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class MovablePanelTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            SpatialConfiguration.hasXrSpatialFeature(context),
        )
        resetSpatialInteraction()
    }

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    /**
     * Validates that a SpatialPanel with default system MovePolicy renders and exists in Subspace.
     */
    @Test
    fun movablePanel_defaultSystemPolicy_rendersInSubspace() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp)
                            .height(200.dp)
                            .offset(0.dp, 0.dp, (-100).dp)
                            .movable(enabled = true, movePolicy = MovePolicy.Default)
                            .testTag("system_movable_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("System Movable Content")
                    }
                }
            }
        }

        // Verify Compose Subspace node existence, bounds, and initial position
        composeTestRule
            .onSubspaceNodeWithTag("system_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(200.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, (-100).dp)

        // Verify 2D content is displayed
        composeTestRule.onAllNodesWithText("System Movable Content").onFirst().assertIsDisplayed()
    }

    @Test
    fun movablePanel_systemMovePolicy_withObserverCallback() {
        val observedEventCount = AtomicInteger(0)

        composeTestRule.setContent {
            val systemPolicy = remember {
                MovePolicy.system(scaleWithDistance = false) { _ ->
                    observedEventCount.incrementAndGet()
                }
            }

            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp)
                            .height(200.dp)
                            .offset(0.dp, 0.dp, (-100).dp)
                            .movable(enabled = true, movePolicy = systemPolicy)
                            .testTag("observed_system_movable_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Observed System Movable Content")
                    }
                }
            }
        }

        runBlocking(Dispatchers.Default) {
            composeTestRule.activity.requestFullSpace()
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onSubspaceNodeWithTag("observed_system_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(200.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, (-100).dp)

        composeTestRule
            .onAllNodesWithText("Observed System Movable Content")
            .onFirst()
            .assertIsDisplayed()

        val panelNode = capturePanelNode()
        assertTrue(panelNode.isMovable, "Expected panel node to have isMovable = true")
        val initialPosition = panelNode.position

        // Perform 3D spatial move interaction using MOVE_DELTA
        panelNode.performSpatialInteraction {
            moveNodeBy(delta = Vector3(0.25f, 0f, 0f))
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(
            conditionDescription =
                "Observer callback was not triggered after spatial move interaction",
            timeoutMillis = 5000L,
        ) {
            observedEventCount.get() > 0
        }

        val updatedPosition = waitUntilPanelMoves(initialPosition)

        assertTrue(
            updatedPosition.x > initialPosition.x,
            "Expected panel to translate in positive X direction, but initial X was ${initialPosition.x} and updated X was ${updatedPosition.x}",
        )
    }

    @Test
    fun movablePanel_customMovePolicy_updatesLayoutOffset() {
        var panelOffsetX by mutableStateOf(0.dp)
        var panelOffsetY by mutableStateOf(0.dp)

        composeTestRule.setContent {
            val density = LocalDensity.current
            val customPolicy = remember {
                MovePolicy.custom(scaleWithDistance = false) { event ->
                    val deltaX = event.pose.translation.x - event.previousPose.translation.x
                    val deltaY = event.pose.translation.y - event.previousPose.translation.y
                    with(density) {
                        panelOffsetX += deltaX.toDp()
                        panelOffsetY += deltaY.toDp()
                    }
                }
            }

            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(250.dp)
                            .height(180.dp)
                            .offset(panelOffsetX, panelOffsetY, (-100).dp)
                            .movable(enabled = true, movePolicy = customPolicy)
                            .testTag("custom_movable_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Custom Movable Content")
                    }
                }
            }
        }

        runBlocking(Dispatchers.Default) {
            composeTestRule.activity.requestFullSpace()
        }
        composeTestRule.waitForIdle()

        // Verify initial subspace bounds and position
        composeTestRule
            .onSubspaceNodeWithTag("custom_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(250.dp)
            .assertHeightIsEqualTo(180.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, (-100).dp)

        // Verify 2D content is displayed
        composeTestRule.onAllNodesWithText("Custom Movable Content").onFirst().assertIsDisplayed()

        val panelNode = capturePanelNode()
        assertTrue(panelNode.isMovable, "Expected panel node to have isMovable = true")

        // Perform 3D spatial move interaction to trigger the custom move policy callback
        panelNode.performSpatialInteraction {
            moveNodeBy(delta = Vector3(0.20f, 0.15f, 0f))
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(
            conditionDescription = "Custom move policy callback did not update offsets",
            timeoutMillis = 5000L,
        ) {
            panelOffsetX != 0.dp || panelOffsetY != 0.dp
        }

        // Verify updated position in Subspace layout matches the policy state holder
        composeTestRule
            .onSubspaceNodeWithTag("custom_movable_panel")
            .assertExists()
            .assertPositionInRootIsEqualTo(panelOffsetX, panelOffsetY, (-100).dp)
    }

    /** Validates that disabling the movable modifier preserves bounds and hierarchy. */
    @Test
    fun movablePanel_disabled_preservesBoundsAndHierarchy() {
        var isMovableEnabled by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(400.dp)
                            .height(300.dp)
                            .offset(0.dp, 50.dp, (-150).dp)
                            .movable(enabled = isMovableEnabled)
                            .testTag("toggle_movable_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Toggle Movable Panel")
                    }
                }
            }
        }

        // Verify initial state with movable enabled
        composeTestRule
            .onSubspaceNodeWithTag("toggle_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(400.dp)
            .assertHeightIsEqualTo(300.dp)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp, (-150).dp)

        // Toggle movable to false
        isMovableEnabled = false
        composeTestRule.waitForIdle()

        // Verify bounds and position are preserved after disabling movable
        composeTestRule
            .onSubspaceNodeWithTag("toggle_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(400.dp)
            .assertHeightIsEqualTo(300.dp)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp, (-150).dp)

        // Toggle movable back to true
        isMovableEnabled = true
        composeTestRule.waitForIdle()

        // Verify bounds and position remain consistent
        composeTestRule
            .onSubspaceNodeWithTag("toggle_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(400.dp)
            .assertHeightIsEqualTo(300.dp)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp, (-150).dp)
    }

    /** Validates that a movable SpatialPanel inside a SpatialRow maintains layout hierarchy. */
    @Test
    fun movablePanel_inSpatialRow_preservesLayoutHierarchy() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(modifier = SubspaceModifier.offset(0.dp, 0.dp, (-100).dp)) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .movable(enabled = true)
                                .testTag("row_movable_panel")
                    ) {
                        Text("Row Movable Panel")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .testTag("row_static_panel")
                    ) {
                        Text("Row Static Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("row_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(150.dp)
            .assertPositionInRootIsEqualTo((-100).dp, 0.dp, (-100).dp)

        composeTestRule
            .onSubspaceNodeWithTag("row_static_panel")
            .assertExists()
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(150.dp)
            .assertPositionInRootIsEqualTo(100.dp, 0.dp, (-100).dp)

        composeTestRule.onAllNodesWithText("Row Movable Panel").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Row Static Panel").onFirst().assertIsDisplayed()
    }

    /** Validates that MovePolicy.system with scaleWithDistance preserves subspace layout bounds. */
    @Test
    fun movablePanel_systemMovePolicy_scaleWithDistance_preservesLayoutBounds() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp)
                            .height(200.dp)
                            .offset(0.dp, 0.dp, (-150).dp)
                            .movable(
                                enabled = true,
                                movePolicy = MovePolicy.system(scaleWithDistance = true),
                            )
                            .testTag("scale_with_distance_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Scale With Distance Content")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("scale_with_distance_panel")
            .assertExists()
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(200.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, (-150).dp)

        composeTestRule
            .onAllNodesWithText("Scale With Distance Content")
            .onFirst()
            .assertIsDisplayed()
    }

    /** Validates that multiple movable panels maintain independent positions and state. */
    @Test
    fun movablePanel_multiplePanels_maintainIndependentPositions() {
        var panelTwoOffsetX by mutableStateOf(150.dp)
        var panelTwoOffsetY by mutableStateOf(0.dp)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(200.dp)
                            .height(150.dp)
                            .offset((-150).dp, 0.dp, (-100).dp)
                            .movable(enabled = true, movePolicy = MovePolicy.Default)
                            .testTag("multi_panel_one")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) { Text("Panel One Content") }
                }
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(200.dp)
                            .height(150.dp)
                            .offset(panelTwoOffsetX, panelTwoOffsetY, (-100).dp)
                            .movable(enabled = true, movePolicy = MovePolicy.Default)
                            .testTag("multi_panel_two")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) { Text("Panel Two Content") }
                }
            }
        }

        // Verify initial bounds and positions for both panels
        composeTestRule
            .onSubspaceNodeWithTag("multi_panel_one")
            .assertExists()
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(150.dp)
            .assertPositionInRootIsEqualTo((-150).dp, 0.dp, (-100).dp)

        composeTestRule
            .onSubspaceNodeWithTag("multi_panel_two")
            .assertExists()
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(150.dp)
            .assertPositionInRootIsEqualTo(150.dp, 0.dp, (-100).dp)

        composeTestRule.onAllNodesWithText("Panel One Content").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel Two Content").onFirst().assertIsDisplayed()

        // Update Panel Two position
        panelTwoOffsetX = 200.dp
        panelTwoOffsetY = 50.dp
        composeTestRule.waitForIdle()

        // Verify Panel Two moved to new position while Panel One stayed at original position
        composeTestRule
            .onSubspaceNodeWithTag("multi_panel_two")
            .assertExists()
            .assertPositionInRootIsEqualTo(200.dp, 50.dp, (-100).dp)

        composeTestRule
            .onSubspaceNodeWithTag("multi_panel_one")
            .assertExists()
            .assertPositionInRootIsEqualTo((-150).dp, 0.dp, (-100).dp)
    }

    /** Validates that a movable SpatialPanel translates across all 3D axes (X, Y, Z depth). */
    @Test
    fun movablePanel_customMovePolicy_depthTranslationAnd3DOffset() {
        var panelOffsetX by mutableStateOf(0.dp)
        var panelOffsetY by mutableStateOf(0.dp)
        var panelOffsetZ by mutableStateOf((-100).dp)

        composeTestRule.setContent {
            val density = LocalDensity.current
            val customPolicy = remember {
                MovePolicy.custom(scaleWithDistance = false) { event ->
                    val deltaX = event.pose.translation.x - event.previousPose.translation.x
                    val deltaY = event.pose.translation.y - event.previousPose.translation.y
                    val deltaZ = event.pose.translation.z - event.previousPose.translation.z
                    with(density) {
                        panelOffsetX += deltaX.toDp()
                        panelOffsetY += deltaY.toDp()
                        panelOffsetZ += deltaZ.toDp()
                    }
                }
            }

            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp)
                            .height(200.dp)
                            .offset(panelOffsetX, panelOffsetY, panelOffsetZ)
                            .movable(enabled = true, movePolicy = customPolicy)
                            .testTag("3d_movable_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("3D Movable Depth Content")
                    }
                }
            }
        }

        runBlocking(Dispatchers.Default) {
            composeTestRule.activity.requestFullSpace()
        }
        composeTestRule.waitForIdle()

        // Verify initial 3D position
        composeTestRule
            .onSubspaceNodeWithTag("3d_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(200.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, (-100).dp)

        composeTestRule.onAllNodesWithText("3D Movable Depth Content").onFirst().assertIsDisplayed()

        val panelNode = capturePanelNode()
        assertTrue(panelNode.isMovable, "Expected panel node to have isMovable = true")

        // Perform 3D spatial move interaction across axes to trigger the custom move policy
        panelNode.performSpatialInteraction {
            moveNodeBy(delta = Vector3(0.25f, 0.16f, 0.20f))
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(
            conditionDescription = "Custom move policy did not update 3D layout offsets",
            timeoutMillis = 5000L,
        ) {
            panelOffsetX != 0.dp || panelOffsetY != 0.dp || panelOffsetZ != (-100).dp
        }

        // Verify updated 3D coordinates in subspace root match the policy state holder
        composeTestRule
            .onSubspaceNodeWithTag("3d_movable_panel")
            .assertExists()
            .assertPositionInRootIsEqualTo(panelOffsetX, panelOffsetY, panelOffsetZ)
    }

    /**
     * Validates that dynamically resizing a movable SpatialPanel updates bounds and maintains
     * position.
     */
    @Test
    fun movablePanel_dynamicResize_preservesPositionAndMovableState() {
        var panelWidth by mutableStateOf(300.dp)
        var panelHeight by mutableStateOf(200.dp)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(panelWidth)
                            .height(panelHeight)
                            .offset(50.dp, (-30).dp, (-120).dp)
                            .movable(enabled = true, movePolicy = MovePolicy.Default)
                            .testTag("resizing_movable_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Resizing Movable Content")
                    }
                }
            }
        }

        // Verify initial bounds and position
        composeTestRule
            .onSubspaceNodeWithTag("resizing_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(200.dp)
            .assertPositionInRootIsEqualTo(50.dp, (-30).dp, (-120).dp)

        composeTestRule.onAllNodesWithText("Resizing Movable Content").onFirst().assertIsDisplayed()

        // Dynamically resize panel
        panelWidth = 450.dp
        panelHeight = 350.dp
        composeTestRule.waitForIdle()

        // Verify updated bounds while position remains anchored
        composeTestRule
            .onSubspaceNodeWithTag("resizing_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(450.dp)
            .assertHeightIsEqualTo(350.dp)
            .assertPositionInRootIsEqualTo(50.dp, (-30).dp, (-120).dp)
    }

    /** Validates that SpatialMainPanel in Subspace can be configured with movable modifier. */
    @Test
    fun movablePanel_spatialMainPanel_rendersWithMovableModifier() {
        composeTestRule.setContent {
            // 2D content rendered to the main panel
            Box(modifier = Modifier.fillMaxSize()) { Text("Main Panel 2D Content") }

            // Spatial Subspace positioning the main panel as movable
            Subspace {
                SpatialMainPanel(
                    modifier =
                        SubspaceModifier.offset(0.dp, 0.dp, (-100).dp)
                            .movable(enabled = true, movePolicy = MovePolicy.Default)
                            .testTag("main_spatial_panel")
                )
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("main_spatial_panel")
            .assertExists()
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, (-100).dp)

        composeTestRule.onAllNodesWithText("Main Panel 2D Content").onFirst().assertIsDisplayed()
    }

    @Test
    fun movablePanel_systemMovePolicy_translatesWithSpatialInteraction() {
        var deltaXAccumulated by mutableStateOf(0f)

        composeTestRule.setContent {
            val systemPolicy = remember {
                MovePolicy.system(scaleWithDistance = false) { event ->
                    deltaXAccumulated += event.pose.translation.x - event.previousPose.translation.x
                }
            }

            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp)
                            .height(200.dp)
                            .offset(0.dp, 0.dp, (-100).dp)
                            .movable(enabled = true, movePolicy = systemPolicy)
                            .testTag("interactive_movable_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Interactive Movable Content")
                    }
                }
            }
        }

        runBlocking(Dispatchers.Default) {
            composeTestRule.activity.requestFullSpace()
        }
        composeTestRule.waitForIdle()

        val panelNode = capturePanelNode()
        assertTrue(panelNode.isMovable, "Expected panel node to have isMovable = true")

        // Perform 3D spatial move raycast interaction on the panel's move affordance
        panelNode.performSpatialInteraction {
            moveNodeBy(delta = Vector3(0.25f, 0f, 0f))
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(
            conditionDescription = "Panel position changed after spatial move interaction",
            timeoutMillis = 5000L,
        ) {
            deltaXAccumulated > 0.05f
        }
    }

    @Test
    fun movablePanel_systemMovePolicy_translatesAlongMultipleAxesWithSpatialInteraction() {
        var deltaXAccumulated by mutableStateOf(0f)
        var deltaYAccumulated by mutableStateOf(0f)

        composeTestRule.setContent {
            val systemPolicy = remember {
                MovePolicy.system(scaleWithDistance = false) { event ->
                    deltaXAccumulated += event.pose.translation.x - event.previousPose.translation.x
                    deltaYAccumulated += event.pose.translation.y - event.previousPose.translation.y
                }
            }

            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp)
                            .height(200.dp)
                            .offset(0.dp, 0.dp, (-100).dp)
                            .movable(enabled = true, movePolicy = systemPolicy)
                            .testTag("multi_axis_movable_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Multi-Axis Movable Content")
                    }
                }
            }
        }

        runBlocking(Dispatchers.Default) {
            composeTestRule.activity.requestFullSpace()
        }
        composeTestRule.waitForIdle()

        val panelNode = capturePanelNode()

        // Perform 3D spatial move interaction along both X and Y axes
        panelNode.performSpatialInteraction {
            moveNodeBy(delta = Vector3(0.25f, 0.16f, 0f))
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(
            conditionDescription = "Panel position changed after multi-axis move interaction",
            timeoutMillis = 5000L,
        ) {
            deltaXAccumulated > 0.05f && deltaYAccumulated > 0.05f
        }
    }

    /**
     * Validates that a SpatialPanel with disabled movable modifier renders in Subspace and does not
     * expose move affordances in the spatial scene.
     */
    @Test
    fun movablePanel_disabledMovable_doesNotExposeMoveAffordance() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp)
                            .height(200.dp)
                            .offset(0.dp, 0.dp, (-100).dp)
                            .movable(enabled = false)
                            .testTag("non_movable_panel")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Non Movable Content")
                    }
                }
            }
        }

        // Verify Compose Subspace node existence, bounds, and position
        composeTestRule
            .onSubspaceNodeWithTag("non_movable_panel")
            .assertExists()
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(200.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, (-100).dp)

        composeTestRule.onAllNodesWithText("Non Movable Content").onFirst().assertIsDisplayed()

        val panelNode = capturePanelNode()

        // Verify no move affordance (DragBar) is attached to the non-movable panel
        assertTrue(
            panelNode.moveAffordanceNodes.isEmpty(),
            "Expected panel with movable(enabled = false) to have no move affordance nodes",
        )
    }

    private fun capturePanelNode(): SpatialNode {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val currentTaskId = composeTestRule.activity.taskId
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task =
            assertNotNull(
                scene.findTaskById(currentTaskId)
                    ?: scene.tasks
                        .filter { it.packageName == packageName }
                        .maxByOrNull { it.taskId },
                "Expected task for package $packageName with taskId $currentTaskId",
            )
        return assertNotNull(
            findPanelNode(task),
            "Expected a panel node in spatial scene for package $packageName in task ${task.taskId}",
        )
    }

    private fun waitUntilPanelMoves(
        initialPosition: Vector3,
        conditionDescription: String = "Panel position changed after spatial move interaction",
    ): Vector3 {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val currentTaskId = composeTestRule.activity.taskId
        var updatedPosition = initialPosition
        composeTestRule.waitUntil(
            conditionDescription = conditionDescription,
            timeoutMillis = 5000L,
        ) {
            val scene = SpatialSceneHelper.captureSpatialScene()
            val task =
                scene.findTaskById(currentTaskId)
                    ?: scene.tasks
                        .filter { it.packageName == packageName }
                        .maxByOrNull { it.taskId }
            val node = findPanelNode(task)
            if (node != null) {
                updatedPosition = node.position
                (updatedPosition - initialPosition).length > 0.05f
            } else {
                false
            }
        }
        return updatedPosition
    }

    private fun findPanelNode(task: SpatialTask?): SpatialNode? =
        task?.windowLeashes?.firstOrNull { it.isEmbeddedWindowLeash && it.isMovable }
            ?: task?.windowLeashes?.firstOrNull { it.isEmbeddedWindowLeash }
            ?: task?.findNodesByName("ViewPanel")?.firstOrNull { it.isMovable }
            ?: task
                ?.findNodes {
                    it.isMovable &&
                        !it.isSysUi &&
                        !it.isTaskWindowLeash &&
                        !it.name.startsWith("space-transform-task") &&
                        it != task.activitySpaceRoot
                }
                ?.firstOrNull()
            ?: task?.findNodes { it.isMovable && !it.isSysUi }?.firstOrNull()
            ?: task?.findNodesByName("ViewPanel")?.lastOrNull()
            ?: task?.activitySpaceRoot?.findNode {
                !it.isWindowLeash && !it.isSysUi && it.hasSurfaceBounds
            }
}
