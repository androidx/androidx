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

package androidx.xr.scenecore.integration.tests

import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialSceneHelper
import androidx.xr.testutils.SpatialTask
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Paths
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device E2E integration tests validating [Entity.setEnabled] and [Entity.isEnabled] across
 * activity space, main panel, hierarchical panel/glTF trees, pose updates while hidden, and Full
 * Space / Home Space mode transitions.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ShowHideEntityTest {

    @Test(timeout = 15_000L)
    @XrDeviceTest
    fun setEnabled_activitySpace_hidesAndUnhidesAllPanelsAndModels() =
        runTestWithSession { activity, session ->
            session.scene.requestFullSpaceAndAwait()
            val panels = createPanelHierarchy(activity, session)
            val gltfs = createGltfHierarchy(session)

            try {
                assertThat(session.scene.activitySpace.isEnabled(includeParents = false)).isTrue()
                assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = true)).isTrue()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )

                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                    expectMainPanelVisible = true,
                    expectMainPanelLocallyEnabled = true,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                // Hide ActivitySpace: all panels (including mainPanelEntity) and glTF models must
                // immediately become disabled in the hierarchy and hidden in the compositor, while
                // keeping their own local requested-vis="show" and parent-child hierarchy intact.
                session.scene.activitySpace.setEnabled(false)

                assertThat(session.scene.activitySpace.isEnabled(includeParents = false)).isFalse()
                assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = true)).isFalse()
                assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = false)).isTrue()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = false,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = false,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )

                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = false,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                    expectMainPanelVisible = false,
                    expectMainPanelLocallyEnabled = true,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = false,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                // Re-enable ActivitySpace: all panels and models reappear.
                session.scene.activitySpace.setEnabled(true)

                assertThat(session.scene.activitySpace.isEnabled(includeParents = false)).isTrue()
                assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = true)).isTrue()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )

                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                    expectMainPanelVisible = true,
                    expectMainPanelLocallyEnabled = true,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )
            } finally {
                session.scene.activitySpace.setEnabled(true)
                panels.cleanup()
                gltfs.cleanup()
            }
        }

    @Test(timeout = 15_000L)
    @XrDeviceTest
    fun setEnabled_mainPanelEntity_hidesAndUnhidesAllPanels() =
        runTestWithSession { activity, session ->
            session.scene.requestFullSpaceAndAwait()
            val panels = createPanelHierarchy(activity, session)

            try {
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                    expectMainPanelVisible = true,
                    expectMainPanelLocallyEnabled = true,
                )

                // Hide MainPanelEntity: main panel and all descendant spatial panels must
                // immediately become disabled and hidden in CPM, while descendant panels preserve
                // their local requested-vis="show" and hierarchy under
                // MainPanelAndTaskWindowLeashNode.
                session.scene.mainPanelEntity.setEnabled(false)

                assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = false))
                    .isFalse()
                assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = true)).isFalse()
                assertThat(panels.parent.isEnabled(includeParents = false)).isTrue()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = false,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )

                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = false,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                    expectMainPanelVisible = false,
                    expectMainPanelLocallyEnabled = false,
                )

                // Re-enable MainPanelEntity: main panel and descendant panels reappear.
                session.scene.mainPanelEntity.setEnabled(true)

                assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = false)).isTrue()
                assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = true)).isTrue()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )

                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                    expectMainPanelVisible = true,
                    expectMainPanelLocallyEnabled = true,
                )
            } finally {
                session.scene.mainPanelEntity.setEnabled(true)
                panels.cleanup()
            }
        }

    @Test(timeout = 15_000L)
    @XrDeviceTest
    fun setEnabled_panelHierarchy_togglesParentAndChildrenIndividually() =
        runTestWithSession { activity, session ->
            session.scene.requestFullSpaceAndAwait()
            val panels = createPanelHierarchy(activity, session)

            try {
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                // 1. Toggle Parent Panel off and on: hides/unhides itself and both children, and
                // disables/re-enables reformOptions on Parent Panel.
                panels.parent.setEnabled(false)
                assertThat(panels.parent.isEnabled(includeParents = false)).isFalse()
                assertThat(panels.child1.isEnabled(includeParents = false)).isTrue()
                assertThat(panels.child2.isEnabled(includeParents = false)).isTrue()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = false,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = false,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = false,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                panels.parent.setEnabled(true)
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                // 2. Toggle Child 1 Panel off and on: hides/unhides itself and Child 2, leaving
                // Parent visible.
                panels.child1.setEnabled(false)
                assertThat(panels.child1.isEnabled(includeParents = false)).isFalse()
                assertThat(panels.child2.isEnabled(includeParents = false)).isTrue()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = false,
                    expectChild2LocallyEnabled = true,
                )

                panels.child1.setEnabled(true)
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                // 3. Toggle Child 2 Panel off and on: hides/unhides only itself, leaving Parent and
                // Child 1 visible.
                panels.child2.setEnabled(false)
                assertThat(panels.child2.isEnabled(includeParents = false)).isFalse()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = false,
                )
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = false,
                )

                panels.child2.setEnabled(true)
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )
            } finally {
                panels.cleanup()
            }
        }

    @Test(timeout = 15_000L)
    @XrDeviceTest
    fun setEnabled_gltfHierarchy_togglesParentAndChildrenIndividually() =
        runTestWithSession { session ->
            session.scene.requestFullSpaceAndAwait()
            val gltfs = createGltfHierarchy(session)

            try {
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                // 1. Toggle Parent glTF off and on: hides/unhides itself and both children.
                gltfs.parent.setEnabled(false)
                assertThat(gltfs.parent.isEnabled(includeParents = false)).isFalse()
                assertThat(gltfs.child1.isEnabled(includeParents = false)).isTrue()
                assertThat(gltfs.child2.isEnabled(includeParents = false)).isTrue()
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = false,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = false,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = false,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                gltfs.parent.setEnabled(true)
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                // 2. Toggle Child 1 glTF off and on: hides/unhides itself and Child 2, leaving
                // Parent enabled.
                gltfs.child1.setEnabled(false)
                assertThat(gltfs.child1.isEnabled(includeParents = false)).isFalse()
                assertThat(gltfs.child2.isEnabled(includeParents = false)).isTrue()
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = false,
                    expectChild2LocallyEnabled = true,
                )

                gltfs.child1.setEnabled(true)
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                // 3. Toggle Child 2 glTF off and on: hides/unhides only itself, leaving Parent and
                // Child 1 enabled.
                gltfs.child2.setEnabled(false)
                assertThat(gltfs.child2.isEnabled(includeParents = false)).isFalse()
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = false,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = false,
                )

                gltfs.child2.setEnabled(true)
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )
            } finally {
                gltfs.cleanup()
            }
        }

    @Test(timeout = 15_000L)
    @XrDeviceTest
    fun setEnabled_parentVisibilityChange_maintainsChildLocalHiddenState() =
        runTestWithSession { activity, session ->
            session.scene.requestFullSpaceAndAwait()
            val panels = createPanelHierarchy(activity, session)

            try {
                // Step 1: Hide the "Child 1" panel.
                panels.child1.setEnabled(false)
                assertThat(panels.child1.isEnabled(includeParents = false)).isFalse()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = false,
                    expectChild2LocallyEnabled = true,
                )

                // Step 2: Hide the "Parent" panel.
                panels.parent.setEnabled(false)
                assertThat(panels.parent.isEnabled(includeParents = false)).isFalse()
                assertThat(panels.child1.isEnabled(includeParents = false)).isFalse()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = false,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = false,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = false,
                    expectChild1LocallyEnabled = false,
                    expectChild2LocallyEnabled = true,
                )

                // Step 3: Unhide the "Parent" panel. Child 1 & Child 2 panels must remain hidden
                // because Child 1's local hidden state (requested-vis=hide and disabled
                // reformOptions) is preserved.
                panels.parent.setEnabled(true)
                assertThat(panels.parent.isEnabled(includeParents = false)).isTrue()
                assertThat(panels.child1.isEnabled(includeParents = false)).isFalse()
                assertThat(panels.child2.isEnabled(includeParents = false)).isTrue()
                assertHierarchyEnabled(
                    panels.parent,
                    panels.child1,
                    panels.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = false,
                    expectChild2LocallyEnabled = true,
                )
            } finally {
                panels.cleanup()
            }
        }

    @Test(timeout = 15_000L)
    @XrDeviceTest
    fun setPose_whileChildGltfHidden_maintainsRelativeSpacingWhenUnhidden() =
        runTestWithSession { session ->
            session.scene.requestFullSpaceAndAwait()
            val gltfs = createGltfHierarchy(session)

            try {
                val initialParentActivityPos = gltfs.parent.getPose(Space.ACTIVITY).translation
                val initialChild1OffsetFromParent =
                    gltfs.child1.getPose(Space.ACTIVITY).translation - initialParentActivityPos
                val initialChild2OffsetFromParent =
                    gltfs.child2.getPose(Space.ACTIVITY).translation - initialParentActivityPos

                // Step 1: Hide "Child 1" GLTF (hiding both Child 1 and Child 2).
                gltfs.child1.setEnabled(false)
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                val initialCpmNodes =
                    awaitAndAssertGltfHierarchyCpmState(
                        expectParentVisible = true,
                        expectChild1Visible = false,
                        expectChild2Visible = false,
                        expectParentLocallyEnabled = true,
                        expectChild1LocallyEnabled = false,
                        expectChild2LocallyEnabled = true,
                        expectParentLocalPosition = PARENT_GLTF_POSE.translation,
                        expectChild1LocalPosition = CHILD_GLTF_1_POSE.translation,
                        expectChild2LocalPosition = CHILD_GLTF_2_POSE.translation,
                    )
                val initialCpmChild1OffsetFromParent =
                    initialCpmNodes.child1.position - initialCpmNodes.parent.position
                val initialCpmChild2OffsetFromParent =
                    initialCpmNodes.child2.position - initialCpmNodes.parent.position

                // Step 2: Move Parent GLTF up (+0.5m Y) and right (+0.25m X) while Child 1 & 2 are
                // hidden.
                val currentPose = gltfs.parent.getPose(Space.PARENT)
                val expectedMovedParentLocalPos = currentPose.translation + GLTF_MOVE_DELTA
                gltfs.parent.setPose(
                    Pose(expectedMovedParentLocalPos, currentPose.rotation),
                    Space.PARENT,
                )

                // Verify Child 1 & 2 GLTFs remain hidden and the moved parent pose is committed in
                // CPM.
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = false,
                    expectChild2Enabled = false,
                )
                awaitAndAssertGltfHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = false,
                    expectChild2Visible = false,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = false,
                    expectChild2LocallyEnabled = true,
                    expectParentLocalPosition = expectedMovedParentLocalPos,
                    expectChild1LocalPosition = CHILD_GLTF_1_POSE.translation,
                    expectChild2LocalPosition = CHILD_GLTF_2_POSE.translation,
                )

                // Step 3: Unhide "Child 1" GLTF and verify Child 1 & 2 moved while hidden to
                // maintain the same relative spacing from the parent in both SceneCore and CPM.
                gltfs.child1.setEnabled(true)
                assertHierarchyEnabled(
                    gltfs.parent,
                    gltfs.child1,
                    gltfs.child2,
                    expectParentEnabled = true,
                    expectChild1Enabled = true,
                    expectChild2Enabled = true,
                )

                val movedParentActivityPos = gltfs.parent.getPose(Space.ACTIVITY).translation
                assertVector3(
                    movedParentActivityPos,
                    initialParentActivityPos + GLTF_MOVE_DELTA,
                    POSE_TOLERANCE,
                )
                assertVector3(
                    gltfs.child1.getPose(Space.PARENT).translation,
                    CHILD_GLTF_1_POSE.translation,
                    POSE_TOLERANCE,
                )
                assertVector3(
                    gltfs.child2.getPose(Space.PARENT).translation,
                    CHILD_GLTF_2_POSE.translation,
                    POSE_TOLERANCE,
                )
                assertVector3(
                    gltfs.child1.getPose(Space.ACTIVITY).translation - movedParentActivityPos,
                    initialChild1OffsetFromParent,
                    POSE_TOLERANCE,
                )
                assertVector3(
                    gltfs.child2.getPose(Space.ACTIVITY).translation - movedParentActivityPos,
                    initialChild2OffsetFromParent,
                    POSE_TOLERANCE,
                )

                val unhiddenCpmNodes =
                    awaitAndAssertGltfHierarchyCpmState(
                        expectParentVisible = true,
                        expectChild1Visible = true,
                        expectChild2Visible = true,
                        expectParentLocallyEnabled = true,
                        expectChild1LocallyEnabled = true,
                        expectChild2LocallyEnabled = true,
                        expectParentLocalPosition = expectedMovedParentLocalPos,
                        expectChild1LocalPosition = CHILD_GLTF_1_POSE.translation,
                        expectChild2LocalPosition = CHILD_GLTF_2_POSE.translation,
                    )
                assertVector3(
                    unhiddenCpmNodes.child1.position - unhiddenCpmNodes.parent.position,
                    initialCpmChild1OffsetFromParent,
                    POSE_TOLERANCE,
                )
                assertVector3(
                    unhiddenCpmNodes.child2.position - unhiddenCpmNodes.parent.position,
                    initialCpmChild2OffsetFromParent,
                    POSE_TOLERANCE,
                )
            } finally {
                gltfs.cleanup()
            }
        }

    @Test(timeout = 15_000L)
    @XrDeviceTest
    fun homeSpaceMode_hidesSpatialEntitiesAndIgnoresMainPanelAndActivitySpaceHide() =
        runTestWithSession { activity, session ->
            session.scene.requestFullSpaceAndAwait()
            val panels = createPanelHierarchy(activity, session)

            try {
                awaitAndAssertPanelHierarchyCpmState(
                    expectParentVisible = true,
                    expectChild1Visible = true,
                    expectChild2Visible = true,
                    expectParentLocallyEnabled = true,
                    expectChild1LocallyEnabled = true,
                    expectChild2LocallyEnabled = true,
                )

                // Transition to Home Space Mode (HSM).
                session.scene.requestHomeSpaceAndAwait()

                // In HSM, spatial parent and child panels are no longer visible in the scene graph.
                awaitSpatialPanelsHiddenInHomeSpace()

                // Hiding the main panel and activity space in HSM updates their local SceneCore
                // enabled state, but the main task window remains visible in HSM.
                session.scene.mainPanelEntity.setEnabled(false)
                session.scene.activitySpace.setEnabled(false)
                assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = false))
                    .isFalse()
                assertThat(session.scene.activitySpace.isEnabled(includeParents = false)).isFalse()
                awaitMainPanelRemainsVisibleInHomeSpaceAfterHide()
            } finally {
                session.scene.activitySpace.setEnabled(true)
                session.scene.mainPanelEntity.setEnabled(true)
                panels.cleanup()
            }
        }

    /**
     * Creates the 3-level [PanelEntity] hierarchy (`Parent Panel` -> `Child Panel 1` -> `Child
     * Panel 2`) parented under [Session.scene.mainPanelEntity], matching `VisibilityActivity`.
     */
    private fun createPanelHierarchy(
        activity: ComponentActivity,
        session: Session,
    ): PanelHierarchy {
        val parentPanel =
            PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = PARENT_PANEL_NAME },
                    pixelDimensions = PANEL_PIXEL_DIMENSIONS,
                    name = PARENT_PANEL_NAME,
                    pose = PARENT_PANEL_POSE,
                    parent = session.scene.mainPanelEntity,
                )
                .apply {
                    assertThat(addComponent(MovableComponent.createSystemMovable(session))).isTrue()
                }

        val childPanel1 =
            PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = CHILD_PANEL_1_NAME },
                    pixelDimensions = PANEL_PIXEL_DIMENSIONS,
                    name = CHILD_PANEL_1_NAME,
                    pose = CHILD_PANEL_OFFSET_POSE,
                    parent = parentPanel,
                )
                .apply {
                    assertThat(addComponent(MovableComponent.createSystemMovable(session))).isTrue()
                }

        val childPanel2 =
            PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = CHILD_PANEL_2_NAME },
                    pixelDimensions = PANEL_PIXEL_DIMENSIONS,
                    name = CHILD_PANEL_2_NAME,
                    pose = CHILD_PANEL_OFFSET_POSE,
                    parent = childPanel1,
                )
                .apply {
                    assertThat(addComponent(MovableComponent.createSystemMovable(session))).isTrue()
                }

        return PanelHierarchy(parent = parentPanel, child1 = childPanel1, child2 = childPanel2)
    }

    /**
     * Loads the test [GltfModel] and creates the 3-level [GltfModelEntity] hierarchy (`parent` ->
     * `child1` -> `child2`) parented under [Session.scene.activitySpace], matching
     * `VisibilityActivity`.
     */
    private suspend fun createGltfHierarchy(session: Session): GltfHierarchy {
        val gltfModel = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))

        val parentGltf =
            GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = PARENT_GLTF_POSE,
                    parent = session.scene.activitySpace,
                )
                .apply { setScale(GLTF_LOCAL_SCALE) }

        val childGltf1 =
            GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = CHILD_GLTF_1_POSE,
                    parent = parentGltf,
                )
                .apply { setScale(GLTF_LOCAL_SCALE) }

        val childGltf2 =
            GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = CHILD_GLTF_2_POSE,
                    parent = childGltf1,
                )
                .apply { setScale(GLTF_LOCAL_SCALE) }

        return GltfHierarchy(
            model = gltfModel,
            parent = parentGltf,
            child1 = childGltf1,
            child2 = childGltf2,
        )
    }

    /** Asserts the effective enabled state (`includeParents = true`) of a 3-level hierarchy. */
    private fun assertHierarchyEnabled(
        parent: Entity,
        child1: Entity,
        child2: Entity,
        expectParentEnabled: Boolean,
        expectChild1Enabled: Boolean,
        expectChild2Enabled: Boolean,
    ) {
        assertThat(parent.isEnabled(includeParents = true)).isEqualTo(expectParentEnabled)
        assertThat(child1.isEnabled(includeParents = true)).isEqualTo(expectChild1Enabled)
        assertThat(child2.isEnabled(includeParents = true)).isEqualTo(expectChild2Enabled)
    }

    /**
     * Captures the spatial scene off the main thread (since the capture blocks while polling) and
     * returns the task of the instrumented package, if present.
     */
    private suspend fun captureTaskOrNull(): SpatialTask? {
        yield()
        return withContext(Dispatchers.IO) {
            SpatialSceneHelper.captureSpatialScene().findTaskByPackageName(packageName)
        }
    }

    /**
     * Polls the CPM scene graph until the 3-level panel hierarchy (`Parent Panel` -> `Child Panel
     * 1` -> `Child Panel 2`, and optionally `MainPanelAndTaskWindowLeashNode`) reaches the expected
     * visibility states within a single compositor snapshot, then asserts:
     * 1. **Hierarchy preservation**: nodes are not detached or moved to another hierarchy when
     *    hidden (`MainPanelAndTaskWindowLeashNode` -> `Parent Panel` -> `Child Panel 1` -> `Child
     *    Panel 2`).
     * 2. **Effective & local visibility flags**: `isVisible`, `stereoVisibility` (`visible on both`
     *    vs `hidden on both`), `ImpNodeIsActive`, `ImpNodeIsEnabled`, and local `requested-vis`
     *    (`show` vs `hide`).
     * 3. **MovableComponent (`reformOptions`)**: when a panel is locally enabled, its CPM node
     *    carries `reformOptions` with `enable=1` (`isMovable == true`); when locally disabled via
     *    `setEnabled(false)`, `AndroidXrEntity.setHidden(true)` calls `disableReform`, removing
     *    `reformOptions` from that node until re-enabled.
     */
    private suspend fun awaitAndAssertPanelHierarchyCpmState(
        expectParentVisible: Boolean,
        expectChild1Visible: Boolean,
        expectChild2Visible: Boolean,
        expectParentLocallyEnabled: Boolean,
        expectChild1LocallyEnabled: Boolean,
        expectChild2LocallyEnabled: Boolean,
        expectMainPanelVisible: Boolean? = null,
        expectMainPanelLocallyEnabled: Boolean? = null,
    ) {
        val start = TimeSource.Monotonic.markNow()
        var mainPanelNode: SpatialNode? = null
        var parentNode: SpatialNode? = null
        var child1Node: SpatialNode? = null
        var child2Node: SpatialNode? = null

        while (start.elapsedNow() < SCENE_POLL_TIMEOUT) {
            val task = captureTaskOrNull()
            val candidateMain = task?.findNodeByName(MAIN_PANEL_NODE_NAME) ?: task?.taskWindowLeash
            val candidateParent = task?.findNode {
                it.name == PARENT_PANEL_NAME && it.isEmbeddedWindowLeash
            }
            val candidateChild1 = task?.findNode {
                it.name == CHILD_PANEL_1_NAME && it.isEmbeddedWindowLeash
            }
            val candidateChild2 = task?.findNode {
                it.name == CHILD_PANEL_2_NAME && it.isEmbeddedWindowLeash
            }

            mainPanelNode = candidateMain
            parentNode = candidateParent
            child1Node = candidateChild1
            child2Node = candidateChild2

            val mainMatched =
                expectMainPanelVisible == null ||
                    (candidateMain != null &&
                        (if (expectMainPanelLocallyEnabled != null) {
                            candidateMain.hasExpectedCpmState(
                                expectVisible = expectMainPanelVisible,
                                expectLocallyEnabled = expectMainPanelLocallyEnabled,
                                expectMovableReformWhenLocallyEnabled = false,
                            )
                        } else {
                            candidateMain.isVisible == expectMainPanelVisible
                        }))
            if (
                mainMatched &&
                    candidateParent != null &&
                    candidateChild1 != null &&
                    candidateChild2 != null &&
                    candidateParent.hasExpectedCpmState(
                        expectVisible = expectParentVisible,
                        expectLocallyEnabled = expectParentLocallyEnabled,
                        expectMovableReformWhenLocallyEnabled = true,
                    ) &&
                    candidateChild1.hasExpectedCpmState(
                        expectVisible = expectChild1Visible,
                        expectLocallyEnabled = expectChild1LocallyEnabled,
                        expectMovableReformWhenLocallyEnabled = true,
                    ) &&
                    candidateChild2.hasExpectedCpmState(
                        expectVisible = expectChild2Visible,
                        expectLocallyEnabled = expectChild2LocallyEnabled,
                        expectMovableReformWhenLocallyEnabled = true,
                    )
            ) {
                break
            }
            delay(SCENE_POLL_INTERVAL)
        }

        val resolvedMainPanel =
            checkNotNull(mainPanelNode) { "Expected '$MAIN_PANEL_NODE_NAME' in $packageName" }
        val resolvedParent =
            checkNotNull(parentNode) { "Expected '$PARENT_PANEL_NAME' in $packageName" }
        val resolvedChild1 =
            checkNotNull(child1Node) { "Expected '$CHILD_PANEL_1_NAME' in $packageName" }
        val resolvedChild2 =
            checkNotNull(child2Node) { "Expected '$CHILD_PANEL_2_NAME' in $packageName" }

        if (expectMainPanelVisible != null && expectMainPanelLocallyEnabled != null) {
            assertCpmNodeState(
                node = resolvedMainPanel,
                nodeLabel = MAIN_PANEL_NODE_NAME,
                expectVisible = expectMainPanelVisible,
                expectLocallyEnabled = expectMainPanelLocallyEnabled,
                expectMovableReformWhenLocallyEnabled = false,
            )
        }

        // 1. Verify parent-child hierarchy in CPM is preserved (not reparented when hidden).
        assertWithMessage("Parent Panel must remain a direct child of MainPanel")
            .that(resolvedParent.parent)
            .isEqualTo(resolvedMainPanel)
        assertWithMessage("Child Panel 1 must remain a direct child of Parent Panel")
            .that(resolvedChild1.parent)
            .isEqualTo(resolvedParent)
        assertWithMessage("Child Panel 2 must remain a direct child of Child Panel 1")
            .that(resolvedChild2.parent)
            .isEqualTo(resolvedChild1)

        // 2. Verify CPM visibility, Impress active/enabled flags, and MovableComponent reform
        // state.
        assertCpmNodeState(
            node = resolvedParent,
            nodeLabel = PARENT_PANEL_NAME,
            expectVisible = expectParentVisible,
            expectLocallyEnabled = expectParentLocallyEnabled,
            expectMovableReformWhenLocallyEnabled = true,
        )
        assertCpmNodeState(
            node = resolvedChild1,
            nodeLabel = CHILD_PANEL_1_NAME,
            expectVisible = expectChild1Visible,
            expectLocallyEnabled = expectChild1LocallyEnabled,
            expectMovableReformWhenLocallyEnabled = true,
        )
        assertCpmNodeState(
            node = resolvedChild2,
            nodeLabel = CHILD_PANEL_2_NAME,
            expectVisible = expectChild2Visible,
            expectLocallyEnabled = expectChild2LocallyEnabled,
            expectMovableReformWhenLocallyEnabled = true,
        )
    }

    /**
     * Polls the CPM scene graph until the 3-level glTF hierarchy (`parentGltf` -> `childGltf1` ->
     * `childGltf2`) reaches the expected visibility states (and optional local positions), then
     * asserts that the `jni node` hierarchy and `gltf_entity_subspace_*` (`hasSubspace=true`) nodes
     * remain in place and reflect the expected `requested-vis`, `stereoVisibility`,
     * `ImpNodeIsActive`, `ImpNodeIsEnabled`, and `ImpNodeLocalPosition` values.
     */
    private suspend fun awaitAndAssertGltfHierarchyCpmState(
        expectParentVisible: Boolean,
        expectChild1Visible: Boolean,
        expectChild2Visible: Boolean,
        expectParentLocallyEnabled: Boolean,
        expectChild1LocallyEnabled: Boolean,
        expectChild2LocallyEnabled: Boolean,
        expectParentLocalPosition: Vector3? = null,
        expectChild1LocalPosition: Vector3? = null,
        expectChild2LocalPosition: Vector3? = null,
    ): GltfCpmNodes {
        val start = TimeSource.Monotonic.markNow()
        var parentGltfNode: SpatialNode? = null
        var child1GltfNode: SpatialNode? = null
        var child2GltfNode: SpatialNode? = null

        while (start.elapsedNow() < SCENE_POLL_TIMEOUT) {
            val task = captureTaskOrNull()
            val activitySpaceRoot = task?.findNodeByName(ACTIVITY_SPACE_ROOT_NODE_NAME)
            val candidateParent =
                activitySpaceRoot?.children?.firstOrNull { node ->
                    node.children.any { it.hasSubspace }
                }
            val candidateChild1 =
                candidateParent?.children?.firstOrNull { node ->
                    node.children.any { it.hasSubspace }
                }
            val candidateChild2 =
                candidateChild1?.children?.firstOrNull { node ->
                    node.children.any { it.hasSubspace }
                }

            parentGltfNode = candidateParent
            child1GltfNode = candidateChild1
            child2GltfNode = candidateChild2

            val parentPosMatched =
                expectParentLocalPosition == null ||
                    candidateParent?.localPosition?.isWithinTolerance(expectParentLocalPosition) ==
                        true
            val child1PosMatched =
                expectChild1LocalPosition == null ||
                    candidateChild1?.localPosition?.isWithinTolerance(expectChild1LocalPosition) ==
                        true
            val child2PosMatched =
                expectChild2LocalPosition == null ||
                    candidateChild2?.localPosition?.isWithinTolerance(expectChild2LocalPosition) ==
                        true

            if (
                candidateParent != null &&
                    candidateChild1 != null &&
                    candidateChild2 != null &&
                    candidateParent.hasExpectedCpmState(
                        expectVisible = expectParentVisible,
                        expectLocallyEnabled = expectParentLocallyEnabled,
                        expectMovableReformWhenLocallyEnabled = false,
                    ) &&
                    candidateChild1.hasExpectedCpmState(
                        expectVisible = expectChild1Visible,
                        expectLocallyEnabled = expectChild1LocallyEnabled,
                        expectMovableReformWhenLocallyEnabled = false,
                    ) &&
                    candidateChild2.hasExpectedCpmState(
                        expectVisible = expectChild2Visible,
                        expectLocallyEnabled = expectChild2LocallyEnabled,
                        expectMovableReformWhenLocallyEnabled = false,
                    ) &&
                    candidateParent.children.firstOrNull { it.hasSubspace }?.isVisible ==
                        expectParentVisible &&
                    candidateChild1.children.firstOrNull { it.hasSubspace }?.isVisible ==
                        expectChild1Visible &&
                    candidateChild2.children.firstOrNull { it.hasSubspace }?.isVisible ==
                        expectChild2Visible &&
                    parentPosMatched &&
                    child1PosMatched &&
                    child2PosMatched
            ) {
                break
            }
            delay(SCENE_POLL_INTERVAL)
        }

        val resolvedParent =
            checkNotNull(parentGltfNode) { "Expected parent glTF node in CPM scene graph" }
        val resolvedChild1 =
            checkNotNull(child1GltfNode) { "Expected child 1 glTF node in CPM scene graph" }
        val resolvedChild2 =
            checkNotNull(child2GltfNode) { "Expected child 2 glTF node in CPM scene graph" }

        // Verify hierarchy preservation: parentGltf -> childGltf1 -> childGltf2.
        assertThat(resolvedChild1.parent).isEqualTo(resolvedParent)
        assertThat(resolvedChild2.parent).isEqualTo(resolvedChild1)

        assertCpmNodeState(
            node = resolvedParent,
            nodeLabel = "parentGltf",
            expectVisible = expectParentVisible,
            expectLocallyEnabled = expectParentLocallyEnabled,
            expectMovableReformWhenLocallyEnabled = false,
        )
        assertCpmNodeState(
            node = resolvedChild1,
            nodeLabel = "childGltf1",
            expectVisible = expectChild1Visible,
            expectLocallyEnabled = expectChild1LocallyEnabled,
            expectMovableReformWhenLocallyEnabled = false,
        )
        assertCpmNodeState(
            node = resolvedChild2,
            nodeLabel = "childGltf2",
            expectVisible = expectChild2Visible,
            expectLocallyEnabled = expectChild2LocallyEnabled,
            expectMovableReformWhenLocallyEnabled = false,
        )

        if (expectParentLocalPosition != null) {
            assertVector3(resolvedParent.localPosition, expectParentLocalPosition, POSE_TOLERANCE)
        }
        if (expectChild1LocalPosition != null) {
            assertVector3(resolvedChild1.localPosition, expectChild1LocalPosition, POSE_TOLERANCE)
        }
        if (expectChild2LocalPosition != null) {
            assertVector3(resolvedChild2.localPosition, expectChild2LocalPosition, POSE_TOLERANCE)
        }

        // Also verify each glTF entity's direct subspace child (`hasSubspace=true`) matches
        // effective visibility.
        val parentSubspace = checkNotNull(resolvedParent.children.firstOrNull { it.hasSubspace })
        val child1Subspace = checkNotNull(resolvedChild1.children.firstOrNull { it.hasSubspace })
        val child2Subspace = checkNotNull(resolvedChild2.children.firstOrNull { it.hasSubspace })
        assertThat(parentSubspace.isVisible).isEqualTo(expectParentVisible)
        assertThat(child1Subspace.isVisible).isEqualTo(expectChild1Visible)
        assertThat(child2Subspace.isVisible).isEqualTo(expectChild2Visible)

        return GltfCpmNodes(
            parent = resolvedParent,
            child1 = resolvedChild1,
            child2 = resolvedChild2,
        )
    }

    /**
     * Returns whether all CPM properties of [this] node asserted by [assertCpmNodeState] have
     * settled to their expected values.
     */
    private fun SpatialNode.hasExpectedCpmState(
        expectVisible: Boolean,
        expectLocallyEnabled: Boolean,
        expectMovableReformWhenLocallyEnabled: Boolean,
    ): Boolean {
        val expectedStereo = if (expectVisible) "visible on both" else "hidden on both"
        val expectedVisStr = expectVisible.toString()
        val expectedRequestedVis = if (expectLocallyEnabled) "show" else "hide"
        val reformMatched =
            if (!expectMovableReformWhenLocallyEnabled) {
                true
            } else if (expectLocallyEnabled) {
                properties["reformOptions"]?.contains("enable=1") == true && isMovable
            } else {
                properties["reformOptions"] == null
            }
        return isVisible == expectVisible &&
            stereoVisibility == expectedStereo &&
            properties["ImpNodeIsActive"] == expectedVisStr &&
            properties["ImpNodeIsEnabled"] == expectedVisStr &&
            properties["requested-vis"] == expectedRequestedVis &&
            reformMatched
    }

    /**
     * Asserts the CPM properties of [node] after `setEnabled` changes:
     * - `isVisible` and `stereoVisibility` (`visible on both` vs `hidden on both`) reflect
     *   effective visibility in the hierarchy.
     * - `ImpNodeIsActive` and `ImpNodeIsEnabled` reflect effective visibility in Impress.
     * - `requested-vis` (`show` vs `hide`) reflects the node's own local enabled state
     *   ([expectLocallyEnabled]).
     * - When [expectMovableReformWhenLocallyEnabled] is true, verifies that `reformOptions` on
     *   [node] is present (`enable=1`, `isMovable == true`) when locally enabled and removed by
     *   `disableReform` (`null`) when locally disabled.
     */
    private fun assertCpmNodeState(
        node: SpatialNode,
        nodeLabel: String,
        expectVisible: Boolean,
        expectLocallyEnabled: Boolean,
        expectMovableReformWhenLocallyEnabled: Boolean,
    ) {
        assertWithMessage("$nodeLabel isVisible").that(node.isVisible).isEqualTo(expectVisible)
        assertWithMessage("$nodeLabel stereoVisibility")
            .that(node.stereoVisibility)
            .isEqualTo(if (expectVisible) "visible on both" else "hidden on both")
        assertWithMessage("$nodeLabel ImpNodeIsActive")
            .that(node.properties["ImpNodeIsActive"])
            .isEqualTo(expectVisible.toString())
        assertWithMessage("$nodeLabel ImpNodeIsEnabled")
            .that(node.properties["ImpNodeIsEnabled"])
            .isEqualTo(expectVisible.toString())
        assertWithMessage("$nodeLabel requested-vis")
            .that(node.properties["requested-vis"])
            .isEqualTo(if (expectLocallyEnabled) "show" else "hide")

        if (expectMovableReformWhenLocallyEnabled) {
            if (expectLocallyEnabled) {
                assertWithMessage("$nodeLabel reformOptions when locally enabled")
                    .that(node.properties["reformOptions"])
                    .contains("enable=1")
                assertWithMessage("$nodeLabel isMovable when locally enabled")
                    .that(node.isMovable)
                    .isTrue()
            } else {
                assertWithMessage(
                        "$nodeLabel reformOptions should be removed by disableReform when locally disabled"
                    )
                    .that(node.properties["reformOptions"])
                    .isNull()
            }
        }
    }

    /**
     * Polls a single CPM scene graph snapshot until all three spatial panels (`Parent Panel`,
     * `Child Panel 1`, `Child Panel 2`) are not visible in Home Space Mode.
     */
    private suspend fun awaitSpatialPanelsHiddenInHomeSpace() {
        val start = TimeSource.Monotonic.markNow()
        var lastTask: SpatialTask? = null
        var parentVisible = true
        var child1Visible = true
        var child2Visible = true
        while (start.elapsedNow() < SCENE_POLL_TIMEOUT) {
            val task = captureTaskOrNull()
            if (task != null) {
                lastTask = task
                parentVisible =
                    (task.findNode { it.name == PARENT_PANEL_NAME && it.isEmbeddedWindowLeash }
                            ?: task.findNodeByName(PARENT_PANEL_NAME))
                        ?.isVisible == true
                child1Visible =
                    (task.findNode { it.name == CHILD_PANEL_1_NAME && it.isEmbeddedWindowLeash }
                            ?: task.findNodeByName(CHILD_PANEL_1_NAME))
                        ?.isVisible == true
                child2Visible =
                    (task.findNode { it.name == CHILD_PANEL_2_NAME && it.isEmbeddedWindowLeash }
                            ?: task.findNodeByName(CHILD_PANEL_2_NAME))
                        ?.isVisible == true
                if (!parentVisible && !child1Visible && !child2Visible) return
            }
            delay(SCENE_POLL_INTERVAL)
        }
        assertWithMessage("Expected SpatialTask for $packageName in CPM").that(lastTask).isNotNull()
        assertWithMessage("Expected Parent Panel to be hidden in HSM").that(parentVisible).isFalse()
        assertWithMessage("Expected Child Panel 1 to be hidden in HSM")
            .that(child1Visible)
            .isFalse()
        assertWithMessage("Expected Child Panel 2 to be hidden in HSM")
            .that(child2Visible)
            .isFalse()
    }

    /**
     * Syncs the instrumentation thread after `mainPanelEntity.setEnabled(false)` and
     * `activitySpace.setEnabled(false)` in Home Space Mode, then polls across multiple CPM
     * snapshots to verify that the main task window remains visible (`isVisible == true`) and does
     * not transition to hidden.
     */
    private suspend fun awaitMainPanelRemainsVisibleInHomeSpaceAfterHide() {
        withContext(Dispatchers.IO) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }

        val start = TimeSource.Monotonic.markNow()
        var settleWindowStart: TimeSource.Monotonic.ValueTimeMark? = null
        var visiblePollCount = 0
        var lastTask: SpatialTask? = null
        var lastVisible: Boolean? = null

        while (start.elapsedNow() < SCENE_POLL_TIMEOUT) {
            val task = captureTaskOrNull()
            if (task != null) {
                lastTask = task
                val mainPanelInTask = task.findNodeByName(MAIN_PANEL_NODE_NAME)
                val activitySpaceInTask = task.findNodeByName(ACTIVITY_SPACE_ROOT_NODE_NAME)
                val mainPanelNode = mainPanelInTask ?: task.taskWindowLeash
                val isVisible = mainPanelNode?.isVisible == true
                lastVisible = isVisible

                // If the task window ever becomes hidden in HSM, fail immediately instead of
                // continuing to poll.
                if (!isVisible) {
                    break
                }

                // If the spatial root or main panel node is still attached in the task tree, wait
                // until its requested-vis=hide transaction has been observed before starting the
                // settle window.
                val requestedVisSettled =
                    (mainPanelInTask == null ||
                        mainPanelInTask.properties["requested-vis"] == "hide") &&
                        (activitySpaceInTask == null ||
                            activitySpaceInTask.properties["requested-vis"] == "hide")
                if (requestedVisSettled) {
                    if (settleWindowStart == null) {
                        settleWindowStart = TimeSource.Monotonic.markNow()
                    }
                    visiblePollCount++
                    if (
                        visiblePollCount >= MIN_HSM_VISIBILITY_POLLS &&
                            settleWindowStart.elapsedNow() >= HSM_VISIBILITY_SETTLE_DURATION
                    ) {
                        break
                    }
                }
            }
            delay(SCENE_POLL_INTERVAL)
        }
        assertWithMessage("Expected SpatialTask for $packageName in CPM").that(lastTask).isNotNull()
        assertWithMessage("Expected main task window in HSM to remain visible after hide")
            .that(lastVisible)
            .isTrue()
        assertWithMessage(
                "Expected main task window in HSM to remain visible across at least $MIN_HSM_VISIBILITY_POLLS CPM polls"
            )
            .that(visiblePollCount)
            .isAtLeast(MIN_HSM_VISIBILITY_POLLS)
    }

    private val SpatialNode.localPosition: Vector3
        get() =
            attributes["ImpNodeLocalPosition"]?.let {
                if (it.size >= 3) Vector3(it[0], it[1], it[2]) else Vector3.Zero
            } ?: Vector3.Zero

    private fun Vector3.isWithinTolerance(
        expected: Vector3,
        tolerance: Float = POSE_TOLERANCE,
    ): Boolean =
        abs(x - expected.x) <= tolerance &&
            abs(y - expected.y) <= tolerance &&
            abs(z - expected.z) <= tolerance

    private val packageName: String
        get() = InstrumentationRegistry.getInstrumentation().targetContext.packageName

    private data class GltfCpmNodes(
        val parent: SpatialNode,
        val child1: SpatialNode,
        val child2: SpatialNode,
    )

    private data class PanelHierarchy(
        val parent: PanelEntity,
        val child1: PanelEntity,
        val child2: PanelEntity,
    ) {
        fun cleanup() {
            for (panel in listOf(child2, child1, parent)) {
                panel.removeAllComponents()
                panel.parent = null
            }
        }
    }

    private data class GltfHierarchy(
        val model: GltfModel,
        val parent: GltfModelEntity,
        val child1: GltfModelEntity,
        val child2: GltfModelEntity,
    ) {
        fun cleanup() {
            for (entity in listOf(child2, child1, parent)) {
                entity.removeAllComponents()
                entity.parent = null
            }
            model.close()
        }
    }

    private companion object {
        const val ACTIVITY_SPACE_ROOT_NODE_NAME = "SpatialSceneAndActivitySpaceRootNode"
        const val MAIN_PANEL_NODE_NAME = "MainPanelAndTaskWindowLeashNode"
        const val PARENT_PANEL_NAME = "Parent Panel"
        const val CHILD_PANEL_1_NAME = "Child Panel 1"
        const val CHILD_PANEL_2_NAME = "Child Panel 2"

        val PANEL_PIXEL_DIMENSIONS = IntSize2d(640, 480)
        val PARENT_PANEL_POSE = Pose(Vector3(-0.5f, -0.65f, 0.1f))
        val CHILD_PANEL_OFFSET_POSE = Pose(Vector3(0.5f, 0f, 0f))

        const val GLTF_LOCAL_SCALE = 0.5f
        val PARENT_GLTF_POSE = Pose(Vector3(2.0f, 0f, 0f))
        val CHILD_GLTF_1_POSE = Pose(Vector3(0.7f, -0.3f, 0f))
        val CHILD_GLTF_2_POSE = Pose(Vector3(0.7f, -0.6f, 0f))
        val GLTF_MOVE_DELTA = Vector3(0.25f, 0.5f, 0f)

        const val POSE_TOLERANCE = 1e-3f
        val SCENE_POLL_TIMEOUT = 2.seconds
        val SCENE_POLL_INTERVAL = 50.milliseconds
        val HSM_VISIBILITY_SETTLE_DURATION = 300.milliseconds
        const val MIN_HSM_VISIBILITY_POLLS = 3
    }
}
