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

package androidx.a2ui.compose.ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiBasicCatalogV1DividerUiTest {

    private val testDivider =
        object : A2uiBasicCatalogV1.Divider {
            var capturedAxis: A2uiBasicCatalogV1.Divider.Axis? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                axis: A2uiBasicCatalogV1.Divider.Axis,
                modifier: Modifier,
            ) {
                SideEffect { capturedAxis = axis }
                BasicText(text = "Divider: ${axis.value}", modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testDivider),
            functions = emptyList(),
        )

    @Test
    fun isReady_unconditional_rendersContentImmediatelyWithoutLoading() = runComposeUiTest {
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
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("Divider: horizontal").assertIsDisplayed()
    }

    @Test
    fun content_staticData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
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

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Divider: vertical").assertIsDisplayed()
        assertThat(testDivider.capturedAxis).isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Vertical)
    }

    @Test
    fun content_explicitHorizontalAxis_resolvesAndPassesToTypedContent() = runComposeUiTest {
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

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Divider: horizontal").assertIsDisplayed()
        assertThat(testDivider.capturedAxis).isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Horizontal)
    }

    @Test
    fun content_omittedOptionalProperties_fallsBackToDefaults() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(id = "root", type = "Divider", properties = emptyMap())
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Divider: horizontal").assertIsDisplayed()
        assertThat(testDivider.capturedAxis).isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Horizontal)
    }

    @Test
    fun content_axis_resolvesAllValuesAndPassesToTypedContent() = runComposeUiTest {
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

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testDivider.capturedAxis).isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Horizontal)

        controller.updateComponent(id = "root", properties = mapOf("axis" to "vertical"))
        controller.waitForIdle()
        waitForIdle()

        assertThat(testDivider.capturedAxis).isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Vertical)
    }

    @Test
    fun content_axisChange_recomposesWithNewAxis() = runComposeUiTest {
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

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Divider: horizontal").assertIsDisplayed()
        assertThat(testDivider.capturedAxis).isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Horizontal)

        controller.updateComponent(id = "root", properties = mapOf("axis" to "vertical"))
        controller.waitForIdle()

        onNodeWithText("Divider: horizontal").assertDoesNotExist()
        onNodeWithText("Divider: vertical").assertIsDisplayed()
        assertThat(testDivider.capturedAxis).isEqualTo(A2uiBasicCatalogV1.Divider.Axis.Vertical)
    }

    @Test
    fun content_passedModifier_appliesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(id = "root", type = "Divider", properties = emptyMap())
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Divider: horizontal") and hasTestTag("custom_tag")).assertIsDisplayed()
    }

    @Test
    fun content_modifierChange_recomposesWithNewModifier() = runComposeUiTest {
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

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("Divider: horizontal") and hasTestTag("initial_tag")).assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("Divider: horizontal") and hasTestTag("updated_tag")).assertIsDisplayed()
    }
}
