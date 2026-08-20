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
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
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
class A2uiBasicCatalogV1TextUiTest {

    private val testText =
        object : A2uiBasicCatalogV1.Text {
            var capturedVariant: A2uiBasicCatalogV1.Text.Variant? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                text: String,
                variant: A2uiBasicCatalogV1.Text.Variant,
                modifier: Modifier,
            ) {
                SideEffect { capturedVariant = variant }
                BasicText(text = text, modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testText),
            functions = emptyList(),
        )

    @Test
    fun isReady_pendingDynamicData_returnsFalseAndGuardsContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to mapOf("path" to "/pendingData")),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Data Arrived").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/pendingData", "Data Arrived")
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("Data Arrived").assertIsDisplayed()
    }

    @Test
    fun isReady_dynamicDataErased_transitionsFromReadyToPending() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to mapOf("path" to "/user/greeting")),
                        )
                    ),
                initialData = mapOf("user" to mapOf("greeting" to "Initial Greeting")),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Initial Greeting").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/user/greeting", null)
        controller.waitForIdle()

        onNodeWithText("Initial Greeting").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_emptyStaticText_returnsTrueAndRendersContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to ""),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("").assertExists()
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
                            type = "Text",
                            properties = mapOf("text" to "Hello World", "variant" to "h1"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Hello World").assertIsDisplayed()
        assertThat(testText.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Text.Variant.H1)
    }

    @Test
    fun content_dynamicData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties =
                                mapOf(
                                    "text" to mapOf("path" to "/user/greeting"),
                                    "variant" to "h1",
                                ),
                        )
                    ),
                initialData = mapOf("user" to mapOf("greeting" to "Hello World")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Hello World").assertIsDisplayed()
        assertThat(testText.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Text.Variant.H1)
    }

    @Test
    fun content_omittedVariant_fallsBackToBody() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to "Literal String"), // Omit variant
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Literal String").assertIsDisplayed()
        assertThat(testText.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Text.Variant.Body)
    }

    @Test
    fun content_functionExpression_evaluatesAndPassesToTypedContent() = runComposeUiTest {
        val catalogWithFunctions =
            A2uiCatalog(
                catalogId = "test_catalog",
                components = listOf(testText),
                functions = listOf(A2uiFormatStringFunction.INSTANCE),
            )
        val controller =
            A2uiTestController(
                catalog = catalogWithFunctions,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties =
                                mapOf(
                                    "text" to
                                        mapOf(
                                            "call" to "formatString",
                                            "args" to
                                                mapOf(
                                                    "value" to
                                                        "Hello \${/user/first} \${/user/last}!"
                                                ),
                                        )
                                ),
                        )
                    ),
                initialData = mapOf("user" to mapOf("first" to "Alice", "last" to "Smith")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Hello Alice Smith!").assertIsDisplayed()
    }

    @Test
    fun content_passedModifier_appliesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to "Tagged Text"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Tagged Text") and hasTestTag("custom_tag")).assertIsDisplayed()
    }

    @Test
    fun content_staticTextChange_recomposesWithNewText() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to "Old Static Title"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Old Static Title").assertIsDisplayed()

        controller.updateComponent(id = "root", properties = mapOf("text" to "New Static Title"))
        controller.waitForIdle()

        onNodeWithText("Old Static Title").assertDoesNotExist()
        onNodeWithText("New Static Title").assertIsDisplayed()
    }

    @Test
    fun content_dynamicTextChange_recomposesWithNewText() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to mapOf("path" to "/user/message")),
                        )
                    ),
                initialData = mapOf("user" to mapOf("message" to "Initial Message")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Initial Message").assertIsDisplayed()

        controller.updateData("/user/message", "Updated Message")
        controller.waitForIdle()

        onNodeWithText("Initial Message").assertDoesNotExist()
        onNodeWithText("Updated Message").assertIsDisplayed()
    }

    @Test
    fun content_staticToDynamicTextChange_recomposesWithNewText() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to "Static Placeholder"),
                        )
                    ),
                initialData = mapOf("user" to mapOf("name" to "Dynamic User Name")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Static Placeholder").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("text" to mapOf("path" to "/user/name")),
        )
        controller.waitForIdle()

        onNodeWithText("Static Placeholder").assertDoesNotExist()
        onNodeWithText("Dynamic User Name").assertIsDisplayed()
    }

    @Test
    fun content_variantChange_recomposesWithNewVariant() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to "Sample Text", "variant" to "h1"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Sample Text").assertIsDisplayed()
        assertThat(testText.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Text.Variant.H1)

        controller.updateComponent(
            id = "root",
            properties = mapOf("text" to "Sample Text", "variant" to "h2"),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testText.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Text.Variant.H2)
    }

    @Test
    fun content_modifierChange_recomposesWithNewModifier() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to "Sample Text"),
                        )
                    ),
            )
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("Sample Text") and hasTestTag("initial_tag")).assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("Sample Text") and hasTestTag("updated_tag")).assertIsDisplayed()
    }
}
