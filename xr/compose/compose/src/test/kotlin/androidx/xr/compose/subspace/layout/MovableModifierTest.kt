/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.xr.compose.subspace.layout

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.runtime.MoveEvent
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeActivitySpace
import androidx.xr.scenecore.testing.FakeMovableComponent
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceModifier.movable] modifier. */
@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class MovableModifierTest {

    private val testDispatcher = StandardTestDispatcher()

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @get:Rule val permissionRule = GrantPermissionRule.grant(SCENE_UNDERSTANDING_COARSE)

    // ========================================================================
    // TESTS FOR MOVE POLICY (NEW API)
    // ========================================================================

    @Test
    fun policyDefault_createsSystemMovableComponent() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").movable(movePolicy = MovePolicy.default())
                ) {
                    Text(text = "Panel")
                }
            }
        }
        assertSingleMovableComponentExist()
    }

    @Test
    fun policyDefault_scaleWithDistance_passedCorrectly() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(movePolicy = MovePolicy.default(scaleWithDistance = false))
                ) {}
            }
        }
        assertThat(scalesInZ.single()).isFalse()
    }

    @Test
    fun policyCustom_createsCustomMovableComponent() {
        val systemMovableFlags = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { systemMovable, _, _ ->
            systemMovableFlags.add(systemMovable)
        }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").movable(movePolicy = MovePolicy.custom {})
                ) {
                    Text(text = "Panel")
                }
            }
        }

        assertSingleMovableComponentExist()
        // custom movable uses MovableComponent.createCustomMovable, so systemMovable is false
        assertThat(systemMovableFlags.single()).isFalse()
    }

    @OptIn(ExperimentalMoveAnchorPolicy::class)
    @Test
    fun policyAnchor_createsAnchorableComponent() {
        val session = composeTestRule.configureFakeSession()
        session.configure(
            Config.Builder(session.config)
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .build()
        )
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").movable(movePolicy = MovePolicy.anchor())
                ) {
                    Text(text = "Panel")
                }
            }
        }
        assertSingleMovableComponentExist()
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalMoveAnchorPolicy::class)
    @Test
    fun policyAnchor_moveWithinAnchorDistanceToMatchingPlane_anchorsToPlane() =
        runTest(testDispatcher) {
            // Setup session specifically for anchor testing to ensure tracking modes are enabled
            val sessionCreateResult = Session.create(composeTestRule.activity, testDispatcher)
            assertThat(sessionCreateResult).isInstanceOf(SessionCreateSuccess::class.java)
            val session = (sessionCreateResult as SessionCreateSuccess).session
            session.configure(
                Config.Builder(session.config)
                    .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                    .build()
            )

            // TODO: b/494305963 Remove references to arcore-testing Fakes
            @Suppress("DEPRECATION")
            val perceptionRuntime =
                session.runtimes
                    .filterIsInstance<androidx.xr.arcore.testing.FakePerceptionRuntime>()
                    .single()
            @Suppress("DEPRECATION") val perceptionManager = perceptionRuntime.perceptionManager

            val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
            val activitySpace = sceneRuntime.activitySpace
            testDispatcher.scheduler.advanceUntilIdle()

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .movable(
                                    movePolicy =
                                        MovePolicy.anchor(
                                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                                        )
                                )
                    ) {
                        Text(text = "Spatial Panel")
                    }
                }
            }

            val entity =
                assertNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("panel")
                        .fetchSemanticsNode()
                        .semanticsEntity
                )
            assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(Pose.Identity)

            val planeCenterPosition = Vector3(1f, 0f, 2f)
            val planeRotation = Quaternion.fromAxisAngle(Vector3.Up, -45f)
            val planePose = Pose(planeCenterPosition, planeRotation)

            addPlaneToRuntime(
                perceptionRuntime,
                perceptionManager,
                type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                label = Plane.Label.FLOOR,
                centerPose = planePose,
                extents = FloatSize2d(5f, 5f),
            )

            assertSingleMovableComponentExist()
            assertThat(sceneRuntime.lastMovableComponent).isNotNull()
            val rtMovableComponent = sceneRuntime.lastMovableComponent!!
            val movePose = Pose(Vector3(2f, 0.01f, 3f), Quaternion.fromEulerAngles(20f, 30f, 45f))
            initiateMoveEvents(rtMovableComponent, activitySpace, movePose)

            // Translation should be the move translation projected to the plane.
            val expectedTranslation =
                Vector3(movePose.translation.x, planeCenterPosition.y, movePose.translation.z)
            // Rotation of the panel should be so that the forward direction of the panel
            // (z-axis) parallel to the normal of the plane, while projecting down the x and z
            // vectors of the move rotation to the plane.
            val planeRotationMatrix = Matrix4.fromQuaternion(planeRotation)
            val planeMatrixData = planeRotationMatrix.data
            val planeNormal =
                Vector3(planeMatrixData[4], planeMatrixData[5], planeMatrixData[6]).toNormalized()
            val movePoseRotationMatrixData = Matrix4.fromQuaternion(movePose.rotation).data
            val moveXAxis =
                Vector3(
                        movePoseRotationMatrixData[0],
                        movePoseRotationMatrixData[1],
                        movePoseRotationMatrixData[2],
                    )
                    .toNormalized()
            val expectedPanelY = planeNormal.cross(moveXAxis).toNormalized()
            val expectedPanelX = expectedPanelY.cross(planeNormal).toNormalized()
            val expectedRotation =
                getRotationMatrixFromAxes(expectedPanelX, expectedPanelY, planeNormal).rotation

            val expectedPose = Pose(expectedTranslation, expectedRotation)
            assertPose(entity.getPose(Space.ACTIVITY), expectedPose, TOLERANCE)
            assertThat(entity.parent).isInstanceOf(AnchorSpace::class.java)
        }

    // ========================================================================
    // TESTS FOR CUSTOM MOVABLE (LEGACY OVERLOAD)
    // ========================================================================

    @Test
    fun customMovable_componentIsNotNullAndIsCustomMovable() {
        val systemMovableFlags = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { systemMovable, _, _ ->
            systemMovableFlags.add(systemMovable)
        }

        composeTestRule.setContent {
            Subspace {
                val customOnMove: (SpatialMoveEvent) -> Unit = { _ -> }
                SpatialPanel(SubspaceModifier.testTag("panel").movable(onMove = customOnMove)) {
                    Text(text = "Panel")
                }
            }
        }

        assertSingleMovableComponentExist()
        // custom movable uses MovableComponent.createCustomMovable, so systemMovable should be
        // false
        assertThat(systemMovableFlags.single()).isFalse()
    }

    @Test
    fun customMovable_modifierIsDisabledAndComponentDoesNotExist() {
        composeTestRule.setContent {
            Subspace {
                val customOnMove: (SpatialMoveEvent) -> Unit = { _ -> }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = false, onMove = customOnMove)
                ) {
                    Text(text = "Panel")
                }
            }
        }
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun customMovable_scaleWithDistance_setTrue() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                val customOnMove: (SpatialMoveEvent) -> Unit = { _ -> }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .width(200.dp)
                        .movable(enabled = true, scaleWithDistance = true, onMove = customOnMove)
                ) {}
            }
        }

        assertThat(scalesInZ.single()).isTrue()
    }

    @Test
    fun customMovable_scaleWithDistance_setFalse() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                val customOnMove: (SpatialMoveEvent) -> Unit = { _ -> }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .width(200.dp)
                        .movable(enabled = true, scaleWithDistance = false, onMove = customOnMove)
                ) {}
            }
        }

        assertThat(scalesInZ.single()).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun customMovable_invokesCallbackWithCorrectPhases() {
        var moveEvent: SpatialMoveEvent? = null
        val session = composeTestRule.configureFakeSession()
        val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
        val activitySpace = sceneRuntime.activitySpace

        composeTestRule.setContent {
            Subspace {
                val customOnMove: (SpatialMoveEvent) -> Unit = { event -> moveEvent = event }
                SpatialPanel(
                    modifier =
                        SubspaceModifier.testTag("panel")
                            .movable(enabled = true, onMove = customOnMove)
                ) {
                    Text(text = "Spatial Panel")
                }
            }
        }

        assertSingleMovableComponentExist()
        val rtMovableComponent = sceneRuntime.lastMovableComponent!!
        val expectedPose =
            Pose(Vector3(1f, 2f, 3f), Quaternion.fromAxisAngle(axis = Vector3.Forward, 45f))

        // Trigger Start
        rtMovableComponent.onMoveEvent(
            MoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose.Identity,
                expectedPose,
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        composeTestRule.waitForIdle()
        assertThat(moveEvent).isNotNull()
        assertThat(moveEvent!!.type).isEqualTo(SpatialMoveEventType.Start)

        // Trigger Ongoing/Dragging
        rtMovableComponent.onMoveEvent(
            MoveEvent(
                MoveEvent.MOVE_STATE_ONGOING,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose.Identity,
                expectedPose,
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        composeTestRule.waitForIdle()
        assertThat(moveEvent!!.type).isEqualTo(SpatialMoveEventType.Moving)
    }

    // ========================================================================
    // TESTS FOR SYSTEM DEPRECATED MOVABLE
    // ========================================================================

    @Test
    fun movable_noComponentByDefault() {
        composeTestRule.setContent {
            Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) { Text(text = "Panel") } }
        }
        assertTrue(
            composeTestRule
                .onSubspaceNodeWithTag("panel")
                .fetchSemanticsNode()
                .components
                .isNullOrEmpty()
        )
    }

    @Test
    fun movable_componentIsNotNullAndOnlyContainsSingleMovable() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").movable(stickyPose = false)) {
                    Text(text = "Panel")
                }
            }
        }
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_modifierIsDisabledAndComponentDoesNotExist() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").movable(enabled = false, stickyPose = false)
                ) {
                    Text(text = "Panel")
                }
            }
        }
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun movable_modifierDoesNotChangeAndOnlyOneComponentExist() {
        composeTestRule.setContent {
            Subspace {
                var panelWidth by remember { mutableStateOf(50.dp) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .width(panelWidth)
                        .movable(enabled = true, stickyPose = false)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { panelWidth += 50.dp },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose there should still only exist one Component.
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_scaleWithDistance_setTrue() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .width(200.dp)
                        .movable(enabled = true, stickyPose = false, scaleWithDistance = true)
                ) {}
            }
        }

        assertThat(scalesInZ.single()).isTrue()
    }

    @Test
    fun movable_scaleWithDistance_setFalse() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .width(200.dp)
                        .movable(enabled = true, stickyPose = false, scaleWithDistance = false)
                ) {}
            }
        }

        assertThat(scalesInZ.single()).isFalse()
    }

    @Test
    fun movable_scaleWithDistance_scaleFlip() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                var scaleWithDistance by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .width(200.dp)
                        .movable(
                            enabled = true,
                            stickyPose = false,
                            scaleWithDistance = scaleWithDistance,
                        )
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { scaleWithDistance = !scaleWithDistance },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }

        assertThat(scalesInZ.single()).isTrue()

        composeTestRule.onNodeWithTag("button").performClick()
        composeTestRule.waitForIdle()

        assertThat(scalesInZ.size).isEqualTo(2)
        assertThat(scalesInZ[0]).isTrue()
        assertThat(scalesInZ[1]).isFalse()
    }

    @Test
    fun movable_modifierEnabledToDisabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = movableEnabled, stickyPose = false)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { movableEnabled = !movableEnabled },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose no Components should exist.
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun movable_modifierOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var onPoseReturnValue by remember { mutableStateOf(true) }
                val oldOnMove: (SpatialMoveEvent) -> Boolean = { _ -> onPoseReturnValue }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = true, stickyPose = false, onMove = oldOnMove)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { onPoseReturnValue = !onPoseReturnValue },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose there should only exist one Component, not necessarily the same as
        // before.
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_modifierDisableWithOnPoseChangeUpdateAndComponentRemoved() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(true) }
                var onPoseReturnValue by remember { mutableStateOf(true) }
                val oldOnMove: (SpatialMoveEvent) -> Boolean = { _ -> onPoseReturnValue }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = movableEnabled, stickyPose = false, onMove = oldOnMove)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = {
                            movableEnabled = !movableEnabled
                            onPoseReturnValue = !onPoseReturnValue
                        },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose Component should be removed.
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun movable_modifierEnabledWithOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(false) }
                var onPoseReturnValue by remember { mutableStateOf(true) }
                val oldOnMove: (SpatialMoveEvent) -> Boolean = { _ -> onPoseReturnValue }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = movableEnabled, stickyPose = false, onMove = oldOnMove)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = {
                            movableEnabled = !movableEnabled
                            onPoseReturnValue = !onPoseReturnValue
                        },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }
        assertMovableComponentDoesNotExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose Component should exist and be attached.
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_modifierDisabledThenEnabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = movableEnabled, stickyPose = false)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { movableEnabled = !movableEnabled },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After disabled, recompose Component should not exist.
        assertMovableComponentDoesNotExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After enabled, recompose Component should be attached.
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_modifierOnPoseChangeTwiceUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var onPoseReturnValue by remember { mutableStateOf(true) }
                val oldOnMove: (SpatialMoveEvent) -> Boolean = { _ -> onPoseReturnValue }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = true, stickyPose = false, onMove = oldOnMove)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { onPoseReturnValue = !onPoseReturnValue },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose there should only exist one Component, not necessarily the same as
        // before.
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose there should only exist one Component, not necessarily the same as
        // before.
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_modifierDisabledThenEnabledWithOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(true) }
                var onPoseReturnValue by remember { mutableStateOf(true) }
                val oldOnMove: (SpatialMoveEvent) -> Boolean = { _ -> onPoseReturnValue }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = movableEnabled, stickyPose = false, onMove = oldOnMove)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = {
                            movableEnabled = !movableEnabled
                            onPoseReturnValue = !onPoseReturnValue
                        },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After disabled, recompose removes Component.
        assertMovableComponentDoesNotExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After enabled, recompose Component should be attached. There should only exist one
        // Component,
        // not necessarily the same as before.
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_modifierEnabledThenDisabledWithOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(false) }
                var onPoseReturnValue by remember { mutableStateOf(true) }
                val oldOnMove: (SpatialMoveEvent) -> Boolean = { _ -> onPoseReturnValue }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = movableEnabled, stickyPose = false, onMove = oldOnMove)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = {
                            movableEnabled = !movableEnabled
                            onPoseReturnValue = !onPoseReturnValue
                        },
                    ) {
                        Text(text = "Sample button for testing")
                    }
                }
            }
        }
        assertMovableComponentDoesNotExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After enabled, recompose Component should be attached. There should only exist one
        // Component,
        // not necessarily the same as before.
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After disabled, recompose removes Component.
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun movable_columnEntity_noComponentByDefault() {
        composeTestRule.setContent {
            Subspace {
                SpatialColumn(SubspaceModifier.testTag("column")) {
                    SpatialPanel { Text(text = "Column") }
                }
            }
        }
        assertTrue(
            composeTestRule
                .onSubspaceNodeWithTag("column")
                .fetchSemanticsNode()
                .components
                .isNullOrEmpty()
        )
    }

    @Test
    fun movable_columnEntity_noComponentWhenMovableIsDisabled() {
        composeTestRule.setContent {
            Subspace {
                SpatialColumn(
                    SubspaceModifier.testTag("column").movable(enabled = false, stickyPose = false)
                ) {
                    SpatialPanel { Text(text = "Column") }
                }
            }
        }
        assertMovableComponentDoesNotExist("column")
    }

    @Test
    fun movable_rowEntity_noComponentByDefault() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.testTag("row")) { SpatialPanel { Text(text = "Row") } }
            }
        }
        assertTrue(
            composeTestRule
                .onSubspaceNodeWithTag("row")
                .fetchSemanticsNode()
                .components
                .isNullOrEmpty()
        )
    }

    @Test
    fun movable_rowEntity_noComponentWhenMovableIsDisabled() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    SubspaceModifier.testTag("row").movable(enabled = false, stickyPose = false)
                ) {
                    SpatialPanel { Text(text = "Row") }
                }
            }
        }
        assertMovableComponentDoesNotExist("row")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun movable_moveEvent_updatesEntityPose() {
        val session = composeTestRule.configureFakeSession()
        val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
        val activitySpace = sceneRuntime.activitySpace

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.testTag("panel")
                            .movable(enabled = true, stickyPose = false)
                ) {
                    Text(text = "Spatial Panel")
                }
            }
        }
        assertSingleMovableComponentExist()
        assertThat(sceneRuntime.lastMovableComponent).isNotNull()
        val rtMovableComponent = sceneRuntime.lastMovableComponent!!
        val expectedPose =
            Pose(Vector3(1f, 2f, 3f), Quaternion.fromAxisAngle(axis = Vector3.Forward, 45f))
        rtMovableComponent.onMoveEvent(
            MoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose.Identity,
                expectedPose,
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                MoveEvent.MOVE_STATE_ONGOING,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose.Identity,
                expectedPose,
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        val entity =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(entity)
        assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(expectedPose)
    }

    // ========================================================================
    // HELPER FUNCTIONS
    // ========================================================================

    @CanIgnoreReturnValue
    private fun assertSingleMovableComponentExist(testTag: String = "panel"): MovableComponent {
        val components =
            composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(1, components.size)
        assertIs<MovableComponent>(components[0])
        return components[0] as MovableComponent
    }

    private fun assertMovableComponentDoesNotExist(testTag: String = "panel") {
        val components =
            composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(0, components.size)
    }

    private fun AndroidComposeTestRule<*, *>.configureFakeSessionWithWatch(
        createMovableComponent:
            ((systemMovable: Boolean, scaleInZ: Boolean, userAnchorable: Boolean) -> Unit)? =
            null
    ) {
        configureFakeSession(
            sceneRuntime = { runtime ->
                object : SceneRuntime by runtime {
                    override fun createMovableComponent(
                        systemMovable: Boolean,
                        scaleInZ: Boolean,
                        userAnchorable: Boolean,
                    ): androidx.xr.scenecore.runtime.MovableComponent =
                        runtime
                            .createMovableComponent(systemMovable, scaleInZ, userAnchorable)
                            .also {
                                createMovableComponent?.invoke(
                                    systemMovable,
                                    scaleInZ,
                                    userAnchorable,
                                )
                            }
                }
            }
        )
    }

    // TODO: b/494305963 Remove references to arcore-testing Fakes
    @Suppress("DEPRECATION")
    private fun addPlaneToRuntime(
        perceptionRuntime: androidx.xr.arcore.testing.FakePerceptionRuntime,
        perceptionManager: androidx.xr.arcore.testing.FakePerceptionManager,
        type: Plane.Type = Plane.Type.HORIZONTAL_UPWARD_FACING,
        label: Plane.Label = Plane.Label.FLOOR,
        trackingState: TrackingState = TrackingState.TRACKING,
        centerPose: Pose = Pose(),
        extents: FloatSize2d = FloatSize2d(),
    ) {
        perceptionManager.trackables.add(
            androidx.xr.arcore.testing.FakeRuntimePlane(
                type,
                label,
                trackingState,
                centerPose,
                extents,
            )
        )
        perceptionRuntime.timeSource.plusAssign(1.milliseconds)
        perceptionRuntime.allowOneMoreCallToUpdate()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun initiateMoveEvents(
        rtMovableComponent: FakeMovableComponent,
        activitySpace: FakeActivitySpace,
        movePose: Pose,
    ) {
        rtMovableComponent.onMoveEvent(
            MoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose.Identity,
                movePose,
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                MoveEvent.MOVE_STATE_ONGOING,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose.Identity,
                movePose,
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                MoveEvent.MOVE_STATE_END,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose.Identity,
                movePose,
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )
    }

    companion object {
        private const val TOLERANCE: Float = 0.001f

        private fun getRotationMatrixFromAxes(
            xAxis: Vector3,
            yAxis: Vector3,
            zAxis: Vector3,
        ): Matrix4 {
            return Matrix4(
                floatArrayOf(
                    xAxis.x,
                    xAxis.y,
                    xAxis.z,
                    0f,
                    yAxis.x,
                    yAxis.y,
                    yAxis.z,
                    0f,
                    zAxis.x,
                    zAxis.y,
                    zAxis.z,
                    0f,
                    0f,
                    0f,
                    0f,
                    1f,
                )
            )
        }
    }
}
