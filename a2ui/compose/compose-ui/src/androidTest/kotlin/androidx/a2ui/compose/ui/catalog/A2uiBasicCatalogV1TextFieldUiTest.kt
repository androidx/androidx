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
import androidx.a2ui.compose.ui.testing.getData
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiBasicCatalogV1TextFieldUiTest {

    private class TestTextField : A2uiBasicCatalogV1.TextField {
        var capturedLabel: String? = null
        var capturedValue: String? = null
        var capturedVariant: A2uiBasicCatalogV1.TextField.Variant? = null
        var capturedValidationRegexp: String? = null
        var capturedOnValueChange: ((String) -> Unit)? = null
        var capturedEnabled: Boolean? = null

        @Composable
        override fun A2uiComponentScope.TypedContent(
            label: String,
            value: String?,
            variant: A2uiBasicCatalogV1.TextField.Variant,
            validationRegexp: String?,
            onValueChange: (String) -> Unit,
            enabled: Boolean,
            modifier: Modifier,
        ) {
            SideEffect {
                capturedLabel = label
                capturedValue = value
                capturedVariant = variant
                capturedValidationRegexp = validationRegexp
                capturedOnValueChange = onValueChange
                capturedEnabled = enabled
            }
            val valStr = value ?: "<null>"
            val regexpStr = if (validationRegexp != null) " [$validationRegexp]" else ""
            val readOnlyStr = if (!enabled) " [RO]" else ""
            BasicText(
                text = "TextField: $label = $valStr (${variant.value})$regexpStr$readOnlyStr",
                modifier =
                    modifier
                        .clickable(
                            enabled = enabled,
                            onClick = {
                                onValueChange(
                                    if (value != null) "${value}_updated" else "new_value"
                                )
                            },
                        )
                        .testTag("text_field_tag"),
            )
        }
    }

    private val testTextField = TestTextField()

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testTextField),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun isReady_pendingDynamicLabel_returnsFalseAndGuardsContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/pendingLabel"),
                                    "value" to "John",
                                ),
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

        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/pendingLabel", "First Name")
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("TextField: First Name = John (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("First Name")
    }

    @Test
    fun isReady_pendingOptionalDynamicValue_returnsTrueAndRendersWithNullValue() =
        runComposeUiTest {
            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(
                            A2uiComponentPayload(
                                id = "root",
                                type = "TextField",
                                properties =
                                    mapOf(
                                        "label" to "Username",
                                        "value" to mapOf("path" to "/pendingValue"),
                                    ),
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

            onNodeWithText("Loading...").assertDoesNotExist()
            onNodeWithText("TextField: Username = <null> (shortText)").assertIsDisplayed()
            assertThat(testTextField.capturedLabel).isEqualTo("Username")
            assertThat(testTextField.capturedValue).isNull()
            assertThat(testTextField.capturedEnabled).isTrue()

            controller.updateData("/pendingValue", "alice")
            controller.waitForIdle()

            onNodeWithText("TextField: Username = alice (shortText)").assertIsDisplayed()
            assertThat(testTextField.capturedValue).isEqualTo("alice")
        }

    @Test
    fun isReady_dynamicLabelErased_transitionsFromReadyToPending() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/form/label"),
                                    "value" to "Initial Value",
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Address")),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("TextField: Address = Initial Value (shortText) [RO]").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/form/label", null)
        controller.waitForIdle()

        onNodeWithText("TextField: Address = Initial Value (shortText) [RO]").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_emptyStaticLabel_returnsTrueAndRendersContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties = mapOf("label" to ""),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField:  = <null> (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("")
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
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Email Address",
                                    "value" to "user@example.com",
                                    "variant" to "shortText",
                                    "validationRegexp" to "^[a-z]+@[a-z]+\\.[a-z]+$",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText(
                "TextField: Email Address = user@example.com (shortText) [^[a-z]+@[a-z]+\\.[a-z]+$] [RO]"
            )
            .assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Email Address")
        assertThat(testTextField.capturedValue).isEqualTo("user@example.com")
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.ShortText)
        assertThat(testTextField.capturedValidationRegexp).isEqualTo("^[a-z]+@[a-z]+\\.[a-z]+$")
        assertThat(testTextField.capturedEnabled).isFalse()
        assertThat(testTextField.capturedOnValueChange).isNotNull()
    }

    @Test
    fun content_omittedOptionalProperties_fallsBackToDefaults() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties = mapOf("label" to "Comments"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Comments = <null> (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Comments")
        assertThat(testTextField.capturedValue).isNull()
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.ShortText)
        assertThat(testTextField.capturedValidationRegexp).isNull()
        assertThat(testTextField.capturedEnabled).isFalse()
    }

    @Test
    fun content_emptyStaticValue_passesEmptyStringToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties = mapOf("label" to "Notes", "value" to ""),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Notes =  (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedValue).isEqualTo("")
    }

    @Test
    fun content_omittedVariant_fallsBackToDefault() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Custom",
                                    "value" to "123",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Custom = 123 (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.ShortText)
    }

    @Test
    fun content_variant_resolvesAllValuesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Field",
                                    "value" to "test",
                                    "variant" to "shortText",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.ShortText)

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "label" to "Field",
                    "value" to "test",
                    "variant" to "longText",
                ),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.LongText)

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "label" to "Field",
                    "value" to "test",
                    "variant" to "number",
                ),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.Number)

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "label" to "Field",
                    "value" to "test",
                    "variant" to "obscured",
                ),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.Obscured)
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
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/form/label"),
                                    "value" to mapOf("path" to "/form/value"),
                                    "variant" to "longText",
                                    "validationRegexp" to ".*",
                                ),
                        )
                    ),
                initialData =
                    mapOf(
                        "form" to
                            mapOf("label" to "Biography", "value" to "Mobile application developer")
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Biography = Mobile application developer (longText) [.*]")
            .assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Biography")
        assertThat(testTextField.capturedValue).isEqualTo("Mobile application developer")
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.LongText)
        assertThat(testTextField.capturedValidationRegexp).isEqualTo(".*")
        assertThat(testTextField.capturedEnabled).isTrue()
        assertThat(testTextField.capturedOnValueChange).isNotNull()
    }

    @Test
    fun content_functionExpression_evaluatesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to
                                        mapOf(
                                            "call" to "formatString",
                                            "args" to
                                                mapOf("value" to "Greeting: \${/profile/name}"),
                                        ),
                                    "value" to "Hello World",
                                ),
                        )
                    ),
                initialData = mapOf("profile" to mapOf("name" to "Antigravity")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Greeting: Antigravity = Hello World (shortText) [RO]")
            .assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Greeting: Antigravity")
    }

    @Test
    fun content_onValueChange_updatesDataModel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Username",
                                    "value" to mapOf("path" to "/user/username"),
                                ),
                        )
                    ),
                initialData = mapOf("user" to mapOf("username" to "alice")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Username = alice (shortText)").assertIsDisplayed()

        onNodeWithText("TextField: Username = alice (shortText)").performClick()
        controller.waitForIdle()

        onNodeWithText("TextField: Username = alice (shortText)").assertDoesNotExist()
        onNodeWithText("TextField: Username = alice_updated (shortText)").assertIsDisplayed()

        val dataModelVal = controller.getData<String>("/user/username")
        assertThat(dataModelVal).isEqualTo("alice_updated")
    }

    @Test
    fun content_onValueChange_whenInitialValueIsNull_updatesDataModel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Bio",
                                    "value" to mapOf("path" to "/profile/bio"),
                                ),
                        )
                    ),
                initialData = mapOf("profile" to emptyMap<String, Any?>()),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Bio = <null> (shortText)").assertIsDisplayed()

        onNodeWithText("TextField: Bio = <null> (shortText)").performClick()
        controller.waitForIdle()

        onNodeWithText("TextField: Bio = <null> (shortText)").assertDoesNotExist()
        onNodeWithText("TextField: Bio = new_value (shortText)").assertIsDisplayed()

        val dataModelVal = controller.getData<String>("/profile/bio")
        assertThat(dataModelVal).isEqualTo("new_value")
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
                            type = "TextField",
                            properties = mapOf("label" to "Tagged Field", "value" to "val"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(
                hasText("TextField: Tagged Field = val (shortText) [RO]") and
                    hasTestTag("custom_tag")
            )
            .assertIsDisplayed()
    }

    @Test
    fun content_propertyUpdates_recomposesWithNewValues() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Initial Label",
                                    "value" to "Initial Value",
                                    "variant" to "shortText",
                                    "validationRegexp" to "^[0-9]+$",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Initial Label = Initial Value (shortText) [^[0-9]+$] [RO]")
            .assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "label" to "Updated Label",
                    "value" to "Updated Value",
                    "variant" to "longText",
                    "validationRegexp" to "^[a-z]+$",
                ),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Initial Label = Initial Value (shortText) [^[0-9]+$] [RO]")
            .assertDoesNotExist()
        onNodeWithText("TextField: Updated Label = Updated Value (longText) [^[a-z]+$] [RO]")
            .assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Updated Label")
        assertThat(testTextField.capturedValue).isEqualTo("Updated Value")
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.LongText)
        assertThat(testTextField.capturedValidationRegexp).isEqualTo("^[a-z]+$")
    }

    @Test
    fun content_staticLabelChange_recomposesWithNewLabel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties = mapOf("label" to "Old Label", "value" to "val"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Old Label = val (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Old Label")

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "New Label", "value" to "val"),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Old Label = val (shortText) [RO]").assertDoesNotExist()
        onNodeWithText("TextField: New Label = val (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("New Label")
    }

    @Test
    fun content_dynamicLabelChange_recomposesWithNewLabel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/form/label"),
                                    "value" to "val",
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Old Label")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Old Label = val (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Old Label")

        controller.updateData("/form/label", "New Label")
        controller.waitForIdle()

        onNodeWithText("TextField: Old Label = val (shortText) [RO]").assertDoesNotExist()
        onNodeWithText("TextField: New Label = val (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("New Label")
    }

    @Test
    fun content_staticToDynamicLabelChange_recomposesWithNewLabel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties = mapOf("label" to "Static Label", "value" to "v"),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Dynamic Label")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Static Label = v (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Static Label")

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "label" to mapOf("path" to "/form/label"),
                    "value" to "v",
                ),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Static Label = v (shortText) [RO]").assertDoesNotExist()
        onNodeWithText("TextField: Dynamic Label = v (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Dynamic Label")
    }

    @Test
    fun content_dynamicToStaticLabelChange_recomposesWithNewLabel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/form/label"),
                                    "value" to "v",
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Dynamic Label")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Dynamic Label = v (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Dynamic Label")

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Static Label", "value" to "v"),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Dynamic Label = v (shortText) [RO]").assertDoesNotExist()
        onNodeWithText("TextField: Static Label = v (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedLabel).isEqualTo("Static Label")
    }

    @Test
    fun content_staticValueChange_recomposesWithNewValue() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties = mapOf("label" to "Title", "value" to "Old Value"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Title = Old Value (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedValue).isEqualTo("Old Value")

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Title", "value" to "New Value"),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Title = Old Value (shortText) [RO]").assertDoesNotExist()
        onNodeWithText("TextField: Title = New Value (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedValue).isEqualTo("New Value")
    }

    @Test
    fun content_dynamicValueChange_recomposesWithNewValue() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Title",
                                    "value" to mapOf("path" to "/item/title"),
                                ),
                        )
                    ),
                initialData = mapOf("item" to mapOf("title" to "Old Title")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Title = Old Title (shortText)").assertIsDisplayed()
        assertThat(testTextField.capturedValue).isEqualTo("Old Title")

        controller.updateData("/item/title", "New Title")
        controller.waitForIdle()

        onNodeWithText("TextField: Title = Old Title (shortText)").assertDoesNotExist()
        onNodeWithText("TextField: Title = New Title (shortText)").assertIsDisplayed()
        assertThat(testTextField.capturedValue).isEqualTo("New Title")
    }

    @Test
    fun content_dynamicValueErased_recomposesWithNullValue() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Search",
                                    "value" to mapOf("path" to "/query"),
                                ),
                        )
                    ),
                initialData = mapOf("query" to "Kotlin"),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Search = Kotlin (shortText)").assertIsDisplayed()
        assertThat(testTextField.capturedValue).isEqualTo("Kotlin")

        controller.updateData("/query", null)
        controller.waitForIdle()

        onNodeWithText("TextField: Search = Kotlin (shortText)").assertDoesNotExist()
        onNodeWithText("TextField: Search = <null> (shortText)").assertIsDisplayed()
        assertThat(testTextField.capturedValue).isNull()
        assertThat(testTextField.capturedEnabled).isTrue()
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
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Password",
                                    "value" to "secret",
                                    "variant" to "shortText",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Password = secret (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.ShortText)

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "label" to "Password",
                    "value" to "secret",
                    "variant" to "obscured",
                ),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Password = secret (shortText) [RO]").assertDoesNotExist()
        onNodeWithText("TextField: Password = secret (obscured) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.TextField.Variant.Obscured)
    }

    @Test
    fun content_validationRegexpChange_recomposesWithNewRegexp() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Code",
                                    "value" to "123",
                                    "validationRegexp" to "^[0-9]+$",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Code = 123 (shortText) [^[0-9]+$] [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedValidationRegexp).isEqualTo("^[0-9]+$")

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "label" to "Code",
                    "value" to "123",
                    "validationRegexp" to "^[a-z]+$",
                ),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Code = 123 (shortText) [^[0-9]+$] [RO]").assertDoesNotExist()
        onNodeWithText("TextField: Code = 123 (shortText) [^[a-z]+$] [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedValidationRegexp).isEqualTo("^[a-z]+$")
    }

    @Test
    fun content_validationRegexpRemoved_recomposesWithNullRegexp() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Pin",
                                    "value" to "1234",
                                    "validationRegexp" to "^[0-9]{4}$",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Pin = 1234 (shortText) [^[0-9]{4}$] [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedValidationRegexp).isEqualTo("^[0-9]{4}$")

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Pin", "value" to "1234"),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Pin = 1234 (shortText) [^[0-9]{4}$] [RO]").assertDoesNotExist()
        onNodeWithText("TextField: Pin = 1234 (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedValidationRegexp).isNull()
    }

    @Test
    fun content_staticToDynamicValueChange_recomposesAndEnables() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties = mapOf("label" to "Input", "value" to "initial"),
                        )
                    ),
                initialData = mapOf("form" to mapOf("value" to "dynamic_val")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Input = initial (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedEnabled).isFalse()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "label" to "Input",
                    "value" to mapOf("path" to "/form/value"),
                ),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Input = dynamic_val (shortText)").assertIsDisplayed()
        assertThat(testTextField.capturedEnabled).isTrue()

        onNodeWithText("TextField: Input = dynamic_val (shortText)").performClick()
        controller.waitForIdle()

        onNodeWithText("TextField: Input = dynamic_val_updated (shortText)").assertIsDisplayed()
        assertThat(controller.getData<String>("/form/value")).isEqualTo("dynamic_val_updated")
    }

    @Test
    fun content_dynamicToStaticValueChange_recomposesAndDisables() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "TextField",
                            properties =
                                mapOf(
                                    "label" to "Input",
                                    "value" to mapOf("path" to "/form/value"),
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("value" to "dynamic_val")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("TextField: Input = dynamic_val (shortText)").assertIsDisplayed()
        assertThat(testTextField.capturedEnabled).isTrue()

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Input", "value" to "static_val"),
        )
        controller.waitForIdle()

        onNodeWithText("TextField: Input = static_val (shortText) [RO]").assertIsDisplayed()
        assertThat(testTextField.capturedEnabled).isFalse()

        onNodeWithText("TextField: Input = static_val (shortText) [RO]").performClick()
        controller.waitForIdle()

        onNodeWithText("TextField: Input = static_val (shortText) [RO]").assertIsDisplayed()
        assertThat(controller.getData<String>("/form/value")).isEqualTo("dynamic_val")
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
                            type = "TextField",
                            properties = mapOf("label" to "Field", "value" to "v"),
                        )
                    ),
            )
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasTestTag("initial_tag")).assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasTestTag("updated_tag")).assertIsDisplayed()
    }
}
