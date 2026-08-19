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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialA2uiBasicCatalogV1TextTest {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1Defaults.text),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun noVariantSpecified_fallsBackToBody() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to "Default Fallback Text"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(textPayload))
        val surface = controller.start()
        lateinit var expectedFallbackStyle: TextStyle

        setContent {
            MaterialTheme {
                expectedFallbackStyle = MaterialTheme.typography.bodyLarge
                A2uiTestSurface(surface)
            }
        }

        onNodeWithText("Default Fallback Text").assertIsDisplayed()
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithText("Default Fallback Text").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) { action ->
            action(results)
        }
        val actualStyle = results.firstOrNull()?.layoutInput?.style
        assertThat(actualStyle?.fontSize).isEqualTo(expectedFallbackStyle.fontSize)
        assertThat(actualStyle?.fontWeight).isEqualTo(expectedFallbackStyle.fontWeight)
        assertThat(actualStyle?.lineHeight).isEqualTo(expectedFallbackStyle.lineHeight)
    }

    @Test
    fun passedModifier_appliedToUnderlyingText() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to "Tagged Text"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(textPayload))
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag"))
            }
        }

        onNode(hasText("Tagged Text") and hasTestTag("custom_tag")).assertIsDisplayed()
    }

    @Test
    fun dynamicText_rendersText() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "/user/display_name")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(textPayload),
                initialData = mapOf("user" to mapOf("display_name" to "Welcome, Developer!")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Welcome, Developer!").assertIsDisplayed()
    }

    @Test
    fun transitionsFromLoadingToSuccess_displaysText() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "/user/display_name")),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(textPayload))
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onLoading = { modifier ->
                        Text("Loading Text...", modifier = modifier.testTag("custom_loader"))
                    },
                )
            }
        }

        onNodeWithTag("custom_loader").assertIsDisplayed()

        controller.updateData("/user/display_name", "Developer")
        controller.waitForIdle()

        onNodeWithTag("custom_loader").assertDoesNotExist()
        onNodeWithText("Developer").assertIsDisplayed()
    }

    @Test
    fun isReady_reactiveToDataAdditionAndRemoval() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "/user/display_name")),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(textPayload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Developer").assertDoesNotExist()

        controller.updateData("/user/display_name", "Developer")
        controller.waitForIdle()

        onNodeWithText("Developer").assertIsDisplayed()

        controller.updateData("/user/display_name", null)
        controller.waitForIdle()

        onNodeWithText("Developer").assertDoesNotExist()
    }

    @Test
    fun isReady_remainsTrueWhenTextIsEmptyString() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(id = "root", type = "Text", properties = mapOf("text" to ""))
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(textPayload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("").assertExists()
    }

    @Test
    fun evaluatesFormatExpression_rendersFormattedText() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties =
                    mapOf(
                        "text" to
                            mapOf(
                                "call" to "formatString",
                                "args" to mapOf("value" to "Hello \${/user/first} \${/user/last}!"),
                            )
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(textPayload),
                initialData = mapOf("user" to mapOf("first" to "Alice", "last" to "Smith")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Hello Alice Smith!").assertIsDisplayed()
    }

    @Test
    fun variantChanges_rendersUpdatedComponent() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to "Headline Text", "variant" to "h1"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(textPayload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Headline Text").assertIsDisplayed()

        // 1. Assert Heading semantics key is defined when variant is H1
        onNodeWithText("Headline Text")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        controller.updateComponent(
            id = "root",
            properties = mapOf("text" to "Headline Text", "variant" to "body"),
        )
        controller.waitForIdle()

        onNodeWithText("Headline Text").assertIsDisplayed()
        // 2. Assert Heading semantics key is NOT defined when variant is body
        onNodeWithText("Headline Text")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
    }

    @Test
    fun staticTextPropertyChange_updatesDisplayedText() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to "Old Static Title"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(textPayload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Old Static Title").assertIsDisplayed()

        controller.updateComponent(id = "root", properties = mapOf("text" to "New Static Title"))
        controller.waitForIdle()

        onNodeWithText("Old Static Title").assertDoesNotExist()
        onNodeWithText("New Static Title").assertIsDisplayed()
    }

    @Test
    fun staticToDynamicTextChange_updatesDisplayedText() = runComposeUiTest {
        val textPayload =
            A2uiComponentPayload(
                id = "root",
                type = "Text",
                properties = mapOf("text" to "Static Placeholder"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(textPayload),
                initialData = mapOf("user" to mapOf("name" to "Dynamic User Name")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Static Placeholder").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("text" to mapOf("path" to "/user/name")),
        )
        controller.waitForIdle()

        onNodeWithText("Static Placeholder").assertDoesNotExist()
        onNodeWithText("Dynamic User Name").assertIsDisplayed()
    }
}
