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
class A2uiBasicCatalogV1ButtonUiTest {

    private val testButton =
        object : A2uiBasicCatalogV1.Button {
            var capturedChildId: String? = null
            var capturedVariant: A2uiBasicCatalogV1.Button.Variant? = null
            var capturedAction: Map<String, Any?>? = null
            var capturedAccessibility: A2uiBasicCatalogV1.AccessibilityAttributes? = null
            var capturedChecks: List<A2uiBasicCatalogV1.CheckRule>? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                childId: String,
                variant: A2uiBasicCatalogV1.Button.Variant,
                action: Map<String, Any?>,
                accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                checks: List<A2uiBasicCatalogV1.CheckRule>,
                modifier: Modifier,
            ) {
                SideEffect {
                    capturedChildId = childId
                    capturedVariant = variant
                    capturedAction = action
                    capturedAccessibility = accessibility
                    capturedChecks = checks
                }
                BasicText(text = "Button: $childId", modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testButton),
            functions = listOf(A2uiRequiredFunction.INSTANCE),
        )

    @Test
    fun content_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit_form"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_xyz",
                                    "variant" to "primary",
                                    "action" to actionPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Button: child_xyz").assertIsDisplayed()
        assertThat(testButton.capturedChildId).isEqualTo("child_xyz")
        assertThat(testButton.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Button.Variant.Primary)
        assertThat(testButton.capturedAction).isEqualTo(actionPayload)
    }

    @Test
    fun content_staticAccessibility_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit_form"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_xyz",
                                    "action" to actionPayload,
                                    "accessibility" to
                                        mapOf(
                                            "label" to "Submit Button",
                                            "description" to "Submits the form",
                                        ),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Button: child_xyz").assertIsDisplayed()
        assertThat(testButton.capturedAccessibility)
            .isEqualTo(
                A2uiBasicCatalogV1.AccessibilityAttributes(
                    label = "Submit Button",
                    description = "Submits the form",
                )
            )
    }

    @Test
    fun content_passedModifier_appliesToTypedContent() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit_form"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties = mapOf("child" to "btn_text", "action" to actionPayload),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Button: btn_text") and hasTestTag("custom_tag")).assertIsDisplayed()
    }

    @Test
    fun content_omittedVariant_fallsBackToDefault() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit_form"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties = mapOf("child" to "btn_text", "action" to actionPayload),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Button: btn_text").assertIsDisplayed()
        assertThat(testButton.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Secondary)
    }

    @Test
    fun content_variant_resolvesAllValuesAndPassesToTypedContent() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit_form"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "btn_text",
                                    "variant" to "default",
                                    "action" to actionPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testButton.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Secondary)

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("child" to "btn_text", "variant" to "primary", "action" to actionPayload),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testButton.capturedVariant).isEqualTo(A2uiBasicCatalogV1.Button.Variant.Primary)

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("child" to "btn_text", "variant" to "borderless", "action" to actionPayload),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testButton.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Borderless)
    }

    @Test
    fun content_variantChange_recomposesWithNewVariant() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_1",
                                    "variant" to "default",
                                    "action" to actionPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Button: child_1").assertIsDisplayed()
        assertThat(testButton.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Secondary)

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("child" to "child_1", "variant" to "borderless", "action" to actionPayload),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Button: child_1").assertIsDisplayed()
        assertThat(testButton.capturedVariant)
            .isEqualTo(A2uiBasicCatalogV1.Button.Variant.Borderless)
    }

    @Test
    fun content_childIdChange_recomposesWithNewChildId() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties = mapOf("child" to "old_child", "action" to actionPayload),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Button: old_child").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("child" to "new_child", "action" to actionPayload),
        )
        controller.waitForIdle()

        onNodeWithText("Button: old_child").assertDoesNotExist()
        onNodeWithText("Button: new_child").assertIsDisplayed()
        assertThat(testButton.capturedChildId).isEqualTo("new_child")
    }

    @Test
    fun content_actionChange_recomposesWithNewAction() = runComposeUiTest {
        val oldAction = mapOf("event" to mapOf("name" to "submit_form"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties = mapOf("child" to "child_1", "action" to oldAction),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testButton.capturedAction).isEqualTo(oldAction)

        val newAction = mapOf("event" to mapOf("name" to "cancel_form"))
        controller.updateComponent(
            id = "root",
            properties = mapOf("child" to "child_1", "action" to newAction),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testButton.capturedAction).isEqualTo(newAction)
    }

    @Test
    fun content_modifierChange_recomposesWithNewModifier() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties = mapOf("child" to "child_id", "action" to actionPayload),
                        )
                    ),
            )
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("Button: child_id") and hasTestTag("initial_tag")).assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("Button: child_id") and hasTestTag("updated_tag")).assertIsDisplayed()
    }

    @Test
    fun content_checksProperty_resolvesAndPassesToTypedContent() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit"))
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
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_id",
                                    "action" to actionPayload,
                                    "checks" to checksPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Button: child_id").assertIsDisplayed()
        assertThat(testButton.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = true, message = "Must be valid"),
                A2uiBasicCatalogV1.CheckRule(condition = false, message = "Failed check"),
            )
            .inOrder()
    }

    @Test
    fun content_noChecks_passesEmptyListToTypedContent() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_id",
                                    "action" to actionPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Button: child_id").assertIsDisplayed()
        assertThat(testButton.capturedChecks).isEmpty()
    }

    @Test
    fun content_dynamicChecks_waitsForDataModelToResolve() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit"))
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
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_id",
                                    "action" to actionPayload,
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

        // Data model does not have /verification/isApproved yet, so button is in Loading state.
        onNodeWithText("Loading...").assertIsDisplayed()
        onNodeWithText("Button: child_id").assertDoesNotExist()

        // Update data model
        controller.updateData("/verification/isApproved", false)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("Button: child_id").assertIsDisplayed()
        assertThat(testButton.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(
                    condition = false,
                    message = "Verification is not approved",
                )
            )
    }

    @Test
    fun content_dynamicChecks_withRequiredFunction_evaluatesCondition() = runComposeUiTest {
        val actionPayload = mapOf("event" to mapOf("name" to "submit"))
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
                            type = "Button",
                            properties =
                                mapOf(
                                    "child" to "child_id",
                                    "action" to actionPayload,
                                    "checks" to checksPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        // Data model does not have /formData/zip yet, so check condition should evaluate to false.
        onNodeWithText("Button: child_id").assertIsDisplayed()
        assertThat(testButton.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = false, message = "Zip code is required")
            )

        // Update data model
        controller.updateData("/formData/zip", "94043")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Button: child_id").assertIsDisplayed()
        assertThat(testButton.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = true, message = "Zip code is required")
            )
    }
}
