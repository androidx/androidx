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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.Volume
import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.setSubspaceContent
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.scenecore.ResizableComponent
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [Resizable] modifier.
 *
 * TODO(b/354723161): Add tests for minimum and maximum size.
 */
@RunWith(AndroidJUnit4::class)
class ResizableModifierTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun resizable_noComponentByDefault() {
        composeTestRule.setSubspaceContent {
            SpatialPanel(SubspaceModifier.testTag("panel")) { Text(text = "Panel") }
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
    fun resizable_componentIsNotNullAndOnlyContainsSingleResizable() {
        composeTestRule.setSubspaceContent {
            SpatialPanel(SubspaceModifier.testTag("panel").resizable()) { Text(text = "Panel") }
        }

        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_modifierIsDisabledAndComponentDoesNotExist() {
        composeTestRule.setSubspaceContent {
            SpatialPanel(SubspaceModifier.testTag("panel").resizable(enabled = false)) {
                Text(text = "Panel")
            }
        }

        assertResizableComponentDoesNotExist()
    }

    @Test
    fun resizable_modifierDoesNotChangeAndComponentDoesNotUpdate() {
        composeTestRule.setSubspaceContent {
            var panelWidth by remember { mutableStateOf(50.dp) }
            SpatialPanel(
                SubspaceModifier.testTag("panel").width(panelWidth).resizable(enabled = true)
            ) {
                Button(modifier = Modifier.testTag("button"), onClick = { panelWidth += 50.dp }) {
                    Text(text = "Click to change width")
                }
            }
        }

        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After recompose we should continue to have the same component.
        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_modifierEnabledToDisabledAndComponentUpdates() {
        composeTestRule.setSubspaceContent {
            var resizableEnabled by remember { mutableStateOf(true) }
            SpatialPanel(SubspaceModifier.testTag("panel").resizable(enabled = resizableEnabled)) {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = { resizableEnabled = !resizableEnabled },
                ) {
                    Text(text = "Click to change resizable")
                }
            }
        }

        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After recompose no Components should exist.
        assertResizableComponentDoesNotExist()
    }

    @Test
    fun resizable_modifierOnSizeChangeUpdateAndComponentUpdates() {
        composeTestRule.setSubspaceContent {
            var onSizeReturnValue by remember { mutableStateOf(true) }
            SpatialPanel(
                SubspaceModifier.testTag("panel")
                    .resizable(enabled = true, onSizeChange = { onSizeReturnValue })
            ) {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = { onSizeReturnValue = !onSizeReturnValue },
                ) {
                    Text(text = "Click to change onSizeChange")
                }
            }
        }

        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After recompose we should still have one Component.
        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_modifierDisableWithOnSizeChangeUpdateAndComponentRemoved() {
        composeTestRule.setSubspaceContent {
            var resizableEnabled by remember { mutableStateOf(true) }
            var onSizeReturnValue by remember { mutableStateOf(true) }
            SpatialPanel(
                SubspaceModifier.testTag("panel")
                    .resizable(enabled = resizableEnabled, onSizeChange = { onSizeReturnValue })
            ) {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = {
                        resizableEnabled = !resizableEnabled
                        onSizeReturnValue = !onSizeReturnValue
                    },
                ) {
                    Text(text = "Click to change resizable and onSizeChange")
                }
            }
        }

        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After recompose Component should be removed.
        assertResizableComponentDoesNotExist()
    }

    @Test
    fun resizable_modifierEnabledWithOnSizeChangeUpdateAndComponentUpdates() {
        composeTestRule.setSubspaceContent {
            var resizableEnabled by remember { mutableStateOf(false) }
            var onSizeReturnValue by remember { mutableStateOf(true) }
            SpatialPanel(
                SubspaceModifier.testTag("panel")
                    .resizable(enabled = resizableEnabled, onSizeChange = { onSizeReturnValue })
            ) {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = {
                        resizableEnabled = !resizableEnabled
                        onSizeReturnValue = !onSizeReturnValue
                    },
                ) {
                    Text(text = "Click to change resizable and onSizeChange")
                }
            }
        }

        assertResizableComponentDoesNotExist()

        composeTestRule.onNodeWithTag("button").performClick()

        // After recompose Component should exist and be attached.
        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_modifierDisabledThenEnabledAndComponentUpdates() {
        composeTestRule.setSubspaceContent {
            var resizableEnabled by remember { mutableStateOf(true) }
            SpatialPanel(SubspaceModifier.testTag("panel").resizable(enabled = resizableEnabled)) {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = { resizableEnabled = !resizableEnabled },
                ) {
                    Text(text = "Click to change resizable")
                }
            }
        }

        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After disabled, recompose Component should not exist.
        assertResizableComponentDoesNotExist()

        composeTestRule.onNodeWithTag("button").performClick()

        // After enabled, recompose Component should be attached.
        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_modifierOnSizeChangeTwiceUpdateAndComponentUpdates() {
        composeTestRule.setSubspaceContent {
            var onSizeReturnValue by remember { mutableStateOf(true) }
            SpatialPanel(
                SubspaceModifier.testTag("panel")
                    .resizable(enabled = true, onSizeChange = { onSizeReturnValue })
            ) {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = { onSizeReturnValue = !onSizeReturnValue },
                ) {
                    Text(text = "Click to change onSizeChange")
                }
            }
        }

        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After recompose should only have one Component.
        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After recompose should only have one Component.
        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_modifierDisabledThenEnabledWithOnSizeChangeUpdateAndComponentUpdates() {
        composeTestRule.setSubspaceContent {
            var resizableEnabled by remember { mutableStateOf(true) }
            var onSizeReturnValue by remember { mutableStateOf(true) }
            SpatialPanel(
                SubspaceModifier.testTag("panel")
                    .resizable(enabled = resizableEnabled, onSizeChange = { onSizeReturnValue })
            ) {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = {
                        resizableEnabled = !resizableEnabled
                        onSizeReturnValue = !onSizeReturnValue
                    },
                ) {
                    Text(text = "Click to change resizabe and onSizeChange")
                }
            }
        }

        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After disabled, recompose removes Component.
        assertResizableComponentDoesNotExist()

        composeTestRule.onNodeWithTag("button").performClick()

        // After enabled, recompose removes Component.
        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_modifierEnabledThenDisabledWithOnSizeChangeUpdateAndComponentUpdates() {
        composeTestRule.setSubspaceContent {
            var resizableEnabled by remember { mutableStateOf(false) }
            var onSizeReturnValue by remember { mutableStateOf(true) }
            SpatialPanel(
                SubspaceModifier.testTag("panel")
                    .resizable(enabled = resizableEnabled, onSizeChange = { onSizeReturnValue })
            ) {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = {
                        resizableEnabled = !resizableEnabled
                        onSizeReturnValue = !onSizeReturnValue
                    },
                ) {
                    Text(text = "Click to change resizabe and onSizeChange")
                }
            }
        }

        assertResizableComponentDoesNotExist()

        composeTestRule.onNodeWithTag("button").performClick()

        // After enabled, recompose removes Component.
        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After disabled, recompose removes Component.
        assertResizableComponentDoesNotExist()
    }

