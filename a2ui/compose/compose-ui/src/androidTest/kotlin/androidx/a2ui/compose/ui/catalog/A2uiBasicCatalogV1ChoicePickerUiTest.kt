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
import androidx.a2ui.model.catalog.functions.A2uiRequiredFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiBasicCatalogV1ChoicePickerUiTest {

    private class TestChoicePicker : A2uiBasicCatalogV1.ChoicePicker {
        var capturedOptions: List<A2uiBasicCatalogV1.ChoicePicker.Option>? = null
        var capturedValue: List<String>? = null
        var capturedOnValueChange: ((List<String>) -> Unit)? = null
        var capturedEnabled: Boolean? = null
        var capturedLabel: String? = null
        var capturedVariant: A2uiBasicCatalogV1.ChoicePicker.Variant? = null
        var capturedDisplayStyle: A2uiBasicCatalogV1.ChoicePicker.DisplayStyle? = null
        var capturedFilterable: Boolean? = null
        var capturedAccessibility: A2uiBasicCatalogV1.AccessibilityAttributes? = null
        var capturedChecks: List<A2uiBasicCatalogV1.CheckRule>? = null

        @Composable
        override fun A2uiComponentScope.TypedContent(
            label: String?,
            options: List<A2uiBasicCatalogV1.ChoicePicker.Option>,
            value: List<String>,
            variant: A2uiBasicCatalogV1.ChoicePicker.Variant,
            displayStyle: A2uiBasicCatalogV1.ChoicePicker.DisplayStyle,
            filterable: Boolean,
            onValueChange: (List<String>) -> Unit,
            enabled: Boolean,
            accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
            checks: List<A2uiBasicCatalogV1.CheckRule>,
            modifier: Modifier,
        ) {
            SideEffect {
                capturedLabel = label
                capturedOptions = options
                capturedValue = value
                capturedVariant = variant
                capturedDisplayStyle = displayStyle
                capturedFilterable = filterable
                capturedOnValueChange = onValueChange
                capturedEnabled = enabled
                capturedAccessibility = accessibility
                capturedChecks = checks
            }
            BasicText(
                text = "ChoicePicker: label=$label, value=$value, options=${options.size}",
                modifier = modifier,
            )
        }
    }

    private val testChoicePicker = TestChoicePicker()

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testChoicePicker),
            functions = listOf(A2uiRequiredFunction.INSTANCE),
        )

    @Test
    fun content_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "label" to "Options Label",
                                    "variant" to "multipleSelection",
                                    "displayStyle" to "chips",
                                    "filterable" to true,
                                    "options" to
                                        listOf(
                                            mapOf("label" to "Option 1", "value" to "1"),
                                            mapOf("label" to "Option 2", "value" to "2"),
                                        ),
                                    "value" to listOf("1"),
                                    "accessibility" to
                                        mapOf(
                                            "label" to "Accessibility Label",
                                            "description" to "Accessibility Description",
                                        ),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("ChoicePicker: label=Options Label, value=[1], options=2")
            .assertIsDisplayed()
        assertThat(testChoicePicker.capturedOptions)
            .containsExactly(
                A2uiBasicCatalogV1.ChoicePicker.Option("Option 1", "1"),
                A2uiBasicCatalogV1.ChoicePicker.Option("Option 2", "2"),
            )
            .inOrder()
        assertThat(testChoicePicker.capturedValue).containsExactly("1")
        assertThat(testChoicePicker.capturedLabel).isEqualTo("Options Label")
        assertThat(testChoicePicker.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.Variant.MultipleSelection)
        assertThat(testChoicePicker.capturedDisplayStyle)
            .isEqualTo(A2uiBasicCatalogV1.ChoicePicker.DisplayStyle.Chips)
        assertThat(testChoicePicker.capturedFilterable).isTrue()
        assertThat(testChoicePicker.capturedEnabled)
            .isFalse() // No updater since value is static list
        assertThat(testChoicePicker.capturedAccessibility)
            .isEqualTo(
                A2uiBasicCatalogV1.AccessibilityAttributes(
                    label = "Accessibility Label",
                    description = "Accessibility Description",
                )
            )
    }

    @Test
    fun content_onValueChange_updatesBoundDataModel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "options" to
                                        listOf(
                                            mapOf("label" to "Option 1", "value" to "1"),
                                            mapOf("label" to "Option 2", "value" to "2"),
                                        ),
                                    "value" to mapOf("path" to "/selectedValues"),
                                ),
                        )
                    ),
                initialData = mapOf("selectedValues" to listOf("1")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("ChoicePicker: label=null, value=[1], options=2").assertIsDisplayed()
        assertThat(testChoicePicker.capturedEnabled).isTrue()

        testChoicePicker.capturedOnValueChange?.invoke(listOf("1", "2"))

        onNodeWithText("ChoicePicker: label=null, value=[1, 2], options=2").assertIsDisplayed()
        assertThat(testChoicePicker.capturedValue).containsExactly("1", "2")
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
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "options" to listOf(mapOf("label" to "Opt", "value" to "val")),
                                    "value" to listOf<String>(),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface, modifier = Modifier.testTag("custom_surface")) }

        onNode(
                hasText("ChoicePicker: label=null, value=[], options=1") and
                    hasTestTag("custom_surface")
            )
            .assertIsDisplayed()
    }

    @Test
    fun content_duplicateOptionValues_reportsError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "options" to
                                        listOf(
                                            mapOf("label" to "Opt 1", "value" to "dup_val"),
                                            mapOf("label" to "Opt 2", "value" to "dup_val"),
                                        ),
                                    "value" to listOf("dup_val"),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, onError = { _, _ -> }) }
        waitForIdle()
        controller.waitForIdle()

        val error = controller.outboundErrors.single()
        assertThat(error.message)
            .isEqualTo("Duplicate option values ['dup_val'] found in ChoicePicker options.")
    }

    @Test
    fun isReady_unresolvedValue_remainsLoading() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "options" to listOf(mapOf("label" to "Opt", "value" to "val")),
                                    "value" to mapOf("path" to "/unresolved/selection"),
                                ),
                        )
                    ),
                initialData = emptyMap(),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier ->
                    BasicText(
                        "Loading ChoicePicker...",
                        modifier = modifier.testTag("loading_view"),
                    )
                },
            )
        }

        onNodeWithTag("loading_view").assertIsDisplayed()
    }

    @Test
    fun isReady_unresolvedOptionLabel_remainsLoading() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "options" to
                                        listOf(
                                            mapOf(
                                                "label" to mapOf("path" to "/unresolved/label"),
                                                "value" to "val",
                                            )
                                        ),
                                    "value" to listOf<String>(),
                                ),
                        )
                    ),
                initialData = emptyMap(),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier ->
                    BasicText(
                        "Loading ChoicePicker...",
                        modifier = modifier.testTag("loading_view"),
                    )
                },
            )
        }

        onNodeWithTag("loading_view").assertIsDisplayed()
    }

    @Test
    fun content_checksPresent_resolvesAndPassesToTypedContent() = runComposeUiTest {
        val checksPayload =
            listOf(
                mapOf("condition" to true, "message" to "Must be valid"),
                mapOf("condition" to false, "message" to "Failed check"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "options" to
                                        listOf(mapOf("label" to "Option 1", "value" to "1")),
                                    "value" to listOf("1"),
                                    "checks" to checksPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("ChoicePicker: label=null, value=[1], options=1").assertIsDisplayed()
        assertThat(testChoicePicker.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = true, message = "Must be valid"),
                A2uiBasicCatalogV1.CheckRule(condition = false, message = "Failed check"),
            )
            .inOrder()
    }

    @Test
    fun content_noChecks_passesEmptyListToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "options" to
                                        listOf(mapOf("label" to "Option 1", "value" to "1")),
                                    "value" to listOf("1"),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("ChoicePicker: label=null, value=[1], options=1").assertIsDisplayed()
        assertThat(testChoicePicker.capturedChecks).isEmpty()
    }

    @Test
    fun content_dynamicChecks_waitsForDataModelToResolve() = runComposeUiTest {
        val checksPayload =
            listOf(
                mapOf(
                    "condition" to mapOf("path" to "/verification/isApproved"),
                    "message" to "Verification is not approved",
                )
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "options" to
                                        listOf(mapOf("label" to "Option 1", "value" to "1")),
                                    "value" to listOf("1"),
                                    "checks" to checksPayload,
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

        // Data model does not have /verification/isApproved yet, so ChoicePicker is in Loading
        // state.
        onNodeWithText("Loading...").assertIsDisplayed()
        onNodeWithText("ChoicePicker: label=null, value=[1], options=1").assertDoesNotExist()

        // Update data model
        controller.updateData("/verification/isApproved", false)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("ChoicePicker: label=null, value=[1], options=1").assertIsDisplayed()
        assertThat(testChoicePicker.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(
                    condition = false,
                    message = "Verification is not approved",
                )
            )
    }

    @Test
    fun content_dynamicChecks_withRequiredFunction_evaluatesCondition() = runComposeUiTest {
        val checksPayload =
            listOf(
                mapOf(
                    "condition" to
                        mapOf(
                            "call" to "required",
                            "args" to mapOf("value" to mapOf("path" to "/formData/zip")),
                        ),
                    "message" to "Zip code is required",
                )
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "ChoicePicker",
                            properties =
                                mapOf(
                                    "options" to
                                        listOf(mapOf("label" to "Option 1", "value" to "1")),
                                    "value" to listOf("1"),
                                    "checks" to checksPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        // Data model does not have /formData/zip yet, so check condition should evaluate to false.
        onNodeWithText("ChoicePicker: label=null, value=[1], options=1").assertIsDisplayed()
        assertThat(testChoicePicker.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = false, message = "Zip code is required")
            )

        // Update data model
        controller.updateData("/formData/zip", "94043")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("ChoicePicker: label=null, value=[1], options=1").assertIsDisplayed()
        assertThat(testChoicePicker.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = true, message = "Zip code is required")
            )
    }
}
