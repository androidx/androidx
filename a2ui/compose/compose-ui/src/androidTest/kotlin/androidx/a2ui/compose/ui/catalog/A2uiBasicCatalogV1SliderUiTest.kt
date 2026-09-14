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
class A2uiBasicCatalogV1SliderUiTest {

    private class TestSlider : A2uiBasicCatalogV1.Slider {
        var capturedLabel: String? = null
        var capturedMin: Float? = null
        var capturedMax: Float? = null
        var capturedValue: Float? = null
        var capturedOnValueChange: ((Float) -> Unit)? = null
        var capturedEnabled: Boolean? = null
        var capturedAccessibility: A2uiBasicCatalogV1.AccessibilityAttributes? = null
        var capturedChecks: List<A2uiBasicCatalogV1.CheckRule>? = null

        @Composable
        override fun A2uiComponentScope.TypedContent(
            label: String?,
            min: Float,
            max: Float,
            value: Float,
            onValueChange: (Float) -> Unit,
            enabled: Boolean,
            accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
            checks: List<A2uiBasicCatalogV1.CheckRule>,
            modifier: Modifier,
        ) {
            SideEffect {
                capturedLabel = label
                capturedMin = min
                capturedMax = max
                capturedValue = value
                capturedOnValueChange = onValueChange
                capturedEnabled = enabled
                capturedAccessibility = accessibility
                capturedChecks = checks
            }
            val readOnlyStr = if (!enabled) " [RO]" else ""
            val labelStr = if (label != null) "$label " else ""
            BasicText(
                text = "Slider: $labelStr$value ($min..$max)$readOnlyStr",
                modifier =
                    modifier
                        .clickable(enabled = enabled, onClick = { onValueChange(value + 10f) })
                        .testTag("slider_tag"),
            )
        }
    }

