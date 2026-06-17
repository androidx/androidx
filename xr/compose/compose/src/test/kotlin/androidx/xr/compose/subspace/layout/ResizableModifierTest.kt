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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.scenecore.ResizableComponent
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceModifier.resizable] modifier. */
@RunWith(AndroidJUnit4::class)
class ResizableModifierTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun resizable_noComponentByDefault() {
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
    fun resizable_componentIsNotNullAndOnlyContainsSingleResizable() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .resizable(resizePolicy = ResizePolicy.custom {})
                ) {
                    Text(text = "Panel")
                }
            }
        }

        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_modifierEnabledToDisabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var resizableEnabled by remember { mutableStateOf(true) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel").resizable(enabled = resizableEnabled)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { resizableEnabled = !resizableEnabled },
                    ) {
                        Text(text = "Click to change resizable")
                    }
                }
            }
        }

        assertSingleResizableComponentExists()

        composeTestRule.onNodeWithTag("button").performClick()

        // After recompose no Components should exist.
        assertResizableComponentDoesNotExist()
    }

    @Test
    fun resizable_modifierDisabledThenEnabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var resizableEnabled by remember { mutableStateOf(false) }
                SpatialPanel(
                    SubspaceModifier.testTag("panel").resizable(enabled = resizableEnabled)
                ) {
                    Button(
                        modifier = Modifier.testTag("button"),
                        onClick = { resizableEnabled = !resizableEnabled },
                    ) {
                        Text(text = "Click to change resizable")
                    }
                }
            }
        }

        assertResizableComponentDoesNotExist()

        composeTestRule.onNodeWithTag("button").performClick()

        // After enabled, recompose Component should be attached.
        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_columnEntity_oneComponentWhenResizableIsEnabled() {
        composeTestRule.setContent {
            Subspace {
                SpatialColumn(
                    SubspaceModifier.testTag("column")
                        .resizable(resizePolicy = ResizePolicy.custom {})
                ) {
                    SpatialPanel { Text(text = "Column") }
                }
            }
        }
        assertResizableComponentDoesExist("column")
    }

    @Test
    fun resizable_rowEntity_oneComponentWhenResizableIsEnabled() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    SubspaceModifier.testTag("row").resizable(resizePolicy = ResizePolicy.custom {})
                ) {
                    SpatialPanel { Text(text = "Row") }
                }
            }
        }
        assertResizableComponentDoesExist("row")
    }

    @Test
    fun resizable_modifierMaxSizeIsSet() {
        val maxSize = DpVolumeSize(500.dp, 500.dp, 500.dp)
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .resizable(maximumSize = maxSize, resizePolicy = ResizePolicy.custom {})
                ) {}
            }
        }
        assertResizableComponentMaxSizeIsSet(size = maxSize)
    }

    @Test
    fun resizable_modifierMaxSizeIsNotSet() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .resizable(resizePolicy = ResizePolicy.custom {})
                ) {}
            }
        }
        assertResizableComponentMaxSizeIsNotSet()
    }

    @Test
    fun resizable_modifierMinSizeIsSet() {
        val minSize = DpVolumeSize(100.dp, 100.dp, 100.dp)
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .resizable(minimumSize = minSize, resizePolicy = ResizePolicy.custom {})
                ) {}
            }
        }
        assertResizableComponentMinSizeIsSet(size = minSize)
    }

    @Test
    fun resizable_modifierMinSizeIsNotSet() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .resizable(resizePolicy = ResizePolicy.custom {})
                ) {}
            }
        }
        assertResizableComponentMinSizeIsNotSet()
    }

    @Test
    fun resizable_defaultPolicy_componentIsNotNull() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .resizable(resizePolicy = ResizePolicy.default())
                ) {
                    Text(text = "Panel")
                }
            }
        }

        assertSingleResizableComponentExists()
    }

    @Test
    fun resizable_defaultPolicy_modifierMaxSizeIsSet() {
        val maxSize = DpVolumeSize(600.dp, 600.dp, 600.dp)
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .resizable(maximumSize = maxSize, resizePolicy = ResizePolicy.default())
                ) {}
            }
        }
        assertResizableComponentMaxSizeIsSet(size = maxSize)
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

    private fun assertResizableComponentDoesExist(testTag: String = "panel") {
        val components =
            composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(1, components.size)
    }

    private fun assertResizableComponentMaxSizeIsSet(
        testTag: String = "panel",
        size: DpVolumeSize,
    ) {
        val resizableComponent =
            composeTestRule
                .onSubspaceNodeWithTag(testTag)
                .fetchSemanticsNode()
                .getLastComponent<ResizableComponent>()

        val maxWidth = resizableComponent.maximumEntitySize.width.meters.toDp()
        val maxHeight = resizableComponent.maximumEntitySize.height.meters.toDp()

        assertEquals(size.width, maxWidth)
        assertEquals(size.height, maxHeight)
    }

    private fun assertResizableComponentMaxSizeIsNotSet(testTag: String = "panel") {
        val resizableComponent =
            composeTestRule
                .onSubspaceNodeWithTag(testTag)
                .fetchSemanticsNode()
                .getLastComponent<ResizableComponent>()

        val maxWidth = resizableComponent.maximumEntitySize.width.meters.toDp()
        val maxHeight = resizableComponent.maximumEntitySize.height.meters.toDp()

        assertEquals(Dp.Infinity, maxWidth)
        assertEquals(Dp.Infinity, maxHeight)
    }

    private fun assertResizableComponentMinSizeIsSet(
        testTag: String = "panel",
        size: DpVolumeSize,
    ) {
        val resizableComponent =
            composeTestRule
                .onSubspaceNodeWithTag(testTag)
                .fetchSemanticsNode()
                .getLastComponent<ResizableComponent>()

        val minWidth = resizableComponent.minimumEntitySize.width.meters.toDp()
        val minHeight = resizableComponent.minimumEntitySize.height.meters.toDp()

        assertEquals(size.width, minWidth)
        assertEquals(size.height, minHeight)
    }

    private fun assertResizableComponentMinSizeIsNotSet(testTag: String = "panel") {
        val resizableComponent =
            composeTestRule
                .onSubspaceNodeWithTag(testTag)
                .fetchSemanticsNode()
                .getLastComponent<ResizableComponent>()

        val minWidth = resizableComponent.minimumEntitySize.width.meters.toDp()
        val minHeight = resizableComponent.minimumEntitySize.height.meters.toDp()

        assertEquals(DpVolumeSize.Zero.width, minWidth)
        assertEquals(DpVolumeSize.Zero.height, minHeight)
    }

    private inline fun <reified T> SubspaceSemanticsInfo.getLastComponent(): T {
        assertNotNull(components)
        val component = components!!.last()
        assertIs<T>(component)
        return component
    }

    @Test
    fun resizableModifier_policyEquality() {
        val defaultPolicy1 = ResizePolicy.default()
        val defaultPolicy2 = ResizePolicy.default()
        assertEquals(defaultPolicy1, defaultPolicy2)
        assertEquals(defaultPolicy1.hashCode(), defaultPolicy2.hashCode())

        val lambda = { _: SpatialResizeEvent -> }
        val customPolicy1 = ResizePolicy.custom(lambda)
        val customPolicy2 = ResizePolicy.custom(lambda)
        assertEquals(customPolicy1, customPolicy2)
        assertEquals(customPolicy1.hashCode(), customPolicy2.hashCode())

        val customPolicy3 = ResizePolicy.custom { _: SpatialResizeEvent -> }
        assertNotEquals(customPolicy1, customPolicy3)
    }

    @Test
    fun resizableModifier_elementEquality() {
        val modifier1 = SubspaceModifier.resizable()
        val modifier2 = SubspaceModifier.resizable()
        assertEquals(modifier1, modifier2)
        assertEquals(modifier1.hashCode(), modifier2.hashCode())

        val modifier3 =
            SubspaceModifier.resizable(
                resizePolicy = ResizePolicy.custom { _: SpatialResizeEvent -> }
            )
        assertNotEquals(modifier1, modifier3)
    }
}
