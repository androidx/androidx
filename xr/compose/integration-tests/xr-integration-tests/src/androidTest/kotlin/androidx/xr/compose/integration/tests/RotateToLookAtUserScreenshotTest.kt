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

package androidx.xr.compose.integration.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.arcore.ArDevice
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.ExperimentalRotateToLookAtUserApi
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.gravityAligned
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.rotateToLookAtUser
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.DEFAULT_XR_MSSIM_THRESHOLD
import androidx.xr.testutils.FakeHeadTrackingRule
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrScreenshotTestRule
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Visual screenshot tests for [SubspaceModifier.rotateToLookAtUser] verifying rendered 3D
 * compositor output against golden images via [XrScreenshotTestRule].
 */
@OptIn(ExperimentalRotateToLookAtUserApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class RotateToLookAtUserScreenshotTest {

    private val sessionRule = createXrSessionRule()

    @get:Rule(order = 0) val headTrackingRule = FakeHeadTrackingRule()

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*filterSupportedPermissions("android.permission.HEAD_TRACKING"))

    @get:Rule(order = 2)
    val composeTestRule =
        AndroidComposeTestRule(
            activityRule = sessionRule,
            activityProvider = { it.activity },
        )

    @get:Rule(order = 3) val screenshotRule = XrScreenshotTestRule(GOLDEN_PATH)

    @Before
    fun setUp() {
        sessionRule.onMainThread {
            val configureResult =
                session.configure(
                    Config.Builder(session.config)
                        .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                        .build()
                )
            assertThat(configureResult).isInstanceOf(SessionConfigureSuccess::class.java)
        }
    }

    @Test
    @XrDeviceTest
    fun rotateToLookAtUser_standardAndBillboard_renderFacingUser() {
        composeTestRule.setXrScreenshotContent {
            SpatialBox {
                LookAtPanel(
                    tag = TAG_STANDARD,
                    label = "LookAt",
                    subtitle = "Yaw + Pitch",
                    width = 210.dp,
                    height = 210.dp,
                    color = Color.Blue,
                    modifier =
                        SubspaceModifier.offset(x = (-190).dp, y = 0.dp, z = 190.dp)
                            .rotateToLookAtUser(),
                )
                LookAtPanel(
                    tag = TAG_BILLBOARD,
                    label = "Billboard",
                    subtitle = "Yaw Only",
                    width = 210.dp,
                    height = 210.dp,
                    color = Color.Green,
                    modifier =
                        SubspaceModifier.offset(x = 0.dp, y = 0.dp, z = 0.dp)
                            .rotateToLookAtUser()
                            .gravityAligned(),
                )
                LookAtPanel(
                    tag = TAG_STATIC,
                    label = "Static",
                    subtitle = "Fixed +Z",
                    width = 210.dp,
                    height = 210.dp,
                    color = Color.Red,
                    modifier = SubspaceModifier.offset(x = 190.dp, y = 0.dp, z = (-190).dp),
                )
            }
        }

        // Move the simulated head to an elevated, off-axis 3D vantage point (~45° yaw, ~42° pitch
        // relative to the Subspace origin at z = -1.75m) and gaze back at the Subspace center so
        // that:
        // - "LookAt" rotates in both yaw and pitch to face the camera squarely (square border,
        //   circular target ring),
        // - "Billboard" rotates in yaw to face the camera horizontally while staying plumb with
        //   gravity (visibly foreshortened in pitch into a shorter rectangle / horizontal oval),
        // - "Static" remains fixed facing world +Z (visibly skewed in both yaw and pitch).
        headTrackingRule.performHeadMovement {
            moveHeadTo(HEAD_OFFSET_POSITION)
            lookAt(SUBSPACE_CENTER_IN_WORLD)
        }
        composeTestRule.waitUntil(
            conditionDescription = "Expected ArDevice head pose to reach $HEAD_OFFSET_POSITION",
            timeoutMillis = WAIT_TIMEOUT_MS,
        ) {
            val headPos =
                ArDevice.getInstance(sessionRule.session).state.value.devicePose.translation
            Vector3.distance(headPos, HEAD_OFFSET_POSITION) <= HEAD_POSITION_TOLERANCE_METERS
        }

        // Wait for the asynchronous rotateToLookAtUser frame callback to finish updating each
        // entity's SceneCore pose in ActivitySpace before capturing the compositor frame.
        waitForLookAtOrientation(TAG_STANDARD) { directionToUser ->
            Quaternion.fromLookTowards(directionToUser, Vector3.Up)
        }
        waitForLookAtOrientation(TAG_BILLBOARD) { directionToUser ->
            val horizontalDir = Vector3(directionToUser.x, 0f, directionToUser.z).toNormalized()
            Quaternion.fromLookTowards(horizontalDir, Vector3.Up)
        }

        sessionRule
            .captureToBitmap(padding = 16, autoCrop = true)
            .assertAgainstGolden(
                screenshotRule,
                "compose_rotate_to_look_at_user_standard_and_billboard",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Test
    @XrDeviceTest
    fun rotateToLookAtUser_underRolledParent_remainsUprightFacingUser() {
        composeTestRule.setXrScreenshotContent {
            SpatialBox {
                SpatialBox(modifier = SubspaceModifier.rotate(pitch = 35f, yaw = 45f, roll = 30f)) {
                    LookAtPanel(
                        tag = TAG_ROLLED_CHILD_LOOKING,
                        label = "Child LookAt",
                        subtitle = "Upright Facing",
                        width = 220.dp,
                        height = 220.dp,
                        color = Color.Blue,
                        modifier =
                            SubspaceModifier.offset(x = (-150).dp, y = 110.dp).rotateToLookAtUser(),
                    )
                    LookAtPanel(
                        tag = TAG_ROLLED_CHILD_STATIC,
                        label = "Child Static",
                        subtitle = "Parent Tilted",
                        width = 220.dp,
                        height = 220.dp,
                        color = Color.Red,
                        modifier = SubspaceModifier.offset(x = 150.dp, y = (-110).dp),
                    )
                }
            }
        }

        // Wait for the asynchronous rotateToLookAtUser frame callback to finish updating the
        // entity's SceneCore pose in ActivitySpace before capturing the compositor frame.
        waitForLookAtOrientation(TAG_ROLLED_CHILD_LOOKING) { directionToUser ->
            Quaternion.fromLookTowards(directionToUser, Vector3.Up)
        }

        sessionRule
            .captureToBitmap(padding = 16, autoCrop = true)
            .assertAgainstGolden(
                screenshotRule,
                "compose_rotate_to_look_at_user_under_rolled_parent",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    private fun waitForLookAtOrientation(
        tag: String,
        expectedRotationFor: (directionToUser: Vector3) -> Quaternion,
    ) {
        composeTestRule.waitUntil(
            conditionDescription = "Expected node '$tag' to finish rotating toward head pose",
            timeoutMillis = WAIT_TIMEOUT_MS,
        ) {
            val pose = getActivityPose(tag) ?: return@waitUntil false
            val headPos = getHeadPoseInActivity().translation
            val delta = headPos - pose.translation
            if (delta.lengthSquared < 1e-6f) return@waitUntil false
            val expected = expectedRotationFor(delta.toNormalized())
            // Unit quaternions q and -q represent the same 3D rotation; canonicalize sign first.
            val signedExpected = if (pose.rotation.dot(expected) < 0f) -expected else expected
            abs(pose.rotation.x - signedExpected.x) <= ROTATION_TOLERANCE &&
                abs(pose.rotation.y - signedExpected.y) <= ROTATION_TOLERANCE &&
                abs(pose.rotation.z - signedExpected.z) <= ROTATION_TOLERANCE &&
                abs(pose.rotation.w - signedExpected.w) <= ROTATION_TOLERANCE
        }
    }

    private fun getActivityPose(tag: String): Pose? = runCatching {
        composeTestRule
            .onSubspaceNodeWithTag(tag)
            .fetchSemanticsNode()
            .semanticsEntity
            ?.getPose(Space.ACTIVITY)
    }
        .getOrNull()

    private fun getHeadPoseInActivity(): Pose {
        val session = sessionRule.session
        val headPoseInPerception = ArDevice.getInstance(session).state.value.devicePose
        return session.scene.perceptionSpace.transformPoseTo(
            headPoseInPerception,
            session.scene.activitySpace,
        )
    }

    @Composable
    @SubspaceComposable
    private fun LookAtPanel(
        tag: String,
        label: String,
        subtitle: String,
        width: Dp,
        height: Dp,
        color: Color,
        textColor: Color = if (color == Color.Green) Color.Black else Color.White,
        modifier: SubspaceModifier = SubspaceModifier,
    ) {
        SpatialPanel(modifier = modifier.width(width).height(height).testTag(tag)) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(color)
                        .border(width = 6.dp, color = Color.White)
                        .padding(10.dp)
            ) {
                Box(
                    modifier =
                        Modifier.size(136.dp)
                            .align(Alignment.Center)
                            .border(
                                width = 4.dp,
                                color = textColor.copy(alpha = 0.7f),
                                shape = CircleShape,
                            )
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "▲ TOP ▲",
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = subtitle,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    private companion object {
        private const val WAIT_TIMEOUT_MS = 10_000L
        private const val ROTATION_TOLERANCE = 0.03f
        private const val HEAD_POSITION_TOLERANCE_METERS = 0.05f

        private val SUBSPACE_CENTER_IN_WORLD = Vector3(0f, 0f, -1.75f)
        private val HEAD_OFFSET_POSITION = Vector3(0.95f, 1.15f, -0.80f)

        private const val TAG_STANDARD = "screenshot_standard"
        private const val TAG_BILLBOARD = "screenshot_billboard"
        private const val TAG_STATIC = "screenshot_static"
        private const val TAG_ROLLED_CHILD_LOOKING = "screenshot_rolled_child_looking"
        private const val TAG_ROLLED_CHILD_STATIC = "screenshot_rolled_child_static"
    }
}
