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

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Button.Variant
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

internal enum class ButtonChildType(val label: String) {
    TEXT("Text Label"),
    ICON("Icon Only"),
}

@Composable
internal fun ButtonSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var variant by rememberSaveable { mutableStateOf(Variant.Primary) }
    var childType by rememberSaveable { mutableStateOf(ButtonChildType.TEXT) }
    var buttonText by rememberSaveable { mutableStateOf("Confirm & Continue") }
    var iconName by rememberSaveable { mutableStateOf("favorite") }

    LaunchedEffect(Unit) {
        updatePayload(
            variant = variant,
            childType = childType,
            buttonText = buttonText,
            iconName = iconName,
            onPayloadUpdated = onPayloadUpdated,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "Button Style Variant", subtitle = "Visual importance and style tier") {
            ChoiceChips(
                options = Variant.entries,
                selectedOption = variant,
                onOptionSelected = {
                    variant = it
                    updatePayload(
                        variant = it,
                        childType = childType,
                        buttonText = buttonText,
                        iconName = iconName,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.value },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(title = "Content Type", subtitle = "Child element embedded inside the button") {
            ChoiceChips(
                options = ButtonChildType.entries,
                selectedOption = childType,
                onOptionSelected = {
                    childType = it
                    updatePayload(
                        variant = variant,
                        childType = it,
                        buttonText = buttonText,
                        iconName = iconName,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.label },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (childType) {
            ButtonChildType.TEXT -> {
                ControlCard(title = "Label Text", subtitle = "The text to display on the button") {
                    TextInputControl(
                        value = buttonText,
                        onValueChange = {
                            buttonText = it
                            updatePayload(
                                variant = variant,
                                childType = childType,
                                buttonText = it,
                                iconName = iconName,
                                onPayloadUpdated = onPayloadUpdated,
                            )
                        },
                        label = "Button Text",
                    )
                }
            }
            ButtonChildType.ICON -> {
                ControlCard(
                    title = "Icon Selection",
                    subtitle = "Icon token to render inside button",
                ) {
                    ChoiceChips(
                        options = listOf("favorite", "star", "check", "settings", "search", "send"),
                        selectedOption = iconName,
                        onOptionSelected = {
                            iconName = it
                            updatePayload(
                                variant = variant,
                                childType = childType,
                                buttonText = buttonText,
                                iconName = it,
                                onPayloadUpdated = onPayloadUpdated,
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun updatePayload(
    variant: Variant,
    childType: ButtonChildType,
    buttonText: String,
    iconName: String,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val childComponent =
        when (childType) {
            ButtonChildType.TEXT ->
                A2uiComponentPayload(
                    id = "btn_text",
                    type = "Text",
                    properties = mapOf("text" to buttonText),
                )
            ButtonChildType.ICON ->
                A2uiComponentPayload(
                    id = "btn_icon",
                    type = "Icon",
                    properties = mapOf("name" to iconName),
                )
        }

    val rootComponent =
        A2uiComponentPayload(
            id = "root",
            type = "Button",
            properties =
                mapOf(
                    "child" to childComponent.id,
                    "variant" to variant.value,
                    "action" to mapOf("event" to mapOf("name" to "button_click")),
                ),
        )

    onPayloadUpdated(listOf(rootComponent, childComponent), emptyMap())
}
