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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests validating SceneCore Transformation and Positioning APIs.
 *
 * Covers:
 * - Scene graph parenting, hierarchy tree updates, and reparenting.
 * - Multi-level pose propagation and hierarchical spatial transformations.
 * - Parent space vs. ActivitySpace pose conversions (`Space.PARENT` vs `Space.ACTIVITY`).
 * - Hierarchical scale propagation.
 * - Combined rotation, translation, and scale composition across a three-level hierarchy.
 * - Relative spatial transformations (`transformPoseTo`, `transformPositionTo`,
 *   `transformVectorTo`, `transformDirectionTo`).
 *
 * Scope: these tests only verify the client-side API & State layer (SceneCore getters and transform
 * math); they do not inspect the CPM scene graph.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class EntityTransformationTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @Test
    fun hierarchy_parentingAndReparentingUpdatesChildrenLists() {
        sessionRule.requestFullSpace()
        val session = sessionRule.session
        val parentA =
            Entity.create(
                session,
                pose = Pose(Vector3(0f, 1f, -2f)),
                parent = session.scene.activitySpace,
            )
        val parentB =
            Entity.create(
                session,
                pose = Pose(Vector3(2f, 1f, -2f)),
                parent = session.scene.activitySpace,
            )
        val child = Entity.create(session, pose = Pose(Vector3(1f, 0f, 0f)), parent = parentA)

        // Verify initial hierarchy
        assertThat(child.parent).isEqualTo(parentA)
        assertThat(parentA.children).containsExactly(child)
        assertThat(parentB.children).isEmpty()

        // Reparent child to parentB
        child.parent = parentB
        assertThat(child.parent).isEqualTo(parentB)
        assertThat(parentA.children).isEmpty()
        assertThat(parentB.children).containsExactly(child)
        // Local pose is kept, so the ActivitySpace pose follows the new parent:
        // (2, 1, -2) + (1, 0, 0) = (3, 1, -2)
        assertPose(child.getPose(Space.ACTIVITY), Pose(Vector3(3f, 1f, -2f)))

        // Re-parent using addChild
        parentA.addChild(child)
        assertThat(child.parent).isEqualTo(parentA)
        assertThat(parentA.children).containsExactly(child)
        assertThat(parentB.children).isEmpty()
        // (0, 1, -2) + (1, 0, 0) = (1, 1, -2)
        assertPose(child.getPose(Space.ACTIVITY), Pose(Vector3(1f, 1f, -2f)))

        // Remove child from parent
        child.parent = null
        assertThat(child.parent).isNull()
        assertThat(parentA.children).isEmpty()
        assertThat(parentB.children).isEmpty()
    }

    @Test
    fun multiLevelHierarchy_poseAndRotationPropagatesToActivitySpace() {
        sessionRule.requestFullSpace()
        val session = sessionRule.session
        // Root entity at (0, 1, -2) rotated 90° Yaw around Y
        val rootRot = Quaternion.fromAxisAngle(Vector3.Up, 90f)
        val root =
            Entity.create(
                session,
                pose = Pose(Vector3(0f, 1f, -2f), rootRot),
                parent = session.scene.activitySpace,
            )
        // Child offset +1.0m locally along X (due to 90° Yaw, local +X points to
        // Activity -Z)
        val child = Entity.create(session, pose = Pose(Vector3(1f, 0f, 0f)), parent = root)

        // Grandchild offset +0.5m locally along Y
        val grandchild = Entity.create(session, pose = Pose(Vector3(0f, 0.5f, 0f)), parent = child)

        // Verify local parent poses
        assertPose(child.getPose(Space.PARENT), Pose(Vector3(1f, 0f, 0f)))
        assertPose(grandchild.getPose(Space.PARENT), Pose(Vector3(0f, 0.5f, 0f)))

        // Verify propagated ActivitySpace poses:
        // Child position:
        //   (0, 1, -2) + Rot90Y * (1, 0, 0) = (0, 1, -2) + (0, 0, -1) = (0, 1, -3)
        assertPose(child.getPose(Space.ACTIVITY), Pose(Vector3(0f, 1f, -3f), rootRot))

        // Grandchild position: (0, 1, -3) + (0, 0.5, 0) = (0, 1.5, -3)
        assertPose(
            grandchild.getPose(Space.ACTIVITY),
            Pose(Vector3(0f, 1.5f, -3f), rootRot),
        )
    }

    @Test
    fun setPoseInActivitySpace_computesCorrectLocalPoseRelativeToParent() {
        sessionRule.requestFullSpace()
        val session = sessionRule.session
        val root =
            Entity.create(
                session,
                pose = Pose(Vector3(1f, 1f, -2f)),
                parent = session.scene.activitySpace,
            )
        val child = Entity.create(session, pose = Pose.Identity, parent = root)

        // Move child to target pose in ActivitySpace
        val targetActivityPose = Pose(Vector3(3f, 1f, -2f))
        child.setPose(targetActivityPose, Space.ACTIVITY)

        // Child's pose in ActivitySpace should match target
        assertPose(child.getPose(Space.ACTIVITY), targetActivityPose)

        // Child's local pose relative to root should be (3 - 1, 1 - 1, -2 - (-2)) =
        // (2, 0, 0)
        assertPose(child.getPose(Space.PARENT), Pose(Vector3(2f, 0f, 0f)))
    }

    @Test
    fun scalePropagation_propagatesThroughHierarchyAndScalesOffsets() {
        sessionRule.requestFullSpace()
        val session = sessionRule.session
        val root =
            Entity.create(
                    session,
                    pose = Pose(Vector3(0f, 1f, -2f)),
                    parent = session.scene.activitySpace,
                )
                .apply { setScale(2.0f) }
        val child =
            Entity.create(session, pose = Pose(Vector3(1f, 0f, 0f)), parent = root).apply {
                setScale(3.0f)
            }

        // Verify local scales
        assertThat(root.getScale(Space.PARENT)).isWithin(1e-4f).of(2.0f)
        assertThat(child.getScale(Space.PARENT)).isWithin(1e-4f).of(3.0f)

        // Verify propagated ActivitySpace scale (2.0 * 3.0 = 6.0)
        assertThat(child.getScale(Space.ACTIVITY)).isWithin(1e-4f).of(6.0f)

        // Child local offset (1, 0, 0) scaled by root (2.0) translates +2.0m along X in
        // ActivitySpace: (0, 1, -2) + (2, 0, 0) = (2, 1, -2)
        assertVector3(child.getPose(Space.ACTIVITY).translation, Vector3(2f, 1f, -2f))
    }

    @Test
    fun multiLevelHierarchy_rotationTranslationAndScaleComposeInActivitySpace() {
        sessionRule.requestFullSpace()
        val session = sessionRule.session
        // Three-level Sun -> Planet -> Moon hierarchy where every level has a non-trivial
        // translation, rotation and uniform scale, so each child's ActivitySpace pose must
        // apply its parent's rotation and accumulated scale to its local translation.
        val sunPos = Vector3(-0.5f, 0.5f, -1f)
        val sunRot = Quaternion.fromEulerAngles(-10f, 20f, 30f)
        val sunScale = 0.8f
        val sunEntity =
            Entity.create(
                    session = session,
                    pose = Pose(sunPos, sunRot),
                    parent = session.scene.activitySpace,
                )
                .apply { setScale(sunScale) }

        val planetPos = Vector3(-1f, 2f, -0.5f)
        val planetRot = Quaternion.fromEulerAngles(20f, -30f, 40f)
        val planetScale = 0.6f
        val planetEntity =
            Entity.create(
                    session = session,
                    pose = Pose(planetPos, planetRot),
                    parent = sunEntity,
                )
                .apply { setScale(planetScale) }

        val moonPos = Vector3(-1.5f, 2f, -0.5f)
        val moonRot = Quaternion.fromEulerAngles(30f, 40f, -50f)
        val moonScale = 0.4f
        val moonEntity =
            Entity.create(
                    session = session,
                    pose = Pose(moonPos, moonRot),
                    parent = planetEntity,
                )
                .apply { setScale(moonScale) }

        // Verify the computed pose in ActivitySpace matches the expected composition.
        val sunActivityPose = Pose(sunPos, sunRot)
        assertPose(sunEntity.getPose(Space.ACTIVITY), sunActivityPose)

        val planetActivityPose = sunActivityPose.compose(Pose(planetPos * sunScale, planetRot))
        assertPose(planetEntity.getPose(Space.ACTIVITY), planetActivityPose)

        val moonActivityPose =
            planetActivityPose.compose(Pose(moonPos * (sunScale * planetScale), moonRot))
        assertPose(moonEntity.getPose(Space.ACTIVITY), moonActivityPose, epsilon = 1e-4f)
    }

    @Test
    fun transformPoseTo_transformsPosesBetweenSiblingEntitiesAndActivitySpace() {
        sessionRule.requestFullSpace()
        val session = sessionRule.session
        val entityA =
            Entity.create(
                session,
                pose = Pose(Vector3(1f, 0f, -2f)),
                parent = session.scene.activitySpace,
            )
        val entityB =
            Entity.create(
                session,
                pose = Pose(Vector3(4f, 0f, -2f)),
                parent = session.scene.activitySpace,
            )

        // Pose at entityA origin transformed to entityB
        val poseInB = entityA.transformPoseTo(Pose.Identity, entityB)
        // B is at x=4, A is at x=1 -> relative to B, A is at x = -3
        assertPose(poseInB, Pose(Vector3(-3f, 0f, 0f)))

        // Symmetrically, origin at B transformed to A should be at x = +3
        val poseInA = entityB.transformPoseTo(Pose.Identity, entityA)
        assertPose(poseInA, Pose(Vector3(3f, 0f, 0f)))

        // Transforming pose from EntityA to ActivitySpace
        val poseInActivity =
            entityA.transformPoseTo(Pose(Vector3(0f, 2f, 0f)), session.scene.activitySpace)
        assertPose(poseInActivity, Pose(Vector3(1f, 2f, -2f)))
    }

    @Test
    fun transformSpatialMath_positionVectorAndDirectionHandleScaleAndRotation() {
        sessionRule.requestFullSpace()
        val session = sessionRule.session
        // Entity with 90° Yaw rotation and 2.0x scale
        val rot90Y = Quaternion.fromAxisAngle(Vector3.Up, 90f)
        val entity =
            Entity.create(
                    session,
                    pose = Pose(Vector3(0f, 1f, -2f), rot90Y),
                    parent = session.scene.activitySpace,
                )
                .apply { setScale(2.0f) }

        // 1. Position vector: affected by translation, rotation, and scale
        // Local (1, 0, 0) scaled by 2 -> (2, 0, 0), rotated 90°Y -> (0, 0, -2),
        // translated -> (0, 1, -4)
        val worldPos = entity.transformPositionTo(Vector3(1f, 0f, 0f), session.scene.activitySpace)
        assertVector3(worldPos, Vector3(0f, 1f, -4f))

        // 2. Vector: affected by rotation and scale, but NOT translation
        // Local (1, 0, 0) scaled by 2 -> (2, 0, 0), rotated 90°Y -> (0, 0, -2)
        val worldVec = entity.transformVectorTo(Vector3(1f, 0f, 0f), session.scene.activitySpace)
        assertVector3(worldVec, Vector3(0f, 0f, -2f))

        // 3. Direction vector: ignores translation and entity scale, preserves input
        // magnitude, and applies rotation ONLY.
        // Non-unit input (3, 0, 0) with entity scale 2.0x is NOT scaled to magnitude 6.0;
        // instead, input magnitude 3.0 is preserved, and rotated 90°Y -> (0, 0, -3)
        val worldDir = entity.transformDirectionTo(Vector3(3f, 0f, 0f), session.scene.activitySpace)
        assertVector3(worldDir, Vector3(0f, 0f, -3f))
    }
}
