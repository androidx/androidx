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

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.TextField.Variant
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

private enum class ValidationPreset(val label: String, val regexp: String) {
    None("None", ""),
    Email("Email", "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"),
    LettersOnly("Letters Only", "^[a-zA-Z ]+$"),
    NumbersOnly("Numbers Only", "^[0-9]+$"),
}

@Composable
internal fun TextFieldSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var label by rememberSaveable { mutableStateOf("Username") }
    var initialText by rememberSaveable { mutableStateOf("android_developer") }
    var variant by rememberSaveable { mutableStateOf(Variant.ShortText) }
    var validationRegexp by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        updatePayload(
            label = label,
            initialText = initialText,
            variant = variant,
            validationRegexp = validationRegexp,
            onPayloadUpdated = onPayloadUpdated,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "TextField Variant", subtitle = "Input field mode and keyboard style") {
            ChoiceChips(
                options = Variant.entries,
                selectedOption = variant,
                onOptionSelected = {
                    variant = it
                    updatePayload(
                        label = label,
                        initialText = initialText,
                        variant = it,
                        validationRegexp = validationRegexp,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.value },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Field Label & Initial Value",
            subtitle = "Label and bound text state",
        ) {
            Column {
                TextInputControl(
                    value = label,
                    onValueChange = {
                        label = it
                        updatePayload(
                            label = it,
                            initialText = initialText,
                            variant = variant,
                            validationRegexp = validationRegexp,
                            onPayloadUpdated = onPayloadUpdated,
                        )
                    },
                    label = "Field Label",
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextInputControl(
                    value = initialText,
                    onValueChange = {
                        initialText = it
                        updatePayload(
                            label = label,
                            initialText = it,
                            variant = variant,
                            validationRegexp = validationRegexp,
                            onPayloadUpdated = onPayloadUpdated,
                        )
                    },
                    label = "Initial Value",
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(
            title = "Validation Regexp",
            subtitle = "Client-side regular expression check",
        ) {
            Column {
                ChoiceChips(
                    options = ValidationPreset.entries,
                    selectedOption =
                        ValidationPreset.entries.find { it.regexp == validationRegexp },
                    onOptionSelected = {
                        validationRegexp = it.regexp
                        updatePayload(
                            label = label,
                            initialText = initialText,
                            variant = variant,
                            validationRegexp = it.regexp,
                            onPayloadUpdated = onPayloadUpdated,
                        )
                    },
                    label = { it.label },
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextInputControl(
                    value = validationRegexp,
                    onValueChange = {
                        validationRegexp = it
                        updatePayload(
                            label = label,
                            initialText = initialText,
                            variant = variant,
                            validationRegexp = it,
                            onPayloadUpdated = onPayloadUpdated,
                        )
                    },
                    label = "Regex Pattern",
                )
            }
        }
    }
}

private fun updatePayload(
    label: String,
    initialText: String,
    variant: Variant,
    validationRegexp: String,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val properties =
        buildMap<String, Any?> {
            put("label", label)
            put("value", mapOf("path" to "/textValue"))
            put("variant", variant.value)
            if (validationRegexp.isNotBlank()) {
                put("validationRegexp", validationRegexp)
            }
        }
    val component =
        A2uiComponentPayload(
            id = "root",
            type = "TextField",
            properties = properties,
        )
    val dataModel = mapOf("textValue" to initialText)
    onPayloadUpdated(listOf(component), dataModel)
}