    private val testSlider = TestSlider()

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testSlider),
            functions = listOf(A2uiRequiredFunction.INSTANCE),
        )

    @Test
    fun isReady_pendingDynamicValue_returnsFalseAndGuardsContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Slider",
                            properties =
                                mapOf("max" to 100, "value" to mapOf("path" to "/pendingValue")),
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

        controller.updateData("/pendingValue", 50)
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("Slider: 50.0 (0.0..100.0)").assertIsDisplayed()
    }

    @Test
    fun isReady_pendingOptionalDynamicLabel_returnsTrueAndRendersWithNullLabel() =
        runComposeUiTest {
            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(
                            A2uiComponentPayload(
                                id = "root",
                                type = "Slider",
                                properties =
                                    mapOf(
                                        "label" to mapOf("path" to "/pendingLabel"),
                                        "max" to 100,
                                        "value" to 50,
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
            onNodeWithText("Slider: 50.0 (0.0..100.0) [RO]").assertIsDisplayed()
            assertThat(testSlider.capturedLabel).isNull()

            controller.updateData("/pendingLabel", "Volume")
            controller.waitForIdle()

            onNodeWithText("Slider: Volume 50.0 (0.0..100.0) [RO]").assertIsDisplayed()
            assertThat(testSlider.capturedLabel).isEqualTo("Volume")
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
                            type = "Slider",
                            properties =
                                mapOf("max" to 100, "value" to mapOf("path" to "/form/value")),
                        )
                    ),
                initialData = mapOf("form" to mapOf("value" to 42)),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Slider: 42.0 (0.0..100.0)").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/form/value", null)
        controller.waitForIdle()

        onNodeWithText("Slider: 42.0 (0.0..100.0)").assertDoesNotExist()
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
                            type = "Slider",
                            properties =
                                mapOf("label" to "Volume", "min" to 10, "max" to 50, "value" to 20),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Volume 20.0 (10.0..50.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedLabel).isEqualTo("Volume")
        assertThat(testSlider.capturedMin).isEqualTo(10f)
        assertThat(testSlider.capturedMax).isEqualTo(50f)
        assertThat(testSlider.capturedValue).isEqualTo(20f)
        assertThat(testSlider.capturedEnabled).isFalse()
        assertThat(testSlider.capturedOnValueChange).isNotNull()
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
                            type = "Slider",
                            properties = mapOf("max" to 10, "value" to 5),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: 5.0 (0.0..10.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedLabel).isNull()
        assertThat(testSlider.capturedMin).isEqualTo(0f)
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/form/label"),
                                    "min" to 0,
                                    "max" to 10,
                                    "value" to mapOf("path" to "/form/value"),
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Brightness", "value" to 5)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Brightness 5.0 (0.0..10.0)").assertIsDisplayed()
        assertThat(testSlider.capturedLabel).isEqualTo("Brightness")
        assertThat(testSlider.capturedMin).isEqualTo(0f)
        assertThat(testSlider.capturedMax).isEqualTo(10f)
        assertThat(testSlider.capturedValue).isEqualTo(5f)
        assertThat(testSlider.capturedEnabled).isTrue()
        assertThat(testSlider.capturedOnValueChange).isNotNull()
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "max" to 100,
                                    "value" to 50,
                                    "accessibility" to
                                        mapOf(
                                            "label" to "Volume slider",
                                            "description" to "Adjust volume level",
                                        ),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: 50.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedAccessibility)
            .isEqualTo(
                A2uiBasicCatalogV1.AccessibilityAttributes(
                    label = "Volume slider",
                    description = "Adjust volume level",
                )
            )
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
                            type = "Slider",
                            properties =
                                mapOf("max" to 100, "value" to mapOf("path" to "/settings/volume")),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("volume" to 20.0)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: 20.0 (0.0..100.0)").assertIsDisplayed()

        onNodeWithText("Slider: 20.0 (0.0..100.0)").performClick() // Stub slider adds 10f
        controller.waitForIdle()

        onNodeWithText("Slider: 20.0 (0.0..100.0)").assertDoesNotExist()
        onNodeWithText("Slider: 30.0 (0.0..100.0)").assertIsDisplayed()

        // Ensure data was updated in the model
        val dataModelVal = controller.getData<Number>("/settings/volume")?.toFloat()
        assertThat(dataModelVal).isEqualTo(30f)
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
                            type = "Slider",
                            properties = mapOf("max" to 10, "value" to 5),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Slider: 5.0 (0.0..10.0) [RO]") and hasTestTag("custom_tag"))
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
                            type = "Slider",
                            properties = mapOf("label" to "Initial", "max" to 50, "value" to 10),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Initial 10.0 (0.0..50.0) [RO]").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Updated", "max" to 100, "value" to 20),
        )
        controller.waitForIdle()

        onNodeWithText("Slider: Initial 10.0 (0.0..50.0) [RO]").assertDoesNotExist()
        onNodeWithText("Slider: Updated 20.0 (0.0..100.0) [RO]").assertIsDisplayed()
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
                            type = "Slider",
                            properties = mapOf("label" to "Old Label", "max" to 100, "value" to 50),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Old Label 50.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedLabel).isEqualTo("Old Label")

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "New Label", "max" to 100, "value" to 50),
        )
        controller.waitForIdle()

        onNodeWithText("Slider: Old Label 50.0 (0.0..100.0) [RO]").assertDoesNotExist()
        onNodeWithText("Slider: New Label 50.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedLabel).isEqualTo("New Label")
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "label" to mapOf("path" to "/form/label"),
                                    "max" to 100,
                                    "value" to 50,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Old Label")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Old Label 50.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedLabel).isEqualTo("Old Label")

        controller.updateData("/form/label", "New Label")
        controller.waitForIdle()

        onNodeWithText("Slider: Old Label 50.0 (0.0..100.0) [RO]").assertDoesNotExist()
        onNodeWithText("Slider: New Label 50.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedLabel).isEqualTo("New Label")
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
                            type = "Slider",
                            properties = mapOf("label" to "Volume", "max" to 100, "value" to 20),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Volume 20.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedValue).isEqualTo(20f)

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Volume", "max" to 100, "value" to 80),
        )
        controller.waitForIdle()

        onNodeWithText("Slider: Volume 20.0 (0.0..100.0) [RO]").assertDoesNotExist()
        onNodeWithText("Slider: Volume 80.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedValue).isEqualTo(80f)
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "label" to "Volume",
                                    "max" to 100,
                                    "value" to mapOf("path" to "/settings/volume"),
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("volume" to 20)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Volume 20.0 (0.0..100.0)").assertIsDisplayed()
        assertThat(testSlider.capturedValue).isEqualTo(20f)

        controller.updateData("/settings/volume", 80)
        controller.waitForIdle()

        onNodeWithText("Slider: Volume 20.0 (0.0..100.0)").assertDoesNotExist()
        onNodeWithText("Slider: Volume 80.0 (0.0..100.0)").assertIsDisplayed()
        assertThat(testSlider.capturedValue).isEqualTo(80f)
    }

    @Test
    fun content_minAndMaxUpdates_recomposesWithNewRange() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Slider",
                            properties =
                                mapOf("label" to "Volume", "min" to 0, "max" to 100, "value" to 50),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Volume 50.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedMin).isEqualTo(0f)
        assertThat(testSlider.capturedMax).isEqualTo(100f)

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Volume", "min" to 10, "max" to 200, "value" to 50),
        )
        controller.waitForIdle()

        onNodeWithText("Slider: Volume 50.0 (0.0..100.0) [RO]").assertDoesNotExist()
        onNodeWithText("Slider: Volume 50.0 (10.0..200.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedMin).isEqualTo(10f)
        assertThat(testSlider.capturedMax).isEqualTo(200f)
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
                            type = "Slider",
                            properties =
                                mapOf("label" to "Dynamic Toggle", "max" to 100, "value" to 10),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("optIn" to 20)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Dynamic Toggle 10.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedEnabled).isFalse()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "label" to "Dynamic Toggle",
                    "max" to 100,
                    "value" to mapOf("path" to "/settings/optIn"),
                ),
        )
        controller.waitForIdle()

        onNodeWithText("Slider: Dynamic Toggle 20.0 (0.0..100.0)").assertIsDisplayed()
        assertThat(testSlider.capturedEnabled).isTrue()

        onNodeWithText("Slider: Dynamic Toggle 20.0 (0.0..100.0)").performClick()
        controller.waitForIdle()

        onNodeWithText("Slider: Dynamic Toggle 30.0 (0.0..100.0)").assertIsDisplayed()
        assertThat(controller.getData<Number>("/settings/optIn")?.toFloat()).isEqualTo(30f)
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "label" to "Static Toggle",
                                    "max" to 100,
                                    "value" to mapOf("path" to "/settings/optIn"),
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("optIn" to 20)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Slider: Static Toggle 20.0 (0.0..100.0)").assertIsDisplayed()
        assertThat(testSlider.capturedEnabled).isTrue()

        controller.updateComponent(
            id = "root",
            properties = mapOf("label" to "Static Toggle", "max" to 100, "value" to 30),
        )
        controller.waitForIdle()

        onNodeWithText("Slider: Static Toggle 30.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(testSlider.capturedEnabled).isFalse()

        onNodeWithText("Slider: Static Toggle 30.0 (0.0..100.0) [RO]").performClick()
        controller.waitForIdle()

        onNodeWithText("Slider: Static Toggle 30.0 (0.0..100.0) [RO]").assertIsDisplayed()
        assertThat(controller.getData<Number>("/settings/optIn")?.toFloat()).isEqualTo(20f)
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
                            type = "Slider",
                            properties = mapOf("max" to 10, "value" to 5),
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "max" to 10,
                                    "value" to 5,
                                    "checks" to checksPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNode(hasTestTag("slider_tag")).assertIsDisplayed()
        assertThat(testSlider.capturedChecks)
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
                            type = "Slider",
                            properties = mapOf("max" to 10, "value" to 5),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNode(hasTestTag("slider_tag")).assertIsDisplayed()
        assertThat(testSlider.capturedChecks).isEmpty()
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "max" to 10,
                                    "value" to 5,
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

        // Data model does not have /verification/isApproved yet, so Slider is in Loading state.
        onNodeWithText("Loading...").assertIsDisplayed()
        onNode(hasTestTag("slider_tag")).assertDoesNotExist()

        // Update data model
        controller.updateData("/verification/isApproved", false)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNode(hasTestTag("slider_tag")).assertIsDisplayed()
        assertThat(testSlider.capturedChecks)
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "max" to 10,
                                    "value" to 5,
                                    "checks" to checksPayload,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        // Data model does not have /formData/zip yet, so check condition should evaluate to false.
        onNode(hasTestTag("slider_tag")).assertIsDisplayed()
        assertThat(testSlider.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = false, message = "Zip code is required")
            )

        // Update data model
        controller.updateData("/formData/zip", "94043")
        controller.waitForIdle()
        waitForIdle()

        onNode(hasTestTag("slider_tag")).assertIsDisplayed()
        assertThat(testSlider.capturedChecks)
            .containsExactly(
                A2uiBasicCatalogV1.CheckRule(condition = true, message = "Zip code is required")
            )
    }
}
