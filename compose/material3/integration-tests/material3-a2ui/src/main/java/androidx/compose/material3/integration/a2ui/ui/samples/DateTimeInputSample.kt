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

package androidx.compose.material3.integration.a2ui.ui.samples

import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.integration.a2ui.ui.ChoiceChips
import androidx.compose.material3.integration.a2ui.ui.ControlCard
import androidx.compose.material3.integration.a2ui.ui.TextInputControl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class DateTimeMode(val label: String, val date: Boolean, val time: Boolean) {
    DATE_ONLY("Date Only", date = true, time = false),
    TIME_ONLY("Time Only", date = false, time = true),
    DATE_AND_TIME("Date & Time", date = true, time = true),
}

@Composable
internal fun DateTimeInputSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by rememberSaveable { mutableStateOf(DateTimeMode.DATE_AND_TIME) }
    var label by rememberSaveable { mutableStateOf("Select Appointment") }
    var dateTimeValue by rememberSaveable { mutableStateOf("2026-09-03T14:30:00Z") }

    LaunchedEffect(Unit) {
        updatePayload(
            mode = mode,
            label = label,
            dateTimeValue = dateTimeValue,
            onPayloadUpdated = onPayloadUpdated,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(
            title = "Input Mode",
            subtitle = "Configures date and time picker capabilities",
        ) {
            ChoiceChips(
                options = DateTimeMode.entries,
                selectedOption = mode,
                onOptionSelected = {
                    mode = it
                    updatePayload(
                        mode = it,
                        label = label,
                        dateTimeValue = dateTimeValue,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.label },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(title = "Label", subtitle = "Prompt text displayed for the date time input") {
            TextInputControl(
                value = label,
                onValueChange = {
                    label = it
                    updatePayload(
                        mode = mode,
                        label = it,
                        dateTimeValue = dateTimeValue,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = "Label Text",
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Value (ISO 8601)",
            subtitle = "Preset date-time string in A2UI data model",
        ) {
            ChoiceChips(
                options =
                    listOf("2026-09-03T14:30:00Z", "2026-12-25T09:00:00Z", "2027-01-01T00:00:00Z"),
                selectedOption = dateTimeValue,
                onOptionSelected = {
                    dateTimeValue = it
                    updatePayload(
                        mode = mode,
                        label = label,
                        dateTimeValue = it,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
            )
        }
    }
}

private fun updatePayload(
    mode: DateTimeMode,
    label: String,
    dateTimeValue: String,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val component =
        A2uiComponentPayload(
            id = "root",
            type = "DateTimeInput",
            properties =
                mapOf(
                    "label" to label,
                    "enableDate" to mode.date,
                    "enableTime" to mode.time,
                    "value" to mapOf("path" to "/dateTimeValue"),
                ),
        )
    val dataModel = mapOf("dateTimeValue" to dateTimeValue)
    onPayloadUpdated(listOf(component), dataModel)
}
