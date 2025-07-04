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
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.SceneManager
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestJxrPlatformAdapter
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertHeightIsNotEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.assertWidthIsNotEqualTo
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.toDp
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SubspaceTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun subspace_alreadyInSubspace_justRendersContentDirectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    Subspace {
                        SpatialPanel(
                            SubspaceModifier.fillMaxWidth().fillMaxHeight().testTag("innerPanel")
                        ) {}
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Width dp = 1151.856 dp / meter * 1.73 meter
        // Width px = 1992.7108799999999 * 1 (density)
        composeTestRule
            .onSubspaceNodeWithTag("innerPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(1993.toDp())
            .assertHeightIsEqualTo(1854.toDp())
    }

    @Test
    fun applicationSubspace_alreadyInApplicationSubspace_justRendersContentDirectly() {
        composeTestRule.setContent {
            TestSetup {
                ApplicationSubspace {
                    ApplicationSubspace {
                        SpatialPanel(
                            SubspaceModifier.fillMaxWidth().fillMaxHeight().testTag("innerPanel")
                        ) {}
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Width dp = 1151.856 dp / meter * 1.73 meter
        // Width px = 1992.7108799999999 * 1 (density)
        composeTestRule
            .onSubspaceNodeWithTag("innerPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(1993.toDp())
            .assertHeightIsEqualTo(1854.toDp())
    }

    @Test
    fun subspace_xrEnabled_contentIsCreated() {
        composeTestRule.setContent {
            TestSetup { Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} } }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun applicationSubspace_recommendedBoxed_xrEnabled_contentIsCreated() {
        composeTestRule.setContent {
            TestSetup { ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} } }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun subspace_nonXr_contentIsNotCreated() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
    }

    @Test
    fun applicationSubspace_recommendedBoxed_nonXr_contentIsNotCreated() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
    }

    @Test
    fun subspace_contentIsParentedToActivitySpace() {
        composeTestRule.setContent {
            TestSetup { Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} } }
        }

        val node = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panel = node.semanticsEntity
        val subspaceBox = panel?.parent
        val session = assertNotNull(composeTestRule.activity.session)
        val subspaceRootEntity = assertNotNull(subspaceBox?.parent)
        val subspaceRootContainerEntity = assertNotNull(subspaceRootEntity.parent)
        assertThat(subspaceRootContainerEntity).isEqualTo(session.scene.activitySpace)
    }

    @Test
    fun applicationSubspace_recommendedBoxed_contentIsParentedToActivitySpace() {
        composeTestRule.setContent {
            TestSetup { ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} } }
        }

        val node = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panel = node.semanticsEntity
        val subspaceBox = panel?.parent
        val session = assertNotNull(composeTestRule.activity.session)
        val subspaceRootEntity = assertNotNull(subspaceBox?.parent)
        val subspaceRootContainerEntity = assertNotNull(subspaceRootEntity.parent)
        assertThat(subspaceRootContainerEntity).isEqualTo(session.scene.activitySpace)
    }

    @Test
    fun subspace_nestedSubspace_contentIsParentedToContainingPanel() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        Subspace { SpatialPanel(SubspaceModifier.testTag("innerPanel")) {} }
                    }
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
    fun applicationSubspace_recommendedBoxed_nestedSubspace_contentIsParentedToContainingPanel() {
        composeTestRule.setContent {
            TestSetup {
                ApplicationSubspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        Subspace { SpatialPanel(SubspaceModifier.testTag("innerPanel")) {} }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

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
            TestSetup {
                if (showSubspace) {
                    Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)

        showSubspace = false

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
        assertThat(SceneManager.getSceneCount()).isEqualTo(0)
    }

    @Test
    fun applicationSubspace_recommendedBoxed_isDisposed() {
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            TestSetup {
                if (showSubspace) {
                    ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)

        showSubspace = false

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
        assertThat(SceneManager.getSceneCount()).isEqualTo(0)
    }

    @Test
    fun subspace_onlyOneSceneExists_afterSpaceModeChanges() {
        val fakeRuntime = createFakeRuntime(composeTestRule.activity)
        val testJxrPlatformAdapter = TestJxrPlatformAdapter.create(fakeRuntime)

        composeTestRule.setContent {
            TestSetup(runtime = testJxrPlatformAdapter) {
                Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)

        testJxrPlatformAdapter.requestHomeSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)

        testJxrPlatformAdapter.requestFullSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)
    }

    @Test
    fun applicationSubspace_recommendedBoxed_onlyOneSceneExists_afterSpaceModeChanges() {
        val fakeRuntime = createFakeRuntime(composeTestRule.activity)

        composeTestRule.setContent {
            TestSetup(runtime = fakeRuntime) {
                ApplicationSubspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)

        fakeRuntime.requestHomeSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)

        fakeRuntime.requestFullSpaceMode()

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)
    }

    @Test
    fun applicationSubspace_recommendedBoxed_asNestedInSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "ApplicationSubspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                TestSetup { Subspace { SpatialPanel { ApplicationSubspace {} } } }
            }
        }
    }

    @Test
    fun applicationSubspace_unbounded_asNestedInSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "ApplicationSubspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                TestSetup {
                    Subspace {
                        SpatialPanel { ApplicationSubspace(constraints = VolumeConstraints()) {} }
                    }
                }
            }
        }
    }

    @Test
    fun applicationSubspace_customBounded_asNestedInSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "ApplicationSubspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                TestSetup {
                    Subspace {
                        SpatialPanel {
                            ApplicationSubspace(
                                constraints =
                                    VolumeConstraints(
                                        minWidth = 0,
                                        maxWidth = 100,
                                        minHeight = 0,
                                        maxHeight = 100,
                                    )
                            ) {}
                        }
                    }
                }
            }
        }
    }

    @Test
    fun applicationSubspace_recommendedBoxed_asNestedInUnboundedApplicationSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "ApplicationSubspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                TestSetup {
                    ApplicationSubspace(constraints = VolumeConstraints()) {
                        SpatialPanel() { ApplicationSubspace {} }
                    }
                }
            }
        }
    }

    @Test
    fun applicationSubspace_unbounded_asNestedInUnboundedApplicationSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "ApplicationSubspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                TestSetup {
                    ApplicationSubspace(constraints = VolumeConstraints()) {
                        SpatialPanel() { ApplicationSubspace(constraints = VolumeConstraints()) {} }
                    }
                }
            }
        }
    }

    @Test
    fun applicationSubspace_customBounded_asNestedinCustomBoundedApplicationSubspace_throwsError() {
        assertFailsWith<IllegalStateException>(
            message = "ApplicationSubspace cannot be nested within another Subspace."
        ) {
            composeTestRule.setContent {
                TestSetup {
                    ApplicationSubspace(
                        constraints =
                            VolumeConstraints(
                                minWidth = 0,
                                maxWidth = 50,
                                minHeight = 0,
                                maxHeight = 50,
                            )
                    ) {
                        SpatialPanel {
                            ApplicationSubspace(
                                constraints =
                                    VolumeConstraints(
                                        minWidth = 0,
                                        maxWidth = 100,
                                        minHeight = 0,
                                        maxHeight = 100,
                                    )
                            ) {}
                        }
                    }
                }
            }
        }
    }

    @Test
    fun subspace_fillMaxSize_returnsCorrectWidthAndHeight() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialBox(
                        SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                    ) {}
                }
            }
        }

        // Width  = 1151.856 dp / meter * 1.73 meter
        // Width  = 1992.7108799999999 * 1 (density)
        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(1993.toDp())
            .assertHeightIsEqualTo(1854.toDp())
    }

    @Test
    fun subspace_fillMaxSize_higherDensity_returnsCorrectConstraints() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                TestSetup {
                    Subspace {
                        SpatialBox(
                            SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                        ) {}
                    }
                }
            }
        }

        // Width dp = 1151.856 dp / meter ∗ 1.73=1992.7108799999999 meter
        // Width px = 1992.7108799999999 dp ∗ 2 (density) = 3985.4217599999997
        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertWidthIsEqualTo(3985.toDp())
            .assertHeightIsEqualTo(3709.toDp())
    }

    @Test
    fun subspace_fillMaxSize_higherScale_returnsCorrectConstraints() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                TestSetup {
                    Subspace {
                        SpatialBox(
                            SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                        ) {}
                    }
                }
            }
        }

        // Width dp = 1151.856 dp / meter * 1.73 meter
        // Width px = 1992.7108799999999 * 1 (density)
        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertWidthIsEqualTo(1993.toDp())
            .assertHeightIsEqualTo(1854.toDp())
    }

    @Test fun applicationSubspace_recommendedBoxed_fillMaxSize_returnsCorrectWidthAndHeight() {}

    @Test
    fun applicationSubspace_unbounded_fillMaxSize_doesNotReturnCorrectWidthAndHeight() {
        composeTestRule.setContent {
            TestSetup {
                ApplicationSubspace(constraints = VolumeConstraints()) {
                    SpatialBox(
                        SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                    ) {}
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsNotEqualTo(VolumeConstraints().maxWidth.toDp())
            .assertHeightIsNotEqualTo(VolumeConstraints().maxHeight.toDp())
    }

    @Test
    fun applicationSubspace_customBounded_fillMaxSize_returnsCorrectWidthAndHeight() {
        val customConstraints = VolumeConstraints(0, 100, 0, 100)

        composeTestRule.setContent {
            TestSetup {
                ApplicationSubspace(constraints = customConstraints) {
                    SpatialBox(
                        SubspaceModifier.fillMaxWidth(1.0f).fillMaxHeight(1.0f).testTag("box")
                    ) {}
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(customConstraints.maxWidth.toDp())
            .assertHeightIsEqualTo(customConstraints.maxHeight.toDp())
    }

    @Test
    fun applicationSubspace_constraintsChange_shouldRecomposeAndChangeConstraints() {
        val initialConstraints =
            VolumeConstraints(
                minWidth = 0,
                maxWidth = 100,
                minHeight = 0,
                maxHeight = 100,
                minDepth = 0,
                maxDepth = VolumeConstraints.INFINITY,
            )
        val updatedConstraints =
            VolumeConstraints(
                minWidth = 50,
                maxWidth = 150,
                minHeight = 50,
                maxHeight = 150,
                minDepth = 0,
                maxDepth = VolumeConstraints.INFINITY,
            )
        val constraintsState = mutableStateOf<VolumeConstraints>(initialConstraints)

        composeTestRule.setContent {
            TestSetup {
                ApplicationSubspace(constraints = constraintsState.value) {
                    SpatialBox(
                        modifier =
                            SubspaceModifier.fillMaxWidth().fillMaxHeight().testTag("testBox")
                    ) {}
                }
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
            TestSetup {
                if (showSubspace) {
                    ApplicationSubspace {}
                }
            }
        }

        val session = composeTestRule.activity.session
        assertNotNull(session)
        val mainPanelEntity = session.scene.mainPanelEntity
        assertThat(mainPanelEntity.isEnabled()).isEqualTo(false)

        showSubspace = false
        composeTestRule.waitForIdle()

        assertThat(mainPanelEntity.isEnabled()).isEqualTo(true)
    }

    @Test
    fun applicationSubspace_retainsState_whenSwitchingModes() {
        val testJxrPlatformAdapter = createFakeRuntime(composeTestRule.activity)

        composeTestRule.setContent {
            TestSetup(runtime = testJxrPlatformAdapter) {
                ApplicationSubspace {
                    SpatialPanel {
                        var state by remember { mutableStateOf(0) }
                        Button(onClick = { state++ }) { Text("Increment") }
                        Text("$state", modifier = Modifier.testTag("state"))
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("state").assertTextContains("0")

        composeTestRule.onNodeWithText("Increment").performClick().performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        testJxrPlatformAdapter.requestHomeSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        testJxrPlatformAdapter.requestFullSpaceMode()
        composeTestRule.onNodeWithText("Increment").performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("5")
    }

    @Test
    fun applicationSubspace_retainsState_whenSwitchingModesStartingFromHomeSpace() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            CompositionLocalProvider {
                TestSetup(runtime = runtime) {
                    ApplicationSubspace {
                        SpatialPanel {
                            var state by remember { mutableStateOf(0) }
                            Button(onClick = { state++ }) { Text("Increment") }
                            Text("$state", modifier = Modifier.testTag("state"))
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("state").assertTextContains("0")

        runtime.requestFullSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("0")

        composeTestRule.onNodeWithText("Increment").performClick().performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        runtime.requestHomeSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("3")

        runtime.requestFullSpaceMode()
        composeTestRule.onNodeWithText("Increment").performClick().performClick()

        composeTestRule.onNodeWithTag("state").assertTextContains("5")

        runtime.requestHomeSpaceMode()

        composeTestRule.onNodeWithTag("state").assertTextContains("5")
    }

    @Test
    fun applicationSubspace_usesProvidedRootContainer() {
        var testNode: Entity? = null

        composeTestRule.setContent {
            TestSetup {
                testNode = GroupEntity.create(LocalSession.current!!, "TestRoot")
                CompositionLocalProvider(LocalSubspaceRootNode provides testNode) {
                    assertThat(LocalSession.current!!.scene.keyEntity).isNull()
                    ApplicationSubspace {
                        SpatialBox(modifier = SubspaceModifier.testTag("Box")) {}
                    }
                }
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
            TestSetup {
                assertThat(LocalSession.current!!.scene.keyEntity).isNull()
                ApplicationSubspace { SpatialBox(modifier = SubspaceModifier.testTag("Box")) {} }
                ApplicationSubspace { SpatialBox(modifier = SubspaceModifier.testTag("Box2")) {} }
            }
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
}
