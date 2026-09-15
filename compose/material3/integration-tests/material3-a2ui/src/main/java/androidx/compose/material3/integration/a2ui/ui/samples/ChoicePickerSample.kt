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

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.ChoicePicker.DisplayStyle
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.ChoicePicker.Variant
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.integration.a2ui.ui.ChoiceChips
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
internal fun ChoicePickerSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var variant by rememberSaveable { mutableStateOf(Variant.MutuallyExclusive) }
    var displayStyle by rememberSaveable { mutableStateOf(DisplayStyle.Checkbox) }
    var isFilterable by rememberSaveable { mutableStateOf(value = false) }
    var label by rememberSaveable { mutableStateOf("Select your favorite cuisine") }
    var selectedPreset by rememberSaveable { mutableStateOf(OptionPreset.CUISINES) }
    var selectedValues by rememberSaveable { mutableStateOf(listOf("italian")) }

    LaunchedEffect(Unit) {
        updatePayload(
            variant = variant,
            displayStyle = displayStyle,
            isFilterable = isFilterable,
            label = label,
            options = selectedPreset.options,
            selectedValues = selectedValues,
            onPayloadUpdated = onPayloadUpdated,
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ControlCard(
            title = "Variant",
            subtitle = "Selection behavior hint (single or multiple)",
        ) {
            ChoiceChips(
                options = Variant.entries,
                selectedOption = variant,
                onOptionSelected = { newVariant ->
                    variant = newVariant
                    val newSelection =
                        if (
                            (newVariant == Variant.MutuallyExclusive) && (selectedValues.size > 1)
                        ) {
                            listOf(selectedValues.first())
                        } else {
                            selectedValues
                        }
                    selectedValues = newSelection
                    updatePayload(
                        variant = newVariant,
                        displayStyle = displayStyle,
                        isFilterable = isFilterable,
                        label = label,
                        options = selectedPreset.options,
                        selectedValues = newSelection,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { option ->
                    when (option) {
                        Variant.MutuallyExclusive -> "Mutually Exclusive"
                        Variant.MultipleSelection -> "Multiple Selection"
                    }
                },
            )
        }

        ControlCard(
            title = "Display Style",
            subtitle = "Visual presentation style for choices",
        ) {
            ChoiceChips(
                options = DisplayStyle.entries,
                selectedOption = displayStyle,
                onOptionSelected = {
                    displayStyle = it
                    updatePayload(
                        variant = variant,
                        displayStyle = it,
                        isFilterable = isFilterable,
                        label = label,
                        options = selectedPreset.options,
                        selectedValues = selectedValues,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { option ->
                    when (option) {
                        DisplayStyle.Checkbox -> "Checkbox"
                        DisplayStyle.Chips -> "Chips"
                    }
                },
            )
        }

        ControlCard(
            title = "Filtering",
            subtitle = "Displays a search input to filter available options",
        ) {
            SwitchControl(
                title = "Filterable",
                subtitle = "Enable query text field to filter options",
                checked = isFilterable,
                onCheckedChange = {
                    isFilterable = it
                    updatePayload(
                        variant = variant,
                        displayStyle = displayStyle,
                        isFilterable = it,
                        label = label,
                        options = selectedPreset.options,
                        selectedValues = selectedValues,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
            )
        }

        ControlCard(
            title = "Header Label",
            subtitle = "Optional label placed above options",
        ) {
            TextInputControl(
                value = label,
                onValueChange = {
                    label = it
                    updatePayload(
                        variant = variant,
                        displayStyle = displayStyle,
                        isFilterable = isFilterable,
                        label = it,
                        options = selectedPreset.options,
                        selectedValues = selectedValues,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = "Label",
            )
        }

        ControlCard(
            title = "Options Preset",
            subtitle = "Predefined choice lists to test different scenarios",
        ) {
            ChoiceChips(
                options = OptionPreset.entries,
                selectedOption = selectedPreset,
                onOptionSelected = { newPreset ->
                    selectedPreset = newPreset
                    val defaultSelection = listOf(newPreset.options.first().value)
                    selectedValues = defaultSelection
                    updatePayload(
                        variant = variant,
                        displayStyle = displayStyle,
                        isFilterable = isFilterable,
                        label = label,
                        options = newPreset.options,
                        selectedValues = defaultSelection,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = { it.displayName },
            )
        }
    }
}

private fun updatePayload(
    variant: Variant,
    displayStyle: DisplayStyle,
    isFilterable: Boolean,
    label: String,
    options: List<OptionItem>,
    selectedValues: List<String>,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val properties =
        buildMap<String, Any?> {
            if (label.isNotBlank()) {
                put("label", label)
            }
            put("variant", variant.value)
            put("displayStyle", displayStyle.value)
            put("filterable", isFilterable)
            put(
                "options",
                options.map { (label, value) -> mapOf("label" to label, "value" to value) },
            )
            put("value", mapOf("path" to "/selectedChoices"))
        }

    val component =
        A2uiComponentPayload(
            id = "root",
            type = "ChoicePicker",
            properties = properties,
        )

    val dataModel = mapOf("selectedChoices" to selectedValues)
    onPayloadUpdated(listOf(component), dataModel)
}

internal data class OptionItem(val label: String, val value: String)

internal enum class OptionPreset(val displayName: String, val options: List<OptionItem>) {
    CUISINES(
        "Cuisines",
        listOf(
            OptionItem("Italian", "italian"),
            OptionItem("Mexican", "mexican"),
            OptionItem("Japanese", "japanese"),
            OptionItem("Indian", "indian"),
            OptionItem("Thai", "thai"),
            OptionItem("French", "french"),
            OptionItem("Mediterranean", "mediterranean"),
        ),
    ),
    COFFEE(
        "Coffee",
        listOf(
            OptionItem("Espresso", "espresso"),
            OptionItem("Americano", "americano"),
            OptionItem("Cappuccino", "cappuccino"),
            OptionItem("Latte", "latte"),
            OptionItem("Mocha", "mocha"),
            OptionItem("Flat White", "flat_white"),
        ),
    ),
    LANGUAGES(
        "Languages",
        listOf(
            OptionItem("Kotlin", "kotlin"),
            OptionItem("Java", "java"),
            OptionItem("Python", "python"),
            OptionItem("Rust", "rust"),
            OptionItem("TypeScript", "typescript"),
            OptionItem("Go", "go"),
        ),
    ),
}
