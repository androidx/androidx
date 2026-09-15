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
import androidx.a2ui.compose.ui.testing.getData
import androidx.a2ui.model.catalog.functions.A2uiRequiredFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialA2uiBasicCatalogV1CheckBoxTest {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1Defaults.checkBox),
            functions = listOf(A2uiRequiredFunction.INSTANCE),
        )

    @Test
    fun value_staticTrue_rendersCheckedCheckboxWithLabel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties = mapOf("value" to true, "label" to "Accept Terms"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasRole(Role.Checkbox)).assertIsDisplayed().assertIsOn()
        onNodeWithText("Accept Terms").assertIsDisplayed()
    }

    @Test
    fun value_staticFalse_rendersUncheckedCheckboxWithLabel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties = mapOf("value" to false, "label" to "Decline Terms"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasRole(Role.Checkbox)).assertIsDisplayed().assertIsOff()
        onNodeWithText("Decline Terms").assertIsDisplayed()
    }

    @Test
    fun value_staticValue_cannotBeToggled() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties = mapOf("value" to true, "label" to "Static Read-Only"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasRole(Role.Checkbox)).assertIsDisplayed().assertIsNotEnabled().assertIsOn()

        onNodeWithText("Static Read-Only").performClick()
        controller.waitForIdle()

        onNode(hasRole(Role.Checkbox)).assertIsDisplayed().assertIsNotEnabled().assertIsOn()
    }

    @Test
    fun value_dynamicBinding_userClick_togglesStateAndUpdatesDataModel() = runComposeUiTest {
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
                                    "value" to mapOf("path" to "/settings/notifications"),
                                    "label" to "Notifications",
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("notifications" to true)),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasRole(Role.Checkbox)).assertIsDisplayed().assertIsOn().assertIsEnabled()

        // Toggle from true -> false
        onNode(hasRole(Role.Checkbox)).performClick()
        controller.waitForIdle()

        onNode(hasRole(Role.Checkbox)).assertIsOff()
        assertThat(controller.getData<Boolean>("/settings/notifications")).isEqualTo(false)

        // Toggle from false -> true
        onNode(hasRole(Role.Checkbox)).performClick()
        controller.waitForIdle()

        onNode(hasRole(Role.Checkbox)).assertIsOn()
        assertThat(controller.getData<Boolean>("/settings/notifications")).isEqualTo(true)
    }

    @Test
    fun value_dynamicBinding_externalDataChange_updatesCheckedState() = runComposeUiTest {
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
                                    "value" to mapOf("path" to "/settings/notifications"),
                                    "label" to "Notifications",
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("notifications" to false)),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasRole(Role.Checkbox)).assertIsOff()

        controller.updateData("/settings/notifications", true)
        controller.waitForIdle()

        onNode(hasRole(Role.Checkbox)).assertIsOn()
    }

    @Test
    fun value_componentPayloadUpdate_updatesCheckedState() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties = mapOf("value" to false, "label" to "Option"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasRole(Role.Checkbox)).assertIsOff()

        controller.updateComponent(
            id = "root",
            properties = mapOf("value" to true, "label" to "Option"),
        )
        controller.waitForIdle()

        onNode(hasRole(Role.Checkbox)).assertIsOn()
    }

    @Test
    fun label_dynamicBinding_rendersTextAndUpdatesWithData() = runComposeUiTest {
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
                                    "value" to true,
                                    "label" to mapOf("path" to "/user/opt_in_label"),
                                ),
                        )
                    ),
                initialData = mapOf("user" to mapOf("opt_in_label" to "Subscribe to daily digest")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Subscribe to daily digest").assertIsDisplayed()

        controller.updateData("/user/opt_in_label", "Subscribe to weekly digest")
        controller.waitForIdle()

        onNodeWithText("Subscribe to daily digest").assertDoesNotExist()
        onNodeWithText("Subscribe to weekly digest").assertIsDisplayed()
    }

    @Test
    fun label_componentPayloadUpdate_updatesLabelText() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties = mapOf("value" to true, "label" to "Initial Option"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Initial Option").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("value" to true, "label" to "Updated Option"),
        )
        controller.waitForIdle()

        onNodeWithText("Initial Option").assertDoesNotExist()
        onNodeWithText("Updated Option").assertIsDisplayed()
    }

    @Test
    fun isReady_unresolvedProperties_remainsInLoadingStateUntilDataArrives() = runComposeUiTest {
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
                                    "value" to mapOf("path" to "/form/agree"),
                                    "label" to mapOf("path" to "/form/label"),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onLoading = { modifier ->
                        Text("Loading CheckBox...", modifier = modifier.testTag("custom_loader"))
                    },
                )
            }
        }

        // Both properties missing -> loading
        onNodeWithTag("custom_loader").assertIsDisplayed()

        // Only value arrives -> still loading
        controller.updateData("/form/agree", true)
        controller.waitForIdle()
        onNodeWithTag("custom_loader").assertIsDisplayed()

        // Label arrives -> success
        controller.updateData("/form/label", "I agree to terms")
        controller.waitForIdle()

        onNodeWithTag("custom_loader").assertDoesNotExist()
        onNodeWithText("I agree to terms").assertIsDisplayed()
        onNode(hasRole(Role.Checkbox)).assertIsOn()
    }

    @Test
    fun isReady_reactiveToDataAdditionAndRemoval() = runComposeUiTest {
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
                                    "value" to mapOf("path" to "/form/agree"),
                                    "label" to "Agree",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasRole(Role.Checkbox)).assertDoesNotExist()

        controller.updateData("/form/agree", true)
        controller.waitForIdle()

        onNode(hasRole(Role.Checkbox)).assertIsDisplayed()

        controller.updateData("/form/agree", null)
        controller.waitForIdle()

        onNode(hasRole(Role.Checkbox)).assertDoesNotExist()
    }

    @Test
    fun modifier_parentModifier_isApplied() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties = mapOf("value" to true, "label" to "Tagged Checkbox"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag"))
            }
        }

        onNodeWithTag("custom_tag").assertIsDisplayed()
        onNodeWithText("Tagged Checkbox").assertIsDisplayed()
    }

    @Test
    fun modifier_parameterChanges_updatesRenderedModifier() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties = mapOf("value" to true, "label" to "Checkbox Tag Test"),
                        )
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

    @Test
    fun accessibility_hasCorrectCheckboxRole() = runComposeUiTest {
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
                                    "value" to mapOf("path" to "/settings/terms"),
                                    "label" to "Accessible Terms",
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("terms" to true)),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasText("Accessible Terms"))
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    @Test
    fun accessibility_withLabelAndDescription_setsContentDescriptionAndOnClickLabel() =
        runComposeUiTest {
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
                                        "value" to mapOf("path" to "/settings/terms"),
                                        "label" to "Terms",
                                        "accessibility" to
                                            mapOf(
                                                "label" to "Custom Checkbox Label",
                                                "description" to "Toggle Terms",
                                            ),
                                    ),
                            )
                        ),
                    initialData = mapOf("settings" to mapOf("terms" to false)),
                )
            val surface = controller.start()

            setContent { MaterialTheme { A2uiTestSurface(surface) } }

            onNodeWithContentDescription("Custom Checkbox Label")
                .assertIsDisplayed()
                .assert(
                    SemanticsMatcher("has onClick with label") { node ->
                        node.config.getOrNull(SemanticsActions.OnClick)?.label == "Toggle Terms"
                    }
                )

            onNodeWithContentDescription("Custom Checkbox Label")
                .performSemanticsAction(SemanticsActions.OnClick)
            controller.waitForIdle()

            assertThat(controller.getData<Boolean>("/settings/terms")).isEqualTo(true)
        }

    @Test
    fun accessibility_withoutAccessibilityAttributes_mergesTextByDefault() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CheckBox",
                            properties = mapOf("value" to true, "label" to "Accept Terms"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        val checkbox = onNode(hasRole(Role.Checkbox) and hasText("Accept Terms"))
        checkbox.assertIsDisplayed().assertIsOn()

        val semanticsNode = checkbox.fetchSemanticsNode()
        assertThat(semanticsNode.config.isMergingSemanticsOfDescendants).isTrue()
        assertThat(semanticsNode.config[SemanticsProperties.Text])
            .isEqualTo(listOf(AnnotatedString("Accept Terms")))
        assertThat(semanticsNode.config.getOrNull(SemanticsProperties.ContentDescription)).isNull()

        onNode(hasText("Accept Terms"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun accessibility_withLabelAndAccessibility_mergesTextAndContentDescription() =
        runComposeUiTest {
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
                                        "value" to true,
                                        "label" to "Accept Terms",
                                        "accessibility" to
                                            mapOf(
                                                "label" to "Custom Checkbox Label",
                                                "description" to "Toggle Terms",
                                            ),
                                    ),
                            )
                        ),
                )
            val surface = controller.start()

            setContent { MaterialTheme { A2uiTestSurface(surface) } }

            val checkbox =
                onNode(
                    hasRole(Role.Checkbox) and
                        hasText("Accept Terms") and
                        hasContentDescription("Custom Checkbox Label")
                )
            checkbox.assertIsDisplayed().assertIsOn()

            val semanticsNode = checkbox.fetchSemanticsNode()
            assertThat(semanticsNode.config.isMergingSemanticsOfDescendants).isTrue()
            assertThat(semanticsNode.config[SemanticsProperties.Text])
                .isEqualTo(listOf(AnnotatedString("Accept Terms")))
            assertThat(semanticsNode.config[SemanticsProperties.ContentDescription])
                .isEqualTo(listOf("Custom Checkbox Label"))

            onNode(hasText("Accept Terms"), useUnmergedTree = true).assertExists()
        }

    @Test
    fun checks_allChecksPass_noErrorMessage() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "CheckBox",
                properties =
                    mapOf(
                        "label" to "Accept Terms",
                        "value" to true,
                        "checks" to
                            listOf(
                                mapOf(
                                    "condition" to
                                        mapOf(
                                            "call" to "required",
                                            "args" to
                                                mapOf(
                                                    "value" to
                                                        mapOf("path" to "/form/agreementDate")
                                                ),
                                        ),
                                    "message" to "Agreement date is required",
                                )
                            ),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("form" to mapOf("agreementDate" to "2026-09-10")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNode(hasRole(Role.Checkbox)).assertIsDisplayed().assertIsOn()
        onNode(hasRole(Role.Checkbox))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        onNodeWithText("Agreement date is required").assertDoesNotExist()
    }

    @Test
    fun checks_failedCheck_displaysErrorMessage() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "CheckBox",
                properties =
                    mapOf(
                        "label" to "Accept Terms",
                        "value" to false,
                        "checks" to
                            listOf(
                                mapOf(
                                    "condition" to
                                        mapOf(
                                            "call" to "required",
                                            "args" to
                                                mapOf(
                                                    "value" to
                                                        mapOf("path" to "/form/agreementDate")
                                                ),
                                        ),
                                    "message" to "Agreement date is required",
                                )
                            ),
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNode(hasRole(Role.Checkbox)).assertIsDisplayed().assertIsOff()
        onNode(hasRole(Role.Checkbox))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Agreement date is required",
                )
            )
        onNodeWithText("Agreement date is required").assertIsDisplayed()
    }

    @Test
    fun checks_dynamicCondition_updatesErrorMessage() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "CheckBox",
                properties =
                    mapOf(
                        "label" to "Accept Terms",
                        "value" to false,
                        "checks" to
                            listOf(
                                mapOf(
                                    "condition" to
                                        mapOf(
                                            "call" to "required",
                                            "args" to
                                                mapOf(
                                                    "value" to
                                                        mapOf("path" to "/form/agreementDate")
                                                ),
                                        ),
                                    "message" to "Agreement date is required",
                                )
                            ),
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNode(hasRole(Role.Checkbox))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Agreement date is required",
                )
            )
        onNodeWithText("Agreement date is required").assertIsDisplayed()

        controller.updateData("/form/agreementDate", "2026-09-10")
        controller.waitForIdle()
        waitForIdle()

        onNode(hasRole(Role.Checkbox))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        onNodeWithText("Agreement date is required").assertDoesNotExist()
    }

    @Test
    fun checks_multipleChecks_displaysFirstFailedCheck() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "CheckBox",
                properties =
                    mapOf(
                        "label" to "Accept Terms",
                        "value" to false,
                        "checks" to
                            listOf(
                                mapOf(
                                    "condition" to
                                        mapOf(
                                            "call" to "required",
                                            "args" to
                                                mapOf(
                                                    "value" to
                                                        mapOf("path" to "/form/agreementDate")
                                                ),
                                        ),
                                    "message" to "Agreement date is required",
                                ),
                                mapOf(
                                    "condition" to
                                        mapOf(
                                            "call" to "required",
                                            "args" to
                                                mapOf(
                                                    "value" to mapOf("path" to "/form/signature")
                                                ),
                                        ),
                                    "message" to "Signature is required",
                                ),
                            ),
                    ),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNode(hasRole(Role.Checkbox))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Agreement date is required",
                )
            )
        onNodeWithText("Agreement date is required").assertIsDisplayed()
        onNodeWithText("Signature is required").assertDoesNotExist()

        controller.updateData("/form/agreementDate", "2026-09-10")
        controller.waitForIdle()
        waitForIdle()

        onNode(hasRole(Role.Checkbox))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Signature is required",
                )
            )
        onNodeWithText("Agreement date is required").assertDoesNotExist()
        onNodeWithText("Signature is required").assertIsDisplayed()

        controller.updateData("/form/signature", "John Doe")
        controller.waitForIdle()
        waitForIdle()

        onNode(hasRole(Role.Checkbox))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        onNodeWithText("Agreement date is required").assertDoesNotExist()
        onNodeWithText("Signature is required").assertDoesNotExist()
    }

    private fun hasRole(role: Role): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, role)
}
