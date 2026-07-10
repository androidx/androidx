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

@file:Suppress("DEPRECATION")

package androidx.xr.compose.subspace.layout

import android.annotation.TargetApi
import android.os.Build
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
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialGltfModel
import androidx.xr.compose.subspace.SpatialGltfModelSource
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.rememberSpatialGltfModelState
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.runtime.MoveEvent
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceModifier.transformingMovable] modifier. */
@RunWith(AndroidJUnit4::class)
class TransformingMovableTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun transformingMovable_componentIsNotNullAndIsSystemMovable() {
        val systemMovableFlags = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { systemMovable, _, _ ->
            systemMovableFlags.add(systemMovable)
        }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").transformingMovable()) {
                    Text(text = "Panel")
                }
            }
        }

        assertSingleMovableComponentExist()
        // transformingMovable uses MovableComponent.createSystemMovable, so systemMovable should be
        // true
        assertThat(systemMovableFlags.single()).isTrue()
    }

    @Test
    fun transformingMovable_modifierIsDisabledAndComponentDoesNotExist() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").transformingMovable(enabled = false)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        assertMovableComponentDoesNotExist()
    }

    @Test
    fun transformingMovable_scaleWithDistance_scaleFlipUpdatesComponent() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                var scaleWithDistance by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .width(200.dp)
                        .transformingMovable(enabled = true, scaleWithDistance = scaleWithDistance)
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

        // Recomposition should have removed and re-added the component with the new scale setting
        assertThat(scalesInZ.size).isEqualTo(2)
        assertThat(scalesInZ[0]).isTrue()
        assertThat(scalesInZ[1]).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun transformingMovable_moveEvent_updatesEntityPose() {
        val session = composeTestRule.configureFakeSession()
        val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
        val activitySpace = sceneRuntime.activitySpace

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier = SubspaceModifier.testTag("panel").transformingMovable(enabled = true)
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

        // Add the END event so TransformingMovableNode commits the pose to the layout
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

        // Wait for Compose to lay out the element at the new userPose
        composeTestRule.waitForIdle()

        val entity =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(entity)
        assertThat(entity.getPose(Space.ACTIVITY)).isEqualTo(expectedPose)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun transformingMovable_optionalCallbackIsInvoked() {
        var moveEvent: SpatialMoveEvent? = null
        val session = composeTestRule.configureFakeSession()
        val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
        val activitySpace = sceneRuntime.activitySpace

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.testTag("panel").transformingMovable(enabled = true) {
                            event ->
                            moveEvent = event
                        }
                ) {
                    Text(text = "Spatial Panel")
                }
            }
        }

        val rtMovableComponent = assertNotNull(sceneRuntime.lastMovableComponent)

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                moveState = MoveEvent.MOVE_STATE_START,
                initialInputRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                currentInputRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                previousPose = Pose.Identity,
                currentPose = Pose.Identity,
                previousScale = Vector3(1f, 1f, 1f),
                currentScale = Vector3(1f, 1f, 1f),
                initialParent = activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        composeTestRule.waitForIdle()
        assertThat(moveEvent).isNotNull()
        assertThat(assertNotNull(moveEvent).type).isEqualTo(SpatialMoveEventType.Start)
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalRotateToLookAtUserApi::class)
    @Test
    fun transformingMovable_withRotateToLookAtUser_movesAndRotatesCorrectly() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sessionCreateResult = Session.create(composeTestRule.activity, testDispatcher)
        assertThat(sessionCreateResult).isInstanceOf(SessionCreateSuccess::class.java)

        val session = (sessionCreateResult as SessionCreateSuccess).session
        composeTestRule.session = session
        session.configure(Config(deviceTracking = DeviceTrackingMode.SPATIAL))
        val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
        val activitySpace = sceneRuntime.activitySpace

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.testTag("panel")
                            .transformingMovable(enabled = true)
                            .rotateToLookAtUser()
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
        assertThat(entity.getPose(Space.ACTIVITY).translation).isEqualTo(expectedPose.translation)

        val fakePerceptionRuntimeInstance =
            session.runtimes
                .filterIsInstance<androidx.xr.arcore.testing.FakePerceptionRuntime>()
                .single()
        val arDevice = fakePerceptionRuntimeInstance.perceptionManager.arDevice

        val userLocation = Vector3(x = 5f, y = 2f, z = 10f)
        arDevice.devicePose = arDevice.devicePose.translate(translation = userLocation)

        testDispatcher.scheduler.advanceUntilIdle()
        fakePerceptionRuntimeInstance.allowOneMoreCallToUpdate()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        // Verify that the panel's position remains at the dragged translation,
        // but its rotation turns to face the new user location
        val finalPose = entity.getPose(Space.ACTIVITY)
        assertThat(finalPose.translation).isEqualTo(expectedPose.translation)

        val targetVector = userLocation - finalPose.translation
        val expectedRotation = Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))

        assertThat(finalPose.rotation.x).isWithin(TOLERANCE).of(expectedRotation.x)
        assertThat(finalPose.rotation.y).isWithin(TOLERANCE).of(expectedRotation.y)
        assertThat(finalPose.rotation.z).isWithin(TOLERANCE).of(expectedRotation.z)
        assertThat(finalPose.rotation.w).isWithin(TOLERANCE).of(expectedRotation.w)
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalRotateToLookAtUserApi::class)
    @Test
    fun transformingMovable_withRotateToLookAtUser_oppositeOrder_movesAndRotatesCorrectly() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val sessionCreateResult = Session.create(composeTestRule.activity, testDispatcher)
            assertThat(sessionCreateResult).isInstanceOf(SessionCreateSuccess::class.java)

            val session = (sessionCreateResult as SessionCreateSuccess).session
            composeTestRule.session = session
            session.configure(Config(deviceTracking = DeviceTrackingMode.SPATIAL))
            val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
            val activitySpace = sceneRuntime.activitySpace

            val fakePerceptionRuntimeInstance =
                session.runtimes
                    .filterIsInstance<androidx.xr.arcore.testing.FakePerceptionRuntime>()
                    .single()
            val arDevice = fakePerceptionRuntimeInstance.perceptionManager.arDevice
            arDevice.devicePose = Pose.Identity

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel")
                                .rotateToLookAtUser()
                                .transformingMovable(enabled = true)
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

            // With the opposite order (rotateToLookAtUser preceding transformingMovable),
            // the transformingMovable is a child of rotateToLookAtUser.
            // Thus, the translation applied by transformingMovable is rotated by the parent's
            // look-at rotation.
            // This causes the final translation in Activity space to be mathematically rotated:
            val initialTranslation = entity.getPose(Space.ACTIVITY).translation
            assertThat(initialTranslation.x).isWithin(TOLERANCE).of(expectedPose.translation.x)
            assertThat(initialTranslation.y).isWithin(TOLERANCE).of(expectedPose.translation.y)
            assertThat(initialTranslation.z).isWithin(TOLERANCE).of(expectedPose.translation.z)

            val userLocation = Vector3(x = 5f, y = 2f, z = 10f)
            arDevice.devicePose = arDevice.devicePose.translate(translation = userLocation)

            testDispatcher.scheduler.advanceUntilIdle()
            fakePerceptionRuntimeInstance.allowOneMoreCallToUpdate()
            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val finalPose = entity.getPose(Space.ACTIVITY)

            // Since the parent rotates to follow the user's new head location,
            // the child's relative translation is rotated.
            // Specifically, the parent look-at rotation is computed from the origin [0, 0, 0]
            // to the user head location [5f, 2f, 10f]:
            //   D = normalized([5, 2, 10])
            //   R = Quaternion.fromLookTowards(D, Vector3.Up)
            //
            // Applying this look-towards rotation R to the child's cached layout translation [1f,
            // 2f, 3f]
            // (i.e. finalTranslation = R * [1f, 2f, 3f]) yields exactly the rotated translation:
            val expectedFinalTranslation = Vector3(2.0576036f, 2.4970186f, 1.8791394f)
            assertThat(finalPose.translation.x).isWithin(TOLERANCE).of(expectedFinalTranslation.x)
            assertThat(finalPose.translation.y).isWithin(TOLERANCE).of(expectedFinalTranslation.y)
            assertThat(finalPose.translation.z).isWithin(TOLERANCE).of(expectedFinalTranslation.z)

            val targetVector = userLocation
            val lookAtRotation = Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))
            val expectedRotation = lookAtRotation * expectedPose.rotation

            assertThat(finalPose.rotation.x).isWithin(TOLERANCE).of(expectedRotation.x)
            assertThat(finalPose.rotation.y).isWithin(TOLERANCE).of(expectedRotation.y)
            assertThat(finalPose.rotation.z).isWithin(TOLERANCE).of(expectedRotation.z)
            assertThat(finalPose.rotation.w).isWithin(TOLERANCE).of(expectedRotation.w)
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

    //    -------------------------------------------------------
    //               SpatialGltfModel tests
    //    -------------------------------------------------------
    @Test
    @TargetApi(Build.VERSION_CODES.O) // needed for the Paths.get API
    fun transformingMovable_onSpatialGltfModelIsMoved_reportsCorrectPoses() {
        val session = composeTestRule.configureFakeSession()
        val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
        val activitySpace = sceneRuntime.activitySpace

        composeTestRule.setContent {
            Subspace {
                val state =
                    rememberSpatialGltfModelState(
                        source = SpatialGltfModelSource.fromPath(Paths.get("models", "test.gltf"))
                    )
                SpatialGltfModel(
                    state = state,
                    modifier = SubspaceModifier.testTag("model").transformingMovable(enabled = true),
                )
            }
        }

        assertSingleMovableComponentExist(testTag = "model")

        val rtMovableComponent = assertNotNull(sceneRuntime.lastMovableComponent)

        // Simulate a move event that reports a new dragged pose
        val draggedPose = Pose(Vector3(1f, 2f, 3f), Quaternion.Identity)

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                moveState = MoveEvent.MOVE_STATE_START,
                initialInputRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                currentInputRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                previousPose = Pose.Identity,
                currentPose = draggedPose,
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
                currentPose = draggedPose,
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
                currentPose = draggedPose,
                previousScale = Vector3(1f, 1f, 1f),
                currentScale = Vector3(1f, 1f, 1f),
                initialParent = activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        val entity =
            composeTestRule
                .onSubspaceNodeWithTag(testTag = "model")
                .fetchSemanticsNode()
                .semanticsEntity
        assertNotNull(entity)
        assertThat(entity.getPose()).isEqualTo(draggedPose)
    }

    @Test
    @TargetApi(Build.VERSION_CODES.O) // needed for the Paths.get API
    fun transformingMovable_onSpatialGltfModel_optionalCallbackIsInvoked() {
        var moveEvent: SpatialMoveEvent? = null
        val session = composeTestRule.configureFakeSession()
        val sceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().single()
        val activitySpace = sceneRuntime.activitySpace

        composeTestRule.setContent {
            Subspace {
                val state =
                    rememberSpatialGltfModelState(
                        source = SpatialGltfModelSource.fromPath(Paths.get("models", "test.gltf"))
                    )
                SpatialGltfModel(
                    state = state,
                    modifier =
                        SubspaceModifier.testTag(tag = "model").transformingMovable(
                            enabled = true
                        ) { event ->
                            moveEvent = event
                        },
                )
            }
        }

        val rtMovableComponent = assertNotNull(sceneRuntime.lastMovableComponent)

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                moveState = MoveEvent.MOVE_STATE_START,
                initialInputRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                currentInputRay = Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                previousPose = Pose.Identity,
                currentPose = Pose.Identity,
                previousScale = Vector3(1f, 1f, 1f),
                currentScale = Vector3(1f, 1f, 1f),
                initialParent = activitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        )

        composeTestRule.waitForIdle()
        assertThat(moveEvent).isNotNull()
        assertThat(assertNotNull(moveEvent).type).isEqualTo(SpatialMoveEventType.Start)
    }

    @Test
    @TargetApi(Build.VERSION_CODES.O) // needed for the Paths.get API
    fun transformingMovable_onSpatialGltfModel_attachesAndDetachesComponentCorrectly() {
        var isEnabled by mutableStateOf(true)
        composeTestRule.setContent {
            Subspace {
                val state =
                    rememberSpatialGltfModelState(
                        source = SpatialGltfModelSource.fromPath(Paths.get("models", "test.gltf"))
                    )
                SpatialGltfModel(
                    state = state,
                    modifier =
                        SubspaceModifier.testTag(tag = "model").transformingMovable(isEnabled),
                )
            }
        }
        assertSingleMovableComponentExist(testTag = "model")
        isEnabled = false
        assertMovableComponentDoesNotExist(testTag = "model")
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

    private companion object {
        const val TOLERANCE = 1e-5f
    }
}
