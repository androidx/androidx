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

package androidx.xr.compose.subspace.layout

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalComposeXrOwners
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpaceChangeEvent
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@OptIn(ExperimentalSpatialContinuityApi::class)
@RunWith(AndroidJUnit4::class)
class SpatialContinuityTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @get:Rule val sceneCoreTestRule = SceneCoreTestRule()

    @Test
    fun spatialContinuityTarget_captureContinuity_setsKeyEntity() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturer)
                        .testTag("Panel1")
                ) {}
            }
        }

        val panel1 =
            composeTestRule.onSubspaceNodeWithTag("Panel1").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel1)

        var captured = false
        composeTestRule.runOnUiThread { captured = capturer.captureContinuity() }

        assertThat(captured).isTrue()
        assertThat(session.scene.keyEntity).isEqualTo(panel1)
    }

    @Test
    fun spatialContinuityTarget_callCapturerBeforeLinkToModifier_noop() {
        val session = composeTestRule.configureFakeSession()
        var captured = true

        composeTestRule.setContent {
            Subspace {
                val panelCapturer = remember { SpatialContinuityCapturer() }
                captured = panelCapturer.captureContinuity()
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(
                            spatialContinuityCapturer = panelCapturer
                        )
                        .testTag("Panel")
                ) {}
            }
        }

        val panel =
            composeTestRule.onSubspaceNodeWithTag("Panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel)

        assertThat(captured).isFalse()
        assertThat(session.scene.keyEntity).isNotEqualTo(panel)
    }

    @Test
    fun spatialContinuityTarget_detach_restoresKeyEntity() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()
        var showPanel1 by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                if (showPanel1) {
                    SpatialPanel(
                        SubspaceModifier.spatialContinuityTarget(
                                spatialContinuityCapturer = capturer
                            )
                            .testTag("Panel1")
                    ) {}
                }
            }
        }

        val panel1 =
            composeTestRule.onSubspaceNodeWithTag("Panel1").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel1)

        composeTestRule.runOnUiThread { capturer.captureContinuity() }
        assertThat(session.scene.keyEntity).isEqualTo(panel1)

        // Detach panel1
        showPanel1 = false
        composeTestRule.waitForIdle()

        assertThat(session.scene.keyEntity).isEqualTo(session.scene.mainPanelEntity)
    }

    @Test
    fun spatialContinuityTarget_detachActive_cleansUpCapturerAndResetsKeyEntity() {
        val session = composeTestRule.configureFakeSession()
        var panelCapturer: SpatialContinuityCapturer? = null
        var showPanel by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                if (showPanel) {
                    val currentCapturer = remember { SpatialContinuityCapturer() }
                    panelCapturer = currentCapturer
                    SpatialPanel(
                        SubspaceModifier.spatialContinuityTarget(
                                spatialContinuityCapturer = currentCapturer
                            )
                            .testTag("Panel")
                    ) {}
                }
            }
        }

        val panel =
            composeTestRule.onSubspaceNodeWithTag("Panel").fetchSemanticsNode().semanticsEntity
        val capturer = assertNotNull(panelCapturer)
        assertNotNull(panel)

        // Verify capturer references are populated when attached.
        assertThat(capturer.nodes.isNotEmpty()).isTrue()

        composeTestRule.runOnUiThread {
            assertThat(capturer.captureContinuity()).isTrue()
        }
        assertThat(session.scene.keyEntity).isEqualTo(panel)

        // Detach panel
        showPanel = false
        composeTestRule.waitForIdle()

        val mainPanel = session.scene.mainPanelEntity

        // Verify capturer references are cleaned up on detach and keyEntity is reset.
        assertThat(capturer.nodes.isEmpty()).isTrue()
        assertThat(session.scene.keyEntity).isEqualTo(mainPanel)

        // Calling capturer after detach should be a no-op and return false.
        composeTestRule.runOnUiThread {
            assertThat(capturer.captureContinuity()).isFalse()
        }
        assertThat(session.scene.keyEntity).isEqualTo(mainPanel)
    }

    @Test
    fun spatialContinuityTarget_swapCapturers_cleansUpOldCapturerAndAttachesNewCapturer() {
        val session = composeTestRule.configureFakeSession()
        val capturerA = SpatialContinuityCapturer()
        val capturerB = SpatialContinuityCapturer()
        var capturerState by mutableStateOf(capturerA)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(
                            spatialContinuityCapturer = capturerState
                        )
                        .testTag("Panel1")
                ) {}
            }
        }

        val panel1 =
            composeTestRule.onSubspaceNodeWithTag("Panel1").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel1)

        // Initially capturerA is attached
        assertThat(capturerA.nodes.isNotEmpty()).isTrue()
        assertThat(capturerB.nodes.isEmpty()).isTrue()

        // Swap to capturerB
        capturerState = capturerB
        composeTestRule.waitForIdle()

        // capturerA references should be cleaned up, and capturerB should now be attached
        assertThat(capturerA.nodes.isEmpty()).isTrue()
        assertThat(capturerB.nodes.isNotEmpty()).isTrue()

        // Calling capturerA is a no-op (keyEntity remains mainPanelEntity)
        composeTestRule.runOnUiThread {
            assertThat(capturerA.captureContinuity()).isFalse()
        }
        assertThat(session.scene.keyEntity).isEqualTo(session.scene.mainPanelEntity)

        // Calling capturerB sets keyEntity to panel1
        composeTestRule.runOnUiThread {
            assertThat(capturerB.captureContinuity()).isTrue()
        }
        assertThat(session.scene.keyEntity).isEqualTo(panel1)
    }

    @Test
    fun spatialContinuityTarget_alternatingCaptureContinuity_switchesKeyEntityBetweenPanels() {
        val session = composeTestRule.configureFakeSession()
        val capturer1 = SpatialContinuityCapturer()
        val capturer2 = SpatialContinuityCapturer()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturer1)
                        .testTag("Panel1")
                ) {}
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturer2)
                        .testTag("Panel2")
                ) {}
            }
        }

        val panel1 =
            composeTestRule.onSubspaceNodeWithTag("Panel1").fetchSemanticsNode().semanticsEntity
        val panel2 =
            composeTestRule.onSubspaceNodeWithTag("Panel2").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel1)
        assertNotNull(panel2)

        // Initially neither capturer has been invoked, so keyEntity is default, main panel.
        assertThat(session.scene.keyEntity).isEqualTo(session.scene.mainPanelEntity)

        composeTestRule.runOnUiThread { capturer1.captureContinuity() }
        assertThat(session.scene.keyEntity).isEqualTo(panel1)

        composeTestRule.runOnUiThread { capturer2.captureContinuity() }
        assertThat(session.scene.keyEntity).isEqualTo(panel2)

        composeTestRule.runOnUiThread { capturer1.captureContinuity() }
        assertThat(session.scene.keyEntity).isEqualTo(panel1)
    }

    @Test
    fun spatialContinuityTarget_repeatedCaptureContinuity_remainsKeyEntity() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturer)
                        .testTag("Panel1")
                ) {}
            }
        }

        val panel1 =
            composeTestRule.onSubspaceNodeWithTag("Panel1").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel1)

        // Initially keyEntity is default, main panel.
        assertThat(session.scene.keyEntity).isEqualTo(session.scene.mainPanelEntity)

        composeTestRule.runOnUiThread { capturer.captureContinuity() }
        assertThat(session.scene.keyEntity).isEqualTo(panel1)

        // Repeated subsequent calls on the same capturer keeps panel1 as keyEntity
        composeTestRule.runOnUiThread { capturer.captureContinuity() }
        assertThat(session.scene.keyEntity).isEqualTo(panel1)

        composeTestRule.runOnUiThread { capturer.captureContinuity() }
        assertThat(session.scene.keyEntity).isEqualTo(panel1)
    }

    @Test
    fun spatialContinuityTarget_keepInFocusTrue_alignsKeyEntity() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturer)
                        .testTag("Panel")
                ) {}
            }
        }

        val panelEntity =
            composeTestRule.onSubspaceNodeWithTag("Panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(panelEntity)

        // Set panel as keyEntity
        composeTestRule.runOnUiThread { capturer.captureContinuity(keepInFocus = true) }
        assertThat(session.scene.keyEntity).isEqualTo(panelEntity)

        // Simulate a space change. The recommended pose is where the content should spawn.
        val recommendedPose =
            Pose(translation = Vector3(x = 10f, y = 20f, z = 30f), rotation = Quaternion.Identity)
        composeTestRule.runOnIdle {
            sceneCoreTestRule.sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = recommendedPose, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        assertThat(panelEntity.getPose(relativeTo = Space.ACTIVITY)).isEqualTo(recommendedPose)
    }

    @Test
    fun spatialContinuityTarget_whenRecommendedScaleSet_updatesSubspaceScaleAndAlignsKeyEntity() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()
        var subspaceRootNode: Entity? = null

        composeTestRule.setContent {
            subspaceRootNode = LocalComposeXrOwners.current.subspaceRootNode
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturer)
                        .testTag("Panel")
                ) {}
            }
        }

        val panelEntity =
            composeTestRule.onSubspaceNodeWithTag("Panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(panelEntity)

        composeTestRule.runOnUiThread { capturer.captureContinuity(keepInFocus = true) }
        assertThat(session.scene.keyEntity).isEqualTo(panelEntity)

        val recommendedPose =
            Pose(translation = Vector3(x = 10f, y = 20f, z = 30f), rotation = Quaternion.Identity)
        val recommendedScale = 2.5f
        composeTestRule.runOnIdle {
            sceneCoreTestRule.sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(
                    recommendedPose = recommendedPose,
                    recommendedScale = recommendedScale,
                )
            )
        }
        composeTestRule.waitForIdle()

        assertThat(panelEntity.getPose(relativeTo = Space.ACTIVITY)).isEqualTo(recommendedPose)
        assertThat(subspaceRootNode?.getScale(relativeTo = Space.ACTIVITY))
            .isEqualTo(recommendedScale)
    }

    @Test
    fun spatialContinuityTarget_keepInFocusFalse_alignsSubspaceRootNode() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()
        var subspaceRootNode: Entity? = null

        composeTestRule.setContent {
            subspaceRootNode = LocalComposeXrOwners.current.subspaceRootNode
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturer)
                        .testTag("Panel")
                ) {}
            }
        }

        val panelEntity =
            composeTestRule.onSubspaceNodeWithTag("Panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(panelEntity)

        // Set panel as keyEntity with keepInFocus = false
        composeTestRule.runOnUiThread { capturer.captureContinuity(keepInFocus = false) }
        assertThat(session.scene.keyEntity).isEqualTo(panelEntity)

        // Simulate a space change.
        val recommendedPose =
            Pose(translation = Vector3(x = 10f, y = 20f, z = 30f), rotation = Quaternion.Identity)
        composeTestRule.runOnIdle {
            sceneCoreTestRule.sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = recommendedPose, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        // With keepInFocus = false, subspaceRootNode is placed at recommendedPose.
        assertThat(subspaceRootNode?.getPose(Space.ACTIVITY)).isEqualTo(recommendedPose)
    }

    @Test
    fun spatialContinuityTarget_alternatingActivePanels_appliesCorrectKeepInFocusState() {
        val session = composeTestRule.configureFakeSession()
        val capturerA = SpatialContinuityCapturer()
        val capturerB = SpatialContinuityCapturer()
        var subspaceRootNode: Entity? = null

        composeTestRule.setContent {
            subspaceRootNode = LocalComposeXrOwners.current.subspaceRootNode
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerA)
                        .testTag("PanelA")
                ) {}
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerB)
                        .testTag("PanelB")
                ) {}
            }
        }

        val panelA =
            composeTestRule.onSubspaceNodeWithTag("PanelA").fetchSemanticsNode().semanticsEntity
        val panelB =
            composeTestRule.onSubspaceNodeWithTag("PanelB").fetchSemanticsNode().semanticsEntity
        assertNotNull(panelA)
        assertNotNull(panelB)

        // Trigger capturerA (keepInFocus = true)
        composeTestRule.runOnUiThread { capturerA.captureContinuity(keepInFocus = true) }
        assertThat(session.scene.keyEntity).isEqualTo(panelA)

        val recommendedPose1 =
            Pose(translation = Vector3(x = 10f, y = 20f, z = 30f), rotation = Quaternion.Identity)
        composeTestRule.runOnIdle {
            sceneCoreTestRule.sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = recommendedPose1, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        // With keepInFocus = true on PanelA, PanelA is placed at recommendedPose1
        assertThat(panelA.getPose(Space.ACTIVITY)).isEqualTo(recommendedPose1)

        // Trigger capturerB (keepInFocus = false)
        composeTestRule.runOnUiThread { capturerB.captureContinuity(keepInFocus = false) }
        assertThat(session.scene.keyEntity).isEqualTo(panelB)

        val recommendedPose2 =
            Pose(translation = Vector3(40f, 50f, 60f), rotation = Quaternion.Identity)
        composeTestRule.runOnIdle {
            sceneCoreTestRule.sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = recommendedPose2, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        // With keepInFocus = false on PanelB, subspaceRootNode is placed at
        // recommendedPose2
        assertThat(subspaceRootNode?.getPose(Space.ACTIVITY)).isEqualTo(recommendedPose2)
    }

    @Test
    fun spatialContinuityTarget_keyEntityNull_subspaceRootNodeAtRecommendedPose() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()

        var subspaceRootNode: Entity? = null
        composeTestRule.setContent {
            subspaceRootNode = LocalComposeXrOwners.current.subspaceRootNode
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturer)
                        .testTag("Panel")
                ) {}
            }
        }

        val panelEntity =
            composeTestRule.onSubspaceNodeWithTag("Panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(panelEntity)

        val localPose =
            Pose(translation = Vector3(x = 1f, y = 2f, z = 3f), rotation = Quaternion.Identity)
        panelEntity.setPose(localPose, relativeTo = Space.ACTIVITY)

        // Explicitly set the keyEntity to null to test the null safety branch
        session.scene.keyEntity = null

        // Simulate a spatial mode change. The recommended pose is where the content should spawn.
        val recommendedPose =
            Pose(translation = Vector3(x = 10f, y = 20f, z = 30f), rotation = Quaternion.Identity)
        composeTestRule.runOnIdle {
            sceneCoreTestRule.sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = recommendedPose, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        // Since keyEntity is null, subspaceRootNode should be set directly to recommendedPose
        assertThat(subspaceRootNode?.getPose(relativeTo = Space.ACTIVITY))
            .isEqualTo(recommendedPose)
    }

    @Test
    fun spatialContinuityTarget_transitionFromFullSpaceToHomeSpace_movesMainPanel() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()

        composeTestRule.setContent {
            LocalComposeXrOwners.current
            if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.spatialContinuityTarget(
                                spatialContinuityCapturer = capturer
                            )
                            .testTag("Panel")
                    ) {}
                }
            }
        }
        composeTestRule.waitForIdle()

        val mainPanel = session.scene.mainPanelEntity
        val panelNode = composeTestRule.onSubspaceNodeWithTag("Panel")
        val panelEntity = panelNode.fetchSemanticsNode().semanticsEntity
        assertNotNull(panelEntity)

        composeTestRule.runOnUiThread { capturer.captureContinuity(keepInFocus = true) }

        // Move the panel to a location while in FSM.
        val newPose =
            Pose(translation = Vector3(x = 1f, y = 2f, z = 3f), rotation = Quaternion.Identity)
        panelEntity.setPose(newPose, Space.ACTIVITY)
        composeTestRule.waitForIdle()

        // Switch to HSM.
        composeTestRule.runOnIdle { session.scene.requestHomeSpace() }
        composeTestRule.waitForIdle()

        // Simulate the platform mode transition to HSM.
        composeTestRule.runOnIdle {
            sceneCoreTestRule.sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = newPose, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        assertThat(mainPanel.getPose(Space.ACTIVITY)).isEqualTo(newPose)
    }

    @Test
    fun spatialContinuityTarget_transitionFromHomeSpaceToFullSpace_movesPanel() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()

        // Start in Home Space Mode
        session.scene.requestHomeSpace()

        composeTestRule.setContent {
            LocalComposeXrOwners.current
            if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.spatialContinuityTarget(
                                spatialContinuityCapturer = capturer
                            )
                            .testTag("Panel")
                    ) {}
                }
            }
        }
        composeTestRule.waitForIdle()

        val mainPanel = session.scene.mainPanelEntity

        // Move the main panel in HSM.
        val newPose =
            Pose(translation = Vector3(x = 10f, y = 20f, z = 30f), rotation = Quaternion.Identity)
        mainPanel.setPose(newPose, Space.ACTIVITY)
        composeTestRule.waitForIdle()

        // Switch back to FSM.
        composeTestRule.runOnIdle { session.scene.requestFullSpace() }
        composeTestRule.waitForIdle()

        val panelNode = composeTestRule.onSubspaceNodeWithTag("Panel")
        val panelEntity = panelNode.fetchSemanticsNode().semanticsEntity
        assertNotNull(panelEntity)

        composeTestRule.runOnUiThread { capturer.captureContinuity(keepInFocus = true) }

        // Simulate the platform mode transition to FSM.
        composeTestRule.runOnIdle {
            sceneCoreTestRule.sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = newPose, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        assertThat(panelEntity.getPose(Space.ACTIVITY)).isEqualTo(newPose)
    }

    @Test
    fun spatialContinuityTarget_backwardNavigation_alignsKeyEntity() {
        val session = composeTestRule.configureFakeSession()
        val sceneTester = sceneCoreTestRule.sceneTester
        val capturerA = SpatialContinuityCapturer()
        val capturerB = SpatialContinuityCapturer()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerA)
                        .testTag("Panel")
                ) {}
            }
        }

        val panel =
            composeTestRule.onSubspaceNodeWithTag("Panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel)

        composeTestRule.runOnUiThread { capturerA.captureContinuity(keepInFocus = true) }
        assertThat(session.scene.keyEntity).isEqualTo(panel)

        val newPose =
            Pose(translation = Vector3(x = 1f, y = 2f, z = 3f), rotation = Quaternion.Identity)
        panel.setPose(newPose, Space.ACTIVITY)
        composeTestRule.waitForIdle()

        val intent = Intent(composeTestRule.activity, SubspaceTestingActivity::class.java)
        composeTestRule.activity.startActivity(intent)

        val controllerB = Robolectric.buildActivity(SubspaceTestingActivity::class.java, intent)
        val activityB = controllerB.create().start().resume().get()
        val sessionB = activityB.configureFakeSession()
        sessionB.scene.mainPanelEntity.parent = sessionB.scene.activitySpace

        activityB.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerB)
                ) {}
            }
        }

        // Execute backward navigation (Activity B finishes)
        activityB.finish()
        controllerB.pause().stop().destroy()

        composeTestRule.runOnIdle {
            sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = newPose, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        assertThat(panel.getPose(relativeTo = Space.ACTIVITY)).isEqualTo(newPose)
    }

    @Test
    fun spatialContinuityTarget_backwardNavigation_alignsSubspaceRootNode() {
        val session = composeTestRule.configureFakeSession()
        val sceneTester = sceneCoreTestRule.sceneTester
        val capturerA = SpatialContinuityCapturer()
        val capturerB = SpatialContinuityCapturer()

        var subspaceRootNode: Entity? = null
        composeTestRule.setContent {
            subspaceRootNode = LocalComposeXrOwners.current.subspaceRootNode
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerA)
                        .testTag("Panel")
                ) {}
            }
        }

        val panel =
            composeTestRule.onSubspaceNodeWithTag("Panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel)

        composeTestRule.runOnUiThread { capturerA.captureContinuity(keepInFocus = false) }
        assertThat(session.scene.keyEntity).isEqualTo(panel)

        val initialPose =
            Pose(translation = Vector3(x = 1f, y = 2f, z = 3f), rotation = Quaternion.Identity)
        panel.setPose(initialPose, Space.ACTIVITY)
        composeTestRule.waitForIdle()

        val intent = Intent(composeTestRule.activity, SubspaceTestingActivity::class.java)
        composeTestRule.activity.startActivity(intent)

        val controllerB = Robolectric.buildActivity(SubspaceTestingActivity::class.java, intent)
        val activityB = controllerB.create().start().resume().get()
        val sessionB = activityB.configureFakeSession()
        sessionB.scene.mainPanelEntity.parent = sessionB.scene.activitySpace

        activityB.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerB)
                ) {}
            }
        }

        // Execute backward navigation (Activity B finishes)
        activityB.finish()
        controllerB.pause().stop().destroy()

        val newPose =
            Pose(translation = Vector3(x = 10f, y = 20f, z = 30f), rotation = Quaternion.Identity)
        composeTestRule.runOnIdle {
            sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = newPose, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        // keyEntity should not be aligned with recommendedPose, but subspaceRootNode should be.
        assertThat(panel.getPose(relativeTo = Space.ACTIVITY)).isNotEqualTo(newPose)
        assertThat(subspaceRootNode?.getPose(Space.ACTIVITY)).isEqualTo(newPose)
    }

    @Test
    fun spatialContinuityTarget_transitionFromActivityAToActivityB_movesPanel() {
        val capturerA = SpatialContinuityCapturer()
        val capturerB = SpatialContinuityCapturer()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerA)
                        .testTag("PanelA")
                ) {}
            }
        }

        // Move Panel A in Activity A.
        val panelNodeA = composeTestRule.onSubspaceNodeWithTag("PanelA")
        val panelA = panelNodeA.fetchSemanticsNode().semanticsEntity
        assertNotNull(panelA)

        composeTestRule.runOnUiThread { capturerA.captureContinuity(keepInFocus = true) }

        val newPose =
            Pose(translation = Vector3(x = 1f, y = 2f, z = 3f), rotation = Quaternion.Identity)
        panelA.setPose(newPose, Space.ACTIVITY)
        composeTestRule.waitForIdle()

        val panelAActivityPose = panelA.getPose(Space.ACTIVITY)

        // Launch Activity B
        val intent = Intent(composeTestRule.activity, SubspaceTestingActivity::class.java)
        composeTestRule.activity.startActivity(intent)

        val controllerB = Robolectric.buildActivity(SubspaceTestingActivity::class.java, intent)
        val activityB = controllerB.create().start().resume().get()
        val sessionB = activityB.configureFakeSession()
        sessionB.scene.mainPanelEntity.parent = sessionB.scene.activitySpace

        activityB.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerB)
                ) {}
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread { capturerB.captureContinuity(keepInFocus = true) }

        val panelEntityB = assertNotNull(sessionB.scene.keyEntity)

        // Simulate the platform transition.
        composeTestRule.runOnIdle {
            sceneCoreTestRule.sceneTester.triggerSpaceChanged(
                SpaceChangeEvent(recommendedPose = panelAActivityPose, recommendedScale = 1f)
            )
        }
        composeTestRule.waitForIdle()

        assertThat(panelEntityB.getPose(Space.ACTIVITY)).isEqualTo(panelAActivityPose)

        activityB.finish()
        controllerB.pause().stop().destroy()
    }

    @Test
    fun spatialContinuityTarget_twoActivitiesWithSpatialContinuity_bothBecomeKeyEntity() {
        val sessionA = composeTestRule.configureFakeSession()
        val capturerA = SpatialContinuityCapturer()
        val capturerB = SpatialContinuityCapturer()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerA)
                        .testTag("PanelA")
                ) {}
            }
        }

        val panelNodeA = composeTestRule.onSubspaceNodeWithTag("PanelA")
        val panelA = panelNodeA.fetchSemanticsNode().semanticsEntity
        assertNotNull(panelA)

        composeTestRule.runOnUiThread { capturerA.captureContinuity() }
        assertThat(sessionA.scene.keyEntity).isEqualTo(panelA)

        // Launch Activity B
        val intent = Intent(composeTestRule.activity, SubspaceTestingActivity::class.java)
        composeTestRule.activity.startActivity(intent)

        val controllerB = Robolectric.buildActivity(SubspaceTestingActivity::class.java, intent)
        val activityB = controllerB.create().start().resume().visible().get()
        val sessionB = activityB.configureFakeSession()

        activityB.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturerB)
                        .testTag("PanelB")
                ) {}
            }
        }
        composeTestRule.waitForIdle()

        val panelB = activityB.findSubspaceSemanticsEntityWithTag("PanelB")
        assertNotNull(panelB)

        composeTestRule.runOnUiThread { capturerB.captureContinuity() }
        assertThat(sessionB.scene.keyEntity).isEqualTo(panelB)

        activityB.finish()
        controllerB.pause().stop().destroy()
    }

    @Test
    fun spatialContinuityTarget_captureBeforeEntityAttached_setsKeyEntityWhenAttached() = runTest {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()
        val coreModelEntity = CoreModelEntity(session.scene.virtualPixelDensity)

        composeTestRule.setContent {
            Subspace {
                SubspaceLayout(
                    modifier =
                        SubspaceModifier.spatialContinuityTarget(
                            spatialContinuityCapturer = capturer
                        ),
                    coreEntity = coreModelEntity,
                ) { _, _ ->
                    layout(100, 100, 100) {}
                }
            }
        }
        composeTestRule.waitForIdle()

        // Capture continuity before the underlying SceneCore Entity is attached to CoreModelEntity
        composeTestRule.runOnUiThread {
            assertThat(capturer.captureContinuity()).isTrue()
        }
        assertThat(session.scene.keyEntity).isEqualTo(session.scene.mainPanelEntity)

        // Attach the underlying SceneCore Entity; the queued action should set it as keyEntity
        @Suppress("NewApi") val gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        val gltfEntity = GltfModelEntity.create(session, gltfModel)
        coreModelEntity.attachEntity(gltfEntity)

        assertThat(session.scene.keyEntity).isEqualTo(gltfEntity)
    }

    @Test
    fun spatialContinuityTarget_resetContinuity_resetsKeyEntityToMainPanel() {
        val session = composeTestRule.configureFakeSession()
        val capturer = SpatialContinuityCapturer()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(spatialContinuityCapturer = capturer)
                        .testTag("Panel1")
                ) {}
            }
        }

        val panel1 =
            composeTestRule.onSubspaceNodeWithTag("Panel1").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel1)

        composeTestRule.runOnUiThread {
            assertThat(capturer.captureContinuity()).isTrue()
        }
        assertThat(session.scene.keyEntity).isEqualTo(panel1)

        composeTestRule.runOnUiThread {
            SpatialContinuityCapturer.resetContinuity(session)
        }
        assertThat(session.scene.keyEntity).isEqualTo(session.scene.mainPanelEntity)
    }

    @Test
    fun spatialContinuityTarget_singleCapturerAttachedToMultipleModifiers_appliesToLastAttached() {
        val session = composeTestRule.configureFakeSession()
        val sharedCapturer = SpatialContinuityCapturer()
        var showPanel2 by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.spatialContinuityTarget(
                            spatialContinuityCapturer = sharedCapturer
                        )
                        .testTag("Panel1")
                ) {}
                if (showPanel2) {
                    SpatialPanel(
                        SubspaceModifier.spatialContinuityTarget(
                                spatialContinuityCapturer = sharedCapturer
                            )
                            .testTag("Panel2")
                    ) {}
                }
            }
        }

        val panel1 =
            composeTestRule.onSubspaceNodeWithTag("Panel1").fetchSemanticsNode().semanticsEntity
        val panel2 =
            composeTestRule.onSubspaceNodeWithTag("Panel2").fetchSemanticsNode().semanticsEntity
        assertNotNull(panel1)
        assertNotNull(panel2)
        assertThat(sharedCapturer.nodes.size).isEqualTo(2)

        // Capturing spatial continuity applies to the last attached composable (Panel2)
        composeTestRule.runOnUiThread {
            assertThat(sharedCapturer.captureContinuity()).isTrue()
        }
        assertThat(session.scene.keyEntity).isEqualTo(panel2)

        // Detaching Panel2 resets keyEntity to mainPanelEntity and leaves Panel1 attached
        showPanel2 = false
        composeTestRule.waitForIdle()

        assertThat(sharedCapturer.nodes.size).isEqualTo(1)
        assertThat(session.scene.keyEntity).isEqualTo(session.scene.mainPanelEntity)

        // Subsequent capture applies to the remaining attached composable (Panel1)
        composeTestRule.runOnUiThread {
            assertThat(sharedCapturer.captureContinuity()).isTrue()
        }
        assertThat(session.scene.keyEntity).isEqualTo(panel1)
    }
}

private fun androidx.activity.ComponentActivity.findSubspaceSemanticsEntityWithTag(
    testTag: String
): Entity? {
    val roots =
        window.decorView.getTag(androidx.xr.compose.R.id.compose_xr_registered_roots) as? List<*>
            ?: return null
    fun findRecursive(node: Any?): Entity? {
        if (node is androidx.xr.compose.subspace.node.SubspaceSemanticsInfo) {
            if (
                node.semanticsConfiguration?.getOrNull(
                    androidx.compose.ui.semantics.SemanticsProperties.TestTag
                ) == testTag
            ) {
                return node.semanticsEntity
            }
            for (child in node.childrenInfo) {
                val found = findRecursive(child)
                if (found != null) return found
            }
        }
        return null
    }
    return roots.firstNotNullOfOrNull { findRecursive(it) }
}
