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

package androidx.xr.arcore.integration.tests

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AnchorResult
import androidx.xr.arcore.PerceptionState
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.RenderingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateTimedOut
import androidx.xr.runtime.SessionCreateUnknownError
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore [RenderViewpoint] field-of-view bounds and owner
 * identity, stereo VIEW-space rig geometry and pose composition, test-local `isPanelInView` CUJ
 * frustum classifier self-consistency and multi-corner edge clipping, dynamic head rotation
 * tracking, forward anchor placement, and unsupported mono viewpoint behavior.
 *
 * SceneCore `mainPanelEntity` coordinate-space seam tests and physical head-turn UI verification
 * remain manual because this test module does not enable `FakeHeadTrackingRule`
 * (`cpm.fake_headtracking` is not enabled).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ViewCameraFovTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.HEAD_TRACKING",
                "android.permission.SCENE_UNDERSTANDING_COARSE",
            )
        )

    private class StereoPerceptionSnapshot(
        val devicePose: Pose,
        val trackingState: TrackingState,
        val leftState: RenderViewpoint.State,
        val rightState: RenderViewpoint.State,
    )

    private class ForwardAnchorObservation(
        val anchorResult: AnchorResult,
        val settledState: Anchor.State?,
        val inLeftView: Boolean,
        val inRightView: Boolean,
    )

    private fun <T : Any> ActivityScenario<ComponentActivity>.withActivity(
        block: (ComponentActivity) -> T
    ): T {
        lateinit var result: T
        onActivity { activity -> result = block(activity) }
        return result
    }

    private fun isRenderingModeSupported(
        scenario: ActivityScenario<ComponentActivity>,
        mode: RenderingMode,
    ): Boolean = scenario.withActivity { activity ->
        XrDevice.getCurrentDevice(activity).isRenderingModeSupported(mode)
    }

    private fun assumeRenderingModeSupported(
        scenario: ActivityScenario<ComponentActivity>,
        mode: RenderingMode,
    ) {
        assumeTrue(
            "Skipping test: Device does not support $mode rendering",
            isRenderingModeSupported(scenario, mode),
        )
    }

    private fun assumeRenderingModeUnsupported(
        scenario: ActivityScenario<ComponentActivity>,
        mode: RenderingMode,
    ) {
        assumeFalse(
            "Skipping test: Device supports $mode rendering",
            isRenderingModeSupported(scenario, mode),
        )
    }

    private fun unwrapSessionCreateResult(result: SessionCreateResult): Session =
        when (result) {
            is SessionCreateSuccess -> result.session
            is SessionCreateApkRequired ->
                throw AssumptionViolatedException(
                    "Skipping test: Missing or outdated APK '${result.requiredApk}'"
                )
            is SessionCreateUnsupportedDevice ->
                throw AssumptionViolatedException(
                    "Skipping test: Device does not support required XR features"
                )
            is SessionCreateUnknownError ->
                error("SessionCreateUnknownError: ${result.errorMessage}")
            is SessionCreateTimedOut -> error("SessionCreateTimedOut")
            else -> error("Session.create failed with result: $result")
        }

    private suspend fun createSpatialSession(
        scenario: ActivityScenario<ComponentActivity>
    ): Session {
        val result = scenario.withActivity { activity ->
            runBlocking { Session.create(context = activity) }
        }
        val session = unwrapSessionCreateResult(result)
        val config = Config.Builder().setDeviceTracking(DeviceTrackingMode.SPATIAL).build()
        val configResult = session.configure(config)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure Session: $configResult"
        }
        val state =
            withTimeoutOrNull(PERCEPTION_TIMEOUT) {
                session.state.drop(1).first { it.perceptionState != null }
            }
        checkNotNull(state) { "Timed out waiting for perception state" }
        return session
    }

    private fun isValidPopulatedFov(fov: FieldOfView): Boolean =
        fov.angleRight > fov.angleLeft && fov.angleUp > fov.angleDown

    private fun toPopulatedStereoSnapshotOrNull(
        perceptionState: PerceptionState?
    ): StereoPerceptionSnapshot? {
        if (perceptionState == null) return null
        val left = perceptionState.leftRenderViewpointState ?: return null
        val right = perceptionState.rightRenderViewpointState ?: return null
        if (!isValidPopulatedFov(left.fieldOfView) || !isValidPopulatedFov(right.fieldOfView)) {
            return null
        }
        return StereoPerceptionSnapshot(
            devicePose = perceptionState.arDeviceState.devicePose,
            trackingState = perceptionState.arDeviceState.trackingState,
            leftState = left,
            rightState = right,
        )
    }

    private fun stereoSnapshots(session: Session): Flow<StereoPerceptionSnapshot> =
        session.state.mapNotNull { toPopulatedStereoSnapshotOrNull(it.perceptionState) }

    private suspend fun awaitPopulatedStereoSnapshot(session: Session): StereoPerceptionSnapshot {
        val snapshot = withTimeoutOrNull(PERCEPTION_TIMEOUT) { stereoSnapshots(session).first() }
        return checkNotNull(snapshot) { "Timed out waiting for populated stereo perception state" }
    }

    // This test module does not enable fake head tracking (cpm.fake_headtracking), so static
    // OpenXR runtimes may keep ArDevice outside TrackingState.TRACKING. Skip via
    // AssumptionViolatedException when TRACKING does not arrive within PERCEPTION_TIMEOUT.
    private suspend fun awaitTrackingStereoSnapshotOrSkip(
        session: Session,
        reason: String,
    ): StereoPerceptionSnapshot {
        val snapshot =
            withTimeoutOrNull(PERCEPTION_TIMEOUT) {
                stereoSnapshots(session).first { it.trackingState == TrackingState.TRACKING }
            }
        return snapshot ?: throw AssumptionViolatedException("Skipping test (b/581124038): $reason")
    }

    // Without fake head tracking or live device motion, orientation may remain static; skip
    // cleanly via AssumptionViolatedException when device orientation does not rotate in time.
    private suspend fun awaitRotatedStereoSnapshotOrSkip(
        session: Session,
        initialDeviceRotation: Quaternion,
    ): StereoPerceptionSnapshot {
        val snapshot =
            withTimeoutOrNull(PERCEPTION_TIMEOUT) {
                stereoSnapshots(session).first { candidate ->
                    candidate.trackingState == TrackingState.TRACKING &&
                        Quaternion.angle(
                            candidate.devicePose.rotation,
                            initialDeviceRotation,
                        ) > MIN_HEAD_ROTATION_DEGREES
                }
            }
        return snapshot
            ?: throw AssumptionViolatedException(
                "Skipping test: Device orientation did not rotate by more than " +
                    "$MIN_HEAD_ROTATION_DEGREES degrees within $PERCEPTION_TIMEOUT " +
                    "(fake head tracking not enabled)"
            )
    }

    private fun projectHorizontalEdgeAngleIntoEye(
        sourceState: RenderViewpoint.State,
        sourceHorizontalAngleRad: Float,
        targetState: RenderViewpoint.State,
    ): Float {
        val edgeInWorld =
            sourceState.pose.transformPoint(
                Vector3(
                    FRUSTUM_TEST_DEPTH_METERS * tan(sourceHorizontalAngleRad),
                    0f,
                    -FRUSTUM_TEST_DEPTH_METERS,
                )
            )
        val edgeInTargetLocal = targetState.pose.inverse.transformPoint(edgeInWorld)
        return atan2(edgeInTargetLocal.x, -edgeInTargetLocal.z)
    }

    private fun poseInEyeFrameAtHorizontalAngle(
        eyeState: RenderViewpoint.State,
        horizontalAngleRad: Float,
    ): Pose =
        eyeState.pose.compose(
            Pose(
                Vector3(
                    FRUSTUM_TEST_DEPTH_METERS * tan(horizontalAngleRad),
                    0f,
                    -FRUSTUM_TEST_DEPTH_METERS,
                )
            )
        )

    private fun buildLeftOnlyPanelPose(
        leftState: RenderViewpoint.State,
        rightState: RenderViewpoint.State,
    ): Pose {
        val rightLeftAngleInLeft =
            projectHorizontalEdgeAngleIntoEye(
                rightState,
                rightState.fieldOfView.angleLeft,
                leftState,
            )
        val upperLeftBound = min(leftState.fieldOfView.angleRight, rightLeftAngleInLeft)
        assertWithMessage(
                "Expected left stereo frustum to extend past right frustum's left edge " +
                    "(angleLeft=${leftState.fieldOfView.angleLeft}, " +
                    "upperLeftBound=$upperLeftBound)"
            )
            .that(upperLeftBound)
            .isGreaterThan(leftState.fieldOfView.angleLeft)
        val leftOnlyAngle = (leftState.fieldOfView.angleLeft + upperLeftBound) * 0.5f
        return poseInEyeFrameAtHorizontalAngle(leftState, leftOnlyAngle)
    }

    private fun buildRightOnlyPanelPose(
        leftState: RenderViewpoint.State,
        rightState: RenderViewpoint.State,
    ): Pose {
        val leftRightAngleInRight =
            projectHorizontalEdgeAngleIntoEye(
                leftState,
                leftState.fieldOfView.angleRight,
                rightState,
            )
        val lowerRightBound = max(rightState.fieldOfView.angleLeft, leftRightAngleInRight)
        assertWithMessage(
                "Expected right stereo frustum to extend past left frustum's right edge " +
                    "(lowerRightBound=$lowerRightBound, " +
                    "angleRight=${rightState.fieldOfView.angleRight})"
            )
            .that(rightState.fieldOfView.angleRight)
            .isGreaterThan(lowerRightBound)
        val rightOnlyAngle = (lowerRightBound + rightState.fieldOfView.angleRight) * 0.5f
        return poseInEyeFrameAtHorizontalAngle(rightState, rightOnlyAngle)
    }

    private fun computeEyeMidpointPose(snapshot: StereoPerceptionSnapshot): Pose =
        Pose(
            (snapshot.leftState.pose.translation + snapshot.rightState.pose.translation) * 0.5f,
            snapshot.devicePose.rotation,
        )

    private fun buildHighElevationOutOfViewPose(
        midpointPose: Pose,
        leftState: RenderViewpoint.State,
        rightState: RenderViewpoint.State,
    ): Pose {
        val maxAngleUp = max(leftState.fieldOfView.angleUp, rightState.fieldOfView.angleUp)
        val outOfViewPitch = (maxAngleUp + HALF_PI) * 0.5f
        val outOfViewPose =
            midpointPose.compose(
                Pose(
                    Vector3(
                        0f,
                        FRUSTUM_TEST_DEPTH_METERS * tan(outOfViewPitch),
                        -FRUSTUM_TEST_DEPTH_METERS,
                    )
                )
            )
        val minRequiredDepth = TEST_PANEL_SIZE.height + MIN_IN_VIEW_DEPTH_METERS
        val leftLocal = leftState.pose.inverse.transformPoint(outOfViewPose.translation)
        val rightLocal = rightState.pose.inverse.transformPoint(outOfViewPose.translation)
        check(-leftLocal.z > minRequiredDepth) {
            "Expected outOfViewPose in front of left eye: depth=${-leftLocal.z}"
        }
        check(-rightLocal.z > minRequiredDepth) {
            "Expected outOfViewPose in front of right eye: depth=${-rightLocal.z}"
        }
        return outOfViewPose
    }

    private fun isCornerInView(
        cornerInPanelSpace: Vector3,
        panelPoseInPerceptionSpace: Pose,
        cameraInverse: Pose,
        cameraFov: FieldOfView,
    ): Boolean {
        val cornerWorld = panelPoseInPerceptionSpace.transformPoint(cornerInPanelSpace)
        val cornerCameraLocal = cameraInverse.transformPoint(cornerWorld)
        // Cameras look down -Z, so depthInFrontOfCamera = -z must stay above the minimum threshold.
        val depthInFrontOfCamera = -cornerCameraLocal.z
        if (depthInFrontOfCamera < MIN_IN_VIEW_DEPTH_METERS) {
            return false
        }
        val horizontalAngle = atan2(cornerCameraLocal.x, depthInFrontOfCamera)
        val verticalAngle = atan2(cornerCameraLocal.y, depthInFrontOfCamera)
        val inHorizontalFov =
            horizontalAngle >= cameraFov.angleLeft && horizontalAngle <= cameraFov.angleRight
        val inVerticalFov =
            verticalAngle >= cameraFov.angleDown && verticalAngle <= cameraFov.angleUp
        return inHorizontalFov && inVerticalFov
    }

    // Test-local copy of PersistentAnchorsActivity.isPanelInView for CUJ frustum self-consistency.
    private fun isPanelInView(
        cameraState: RenderViewpoint.State,
        panelPoseInPerceptionSpace: Pose,
        panelSizeInMeters: FloatSize2d,
    ): Boolean {
        val cameraFov = cameraState.fieldOfView
        val cameraInverse = cameraState.pose.inverse
        val halfWidth = panelSizeInMeters.width / 2f
        val halfHeight = panelSizeInMeters.height / 2f
        val localCorners =
            listOf(
                Vector3(-halfWidth, halfHeight, 0f),
                Vector3(halfWidth, halfHeight, 0f),
                Vector3(-halfWidth, -halfHeight, 0f),
                Vector3(halfWidth, -halfHeight, 0f),
            )
        return localCorners.all { corner ->
            isCornerInView(corner, panelPoseInPerceptionSpace, cameraInverse, cameraFov)
        }
    }

    private fun isFiniteQuaternion(q: Quaternion): Boolean =
        q.x.isFinite() && q.y.isFinite() && q.z.isFinite() && q.w.isFinite()

    private fun isFinitePose(pose: Pose): Boolean =
        pose.translation.x.isFinite() &&
            pose.translation.y.isFinite() &&
            pose.translation.z.isFinite() &&
            isFiniteQuaternion(pose.rotation)

    private fun isNegativeFovAngle(angle: Float): Boolean =
        angle.isFinite() && angle > -HALF_PI && angle < 0f

    private fun isPositiveFovAngle(angle: Float): Boolean =
        angle.isFinite() && angle > 0f && angle < HALF_PI

    // OpenXR XrFovf stores signed optical-axis offsets in radians where angleLeft and angleDown
    // are negative and angleRight and angleUp are positive. RenderViewpoint initializes state
    // with FieldOfView(0f, 0f, 0f, 0f) (and FieldOfView() defaults to +PI/3), so requiring
    // negative angleLeft and angleDown rejects both unpopulated seeds and constructor defaults.
    private fun isValidXrFovf(fov: FieldOfView): Boolean =
        isNegativeFovAngle(fov.angleLeft) &&
            isPositiveFovAngle(fov.angleRight) &&
            isNegativeFovAngle(fov.angleDown) &&
            isPositiveFovAngle(fov.angleUp)

    private fun checkValidFieldOfViewAndPose(state: RenderViewpoint.State) {
        check(isValidXrFovf(state.fieldOfView)) {
            "Expected FieldOfView to satisfy XrFovf bounds in (-PI/2, 0) / (0, PI/2), got " +
                "${state.fieldOfView}"
        }
        check(isFinitePose(state.pose) && isFinitePose(state.localPose)) {
            "Expected finite RenderViewpoint pose=${state.pose} and localPose=${state.localPose}"
        }
    }

    private fun checkValidStereoSnapshot(snapshot: StereoPerceptionSnapshot) {
        checkValidFieldOfViewAndPose(snapshot.leftState)
        checkValidFieldOfViewAndPose(snapshot.rightState)
    }

    private fun assertFiniteQuaternion(q: Quaternion) {
        assertWithMessage("Expected finite quaternion, got $q").that(isFiniteQuaternion(q)).isTrue()
    }

    private fun assertFinitePose(pose: Pose) {
        assertWithMessage("Expected finite pose, got $pose").that(isFinitePose(pose)).isTrue()
    }

    private fun assertPoseEquals(
        actual: Pose,
        expected: Pose,
        translationEpsilon: Float = POSE_EPSILON_METERS,
        rotationDotEpsilon: Float = QUATERNION_DOT_EPSILON,
    ) {
        assertThat(actual.translation.x).isWithin(translationEpsilon).of(expected.translation.x)
        assertThat(actual.translation.y).isWithin(translationEpsilon).of(expected.translation.y)
        assertThat(actual.translation.z).isWithin(translationEpsilon).of(expected.translation.z)
        val dotProduct = Quaternion.dot(actual.rotation, expected.rotation)
        assertThat(abs(dotProduct)).isWithin(rotationDotEpsilon).of(1.0f)
    }

    private fun assertFieldOfViewEquals(
        actual: FieldOfView,
        expected: FieldOfView,
        epsilon: Float = FOV_EPSILON_RAD,
    ) {
        assertThat(actual.angleLeft).isWithin(epsilon).of(expected.angleLeft)
        assertThat(actual.angleRight).isWithin(epsilon).of(expected.angleRight)
        assertThat(actual.angleUp).isWithin(epsilon).of(expected.angleUp)
        assertThat(actual.angleDown).isWithin(epsilon).of(expected.angleDown)
    }

    private fun assertValidFieldOfViewAndPose(state: RenderViewpoint.State) {
        val fov = state.fieldOfView
        assertWithMessage(
                "Expected FieldOfView to satisfy XrFovf bounds in (-PI/2, 0) / (0, PI/2), got $fov"
            )
            .that(isValidXrFovf(fov))
            .isTrue()
        assertFinitePose(state.pose)
        assertFinitePose(state.localPose)
    }

    private suspend fun createAndObserveForwardAnchor(
        session: Session,
        forwardPose: Pose,
    ): ForwardAnchorObservation {
        val anchorResult = Anchor.create(session, forwardPose)
        if (anchorResult !is AnchorCreateSuccess) {
            return ForwardAnchorObservation(
                anchorResult = anchorResult,
                settledState = null,
                inLeftView = false,
                inRightView = false,
            )
        }
        val anchor = anchorResult.anchor
        val createdTimeMark = session.state.value.timeMark
        return try {
            val settledSnapshot =
                withTimeoutOrNull(PERCEPTION_TIMEOUT) {
                    session.state
                        .filter { it.timeMark - createdTimeMark >= ANCHOR_SETTLE_DURATION }
                        .mapNotNull { toPopulatedStereoSnapshotOrNull(it.perceptionState) }
                        .first()
                }
                    ?: return ForwardAnchorObservation(
                        anchorResult = anchorResult,
                        settledState = null,
                        inLeftView = false,
                        inRightView = false,
                    )
            val settledState = anchor.state.value
            ForwardAnchorObservation(
                anchorResult = anchorResult,
                settledState = settledState,
                inLeftView =
                    isPanelInView(
                        settledSnapshot.leftState,
                        settledState.pose,
                        ANCHOR_MARKER_SIZE,
                    ),
                inRightView =
                    isPanelInView(
                        settledSnapshot.rightState,
                        settledState.pose,
                        ANCHOR_MARKER_SIZE,
                    ),
            )
        } finally {
            anchor.detach()
        }
    }

    @Test
    @XrDeviceTest
    fun renderViewpoint_leftAndRightStereoViews_emitValidFieldOfViewAndOwnerIdentity() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                assumeRenderingModeSupported(scenario, RenderingMode.STEREO)
                val session = createSpatialSession(scenario)

                val snapshot = awaitPopulatedStereoSnapshot(session)
                val leftViewpoint = RenderViewpoint.left(session)
                val rightViewpoint = RenderViewpoint.right(session)
                val leftDirectState = leftViewpoint.state.value
                val rightDirectState = rightViewpoint.state.value

                assertThat(leftViewpoint).isNotEqualTo(rightViewpoint)
                assertThat(snapshot.leftState.owner).isEqualTo(leftViewpoint)
                assertThat(snapshot.rightState.owner).isEqualTo(rightViewpoint)
                assertValidFieldOfViewAndPose(snapshot.leftState)
                assertValidFieldOfViewAndPose(snapshot.rightState)
                assertThat(leftDirectState.owner).isEqualTo(leftViewpoint)
                assertThat(rightDirectState.owner).isEqualTo(rightViewpoint)
                assertValidFieldOfViewAndPose(leftDirectState)
                assertValidFieldOfViewAndPose(rightDirectState)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun renderViewpoint_stereoViewSpaceRig_satisfiesIpdCenteringFovMirrorAndWorldPoseComposition() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                assumeRenderingModeSupported(scenario, RenderingMode.STEREO)
                val session = createSpatialSession(scenario)

                val snapshot = awaitPopulatedStereoSnapshot(session)
                val leftState = snapshot.leftState
                val rightState = snapshot.rightState
                val ipdMeters =
                    rightState.localPose.translation.x - leftState.localPose.translation.x

                assertThat(leftState.localPose.translation.x).isLessThan(0f)
                assertThat(rightState.localPose.translation.x).isGreaterThan(0f)
                assertThat(ipdMeters).isWithin(IPD_TOLERANCE_METERS).of(NOMINAL_IPD_METERS)
                assertThat(
                        abs(leftState.localPose.translation.x + rightState.localPose.translation.x)
                    )
                    .isLessThan(VIEW_SYMMETRY_TOLERANCE_METERS)
                assertThat(
                        abs(leftState.localPose.translation.y - rightState.localPose.translation.y)
                    )
                    .isLessThan(VIEW_SYMMETRY_TOLERANCE_METERS)
                assertThat(abs(leftState.fieldOfView.angleLeft + rightState.fieldOfView.angleRight))
                    .isLessThan(FOV_SYMMETRY_TOLERANCE_RAD)
                assertThat(abs(leftState.fieldOfView.angleUp - rightState.fieldOfView.angleUp))
                    .isLessThan(FOV_SYMMETRY_TOLERANCE_RAD)
                // Guards RenderViewpoint.update() world-pose composition from devicePose and
                // localPose.
                assertPoseEquals(
                    leftState.pose,
                    snapshot.devicePose.compose(leftState.localPose),
                    translationEpsilon = STRICT_POSE_EPSILON_METERS,
                    rotationDotEpsilon = STRICT_QUATERNION_DOT_EPSILON,
                )
                assertPoseEquals(
                    rightState.pose,
                    snapshot.devicePose.compose(rightState.localPose),
                    translationEpsilon = STRICT_POSE_EPSILON_METERS,
                    rotationDotEpsilon = STRICT_QUATERNION_DOT_EPSILON,
                )
            }
        }
    }

    @Test
    @XrDeviceTest
    fun renderViewpoint_cujFrustumVisibilityClassifier_distinguishesAllFourStereoViewStates() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                assumeRenderingModeSupported(scenario, RenderingMode.STEREO)
                val session = createSpatialSession(scenario)
                val snapshot = awaitPopulatedStereoSnapshot(session)
                checkValidStereoSnapshot(snapshot)
                val leftState = snapshot.leftState
                val rightState = snapshot.rightState
                val midpointPose = computeEyeMidpointPose(snapshot)
                val bothInViewPose =
                    midpointPose.compose(Pose(Vector3(0f, 0f, -TEST_PANEL_OFFSET_METERS)))
                val leftOnlyPose = buildLeftOnlyPanelPose(leftState, rightState)
                val rightOnlyPose = buildRightOnlyPanelPose(leftState, rightState)
                val outOfViewPose =
                    buildHighElevationOutOfViewPose(midpointPose, leftState, rightState)

                val leftSeesBoth = isPanelInView(leftState, bothInViewPose, TEST_PANEL_SIZE)
                val rightSeesBoth = isPanelInView(rightState, bothInViewPose, TEST_PANEL_SIZE)
                val leftSeesLeftOnly = isPanelInView(leftState, leftOnlyPose, TEST_PANEL_SIZE)
                val rightSeesLeftOnly = isPanelInView(rightState, leftOnlyPose, TEST_PANEL_SIZE)
                val leftSeesRightOnly = isPanelInView(leftState, rightOnlyPose, TEST_PANEL_SIZE)
                val rightSeesRightOnly = isPanelInView(rightState, rightOnlyPose, TEST_PANEL_SIZE)
                val leftSeesOutOfView = isPanelInView(leftState, outOfViewPose, TEST_PANEL_SIZE)
                val rightSeesOutOfView = isPanelInView(rightState, outOfViewPose, TEST_PANEL_SIZE)

                assertThat(leftSeesBoth).isTrue()
                assertThat(rightSeesBoth).isTrue()
                assertThat(leftSeesLeftOnly).isTrue()
                assertThat(rightSeesLeftOnly).isFalse()
                assertThat(leftSeesRightOnly).isFalse()
                assertThat(rightSeesRightOnly).isTrue()
                assertThat(leftSeesOutOfView).isFalse()
                assertThat(rightSeesOutOfView).isFalse()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun renderViewpoint_cujFrustumVisibilityClassifier_clipsPanelStraddlingRightFrustumEdge() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                assumeRenderingModeSupported(scenario, RenderingMode.STEREO)
                val session = createSpatialSession(scenario)
                val snapshot = awaitPopulatedStereoSnapshot(session)
                checkValidStereoSnapshot(snapshot)
                val rightState = snapshot.rightState
                val rightEdgeX = FRUSTUM_TEST_DEPTH_METERS * tan(rightState.fieldOfView.angleRight)
                check(rightEdgeX > STRADDLING_PANEL_WIDTH_METERS) {
                    "Expected rightEdgeX ($rightEdgeX) > STRADDLING_PANEL_WIDTH_METERS " +
                        "($STRADDLING_PANEL_WIDTH_METERS)"
                }
                val straddlingRightEdgePose =
                    rightState.pose.compose(
                        Pose(
                            Vector3(
                                rightEdgeX - STRADDLE_EDGE_INSET_METERS,
                                0f,
                                -FRUSTUM_TEST_DEPTH_METERS,
                            )
                        )
                    )
                val straddlingPanelSize =
                    FloatSize2d(STRADDLING_PANEL_WIDTH_METERS, TEST_PANEL_SIZE.height)

                val insetPanelInView =
                    isPanelInView(rightState, straddlingRightEdgePose, TEST_PANEL_SIZE)
                val straddlingPanelInView =
                    isPanelInView(rightState, straddlingRightEdgePose, straddlingPanelSize)

                assertThat(insetPanelInView).isTrue()
                assertThat(straddlingPanelInView).isFalse()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun renderViewpoint_duringHeadRotation_updatesWorldPosesWhileKeepingLocalRigFixed() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                assumeRenderingModeSupported(scenario, RenderingMode.STEREO)
                val session = createSpatialSession(scenario)
                val initial =
                    awaitTrackingStereoSnapshotOrSkip(
                        session,
                        "ArDevice did not reach TrackingState.TRACKING before rotation",
                    )
                checkValidStereoSnapshot(initial)

                val rotated = awaitRotatedStereoSnapshotOrSkip(session, initial.devicePose.rotation)

                assertValidFieldOfViewAndPose(rotated.leftState)
                assertValidFieldOfViewAndPose(rotated.rightState)
                assertPoseEquals(
                    rotated.leftState.localPose,
                    initial.leftState.localPose,
                    translationEpsilon = STRICT_POSE_EPSILON_METERS,
                    rotationDotEpsilon = STRICT_QUATERNION_DOT_EPSILON,
                )
                assertPoseEquals(
                    rotated.rightState.localPose,
                    initial.rightState.localPose,
                    translationEpsilon = STRICT_POSE_EPSILON_METERS,
                    rotationDotEpsilon = STRICT_QUATERNION_DOT_EPSILON,
                )
                assertFieldOfViewEquals(
                    rotated.leftState.fieldOfView,
                    initial.leftState.fieldOfView,
                )
                assertFieldOfViewEquals(
                    rotated.rightState.fieldOfView,
                    initial.rightState.fieldOfView,
                )
                // Guards RenderViewpoint.update() world-pose composition under rotated devicePose.
                assertPoseEquals(
                    rotated.leftState.pose,
                    rotated.devicePose.compose(rotated.leftState.localPose),
                    translationEpsilon = STRICT_POSE_EPSILON_METERS,
                    rotationDotEpsilon = STRICT_QUATERNION_DOT_EPSILON,
                )
                assertPoseEquals(
                    rotated.rightState.pose,
                    rotated.devicePose.compose(rotated.rightState.localPose),
                    translationEpsilon = STRICT_POSE_EPSILON_METERS,
                    rotationDotEpsilon = STRICT_QUATERNION_DOT_EPSILON,
                )
                // Cross-checks RenderViewpoint.update() propagation of rotated devicePose to eye
                // world orientations while localPose remains fixed.
                assertThat(
                        Quaternion.angle(
                            initial.leftState.pose.rotation,
                            rotated.leftState.pose.rotation,
                        )
                    )
                    .isGreaterThan(MIN_HEAD_ROTATION_DEGREES - ROTATION_ANGLE_EPSILON_DEGREES)
                assertThat(
                        Quaternion.angle(
                            initial.rightState.pose.rotation,
                            rotated.rightState.pose.rotation,
                        )
                    )
                    .isGreaterThan(MIN_HEAD_ROTATION_DEGREES - ROTATION_ANGLE_EPSILON_DEGREES)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun renderViewpoint_forwardAnchorAtOneMeter_tracksInBothStereoFrustums() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                assumeRenderingModeSupported(scenario, RenderingMode.STEREO)
                val session = createSpatialSession(scenario)
                val trackingSnapshot =
                    awaitTrackingStereoSnapshotOrSkip(
                        session,
                        "ArDevice did not reach TrackingState.TRACKING before anchor",
                    )
                checkValidStereoSnapshot(trackingSnapshot)
                val forwardPose =
                    trackingSnapshot.devicePose.compose(
                        Pose(Vector3(0f, 0f, -FORWARD_ANCHOR_DISTANCE_METERS))
                    )

                val observation = createAndObserveForwardAnchor(session, forwardPose)

                assertIs<AnchorCreateSuccess>(observation.anchorResult)
                val anchorState =
                    assertNotNull(
                        observation.settledState,
                        "Timed out waiting for session updates after anchor creation",
                    )
                assertThat(anchorState.trackingState).isEqualTo(TrackingState.TRACKING)
                assertFiniteQuaternion(anchorState.pose.rotation)
                assertPoseEquals(anchorState.pose, forwardPose)
                assertThat(observation.inLeftView).isTrue()
                assertThat(observation.inRightView).isTrue()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun renderViewpoint_monoWhenUnsupported_throwsIllegalStateException() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                assumeRenderingModeUnsupported(scenario, RenderingMode.MONO)
                val session = createSpatialSession(scenario)

                // OpenXrPerceptionManager and ArCorePerceptionManager leave monoRenderViewpoint
                // null; assert the intended IllegalStateException runtime contract while XrDevice
                // and RenderViewpoint.mono KDoc omit @throws.
                assertFailsWith<IllegalStateException>(
                    "Expected RenderViewpoint.mono(session) to throw IllegalStateException when " +
                        "MONO rendering is unsupported (intended per b/469451362; KDoc @throws " +
                        "fix: b/584782784)"
                ) {
                    RenderViewpoint.mono(session)
                }
            }
        }
    }

    private companion object {
        const val HALF_PI = (PI / 2.0).toFloat()
        val PERCEPTION_TIMEOUT = 5.seconds
        val ANCHOR_SETTLE_DURATION = 500.milliseconds
        const val FRUSTUM_TEST_DEPTH_METERS = 0.5f
        const val TEST_PANEL_OFFSET_METERS = 1.0f
        const val FORWARD_ANCHOR_DISTANCE_METERS = 1.0f
        const val MIN_IN_VIEW_DEPTH_METERS = 0.001f
        const val STRADDLE_EDGE_INSET_METERS = 0.01f
        const val STRADDLING_PANEL_WIDTH_METERS = 0.04f
        const val NOMINAL_IPD_METERS = 0.065f
        const val IPD_TOLERANCE_METERS = 0.015f
        const val VIEW_SYMMETRY_TOLERANCE_METERS = 0.005f
        const val FOV_SYMMETRY_TOLERANCE_RAD = 0.05f
        const val FOV_EPSILON_RAD = 1e-4f
        const val MIN_HEAD_ROTATION_DEGREES = 2.0f
        const val ROTATION_ANGLE_EPSILON_DEGREES = 0.05f
        const val QUATERNION_DOT_EPSILON = 1e-4f
        const val STRICT_QUATERNION_DOT_EPSILON = 1e-6f
        const val POSE_EPSILON_METERS = 1e-2f
        const val STRICT_POSE_EPSILON_METERS = 1e-4f
        val TEST_PANEL_SIZE = FloatSize2d(0.004f, 0.004f)
        val ANCHOR_MARKER_SIZE = FloatSize2d(0.02f, 0.02f)
    }
}