    @Test
    fun resizable_modifierMaxSizeIsSet() {
        val maxSize = DpVolumeSize(500.dp, 500.dp, 500.dp)
        composeTestRule.setSubspaceContent {
            SpatialPanel(SubspaceModifier.testTag("panel").resizable(maximumSize = maxSize)) {}
        }
        assertResizableComponentMaxSizeIsSet(size = maxSize)
    }

    @Test
    fun resizable_modifierMaxSizeIsNotSet() {
        composeTestRule.setSubspaceContent {
            SpatialPanel(SubspaceModifier.testTag("panel").resizable()) {}
        }
        assertResizableComponentMaxSizeIsNotSet()
    }

    @Test
    fun resizable_modifierMinSizeIsSet() {
        val minSize = DpVolumeSize(100.dp, 100.dp, 100.dp)
        composeTestRule.setSubspaceContent {
            SpatialPanel(SubspaceModifier.testTag("panel").resizable(minimumSize = minSize)) {}
        }
        assertResizableComponentMinSizeIsSet(size = minSize)
    }

    @Test
    fun resizable_modifierMinSizeIsNotSet() {
        composeTestRule.setSubspaceContent {
            SpatialPanel(SubspaceModifier.testTag("panel").resizable()) {}
        }
        assertResizableComponentMinSizeIsNotSet()
    }

