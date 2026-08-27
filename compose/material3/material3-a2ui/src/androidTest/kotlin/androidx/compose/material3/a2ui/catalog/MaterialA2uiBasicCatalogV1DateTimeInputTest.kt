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
import androidx.a2ui.model.catalog.functions.A2uiFormatDateFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialA2uiBasicCatalogV1DateTimeInputTest {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1Defaults.dateTimeInput),
            functions = listOf(A2uiFormatDateFunction.INSTANCE),
        )

    private val dateFormatter =
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())

    @Test
    fun bothDateAndTimeEnabled_rendersBothChipsWithFormattedValues() = runComposeUiTest {
        val expectedDate = formatTestDate(2026, 3, 24)
        val expectedTime = formatTestTime(15, 25)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to "2026-03-24T15:25:00",
                                    "enableDate" to true,
                                    "enableTime" to true,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedDate).assertIsDisplayed()
        onNodeWithText(expectedTime).assertIsDisplayed()
    }

    @Test
    fun dateOnlyEnabled_rendersDateChipOnly() = runComposeUiTest {
        val expectedDate = formatTestDate(2026, 3, 24)
        val expectedTime = formatTestTime(15, 25)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to "2026-03-24",
                                    "enableDate" to true,
                                    "enableTime" to false,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedDate).assertIsDisplayed()
        onNodeWithText(expectedTime).assertDoesNotExist()
    }

    @Test
    fun timeOnlyEnabled_rendersTimeChipOnly() = runComposeUiTest {
        val expectedDate = formatTestDate(2026, 3, 24)
        val expectedTime = formatTestTime(15, 25)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to "15:25:00",
                                    "enableDate" to false,
                                    "enableTime" to true,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedTime).assertIsDisplayed()
        onNodeWithText(expectedDate).assertDoesNotExist()
    }

    @Test
    fun unsetValue_rendersPlaceholderChips() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf("value" to "", "enableDate" to true, "enableTime" to true),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNodeWithText("Select date").assertIsDisplayed()
        onNodeWithText("Select time").assertIsDisplayed()
    }

    @Test
    fun staticValue_chipsAreDisabled() = runComposeUiTest {
        val expectedDate = formatTestDate(2026, 3, 24)
        val expectedTime = formatTestTime(15, 25)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to "2026-03-24T15:25:00",
                                    "enableDate" to true,
                                    "enableTime" to true,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedDate).assertIsDisplayed().assertIsNotEnabled()
        onNodeWithText(expectedTime).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun dynamicBinding_chipsAreEnabled() = runComposeUiTest {
        val expectedDate = formatTestDate(2026, 3, 24)
        val expectedTime = formatTestTime(15, 25)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/datetime"),
                                    "enableDate" to true,
                                    "enableTime" to true,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("datetime" to "2026-03-24T15:25:00")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedDate).assertIsDisplayed().assertIsEnabled()
        onNodeWithText(expectedTime).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun datePickerInteraction_clickAndConfirm_updatesDataModel() = runComposeUiTest {
        val expectedInitialDate = formatTestDate(2026, 3, 24)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/date"),
                                    "enableDate" to true,
                                    "enableTime" to false,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("date" to "2026-03-24")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialDate).assertIsDisplayed().performClick()
        waitForIdle()

        // Dialog is open; click confirm "OK"
        onNodeWithText("OK").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.getData<String>("/form/date")).isEqualTo("2026-03-24")
    }

    @Test
    fun datePickerInteraction_clickAndDismiss_doesNotUpdateDataModel() = runComposeUiTest {
        val expectedInitialDate = formatTestDate(2026, 3, 24)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/date"),
                                    "enableDate" to true,
                                    "enableTime" to false,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("date" to "2026-03-24")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialDate).assertIsDisplayed().performClick()
        waitForIdle()

        // Dialog is open; click dismiss "Cancel"
        onNodeWithText("Cancel").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.getData<String>("/form/date")).isEqualTo("2026-03-24")
    }

    @Test
    fun bothDateAndTimeEnabled_datePickerInteraction_clickAndConfirm_preservesTimeAndUpdateDate() =
        runComposeUiTest {
            val expectedInitialDate = formatTestDate(2026, 3, 24)

            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(
                            A2uiComponentPayload(
                                id = "root",
                                type = "DateTimeInput",
                                properties =
                                    mapOf(
                                        "value" to mapOf("path" to "/form/datetime"),
                                        "enableDate" to true,
                                        "enableTime" to true,
                                    ),
                            )
                        ),
                    initialData = mapOf("form" to mapOf("datetime" to "2026-03-24T15:25:00")),
                )
            val surface = controller.start()

            setContent { MaterialTheme { A2uiTestSurface(surface) } }

            onNodeWithText(expectedInitialDate).assertIsDisplayed().performClick()
            waitForIdle()

            // Dialog is open; click confirm "OK"
            onNodeWithText("OK").assertIsDisplayed().performClick()
            waitForIdle()
            controller.waitForIdle()

            assertThat(controller.getData<String>("/form/datetime"))
                .isEqualTo("2026-03-24T15:25:00Z")
        }

    @Test
    fun timePickerInteraction_clickAndConfirm_updatesDataModel() = runComposeUiTest {
        val expectedInitialTime = formatTestTime(15, 25)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/time"),
                                    "enableDate" to false,
                                    "enableTime" to true,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("time" to "15:25:00")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialTime).assertIsDisplayed().performClick()
        waitForIdle()

        // Dialog is open; click confirm "OK"
        onNodeWithText("OK").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.getData<String>("/form/time")).isEqualTo("15:25:00")
    }

    @Test
    fun timePickerInteraction_clickAndDismiss_doesNotUpdateDataModel() = runComposeUiTest {
        val expectedInitialTime = formatTestTime(15, 25)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/time"),
                                    "enableDate" to false,
                                    "enableTime" to true,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("time" to "15:25:00")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialTime).assertIsDisplayed().performClick()
        waitForIdle()

        // Dialog is open; click dismiss "Cancel"
        onNodeWithText("Cancel").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.getData<String>("/form/time")).isEqualTo("15:25:00")
    }

    @Test
    fun bothDateAndTimeEnabled_timePickerInteraction_clickAndConfirm_preservesDateAndUpdateTime() =
        runComposeUiTest {
            val expectedInitialTime = formatTestTime(15, 25)

            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(
                            A2uiComponentPayload(
                                id = "root",
                                type = "DateTimeInput",
                                properties =
                                    mapOf(
                                        "value" to mapOf("path" to "/form/datetime"),
                                        "enableDate" to true,
                                        "enableTime" to true,
                                    ),
                            )
                        ),
                    initialData = mapOf("form" to mapOf("datetime" to "2026-03-24T15:25:00")),
                )
            val surface = controller.start()

            setContent { MaterialTheme { A2uiTestSurface(surface) } }

            onNodeWithText(expectedInitialTime).assertIsDisplayed().performClick()
            waitForIdle()

            // Dialog is open; click confirm "OK"
            onNodeWithText("OK").assertIsDisplayed().performClick()
            waitForIdle()
            controller.waitForIdle()

            assertThat(controller.getData<String>("/form/datetime"))
                .isEqualTo("2026-03-24T15:25:00Z")
        }

    @Test
    fun value_dynamicBinding_externalDataChange_updatesDateTime() = runComposeUiTest {
        val expectedInitialDate = formatTestDate(2026, 3, 24)
        val expectedInitialTime = formatTestTime(15, 25)
        val expectedUpdatedDate = formatTestDate(2026, 4, 10)
        val expectedUpdatedTime = formatTestTime(10, 0)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/datetime"),
                                    "enableDate" to true,
                                    "enableTime" to true,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("datetime" to "2026-03-24T15:25:00")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialDate).assertIsDisplayed()
        onNodeWithText(expectedInitialTime).assertIsDisplayed()

        controller.updateData("/form/datetime", "2026-04-10T10:00:00")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText(expectedInitialDate).assertDoesNotExist()
        onNodeWithText(expectedInitialTime).assertDoesNotExist()
        onNodeWithText(expectedUpdatedDate).assertIsDisplayed()
        onNodeWithText(expectedUpdatedTime).assertIsDisplayed()
    }

    @Test
    fun value_componentPayloadUpdate_updatesDateTime() = runComposeUiTest {
        val expectedInitialDate = formatTestDate(2026, 3, 24)
        val expectedInitialTime = formatTestTime(15, 25)
        val expectedUpdatedDate = formatTestDate(2026, 4, 10)
        val expectedUpdatedTime = formatTestTime(10, 0)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to "2026-03-24T15:25:00",
                                    "enableDate" to true,
                                    "enableTime" to true,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialDate).assertIsDisplayed()
        onNodeWithText(expectedInitialTime).assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("value" to "2026-04-10T10:00:00", "enableDate" to true, "enableTime" to true),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText(expectedInitialDate).assertDoesNotExist()
        onNodeWithText(expectedInitialTime).assertDoesNotExist()
        onNodeWithText(expectedUpdatedDate).assertIsDisplayed()
        onNodeWithText(expectedUpdatedTime).assertIsDisplayed()
    }

    @Test
    fun label_rendersLabelAboveChips() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to "2026-03-24",
                                    "enableDate" to true,
                                    "label" to "Meeting Date",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Meeting Date").assertIsDisplayed()
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
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to "2026-03-24",
                                    "enableDate" to true,
                                    "label" to mapOf("path" to "/form/label"),
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("label" to "Initial Title")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Initial Title").assertIsDisplayed()

        controller.updateData("/form/label", "Updated Title")
        controller.waitForIdle()
        waitForIdle()

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
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to "2026-03-24",
                                    "enableDate" to true,
                                    "label" to "Initial Label",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Initial Label").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("value" to "2026-03-24", "enableDate" to true, "label" to "Updated Label"),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("Initial Label").assertDoesNotExist()
        onNodeWithText("Updated Label").assertIsDisplayed()
    }

    @Test
    fun isReady_waitsForDataModelValue() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/datetime"),
                                    "enableDate" to true,
                                    "enableTime" to true,
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
                        Text("Loading DateTime...", modifier = modifier.testTag("custom_loader"))
                    },
                )
            }
        }

        onNodeWithTag("custom_loader").assertIsDisplayed()

        controller.updateData("/form/datetime", "2026-03-24T15:25:00")
        controller.waitForIdle()

        val expectedDate = formatTestDate(2026, 3, 24)
        onNodeWithTag("custom_loader").assertDoesNotExist()
        onNodeWithText(expectedDate).assertIsDisplayed()
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
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/datetime"),
                                    "enableDate" to true,
                                    "enableTime" to true,
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
                        Text("Loading DateTime...", modifier = modifier.testTag("custom_loader"))
                    },
                )
            }
        }

        onNodeWithTag("custom_loader").assertIsDisplayed()

        controller.updateData("/form/datetime", "2026-03-24T15:25:00")
        controller.waitForIdle()
        waitForIdle()

        val expectedDate = formatTestDate(2026, 3, 24)
        onNodeWithTag("custom_loader").assertDoesNotExist()
        onNodeWithText(expectedDate).assertIsDisplayed()

        controller.updateData("/form/datetime", null)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText(expectedDate).assertDoesNotExist()
        onNodeWithTag("custom_loader").assertIsDisplayed()
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
                            type = "DateTimeInput",
                            properties = mapOf("value" to "2026-03-24", "enableDate" to true),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag"))
            }
        }

        onNode(hasTestTag("custom_tag")).assertIsDisplayed()
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
                            type = "DateTimeInput",
                            properties = mapOf("value" to "2026-03-24", "enableDate" to true),
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
    fun isReady_unparsedDateTime_reportsErrorAndGuardsContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/datetime"),
                                    "enableDate" to true,
                                    "enableTime" to true,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("datetime" to "not-a-valid-datetime")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface, onError = { _, _ -> }) } }
        waitForIdle()
        controller.waitForIdle()

        val error = controller.outboundErrors.single()
        assertThat(error.message).isEqualTo("Invalid date-time format: not-a-valid-datetime")
    }

    @Test
    fun validation_minGreaterThanMax_reportsError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/datetime"),
                                    "enableDate" to true,
                                    "enableTime" to true,
                                    "min" to "2026-04-01T00:00:00",
                                    "max" to "2026-03-01T00:00:00",
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("datetime" to "2026-03-24T15:25:00")),
            )
        val surface = controller.start()

        val minMillis = parseIsoToUtcMillis("2026-04-01T00:00:00")
        val maxMillis = parseIsoToUtcMillis("2026-03-01T00:00:00")

        setContent { MaterialTheme { A2uiTestSurface(surface = surface, onError = { _, _ -> }) } }
        waitForIdle()
        controller.waitForIdle()

        val error = controller.outboundErrors.single()
        assertThat(error.message)
            .isEqualTo("Min value ($minMillis) cannot be greater than max value ($maxMillis).")
    }

    @Test
    fun validation_disabled_doesNotReportError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to "2026-03-24",
                                    "enableDate" to true,
                                    "enableTime" to false,
                                    "min" to "2026-03-01",
                                    "max" to "2026-04-01",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        val expectedDate = formatTestDate(2026, 3, 24)
        onNodeWithText(expectedDate).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun minMax_staticString_handlesBoundsAndAllowsInteraction() = runComposeUiTest {
        val expectedInitialDate = formatTestDate(2026, 3, 15)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/date"),
                                    "enableDate" to true,
                                    "enableTime" to false,
                                    "min" to "2026-03-01T00:00",
                                    "max" to "2026-03-31T23:59",
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("date" to "2026-03-15")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNodeWithText(expectedInitialDate).assertIsDisplayed().performClick()
        waitForIdle()

        // Date picker dialog is displayed with confirm button
        onNodeWithText("OK").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.getData<String>("/form/date")).isEqualTo("2026-03-15")
    }

    @Test
    fun minMax_dynamicFunctionCall_handlesBoundsAndAllowsInteraction() = runComposeUiTest {
        val expectedInitialDate = formatTestDate(2026, 3, 15)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/date"),
                                    "enableDate" to true,
                                    "enableTime" to false,
                                    "min" to
                                        mapOf(
                                            "call" to "formatDate",
                                            "args" to
                                                mapOf(
                                                    "value" to 1772323200000L, // 2026-03-01 UTC
                                                    "format" to "ISO",
                                                ),
                                            "returnType" to "string",
                                        ),
                                    "max" to
                                        mapOf(
                                            "call" to "formatDate",
                                            "args" to
                                                mapOf(
                                                    "value" to 1774915200000L, // 2026-03-31 UTC
                                                    "format" to "ISO",
                                                ),
                                            "returnType" to "string",
                                        ),
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("date" to "2026-03-15")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNodeWithText(expectedInitialDate).assertIsDisplayed().performClick()
        waitForIdle()

        // Date picker dialog is displayed with confirm button
        onNodeWithText("OK").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.getData<String>("/form/date")).isEqualTo("2026-03-15")
    }

    @Test
    fun minMax_dynamicDataBinding_handlesBoundsAndAllowsInteraction() = runComposeUiTest {
        val expectedInitialDate = formatTestDate(2026, 3, 15)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/date"),
                                    "enableDate" to true,
                                    "enableTime" to false,
                                    "min" to mapOf("path" to "/form/minDate"),
                                    "max" to mapOf("path" to "/form/maxDate"),
                                ),
                        )
                    ),
                initialData =
                    mapOf(
                        "form" to
                            mapOf(
                                "date" to "2026-03-15",
                                "minDate" to "2026-03-01",
                                "maxDate" to "2026-03-31",
                            )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface = surface) } }

        onNodeWithText(expectedInitialDate).assertIsDisplayed().performClick()
        waitForIdle()

        // Date picker dialog is displayed with confirm button
        onNodeWithText("OK").assertIsDisplayed().performClick()
        waitForIdle()
        controller.waitForIdle()

        assertThat(controller.getData<String>("/form/date")).isEqualTo("2026-03-15")
    }

    @Test
    fun datePickerDialog_dismissOnBackPress_dismissesDialog() = runComposeUiTest {
        val expectedInitialDate = formatTestDate(2026, 3, 24)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/date"),
                                    "enableDate" to true,
                                    "enableTime" to false,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("date" to "2026-03-24")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialDate).assertIsDisplayed().performClick()
        waitForIdle()

        onNodeWithText("OK").assertIsDisplayed()

        Espresso.pressBack()
        waitForIdle()

        onNodeWithText("OK").assertDoesNotExist()
        assertThat(controller.getData<String>("/form/date")).isEqualTo("2026-03-24")
    }

    @Test
    fun timePickerDialog_dismissOnBackPress_dismissesDialog() = runComposeUiTest {
        val expectedInitialTime = formatTestTime(15, 25)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/time"),
                                    "enableDate" to false,
                                    "enableTime" to true,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("time" to "15:25:00")),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialTime).assertIsDisplayed().performClick()
        waitForIdle()

        onNodeWithText("OK").assertIsDisplayed()

        Espresso.pressBack()
        waitForIdle()

        onNodeWithText("OK").assertDoesNotExist()
        assertThat(controller.getData<String>("/form/time")).isEqualTo("15:25:00")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun datePickerDialog_stateRestoration_dialogRemainsOpen() = runComposeUiTest {
        val expectedInitialDate = formatTestDate(2026, 3, 24)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/date"),
                                    "enableDate" to true,
                                    "enableTime" to false,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("date" to "2026-03-24")),
            )
        val surface = controller.start()
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialDate).assertIsDisplayed().performClick()
        waitForIdle()

        onNodeWithText("OK").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("OK").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timePickerDialog_stateRestoration_dialogRemainsOpen() = runComposeUiTest {
        val expectedInitialTime = formatTestTime(15, 25)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties =
                                mapOf(
                                    "value" to mapOf("path" to "/form/time"),
                                    "enableDate" to false,
                                    "enableTime" to true,
                                ),
                        )
                    ),
                initialData = mapOf("form" to mapOf("time" to "15:25:00")),
            )
        val surface = controller.start()
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText(expectedInitialTime).assertIsDisplayed().performClick()
        waitForIdle()

        onNodeWithText("OK").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("OK").assertIsDisplayed()
    }

    private fun formatTestDate(year: Int, month: Int, day: Int): String {
        val calendar =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
            }
        return dateFormatter.format(calendar.time)
    }

    private fun formatTestTime(hour: Int, minute: Int): String {
        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
        return timeFormatter.format(calendar.time)
    }

    private fun parseIsoToUtcMillis(value: String): Long {
        val patterns = arrayOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "HH:mm:ss")
        for (pattern in patterns) {
            try {
                val dateFormat =
                    SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                        isLenient = false
                    }
                val date = dateFormat.parse(value)
                if (date != null) return date.time
            } catch (_: Exception) {
                // Ignore and try next pattern
            }
        }
        error("Failed to parse: $value")
    }
}
