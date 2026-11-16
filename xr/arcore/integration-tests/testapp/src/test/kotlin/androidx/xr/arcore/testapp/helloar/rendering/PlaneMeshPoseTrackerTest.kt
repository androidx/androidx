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

package androidx.xr.arcore.testapp.helloar.rendering

import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PlaneMeshPoseTrackerTest {

    @Test
    fun planeMeshPoseInAnchorSpace_centerAtAnchor_returnsIdentity() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion.fromAxisAngle(Vector3.Up, 30f))

        val meshPose = planeMeshPoseInAnchorSpace(anchorPose = pose, centerPose = pose)

        assertPoseNear(meshPose, Pose.Identity)
    }

    @Test
    fun planeMeshPoseInAnchorSpace_centerDriftedInPlane_returnsDriftAsTranslation() {
        val anchorPose = Pose(Vector3(1f, 0f, -2f), Quaternion.Identity)
        val centerPose = Pose(Vector3(1.1f, 0f, -2.05f), Quaternion.Identity)

        val meshPose = planeMeshPoseInAnchorSpace(anchorPose, centerPose)

        assertPoseNear(meshPose, Pose(Vector3(0.1f, 0f, -0.05f), Quaternion.Identity))
    }

    @Test
    fun planeMeshPoseInAnchorSpace_yawedAnchor_returnsDriftInAnchorLocalSpace() {
        // A 90 degree yaw maps the anchor's local +X axis to world -Z.
        val rotation = Quaternion.fromAxisAngle(Vector3.Up, 90f)
        val anchorPose = Pose(Vector3(0f, 1f, 0f), rotation)
        val centerPose = Pose(Vector3(0f, 1f, -0.1f), rotation)

        val meshPose = planeMeshPoseInAnchorSpace(anchorPose, centerPose)

        assertPoseNear(meshPose, Pose(Vector3(0.1f, 0f, 0f), Quaternion.Identity))
    }

    @Test
    fun planeMeshPoseInAnchorSpace_rotatedVerticalPlane_returnsLocalTranslationAndRotation() {
        // Tilting the plane +Y normal by 90 degrees around +X makes it face world +Z, like a wall.
        val wallRotation = Quaternion.fromAxisAngle(Vector3.Right, 90f)
        val inPlaneRotation = Quaternion.fromAxisAngle(Vector3.Up, 15f)
        val anchorPose = Pose(Vector3(0.5f, 1.2f, -3f), wallRotation)
        val centerPose = Pose(Vector3(0.5f, 1.4f, -3f), wallRotation * inPlaneRotation)

        val meshPose = planeMeshPoseInAnchorSpace(anchorPose, centerPose)

        // The world +Y drift lies in the wall and maps to the anchor's local -Z axis, with no
        // offset along the plane normal (local +Y).
        assertPoseNear(meshPose, Pose(Vector3(0f, 0f, -0.2f), inPlaneRotation))
    }

    @Test
    fun planeMeshPoseInAnchorSpace_composedWithAnchorPose_returnsCenterPose() {
        val anchorPose =
            Pose(Vector3(-0.7f, 0.3f, 2.4f), Quaternion.fromEulerAngles(20f, -35f, 10f))
        val centerPose =
            Pose(Vector3(-0.2f, 0.25f, 2.9f), Quaternion.fromEulerAngles(18f, -50f, 12f))

        val meshPose = planeMeshPoseInAnchorSpace(anchorPose, centerPose)

        assertPoseNear(anchorPose compose meshPose, centerPose)
    }

    @Test
    fun update_initialPausedAtIdentityAnchor_returnsIdentity() {
        val initialCenterPose =
            Pose(Vector3(0.4f, -1.3f, -2.1f), Quaternion.fromAxisAngle(Vector3.Up, 30f))
        val tracker = PlaneMeshPoseTracker(initialCenterPose)

        val meshPose = tracker.update(TrackingState.PAUSED, Pose.Identity, initialCenterPose)

        assertPoseNear(meshPose, Pose.Identity)
    }

    @Test
    fun update_unlocatedPausedAnchorAfterCenterShift_usesInitialCenterPose() {
        val initialCenterPose =
            Pose(Vector3(0.4f, -1.3f, -2.1f), Quaternion.fromAxisAngle(Vector3.Up, 30f))
        val tracker = PlaneMeshPoseTracker(initialCenterPose)
        val shiftedCenterPose =
            Pose(Vector3(0.55f, -1.3f, -2.2f), Quaternion.fromAxisAngle(Vector3.Up, 45f))
        tracker.update(TrackingState.PAUSED, Pose.Identity, initialCenterPose)

        val meshPose = tracker.update(TrackingState.PAUSED, Pose.Identity, shiftedCenterPose)

        assertPoseNear(initialCenterPose compose meshPose, shiftedCenterPose)
    }

    @Test
    fun update_locatedPausedAnchor_usesReportedAnchorPose() {
        val initialCenterPose = Pose(Vector3(0f, -1f, -2f), Quaternion.Identity)
        val tracker = PlaneMeshPoseTracker(initialCenterPose)
        val locatedAnchorPose = Pose(Vector3(0.02f, -1f, -1.98f), Quaternion.Identity)
        val shiftedCenterPose = Pose(Vector3(0.22f, -1f, -2.08f), Quaternion.Identity)

        val meshPose = tracker.update(TrackingState.PAUSED, locatedAnchorPose, shiftedCenterPose)

        assertPoseNear(meshPose, Pose(Vector3(0.2f, 0f, -0.1f), Quaternion.Identity))
    }

    @Test
    fun update_trackingAfterPausedFallback_usesReportedAnchorPose() {
        val initialCenterPose = Pose(Vector3(1f, 0f, -2f), Quaternion.Identity)
        val tracker = PlaneMeshPoseTracker(initialCenterPose)
        val shiftedCenterPose = Pose(Vector3(1.1f, 0f, -2.1f), Quaternion.Identity)
        val locatedAnchorPose = Pose(Vector3(1.02f, 0f, -2.01f), Quaternion.Identity)
        val trackingAnchorPose = Pose(Vector3(1.01f, 0f, -2.02f), Quaternion.Identity)
        val trackingCenterPose = Pose(Vector3(1.21f, 0f, -2.12f), Quaternion.Identity)
        tracker.update(TrackingState.PAUSED, Pose.Identity, shiftedCenterPose)
        tracker.update(TrackingState.PAUSED, locatedAnchorPose, shiftedCenterPose)

        val trackingPose =
            tracker.update(TrackingState.TRACKING, trackingAnchorPose, trackingCenterPose)

        assertPoseNear(trackingPose, Pose(Vector3(0.2f, 0f, -0.1f), Quaternion.Identity))
    }

    @Test
    fun update_trackingAnchorAtWorldOrigin_usesReportedAnchorPose() {
        val initialCenterPose = Pose(Vector3(0.5f, -1f, -2f), Quaternion.Identity)
        val tracker = PlaneMeshPoseTracker(initialCenterPose)
        val updatedCenterPose = Pose(Vector3(0.75f, 0.25f, -1.5f), Quaternion.Identity)

        val meshPose = tracker.update(TrackingState.TRACKING, Pose.Identity, updatedCenterPose)

        assertPoseNear(meshPose, updatedCenterPose)
    }

    @Test
    fun update_locatedThenPausedAtIdentity_preservesLastValidMeshPose() {
        val initialCenterPose = Pose(Vector3(0.5f, -1f, -2f), Quaternion.Identity)
        val tracker = PlaneMeshPoseTracker(initialCenterPose)
        val expectedMeshPose =
            tracker.update(
                TrackingState.TRACKING,
                initialCenterPose,
                Pose(Vector3(0.65f, -1f, -2.05f), Quaternion.Identity),
            )
        val updatedCenterPose = Pose(Vector3(0.25f, 0f, -0.5f), Quaternion.Identity)

        val meshPose = tracker.update(TrackingState.PAUSED, Pose.Identity, updatedCenterPose)

        assertPoseNear(meshPose, expectedMeshPose)
    }

    @Test
    fun update_stoppedAnchor_preservesLastValidMeshPose() {
        val anchorPose = Pose(Vector3(1f, 0f, -2f), Quaternion.Identity)
        val tracker = PlaneMeshPoseTracker(anchorPose)
        val expectedMeshPose =
            tracker.update(
                TrackingState.TRACKING,
                anchorPose,
                Pose(Vector3(1.15f, 0f, -2.05f), Quaternion.Identity),
            )

        val meshPoseAfterStop =
            tracker.update(
                TrackingState.STOPPED,
                Pose.Identity,
                Pose(Vector3(3f, 2f, -5f), Quaternion.Identity),
            )

        assertPoseNear(meshPoseAfterStop, expectedMeshPose)
    }

    private fun assertPoseNear(actual: Pose, expected: Pose) {
        assertThat(actual.translation.x).isWithin(TOLERANCE).of(expected.translation.x)
        assertThat(actual.translation.y).isWithin(TOLERANCE).of(expected.translation.y)
        assertThat(actual.translation.z).isWithin(TOLERANCE).of(expected.translation.z)
        val sign = if (actual.rotation.dot(expected.rotation) < 0f) -1f else 1f
        assertThat(actual.rotation.x * sign).isWithin(TOLERANCE).of(expected.rotation.x)
        assertThat(actual.rotation.y * sign).isWithin(TOLERANCE).of(expected.rotation.y)
        assertThat(actual.rotation.z * sign).isWithin(TOLERANCE).of(expected.rotation.z)
        assertThat(actual.rotation.w * sign).isWithin(TOLERANCE).of(expected.rotation.w)
    }

    private companion object {
        private const val TOLERANCE = 1e-5f
    }
}
