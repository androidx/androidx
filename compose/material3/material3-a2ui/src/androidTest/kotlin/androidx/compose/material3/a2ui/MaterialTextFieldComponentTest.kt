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
import androidx.a2ui.compose.ui.testing.getData
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialTextFieldComponentTest {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialTextFieldComponent),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun noVariantSpecified_fallsBackToShortText() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties = mapOf("label" to "Default Field", "value" to "Sample Value"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Default Field").assertIsDisplayed()
        onNodeWithText("Sample Value").assertIsDisplayed()
    }

    @Test
    fun passedModifier_appliedToUnderlyingTextField() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties = mapOf("label" to "Tagged Field"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag"))
            }
        }

        onNode(hasText("Tagged Field") and hasTestTag("custom_tag")).assertIsDisplayed()
    }

    @Test
    fun dynamicLabelAndValue_rendersCorrectly() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf(
                        "label" to mapOf("path" to "/form/label"),
                        "value" to mapOf("path" to "/form/value"),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData =
                    mapOf("form" to mapOf("label" to "Full Name", "value" to "Alex Johnson")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Full Name").assertIsDisplayed()
        onNodeWithText("Alex Johnson").assertIsDisplayed()
    }

    @Test
    fun twoWayDataBinding_userTyping_updatesDataModel() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties = mapOf("label" to "Email", "value" to mapOf("path" to "/user/email")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("email" to "initial@example.com")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("initial@example.com").assertIsDisplayed()

        onNodeWithText("initial@example.com").performTextReplacement("updated@example.com")
        controller.waitForIdle()

        assertThat(controller.getData<String>("/user/email")).isEqualTo("updated@example.com")
    }

    @Test
    fun twoWayDataBinding_externalUpdate_reflectsInUi() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf("label" to "Address", "value" to mapOf("path" to "/user/address")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("address" to "123 Main St")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("123 Main St").assertIsDisplayed()

        controller.updateData("/user/address", "456 Market St")
        controller.waitForIdle()

        onNodeWithText("456 Market St").assertIsDisplayed()
    }

    @Test
    fun externalUpdate_shortenedValue_adjustsSelectionSafelyWithoutCrashing() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties = mapOf("label" to "Text", "value" to mapOf("path" to "/form/text")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("form" to mapOf("text" to "Long Initial String")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Long Initial String").assertIsDisplayed()

        controller.updateData("/form/text", "Short")
        controller.waitForIdle()

        onNodeWithText("Short").assertIsDisplayed()
    }

    @Test
    fun initialEmptyValue_rendersEmptyFieldAndAllowsTyping() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf("label" to "First Name", "value" to mapOf("path" to "/form/firstName")),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("First Name").assertIsDisplayed()

        onNodeWithText("First Name").performTextInput("Alice")
        controller.waitForIdle()

        assertThat(controller.getData<String>("/form/firstName")).isEqualTo("Alice")
    }

    @Test
    fun omittedValue_rendersLabelWithEmptyText() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties = mapOf("label" to "Search"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun staticValue_rendersCorrectly() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties = mapOf("label" to "Static Field", "value" to "Initial Value"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Initial Value").assertIsDisplayed()
    }

    @Test
    fun transitionsFromLoadingToSuccess_displaysTextField() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf("label" to mapOf("path" to "/config/label"), "value" to "Some value"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onLoading = { modifier ->
                        Text("Loading TextField...", modifier = modifier.testTag("custom_loader"))
                    },
                )
            }
        }

        onNodeWithTag("custom_loader").assertIsDisplayed()

        controller.updateData("/config/label", "Dynamic Label")
        controller.waitForIdle()

        onNodeWithTag("custom_loader").assertDoesNotExist()
        onNodeWithText("Dynamic Label").assertIsDisplayed()
        onNodeWithText("Some value").assertIsDisplayed()
    }

    @Test
    fun isReady_reactiveToDataAdditionAndRemoval() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf("label" to mapOf("path" to "/config/label"), "value" to "Bound Value"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Bound Value").assertDoesNotExist()

        controller.updateData("/config/label", "Form Label")
        controller.waitForIdle()

        onNodeWithText("Form Label").assertIsDisplayed()
        onNodeWithText("Bound Value").assertIsDisplayed()

        controller.updateData("/config/label", null)
        controller.waitForIdle()

        onNodeWithText("Form Label").assertDoesNotExist()
    }

    @Test
    fun isReady_emptyLabelString_remainsTrue() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties = mapOf("label" to "", "value" to "Input Text"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Input Text").assertIsDisplayed()
    }

    @Test
    fun evaluatesFormatExpression_rendersFormattedLabel() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf(
                        "label" to
                            mapOf(
                                "call" to "formatString",
                                "args" to mapOf("value" to "Field for \${/user/name}"),
                            ),
                        "value" to "Input",
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("name" to "Alice")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Field for Alice").assertIsDisplayed()
    }

    @Test
    fun staticLabelPropertyChange_updatesDisplayedLabel() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties = mapOf("label" to "Old Label", "value" to "Value"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Old Label").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "New Label", "value" to "Value"),
        )
        controller.waitForIdle()

        onNodeWithText("Old Label").assertDoesNotExist()
        onNodeWithText("New Label").assertIsDisplayed()
    }

    @Test
    fun staticToDynamicLabelChange_updatesDisplayedLabel() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties = mapOf("label" to "Static Label"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("label" to "Dynamic User Label")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Static Label").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to mapOf("path" to "/user/label")),
        )
        controller.waitForIdle()

        onNodeWithText("Static Label").assertDoesNotExist()
        onNodeWithText("Dynamic User Label").assertIsDisplayed()
    }

    @Test
    fun validationRegexp_validInput_hasNoError() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf(
                        "label" to "Zip Code",
                        "value" to "12345",
                        "validationRegexp" to "^[0-9]{5}$",
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("12345").assertIsDisplayed()
        onNodeWithText("12345").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    @Test
    fun validationRegexp_invalidInput_setsError() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf(
                        "label" to "Zip Code",
                        "value" to "invalid-zip",
                        "validationRegexp" to "^[0-9]{5}$",
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("invalid-zip").assertIsDisplayed()
        onNodeWithText("invalid-zip")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun validationRegexp_emptyValue_hasNoError() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf("label" to "Zip Code", "value" to "", "validationRegexp" to "^[0-9]{5}$"),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Zip Code").assertIsDisplayed()
        onNodeWithText("Zip Code").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    @Test
    fun validationRegexp_invalidRegexPattern_reportsError() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "TextField",
                properties =
                    mapOf(
                        "label" to "Field",
                        "value" to "Some Input",
                        "validationRegexp" to "[invalid(regex",
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onError = { exception, _ -> Text("Error: ${exception.message}") },
                )
            }
        }

        onNodeWithText("Error: Invalid validationRegexp '[invalid(regex'", substring = true)
            .assertIsDisplayed()
    }
}
