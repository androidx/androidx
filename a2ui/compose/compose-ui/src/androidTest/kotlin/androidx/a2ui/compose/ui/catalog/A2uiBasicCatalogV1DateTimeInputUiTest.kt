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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiBasicCatalogV1DateTimeInputUiTest {

    private class TestDateTimeInput : A2uiBasicCatalogV1.DateTimeInput {
        var capturedValue: Long? = null
        var capturedEnableDate: Boolean? = null
        var capturedEnableTime: Boolean? = null
        var capturedMin: Long? = null
        var capturedMax: Long? = null
        var capturedLabel: String? = null
        var capturedOnValueChange: ((Long?) -> Unit)? = null

        @Composable
        override fun A2uiComponentScope.TypedContent(
            value: Long?,
            onValueChange: ((Long?) -> Unit)?,
            enableDate: Boolean,
            enableTime: Boolean,
            min: Long?,
            max: Long?,
            label: String?,
            modifier: Modifier,
        ) {
            SideEffect {
                capturedValue = value
                capturedEnableDate = enableDate
                capturedEnableTime = enableTime
                capturedMin = min
                capturedMax = max
                capturedLabel = label
                capturedOnValueChange = onValueChange
            }
            BasicText(
                text = "DateTimeInput: $value (date=$enableDate, time=$enableTime, label=$label)",
                modifier = modifier,
            )
        }
    }

    private val testDateTimeInput = TestDateTimeInput()

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testDateTimeInput),
            functions = emptyList(),
        )

    @Test
    fun content_resolvesPropertiesAndPassesThemToTypedContent() = runComposeUiTest {
        val valueString = "2026-03-24T15:25:00"
        val minString = "2026-01-01T00:00:00"
        val maxString = "2026-12-31T23:59:59"
        val expectedValueMillis = parseIsoToUtcMillis(valueString)
        val expectedMinMillis = parseIsoToUtcMillis(minString)
        val expectedMaxMillis = parseIsoToUtcMillis(maxString)

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
                                    "value" to valueString,
                                    "enableDate" to true,
                                    "enableTime" to true,
                                    "min" to minString,
                                    "max" to maxString,
                                    "label" to "Appointment Time",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText(
                "DateTimeInput: $expectedValueMillis (date=true, time=true, label=Appointment Time)"
            )
            .assertIsDisplayed()
        assertThat(testDateTimeInput.capturedValue).isEqualTo(expectedValueMillis)
        assertThat(testDateTimeInput.capturedEnableDate).isTrue()
        assertThat(testDateTimeInput.capturedEnableTime).isTrue()
        assertThat(testDateTimeInput.capturedMin).isEqualTo(expectedMinMillis)
        assertThat(testDateTimeInput.capturedMax).isEqualTo(expectedMaxMillis)
        assertThat(testDateTimeInput.capturedLabel).isEqualTo("Appointment Time")
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
                            type = "DateTimeInput",
                            properties = mapOf("value" to "2026-03-24"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasTestTag("custom_tag")).assertIsDisplayed()
    }

    @Test
    fun content_onValueChange_updatesDataModel() = runComposeUiTest {
        val initialDateString = "2026-03-24"
        val initialMillis = parseIsoToUtcMillis(initialDateString)
        val updatedDateString = "2026-03-25"
        val updatedMillis = parseIsoToUtcMillis(updatedDateString)

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
                                    "value" to mapOf("path" to "/booking/date"),
                                    "enableDate" to true,
                                ),
                        )
                    ),
                initialData = mapOf("booking" to mapOf("date" to initialDateString)),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("DateTimeInput: $initialMillis (date=true, time=false, label=null)")
            .assertIsDisplayed()

        assertThat(testDateTimeInput.capturedOnValueChange).isNotNull()
        testDateTimeInput.capturedOnValueChange?.invoke(updatedMillis)
        controller.waitForIdle()
        waitForIdle()

        assertThat(controller.getData<String>("/booking/date")).isEqualTo(updatedDateString)
        onNodeWithText("DateTimeInput: $updatedMillis (date=true, time=false, label=null)")
            .assertIsDisplayed()
    }

    @Test
    fun content_propertyUpdates_recomposesWithNewValues() = runComposeUiTest {
        val initialDateString = "2026-03-24"
        val initialMillis = parseIsoToUtcMillis(initialDateString)
        val updatedDateString = "2026-03-24T10:00:00"
        val updatedMillis = parseIsoToUtcMillis(updatedDateString)

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
                                    "value" to initialDateString,
                                    "enableDate" to true,
                                    "enableTime" to false,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("DateTimeInput: $initialMillis (date=true, time=false, label=null)")
            .assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "value" to updatedDateString,
                    "enableDate" to true,
                    "enableTime" to true,
                    "label" to "Updated Label",
                ),
        )
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("DateTimeInput: $updatedMillis (date=true, time=true, label=Updated Label)")
            .assertIsDisplayed()
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
                            type = "DateTimeInput",
                            properties = mapOf("value" to "2026-03-24"),
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
    fun isReady_pendingDynamicData_returnsFalseAndGuardsContent() = runComposeUiTest {
        val dateString = "2026-03-24T15:25:00"
        val expectedMillis = parseIsoToUtcMillis(dateString)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties = mapOf("value" to mapOf("path" to "/pendingData")),
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

        controller.updateData("/pendingData", dateString)
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("DateTimeInput: $expectedMillis (date=false, time=false, label=null)")
            .assertIsDisplayed()
    }

    @Test
    fun isReady_dynamicDataErased_transitionsFromReadyToPending() = runComposeUiTest {
        val dateString = "2026-03-24T15:25:00"
        val expectedMillis = parseIsoToUtcMillis(dateString)

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties = mapOf("value" to mapOf("path" to "/form/datetime")),
                        )
                    ),
                initialData = mapOf("form" to mapOf("datetime" to dateString)),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("DateTimeInput: $expectedMillis (date=false, time=false, label=null)")
            .assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/form/datetime", null)
        controller.waitForIdle()

        onNodeWithText("DateTimeInput: $expectedMillis (date=false, time=false, label=null)")
            .assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_invalidDateFormat_reportsErrorAndGuardsContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties = mapOf("value" to "not-a-valid-date"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, onError = { _, _ -> }) }
        waitForIdle()
        controller.waitForIdle()

        val error = controller.outboundErrors.single()
        assertThat(error.message).isEqualTo("Invalid date-time format: not-a-valid-date")
    }

    @Test
    fun content_fractionalSeconds_parsesSuccessfully() = runComposeUiTest {
        val dateString = "2026-03-24T15:25:00.123Z"
        val expectedMillis = parseIsoToUtcMillis(dateString)

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
                                    "value" to dateString,
                                    "enableDate" to true,
                                    "enableTime" to true,
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("DateTimeInput: $expectedMillis (date=true, time=true, label=null)")
            .assertIsDisplayed()
    }

    @Test
    fun isReady_emptyValue_returnsTrueAndPassesNullValueToContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "DateTimeInput",
                            properties = mapOf("value" to ""),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("DateTimeInput: null (date=false, time=false, label=null)")
            .assertIsDisplayed()
        assertThat(testDateTimeInput.capturedValue).isNull()
    }

    private fun parseIsoToUtcMillis(value: String): Long =
        checkNotNull(parseIsoDateTimeToUtcMillis(value)) {
            "Unable to parse test date-time string: $value"
        }
}
