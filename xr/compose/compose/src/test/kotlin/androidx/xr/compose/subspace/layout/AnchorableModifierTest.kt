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

package androidx.xr.compose.subspace.layout

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.arcore.testing.FakeRuntimePlane
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.runtime.MoveEvent
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceModifier.anchorable] modifier. */
@RunWith(AndroidJUnit4::class)
class AnchorableModifierTest {

    private val testDispatcher = StandardTestDispatcher()

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @get:Rule val permissionRule = GrantPermissionRule.grant(SCENE_UNDERSTANDING_COARSE)

    private lateinit var session: Session
    private lateinit var lifecycleManager: FakeLifecycleManager
    private lateinit var perceptionManager: FakePerceptionManager
    private lateinit var activitySpace: FakeActivitySpace
    private lateinit var sceneRuntime: FakeSceneRuntime

    @Before
    fun setup() {
        val sessionCreateResult = Session.create(composeTestRule.activity, testDispatcher)
        assertThat(sessionCreateResult).isInstanceOf(SessionCreateSuccess::class.java)
        session = (sessionCreateResult as SessionCreateSuccess).session
        session.configure(
            config = session.config.copy(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
        )
        session.runtimes.filterIsInstance<FakePerceptionRuntime>().single().let {
            lifecycleManager = it.lifecycleManager
            perceptionManager = it.perceptionManager
        }
        session.runtimes.filterIsInstance<FakeSceneRuntime>().single().let {
            activitySpace = it.activitySpace
            sceneRuntime = it
        }
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun anchorable_noComponentByDefault() {
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
    fun anchorable_componentIsNotNullAndOnlyContainsSingleMovable() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .anchorable(
                            enabled = true,
                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                        )
                ) {
                    Text(text = "Panel")
                }
            }
        }
        assertSingleMovableComponentExist()
    }

    @Test
    fun anchorable_modifierIsDisabledAndComponentDoesNotExist() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .anchorable(
                            enabled = false,
                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                        )
                ) {
                    Text(text = "Panel")
                }
            }
        }
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun anchorable_modifierDoesNotChangeAndOnlyOneComponentExist() {
        composeTestRule.setContent {
            Subspace {
                var panelWidth by remember { mutableStateOf(50.dp) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .anchorable(
                            enabled = true,
                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                        )
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
    fun anchorable_modifierEnabledToDisabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var anchorableEnabled by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .anchorable(
                            enabled = anchorableEnabled,
                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                        )
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { anchorableEnabled = !anchorableEnabled },
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
    fun anchorable_modifierDisabledThenEnabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var anchorableEnabled by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .anchorable(
                            enabled = anchorableEnabled,
                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                        )
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { anchorableEnabled = !anchorableEnabled },
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
    fun anchorable_columnEntity_noComponentByDefault() {
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
    fun anchorable_columnEntity_noComponentWhenAnchorableIsEnabled() {
        composeTestRule.setContent {
            Subspace {
                SpatialColumn(
                    SubspaceModifier.testTag("column")
                        .anchorable(
                            enabled = true,
                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                        )
                ) {
                    SpatialPanel { Text(text = "Column") }
                }
            }
        }
        assertMovableComponentDoesNotExist("column")
    }

    @Test
    fun anchorable_columnEntity_noComponentWhenAnchorableIsDisabled() {
        composeTestRule.setContent {
            Subspace {
                SpatialColumn(
                    SubspaceModifier.testTag("column")
                        .anchorable(
                            enabled = false,
                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                        )
                ) {
                    SpatialPanel { Text(text = "Column") }
                }
            }
        }
        assertMovableComponentDoesNotExist("column")
    }

    @Test
    fun anchorable_rowEntity_noComponentByDefault() {
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
    fun anchorable_rowEntity_noComponentWhenAnchorableIsEnabled() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    SubspaceModifier.testTag("row")
                        .anchorable(
                            enabled = true,
                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                        )
                ) {
                    SpatialPanel { Text(text = "Row") }
                }
            }
        }
        assertMovableComponentDoesNotExist("row")
    }

    @Test
    fun anchorable_rowEntity_noComponentWhenAnchorableIsDisabled() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    SubspaceModifier.testTag("row")
                        .anchorable(
                            enabled = false,
                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                        )
                ) {
                    SpatialPanel { Text(text = "Row") }
                }
            }
        }
        assertMovableComponentDoesNotExist("row")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anchorable_moveCloseEnoughToNonMatchingPlane_doesNotAnchorToPlane() {
        runTest(testDispatcher) {
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .anchorable(
                                    enabled = true,
                                    anchorPlaneOrientations = setOf(PlaneOrientation.Vertical),
                                    anchorPlaneSemantics = setOf(PlaneSemantic.Wall),
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
            val initialParent = entity.parent
            assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(Pose.Identity)

            val planeCenterPosition = Vector3(1f, 0f, 2f)
            val planeRotation = Quaternion.fromAxisAngle(Vector3.Up, -45f)
            val planePose = Pose(planeCenterPosition, planeRotation)
            addPlaneToRuntime(
                type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                label = Plane.Label.FLOOR,
                centerPose = planePose,
                extents = FloatSize2d(5f, 5f),
            )

            assertSingleMovableComponentExist()
            assertThat(sceneRuntime.lastMovableComponent).isNotNull()
            val rtMovableComponent = sceneRuntime.lastMovableComponent!!
            // Move pose is above plane within anchor distance
            val movePose = Pose(Vector3(2f, 0.05f, 3f), Quaternion.fromEulerAngles(20f, 30f, 45f))
            initiateMoveEvents(rtMovableComponent, movePose)

            assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(movePose)
            assertThat(entity.parent).isEqualTo(initialParent)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anchorable_moveNotCloseEnoughToMatchingPlane_doesNotAnchorToPlane() {
        runTest(testDispatcher) {
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .anchorable(
                                    enabled = true,
                                    anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                                    anchorPlaneSemantics = setOf(PlaneSemantic.Any),
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
            val initialParent = entity.parent
            assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(Pose.Identity)

            val planeCenterPosition = Vector3(1f, 0f, 2f)
            val planeRotation = Quaternion.fromAxisAngle(Vector3.Up, -45f)
            val planePose = Pose(planeCenterPosition, planeRotation)
            addPlaneToRuntime(
                type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                label = Plane.Label.FLOOR,
                centerPose = planePose,
                extents = FloatSize2d(5f, 5f),
            )

            assertSingleMovableComponentExist()
            assertThat(sceneRuntime.lastMovableComponent).isNotNull()
            val rtMovableComponent = sceneRuntime.lastMovableComponent!!
            // Move pose is above plane but too far away to anchor
            val movePose = Pose(Vector3(2f, 0.5f, 3f), Quaternion.fromEulerAngles(20f, 30f, 45f))
            initiateMoveEvents(rtMovableComponent, movePose)

            assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(movePose)
            assertThat(entity.parent).isEqualTo(initialParent)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anchorable_moveCloseEnoughToMatchingPlaneOutsideExtents_doesNotAnchorToPlane() {
        runTest(testDispatcher) {
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .anchorable(
                                    enabled = true,
                                    anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                                    anchorPlaneSemantics = setOf(PlaneSemantic.Any),
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
            val initialParent = entity.parent
            assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(Pose.Identity)

            val planeCenterPosition = Vector3(1f, 0f, 2f)
            val planeRotation = Quaternion.fromAxisAngle(Vector3.Up, -45f)
            val planePose = Pose(planeCenterPosition, planeRotation)
            addPlaneToRuntime(
                type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                label = Plane.Label.FLOOR,
                centerPose = planePose,
                extents = FloatSize2d(5f, 5f),
            )

            assertSingleMovableComponentExist()
            assertThat(sceneRuntime.lastMovableComponent).isNotNull()
            val rtMovableComponent = sceneRuntime.lastMovableComponent!!
            // Move pose is above plane within anchor distance.
            val movePose = Pose(Vector3(4f, 0.05f, 5f), Quaternion.fromEulerAngles(20f, 30f, 45f))
            initiateMoveEvents(rtMovableComponent, movePose)

            assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(movePose)
            assertThat(entity.parent).isEqualTo(initialParent)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anchorable_moveWithinAnchorDistanceToMatchingPlane_anchorsToPlane() {
        runTest(testDispatcher) {
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .anchorable(
                                    enabled = true,
                                    anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                                    anchorPlaneSemantics = setOf(PlaneSemantic.Any),
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
                type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                label = Plane.Label.FLOOR,
                centerPose = planePose,
                extents = FloatSize2d(5f, 5f),
            )

            assertSingleMovableComponentExist()
            assertThat(sceneRuntime.lastMovableComponent).isNotNull()
            val rtMovableComponent = sceneRuntime.lastMovableComponent!!
            val movePose = Pose(Vector3(2f, 0.01f, 3f), Quaternion.fromEulerAngles(20f, 30f, 45f))
            initiateMoveEvents(rtMovableComponent, movePose)

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
            assertThat(entity.parent).isInstanceOf(AnchorEntity::class.java)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anchorable_moveBelowMatchingPlane_anchorsToPlane() {
        runTest(testDispatcher) {
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .anchorable(
                                    enabled = true,
                                    anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                                    anchorPlaneSemantics = setOf(PlaneSemantic.Any),
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
                type = Plane.Type.HORIZONTAL_UPWARD_FACING,
                label = Plane.Label.FLOOR,
                centerPose = planePose,
                extents = FloatSize2d(5f, 5f),
            )

            assertSingleMovableComponentExist()
            assertThat(sceneRuntime.lastMovableComponent).isNotNull()
            val rtMovableComponent = sceneRuntime.lastMovableComponent!!
            val movePose = Pose(Vector3(2f, -10f, 3f), Quaternion.fromEulerAngles(20f, 30f, 45f))

            initiateMoveEvents(rtMovableComponent, movePose)

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
            assertThat(entity.parent).isInstanceOf(AnchorEntity::class.java)
        }
    }

    private fun addPlaneToRuntime(
        type: Plane.Type = Plane.Type.HORIZONTAL_UPWARD_FACING,
        label: Plane.Label = Plane.Label.FLOOR,
        trackingState: TrackingState = TrackingState.TRACKING,
        centerPose: Pose = Pose(),
        extents: FloatSize2d = FloatSize2d(),
    ) {
        perceptionManager.trackables.add(
            FakeRuntimePlane(type, label, trackingState, centerPose, extents)
        )
        lifecycleManager.timeSource.plusAssign(1.milliseconds)
        lifecycleManager.allowOneMoreCallToUpdate()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun initiateMoveEvents(rtMovableComponent: FakeMovableComponent, movePose: Pose) {
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

    private companion object {
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
