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
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialCheckBoxComponentTest {

    private val testCatalog =
        A2uiCatalog(catalogId = "test_catalog", components = listOf(MaterialCheckBoxComponent))

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
                                mapOf("value" to mapOf("path" to "/form/agree"), "label" to "Agree"),
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

        onNode(hasText("Tagged Checkbox") and hasTestTag("custom_tag")).assertIsDisplayed()
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

    private fun hasRole(role: Role): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, role)
}
