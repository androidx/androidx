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
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ExperimentalSubspaceVolumeApi
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.Volume
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.scenecore.MovableComponent
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [Movable] modifier. */
@RunWith(AndroidJUnit4::class)
class MovableModifierTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun movable_noComponentByDefault() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) { Text(text = "Panel") }
                }
            }
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
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").movable()) {
                        Text(text = "Panel")
                    }
                }
            }
        }
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_modifierIsDisabledAndComponentDoesNotExist() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").movable(false)) {
                        Text(text = "Panel")
                    }
                }
            }
        }
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun movable_modifierDoesNotChangeAndOnlyOneComponentExist() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    var panelWidth by remember { mutableStateOf(50.dp) }
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").width(panelWidth).movable(enabled = true)
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
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose there should still only exist one Component.
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_modifierEnabledToDisabledAndComponentUpdates() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    var movableEnabled by remember { mutableStateOf(true) }
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").movable(enabled = movableEnabled)
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
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose no Components should exist.
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun movable_modifierOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    var onPoseReturnValue by remember { mutableStateOf(true) }
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .movable(enabled = true, onMove = { onPoseReturnValue })
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
            TestSetup {
                Subspace {
                    var movableEnabled by remember { mutableStateOf(true) }
                    var onPoseReturnValue by remember { mutableStateOf(true) }
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .movable(enabled = movableEnabled, onMove = { onPoseReturnValue })
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
        }
        assertSingleMovableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose Component should be removed.
        assertMovableComponentDoesNotExist()
    }

    @Test
    fun movable_modifierEnabledWithOnPoseChangeUpdateAndComponentUpdates() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    var movableEnabled by remember { mutableStateOf(false) }
                    var onPoseReturnValue by remember { mutableStateOf(true) }
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .movable(enabled = movableEnabled, onMove = { onPoseReturnValue })
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
        }
        assertMovableComponentDoesNotExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose Component should exist and be attached.
        assertSingleMovableComponentExist()
    }

    @Test
    fun movable_modifierDisabledThenEnabledAndComponentUpdates() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    var movableEnabled by remember { mutableStateOf(true) }
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").movable(enabled = movableEnabled)
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
            TestSetup {
                Subspace {
                    var onPoseReturnValue by remember { mutableStateOf(true) }
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .movable(enabled = true, onMove = { onPoseReturnValue })
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
            TestSetup {
                Subspace {
                    var movableEnabled by remember { mutableStateOf(true) }
                    var onPoseReturnValue by remember { mutableStateOf(true) }
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .movable(enabled = movableEnabled, onMove = { onPoseReturnValue })
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
            TestSetup {
                Subspace {
                    var movableEnabled by remember { mutableStateOf(false) }
                    var onPoseReturnValue by remember { mutableStateOf(true) }
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .movable(enabled = movableEnabled, onMove = { onPoseReturnValue })
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
            TestSetup {
                Subspace {
                    SpatialColumn(SubspaceModifier.testTag("column")) {
                        SpatialPanel { Text(text = "Column") }
                    }
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
    fun movable_columnEntity_noComponentWhenMovableIsEnabled() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialColumn(SubspaceModifier.testTag("column").movable()) {
                        SpatialPanel { Text(text = "Column") }
                    }
                }
            }
        }
        assertMovableComponentDoesNotExist("column")
    }

    @Test
    fun movable_columnEntity_noComponentWhenMovableIsDisabled() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialColumn(SubspaceModifier.testTag("column").movable(false)) {
                        SpatialPanel { Text(text = "Column") }
                    }
                }
            }
        }
        assertMovableComponentDoesNotExist("column")
    }

    @Test
    fun movable_rowEntity_noComponentByDefault() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.testTag("row")) {
                        SpatialPanel { Text(text = "Row") }
                    }
                }
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
    fun movable_rowEntity_noComponentWhenMovableIsEnabled() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.testTag("row").movable()) {
                        SpatialPanel { Text(text = "Row") }
                    }
                }
            }
        }
        assertMovableComponentDoesNotExist("row")
    }

    @Test
    fun movable_rowEntity_noComponentWhenMovableIsDisabled() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.testTag("row").movable(false)) {
                        SpatialPanel { Text(text = "Row") }
                    }
                }
            }
        }
        assertMovableComponentDoesNotExist("row")
    }

    @OptIn(ExperimentalSubspaceVolumeApi::class)
    @Test
    fun movable_volumeEntity_noComponentByDefault() {
        composeTestRule.setContent {
            TestSetup { Subspace { Volume(SubspaceModifier.testTag("volume")) {} } }
        }
        assertTrue(
            composeTestRule
                .onSubspaceNodeWithTag("volume")
                .fetchSemanticsNode()
                .components
                .isNullOrEmpty()
        )
    }

    @OptIn(ExperimentalSubspaceVolumeApi::class)
    @Test
    fun movable_volumeEntity_noComponentWhenMovableIsEnabled() {
        composeTestRule.setContent {
            TestSetup { Subspace { Volume(SubspaceModifier.testTag("volume").movable()) {} } }
        }
        assertMovableComponentDoesNotExist("volume")
    }

    @OptIn(ExperimentalSubspaceVolumeApi::class)
    @Test
    fun movable_volumeEntity_noComponentWhenMovableIsDisabled() {
        composeTestRule.setContent {
            TestSetup { Subspace { Volume(SubspaceModifier.testTag("volume").movable(false)) {} } }
        }
        assertMovableComponentDoesNotExist("volume")
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
}
