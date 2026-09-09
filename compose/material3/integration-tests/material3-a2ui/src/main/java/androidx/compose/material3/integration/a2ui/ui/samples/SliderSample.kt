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
import androidx.compose.material3.integration.a2ui.ui.SliderControl
import androidx.compose.material3.integration.a2ui.ui.TextInputControl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SliderSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var label by rememberSaveable { mutableStateOf("Audio Volume") }
    var maxVal by rememberSaveable { mutableFloatStateOf(100f) }
    var currentVal by rememberSaveable { mutableFloatStateOf(65f) }

    LaunchedEffect(Unit) {
        updatePayload(
            label = label,
            maxVal = maxVal,
            currentVal = currentVal,
            onPayloadUpdated = onPayloadUpdated,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "Slider Label", subtitle = "Text label displayed adjacent to slider") {
            TextInputControl(
                value = label,
                onValueChange = {
                    label = it
                    updatePayload(
                        label = it,
                        maxVal = maxVal,
                        currentVal = currentVal,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = "Label",
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(title = "Maximum Bound", subtitle = "The upper boundary value for the slider") {
            ChoiceChips(
                options = listOf(50f, 100f, 200f),
                selectedOption = maxVal,
                onOptionSelected = {
                    maxVal = it
                    val adjustedCurrent = if (currentVal > it) it else currentVal
                    currentVal = adjustedCurrent
                    updatePayload(
                        label = label,
                        maxVal = it,
                        currentVal = adjustedCurrent,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { "Max: ${it.toInt()}" },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Data Model Value",
            subtitle = "Two-way bound value in the A2UI state model (/sliderValue)",
        ) {
            SliderControl(
                title = "Current Value",
                value = currentVal,
                valueRange = 0f..maxVal,
                onValueChange = {
                    currentVal = it
                    updatePayload(
                        label = label,
                        maxVal = maxVal,
                        currentVal = it,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                valueDisplay = "${currentVal.toInt()}",
            )
        }
    }
}

private fun updatePayload(
    label: String,
    maxVal: Float,
    currentVal: Float,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val component =
        A2uiComponentPayload(
            id = "root",
            type = "Slider",
            properties =
                mapOf(
                    "label" to label,
                    "min" to 0,
                    "max" to maxVal.toInt(),
                    "value" to mapOf("path" to "/sliderValue"),
                ),
        )
    val dataModel = mapOf<String, Any?>("sliderValue" to currentVal.toInt())
    onPayloadUpdated(listOf(component), dataModel)
}
