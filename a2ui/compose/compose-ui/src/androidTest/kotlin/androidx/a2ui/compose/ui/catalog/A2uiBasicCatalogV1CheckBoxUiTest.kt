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
import androidx.a2ui.model.catalog.functions.A2uiRequiredFunction
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
class A2uiBasicCatalogV1CheckBoxUiTest {

    private class TestCheckBox : A2uiBasicCatalogV1.CheckBox {
        var capturedLabel: String? = null
        var capturedValue: Boolean? = null
        var capturedOnValueChange: ((Boolean) -> Unit)? = null
        var capturedEnabled: Boolean? = null
        var capturedAccessibility: A2uiBasicCatalogV1.AccessibilityAttributes? = null
        var capturedChecks: List<A2uiBasicCatalogV1.CheckRule>? = null

        @Composable
        override fun A2uiComponentScope.TypedContent(
            label: String,
            value: Boolean,
            onValueChange: (Boolean) -> Unit,
            enabled: Boolean,
            accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
            checks: List<A2uiBasicCatalogV1.CheckRule>,
            modifier: Modifier,
        ) {
            SideEffect {
                capturedLabel = label
                capturedValue = value
                capturedOnValueChange = onValueChange
                capturedEnabled = enabled
                capturedAccessibility = accessibility
                capturedChecks = checks
            }
            val readOnlyStr = if (!enabled) " [RO]" else ""
            BasicText(
                text = "CheckBox: $label = $value$readOnlyStr",
                modifier =
                    modifier.clickable(enabled = enabled, onClick = { onValueChange(!value) }),
            )
        }
    }

