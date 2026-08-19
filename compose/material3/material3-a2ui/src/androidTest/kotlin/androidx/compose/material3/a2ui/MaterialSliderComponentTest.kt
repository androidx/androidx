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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialSliderComponentTest {

    private val testCatalog =
        A2uiCatalog(catalogId = "test_catalog", components = listOf(MaterialSliderComponent))

    @Test
    fun value_staticValue_rendersSliderWithValueAndLabel() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Slider",
                            properties =
                                mapOf("value" to 25, "min" to 0, "max" to 100, "label" to "Volume"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Volume").assertIsDisplayed()
        onNodeWithText("25").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(25f, 0f..100f, 99))).assertIsDisplayed()
    }

    @Test
    fun value_staticValue_withoutLabel_rendersSliderWithValue() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Slider",
                            properties = mapOf("value" to 50, "max" to 100),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("50").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(50f, 0f..100f, 99))).assertIsDisplayed()
    }

    @Test
    fun value_staticValue_cannotBeAdjusted() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Slider",
                            properties =
                                mapOf("value" to 30, "max" to 100, "label" to "Static Slider"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Static Slider").assertIsDisplayed()
        onNodeWithText("30").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(30f, 0f..100f, 99)))
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun value_dynamicBinding_userAdjustment_updatesStateAndDataModel() = runComposeUiTest {
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
                                    "value" to mapOf("path" to "/settings/volume"),
                                    "min" to 0,
                                    "max" to 100,
                                    "label" to "Volume",
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("volume" to 20)),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Volume").assertIsDisplayed()
        onNodeWithText("20").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(20f, 0f..100f, 99)))
            .assertIsDisplayed()
            .assertIsEnabled()

        // Adjust slider progress from 20 -> 80
        onNode(hasSetProgressAction()).performSemanticsAction(SemanticsActions.SetProgress) {
            setProgress ->
            setProgress(80f)
        }
        controller.waitForIdle()

        onNodeWithText("Volume").assertIsDisplayed()
        onNodeWithText("20").assertDoesNotExist()
        onNodeWithText("80").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(80f, 0f..100f, 99))).assertIsDisplayed()
        assertThat(controller.getData<Number>("/settings/volume")?.toInt()).isEqualTo(80)
    }

    @Test
    fun value_dynamicBinding_externalDataChange_updatesSliderValue() = runComposeUiTest {
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
                                    "value" to mapOf("path" to "/settings/brightness"),
                                    "min" to 0,
                                    "max" to 100,
                                ),
                        )
                    ),
                initialData = mapOf("settings" to mapOf("brightness" to 10)),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("10").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(10f, 0f..100f, 99))).assertIsDisplayed()

        controller.updateData("/settings/brightness", 75)
        controller.waitForIdle()

        onNodeWithText("10").assertDoesNotExist()
        onNodeWithText("75").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(75f, 0f..100f, 99))).assertIsDisplayed()
    }

    @Test
    fun value_componentPayloadUpdate_updatesSliderValue() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Slider",
                            properties = mapOf("value" to 15, "min" to 0, "max" to 100),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("15").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(15f, 0f..100f, 99))).assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("value" to 60, "min" to 0, "max" to 100),
        )
        controller.waitForIdle()

        onNodeWithText("15").assertDoesNotExist()
        onNodeWithText("60").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(60f, 0f..100f, 99))).assertIsDisplayed()
    }

    @Test
    fun value_coercedWithinRange() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Slider",
                            properties = mapOf("value" to 150, "min" to 0, "max" to 100),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("100").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(100f, 0f..100f, 99)))
            .assertIsDisplayed()
    }

    @Test
    fun minMax_customRange_appliesCorrectRange() = runComposeUiTest {
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
                                    "value" to 35,
                                    "min" to 10,
                                    "max" to 50,
                                    "label" to "Custom Range",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(35f, 10f..50f, 39))).assertIsDisplayed()
        onNodeWithText("Custom Range").assertIsDisplayed()
        onNodeWithText("35").assertIsDisplayed()
    }

    @Test
    fun minMax_minGreaterThanMax_reportsError() = runComposeUiTest {
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
                                    "value" to 75,
                                    "min" to 100,
                                    "max" to 50,
                                    "label" to "Invalid Bounds",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    onError = { exception, _ -> Text("Error: ${exception.message}") },
                )
            }
        }

        onNodeWithText("Error: Min value cannot be greater than max value.").assertIsDisplayed()
    }

    @Test
    fun label_dynamicLabelBinding_rendersTextAndUpdatesWithData() = runComposeUiTest {
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
                                    "value" to 50,
                                    "max" to 100,
                                    "label" to mapOf("path" to "/slider/title"),
                                ),
                        )
                    ),
                initialData = mapOf("slider" to mapOf("title" to "Initial Title")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Initial Title").assertIsDisplayed()

        controller.updateData("/slider/title", "Updated Title")
        controller.waitForIdle()

        onNodeWithText("Initial Title").assertDoesNotExist()
        onNodeWithText("Updated Title").assertIsDisplayed()
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "value" to 50,
                                    "max" to 100,
                                    "label" to "Initial Slider Label",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Initial Slider Label").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("value" to 50, "max" to 100, "label" to "Updated Slider Label"),
        )
        controller.waitForIdle()

        onNodeWithText("Initial Slider Label").assertDoesNotExist()
        onNodeWithText("Updated Slider Label").assertIsDisplayed()
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
                            type = "Slider",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/volume"),
                                    "max" to 100,
                                    "label" to "Volume",
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
                        Text("Loading Slider...", modifier = modifier.testTag("custom_loader"))
                    },
                )
            }
        }

        // Value missing -> loading
        onNodeWithTag("custom_loader").assertIsDisplayed()

        // Value arrives -> success
        controller.updateData("/form/volume", 42)
        controller.waitForIdle()

        onNodeWithTag("custom_loader").assertDoesNotExist()
        onNodeWithText("Volume").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(42f, 0f..100f, 99))).assertIsDisplayed()
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
                            type = "Slider",
                            properties =
                                mapOf("value" to mapOf("path" to "/form/volume"), "max" to 100),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNode(hasSetProgressAction()).assertDoesNotExist()

        controller.updateData("/form/volume", 50)
        controller.waitForIdle()

        onNodeWithText("50").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(50f, 0f..100f, 99))).assertIsDisplayed()

        controller.updateData("/form/volume", null)
        controller.waitForIdle()

        onNodeWithText("50").assertDoesNotExist()
        onNode(hasSetProgressAction()).assertDoesNotExist()
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
                            type = "Slider",
                            properties =
                                mapOf("value" to 20, "max" to 100, "label" to "Tagged Slider"),
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
        onNodeWithText("Tagged Slider").assertIsDisplayed()
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
                            type = "Slider",
                            properties =
                                mapOf("value" to 20, "max" to 100, "label" to "Slider Tag Test"),
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

    private fun hasSetProgressAction(): SemanticsMatcher =
        SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)

    private fun hasProgressBarRangeInfo(
        rangeInfo: ProgressBarRangeInfo,
        tolerance: Float = 0.001f,
    ): SemanticsMatcher =
        SemanticsMatcher("ProgressBarRangeInfo ≈ $rangeInfo") { node ->
            val actual =
                node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
                    ?: return@SemanticsMatcher false
            abs(actual.current - rangeInfo.current) <= tolerance &&
                actual.range == rangeInfo.range &&
                actual.steps == rangeInfo.steps
        }
}
