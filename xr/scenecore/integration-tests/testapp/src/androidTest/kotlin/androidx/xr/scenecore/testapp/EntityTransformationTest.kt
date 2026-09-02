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

package androidx.xr.scenecore.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
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
 * - Relative spatial transformations (`transformPoseTo`, `transformPositionTo`,
 *   `transformVectorTo`, `transformDirectionTo`).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class EntityTransformationTest {

    @Test
    fun hierarchy_parentingAndReparentingUpdatesChildrenLists() = runTestWithSession { session ->
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

        // Re-parent using addChild
        parentA.addChild(child)
        assertThat(child.parent).isEqualTo(parentA)
        assertThat(parentA.children).containsExactly(child)
        assertThat(parentB.children).isEmpty()

        // Remove child from parent
        child.parent = null
        assertThat(child.parent).isNull()
        assertThat(parentA.children).isEmpty()
    }

    @Test
    fun multiLevelHierarchy_poseAndRotationPropagatesToActivitySpace() =
        runTestWithSession { session ->
            // Root entity at (0, 1, -2) rotated 90° Yaw around Y
            val rootRot = Quaternion.fromAxisAngle(Vector3.Up, 90f)
            val root =
                Entity.create(
                    session,
                    pose = Pose(Vector3(0f, 1f, -2f), rootRot),
                    parent = session.scene.activitySpace,
                )

            // Child offset +1.0m locally along X (due to 90° Yaw, local +X points to Activity -Z)
            val child = Entity.create(session, pose = Pose(Vector3(1f, 0f, 0f)), parent = root)

            // Grandchild offset +0.5m locally along Y
            val grandchild =
                Entity.create(session, pose = Pose(Vector3(0f, 0.5f, 0f)), parent = child)

            // Verify local parent poses
            assertPose(child.getPose(Space.PARENT), Pose(Vector3(1f, 0f, 0f)))
            assertPose(grandchild.getPose(Space.PARENT), Pose(Vector3(0f, 0.5f, 0f)))

            // Verify propagated ActivitySpace poses:
            // Child position: (0, 1, -2) + Rot90Y * (1, 0, 0) = (0, 1, -2) + (0, 0, -1) = (0, 1,
            // -3)
            assertPose(child.getPose(Space.ACTIVITY), Pose(Vector3(0f, 1f, -3f), rootRot))

            // Grandchild position: (0, 1, -3) + (0, 0.5, 0) = (0, 1.5, -3)
            assertPose(grandchild.getPose(Space.ACTIVITY), Pose(Vector3(0f, 1.5f, -3f), rootRot))
        }

    @Test
    fun setPoseInActivitySpace_computesCorrectLocalPoseRelativeToParent() =
        runTestWithSession { session ->
            val root =
                Entity.create(
                    session,
                    pose = Pose(Vector3(1f, 1f, -2f)),
                    parent = session.scene.activitySpace,
                )
            val child = Entity.create(session, pose = Pose(), parent = root)

            // Move child to target pose in ActivitySpace
            val targetActivityPose = Pose(Vector3(3f, 1f, -2f))
            child.setPose(targetActivityPose, Space.ACTIVITY)

            // Child's pose in ActivitySpace should match target
            assertPose(child.getPose(Space.ACTIVITY), targetActivityPose)

            // Child's local pose relative to root should be (3 - 1, 1 - 1, -2 - (-2)) = (2, 0, 0)
            assertPose(child.getPose(Space.PARENT), Pose(Vector3(2f, 0f, 0f)))
        }

    @Test
    fun scalePropagation_propagatesThroughHierarchyAndScalesOffsets() =
        runTestWithSession { session ->
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
            // ActivitySpace
            assertThat(child.getPose(Space.ACTIVITY).translation.x).isWithin(1e-4f).of(2.0f)
        }

    @Test
    fun transformPoseTo_transformsPosesBetweenSiblingEntitiesAndActivitySpace() =
        runTestWithSession { session ->
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
    fun transformSpatialMath_positionVectorAndDirectionHandleScaleAndRotation() =
        runTestWithSession { session ->
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
            // Local (1, 0, 0) scaled by 2 -> (2, 0, 0), rotated 90°Y -> (0, 0, -2), translated ->
            // (0, 1, -4)
            val worldPos =
                entity.transformPositionTo(Vector3(1f, 0f, 0f), session.scene.activitySpace)
            assertVector3(worldPos, Vector3(0f, 1f, -4f))

            // 2. Vector: affected by rotation and scale, but NOT translation
            // Local (1, 0, 0) scaled by 2 -> (2, 0, 0), rotated 90°Y -> (0, 0, -2)
            val worldVec =
                entity.transformVectorTo(Vector3(1f, 0f, 0f), session.scene.activitySpace)
            assertVector3(worldVec, Vector3(0f, 0f, -2f))

            // 3. Direction vector: ignores translation and entity scale, preserves input
            // magnitude, and applies rotation ONLY.
            // Non-unit input (3, 0, 0) with entity scale 2.0x is NOT scaled to magnitude 6.0;
            // instead, input magnitude 3.0 is preserved, and rotated 90°Y -> (0, 0, -3)
            val worldDir =
                entity.transformDirectionTo(Vector3(3f, 0f, 0f), session.scene.activitySpace)
            assertVector3(worldDir, Vector3(0f, 0f, -3f))
        }
}