    private val testCheckBox = TestCheckBox()

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testCheckBox),
            functions = listOf(A2uiRequiredFunction.INSTANCE),
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
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/pendingLabel"),
                                    "value" to false,
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

        controller.updateData("/pendingLabel", "Loaded Label")
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("CheckBox: Loaded Label = false [RO]").assertIsDisplayed()
    }

    @Test
    fun isReady_pendingDynamicValue_returnsFalseAndGuardsContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to "Accept Terms",
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

        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/pendingValue", true)
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("CheckBox: Accept Terms = true").assertIsDisplayed()
    }

    @Test
    fun isReady_bothPending_returnsFalseAndGuardsContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/pendingLabel"),
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

        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/pendingLabel", "Agree")
        controller.waitForIdle()

        // Still loading because value is pending
        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/pendingValue", true)
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("CheckBox: Agree = true").assertIsDisplayed()
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
                            type = "CheckBox",
                            properties =
                                mapOf("label" to mapOf("path" to "/form/label"), "value" to true),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Accept")),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("CheckBox: Accept = true [RO]").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/form/label", null)
        controller.waitForIdle()

        onNodeWithText("CheckBox: Accept = true [RO]").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_dynamicValueErased_transitionsFromReadyToPending() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties =
                                mapOf("label" to "Test", "value" to mapOf("path" to "/form/value")),
                        )
                    ),
                initialData = mapOf("form" to mapOf("value" to true)),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("CheckBox: Test = true").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/form/value", null)
        controller.waitForIdle()

        onNodeWithText("CheckBox: Test = true").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
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
                            type = "CheckBox",
                            properties = mapOf("label" to "Accept Terms", "value" to true),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Accept Terms = true [RO]").assertIsDisplayed()
        assertThat(testCheckBox.capturedLabel).isEqualTo("Accept Terms")
        assertThat(testCheckBox.capturedValue).isTrue()
        assertThat(testCheckBox.capturedEnabled).isFalse()
        assertThat(testCheckBox.capturedOnValueChange).isNotNull()
    }

    @Test
    fun content_staticAccessibility_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to "Accept Terms",
                                    "value" to true,
                                    "accessibility" to
                                        mapOf(
                                            "label" to "Terms checkbox",
                                            "description" to "Accept license terms",
                                        ),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Accept Terms = true [RO]").assertIsDisplayed()
        assertThat(testCheckBox.capturedAccessibility)
            .isEqualTo(
                A2uiBasicCatalogV1.AccessibilityAttributes(
                    label = "Terms checkbox",
                    description = "Accept license terms",
                )
            )
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
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/form/label"),
                                    "value" to mapOf("path" to "/form/checked"),
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Subscribe", "checked" to true)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Subscribe = true").assertIsDisplayed()
        assertThat(testCheckBox.capturedLabel).isEqualTo("Subscribe")
        assertThat(testCheckBox.capturedValue).isTrue()
        assertThat(testCheckBox.capturedEnabled).isTrue()
        assertThat(testCheckBox.capturedOnValueChange).isNotNull()
    }

    @Test
    fun content_onValueChange_toggleFromFalseToTrue_updatesDataModel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to "Toggle Me",
                                    "value" to mapOf("path" to "/settings/enabled"),
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("enabled" to false)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Toggle Me = false").assertIsDisplayed()

        onNodeWithText("CheckBox: Toggle Me = false").performClick()
        controller.waitForIdle()

        onNodeWithText("CheckBox: Toggle Me = false").assertDoesNotExist()
        onNodeWithText("CheckBox: Toggle Me = true").assertIsDisplayed()
        assertThat(controller.getData<Boolean>("/settings/enabled")).isTrue()
    }

    @Test
    fun content_onValueChange_toggleFromTrueToFalse_updatesDataModel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to "Uncheck Me",
                                    "value" to mapOf("path" to "/settings/enabled"),
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("enabled" to true)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Uncheck Me = true").assertIsDisplayed()

        onNodeWithText("CheckBox: Uncheck Me = true").performClick()
        controller.waitForIdle()

        onNodeWithText("CheckBox: Uncheck Me = true").assertDoesNotExist()
        onNodeWithText("CheckBox: Uncheck Me = false").assertIsDisplayed()
        assertThat(controller.getData<Boolean>("/settings/enabled")).isFalse()
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
                            type = "CheckBox",
                            properties = mapOf("label" to "Tagged", "value" to false),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("CheckBox: Tagged = false [RO]") and hasTestTag("custom_tag"))
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
                            type = "CheckBox",
                            properties = mapOf("label" to "Initial", "value" to false),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Initial = false [RO]").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Updated", "value" to true),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("CheckBox: Initial = false [RO]").assertDoesNotExist()
        onNodeWithText("CheckBox: Updated = true [RO]").assertIsDisplayed()
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
                            type = "CheckBox",
                            properties = mapOf("label" to "Old Label", "value" to true),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Old Label = true [RO]").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "New Label", "value" to true),
        )
        controller.waitForIdle()

        onNodeWithText("CheckBox: Old Label = true [RO]").assertDoesNotExist()
        onNodeWithText("CheckBox: New Label = true [RO]").assertIsDisplayed()
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
                            type = "CheckBox",
                            properties =
                                mapOf("label" to mapOf("path" to "/form/label"), "value" to false),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Old Label")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Old Label = false [RO]").assertIsDisplayed()

        controller.updateData("/form/label", "New Label")
        controller.waitForIdle()

        onNodeWithText("CheckBox: Old Label = false [RO]").assertDoesNotExist()
        onNodeWithText("CheckBox: New Label = false [RO]").assertIsDisplayed()
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
                            type = "CheckBox",
                            properties = mapOf("label" to "Accept Terms", "value" to false),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Accept Terms = false [RO]").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Accept Terms", "value" to true),
        )
        controller.waitForIdle()

        onNodeWithText("CheckBox: Accept Terms = false [RO]").assertDoesNotExist()
        onNodeWithText("CheckBox: Accept Terms = true [RO]").assertIsDisplayed()
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
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to "Static Label",
                                    "value" to mapOf("path" to "/val"),
                                ),
                        )
                    ),
                initialData = mapOf("val" to false),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Static Label = false").assertIsDisplayed()

        controller.updateData("/val", true)
        controller.waitForIdle()

        onNodeWithText("CheckBox: Static Label = false").assertDoesNotExist()
        onNodeWithText("CheckBox: Static Label = true").assertIsDisplayed()
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
                            type = "CheckBox",
                            properties = mapOf("label" to "Dynamic Toggle", "value" to false),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("optIn" to false)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Dynamic Toggle = false [RO]").assertIsDisplayed()
        assertThat(testCheckBox.capturedEnabled).isFalse()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("label" to "Dynamic Toggle", "value" to mapOf("path" to "/settings/optIn")),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("CheckBox: Dynamic Toggle = false").assertIsDisplayed()
        assertThat(testCheckBox.capturedEnabled).isTrue()

        onNodeWithText("CheckBox: Dynamic Toggle = false").performClick()
        controller.waitForIdle()

        onNodeWithText("CheckBox: Dynamic Toggle = true").assertIsDisplayed()
        assertThat(controller.getData<Boolean>("/settings/optIn")).isTrue()
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
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to "Static Toggle",
                                    "value" to mapOf("path" to "/settings/optIn"),
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("optIn" to false)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Static Toggle = false").assertIsDisplayed()
        assertThat(testCheckBox.capturedEnabled).isTrue()

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Static Toggle", "value" to true),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("CheckBox: Static Toggle = true [RO]").assertIsDisplayed()
        assertThat(testCheckBox.capturedEnabled).isFalse()

        onNodeWithText("CheckBox: Static Toggle = true [RO]").performClick()
        controller.waitForIdle()

        onNodeWithText("CheckBox: Static Toggle = true [RO]").assertIsDisplayed()
        assertThat(controller.getData<Boolean>("/settings/optIn")).isFalse()
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
                            type = "CheckBox",
                            properties = mapOf("label" to "Test", "value" to true),
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
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to "Test",
                                    "value" to true,
                                    "checks" to checksPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Test = true [RO]").assertIsDisplayed()
        assertThat(testCheckBox.capturedChecks)
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
                            type = "CheckBox",
                            properties = mapOf("label" to "Test", "value" to true),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("CheckBox: Test = true [RO]").assertIsDisplayed()
        assertThat(testCheckBox.capturedChecks).isEmpty()
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
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to "Test",
                                    "value" to true,
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

        // Data model does not have /verification/isApproved yet, so CheckBox is in Loading state.
        onNodeWithText("Loading...").assertIsDisplayed()
        onNodeWithText("CheckBox: Test = true [RO]").assertDoesNotExist()

        // Update data model
        controller.updateData("/verification/isApproved", false)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("CheckBox: Test = true [RO]").assertIsDisplayed()
        assertThat(testCheckBox.capturedChecks)
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
                            type = "CheckBox",
                            properties =
                                mapOf(
                                    "label" to "Test",
                                    "value" to true,
                                    "checks" to checksPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        // Data model does not have /formData/zip yet, so check condition should evaluate to false.
        onNodeWithText("CheckBox: Test = true [RO]").assertIsDisplayed()
        assertThat(testCheckBox.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = false, message = "Zip code is required")
            )

        // Update data model
        controller.updateData("/formData/zip", "94043")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("CheckBox: Test = true [RO]").assertIsDisplayed()
        assertThat(testCheckBox.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = true, message = "Zip code is required")
            )
    }
}
