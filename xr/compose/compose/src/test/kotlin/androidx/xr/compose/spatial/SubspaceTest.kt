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
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.SceneManager
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
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
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestActivitySpace
import androidx.xr.compose.testing.TestSceneRuntime
import androidx.xr.compose.testing.assertDepthIsAtLeast
import androidx.xr.compose.testing.assertDepthIsEqualTo
import androidx.xr.compose.testing.assertDepthIsNotEqualTo
import androidx.xr.compose.testing.assertHeightIsAtLeast
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertHeightIsNotEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertPositionIsEqualTo
import androidx.xr.compose.testing.assertWidthIsAtLeast
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.assertWidthIsNotEqualTo
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.compose.testing.createFakeSession
import androidx.xr.compose.testing.disableXr
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.compose.testing.toDp
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubspaceTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

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
    private fun createAdapterWithRecommendedBox(
        widthMeters: Float = DefaultTestRecommendedBoxSize.WIDTH_METERS,
        heightMeters: Float = DefaultTestRecommendedBoxSize.HEIGHT_METERS,
        depthMeters: Float = DefaultTestRecommendedBoxSize.DEPTH_METERS,
    ): TestSceneRuntime {
        val fakeRuntime = createFakeRuntime(composeTestRule.activity)

        return TestSceneRuntime.create(fakeRuntime).apply {
            activitySpace =
                TestActivitySpace(
                    fakeRuntime.activitySpace,
                    recommendedContentBoxInFullSpace =
                        BoundingBox.fromMinMax(
                            min = Vector3(-widthMeters / 2, -heightMeters / 2, -depthMeters / 2),
                            max = Vector3(widthMeters / 2, heightMeters / 2, depthMeters / 2),
                        ),
                )
        }
    }

    @Test
    fun subspace_directlyParentedToSubspace_justRendersContentDirectly() {
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
    fun applicationSubspace_directlyParentedToApplicationSubspace_justRendersContentDirectly() {
        composeTestRule.setContent {
            ApplicationSubspace {
                ApplicationSubspace {
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
    fun applicationSubspace_nestedInSubspace_rendersContentDirectlyAndRespectsOffsets() {
        composeTestRule.setContent {
            Subspace {
                SpatialBox(modifier = SubspaceModifier.offset(x = 10.dp, y = 20.dp, z = 30.dp)) {
                    ApplicationSubspace(
                        modifier = SubspaceModifier.offset(x = 40.dp, y = 50.dp, z = 60.dp)
                    ) {
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
    fun applicationSubspace_nestedInApplicationSubspace_rendersContentAndRespectsOffsets() {
        composeTestRule.setContent {
            ApplicationSubspace {
                SpatialBox(modifier = SubspaceModifier.offset(x = 10.dp, y = 20.dp, z = 30.dp)) {
                    ApplicationSubspace(
                        modifier = SubspaceModifier.offset(x = 40.dp, y = 50.dp, z = 60.dp)
                    ) {
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
    fun subspace_xrEnabled_contentIsCreated() {
        composeTestRule.setContent {
            Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun applicationSubspace_recommendedBoxed_xrEnabled_contentIsCreated() {
        composeTestRule.setContent {
            ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun subspace_nonXr_contentIsNotCreated() {
        composeTestRule.disableXr()

        composeTestRule.setContent {
            Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
    }

    @Test
    fun applicationSubspace_recommendedBoxed_nonXr_contentIsNotCreated() {
        composeTestRule.disableXr()

        composeTestRule.setContent {
            ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
    }

    @Test
    fun subspace_contentIsParentedToTheKeyEntity() {
        composeTestRule.setContent {
            Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        val node = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panel = node.semanticsEntity
        val subspaceBox = panel?.parent
        val session = assertNotNull(composeTestRule.session)
        val subspaceRootEntity = assertNotNull(subspaceBox?.parent)
        val subspaceRootContainerEntity = assertNotNull(subspaceRootEntity.parent)
        assertThat(subspaceRootContainerEntity).isEqualTo(session.scene.keyEntity)
    }

    @Test
    fun applicationSubspace_recommendedBoxed_contentIsParentedToTheKeyEntity() {
        composeTestRule.setContent {
            ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        val node = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panel = node.semanticsEntity
        val subspaceBox = panel?.parent
        val session = assertNotNull(composeTestRule.session)
        val subspaceRootEntity = assertNotNull(subspaceBox?.parent)
        val subspaceRootContainerEntity = assertNotNull(subspaceRootEntity.parent)
        assertThat(subspaceRootContainerEntity).isEqualTo(session.scene.keyEntity)
    }

    @Test
    fun subspace_panelEmbedded_contentIsParentedToContainingPanel() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Subspace { SpatialPanel(SubspaceModifier.testTag("innerPanel")) {} }
                }
            }
        }

        val outerPanelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val outerPanelEntity = outerPanelNode.semanticsEntity
        val innerPanelNode =
            composeTestRule.onSubspaceNodeWithTag("innerPanel").fetchSemanticsNode()
        val innerPanelEntity = innerPanelNode.semanticsEntity
        val subspaceBoxEntity = innerPanelEntity?.parent
        val subspaceLayoutEntity = subspaceBoxEntity?.parent
        val subspaceRootEntity = subspaceLayoutEntity?.parent
        val subspaceRootContainerEntity = subspaceRootEntity?.parent
        val parentPanel = subspaceRootContainerEntity?.parent
        assertNotNull(parentPanel)
        assertThat(parentPanel).isEqualTo(outerPanelEntity)
    }

    @Test
    fun subspace_panelEmbedded_contentIsEnabledWhenContentSizeMatchesParentSize() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp).testTag("panel")) {
                    Subspace {
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
    fun subspace_panelEmbedded_depthConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.depth(10.dp).testTag("panel")) {
                    Subspace {
                        SpatialPanel(SubspaceModifier.depth(20.dp).testTag("innerPanel")) {}
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertDepthIsEqualTo(10.dp)
    }

    @Test
    fun subspace_panelEmbedded_fillMaxDepth() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.depth(10.dp).testTag("panel")) {
                    Subspace {
                        SpatialPanel(SubspaceModifier.fillMaxDepth().testTag("innerPanel")) {}
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertDepthIsEqualTo(10.dp)
    }

    @Test
    fun subspace_panelEmbedded_unboundedDepth() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.sizeIn(maxDepth = VolumeConstraints.INFINITY.dp)
                        .testTag("panel")
                ) {
                    Subspace {
                        SpatialPanel(SubspaceModifier.depth(20.dp).testTag("innerPanel")) {}
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("innerPanel").assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun applicationSubspace_recommendedBoxed_panelEmbedded_contentIsParentedToContainingPanel() {
        composeTestRule.setContent {
            ApplicationSubspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Subspace { SpatialPanel(SubspaceModifier.testTag("innerPanel")) {} }
                }
            }
        }

        val outerPanelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val outerPanelEntity = outerPanelNode.semanticsEntity
        val innerPanelNode =
            composeTestRule.onSubspaceNodeWithTag("innerPanel").fetchSemanticsNode()
        val innerPanelEntity = innerPanelNode.semanticsEntity
        val subspaceBoxEntity = innerPanelEntity?.parent
        val subspaceLayoutEntity = subspaceBoxEntity?.parent
        val subspaceRootEntity = subspaceLayoutEntity?.parent
        val subspaceRootContainerEntity = subspaceRootEntity?.parent
        val parentPanel = subspaceRootContainerEntity?.parent
        assertNotNull(parentPanel)
        assertThat(parentPanel).isEqualTo(outerPanelEntity)
    }

    @Test
    fun subspace_isDisposed() {
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
    fun applicationSubspace_recommendedBoxed_isDisposed() {
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            if (showSubspace) {
                ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        showSubspace = false

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(0)
    }

    @Test
    fun subspace_onlyOneSceneExists_afterSpaceModeChanges() {
        val fakeRuntime = createFakeRuntime(composeTestRule.activity)
        val testSceneRuntime = TestSceneRuntime.create(fakeRuntime)
        composeTestRule.session = createFakeSession(composeTestRule.activity, testSceneRuntime)

        composeTestRule.setContent {
            Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        testSceneRuntime.requestHomeSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        testSceneRuntime.requestFullSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)
    }

    @Test
    fun applicationSubspace_recommendedBoxed_onlyOneSceneExists_afterSpaceModeChanges() {
        val fakeRuntime = createFakeRuntime(composeTestRule.activity)
        composeTestRule.session = createFakeSession(composeTestRule.activity, fakeRuntime)

        composeTestRule.setContent {
            ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        fakeRuntime.requestHomeSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        fakeRuntime.requestFullSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)
    }

    @Test
    fun subspace_fillMaxSize_returnsRecommendedContentBoxSizeConstraints() {
        var density: Density? = null
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
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
    fun subspace_fillMaxSize_higherDensity_returnsCorrectConstraints() {
        var density: Density? = null
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                density = LocalDensity.current
                Subspace { SpatialBox(SubspaceModifier.fillMaxSize(1.0f).testTag("box")) {} }
            }
        }

        assertNotNull(density)
        assertThat(density.density).isEqualTo(2f)
        val expectedWidthPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS).roundToPx(this) }
        val expectedHeightPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.HEIGHT_METERS).roundToPx(this) }
        val expectedDepthPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.DEPTH_METERS).roundToPx(this) }
        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .assertDepthIsEqualTo(expectedDepthPx.toDp())
    }

    @Test
    fun applicationSubspace_fillMaxSize_returnsRecommendedContentBoxSizeConstraints() {
        var density: Density? = null
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
        composeTestRule.setContent {
            density = LocalDensity.current
            ApplicationSubspace { SpatialBox(SubspaceModifier.fillMaxSize(1.0f).testTag("box")) {} }
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
    fun applicationSubspace_unbounded_fillMaxSize_doesNotReturnCorrectWidthAndHeight() {
        composeTestRule.setContent {
            ApplicationSubspace(allowUnboundedSubspace = true) {
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
    fun applicationSubspace_customBounded_fillMaxSize_returnsCorrectWidthAndHeight() {
        composeTestRule.setContent {
            ApplicationSubspace(modifier = SubspaceModifier.sizeIn(0.dp, 100.dp, 0.dp, 100.dp)) {
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
    fun applicationSubspace_allowUnboundedSubspaceIsTrue_isUnbounded() {
        var density: Density? = null
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
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
            ApplicationSubspace(allowUnboundedSubspace = true) {
                SpatialPanel(
                    SubspaceModifier.width(widthLargerThanRecommendedBox)
                        .height(heightLargerThanRecommendedBox)
                        .depth(depthLargerThanRecommendedBox)
                        .testTag("panel")
                ) {}
            }
        }

        val recommendedWidthPx =
            with(density!!) { Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS).roundToPx(this) }
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
    fun applicationSubspace_userProvidedModifierBiggerThanDefault_isRespected() {
        val largeSize = 500000000.dp
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
        composeTestRule.setContent {
            // The user provides a modifier bigger than the recommended box.
            ApplicationSubspace(modifier = SubspaceModifier.size(largeSize)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(largeSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(largeSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(largeSize)
    }

    @Test
    fun applicationSubspace_userProvidedModifierSmallerThanDefault_isRespected() {
        val smallSize = 2.dp
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
        composeTestRule.setContent {
            // The user provides a modifier smaller than the recommended box.
            ApplicationSubspace(modifier = SubspaceModifier.size(smallSize)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(smallSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(smallSize)
        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(smallSize)
    }

    @Test
    fun applicationSubspace_constraintsChange_shouldRecomposeAndChangeConstraints() {
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
            ApplicationSubspace(modifier = constraintsState.value) {
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
    fun privateApplicationSubspace_mainPanelEntityDisabled_whenSubspaceLeavesComposition() {
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            if (showSubspace) {
                ApplicationSubspace {}
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
    fun applicationSubspace_retainsState_whenSwitchingModes() {
        val testSceneRuntime = createFakeRuntime(composeTestRule.activity)
        composeTestRule.session = createFakeSession(composeTestRule.activity, testSceneRuntime)

        composeTestRule.setContent {
            ApplicationSubspace {
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

        testSceneRuntime.requestHomeSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        testSceneRuntime.requestFullSpaceMode()
        composeTestRule.onNodeWithText("Increment").performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("5")
    }

    @Test
    fun applicationSubspace_retainsState_whenSwitchingModesStartingFromHomeSpace() {
        composeTestRule.session =
            createFakeSession(composeTestRule.activity).apply { scene.requestHomeSpaceMode() }

        composeTestRule.setContent {
            CompositionLocalProvider {
                ApplicationSubspace {
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

        // Switch to home space one last time and verify the final state.
        composeTestRule.session!!.scene.requestHomeSpaceMode()
        assertStateIs(5)
    }

    @Test
    fun applicationSubspace_usesProvidedRootContainer() {
        var testNode: Entity? = null

        composeTestRule.setContent {
            testNode = GroupEntity.create(LocalSession.current!!, "TestRoot")
            CompositionLocalProvider(LocalSubspaceRootNode provides testNode) {
                ApplicationSubspace { SpatialBox(modifier = SubspaceModifier.testTag("Box")) {} }
            }
        }

        val boxNode = composeTestRule.onSubspaceNodeWithTag("Box").fetchSemanticsNode()
        val boxEntity = assertNotNull(boxNode.semanticsEntity)
        val layoutRootEntity = assertNotNull(boxEntity.parent)
        val subspaceRootEntity = assertNotNull(layoutRootEntity.parent)
        val subspaceRootContainer = assertNotNull(subspaceRootEntity.parent)

        assertThat(testNode).isEqualTo(subspaceRootContainer)
    }

    @Test
    fun applicationSubspace_multipleApplicationSubspaces_haveTheSameRootContainer() {
        composeTestRule.setContent {
            ApplicationSubspace { SpatialBox(modifier = SubspaceModifier.testTag("Box")) {} }
            ApplicationSubspace { SpatialBox(modifier = SubspaceModifier.testTag("Box2")) {} }
        }

        val boxNode = composeTestRule.onSubspaceNodeWithTag("Box").fetchSemanticsNode()
        val boxEntity = assertNotNull(boxNode.semanticsEntity)
        val layoutRootEntity = assertNotNull(boxEntity.parent)
        val subspaceRootEntity = assertNotNull(layoutRootEntity.parent)
        val subspaceRootContainer = assertNotNull(subspaceRootEntity.parent)
        val boxNode2 = composeTestRule.onSubspaceNodeWithTag("Box2").fetchSemanticsNode()
        val boxEntity2 = assertNotNull(boxNode2.semanticsEntity)
        val layoutRootEntity2 = assertNotNull(boxEntity2.parent)
        val subspaceRootEntity2 = assertNotNull(layoutRootEntity2.parent)
        val subspaceRootContainer2 = assertNotNull(subspaceRootEntity2.parent)

        assertThat(subspaceRootContainer).isEqualTo(subspaceRootContainer2)
    }

    @Test
    fun gravityAlignedSubspace_alreadyInGravityAlignedSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "Gravity Aligned Subspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                GravityAlignedSubspace {
                    GravityAlignedSubspace {
                        SpatialPanel(
                            SubspaceModifier.fillMaxWidth().fillMaxHeight().testTag("innerPanel")
                        ) {}
                    }
                }
            }
        }
    }

    @Test
    fun gravityAlignedSubspace_recommendedBoxed_xrEnabled_contentIsCreated() {
        composeTestRule.setContent {
            GravityAlignedSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun gravityAlignedSubspace_recommendedBoxed_nonXr_contentIsNotCreated() {
        composeTestRule.disableXr()

        composeTestRule.setContent {
            GravityAlignedSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
    }

    @Test
    fun gravityAlignedSubspace_recommendedBoxed_contentIsParentedToActivitySpace() {
        composeTestRule.setContent {
            GravityAlignedSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        val node = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panel = node.semanticsEntity
        val subspaceBox = panel?.parent
        val session = assertNotNull(composeTestRule.session)
        val subspaceRootEntity = assertNotNull(subspaceBox?.parent)
        val subspaceRootContainerEntity = assertNotNull(subspaceRootEntity.parent)
        assertThat(subspaceRootContainerEntity).isEqualTo(session.scene.activitySpace)
    }

    @Test
    fun gravityAlignedSubspace_panelEmbedded_contentIsParentedToContainingPanel() {
        composeTestRule.setContent {
            GravityAlignedSubspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Subspace { SpatialPanel(SubspaceModifier.testTag("innerPanel")) {} }
                }
            }
        }

        val outerPanelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val outerPanelEntity = outerPanelNode.semanticsEntity
        val innerPanelNode =
            composeTestRule.onSubspaceNodeWithTag("innerPanel").fetchSemanticsNode()
        val innerPanelEntity = innerPanelNode.semanticsEntity
        val subspaceBoxEntity = innerPanelEntity?.parent
        val subspaceLayoutEntity = subspaceBoxEntity?.parent
        val subspaceRootEntity = subspaceLayoutEntity?.parent
        val subspaceRootContainerEntity = subspaceRootEntity?.parent
        val parentPanel = subspaceRootContainerEntity?.parent
        assertNotNull(parentPanel)
        assertThat(parentPanel).isEqualTo(outerPanelEntity)
    }

    @Test
    fun gravityAlignedSubspace_recommendedBoxed_isDisposed() {
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            if (showSubspace) {
                GravityAlignedSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        showSubspace = false

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(0)
    }

    @Test
    fun gravityAlignedSubspace_recommendedBoxed_onlyOneSceneExists_afterSpaceModeChanges() {
        val fakeRuntime = createFakeRuntime(composeTestRule.activity)
        composeTestRule.session = createFakeSession(composeTestRule.activity, fakeRuntime)

        composeTestRule.setContent {
            GravityAlignedSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        fakeRuntime.requestHomeSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)

        fakeRuntime.requestFullSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount(composeTestRule.activity)).isEqualTo(1)
    }

    @Test
    fun gravityAlignedSubspace_unbounded_asNestedInSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "Gravity Aligned Subspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel { GravityAlignedSubspace(allowUnboundedSubspace = true) {} }
                }
            }
        }
    }

    @Test
    fun gravityAlignedSubspace_customBounded_asNestedInSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "Gravity Aligned Subspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel {
                        GravityAlignedSubspace(
                            modifier = SubspaceModifier.sizeIn(0.dp, 100.dp, 0.dp, 100.dp)
                        ) {}
                    }
                }
            }
        }
    }

    @Test
    fun gravityAlignedSubspace_unbounded_asNestedInUnboundedApplicationSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "Gravity Aligned Subspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                GravityAlignedSubspace(allowUnboundedSubspace = true) {
                    SpatialPanel() { GravityAlignedSubspace(allowUnboundedSubspace = true) {} }
                }
            }
        }
    }

    @Test
    fun gravityAlignedSubspace_customBounded_asNestedinCustomBoundedApplicationSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "Gravity Aligned Subspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                GravityAlignedSubspace(
                    modifier = SubspaceModifier.sizeIn(0.dp, 50.dp, 0.dp, 50.dp)
                ) {
                    SpatialPanel {
                        GravityAlignedSubspace(
                            modifier = SubspaceModifier.sizeIn(0.dp, 100.dp, 0.dp, 100.dp)
                        ) {}
                    }
                }
            }
        }
    }

    @Test
    fun gravityAlignedSubspace_fillMaxSize_higherDensity_returnsCorrectConstraints() {
        var density: Density? = null
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                density = LocalDensity.current
                GravityAlignedSubspace {
                    SpatialBox(
                        SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                    ) {}
                }
            }
        }

        assertNotNull(density)
        assertThat(density.density).isEqualTo(2f)
        val expectedWidthPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS).roundToPx(this) }
        val expectedHeightPx =
            with(density) { Meter(DefaultTestRecommendedBoxSize.HEIGHT_METERS).roundToPx(this) }
        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
    }

    @Test
    fun gravityAlignedSubspace_unbounded_fillMaxSize_doesNotReturnCorrectWidthAndHeight() {
        composeTestRule.setContent {
            GravityAlignedSubspace(allowUnboundedSubspace = true) {
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
    }

    @Test
    fun gravityAlignedSubspace_customBounded_fillMaxSize_returnsCorrectWidthAndHeight() {
        SubspaceModifier.sizeIn(0.dp, 100.dp, 0.dp, 100.dp)

        composeTestRule.setContent {
            GravityAlignedSubspace(modifier = SubspaceModifier.sizeIn(0.dp, 100.dp, 0.dp, 100.dp)) {
                SpatialBox(
                    SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(100.toDp())
            .assertHeightIsEqualTo(100.toDp())
    }

    @Test
    fun gravityAlignedSubspace_allowUnboundedSubspaceIsTrue_isUnbounded() {
        var density: Density? = null
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
        composeTestRule.setContent {
            density = LocalDensity.current
            // This large width is explicitly bigger than the recommended box width.
            val widthLargerThanRecommendedBox =
                with(LocalDensity.current) {
                    Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS + 1000000.0f)
                        .roundToPx(this)
                        .toDp()
                }
            GravityAlignedSubspace(allowUnboundedSubspace = true) {
                SpatialPanel(
                    SubspaceModifier.size(widthLargerThanRecommendedBox).testTag("panel")
                ) {}
            }
        }

        val recommendedWidthDp =
            with(density!!) {
                Meter(DefaultTestRecommendedBoxSize.WIDTH_METERS).roundToPx(this).toDp()
            }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsAtLeast(recommendedWidthDp)
    }

    @Test
    fun gravityAlignedSubspace_userProvidedModifierBiggerThanDefault_isRespected() {
        val largeWidth = 500000000.dp
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
        composeTestRule.setContent {
            // The user provides a modifier bigger than the recommended box.
            GravityAlignedSubspace(modifier = SubspaceModifier.size(largeWidth)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(largeWidth)
    }

    @Test
    fun gravityAlignedSubspace_userProvidedModifierSmallerThanDefault_isRespected() {
        val smallWidth = 2.dp
        val runtime = createAdapterWithRecommendedBox()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)
        composeTestRule.setContent {
            // The user provides a modifier smaller than the recommended box.
            GravityAlignedSubspace(modifier = SubspaceModifier.size(smallWidth)) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(smallWidth)
    }

    @Test
    fun gravityAlignedSubspace_constraintsChange_shouldRecomposeAndChangeConstraints() {
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
        val constraintsState = mutableStateOf<SubspaceModifier>(initialConstraints)

        composeTestRule.setContent {
            GravityAlignedSubspace(modifier = constraintsState.value) {
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
    fun privateGravityAlignedSubspace_mainPanelEntityDisabled_whenSubspaceLeavesComposition() {
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            if (showSubspace) {
                GravityAlignedSubspace {}
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
    fun gravityAlignedSubspace_retainsState_whenSwitchingModes() {
        composeTestRule.session = createFakeSession(composeTestRule.activity)

        composeTestRule.setContent {
            GravityAlignedSubspace {
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

        composeTestRule.session!!.scene.requestHomeSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        composeTestRule.session!!.scene.requestFullSpaceMode()
        composeTestRule.onNodeWithText("Increment").performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("5")
    }

    @Test
    fun gravityAlignedSubspace_retainsState_whenSwitchingModesStartingFromHomeSpace() {
        composeTestRule.session =
            createFakeSession(composeTestRule.activity).apply { scene.requestHomeSpaceMode() }

        composeTestRule.setContent {
            CompositionLocalProvider {
                GravityAlignedSubspace {
                    SpatialPanel {
                        var state by remember { mutableStateOf(0) }
                        Button(onClick = { state++ }) { Text("Increment") }
                        Text("$state", modifier = Modifier.testTag("state"))
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("state").assertTextContains("0")

        composeTestRule.session!!.scene.requestFullSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("0")

        composeTestRule.onNodeWithText("Increment").performClick().performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        composeTestRule.session!!.scene.requestHomeSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        composeTestRule.session!!.scene.requestFullSpaceMode()
        composeTestRule.onNodeWithText("Increment").performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("5")

        composeTestRule.session!!.scene.requestHomeSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("5")
    }

    @Test
    fun gravityAlignedSubspace_multipleApplicationSubspaces_haveTheSameRootContainer() {
        composeTestRule.setContent {
            GravityAlignedSubspace { SpatialBox(modifier = SubspaceModifier.testTag("Box")) {} }
            GravityAlignedSubspace { SpatialBox(modifier = SubspaceModifier.testTag("Box2")) {} }
        }

        val boxNode = composeTestRule.onSubspaceNodeWithTag("Box").fetchSemanticsNode()
        val boxEntity = assertNotNull(boxNode.semanticsEntity)
        val layoutRootEntity = assertNotNull(boxEntity.parent)
        val subspaceRootEntity = assertNotNull(layoutRootEntity.parent)
        val subspaceRootContainer = assertNotNull(subspaceRootEntity.parent)
        val boxNode2 = composeTestRule.onSubspaceNodeWithTag("Box2").fetchSemanticsNode()
        val boxEntity2 = assertNotNull(boxNode2.semanticsEntity)
        val layoutRootEntity2 = assertNotNull(boxEntity2.parent)
        val subspaceRootEntity2 = assertNotNull(layoutRootEntity2.parent)
        val subspaceRootContainer2 = assertNotNull(subspaceRootEntity2.parent)

        assertThat(subspaceRootContainer).isEqualTo(subspaceRootContainer2)
    }

    @Test
    fun anchoredSubspace_whenCreated_isParentedToProvidedAnchorEntity() {
        composeTestRule.session = createFakeSession(composeTestRule.activity)
        val session = assertNotNull(composeTestRule.session)

        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

        composeTestRule.setContent {
            AnchoredSubspace(lockTo = anchorEntity) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        val node = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panel = node.semanticsEntity
        val subspaceBox = panel?.parent
        val subspaceRootEntity = assertNotNull(subspaceBox?.parent)
        val subspaceRootContainerEntity = assertNotNull(subspaceRootEntity.parent)
        assertThat(subspaceRootContainerEntity).isEqualTo(anchorEntity)
    }

    @Test
    fun anchoredSubspace_withContent_positionsContentAtOrigin() {
        composeTestRule.session = createFakeSession(composeTestRule.activity)
        val session = assertNotNull(composeTestRule.session)

        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

        composeTestRule.setContent {
            AnchoredSubspace(lockTo = anchorEntity) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        // TODO(b/448999330): check the panelWorldPose when fake setPose is fixed
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun anchoredSubspace_whenNested_positionsContentRelativeToAnchor() {
        composeTestRule.session = createFakeSession(composeTestRule.activity)
        val session = assertNotNull(composeTestRule.session)

        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

        composeTestRule.setContent {
            ApplicationSubspace(
                modifier = SubspaceModifier.offset(x = 40.dp, y = 50.dp, z = 60.dp)
            ) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("subspacePanel")) {}
                AnchoredSubspace(lockTo = anchorEntity) {
                    SpatialPanel(SubspaceModifier.fillMaxSize().testTag("anchoredSubspacePanel")) {}
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("anchoredSubspacePanel")
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
    fun anchoredSubspace_contentIsAnchoredToIdentityPosition() {
        var session = assertIs<SessionCreateSuccess>(Session.create(composeTestRule.activity))
        composeTestRule.session = session.session

        val anchorResult = Anchor.create(session.session, Pose.Identity)
        val success = assertIs<AnchorCreateSuccess>(anchorResult)
        val anchorEntity = AnchorEntity.create(session.session, anchor = success.anchor)

        composeTestRule.setContent {
            AnchoredSubspace(lockTo = anchorEntity) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun anchoredSubspace_whenLocked_contentIsWorldPositionedCorrectly() {
        val session = assertIs<SessionCreateSuccess>(Session.create(composeTestRule.activity))
        composeTestRule.session = session.session

        val anchorResult = Anchor.create(session.session, Pose(Vector3(20.0f, 30.0f, 40.0f)))
        val success = assertIs<AnchorCreateSuccess>(anchorResult)
        val anchorEntity = AnchorEntity.create(session.session, anchor = success.anchor)

        composeTestRule.setContent {
            AnchoredSubspace(lockTo = anchorEntity) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        val node = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelEntity = assertNotNull(node.semanticsEntity)

        val anchorWorldPose = anchorEntity.getPose(Space.REAL_WORLD)
        val panelWorldPose = panelEntity.getPose(Space.REAL_WORLD)
        assertThat(anchorWorldPose).isEqualTo(Pose(Vector3(20.0f, 30.0f, 40.0f)))
        assertThat(panelWorldPose).isEqualTo(Pose(Vector3(20.0f, 30.0f, 40.0f)))

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertExists()
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun anchoredSubspace_whenAnchorChanges_anchorsToNewAnchor() {
        var session = assertIs<SessionCreateSuccess>(Session.create(composeTestRule.activity))
        composeTestRule.session = session.session

        val initialPose = Pose(Vector3(10f, 20f, 30f), Quaternion(10f, 20f, 30f, 40f))
        val anchorResult = Anchor.create(session.session, initialPose)
        val anchorEntity =
            AnchorEntity.create(
                session.session,
                anchor = assertIs<AnchorCreateSuccess>(anchorResult).anchor,
            )

        val updatedPose = Pose(Vector3(40f, 50f, 60f), Quaternion(15f, 25f, 35f, 45f))
        val updatedAnchorResult = Anchor.create(session.session, updatedPose)
        val updatedAnchorEntity =
            AnchorEntity.create(
                session.session,
                anchor = assertIs<AnchorCreateSuccess>(updatedAnchorResult).anchor,
            )

        val currentAnchorState = mutableStateOf(anchorEntity)

        composeTestRule.setContent {
            val lockEntity = assertNotNull(currentAnchorState.value)
            AnchoredSubspace(lockTo = lockEntity) {
                SpatialPanel(SubspaceModifier.fillMaxSize().testTag("panel")) {}
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelEntity = assertNotNull(panelNode.semanticsEntity)
        assertThat(panelEntity.getPose(Space.REAL_WORLD)).isEqualTo(initialPose)
        composeTestRule.onSubspaceNodeWithTag("panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)

        currentAnchorState.value = updatedAnchorEntity
        composeTestRule.waitForIdle()

        assertThat(panelEntity.getPose(Space.REAL_WORLD)).isEqualTo(updatedPose)

        composeTestRule.onSubspaceNodeWithTag("panel").assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
    }
}
