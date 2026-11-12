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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrSessionScope
import androidx.xr.testutils.createXrSessionRule
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import org.junit.Rule
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

    @get:Rule val sessionRule = createXrSessionRule()

    @Test(timeout = 30_000L)
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun setEnabled_activitySpace_hidesAndUnhidesAllPanelsAndModels() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            val panels = createPanelHierarchy()
            val gltfs = createGltfHierarchy()

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
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )
            assertLocallyEnabledAndAttached(
                gltfs,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )

            // Hide ActivitySpace: all panels (including mainPanelEntity) and glTF models must
            // immediately become disabled in the hierarchy, while keeping their own local enabled
            // state and parent-child hierarchy intact.
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
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )
            assertLocallyEnabledAndAttached(
                gltfs,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )
            // TODO(b/583873060): Verify panels and glTF models stop rendering while
            //  ActivitySpace is disabled.

            // Re-enable ActivitySpace: all panels and models are enabled again.
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
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )
            assertLocallyEnabledAndAttached(
                gltfs,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )
        }
    }

    @Test(timeout = 30_000L)
    @XrDeviceTest
    fun setEnabled_mainPanelEntity_hidesAndUnhidesAllPanels() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            val panels = createPanelHierarchy()

            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )

            // Hide MainPanelEntity: main panel and all descendant spatial panels must immediately
            // become disabled, while descendant panels preserve their local enabled state and
            // hierarchy under mainPanelEntity.
            session.scene.mainPanelEntity.setEnabled(false)

            assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = false)).isFalse()
            assertThat(session.scene.mainPanelEntity.isEnabled(includeParents = true)).isFalse()
            assertHierarchyEnabled(
                panels.parent,
                panels.child1,
                panels.child2,
                expectParentEnabled = false,
                expectChild1Enabled = false,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )

            // Re-enable MainPanelEntity: main panel and descendant panels are enabled again.
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
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )
        }
    }

    @Test(timeout = 30_000L)
    @XrDeviceTest
    fun setEnabled_panelHierarchy_togglesParentAndChildrenIndividually() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            val panels = createPanelHierarchy()

            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )

            // 1. Toggle Parent Panel off and on: disables/re-enables itself and both children.
            panels.parent.setEnabled(false)
            assertHierarchyEnabled(
                panels.parent,
                panels.child1,
                panels.child2,
                expectParentEnabled = false,
                expectChild1Enabled = false,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                panels,
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
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )

            // 2. Toggle Child 1 Panel off and on: disables/re-enables itself and Child 2, leaving
            // Parent enabled.
            panels.child1.setEnabled(false)
            assertHierarchyEnabled(
                panels.parent,
                panels.child1,
                panels.child2,
                expectParentEnabled = true,
                expectChild1Enabled = false,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                panels,
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
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )

            // 3. Toggle Child 2 Panel off and on: disables/re-enables only itself, leaving Parent
            // and Child 1 enabled.
            panels.child2.setEnabled(false)
            assertHierarchyEnabled(
                panels.parent,
                panels.child1,
                panels.child2,
                expectParentEnabled = true,
                expectChild1Enabled = true,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                panels,
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
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )
        }
    }

    @Test(timeout = 30_000L)
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun setEnabled_gltfHierarchy_togglesParentAndChildrenIndividually() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            val gltfs = createGltfHierarchy()

            assertLocallyEnabledAndAttached(
                gltfs,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )

            // 1. Toggle Parent glTF off and on: disables/re-enables itself and both children.
            gltfs.parent.setEnabled(false)
            assertHierarchyEnabled(
                gltfs.parent,
                gltfs.child1,
                gltfs.child2,
                expectParentEnabled = false,
                expectChild1Enabled = false,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                gltfs,
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
            assertLocallyEnabledAndAttached(
                gltfs,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )

            // 2. Toggle Child 1 glTF off and on: disables/re-enables itself and Child 2, leaving
            // Parent enabled.
            gltfs.child1.setEnabled(false)
            assertHierarchyEnabled(
                gltfs.parent,
                gltfs.child1,
                gltfs.child2,
                expectParentEnabled = true,
                expectChild1Enabled = false,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                gltfs,
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
            assertLocallyEnabledAndAttached(
                gltfs,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )

            // 3. Toggle Child 2 glTF off and on: disables/re-enables only itself, leaving Parent
            // and Child 1 enabled.
            gltfs.child2.setEnabled(false)
            assertHierarchyEnabled(
                gltfs.parent,
                gltfs.child1,
                gltfs.child2,
                expectParentEnabled = true,
                expectChild1Enabled = true,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                gltfs,
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
            assertLocallyEnabledAndAttached(
                gltfs,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = true,
                expectChild2LocallyEnabled = true,
            )
        }
    }

    @Test(timeout = 30_000L)
    @XrDeviceTest
    fun setEnabled_parentVisibilityChange_maintainsChildLocalHiddenState() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            val panels = createPanelHierarchy()

            // Step 1: Hide the "Child 1" panel.
            panels.child1.setEnabled(false)
            assertHierarchyEnabled(
                panels.parent,
                panels.child1,
                panels.child2,
                expectParentEnabled = true,
                expectChild1Enabled = false,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = false,
                expectChild2LocallyEnabled = true,
            )

            // Step 2: Hide the "Parent" panel.
            panels.parent.setEnabled(false)
            assertHierarchyEnabled(
                panels.parent,
                panels.child1,
                panels.child2,
                expectParentEnabled = false,
                expectChild1Enabled = false,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = false,
                expectChild1LocallyEnabled = false,
                expectChild2LocallyEnabled = true,
            )

            // Step 3: Unhide the "Parent" panel. Child 1 & Child 2 panels must remain disabled
            // because Child 1's local disabled state is preserved.
            panels.parent.setEnabled(true)
            assertHierarchyEnabled(
                panels.parent,
                panels.child1,
                panels.child2,
                expectParentEnabled = true,
                expectChild1Enabled = false,
                expectChild2Enabled = false,
            )
            assertLocallyEnabledAndAttached(
                panels,
                expectParentLocallyEnabled = true,
                expectChild1LocallyEnabled = false,
                expectChild2LocallyEnabled = true,
            )
        }
    }

    @Test(timeout = 30_000L)
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun setPose_whileChildGltfHidden_maintainsRelativeSpacingWhenUnhidden() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            val gltfs = createGltfHierarchy()

            assertPose(gltfs.parent.getPose(Space.PARENT), PARENT_GLTF_POSE, POSE_TOLERANCE)
            assertPose(gltfs.child1.getPose(Space.PARENT), CHILD_GLTF_1_POSE, POSE_TOLERANCE)
            assertPose(gltfs.child2.getPose(Space.PARENT), CHILD_GLTF_2_POSE, POSE_TOLERANCE)

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

            // Step 2: Move Parent GLTF up (+0.5m Y) and right (+0.25m X) while Child 1 & 2 are
            // hidden.
            val currentPose = gltfs.parent.getPose(Space.PARENT)
            val movedParentPose =
                Pose(currentPose.translation + GLTF_MOVE_DELTA, currentPose.rotation)
            gltfs.parent.setPose(movedParentPose, Space.PARENT)

            // Verify Child 1 & 2 GLTFs remain hidden, the parent moved, and both children moved
            // with it while hidden, keeping their local poses and relative spacing from the parent.
            assertHierarchyEnabled(
                gltfs.parent,
                gltfs.child1,
                gltfs.child2,
                expectParentEnabled = true,
                expectChild1Enabled = false,
                expectChild2Enabled = false,
            )
            assertPose(gltfs.parent.getPose(Space.PARENT), movedParentPose, POSE_TOLERANCE)
            val movedParentActivityPos = gltfs.parent.getPose(Space.ACTIVITY).translation
            assertVector3(
                movedParentActivityPos,
                initialParentActivityPos + GLTF_MOVE_DELTA,
                POSE_TOLERANCE,
            )
            assertChildrenKeepSpacing(
                gltfs,
                movedParentActivityPos,
                initialChild1OffsetFromParent,
                initialChild2OffsetFromParent,
            )

            // Step 3: Unhide "Child 1" GLTF and verify Child 1 & 2 still keep the same relative
            // spacing from the moved parent.
            gltfs.child1.setEnabled(true)
            assertHierarchyEnabled(
                gltfs.parent,
                gltfs.child1,
                gltfs.child2,
                expectParentEnabled = true,
                expectChild1Enabled = true,
                expectChild2Enabled = true,
            )
            assertChildrenKeepSpacing(
                gltfs,
                movedParentActivityPos,
                initialChild1OffsetFromParent,
                initialChild2OffsetFromParent,
            )
        }
    }

    @Test(timeout = 30_000L)
    @XrDeviceTest
    fun setEnabled_mainPanelAndActivitySpaceInHomeSpaceMode_updatesLocalEnabledState() {
        sessionRule.requestFullSpace()
        val panels = sessionRule.onMainThread { createPanelHierarchy() }

        // Transition to Home Space Mode (HSM).
        sessionRule.requestHomeSpace()
        val scene = sessionRule.session.scene
        assertThat(scene.spatialCapabilities).doesNotContain(SpatialCapability.SPATIAL_UI)
        assertLocallyEnabledAndAttached(
            panels,
            expectParentLocallyEnabled = true,
            expectChild1LocallyEnabled = true,
            expectChild2LocallyEnabled = true,
        )
        // TODO(b/583873060): Verify spatial panels are not rendered in HSM.

        // Hiding the main panel and activity space in HSM updates their local SceneCore
        // enabled state.
        scene.mainPanelEntity.setEnabled(false)
        scene.activitySpace.setEnabled(false)
        assertThat(scene.mainPanelEntity.isEnabled(includeParents = false)).isFalse()
        assertThat(scene.activitySpace.isEnabled(includeParents = false)).isFalse()
        // TODO(b/583873060): Verify the main task window stays visible in HSM after the hide.
    }

    /**
     * Creates the 3-level [PanelEntity] hierarchy (`Parent Panel` -> `Child Panel 1` -> `Child
     * Panel 2`) parented under the main panel, matching `VisibilityActivity`. Must be called on the
     * main thread, i.e. inside `sessionRule.onMainThread { }`.
     */
    private fun XrSessionScope<*>.createPanelHierarchy(): PanelHierarchy {
        val parentPanel = createMovablePanel(PARENT_PANEL_NAME, PARENT_PANEL_POSE, mainPanel)
        val childPanel1 =
            createMovablePanel(CHILD_PANEL_1_NAME, CHILD_PANEL_OFFSET_POSE, parentPanel)
        val childPanel2 =
            createMovablePanel(CHILD_PANEL_2_NAME, CHILD_PANEL_OFFSET_POSE, childPanel1)
        return PanelHierarchy(
            root = mainPanel,
            parent = parentPanel,
            child1 = childPanel1,
            child2 = childPanel2,
        )
    }

    private val XrSessionScope<*>.mainPanel: Entity
        get() = session.scene.mainPanelEntity

    /** Creates a [PanelEntity] named [name] with a system [MovableComponent]. */
    private fun XrSessionScope<*>.createMovablePanel(
        name: String,
        pose: Pose,
        parent: Entity,
    ): PanelEntity {
        val panel =
            PanelEntity.create(
                session = session,
                view = TextView(activity).apply { text = name },
                pixelDimensions = PANEL_PIXEL_DIMENSIONS,
                name = name,
                pose = pose,
                parent = parent,
            )
        assertThat(panel.addComponent(MovableComponent.createSystemMovable(session))).isTrue()
        return panel
    }

    /**
     * Loads the test [GltfModel] and creates the 3-level [GltfModelEntity] hierarchy (`parent` ->
     * `child1` -> `child2`) parented under the activity space, matching `VisibilityActivity`. Must
     * be called on the main thread, i.e. inside `sessionRule.onMainThread { }`.
     */
    private suspend fun XrSessionScope<*>.createGltfHierarchy(): GltfHierarchy {
        val gltfModel = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
        val activitySpace = session.scene.activitySpace

        fun createGltf(pose: Pose, parent: Entity): GltfModelEntity =
            GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = pose,
                    parent = parent,
                )
                .apply { setScale(GLTF_LOCAL_SCALE) }

        val parentGltf = createGltf(PARENT_GLTF_POSE, activitySpace)
        val childGltf1 = createGltf(CHILD_GLTF_1_POSE, parentGltf)
        val childGltf2 = createGltf(CHILD_GLTF_2_POSE, childGltf1)
        return GltfHierarchy(
            root = activitySpace,
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
     * Asserts the local enabled state (`includeParents = false`) of each entity in [hierarchy], and
     * that hiding did not detach or reparent any of them.
     */
    private fun assertLocallyEnabledAndAttached(
        hierarchy: EntityHierarchy,
        expectParentLocallyEnabled: Boolean,
        expectChild1LocallyEnabled: Boolean,
        expectChild2LocallyEnabled: Boolean,
    ) {
        assertThat(hierarchy.parent.isEnabled(includeParents = false))
            .isEqualTo(expectParentLocallyEnabled)
        assertThat(hierarchy.child1.isEnabled(includeParents = false))
            .isEqualTo(expectChild1LocallyEnabled)
        assertThat(hierarchy.child2.isEnabled(includeParents = false))
            .isEqualTo(expectChild2LocallyEnabled)
        assertThat(hierarchy.parent.parent).isSameInstanceAs(hierarchy.root)
        assertThat(hierarchy.child1.parent).isSameInstanceAs(hierarchy.parent)
        assertThat(hierarchy.child2.parent).isSameInstanceAs(hierarchy.child1)
    }

    /**
     * Asserts that both glTF children keep their local poses and their initial ActivitySpace
     * offsets from the parent, now at [parentActivityPos].
     */
    private fun assertChildrenKeepSpacing(
        gltfs: GltfHierarchy,
        parentActivityPos: Vector3,
        expectedChild1Offset: Vector3,
        expectedChild2Offset: Vector3,
    ) {
        assertPose(gltfs.child1.getPose(Space.PARENT), CHILD_GLTF_1_POSE, POSE_TOLERANCE)
        assertPose(gltfs.child2.getPose(Space.PARENT), CHILD_GLTF_2_POSE, POSE_TOLERANCE)
        assertVector3(
            gltfs.child1.getPose(Space.ACTIVITY).translation - parentActivityPos,
            expectedChild1Offset,
            POSE_TOLERANCE,
        )
        assertVector3(
            gltfs.child2.getPose(Space.ACTIVITY).translation - parentActivityPos,
            expectedChild2Offset,
            POSE_TOLERANCE,
        )
    }

    /** A 3-level entity hierarchy (`parent` -> `child1` -> `child2`) attached under [root]. */
    private interface EntityHierarchy {
        val root: Entity
        val parent: Entity
        val child1: Entity
        val child2: Entity
    }

    private class PanelHierarchy(
        override val root: Entity,
        override val parent: PanelEntity,
        override val child1: PanelEntity,
        override val child2: PanelEntity,
    ) : EntityHierarchy

    private class GltfHierarchy(
        override val root: Entity,
        override val parent: GltfModelEntity,
        override val child1: GltfModelEntity,
        override val child2: GltfModelEntity,
    ) : EntityHierarchy

    private companion object {
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
    }
}
