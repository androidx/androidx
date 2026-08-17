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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MaterialDividerComponentTest {

    private val testCatalog =
        A2uiCatalog(catalogId = "test_catalog", components = listOf(MaterialDividerComponent))

    @Test
    fun axis_default_rendersHorizontalDivider() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(id = "root", type = "Divider", properties = emptyMap())
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("divider"))
            }
        }

        onNodeWithTag("divider")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(DividerDefaults.Thickness)
    }

    @Test
    fun axis_horizontal_rendersHorizontalDivider() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Divider",
                            properties = mapOf("axis" to "horizontal"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("divider"))
            }
        }

        onNodeWithTag("divider")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(DividerDefaults.Thickness)
    }

    @Test
    fun axis_vertical_rendersVerticalDivider() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Divider",
                            properties = mapOf("axis" to "vertical"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("divider"))
            }
        }

        onNodeWithTag("divider").assertIsDisplayed().assertWidthIsEqualTo(DividerDefaults.Thickness)
    }

    @Test
    fun axisChanges_horizontalToVertical_rendersVerticalDivider() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Divider",
                            properties = mapOf("axis" to "horizontal"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("divider"))
            }
        }

        onNodeWithTag("divider").assertHeightIsEqualTo(DividerDefaults.Thickness)

        controller.updateComponent(id = "root", properties = mapOf("axis" to "vertical"))
        controller.waitForIdle()

        onNodeWithTag("divider").assertWidthIsEqualTo(DividerDefaults.Thickness)
    }

    @Test
    fun axisChanges_verticalToHorizontal_rendersHorizontalDivider() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Divider",
                            properties = mapOf("axis" to "vertical"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("divider"))
            }
        }

        onNodeWithTag("divider").assertWidthIsEqualTo(DividerDefaults.Thickness)

        controller.updateComponent(id = "root", properties = mapOf("axis" to "horizontal"))
        controller.waitForIdle()

        onNodeWithTag("divider").assertHeightIsEqualTo(DividerDefaults.Thickness)
    }

    @Test
    fun modifier_parentModifier_isApplied() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(id = "root", type = "Divider", properties = emptyMap())
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag"))
            }
        }

        onNodeWithTag("custom_tag").assertIsDisplayed()
    }

    @Test
    fun modifierParameterChanges_updatesRenderedModifier() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(id = "root", type = "Divider", properties = emptyMap())
                    ),
            )
        val surface = controller.start()

        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { MaterialTheme { A2uiTestSurface(surface = surface, modifier = modifier) } }

        onNodeWithTag("initial_tag").assertIsDisplayed()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNodeWithTag("initial_tag").assertDoesNotExist()
        onNodeWithTag("updated_tag").assertIsDisplayed()
    }
}