    @Test
    fun resizable_columnEntity_noComponentByDefault() {
        composeTestRule.setSubspaceContent {
            SpatialColumn(SubspaceModifier.testTag("column")) {
                SpatialPanel { Text(text = "Column") }
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
    fun resizable_columnEntity_noComponentWhenResizableIsEnabled() {
        composeTestRule.setSubspaceContent {
            SpatialColumn(SubspaceModifier.testTag("column").resizable()) {
                SpatialPanel { Text(text = "Column") }
            }
        }
        assertResizableComponentDoesNotExist("column")
    }

    @Test
    fun resizable_columnEntity_noComponentWhenResizableIsDisabled() {
        composeTestRule.setSubspaceContent {
            SpatialColumn(SubspaceModifier.testTag("column").resizable(false)) {
                SpatialPanel { Text(text = "Column") }
            }
        }
        assertResizableComponentDoesNotExist("column")
    }

    @Test
    fun resizable_rowEntity_noComponentByDefault() {
        composeTestRule.setSubspaceContent {
            SpatialRow(SubspaceModifier.testTag("row")) { SpatialPanel { Text(text = "Row") } }
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
    fun resizable_rowEntity_noComponentWhenResizableIsEnabled() {
        composeTestRule.setSubspaceContent {
            SpatialRow(SubspaceModifier.testTag("row").resizable()) {
                SpatialPanel { Text(text = "Row") }
            }
        }
        assertResizableComponentDoesNotExist("row")
    }

    @Test
    fun resizable_rowEntity_noComponentWhenResizableIsDisabled() {
        composeTestRule.setSubspaceContent {
            SpatialRow(SubspaceModifier.testTag("row").resizable(false)) {
                SpatialPanel { Text(text = "Row") }
            }
        }
        assertResizableComponentDoesNotExist("row")
    }

    @Test
    fun resizable_volumeEntity_noComponentByDefault() {
        composeTestRule.setSubspaceContent { Volume(SubspaceModifier.testTag("volume")) {} }
        assertTrue(
            composeTestRule
                .onSubspaceNodeWithTag("volume")
                .fetchSemanticsNode()
                .components
                .isNullOrEmpty()
        )
    }

    @Test
    fun resizable_volumeEntity_noComponentWhenResizableIsEnabled() {
        composeTestRule.setSubspaceContent {
            Volume(SubspaceModifier.testTag("volume").resizable()) {}
        }
        assertResizableComponentDoesNotExist("volume")
    }

    @Test
    fun resizable_volumeEntity_noComponentWhenResizableIsDisabled() {
        composeTestRule.setSubspaceContent {
            Volume(SubspaceModifier.testTag("volume").resizable(false)) {}
        }
        assertResizableComponentDoesNotExist("volume")
    }

    private fun assertSingleResizableComponentExists(testTag: String = "panel") {
        val components =
            composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(1, components.size)
        assertIs<ResizableComponent>(components[0])
    }

    private fun assertResizableComponentDoesNotExist(testTag: String = "panel") {
        val components =
            composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(0, components.size)
    }

    private fun assertResizableComponentMaxSizeIsSet(
        testTag: String = "panel",
        size: DpVolumeSize
    ) {
        val resizableComponent =
            composeTestRule
                .onSubspaceNodeWithTag(testTag)
                .fetchSemanticsNode()
                .getLastComponent<ResizableComponent>()

        val maxWidth = resizableComponent.maximumSize.width.meters.toDp()
        val maxHeight = resizableComponent.maximumSize.height.meters.toDp()

        assertEquals(size.width, maxWidth)
        assertEquals(size.height, maxHeight)
    }

    private fun assertResizableComponentMaxSizeIsNotSet(testTag: String = "panel") {
        val resizableComponent =
            composeTestRule
                .onSubspaceNodeWithTag(testTag)
                .fetchSemanticsNode()
                .getLastComponent<ResizableComponent>()

        val maxWidth = resizableComponent.maximumSize.width.meters.toDp()
        val maxHeight = resizableComponent.maximumSize.height.meters.toDp()

        assertEquals(Dp.Infinity, maxWidth)
        assertEquals(Dp.Infinity, maxHeight)
    }

    private fun assertResizableComponentMinSizeIsSet(
        testTag: String = "panel",
        size: DpVolumeSize
    ) {
        val resizableComponent =
            composeTestRule
                .onSubspaceNodeWithTag(testTag)
                .fetchSemanticsNode()
                .getLastComponent<ResizableComponent>()

        val minWidth = resizableComponent.minimumSize.width.meters.toDp()
        val minHeight = resizableComponent.minimumSize.height.meters.toDp()

        assertEquals(size.width, minWidth)
        assertEquals(size.height, minHeight)
    }

    private fun assertResizableComponentMinSizeIsNotSet(testTag: String = "panel") {
        val resizableComponent =
            composeTestRule
                .onSubspaceNodeWithTag(testTag)
                .fetchSemanticsNode()
                .getLastComponent<ResizableComponent>()

        val minWidth = resizableComponent.minimumSize.width.meters.toDp()
        val minHeight = resizableComponent.minimumSize.height.meters.toDp()

        assertEquals(DpVolumeSize.Zero.width, minWidth)
        assertEquals(DpVolumeSize.Zero.height, minHeight)
    }

    private inline fun <reified T> SubspaceSemanticsInfo.getLastComponent(): T {
        assertNotNull(components)
        val component = components!!.last()
        assertIs<T>(component)
        return component
    }
}
