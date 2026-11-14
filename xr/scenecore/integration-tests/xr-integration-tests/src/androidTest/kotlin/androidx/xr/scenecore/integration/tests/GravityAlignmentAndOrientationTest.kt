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
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.manifest.HEAD_TRACKING
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PerceptionSpace
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.FakeHeadTrackingRule
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitForSettledSpatialTask
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Integration tests covering [ActivitySpace] gravity alignment and [Pose] orientation helpers
 * across Full Space Mode head tracking and live scene-graph entities (b/581430543).
 *
 * Uses [PerceptionSpace] (`session.scene.perceptionSpace`) as the public real-world reference space
 * in place of deprecated/library-restricted `Space.REAL_WORLD`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class GravityAlignmentAndOrientationTest {

    @get:Rule(order = 0) val headTrackingRule = FakeHeadTrackingRule()

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*filterSupportedPermissions(HEAD_TRACKING))

    @get:Rule(order = 2) val sessionRule = createXrSessionRule()

    @get:Rule(order = 3) val timeout: Timeout = Timeout.seconds(60)

    @Test
    fun activitySpace_remainsGravityAligned_whenHeadPitchesAndRollsInFullSpace() {
        val session = sessionRule.session
        val configureResult = sessionRule.onMainThread {
            session.configure(
                Config.Builder(session.config).setDeviceTracking(DeviceTrackingMode.SPATIAL).build()
            )
        }
        assertThat(configureResult).isInstanceOf(SessionConfigureSuccess::class.java)

        sessionRule.requestFullSpace()

        val arDevice = ArDevice.getInstance(session)
        waitUntil("ArDevice enters TRACKING state", timeout = TRACKING_TIMEOUT) {
            arDevice.state.value.trackingState == TrackingState.TRACKING
        }

        val activitySpace = session.scene.activitySpace
        val perceptionSpace = session.scene.perceptionSpace
        assertVector3(
            activitySpace.transformDirectionTo(Vector3.Up, perceptionSpace),
            Vector3.Up,
            GRAVITY_UP_TOLERANCE,
        )

        val targetHeadPose =
            Pose(
                translation = Vector3(0.3f, 0.2f, -0.4f),
                rotation = Quaternion.fromEulerAngles(pitch = 25f, yaw = -30f, roll = 20f),
            )
        headTrackingRule.performHeadMovement { poseHeadTo(targetHeadPose) }

        waitUntil(
            "ArDevice reflects pitched and rolled head pose",
            timeout = TRACKING_TIMEOUT,
        ) {
            val observedPose = arDevice.state.value.devicePose
            Vector3.distance(observedPose.translation, targetHeadPose.translation) <=
                POSITION_TOLERANCE_METERS &&
                Quaternion.angle(observedPose.rotation, targetHeadPose.rotation) <=
                    ROTATION_TOLERANCE_DEGREES
        }

        val headPoseInPerception = arDevice.state.value.devicePose
        val headPoseInActivity =
            perceptionSpace.transformPoseTo(headPoseInPerception, activitySpace)
        assertThat(Vector3.angleBetween(headPoseInPerception.up, Vector3.Up))
            .isGreaterThan(MIN_TILT_ANGLE_DEGREES)
        assertThat(Vector3.angleBetween(headPoseInActivity.up, Vector3.Up))
            .isGreaterThan(MIN_TILT_ANGLE_DEGREES)

        // ActivitySpace stays gravity-aligned (+Y up) in both PerceptionSpace and CPM world
        // space despite the tilted head.
        assertVector3(
            activitySpace.transformDirectionTo(Vector3.Up, perceptionSpace),
            Vector3.Up,
            GRAVITY_UP_TOLERANCE,
        )
        assertVector3(
            perceptionSpace.transformDirectionTo(Vector3.Up, activitySpace),
            Vector3.Up,
            GRAVITY_UP_TOLERANCE,
        )
        assertVector3(
            activitySpace.transformPoseTo(Pose.Identity, perceptionSpace).up,
            Vector3.Up,
            GRAVITY_UP_TOLERANCE,
        )
        assertActivitySpaceCpmRootGravityAligned()

        // Transition HSM -> FSM while the head remains pitched/rolled and re-verify origin
        // alignment.
        sessionRule.requestHomeSpace()
        sessionRule.requestFullSpace()

        waitUntil(
            "ActivitySpace gravity-aligned origin settles after Full Space transition",
            timeout = TRACKING_TIMEOUT,
        ) {
            Vector3.distance(
                activitySpace.transformDirectionTo(Vector3.Up, perceptionSpace),
                Vector3.Up,
            ) <= GRAVITY_UP_TOLERANCE
        }
        assertVector3(
            activitySpace.transformDirectionTo(Vector3.Up, perceptionSpace),
            Vector3.Up,
            GRAVITY_UP_TOLERANCE,
        )
        val postTransitionHeadInActivity =
            perceptionSpace.transformPoseTo(arDevice.state.value.devicePose, activitySpace)
        assertThat(Vector3.angleBetween(postTransitionHeadInActivity.up, Vector3.Up))
            .isGreaterThan(MIN_TILT_ANGLE_DEGREES)
        assertActivitySpaceCpmRootGravityAligned()
    }

    @Test
    fun gravityAlignedPlacement_fromTiltedArDevicePose_alignsChildEntityAndPanel() {
        val session = sessionRule.session
        val configureResult = sessionRule.onMainThread {
            session.configure(
                Config.Builder(session.config).setDeviceTracking(DeviceTrackingMode.SPATIAL).build()
            )
        }
        assertThat(configureResult).isInstanceOf(SessionConfigureSuccess::class.java)

        sessionRule.requestFullSpace()

        val arDevice = ArDevice.getInstance(session)
        waitUntil("ArDevice enters TRACKING state", timeout = TRACKING_TIMEOUT) {
            arDevice.state.value.trackingState == TrackingState.TRACKING
        }

        val tiltedHeadPose =
            Pose(
                translation = Vector3(0.0f, 0.1f, 0.0f),
                rotation = Quaternion.fromEulerAngles(pitch = 30f, yaw = 40f, roll = -20f),
            )
        headTrackingRule.performHeadMovement { poseHeadTo(tiltedHeadPose) }

        waitUntil("ArDevice reaches tilted orientation", timeout = TRACKING_TIMEOUT) {
            val observedPose = arDevice.state.value.devicePose
            Vector3.distance(observedPose.translation, tiltedHeadPose.translation) <=
                POSITION_TOLERANCE_METERS &&
                Quaternion.angle(observedPose.rotation, tiltedHeadPose.rotation) <=
                    ROTATION_TOLERANCE_DEGREES
        }

        val activitySpace = session.scene.activitySpace
        val perceptionSpace = session.scene.perceptionSpace
        val headPoseInActivity =
            perceptionSpace.transformPoseTo(arDevice.state.value.devicePose, activitySpace)
        assertThat(Vector3.angleBetween(headPoseInActivity.up, Vector3.Up))
            .isGreaterThan(MIN_TILT_ANGLE_DEGREES)

        // 1. Place an Entity oriented via getUpVectorToUpRotation(Pose.Identity) so its +Y axis
        // aligns with gravity while preserving the horizontal +X right direction.
        val gravityRotation = headPoseInActivity.getUpVectorToUpRotation(Pose.Identity)
        val gravityAlignedEntity =
            Entity.create(
                session = session,
                name = "gravity_aligned_from_head",
                pose = Pose(Vector3(0f, 0f, -1.5f), gravityRotation),
                parent = activitySpace,
            )

        val entityActivityPose = gravityAlignedEntity.getPose(Space.ACTIVITY)
        assertVector3(entityActivityPose.up, Vector3.Up, FLOAT_TOLERANCE)
        assertVector3(
            gravityAlignedEntity.transformDirectionTo(Vector3.Up, activitySpace),
            Vector3.Up,
            FLOAT_TOLERANCE,
        )
        assertVector3(
            gravityAlignedEntity.transformDirectionTo(Vector3.Up, perceptionSpace),
            Vector3.Up,
            GRAVITY_UP_TOLERANCE,
        )
        val expectedHorizontalRight =
            Vector3(headPoseInActivity.right.x, 0f, headPoseInActivity.right.z).toNormalized()
        val expectedHorizontalForward =
            (Vector3.Up.cross(headPoseInActivity.right.toNormalized())).toNormalized()
        assertVector3(entityActivityPose.right, expectedHorizontalRight, FLOAT_TOLERANCE)
        assertThat(Vector3.angleBetween(entityActivityPose.forward, expectedHorizontalForward))
            .isAtMost(ORIENTATION_ANGLE_TOLERANCE_DEGREES)

        // 2. Place a child PanelEntity under a tilted parent and compensate its orientation in
        // Space.ACTIVITY using getUpVectorToUpRotation(Pose.Identity).
        val tiltedParent =
            Entity.create(
                session = session,
                name = "tilted_parent",
                pose = Pose(Vector3(-0.6f, 0.2f, -1.5f), headPoseInActivity.rotation),
                parent = activitySpace,
            )
        val parentCompensatedRotation =
            tiltedParent.getPose(Space.ACTIVITY).getUpVectorToUpRotation(Pose.Identity)
        val forwardToUpRotation = Pose.Identity.getForwardVectorToUpRotation(headPoseInActivity)

        val (childPanel, forwardToHeadUpPanel) =
            sessionRule.onMainThread {
                val createdChildPanel =
                    PanelEntity.create(
                        session = session,
                        view = TextView(activity),
                        pixelDimensions = IntSize2d(480, 320),
                        name = GRAVITY_CHILD_PANEL_NAME,
                        pose = Pose.Identity,
                        parent = tiltedParent,
                    )
                val createdForwardPanel =
                    PanelEntity.create(
                        session = session,
                        view = TextView(activity),
                        pixelDimensions = IntSize2d(480, 320),
                        name = FORWARD_TO_HEAD_UP_PANEL_NAME,
                        pose = Pose(Vector3(0.6f, 0f, -1.5f), forwardToUpRotation),
                        parent = activitySpace,
                    )
                createdChildPanel to createdForwardPanel
            }

        childPanel.setPose(
            Pose(Vector3(-0.6f, 0.2f, -1.5f), parentCompensatedRotation),
            Space.ACTIVITY,
        )
        assertVector3(childPanel.getPose(Space.ACTIVITY).up, Vector3.Up, FLOAT_TOLERANCE)
        val expectedLocalUp = tiltedParent.getPose(Space.ACTIVITY).rotation.inverse * Vector3.Up
        assertVector3(childPanel.getPose(Space.PARENT).up, expectedLocalUp, FLOAT_TOLERANCE)

        // 3. Verify getForwardVectorToUpRotation(headPoseInActivity): Quaternion.fromLookTowards
        // aligns local +Z (Pose.backward) with headPoseInActivity.up while keeping local +X
        // leveled with horizontal gravity (y ~ 0).
        val panelActivityPose = forwardToHeadUpPanel.getPose(Space.ACTIVITY)
        val expectedTargetUp = headPoseInActivity.up.toNormalized()
        assertVector3(panelActivityPose.backward, expectedTargetUp, FLOAT_TOLERANCE)
        assertVector3(panelActivityPose.forward, -expectedTargetUp, FLOAT_TOLERANCE)
        assertThat(Vector3.angleBetween(panelActivityPose.backward, expectedTargetUp))
            .isAtMost(ORIENTATION_ANGLE_TOLERANCE_DEGREES)

        // 4. Inspect CPM embeddedWindowLeashes to verify live compositor orientations match the
        // SceneCore placements despite the tilted head pose.
        val settledTask =
            waitForSettledSpatialTask(
                activity = sessionRule.activity,
                expectedEmbeddedPanelCount = 2,
                requireFullSpace = true,
                timeout = SCENE_TIMEOUT,
            )
        val childPanelNode =
            checkNotNull(
                settledTask.embeddedWindowLeashes.firstOrNull {
                    it.name == GRAVITY_CHILD_PANEL_NAME && it.isVisible
                }
            ) {
                "Expected visible $GRAVITY_CHILD_PANEL_NAME in embeddedWindowLeashes"
            }
        val forwardPanelNode =
            checkNotNull(
                settledTask.embeddedWindowLeashes.firstOrNull {
                    it.name == FORWARD_TO_HEAD_UP_PANEL_NAME && it.isVisible
                }
            ) {
                "Expected visible $FORWARD_TO_HEAD_UP_PANEL_NAME in embeddedWindowLeashes"
            }

        val childPanelCpmUp = (childPanelNode.rotation * Vector3.Up).toNormalized()
        assertVector3(childPanelCpmUp, Vector3.Up, CPM_ROTATION_TOLERANCE)

        val forwardPanelCpmBackward = (forwardPanelNode.rotation * Vector3.Backward).toNormalized()
        val forwardPanelCpmRight = (forwardPanelNode.rotation * Vector3.Right).toNormalized()
        assertVector3(forwardPanelCpmBackward, expectedTargetUp, CPM_ROTATION_TOLERANCE)
        assertVector3(
            forwardPanelCpmRight,
            panelActivityPose.right.toNormalized(),
            CPM_ROTATION_TOLERANCE,
        )
    }

    private fun assertActivitySpaceCpmRootGravityAligned() {
        val settledTask =
            waitForSettledSpatialTask(
                activity = sessionRule.activity,
                requireFullSpace = true,
                timeout = SCENE_TIMEOUT,
            )
        val rootNode =
            checkNotNull(settledTask.findNode { it.name == ACTIVITY_SPACE_NODE_NAME }) {
                "Expected $ACTIVITY_SPACE_NODE_NAME in settled CPM task"
            }
        val rootUp = (rootNode.rotation * Vector3.Up).toNormalized()
        assertThat(abs(rootUp.x)).isLessThan(CPM_ROTATION_TOLERANCE)
        assertThat(abs(rootUp.z)).isLessThan(CPM_ROTATION_TOLERANCE)
        assertThat(rootUp.y).isGreaterThan(0.99f)
    }

    private companion object {
        const val ACTIVITY_SPACE_NODE_NAME = "SpatialSceneAndActivitySpaceRootNode"
        const val GRAVITY_CHILD_PANEL_NAME = "gravity_aligned_child_panel"
        const val FORWARD_TO_HEAD_UP_PANEL_NAME = "forward_to_head_up_panel"
        const val POSITION_TOLERANCE_METERS = 0.05f
        const val ROTATION_TOLERANCE_DEGREES = 3.0f
        const val ORIENTATION_ANGLE_TOLERANCE_DEGREES = 0.5f
        const val MIN_TILT_ANGLE_DEGREES = 15.0f
        const val FLOAT_TOLERANCE = 1e-4f
        const val GRAVITY_UP_TOLERANCE = 1e-3f
        const val CPM_ROTATION_TOLERANCE = 5e-2f
        val TRACKING_TIMEOUT = 10.seconds
        val SCENE_TIMEOUT = 5.seconds
    }
}
