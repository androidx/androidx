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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.arcore.testing.FakeRuntimeAnchor
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.SceneManager
import androidx.xr.compose.subspace.ArDeviceTarget
import androidx.xr.compose.subspace.FollowBehavior
import androidx.xr.compose.subspace.FollowTarget
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.TrackedDimensions
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.depth
import androidx.xr.compose.subspace.layout.fillMaxDepth
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.sizeIn
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertDepthIsAtLeast
import androidx.xr.compose.testing.assertDepthIsEqualTo
import androidx.xr.compose.testing.assertDepthIsNotEqualTo
import androidx.xr.compose.testing.assertEntityIsDescendantOf
import androidx.xr.compose.testing.assertHeightIsAtLeast
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertHeightIsNotEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertPositionIsEqualTo
import androidx.xr.compose.testing.assertWidthIsAtLeast
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.assertWidthIsNotEqualTo
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.compose.testing.toDp
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.RenderingEntityFactory
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class SubspaceTest {

    private val testDispatcher = StandardTestDispatcher()
    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Before
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun setUp() {
        FollowBehavior.dispatcherOverride = testDispatcher
        FakeRuntimeAnchor.anchorsCreatedCount = 0
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @After
    fun tearDown() {
        FollowBehavior.dispatcherOverride = Dispatchers.Default
    }

    private object DefaultTestRecommendedBoxSize {
        const val WIDTH_METERS: Float = 1.73f
        const val HEIGHT_METERS: Float = 1.61f
        const val DEPTH_METERS: Float = 0.5f
    }

    /**
     * Creates a TestSceneRuntime with a recommended content box of the given size.
     *
     * Don't call this inside composeTestRule in a test. If it recomposes, a new Session will be
     * created when a previous one already exists for the activity.
     */
    private fun configureSessionWithRecommendedBox(
        widthMeters: Float = DefaultTestRecommendedBoxSize.WIDTH_METERS,
        heightMeters: Float = DefaultTestRecommendedBoxSize.HEIGHT_METERS,
        depthMeters: Float = DefaultTestRecommendedBoxSize.DEPTH_METERS,
    ) {
        composeTestRule.configureFakeSession(
            sceneRuntime = { runtime ->
                object :
                    SceneRuntime by runtime,
                    RenderingEntityFactory by (runtime as RenderingEntityFactory) {
                    override val activitySpace =
                        object : ActivitySpace by runtime.activitySpace {
                            override val recommendedContentBoxInFullSpace =
                                BoundingBox.fromMinMax(
                                    min =
                                        Vector3(
                                            -widthMeters / 2,
                                            -heightMeters / 2,
                                            -depthMeters / 2,
                                        ),
                                    max =
                                        Vector3(widthMeters / 2, heightMeters / 2, depthMeters / 2),
                                )
                        }
                }
            }
        )
    }

    private fun configureSessionWithDeviceTracking(): Session {
        val result = Session.create(composeTestRule.activity, testDispatcher)
        val session = assertIs<SessionCreateSuccess>(result).session
        session.configure(
            config = session.config.copy(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN)
        )

        return session
    }

    private fun translateDevice(fakeRuntime: FakePerceptionRuntime, offset: Vector3) {
        val arDevice = fakeRuntime.perceptionManager.arDevice
        arDevice.devicePose = arDevice.devicePose.translate(translation = offset)
        testDispatcher.scheduler.advanceUntilIdle()
        fakeRuntime.lifecycleManager.allowOneMoreCallToUpdate()
    }

    private fun rotateDevice(fakeRuntime: FakePerceptionRuntime, offset: Quaternion) {
        val fakePerceptionManager = fakeRuntime.perceptionManager
        val fakeLifecycleManager: FakeLifecycleManager = fakeRuntime.lifecycleManager

        fakePerceptionManager.arDevice.devicePose =
            fakePerceptionManager.arDevice.devicePose.rotate(rotation = offset)

        testDispatcher.scheduler.advanceUntilIdle()
        fakeLifecycleManager.allowOneMoreCallToUpdate()
    }

    // ---------------------------------------------------------------------------------------------
    //                                    Subspace Tests
    // ---------------------------------------------------------------------------------------------
    @Test
    fun subspace_whenDirectlyParented_rendersContent() {
        composeTestRule.setContent {
            Subspace {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.width(100.dp).height(100.dp).testTag("innerPanel")
                    ) {}
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("innerPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(100.toDp())
            .assertHeightIsEqualTo(100.toDp())
    }

    @Test
    fun subspace_whenNested_respectsOffsets() {
        composeTestRule.setContent {
            Subspace {
                SpatialBox(modifier = SubspaceModifier.offset(x = 10.dp, y = 20.dp, z = 30.dp)) {
                    Subspace(modifier = SubspaceModifier.offset(x = 40.dp, y = 50.dp, z = 60.dp)) {
                        SpatialPanel(SubspaceModifier.size(100.dp).testTag("innerPanel")) {}
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("innerPanel")
            .assertExists()
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
            .assertPositionInRootIsEqualTo(40.dp, 50.dp, 60.dp)
    }

    @Test
    fun subspace_whenXrIsEnabled_createsContent() {
        composeTestRule.setContent {
            Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun subspace_whenXrIsDisabled_doesNotCreateContent() {
        composeTestRule.activity.disableXr()

        composeTestRule.setContent {
            Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
    }

    @Test
    fun subspace_whenCreated_isParentedToKeyEntity() {
        composeTestRule.setContent {
            Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertEntityIsDescendantOf(assertNotNull(composeTestRule.session?.scene?.keyEntity))
    }

    @Test
    fun subspace_whenRemovedFromComposition_isDisposed() {
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            if (showSubspace) {
                Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        showSubspace = false

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(0)
    }

    @Test
    fun subspace_whenSpaceModeChanges_onlyOneSceneExists() {
        composeTestRule.setContent {
            Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        composeTestRule.session?.scene?.requestHomeSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        composeTestRule.session?.scene?.requestFullSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)
    }

    @Test
    fun subspace_withFillMaxSizeAndHigherDensity_respectsConstraints() {
        var density: Density? = null
        configureSessionWithRecommendedBox()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                density = LocalDensity.current
                Subspace { SpatialBox(SubspaceModifier.fillMaxSize(1.0f).testTag("box")) {} }
            }
        }

        assertNotNull(density)
        assertThat(density.density).isEqualTo(2f)
        val expectedWidthPx = Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS).roundToPx(density)
        val expectedHeightPx = Meter(DefaultTestRecommendedBoxSize.HEIGHT_METERS).roundToPx(density)
        val expectedDepthPx = Meter(DefaultTestRecommendedBoxSize.DEPTH_METERS).roundToPx(density)
        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .assertDepthIsEqualTo(expectedDepthPx.toDp())
    }

    @Test
    fun subspace_withFillMaxSize_respectsRecommendedBoxConstraints() {
        var density: Density? = null
        configureSessionWithRecommendedBox()
        composeTestRule.setContent {
            density = LocalDensity.current
            Subspace { SpatialBox(SubspaceModifier.fillMaxSize(1.0f).testTag("box")) {} }
        }

        assertNotNull(density)
        val expectedWidthPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS).roundToPx(this) }
        val expectedHeightPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.HEIGHT_METERS).roundToPx(this) }
        val expectedDepthPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.DEPTH_METERS).roundToPx(this) }
        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .assertDepthIsEqualTo(expectedDepthPx.toDp())
    }

    @Test
    fun subspace_whenUnbounded_withFillMaxSize_doesNotRespectConstraints() {
        composeTestRule.setContent {
            Subspace(allowUnboundedSubspace = true) {
                SpatialBox(
                    SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsNotEqualTo(VolumeConstraints().maxWidth.toDp())
            .assertHeightIsNotEqualTo(VolumeConstraints().maxHeight.toDp())
            .assertDepthIsNotEqualTo(VolumeConstraints().maxDepth.toDp())
    }

    @Test
    fun subspace_whenCustomBounded_withFillMaxSize_respectsConstraints() {
        composeTestRule.setContent {
            Subspace(modifier = SubspaceModifier.sizeIn(0.dp, 100.dp, 0.dp, 100.dp)) {
                SpatialBox(SubspaceModifier.fillMaxSize(1.0f).testTag("box")) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(100.toDp())
            .assertHeightIsEqualTo(100.toDp())
    }

    @Test
    fun subspace_whenAllowUnbounded_isUnbounded() {
        var density: Density? = null
        configureSessionWithRecommendedBox()
        composeTestRule.setContent {
            density = LocalDensity.current
            // This large width is explicitly bigger than the recommended box width.
            val widthLargerThanRecommendedBox =
                with(LocalDensity.current) {
                    Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS + 1000000.0f)
                        .roundToPx(this)
                        .toDp()
                }
            val heightLargerThanRecommendedBox =
                with(LocalDensity.current) {
                    Meter(DefaultTestRecommendedBoxSize.HEIGHT_METERS + 100000.0f)
                        .roundToPx(this)
                        .toDp()
                }
            val depthLargerThanRecommendedBox =
                with(LocalDensity.current) {
                    Meter(DefaultTestRecommendedBoxSize.DEPTH_METERS + 100000.0f)
                        .roundToPx(this)
                        .toDp()
                }
            Subspace(allowUnboundedSubspace = true) {
                SpatialPanel(
                    SubspaceModifier.width(widthLargerThanRecommendedBox)
                        .height(heightLargerThanRecommendedBox)
                        .depth(depthLargerThanRecommendedBox)
                        .testTag("panel")
                ) {}
            }
        }

        val recommendedWidthPx =
            with(assertNotNull(density)) {
                Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS).roundToPx(this)
            }
        val recommendedHeightPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.HEIGHT_METERS).roundToPx(this) }
        val recommendedDepthPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.DEPTH_METERS).roundToPx(this) }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsAtLeast(recommendedWidthPx.toDp())
            .assertHeightIsAtLeast(recommendedHeightPx.toDp())
            .assertDepthIsAtLeast(recommendedDepthPx.toDp())
    }

    @Test
    fun subspace_withLargerThanDefaultModifier_respectsModifier() {
        val largeSize = 500000000.dp
        configureSessionWithRecommendedBox()
        composeTestRule.setContent {
            // The user provides a modifier bigger than the recommended box.
            Subspace(modifier = SubspaceModifier.size(largeSize)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(largeSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(largeSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(largeSize)
    }

    @Test
    fun subspace_withSmallerThanDefaultModifier_respectsModifier() {
        val smallSize = 2.dp
        configureSessionWithRecommendedBox()
        composeTestRule.setContent {
            // The user provides a modifier smaller than the recommended box.
            Subspace(modifier = SubspaceModifier.size(smallSize)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(smallSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(smallSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(smallSize)
    }

    @Test
    fun subspace_whenConstraintsChange_recomposesWithNewConstraints() {
        val initialConstraints =
            SubspaceModifier.sizeIn(
                minWidth = 0.dp,
                maxWidth = 100.dp,
                minHeight = 0.dp,
                maxHeight = 100.dp,
                minDepth = 0.dp,
                maxDepth = VolumeConstraints.INFINITY.dp,
            )
        val updatedConstraints =
            SubspaceModifier.sizeIn(
                minWidth = 50.dp,
                maxWidth = 150.dp,
                minHeight = 50.dp,
                maxHeight = 150.dp,
                minDepth = 0.dp,
                maxDepth = VolumeConstraints.INFINITY.dp,
            )
        val constraintsState = mutableStateOf(initialConstraints)

        composeTestRule.setContent {
            Subspace(modifier = constraintsState.value) {
                SpatialBox(
                    modifier = SubspaceModifier.fillMaxWidth().fillMaxHeight().testTag("testBox")
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("testBox")
            .assertWidthIsEqualTo(100.toDp())
            .assertHeightIsEqualTo(100.toDp())

        constraintsState.value = updatedConstraints

        composeTestRule
            .onSubspaceNodeWithTag("testBox")
            .assertWidthIsEqualTo(150.toDp())
            .assertHeightIsEqualTo(150.toDp())
    }

    @Test
    fun subspace_whenItLeavesComposition_disablesMainPanelEntity() {
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            if (showSubspace) {
                Subspace {}
            }
        }

        val session = composeTestRule.session
        assertNotNull(session)
        val mainPanelEntity = session.scene.mainPanelEntity
        assertThat(mainPanelEntity.isEnabled()).isEqualTo(false)

        showSubspace = false
        composeTestRule.waitForIdle()

        assertThat(mainPanelEntity.isEnabled()).isEqualTo(true)
    }

    @Test
    fun subspace_whenMultipleSubspacesLeaveComposition_disablesMainPanelEntity() {
        var showFirstSubspace by mutableStateOf(true)
        var showSecondSubspace by mutableStateOf(false)

        composeTestRule.setContent {
            if (showFirstSubspace) {
                Subspace {}
            }

            if (showSecondSubspace) {
                Subspace {}
            }
        }

        val session = assertNotNull(composeTestRule.session)
        val mainPanelEntity = session.scene.mainPanelEntity
        assertThat(mainPanelEntity.isEnabled()).isFalse()

        showSecondSubspace = true
        composeTestRule.waitForIdle()

        assertThat(mainPanelEntity.isEnabled()).isFalse()

        showFirstSubspace = false
        composeTestRule.waitForIdle()

        assertThat(mainPanelEntity.isEnabled()).isFalse()

        showSecondSubspace = false
        composeTestRule.waitForIdle()

        assertThat(mainPanelEntity.isEnabled()).isTrue()
    }

    @Test
    fun subspace_whenSwitchingModes_retainsState() {
        val session = composeTestRule.configureFakeSession()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel {
                    var state by remember { mutableStateOf(0) }
                    Button(onClick = { state++ }) { Text("Increment") }
                    Text("$state", modifier = Modifier.testTag("state"))
                }
            }
        }

        composeTestRule.onNodeWithTag("state").assertTextContains("0")

        composeTestRule.onNodeWithText("Increment").performClick().performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        session.scene.requestHomeSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        session.scene.requestFullSpaceMode()
        composeTestRule.onNodeWithText("Increment").performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("5")
    }

    @Test
    fun subspace_whenSwitchingModesFromHomeSpace_retainsState() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            CompositionLocalProvider {
                Subspace {
                    SpatialPanel {
                        var state by remember { mutableStateOf(0) }
                        Button(
                            onClick = { state++ },
                            modifier = Modifier.testTag("increment_button"),
                        ) {
                            Text("Increment")
                        }
                        Text("$state", modifier = Modifier.testTag("state"))
                    }
                }
            }
        }

        // Helper function to assert the state. This also synchronizes the UI.
        fun assertStateIs(count: Int) {
            composeTestRule.onNodeWithTag("state").assertTextContains(count.toString())
        }

        // Helper function to perform clicks, making the test more readable.
        fun clickIncrement(times: Int = 1) {
            repeat(times) { composeTestRule.onNodeWithTag("increment_button").performClick() }
        }

        // --- Test Execution ---

        // Verify initial state.
        assertStateIs(0)

        // Switch to full space mode and verify state is preserved.
        composeTestRule.session!!.scene.requestFullSpaceMode()
        assertStateIs(0)

        // Increment the counter and verify the new state.
        clickIncrement(3)
        assertStateIs(3)

        // Switch to home space mode and verify state is preserved.
        composeTestRule.session!!.scene.requestHomeSpaceMode()
        assertStateIs(3)

        // Switch back to full space, increment again, and verify.
        composeTestRule.session!!.scene.requestFullSpaceMode()
        clickIncrement(2)
        assertStateIs(5)

        // Switch to home space one last time and.
        composeTestRule.session!!.scene.requestHomeSpaceMode()
        assertStateIs(5)
    }

    @Test
    fun subspace_withProvidedRootContainer_usesIt() {
        var testNode: Entity? = null

        composeTestRule.setContent {
            testNode = Entity.create(LocalSession.current!!, "TestRoot")
            CompositionLocalProvider(LocalSubspaceRootNode provides testNode) {
                Subspace { SpatialBox(modifier = SubspaceModifier.testTag("Box")) {} }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("Box")
            .assertEntityIsDescendantOf(assertNotNull(testNode))
    }

    @Test
    fun subspace_withMultipleSubspaces_shareTheSameRootContainer() {
        composeTestRule.setContent {
            Subspace { SpatialBox(modifier = SubspaceModifier.testTag("Box")) {} }
            Subspace { SpatialBox(modifier = SubspaceModifier.testTag("Box2")) {} }
        }

        composeTestRule
            .onSubspaceNodeWithTag("Box")
            .assertEntityIsDescendantOf(assertNotNull(composeTestRule.session?.scene?.keyEntity))
        composeTestRule
            .onSubspaceNodeWithTag("Box2")
            .assertEntityIsDescendantOf(assertNotNull(composeTestRule.session?.scene?.keyEntity))
    }

    // ---------------------------------------------------------------------------------------------
    //                                PlanarEmbeddedSubspace Tests
    // ---------------------------------------------------------------------------------------------
    @Test
    fun planarEmbeddedSubspace_whenInContainingPanel_isParented() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    PlanarEmbeddedSubspace {
                        SpatialPanel(SubspaceModifier.testTag("innerPanel")) {}
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("innerPanel")
            .assertEntityIsDescendantOf(
                assertNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("panel")
                        .fetchSemanticsNode()
                        .semanticsEntity
                )
            )
    }

    @Test
    fun planarEmbeddedSubspace_whenContentSizeMatchesParent_isEnabled() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp).testTag("panel")) {
                    PlanarEmbeddedSubspace {
                        SpatialPanel(SubspaceModifier.testTag("innerPanel")) {
                            Box(Modifier.size(100.dp))
                        }
                    }
                }
            }
        }

        val innerPanelNode =
            composeTestRule.onSubspaceNodeWithTag("innerPanel").fetchSemanticsNode()
        val innerPanelEntity = innerPanelNode.semanticsEntity
        assertThat(innerPanelEntity?.isEnabled(true)).isTrue()
    }

    @Test
    fun planarEmbeddedSubspace_withDepthConstraint_respectsConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.depth(10.dp).testTag("panel")) {
                    PlanarEmbeddedSubspace {
                        SpatialPanel(SubspaceModifier.depth(20.dp).testTag("innerPanel")) {}
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertDepthIsEqualTo(10.dp)
    }

    @Test
    fun planarEmbeddedSubspace_withFillMaxDepth_respectsConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.depth(10.dp).testTag("panel")) {
                    PlanarEmbeddedSubspace {
                        SpatialPanel(SubspaceModifier.fillMaxDepth().testTag("innerPanel")) {}
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertDepthIsEqualTo(10.dp)
    }

    @Test
    fun planarEmbeddedSubspace_withUnboundedDepth_respectsDepth() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.sizeIn(maxDepth = VolumeConstraints.INFINITY.dp)
                        .testTag("panel")
                ) {
                    PlanarEmbeddedSubspace {
                        SpatialPanel(SubspaceModifier.depth(20.dp).testTag("innerPanel")) {}
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun planarEmbeddedSubspace_whenXrIsEnabled_createsContent() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel {
                    PlanarEmbeddedSubspace {
                        SpatialPanel(SubspaceModifier.testTag("innerPanel")) {}
                    }
                }
            }
        }
        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertExists()
    }

    @Test
    fun planarEmbeddedSubspace_whenXrIsDisabled_doesNotCreateContent() {
        composeTestRule.activity.disableXr()
        composeTestRule.setContent {
            Subspace {
                SpatialPanel {
                    PlanarEmbeddedSubspace {
                        SpatialPanel(SubspaceModifier.testTag("innerPanel")) {}
                    }
                }
            }
        }
        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertDoesNotExist()
    }

    @Test
    fun planarEmbeddedSubspace_whenRemovedFromComposition_disposesScene() {
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    if (showSubspace) {
                        PlanarEmbeddedSubspace {
                            SpatialPanel(SubspaceModifier.testTag("innerPanel")) {}
                        }
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(2)

        showSubspace = false
        composeTestRule.waitForIdle()

        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertDoesNotExist()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)
    }

    @Test
    fun planarEmbeddedSubspace_whenPlacedBelow2DView_hasCorrectPose() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(200.dp).testTag("panel")) {
                    Row {
                        Spacer(Modifier.size(100.dp))
                        Column {
                            Spacer(Modifier.size(25.dp))
                            PlanarEmbeddedSubspace {
                                SpatialPanel(SubspaceModifier.size(100.dp).testTag("innerPanel")) {}
                            }
                        }
                    }
                }
            }
        }

        // The outer panel should have only a single child and it should be to root node of the
        // PlanarEmbeddedSubspace.
        val subspaceRootContainerEntity =
            assertNotNull(
                composeTestRule
                    .onSubspaceNodeWithTag("panel")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.children
                    ?.single()
            )
        composeTestRule
            .onSubspaceNodeWithTag("innerPanel")
            .assertEntityIsDescendantOf(subspaceRootContainerEntity)

        /*
         * (0,0)
         * 1-----------------------
         * |          |           |
         * |          |[         ]|
         * |          |[    4    ]|
         * -----------2[---------]-
         * |          |           |
         * |          |           |
         * |          |           |
         * -----------------------3
         *                         (200,200)
         *
         * 1 is the origin (0, 0) of the 2D layout of the parent panel
         * 2 is the center of the parent panel (100, 100) this is also the origin (0, 0, 0) in
         *    3D space.
         * 3 is the bottom right corner of the parent panel, it is (200, 200) in the parent layout
         * 4 is the center of the inner panel (150, 100)
         *
         * The expected offset is 4 relative to 2 which is +50 dp in x and +25 dp in y directions
         *  in 3D space.
         */
        val expectedXOffset = 50.dp
        val expectedYOffset = 25.dp
        val expectedZOffset = 0.dp

        val actualXOffsetMeters = subspaceRootContainerEntity.getPose().translation.x
        val actualXOffsetDp = Meter(actualXOffsetMeters).toDp()
        val actualYOffsetMeters = subspaceRootContainerEntity.getPose().translation.y
        val actualYOffsetDp = Meter(actualYOffsetMeters).toDp()
        val actualZOffsetMeters = subspaceRootContainerEntity.getPose().translation.z
        val actualZOffsetDp = Meter(actualZOffsetMeters).toDp()

        assertThat(actualXOffsetDp).isEqualTo(expectedXOffset)
        assertThat(actualYOffsetDp).isEqualTo(expectedYOffset)
        assertThat(actualZOffsetDp).isEqualTo(expectedZOffset)
    }

    @Test
    fun planarEmbeddedSubspace_withFixedSizeParent_isConstrainedByParent() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.width(300.dp).height(300.dp).depth(10.dp)) {
                    PlanarEmbeddedSubspace {
                        // Child requests a larger size, constrained to parent's size.
                        SpatialBox(
                            SubspaceModifier.width(500.dp)
                                .height(500.dp)
                                .depth(20.dp)
                                .testTag("constrainedChild")
                        ) {}
                        // Child fills max size is equal to parent's size.
                        SpatialBox(SubspaceModifier.fillMaxSize().testTag("fillMaxSizeChild")) {}
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("constrainedChild").assertWidthIsEqualTo(300.dp)
        composeTestRule.onSubspaceNodeWithTag("constrainedChild").assertHeightIsEqualTo(300.dp)
        composeTestRule.onSubspaceNodeWithTag("constrainedChild").assertDepthIsEqualTo(10.dp)
        composeTestRule.onSubspaceNodeWithTag("fillMaxSizeChild").assertWidthIsEqualTo(300.dp)
        composeTestRule.onSubspaceNodeWithTag("fillMaxSizeChild").assertHeightIsEqualTo(300.dp)
        composeTestRule.onSubspaceNodeWithTag("fillMaxSizeChild").assertDepthIsEqualTo(10.dp)
    }

    @Test
    fun planarEmbeddedSubspace_inSetContent_isParentedToTheMainPanel() {
        composeTestRule.setContent {
            PlanarEmbeddedSubspace { SpatialBox(SubspaceModifier.testTag("embeddedBox")) {} }
        }

        composeTestRule
            .onSubspaceNodeWithTag("embeddedBox")
            .assertExists()
            .assertEntityIsDescendantOf(
                checkNotNull(composeTestRule.session?.scene?.mainPanelEntity)
            )
    }

    // TODO(b/449821552) Improve unit testing for PlanarEmbeddedSubspace.

    // ---------------------------------------------------------------------------------------------
    //                                    FollowingSubspace Tests
    // ---------------------------------------------------------------------------------------------
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenNoDeviceTracking_DoNotRender() {
        composeTestRule.setContent {
            FollowingSubspace(
                target = FollowTarget.ArDevice(assertNotNull(LocalSession.current)),
                behavior = FollowBehavior.Static,
                modifier = SubspaceModifier.testTag("FollowingSubspace"),
            ) {}
        }

        composeTestRule.onSubspaceNodeWithTag("FollowingSubspace").assertDoesNotExist()
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenArDeviceTightUsedTogether_DoNotRender() {
        composeTestRule.session = configureSessionWithDeviceTracking()

        composeTestRule.setContent {
            FollowingSubspace(
                target = FollowTarget.ArDevice(assertNotNull(composeTestRule.session)),
                behavior = FollowBehavior.Tight,
                modifier = SubspaceModifier.testTag("FollowingSubspace"),
            ) {}
        }

        composeTestRule.onSubspaceNodeWithTag("FollowingSubspace").assertDoesNotExist()
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenLoads_respectsDefaultOffset() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                ) {
                    SpatialPanel(modifier = SubspaceModifier.testTag("HeadPanel")) {}
                }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val headPanelPose = getSemanticsNodeWorldPose("HeadPanel")
            assertThat(headPanelPose).isEqualTo(ArDeviceTarget.DEFAULT_OFFSET)
        }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_whenScaleChanges_subspaceScaleUpdates() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            var forceRecompose by mutableStateOf(false)

            composeTestRule.setContent {
                val _forceRecompose = forceRecompose
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            var spaceNode =
                composeTestRule.onSubspaceNodeWithTag("FollowingSubspace").fetchSemanticsNode()
            val initialSpaceRoot = spaceNode.semanticsEntity?.parent?.parent
            var expectedScale = spaceNode.semanticsEntity?.getScale(Space.REAL_WORLD)
            assertNotNull(expectedScale)
            assertThat(initialSpaceRoot?.getScale(Space.REAL_WORLD)).isEqualTo(expectedScale)

            expectedScale += .1f
            session.scene.keyEntity?.setScale(expectedScale, Space.REAL_WORLD)
            forceRecompose = !forceRecompose

            spaceNode =
                composeTestRule.onSubspaceNodeWithTag("FollowingSubspace").fetchSemanticsNode()
            val updatedSpaceRoot = spaceNode.semanticsEntity?.parent?.parent
            val spaceScale = updatedSpaceRoot?.getScale(Space.REAL_WORLD)
            assertThat(spaceScale).isEqualTo(expectedScale)
        }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_whenFollowTargetChanges_switchesTarget() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            var followTarget by mutableStateOf(FollowTarget.ArDevice(session))
            var followBehavior by mutableStateOf(FollowBehavior.Soft(durationMs = 1000))

            composeTestRule.setContent {
                FollowingSubspace(
                    target = followTarget,
                    behavior = followBehavior,
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val arDeviceTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, arDeviceTranslation)

            var subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            assertThat(subspaceCurrentPose.translation).isEqualTo(arDeviceTranslation)

            // Switch to an anchor target
            val anchorTranslation = Vector3(20.0f, 30.0f, 40.0f)
            val anchorResult = Anchor.create(session, Pose(anchorTranslation))
            val success = assertIs<AnchorCreateSuccess>(anchorResult)
            val anchorEntity = AnchorEntity.create(session, anchor = success.anchor)
            testDispatcher.scheduler.advanceUntilIdle()

            followTarget = FollowTarget.Anchor(anchorEntity)
            followBehavior = FollowBehavior.Tight
            subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            assertThat(subspaceCurrentPose.translation).isEqualTo(anchorTranslation)
        }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class, ExperimentalCoroutinesApi::class)
    fun followingSubspace_whenFollowBehaviorChanges_actsLikeNewBehavior() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            var followBehavior by mutableStateOf(FollowBehavior.Soft(durationMs = 1000))

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = followBehavior,
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val unitVector = Vector3(x = 1F, y = 1F, z = 1F)
            translateDevice(fakeRuntime, unitVector)
            translateDevice(fakeRuntime, unitVector)

            // With Soft behavior, subspace should have moved 2 unit vectors.
            var subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            assertThat(subspaceCurrentPose.translation).isEqualTo(unitVector * 2F)

            followBehavior = FollowBehavior.Static
            composeTestRule.waitForIdle()
            translateDevice(fakeRuntime, unitVector)
            testDispatcher.scheduler.advanceUntilIdle()

            // With Static behavior, it should not move any more since it already moved.
            subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            assertThat(subspaceCurrentPose.translation).isEqualTo(unitVector * 2F)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenStaticBehavior_OnlyMovesOnce() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Static,
                    dimensions =
                        TrackedDimensions(
                            isTranslationXTracked = true,
                            isTranslationYTracked = true,
                            isTranslationZTracked = true,
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val unitVector = Vector3(x = 1F, y = 1F, z = 1F)
            translateDevice(fakeRuntime, unitVector)
            translateDevice(fakeRuntime, unitVector)

            val subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            // Device was moved 1 unit vector twice but is still just offset 1 unit vector.
            assertThat(subspaceCurrentPose.translation).isEqualTo(unitVector)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenNoDimensionsTracked_DoesNotMove() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions =
                        TrackedDimensions(
                            isTranslationXTracked = false,
                            isTranslationYTracked = false,
                            isTranslationZTracked = false,
                            isRotationXTracked = false,
                            isRotationYTracked = false,
                            isRotationZTracked = false,
                        ),
                ) {
                    SpatialPanel(modifier = SubspaceModifier.testTag("HeadPanel")) {}
                }
            }

            val headPanelInitialPose = getSemanticsNodeWorldPose("HeadPanel")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            assertThat(getSemanticsNodeWorldPose("HeadPanel")).isEqualTo(headPanelInitialPose)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenDeviceTranslatesAndRotates_MatchesMovement() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val subspaceWorldPose = getSemanticsNodeWorldPose("FollowingSubspace")
            assertThat(subspaceWorldPose)
                .isEqualTo(fakeRuntime.perceptionManager.arDevice.devicePose)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenDeviceTranslates_MatchesMovement() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions =
                        TrackedDimensions(
                            isTranslationXTracked = true,
                            isTranslationYTracked = true,
                            isTranslationZTracked = true,
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val subspaceInitialPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            assertThat(subspaceCurrentPose.translation)
                .isEqualTo(fakeRuntime.perceptionManager.arDevice.devicePose.translation)
            // Panel should not rotate because rotation is not being tracked.
            assertThat(subspaceCurrentPose.rotation).isEqualTo(subspaceInitialPose.rotation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenOnlyXTranslationTracked_OnlyXTranslationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions = TrackedDimensions(isTranslationXTracked = true),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val subspaceInitialPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val expectedTranslation = Vector3(fakeArDevice.devicePose.translation.x, 0F, 0F)

            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)
            // Panel should not rotate because rotation is not being tracked.
            assertThat(subspaceCurrentPose.rotation).isEqualTo(subspaceInitialPose.rotation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenOnlyYTranslationTracked_OnlyYTranslationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions = TrackedDimensions(isTranslationYTracked = true),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val subspaceInitialPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val expectedTranslation = Vector3(0F, fakeArDevice.devicePose.translation.y, 0F)

            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)
            // Panel should not rotate because rotation is not being tracked.
            assertThat(subspaceCurrentPose.rotation).isEqualTo(subspaceInitialPose.rotation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenOnlyZTranslationTracked_OnlyZTranslationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions = TrackedDimensions(isTranslationZTracked = true),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val subspaceInitialPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val expectedTranslation = Vector3(0F, 0F, fakeArDevice.devicePose.translation.z)
            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)
            // Panel should not rotate because rotation is not being tracked.
            assertThat(subspaceCurrentPose.rotation).isEqualTo(subspaceInitialPose.rotation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenDeviceRotates_MatchesMovement() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions =
                        TrackedDimensions(
                            isRotationXTracked = true,
                            isRotationYTracked = true,
                            isRotationZTracked = true,
                        ),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val headPanelInitialPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            assertThat(getSemanticsNodeWorldPose("FollowingSubspace").rotation)
                .isEqualTo(fakeArDevice.devicePose.rotation)
            assertThat(getSemanticsNodeWorldPose("FollowingSubspace").translation)
                .isEqualTo(headPanelInitialPose.translation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenOnlyXRotationTracked_OnlyXRotationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions = TrackedDimensions(isRotationXTracked = true),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val headPanelInitialPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val currentTranslation = getSemanticsNodeWorldPose("FollowingSubspace").translation
            val currentRotation = getSemanticsNodeWorldPose("FollowingSubspace").rotation
            val deviceRotation = fakeArDevice.devicePose.rotation.eulerAngles
            val expectedRotation =
                Quaternion.fromEulerAngles(pitch = deviceRotation.x, yaw = 0F, roll = 0F)

            assertThat(currentRotation.x).isWithin(1f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(0f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(0f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1f).of(expectedRotation.w)
            assertThat(currentTranslation).isEqualTo(headPanelInitialPose.translation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenOnlyYRotationTracked_OnlyYRotationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions = TrackedDimensions(isRotationYTracked = true),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val headPanelInitialPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val currentTranslation = getSemanticsNodeWorldPose("FollowingSubspace").translation
            val currentRotation = getSemanticsNodeWorldPose("FollowingSubspace").rotation
            val deviceRotation = fakeArDevice.devicePose.rotation.eulerAngles
            val expectedRotation =
                Quaternion.fromEulerAngles(pitch = 0F, yaw = deviceRotation.y, roll = 0F)

            assertThat(currentRotation.x).isWithin(0f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(1f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(0f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1f).of(expectedRotation.w)
            assertThat(currentTranslation).isEqualTo(headPanelInitialPose.translation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenOnlyZRotationTracked_OnlyZRotationMatches() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions = TrackedDimensions(isRotationZTracked = true),
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            val headPanelInitialPose = getSemanticsNodeWorldPose("FollowingSubspace")
            val offsetTranslation = Vector3(x = 1F, y = 2F, z = 3F)
            translateDevice(fakeRuntime, offsetTranslation)

            val offsetRotation = Quaternion.fromEulerAngles(pitch = 15F, yaw = 30F, roll = 45F)
            rotateDevice(fakeRuntime, offsetRotation)

            val currentTranslation = getSemanticsNodeWorldPose("FollowingSubspace").translation
            val currentRotation = getSemanticsNodeWorldPose("FollowingSubspace").rotation
            val deviceRotation = fakeArDevice.devicePose.rotation.eulerAngles
            val expectedRotation =
                Quaternion.fromEulerAngles(pitch = 0F, yaw = 0F, roll = deviceRotation.z)

            assertThat(currentRotation.x).isWithin(0f).of(expectedRotation.x)
            assertThat(currentRotation.y).isWithin(0f).of(expectedRotation.y)
            assertThat(currentRotation.z).isWithin(1f).of(expectedRotation.z)
            assertThat(currentRotation.w).isWithin(1f).of(expectedRotation.w)
            assertThat(currentTranslation).isEqualTo(headPanelInitialPose.translation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenTrackedDimensionsChange_MatchedDimensionsChange() =
        runTest(testDispatcher) {
            composeTestRule.session = configureSessionWithDeviceTracking()
            val session = assertNotNull(composeTestRule.session)
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakeArDevice = fakeRuntime.perceptionManager.arDevice
            var trackedDimensions by mutableStateOf(TrackedDimensions(isTranslationXTracked = true))

            composeTestRule.session = session
            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                    dimensions = trackedDimensions,
                    modifier = SubspaceModifier.testTag("FollowingSubspace"),
                ) {}
            }

            translateDevice(fakeRuntime, Vector3(x = 1F, y = 2F, z = 3F))

            var subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            var expectedTranslation = Vector3(fakeArDevice.devicePose.translation.x, 0F, 0F)
            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)

            // Switch x-only tracking to y-only tracking.
            trackedDimensions = TrackedDimensions(isTranslationYTracked = true)
            composeTestRule.waitForIdle()
            testDispatcher.scheduler.advanceUntilIdle()

            subspaceCurrentPose = getSemanticsNodeWorldPose("FollowingSubspace")
            expectedTranslation += Vector3(0F, fakeArDevice.devicePose.translation.y, 0F)
            assertThat(subspaceCurrentPose.translation).isEqualTo(expectedTranslation)
        }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenRemovedFromComposition_isDisposed() {
        composeTestRule.session = configureSessionWithDeviceTracking()
        val session = assertNotNull(composeTestRule.session)
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            if (showSubspace) {
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(),
                ) {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {}
                }
            }
        }

        assertThat(composeTestRule.onSubspaceNodeWithTag("panel")).isNotNull()

        showSubspace = false

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_withFillMaxSizeAndHigherDensity_respectsConstraints() {
        composeTestRule.session = configureSessionWithDeviceTracking()
        val session = assertNotNull(composeTestRule.session)
        var density: Density? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                density = LocalDensity.current
                FollowingSubspace(
                    target = FollowTarget.ArDevice(session),
                    behavior = FollowBehavior.Soft(),
                ) {
                    SpatialBox(SubspaceModifier.fillMaxSize(1.0f).testTag("box")) {}
                }
            }
        }

        assertNotNull(density)
        assertThat(density.density).isEqualTo(2f)
        val expectedWidthPx = Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS).roundToPx(density)
        val expectedHeightPx = Meter(DefaultTestRecommendedBoxSize.HEIGHT_METERS).roundToPx(density)
        val expectedDepthPx = Meter(DefaultTestRecommendedBoxSize.DEPTH_METERS).roundToPx(density)
        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .assertDepthIsEqualTo(expectedDepthPx.toDp())
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenUnbounded_withFillMaxSize_doesNotRespectConstraints() {
        composeTestRule.session = configureSessionWithDeviceTracking()
        val session = assertNotNull(composeTestRule.session)

        composeTestRule.setContent {
            FollowingSubspace(
                target = FollowTarget.ArDevice(session),
                behavior = FollowBehavior.Soft(),
                allowUnboundedSubspace = true,
            ) {
                SpatialBox(
                    SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertPositionInRootIsEqualTo(
                0.dp,
                0.dp,
                ArDeviceTarget.DEFAULT_OFFSET.translation.z.meters.toDp(),
            )
            .assertWidthIsNotEqualTo(VolumeConstraints().maxWidth.toDp())
            .assertHeightIsNotEqualTo(VolumeConstraints().maxHeight.toDp())
            .assertDepthIsNotEqualTo(VolumeConstraints().maxDepth.toDp())
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Test
    fun followingSubspace_whenCreated_isParentedToAnchor() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

        composeTestRule.setContent {
            FollowingSubspace(
                target = FollowTarget.Anchor(anchorEntity),
                behavior = FollowBehavior.Tight,
            ) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertEntityIsDescendantOf(anchorEntity)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_withContent_positionsAtOrigin() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

        composeTestRule.setContent {
            FollowingSubspace(
                target = FollowTarget.Anchor(anchorEntity),
                behavior = FollowBehavior.Tight,
            ) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_whenNested_positionsRelativeToAnchor() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

        composeTestRule.setContent {
            Subspace(modifier = SubspaceModifier.offset(x = 40.dp, y = 50.dp, z = 60.dp)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("subspacePanel")) {}
                FollowingSubspace(
                    target = FollowTarget.Anchor(anchorEntity),
                    behavior = FollowBehavior.Tight,
                ) {
                    SpatialPanel(
                        SubspaceModifier.fillMaxSize().testTag("followingSubspacePanel")
                    ) {}
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("followingSubspacePanel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)

        composeTestRule
            .onSubspaceNodeWithTag("subspacePanel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertPositionInRootIsEqualTo(40.dp, 50.dp, 60.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_whenAnchoredToIdentity_positionsAtOrigin() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)

        val anchorResult = Anchor.create(session, Pose.Identity)
        val success = assertIs<AnchorCreateSuccess>(anchorResult)
        val anchorEntity = AnchorEntity.create(session, anchor = success.anchor)

        composeTestRule.setContent {
            FollowingSubspace(
                target = FollowTarget.Anchor(anchorEntity),
                behavior = FollowBehavior.Tight,
            ) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_whenLocked_isPositionedCorrectly() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)

        val anchorResult = Anchor.create(session, Pose(Vector3(20.0f, 30.0f, 40.0f)))
        val success = assertIs<AnchorCreateSuccess>(anchorResult)
        val anchorEntity = AnchorEntity.create(session, anchor = success.anchor)

        composeTestRule.setContent {
            FollowingSubspace(
                target = FollowTarget.Anchor(anchorEntity),
                behavior = FollowBehavior.Tight,
            ) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
            }
        }

        val anchorWorldPose = anchorEntity.getPose(Space.REAL_WORLD)
        val panelWorldPose = getSemanticsNodeWorldPose("Panel")
        assertThat(anchorWorldPose).isEqualTo(Pose(Vector3(20.0f, 30.0f, 40.0f)))
        assertThat(panelWorldPose).isEqualTo(Pose(Vector3(20.0f, 30.0f, 40.0f)))

        composeTestRule
            .onSubspaceNodeWithTag("Panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_whenAnchorChanges_repositions() {
        composeTestRule.session = composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)

        val initialPose = Pose(Vector3(10f, 20f, 30f), Quaternion(10f, 20f, 30f, 40f))
        val anchorResult = Anchor.create(session, initialPose)
        val anchorEntity =
            AnchorEntity.create(
                session,
                anchor = assertIs<AnchorCreateSuccess>(anchorResult).anchor,
            )

        val updatedPose = Pose(Vector3(40f, 50f, 60f), Quaternion(15f, 25f, 35f, 45f))
        val updatedAnchorResult = Anchor.create(session, updatedPose)
        val updatedAnchorEntity =
            AnchorEntity.create(
                session,
                anchor = assertIs<AnchorCreateSuccess>(updatedAnchorResult).anchor,
            )

        val currentAnchorState = mutableStateOf(anchorEntity)

        composeTestRule.setContent {
            FollowingSubspace(
                target = FollowTarget.Anchor(assertNotNull(currentAnchorState.value)),
                behavior = FollowBehavior.Tight,
            ) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
            }
        }

        assertThat(getSemanticsNodeWorldPose("Panel")).isEqualTo(initialPose)
        composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)

        currentAnchorState.value = updatedAnchorEntity
        composeTestRule.waitForIdle()

        assertThat(getSemanticsNodeWorldPose("Panel")).isEqualTo(updatedPose)

        composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_whenAnchorPoseChanges_repositions() {
        runBlocking {
            composeTestRule.session = composeTestRule.configureFakeSession()
            val session = assertNotNull(composeTestRule.session)
            val initialPose = Pose(Vector3(10f, 20f, 30f), Quaternion(10f, 20f, 30f, 40f))
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakePerceptionManager = fakeRuntime.perceptionManager
            val runtimeAnchor = fakePerceptionManager.createAnchor(initialPose) as FakeRuntimeAnchor
            val anchorUnderTest = Anchor(runtimeAnchor)
            assertThat(anchorUnderTest.state.value.pose).isEqualTo(initialPose)
            val anchorEntity = AnchorEntity.create(session, anchor = anchorUnderTest)

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.Anchor(assertNotNull(anchorEntity)),
                    behavior = FollowBehavior.Tight,
                ) {
                    SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
                }
            }

            assertThat(getSemanticsNodeWorldPose("Panel")).isEqualTo(initialPose)
            composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)

            val updatedPose = Pose(Vector3(40f, 50f, 60f), Quaternion(15f, 25f, 35f, 45f))
            runtimeAnchor.pose = updatedPose
            anchorUnderTest.update()

            assertThat(anchorUnderTest.state.value.pose).isEqualTo(updatedPose)
            assertThat(getSemanticsNodeWorldPose("Panel")).isEqualTo(updatedPose)
            composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
        }
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_whenAnchorUsesSoftFollow_repositions() {
        runTest(testDispatcher) {
            composeTestRule.session = composeTestRule.configureFakeSession()
            val session = assertNotNull(composeTestRule.session)
            val initialAnchorPose = Pose(Vector3(10f, 20f, 30f), Quaternion(10f, 20f, 30f, 40f))
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakePerceptionManager = fakeRuntime.perceptionManager
            val runtimeAnchor =
                fakePerceptionManager.createAnchor(initialAnchorPose) as FakeRuntimeAnchor
            val anchorUnderTest = Anchor(runtimeAnchor)
            assertThat(anchorUnderTest.state.value.pose).isEqualTo(initialAnchorPose)
            val anchorEntity = AnchorEntity.create(session, anchor = anchorUnderTest)

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.Anchor(assertNotNull(anchorEntity)),
                    behavior = FollowBehavior.Soft(durationMs = 1000),
                ) {
                    SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
                }
            }

            // Update anchor's pose and verify the Panel is at the new location.
            val updatedPose = Pose(Vector3(40f, 50f, 60f), Quaternion(15f, 25f, 35f, 45f))
            runtimeAnchor.pose = updatedPose
            anchorUnderTest.update()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(anchorUnderTest.state.value.pose).isEqualTo(updatedPose)
            val currentTranslation = getSemanticsNodeWorldPose("Panel").translation
            val currentRotation = getSemanticsNodeWorldPose("Panel").rotation

            assertThat(currentRotation.x).isWithin(1f).of(updatedPose.rotation.x)
            assertThat(currentRotation.y).isWithin(1f).of(updatedPose.rotation.y)
            assertThat(currentRotation.z).isWithin(1f).of(updatedPose.rotation.z)
            assertThat(currentRotation.w).isWithin(1f).of(updatedPose.rotation.w)
            assertThat(currentTranslation).isEqualTo(updatedPose.translation)
        }
    }

    @Test
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun followingSubspace_whenAnchorUsesStaticFollow_movesOnlyOnce() {
        runTest(testDispatcher) {
            composeTestRule.session = composeTestRule.configureFakeSession()
            val session = assertNotNull(composeTestRule.session)
            val initialPose = Pose(Vector3(10f, 20f, 30f), Quaternion(10f, 20f, 30f, 40f))
            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakePerceptionManager = fakeRuntime.perceptionManager
            val runtimeAnchor = fakePerceptionManager.createAnchor(initialPose) as FakeRuntimeAnchor
            val anchorUnderTest = Anchor(runtimeAnchor)
            assertThat(anchorUnderTest.state.value.pose).isEqualTo(initialPose)
            val anchorEntity = AnchorEntity.create(session, anchor = anchorUnderTest)

            composeTestRule.setContent {
                FollowingSubspace(
                    target = FollowTarget.Anchor(assertNotNull(anchorEntity)),
                    behavior = FollowBehavior.Static,
                ) {
                    SpatialPanel(SubspaceModifier.fillMaxSize().testTag("Panel")) {}
                }
            }

            // Verify the panel is not at its destination immediately but after waiting, it is
            // there.
            assertThat(getSemanticsNodeWorldPose("Panel").translation)
                .isNotEqualTo(initialPose.translation)

            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(getSemanticsNodeWorldPose("Panel").translation)
                .isEqualTo(initialPose.translation)

            // Verify the panel doesn't move if pose changes again.
            val updatedPose = Pose(Vector3(40f, 50f, 60f), Quaternion(15f, 25f, 35f, 45f))
            runtimeAnchor.pose = updatedPose
            anchorUnderTest.update()

            assertThat(anchorUnderTest.state.value.pose).isEqualTo(updatedPose)

            composeTestRule.waitForIdle()

            assertThat(getSemanticsNodeWorldPose("Panel").translation)
                .isNotEqualTo(updatedPose.translation)
            composeTestRule.onSubspaceNodeWithTag("Panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
        }
    }

    private fun getSemanticsNodeWorldPose(testTag: String): Pose {
        val node = composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode()
        return assertNotNull(node.semanticsEntity).getPose(relativeTo = Space.REAL_WORLD)
    }
}
