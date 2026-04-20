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
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

        // Add the END event so TransformingMovableNode commits the pose to the layout
        rtMovableComponent.onMoveEvent(
            MoveEvent(
                MoveEvent.MOVE_STATE_END,
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

        val rtMovableComponent = sceneRuntime.lastMovableComponent!!

        rtMovableComponent.onMoveEvent(
            MoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose.Identity,
                Pose.Identity,
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
}
