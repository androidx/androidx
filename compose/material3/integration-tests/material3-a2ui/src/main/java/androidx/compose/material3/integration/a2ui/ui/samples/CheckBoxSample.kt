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
import androidx.compose.material3.integration.a2ui.ui.ControlCard
import androidx.compose.material3.integration.a2ui.ui.SwitchControl
import androidx.compose.material3.integration.a2ui.ui.TextInputControl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CheckBoxSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var label by rememberSaveable { mutableStateOf("Receive notification updates") }
    var isChecked by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        updatePayload(label = label, isChecked = isChecked, onPayloadUpdated = onPayloadUpdated)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "CheckBox Label", subtitle = "Text label placed beside the CheckBox") {
            TextInputControl(
                value = label,
                onValueChange = {
                    label = it
                    updatePayload(
                        label = it,
                        isChecked = isChecked,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = "Label",
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Checked State",
            subtitle = "Two-way bound state in the A2UI data model (/checkboxValue)",
        ) {
            SwitchControl(
                title = "Checked",
                subtitle = "Toggle to update the underlying state model",
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    updatePayload(
                        label = label,
                        isChecked = it,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
            )
        }
    }
}

private fun updatePayload(
    label: String,
    isChecked: Boolean,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val component =
        A2uiComponentPayload(
            id = "root",
            type = "CheckBox",
            properties =
                mapOf(
                    "label" to label,
                    "value" to mapOf("path" to "/checkboxValue"),
                ),
        )
    val dataModel = mapOf("checkboxValue" to isChecked)
    onPayloadUpdated(listOf(component), dataModel)
}
