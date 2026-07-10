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
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.runtime.SceneRuntime
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [MovePolicy] class for SpatialPanels. */
@RunWith(AndroidJUnit4::class)
class MovePolicyTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun movePolicy_noComponentByDefault() {
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
    fun movePolicy_componentIsNotNullAndOnlyContainsSingleMovable() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel"), dragPolicy = MovePolicy()) {
                    Text(text = "Panel")
                }
            }
        }
        assertSingleMovableComponentExist()
    }

    @Test
    fun movePolicy_modifierIsDisabledAndComponentDoesNotExist() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel"), dragPolicy = MovePolicy(false)) {
                    Text(text = "Panel")
                }
            }
        }
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun movable_scaleWithDistance_setTrue() {
        val scalesInZ = mutableListOf<Boolean>()
        composeTestRule.configureFakeSessionWithWatch { _, scaleInZ, _ -> scalesInZ.add(scaleInZ) }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").width(200.dp),
                    dragPolicy = MovePolicy(isEnabled = true, shouldScaleWithDistance = true),
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
                    SubspaceModifier.testTag("panel").width(200.dp),
                    dragPolicy = MovePolicy(isEnabled = true, shouldScaleWithDistance = false),
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
                var scaleWithDistance by remember { mutableStateOf(false) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel").width(200.dp),
                    dragPolicy =
                        MovePolicy(isEnabled = true, shouldScaleWithDistance = scaleWithDistance),
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

        assertThat(scalesInZ.single()).isFalse()

        composeTestRule.onNodeWithTag("button").performClick()
        composeTestRule.waitForIdle()

        assertThat(scalesInZ.size).isEqualTo(2)
        assertThat(scalesInZ[0]).isFalse()
        assertThat(scalesInZ[1]).isTrue()
    }

    @Test
    fun movePolicy_modifierDoesNotChangeAndOnlyOneComponentExist() {
        composeTestRule.setContent {
            Subspace {
                var panelWidth by remember { mutableStateOf(50.dp) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel").width(panelWidth),
                    dragPolicy = MovePolicy(isEnabled = true),
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
    fun movePolicy_modifierEnabledToDisabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel"),
                    dragPolicy = MovePolicy(isEnabled = movableEnabled),
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
    fun movePolicy_modifierOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var onPoseReturnValue by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel"),
                    dragPolicy = MovePolicy(isEnabled = true, onMove = { onPoseReturnValue }),
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
    fun movePolicy_modifierDisableWithOnPoseChangeUpdateAndComponentRemoved() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(true) }
                var onPoseReturnValue by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel"),
                    dragPolicy =
                        MovePolicy(isEnabled = movableEnabled, onMove = { onPoseReturnValue }),
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
    fun movePolicy_modifierEnabledWithOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(false) }
                var onPoseReturnValue by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel"),
                    dragPolicy =
                        MovePolicy(isEnabled = movableEnabled, onMove = { onPoseReturnValue }),
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
    fun movePolicy_modifierDisabledThenEnabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel"),
                    dragPolicy = MovePolicy(isEnabled = movableEnabled),
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
    fun movePolicy_modifierOnPoseChangeTwiceUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var onPoseReturnValue by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel"),
                    dragPolicy = MovePolicy(isEnabled = true, onMove = { onPoseReturnValue }),
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
    fun movePolicy_modifierDisabledThenEnabledWithOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(true) }
                var onPoseReturnValue by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel"),
                    dragPolicy =
                        MovePolicy(isEnabled = movableEnabled, onMove = { onPoseReturnValue }),
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
    fun movePolicy_modifierEnabledThenDisabledWithOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var movableEnabled by remember { mutableStateOf(false) }
                var onPoseReturnValue by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel"),
                    dragPolicy =
                        MovePolicy(isEnabled = movableEnabled, onMove = { onPoseReturnValue }),
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

    private fun assertSingleMovableComponentExist(testTag: String = "panel") {
        val components =
            composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(1, components.size)
        assertIs<MovableComponent>(components[0])
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
