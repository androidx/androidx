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
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.FakeActivitySpace
import androidx.xr.scenecore.testing.FakeMovableComponent
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
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

    @Before
    fun setup() {
        // TODO: b/537470420 Remove once Anchors are properly detached in unit tests.
        androidx.xr.arcore.testing.FakeRuntimeAnchor.anchorsCreatedCount = 0
    }

    // ========================================================================
    // TESTS FOR MOVE POLICY (NEW API)
    // ========================================================================

    @Test
    fun policySystem_createsSystemMovableComponent() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").movable(movePolicy = MovePolicy.system())
                ) {
                    Text(text = "Panel")
                }
            }
        }
        assertSingleMovableComponentExist()
    }

    @Test
    fun policySystem_scaleWithDistance_passedCorrectly() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(movePolicy = MovePolicy.system(scaleWithDistance = false))
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
            assertIs<AnchorSpace>(entity.parent)
        }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalMoveAnchorPolicy::class)
    @Test
    fun policyAnchor_whenAnchoredToPlaneAndRecomposed_retainsPoseAndRotation() =
        runTest(testDispatcher) {
            val anchorTestSession = setupAnchorTestSession()
            var planeSemantics by mutableStateOf(setOf(PlaneSemantic.Any))
            var panelSize by mutableStateOf(200.dp)

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .size(panelSize)
                                .movable(
                                    enabled = true,
                                    movePolicy =
                                        MovePolicy.anchor(
                                            anchorPlaneSemantics = planeSemantics,
                                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                                        ),
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

            addFloorPlaneToRuntime(
                anchorTestSession.perceptionRuntime,
                anchorTestSession.perceptionManager,
            )

            assertSingleMovableComponentExist()
            val rtMovableComponent =
                assertNotNull(anchorTestSession.sceneRuntime.lastMovableComponent)
            val movePose = Pose(Vector3(2f, 0.01f, 3f), Quaternion.fromEulerAngles(20f, 30f, 45f))
            initiateMoveEvents(rtMovableComponent, anchorTestSession.activitySpace, movePose)

            assertIs<AnchorSpace>(entity.parent)
            val anchoredPose = entity.getPose(Space.ACTIVITY)
            val anchoredLocalPose = entity.getPose()

            // Trigger modifier update and layout pass by changing semantics and size
            planeSemantics = setOf(PlaneSemantic.Floor)
            panelSize = 250.dp
            composeTestRule.waitForIdle()

            // Verify that the parent is still AnchorSpace, and the pose did not change
            assertIs<AnchorSpace>(entity.parent)
            assertPose(entity.getPose(Space.ACTIVITY), anchoredPose, TOLERANCE)
            assertPose(entity.getPose(), anchoredLocalPose, TOLERANCE)
        }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalMoveAnchorPolicy::class)
    @Test
    fun policyAnchor_whenAnchoredToPlaneInContainerAndRecomposed_retainsPoseAndRotation() =
        runTest(testDispatcher) {
            val anchorTestSession = setupAnchorTestSession()
            var planeSemantics by mutableStateOf(setOf(PlaneSemantic.Any))
            var panelSize by mutableStateOf(200.dp)

            composeTestRule.setContent {
                Subspace {
                    SpatialRow(modifier = SubspaceModifier.testTag("row")) {
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.testTag("panel")
                                    .size(panelSize)
                                    .movable(
                                        enabled = true,
                                        movePolicy =
                                            MovePolicy.anchor(
                                                anchorPlaneSemantics = planeSemantics,
                                                anchorPlaneOrientations =
                                                    setOf(PlaneOrientation.Any),
                                            ),
                                    )
                        ) {
                            Text(text = "Spatial Panel")
                        }
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

            addFloorPlaneToRuntime(
                anchorTestSession.perceptionRuntime,
                anchorTestSession.perceptionManager,
            )

            assertSingleMovableComponentExist()
            val rtMovableComponent =
                assertNotNull(anchorTestSession.sceneRuntime.lastMovableComponent)
            val movePose = Pose(Vector3(2f, 0.01f, 3f), Quaternion.fromEulerAngles(20f, 30f, 45f))
            initiateMoveEvents(rtMovableComponent, anchorTestSession.activitySpace, movePose)

            assertIs<AnchorSpace>(entity.parent)
            val anchoredPose = entity.getPose(Space.ACTIVITY)
            val anchoredLocalPose = entity.getPose()

            // Trigger modifier update and layout pass by changing semantics and size
            planeSemantics = setOf(PlaneSemantic.Floor)
            panelSize = 250.dp
            composeTestRule.waitForIdle()

            // Verify that the parent is still AnchorSpace, and the pose did not change
            assertIs<AnchorSpace>(entity.parent)
            assertPose(entity.getPose(Space.ACTIVITY), anchoredPose, TOLERANCE)
            assertPose(entity.getPose(), anchoredLocalPose, TOLERANCE)
        }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalMoveAnchorPolicy::class)
    @Test
    fun policyAnchor_moveWithoutMatchingPlane_doesNotAnchorAndUpdatesPose() =
        runTest(testDispatcher) {
            val anchorTestSession = setupAnchorTestSession()
            var panelSize by mutableStateOf(200.dp)
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .size(panelSize)
                                .movable(
                                    enabled = true,
                                    movePolicy =
                                        MovePolicy.anchor(
                                            anchorPlaneSemantics = setOf(PlaneSemantic.Floor),
                                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                                        ),
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

            // Initiate move without matching plane added to runtime
            assertSingleMovableComponentExist()
            val rtMovableComponent =
                assertNotNull(anchorTestSession.sceneRuntime.lastMovableComponent)
            val movePose = Pose(Vector3(2f, 1f, 3f), Quaternion.fromEulerAngles(20f, 30f, 45f))
            initiateMoveEvents(rtMovableComponent, anchorTestSession.activitySpace, movePose)

            // Verify entity is NOT anchored to AnchorSpace
            assertThat(entity.parent).isNotInstanceOf(AnchorSpace::class.java)

            // Change panel size to trigger layout pass / recomposition
            panelSize = 250.dp
            composeTestRule.waitForIdle()

            // Verify layout pose updates still apply when un-anchored
            assertThat(entity.parent).isNotInstanceOf(AnchorSpace::class.java)
        }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalMoveAnchorPolicy::class)
    @Test
    fun policyAnchor_anchoredEntityLosesTracking_unanchorsAndUpdatesPose() =
        runTest(testDispatcher) {
            val anchorTestSession = setupAnchorTestSession()
            var panelSize by mutableStateOf(200.dp)
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .size(panelSize)
                                .movable(
                                    enabled = true,
                                    movePolicy =
                                        MovePolicy.anchor(
                                            anchorPlaneSemantics = setOf(PlaneSemantic.Any),
                                            anchorPlaneOrientations = setOf(PlaneOrientation.Any),
                                        ),
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

            val originalParent = entity.parent
            addFloorPlaneToRuntime(
                anchorTestSession.perceptionRuntime,
                anchorTestSession.perceptionManager,
            )

            assertSingleMovableComponentExist()
            val rtMovableComponent =
                assertNotNull(anchorTestSession.sceneRuntime.lastMovableComponent)
            val movePose = Pose(Vector3(2f, 0.01f, 3f), Quaternion.fromEulerAngles(20f, 30f, 45f))
            initiateMoveEvents(rtMovableComponent, anchorTestSession.activitySpace, movePose)

            // Verify entity is anchored to AnchorSpace
            assertIs<AnchorSpace>(entity.parent)

            // Simulate loss of tracking / unanchoring by restoring parent to non-AnchorSpace
            entity.parent = originalParent
            assertThat(entity.parent).isNotInstanceOf(AnchorSpace::class.java)

            // Trigger recomposition and layout pass by changing panel size
            panelSize = 250.dp
            composeTestRule.waitForIdle()

            // Verify layout pose updates work again now that entity is un-anchored
            assertThat(entity.parent).isNotInstanceOf(AnchorSpace::class.java)
        }

    private data class AnchorTestSession(
        val session: Session,
        val perceptionRuntime: androidx.xr.arcore.testing.FakePerceptionRuntime,
        val perceptionManager: androidx.xr.arcore.testing.FakePerceptionManager,
        val sceneRuntime: FakeSceneRuntime,
        val activitySpace: FakeActivitySpace,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupAnchorTestSession(): AnchorTestSession {
        val sessionCreateResult = Session.create(composeTestRule.activity, testDispatcher)
        val session = assertIs<SessionCreateSuccess>(sessionCreateResult).session
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
        return AnchorTestSession(
            session,
            perceptionRuntime,
            perceptionManager,
            sceneRuntime,
            activitySpace,
        )
    }

    // TODO: b/494305963 Remove references to arcore-testing Fakes
    @Suppress("DEPRECATION")
    private fun addFloorPlaneToRuntime(
        perceptionRuntime: androidx.xr.arcore.testing.FakePerceptionRuntime,
        perceptionManager: androidx.xr.arcore.testing.FakePerceptionManager,
    ) {
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
    }

    @Test
    fun policySystem_modifierIsDisabledAndComponentDoesNotExist() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(enabled = false, movePolicy = MovePolicy.system())
                ) {
                    Text(text = "Panel")
                }
            }
        }
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun policySystem_scaleWithDistance_scaleFlipUpdatesComponent() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                var scaleWithDistance by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .movable(
                            enabled = true,
                            movePolicy = MovePolicy.system(scaleWithDistance = scaleWithDistance),
                        )
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { scaleWithDistance = !scaleWithDistance },
                    ) {
                        Text(text = "Toggle Scale")
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun policySystem_moveEvent_updatesEntityPose() {
        val session = composeTestRule.configureFakeSession()
        val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
        val activitySpace = sceneRuntime.activitySpace

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.testTag("panel")
                            .movable(enabled = true, movePolicy = MovePolicy.system())
                ) {
                    Text(text = "Spatial Panel")
                }
            }
        }

        assertSingleMovableComponentExist()

        val rtMovableComponent = assertNotNull(sceneRuntime.lastMovableComponent)
        val expectedPose =
            Pose(Vector3(1f, 2f, 3f), Quaternion.fromAxisAngle(axis = Vector3.Forward, 45f))

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                moveState = MoveEvent.MOVE_STATE_START,
                initialInputRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                currentInputRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                previousPose = Pose.Identity,
                currentPose = expectedPose,
                previousScale = Vector3(1f, 1f, 1f),
                currentScale = Vector3(1f, 1f, 1f),
                initialParent = activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                moveState = MoveEvent.MOVE_STATE_ONGOING,
                initialInputRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                currentInputRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                previousPose = Pose.Identity,
                currentPose = expectedPose,
                previousScale = Vector3(1f, 1f, 1f),
                currentScale = Vector3(1f, 1f, 1f),
                initialParent = activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                moveState = MoveEvent.MOVE_STATE_END,
                initialInputRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                currentInputRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                previousPose = Pose.Identity,
                currentPose = expectedPose,
                previousScale = Vector3(1f, 1f, 1f),
                currentScale = Vector3(1f, 1f, 1f),
                initialParent = activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        composeTestRule.waitForIdle()

        val entity =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(entity)
        assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(expectedPose)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun policySystem_optionalCallbackIsInvoked() {
        var moveEvent: SpatialMoveEvent? = null
        val session = composeTestRule.configureFakeSession()
        val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
        val activitySpace = sceneRuntime.activitySpace

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.testTag("panel")
                            .movable(
                                enabled = true,
                                movePolicy = MovePolicy.system { event -> moveEvent = event },
                            )
                ) {
                    Text(text = "Spatial Panel")
                }
            }
        }

        val rtMovableComponent = assertNotNull(sceneRuntime.lastMovableComponent)
        val pixelDensity = session.scene.virtualPixelDensity
        val expectedPose =
            Pose(Vector3(1f, 2f, 3f), Quaternion.fromAxisAngle(axis = Vector3.Forward, 45f))
        val expectedPixelsPose = expectedPose.metersToPx(pixelDensity)

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                moveState = MoveEvent.MOVE_STATE_START,
                initialInputRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                currentInputRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                previousPose = Pose.Identity,
                currentPose = expectedPose,
                previousScale = Vector3(1f, 1f, 1f),
                currentScale = Vector3(1f, 1f, 1f),
                initialParent = activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        composeTestRule.waitForIdle()
        assertThat(moveEvent).isNotNull()
        assertThat(moveEvent!!.type).isEqualTo(SpatialMoveEventType.Start)
        assertThat(moveEvent!!.pose).isEqualTo(Pose.Identity)

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                moveState = MoveEvent.MOVE_STATE_ONGOING,
                initialInputRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                currentInputRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                previousPose = Pose.Identity,
                currentPose = expectedPose,
                previousScale = Vector3(1f, 1f, 1f),
                currentScale = Vector3(1f, 1f, 1f),
                initialParent = activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        composeTestRule.waitForIdle()
        assertThat(moveEvent!!.type).isEqualTo(SpatialMoveEventType.Moving)
        assertThat(moveEvent!!.pose).isEqualTo(expectedPixelsPose)

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                moveState = MoveEvent.MOVE_STATE_END,
                initialInputRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                currentInputRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                previousPose = Pose.Identity,
                currentPose = expectedPose,
                previousScale = Vector3(1f, 1f, 1f),
                currentScale = Vector3(1f, 1f, 1f),
                initialParent = activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        composeTestRule.waitForIdle()
        assertThat(moveEvent!!.type).isEqualTo(SpatialMoveEventType.End)
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
