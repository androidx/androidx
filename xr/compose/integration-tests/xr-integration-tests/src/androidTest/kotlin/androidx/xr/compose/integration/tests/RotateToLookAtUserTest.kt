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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.ArDevice
import androidx.xr.compose.platform.requestFullSpace
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.layout.ExperimentalRotateToLookAtUserApi
import androidx.xr.compose.subspace.layout.MovePolicy
import androidx.xr.compose.subspace.layout.PitchLimits
import androidx.xr.compose.subspace.layout.SpatialMoveEventType
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.gravityAligned
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.rotateToLookAtUser
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.assertRotationIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.toDegrees
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.performSpatialInteraction
import androidx.xr.testutils.resetSpatialInteraction
import androidx.xr.testutils.waitForSpatialScene
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.asin
import kotlin.test.assertNotNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end instrumentation tests for [SubspaceModifier.rotateToLookAtUser] covering core spatial
 * composables, billboard mode, yaw/pitch/clamping constraints, nested parent transforms, movable
 * drag integration, and dynamic enable/disable reset.
 */
@OptIn(ExperimentalRotateToLookAtUserApi::class)
@RunWith(AndroidJUnit4::class)
@XrDeviceTest
@LargeTest
class RotateToLookAtUserTest {

    private val sessionRule = createXrSessionRule()

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*filterSupportedPermissions("android.permission.HEAD_TRACKING"))

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRule(
            activityRule = sessionRule,
            activityProvider = { it.activity },
        )

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
            activity.requestFullSpace()
        }
    }

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    fun coreComposables_rotateToLookAtUser_faceHeadPose() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(240.dp)
                            .height(180.dp)
                            .offset(x = (-450).dp, y = 250.dp, z = (-200).dp)
                            .rotateToLookAtUser()
                            .testTag(TAG_PANEL)
                ) {
                    TestPanelSurface("Panel")
                }

                SpatialRow(
                    modifier =
                        SubspaceModifier.offset(x = 450.dp, y = 250.dp, z = (-200).dp)
                            .rotateToLookAtUser()
                            .testTag(TAG_ROW)
                ) {
                    SpatialPanel(modifier = SubspaceModifier.width(200.dp).height(160.dp)) {
                        TestPanelSurface("Row")
                    }
                }

                SpatialColumn(
                    modifier =
                        SubspaceModifier.offset(x = (-450).dp, y = (-250).dp, z = (-200).dp)
                            .rotateToLookAtUser()
                            .testTag(TAG_COLUMN)
                ) {
                    SpatialPanel(modifier = SubspaceModifier.width(200.dp).height(160.dp)) {
                        TestPanelSurface("Column")
                    }
                }

                SpatialExternalSurface(
                    stereoMode = StereoMode.Mono,
                    modifier =
                        SubspaceModifier.width(240.dp)
                            .height(180.dp)
                            .offset(x = 450.dp, y = (-250).dp, z = (-200).dp)
                            .rotateToLookAtUser()
                            .testTag(TAG_EXTERNAL_SURFACE),
                ) {}
            }
        }

        waitForNodeToFaceUser(TAG_PANEL)
        waitForNodeToFaceUser(TAG_ROW)
        waitForNodeToFaceUser(TAG_COLUMN)
        waitForNodeToFaceUser(TAG_EXTERNAL_SURFACE)
    }

    @Test
    fun billboardMode_rotateToLookAtUserWithGravityAligned_tracksYawWhileStayingUpright() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(240.dp)
                            .height(180.dp)
                            .offset(x = 600.dp, y = 600.dp, z = (-250).dp)
                            .rotateToLookAtUser()
                            .gravityAligned()
                            .testTag(TAG_BILLBOARD)
                ) {
                    TestPanelSurface("Billboard")
                }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(240.dp)
                            .height(180.dp)
                            .offset(x = (-600).dp, y = 600.dp, z = (-250).dp)
                            .rotateToLookAtUser()
                            .testTag(TAG_REFERENCE_PANEL)
                ) {
                    TestPanelSurface("Standard")
                }
            }
        }

        waitForNodeToFaceUser(TAG_REFERENCE_PANEL)
        composeTestRule.waitUntil(
            conditionDescription = "Expected billboard panel to rotate horizontally toward user",
            timeoutMillis = WAIT_TIMEOUT_MS,
        ) {
            val billboardPose = getActivityPose(TAG_BILLBOARD) ?: return@waitUntil false
            val headPos = getHeadPoseInActivity().translation
            val horizontalDirToUser = horizontalVector(headPos - billboardPose.translation)
            val horizontalForward = horizontalVector(billboardPose.rotation * Vector3.Backward)
            Vector3.angleBetween(horizontalForward, horizontalDirToUser) < ANGLE_TOLERANCE_DEG
        }

        val billboardPose = assertNotNull(getActivityPose(TAG_BILLBOARD))
        val headPos = getHeadPoseInActivity().translation
        val horizontalDirToUser = horizontalVector(headPos - billboardPose.translation)
        val expectedBillboardWorldRotation =
            Quaternion.fromLookTowards(horizontalDirToUser, Vector3.Up)
        assertQuaternionsClose(
            actual = billboardPose.rotation,
            expected = expectedBillboardWorldRotation,
            tolerance = ROTATION_TOLERANCE,
        )

        val billboardForward = billboardPose.rotation * Vector3.Backward
        val billboardUp = billboardPose.rotation * Vector3.Up
        assertThat(abs(billboardForward.y)).isLessThan(LEVEL_COMPONENT_TOLERANCE)
        assertThat(Vector3.angleBetween(billboardUp, Vector3.Up)).isLessThan(ANGLE_TOLERANCE_DEG)

        val standardPose = assertNotNull(getActivityPose(TAG_REFERENCE_PANEL))
        val standardForward = standardPose.rotation * Vector3.Backward
        assertThat(abs(standardForward.y)).isGreaterThan(MIN_TILT_COMPONENT)
    }

    @Test
    fun rotationConstraints_yawDisabledPitchDisabledAndPitchLimits_constrainOrientation() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(220.dp)
                            .height(160.dp)
                            .offset(x = 600.dp, y = 2000.dp, z = (-200).dp)
                            .rotateToLookAtUser()
                            .testTag(TAG_REFERENCE_PANEL)
                ) {
                    TestPanelSurface("Unconstrained")
                }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(220.dp)
                            .height(160.dp)
                            .offset(x = 600.dp, y = 2000.dp, z = (-200).dp)
                            .rotateToLookAtUser(
                                isYawUpdateEnabled = false,
                                isPitchUpdateEnabled = true,
                            )
                            .testTag(TAG_YAW_DISABLED)
                ) {
                    TestPanelSurface("Yaw Off")
                }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(220.dp)
                            .height(160.dp)
                            .offset(x = 600.dp, y = 2000.dp, z = (-200).dp)
                            .rotateToLookAtUser(
                                isYawUpdateEnabled = true,
                                isPitchUpdateEnabled = false,
                            )
                            .testTag(TAG_PITCH_DISABLED)
                ) {
                    TestPanelSurface("Pitch Off")
                }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(220.dp)
                            .height(160.dp)
                            .offset(x = 600.dp, y = 2000.dp, z = (-200).dp)
                            .rotateToLookAtUser(
                                isYawUpdateEnabled = false,
                                isPitchUpdateEnabled = false,
                            )
                            .testTag(TAG_BOTH_DISABLED)
                ) {
                    TestPanelSurface("Both Off")
                }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(220.dp)
                            .height(160.dp)
                            .offset(x = 600.dp, y = 2000.dp, z = (-200).dp)
                            .rotateToLookAtUser(pitchLimits = PitchLimits(-15f, 15f))
                            .testTag(TAG_PITCH_CLAMPED)
                ) {
                    TestPanelSurface("Clamped")
                }
            }
        }

        waitForNodeToFaceUser(TAG_REFERENCE_PANEL)
        composeTestRule.waitUntil(
            conditionDescription =
                "Expected constrained panels to finish initial look-at placement",
            timeoutMillis = WAIT_TIMEOUT_MS,
        ) {
            val yawDisabledForward =
                getActivityPose(TAG_YAW_DISABLED)?.rotation?.times(Vector3.Backward)
                    ?: return@waitUntil false
            val pitchDisabledForward =
                getActivityPose(TAG_PITCH_DISABLED)?.rotation?.times(Vector3.Backward)
                    ?: return@waitUntil false
            abs(yawDisabledForward.y) > MIN_TILT_COMPONENT &&
                abs(pitchDisabledForward.x) > MIN_TILT_COMPONENT
        }

        // Verify that the unconstrained reference panel pitches by > 20 degrees from this elevated
        // head position; this confirms the test vantage point is steep enough that
        // TAG_PITCH_CLAMPED (below) is actively clamped by its [-15, 15] degree PitchLimits.
        val referencePose = assertNotNull(getActivityPose(TAG_REFERENCE_PANEL))
        val referencePitchDeg = abs(computePitchDegrees(referencePose.rotation * Vector3.Backward))
        assertThat(referencePitchDeg).isGreaterThan(20f)

        // Yaw disabled: horizontal X component of forward normal stays 0 while vertical Y pitches.
        val yawDisabledPose = assertNotNull(getActivityPose(TAG_YAW_DISABLED))
        val yawDisabledForward = yawDisabledPose.rotation * Vector3.Backward
        assertThat(abs(yawDisabledForward.x)).isLessThan(LEVEL_COMPONENT_TOLERANCE)
        assertThat(abs(yawDisabledForward.y)).isGreaterThan(MIN_TILT_COMPONENT)

        // Pitch disabled: vertical Y component of forward normal stays 0 while horizontal X yaws.
        val pitchDisabledPose = assertNotNull(getActivityPose(TAG_PITCH_DISABLED))
        val pitchDisabledForward = pitchDisabledPose.rotation * Vector3.Backward
        assertThat(abs(pitchDisabledForward.y)).isLessThan(LEVEL_COMPONENT_TOLERANCE)
        assertThat(abs(pitchDisabledForward.x)).isGreaterThan(MIN_TILT_COMPONENT)

        // Both disabled: orientation remains untouched at identity.
        composeTestRule
            .onSubspaceNodeWithTag(TAG_BOTH_DISABLED)
            .assertRotationInRootIsEqualTo(Quaternion.Identity, tolerance = ROTATION_TOLERANCE)

        // Pitch limits [-15, 15]: pitch is clamped at 15 degrees while yaw still tracks user.
        val clampedPose = assertNotNull(getActivityPose(TAG_PITCH_CLAMPED))
        val clampedForward = clampedPose.rotation * Vector3.Backward
        val clampedPitchDeg = abs(computePitchDegrees(clampedForward))
        assertThat(clampedPitchDeg).isWithin(ANGLE_TOLERANCE_DEG).of(15f)
        val headPos = getHeadPoseInActivity().translation
        assertThat(
                Vector3.angleBetween(
                    horizontalVector(clampedForward),
                    horizontalVector(headPos - clampedPose.translation),
                )
            )
            .isLessThan(ANGLE_TOLERANCE_DEG)
    }

    @Test
    fun nestedParentsAndLargeOffset_childFacesUserIndependentlyAndBehindUserPanelRotates() {
        composeTestRule.setContent {
            Subspace {
                SpatialBox(
                    modifier =
                        SubspaceModifier.offset(x = (-400).dp, y = 200.dp, z = (-200).dp)
                            .rotate(roll = 25f)
                            .testTag(TAG_ROTATED_PARENT)
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .rotateToLookAtUser()
                                .testTag(TAG_ROTATED_PARENT_CHILD_LOOKING)
                    ) {
                        TestPanelSurface("Rotated Parent Child Looking")
                    }

                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .offset(y = (-220).dp)
                                .testTag(TAG_ROTATED_PARENT_CHILD_STATIC)
                    ) {
                        TestPanelSurface("Rotated Parent Child Static")
                    }
                }

                SpatialBox(
                    modifier =
                        SubspaceModifier.offset(x = 400.dp, y = 200.dp, z = (-200).dp)
                            .testTag(TAG_OFFSET_PARENT)
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .offset(x = 150.dp)
                                .rotateToLookAtUser()
                                .testTag(TAG_OFFSET_PARENT_CHILD_LOOKING)
                    ) {
                        TestPanelSurface("Offset Parent Child Looking")
                    }
                }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(280.dp)
                            .height(200.dp)
                            .offset(x = 1000.dp, y = 500.dp, z = 5000.dp)
                            .rotateToLookAtUser()
                            .testTag(TAG_BEHIND_USER_PANEL)
                ) {
                    TestPanelSurface("Behind User")
                }
            }
        }

        // Rotated parent: only the child with rotateToLookAtUser faces the user; static sibling
        // keeps identity relative rotation (inheriting parent roll).
        waitForNodeToFaceUser(TAG_ROTATED_PARENT_CHILD_LOOKING)
        composeTestRule
            .onSubspaceNodeWithTag(TAG_ROTATED_PARENT_CHILD_STATIC)
            .assertRotationIsEqualTo(Quaternion.Identity, tolerance = ROTATION_TOLERANCE)

        // Offset parent: parent remains unrotated while child rotates to face user from its
        // composed world position.
        waitForNodeToFaceUser(TAG_OFFSET_PARENT_CHILD_LOOKING)
        composeTestRule
            .onSubspaceNodeWithTag(TAG_OFFSET_PARENT)
            .assertRotationInRootIsEqualTo(Quaternion.Identity, tolerance = ROTATION_TOLERANCE)

        // Behind-user panel with large positive Z offset rotates ~180 degrees to face user.
        waitForNodeToFaceUser(TAG_BEHIND_USER_PANEL)
        val behindPose = assertNotNull(getActivityPose(TAG_BEHIND_USER_PANEL))
        val behindForward = behindPose.rotation * Vector3.Backward
        assertThat(behindForward.z).isLessThan(-0.5f)
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun movableSystemPanel_afterDrag_reorientsToFaceHeadPose() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(400.dp)
                            .height(300.dp)
                            .offset(x = (-350).dp, y = 0.dp, z = (-200).dp)
                            .movable()
                            .rotateToLookAtUser()
                            .testTag(TAG_MOVABLE_SYSTEM)
                ) {
                    TestPanelSurface("System Movable")
                }
            }
        }

        waitForNodeToFaceUser(TAG_MOVABLE_SYSTEM)

        val initialActivityPose = assertNotNull(getActivityPose(TAG_MOVABLE_SYSTEM))
        val initialRootRotation =
            composeTestRule
                .onSubspaceNodeWithTag(TAG_MOVABLE_SYSTEM)
                .fetchSemanticsNode()
                .poseInRoot
                .rotation

        val panelCpmNode = waitForMovablePanelCpmNode()
        panelCpmNode.performSpatialInteraction { moveNodeBy(Vector3(0.55f, 0.25f, 0f)) }

        composeTestRule.waitUntil(
            conditionDescription =
                "Expected movable system panel to translate and re-orient toward user",
            timeoutMillis = WAIT_TIMEOUT_MS,
        ) {
            val updatedPose = getActivityPose(TAG_MOVABLE_SYSTEM) ?: return@waitUntil false
            val movedDistance = (updatedPose.translation - initialActivityPose.translation).length
            movedDistance > 0.1f && isFacingUser(updatedPose)
        }

        val updatedRootRotation =
            composeTestRule
                .onSubspaceNodeWithTag(TAG_MOVABLE_SYSTEM)
                .fetchSemanticsNode()
                .poseInRoot
                .rotation
        assertThat(updatedRootRotation).isNotEqualTo(initialRootRotation)
    }

    @Test
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun movableCustomPanel_afterDrag_reorientsToFaceHeadPose() {
        var moveEventCount by mutableIntStateOf(0)

        composeTestRule.setContent {
            Subspace {
                val density = LocalDensity.current
                var offsetX: Dp by remember { mutableStateOf((-350).dp) }
                var offsetY: Dp by remember { mutableStateOf(0.dp) }
                var offsetZ: Dp by remember { mutableStateOf((-200).dp) }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(400.dp)
                            .height(300.dp)
                            .offset(x = offsetX, y = offsetY, z = offsetZ)
                            .movable(
                                movePolicy =
                                    MovePolicy.custom(
                                        onMove = { event ->
                                            if (
                                                event.type == SpatialMoveEventType.Moving ||
                                                    event.type == SpatialMoveEventType.End
                                            ) {
                                                moveEventCount++
                                                val deltaPx =
                                                    event.pose.translation -
                                                        event.previousPose.translation
                                                with(density) {
                                                    offsetX += deltaPx.x.toDp()
                                                    offsetY += deltaPx.y.toDp()
                                                    offsetZ += deltaPx.z.toDp()
                                                }
                                            }
                                        }
                                    )
                            )
                            .rotateToLookAtUser()
                            .testTag(TAG_MOVABLE_CUSTOM)
                ) {
                    TestPanelSurface("Custom Movable")
                }
            }
        }

        waitForNodeToFaceUser(TAG_MOVABLE_CUSTOM)

        val initialActivityPose = assertNotNull(getActivityPose(TAG_MOVABLE_CUSTOM))
        val panelCpmNode = waitForMovablePanelCpmNode()
        panelCpmNode.performSpatialInteraction { moveNodeBy(Vector3(0.55f, 0.25f, 0f)) }

        composeTestRule.waitUntil(
            conditionDescription =
                "Expected custom movable panel to receive move events, translate, and face user",
            timeoutMillis = WAIT_TIMEOUT_MS,
        ) {
            val updatedPose = getActivityPose(TAG_MOVABLE_CUSTOM) ?: return@waitUntil false
            val movedDistance = (updatedPose.translation - initialActivityPose.translation).length
            moveEventCount > 0 && movedDistance > 0.1f && isFacingUser(updatedPose)
        }

        assertThat(moveEventCount).isGreaterThan(0)
    }

    @Test
    fun dynamicToggle_disablingRotateToLookAtUser_resetsToOriginalOrientation() {
        var isLookAtEnabled by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                val lookAtModifier =
                    if (isLookAtEnabled) {
                        SubspaceModifier.rotateToLookAtUser()
                    } else {
                        SubspaceModifier
                    }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(300.dp)
                            .height(220.dp)
                            .offset(x = 550.dp, y = 350.dp, z = (-200).dp)
                            .then(lookAtModifier)
                            .testTag(TAG_TOGGLE_PANEL)
                ) {
                    TestPanelSurface("Toggle Panel")
                }
            }
        }

        waitForNodeToFaceUser(TAG_TOGGLE_PANEL)

        isLookAtEnabled = false
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(
            conditionDescription = "Expected panel rotation to reset to Identity when disabled",
            timeoutMillis = WAIT_TIMEOUT_MS,
        ) {
            val currentRotation =
                composeTestRule
                    .onSubspaceNodeWithTag(TAG_TOGGLE_PANEL)
                    .fetchSemanticsNode()
                    .poseInRoot
                    .rotation
            Vector3.angleBetween(currentRotation * Vector3.Backward, Vector3.Backward) <
                ANGLE_TOLERANCE_DEG
        }
        composeTestRule
            .onSubspaceNodeWithTag(TAG_TOGGLE_PANEL)
            .assertRotationInRootIsEqualTo(Quaternion.Identity, tolerance = ROTATION_TOLERANCE)

        isLookAtEnabled = true
        composeTestRule.waitForIdle()
        waitForNodeToFaceUser(TAG_TOGGLE_PANEL)
    }

    private fun waitForMovablePanelCpmNode(): SpatialNode =
        checkNotNull(
            waitForSpatialScene(
                description = "movable embedded panel with bounds ready for interaction",
                select = { scene ->
                    scene
                        .findTaskByActivity(sessionRule.activity)
                        ?.embeddedWindowLeashes
                        ?.firstOrNull { it.isVisible && it.isMovable && it.worldExtents != null }
                },
                until = { it != null },
            )
        )

    private fun waitForNodeToFaceUser(tag: String) {
        var lastDiagnostic = "node not found"
        try {
            composeTestRule.waitUntil(
                conditionDescription =
                    "Expected node '$tag' to rotate to face the user's head pose",
                timeoutMillis = WAIT_TIMEOUT_MS,
            ) {
                val pose = getActivityPose(tag)
                if (pose == null) {
                    lastDiagnostic = "pose=null (headPos=${getHeadPoseInActivity().translation})"
                    return@waitUntil false
                }
                val headPos = getHeadPoseInActivity().translation
                lastDiagnostic = "headPos=$headPos pose=$pose"
                isFacingUser(pose)
            }
        } catch (e: Throwable) {
            throw AssertionError("${e.message} [$lastDiagnostic]", e)
        }
    }

    private fun isFacingUser(entityPoseInActivity: Pose): Boolean {
        val headPos = getHeadPoseInActivity().translation
        val directionToUser = (headPos - entityPoseInActivity.translation).toNormalized()
        val expectedWorldRotation = Quaternion.fromLookTowards(directionToUser, Vector3.Up)
        return areQuaternionsClose(
            actual = entityPoseInActivity.rotation,
            expected = expectedWorldRotation,
            tolerance = ROTATION_TOLERANCE,
        )
    }

    private fun areQuaternionsClose(
        actual: Quaternion,
        expected: Quaternion,
        tolerance: Float,
    ): Boolean {
        // Unit quaternions q and -q represent the exact same 3D rotation (antipodal double cover
        // on S^3). A negative dot product means `actual` and `expected` lie in opposite
        // hemispheres, so we negate `expected` before comparing component-wise deltas.
        val signedExpected = if (actual.dot(expected) < 0f) -expected else expected
        return abs(actual.x - signedExpected.x) <= tolerance &&
            abs(actual.y - signedExpected.y) <= tolerance &&
            abs(actual.z - signedExpected.z) <= tolerance &&
            abs(actual.w - signedExpected.w) <= tolerance
    }

    private fun assertQuaternionsClose(
        actual: Quaternion,
        expected: Quaternion,
        tolerance: Float,
    ) {
        // Canonicalize antipodal quaternions (q and -q represent the same 3D orientation).
        val signedExpected = if (actual.dot(expected) < 0f) -expected else expected
        assertThat(actual.x).isWithin(tolerance).of(signedExpected.x)
        assertThat(actual.y).isWithin(tolerance).of(signedExpected.y)
        assertThat(actual.z).isWithin(tolerance).of(signedExpected.z)
        assertThat(actual.w).isWithin(tolerance).of(signedExpected.w)
    }

    private fun getActivityPose(tag: String): Pose? {
        return runCatching {
            composeTestRule
                .onSubspaceNodeWithTag(tag)
                .fetchSemanticsNode()
                .semanticsEntity
                ?.getPose(Space.ACTIVITY)
        }
            .getOrNull()
    }

    private fun getHeadPoseInActivity(): Pose {
        val session = sessionRule.session
        val headPoseInPerception = ArDevice.getInstance(session).state.value.devicePose
        return session.scene.perceptionSpace.transformPoseTo(
            headPoseInPerception,
            session.scene.activitySpace,
        )
    }

    private fun horizontalVector(vector: Vector3): Vector3 =
        Vector3(vector.x, 0f, vector.z).toNormalized()

    private fun computePitchDegrees(forwardNormal: Vector3): Float =
        toDegrees(asin(forwardNormal.toNormalized().y.coerceIn(-1f, 1f)))

    @Composable
    private fun TestPanelSurface(label: String) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Blue),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, color = Color.White)
        }
    }

    companion object {
        private const val WAIT_TIMEOUT_MS = 10_000L
        private const val ROTATION_TOLERANCE = 0.03f
        private const val ANGLE_TOLERANCE_DEG = 3.0f
        private const val LEVEL_COMPONENT_TOLERANCE = 0.03f
        private const val MIN_TILT_COMPONENT = 0.1f

        private const val TAG_PANEL = "look_at_panel"
        private const val TAG_ROW = "look_at_row"
        private const val TAG_COLUMN = "look_at_column"
        private const val TAG_EXTERNAL_SURFACE = "look_at_external_surface"
        private const val TAG_BILLBOARD = "billboard_panel"
        private const val TAG_REFERENCE_PANEL = "reference_panel"
        private const val TAG_YAW_DISABLED = "yaw_disabled_panel"
        private const val TAG_PITCH_DISABLED = "pitch_disabled_panel"
        private const val TAG_BOTH_DISABLED = "both_disabled_panel"
        private const val TAG_PITCH_CLAMPED = "pitch_clamped_panel"
        private const val TAG_ROTATED_PARENT = "rotated_parent"
        private const val TAG_ROTATED_PARENT_CHILD_LOOKING = "rotated_parent_child_looking"
        private const val TAG_ROTATED_PARENT_CHILD_STATIC = "rotated_parent_child_static"
        private const val TAG_OFFSET_PARENT = "offset_parent"
        private const val TAG_OFFSET_PARENT_CHILD_LOOKING = "offset_parent_child_looking"
        private const val TAG_BEHIND_USER_PANEL = "behind_user_panel"
        private const val TAG_MOVABLE_SYSTEM = "movable_system_panel"
        private const val TAG_MOVABLE_CUSTOM = "movable_custom_panel"
        private const val TAG_TOGGLE_PANEL = "toggle_panel"
    }
}
